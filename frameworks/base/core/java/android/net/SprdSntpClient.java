/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import android.os.SystemClock;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@hide}
 *
 * Simple SNTP client class for retrieving network time.
 *
 * Sample usage:
 * <pre>SntpClient client = new SntpClient();
 * if (client.requestTime("time.foo.com")) {
 *     long now = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
 * }
 * </pre>
 */
public class SprdSntpClient
{
    private static final String TAG = "SprdSntpClient";
    private static final boolean DBG = true;

    private static final int REFERENCE_TIME_OFFSET = 16;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_MODE_SERVER = 4;
    private static final int NTP_MODE_BROADCAST = 5;
    private static final int NTP_VERSION = 3;

    private static final int NTP_LEAP_NOSYNC = 3;
    private static final int NTP_STRATUM_DEATH = 0;
    private static final int NTP_STRATUM_MAX = 15;

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    // system time computed from NTP server response
    private long mNtpTime;

    // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
    private long mNtpTimeReference;

    // round trip time in milliseconds
    private long mRoundTripTime;

    private static class InvalidServerReplyException extends Exception {
        public InvalidServerReplyException(String message) {
            super(message);
        }
    }

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param host host name of the server.
     * @param timeout network timeout in milliseconds.
     * @param network network over which to send the request.
     * @return true if the transaction was successful.
     */
    public boolean requestTime(String host, int timeout, Network network) {
        Log.d(TAG, "Start requestTime:" + host);
        // This flag only affects DNS resolution and not other socket semantics,
        // therefore it's safe to set unilaterally rather than take more
        // defensive measures like making a copy.
        network.setPrivateDnsBypass(true);
        InetAddress[] address=null;
        try {
            address = network.getAllByName(host);
        } catch (Exception e) {
           Log.d(TAG, "request (" + host + ")time failed: " + e);
           return false;
        }finally {
            if (address == null || ( address != null && address.length == 0)) {
                Log.d(TAG,"address=" +address+",request time failed");
                return false;
            }
        }
        return requestTime(address, NTP_PORT, timeout, network);
    }

    public boolean requestTime(InetAddress address, int port, int timeout, Network network) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            network.bindSocket(socket);
            socket.setSoTimeout(timeout);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, port);

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

            // get current time and write it to the request packet
            final long requestTime = System.currentTimeMillis();
            final long requestTicks = SystemClock.elapsedRealtime();
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

            socket.send(request);

            // read the response
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            final long responseTicks = SystemClock.elapsedRealtime();
            final long responseTime = requestTime + (responseTicks - requestTicks);

            // extract the results
            final byte leap = (byte) ((buffer[0] >> 6) & 0x3);
            final byte mode = (byte) (buffer[0] & 0x7);
            final int stratum = (int) (buffer[1] & 0xff);
            final long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            final long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            final long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);

            /* do sanity check according to RFC */
            // TODO: validate originateTime == requestTime.
            checkValidServerReply(leap, mode, stratum, transmitTime);

            long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);
            // receiveTime = originateTime + transit + skew
            // responseTime = transmitTime + transit - skew
            // clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2
            //             = ((originateTime + transit + skew - originateTime) +
            //                (transmitTime - (transmitTime + transit - skew)))/2
            //             = ((transit + skew) + (transmitTime - transmitTime - transit + skew))/2
            //             = (transit + skew - transit + skew)/2
            //             = (2 * skew)/2 = skew
            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2;
            if (DBG) {
                Log.d(TAG, "round trip: " + roundTripTime + "ms, " +
                        "clock offset: " + clockOffset + "ms");
            }

            // save our results - use the times on this side of the network latency
            // (response rather than request time)
            mNtpTime = responseTime + clockOffset;

            Log.d(TAG, "request mNtpTime: " + mNtpTime + " ,clockOffset: " + clockOffset + " ,transmitTime:" + transmitTime +
                        " ,originateTime " + originateTime + ",receiveTime" + receiveTime + " ,responseTime" + responseTime);

            mNtpTimeReference = responseTicks;
            mRoundTripTime = roundTripTime;
        } catch (Exception e) {
            if (DBG) Log.d(TAG, "request time failed: " + e);
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
        return true;
    }


     public boolean requestTime(InetAddress[] address, int port, int timeout, Network network) {
        DatagramChannel[] channel = {null, null, null, null, null, null, null, null};
        SelectionKey[] key = {null, null, null, null, null, null, null, null};
        int count = 0;
        Selector select = null;
        try {
            //InetAddress[] address = InetAddress.getAllByName(host);
            // only send <=8 ntp query packages
            if (address.length > 8)
                count = 8;
            else
                count = address.length;

            byte[] buffer = new byte[NTP_PACKET_SIZE];

            for (int i = 0; i < count; ++i) {
                channel[i] = DatagramChannel.open();
                channel[i].configureBlocking(false);
            }

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

            // get current time and write it to the request packet
            long requestTime = System.currentTimeMillis();
            long requestTicks = SystemClock.elapsedRealtime();
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            select = Selector.open();
            for (int i = 0; i < count; ++i) {
                SocketAddress remoteAddr = new InetSocketAddress(address[i], NTP_PORT);
                channel[i].connect(remoteAddr);
                channel[i].write(byteBuffer);
                byteBuffer.position(0); // reset postion for next
                key[i] = channel[i].register(select, SelectionKey.OP_READ);
            }
            boolean succ = false;
            if (select.select(timeout) > 0) {
                for (int i = 0; i < count; ++i)
                    if (key[i].readyOps() == SelectionKey.OP_READ) {
                        byteBuffer.clear();
                        Log.d(TAG, "received Data" + address[i]);
                        if (channel[i].read(byteBuffer) == NTP_PACKET_SIZE) {
                            succ = true;
                            break;
                        }
                    }
            }
            if (!succ) {
                Log.d(TAG, "not Sucesss");
                throw new IOException("Received ntp response");
            }

            long responseTicks = SystemClock.elapsedRealtime();
            long responseTime = requestTime + (responseTicks - requestTicks);

            // extract the results
            final byte leap = (byte) ((buffer[0] >> 6) & 0x3);
            final byte mode = (byte) (buffer[0] & 0x7);
            final int stratum = (int) (buffer[1] & 0xff);

            long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);

              /* do sanity check according to RFC */
            // TODO: validate originateTime == requestTime.
            checkValidServerReply(leap, mode, stratum, transmitTime);

            long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);
            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;

            mNtpTime = responseTime + clockOffset;
            mNtpTimeReference = responseTicks;
            mRoundTripTime = roundTripTime;
        } catch (Exception e) {
            Log.d(TAG, "request time failed: " + e);
            return false;
        } finally {
            try {
                if (select != null && select.isOpen()) {
                    select.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "close select failed: " + e);
            }

            if (count > 0) {
                for (int i = 0; i < count; ++i)
                    try {
                        if (channel[i] != null)
                            channel[i].close();
                    } catch (Exception e) {
                        if (false)
                            Log.d(TAG, "close channel failed: " + e);
                    }
            }
        }
        Log.d(TAG, "request time Sucess: ");
        return true;
    }

    /**
     * Returns the time computed from the NTP transaction.
     *
     * @return time value computed from NTP server response.
     */
    public long getNtpTime() {
        return mNtpTime;
    }

    /**
     * Returns the reference clock value (value of SystemClock.elapsedRealtime())
     * corresponding to the NTP time.
     *
     * @return reference clock corresponding to the NTP time.
     */
    public long getNtpTimeReference() {
        return mNtpTimeReference;
    }

    /**
     * Returns the round trip time of the NTP transaction
     *
     * @return round trip time in milliseconds.
     */
    public long getRoundTripTime() {
        return mRoundTripTime;
    }

    private static void checkValidServerReply(
            byte leap, byte mode, int stratum, long transmitTime)
            throws InvalidServerReplyException {
        if (leap == NTP_LEAP_NOSYNC) {
            throw new InvalidServerReplyException("unsynchronized server");
        }
        if ((mode != NTP_MODE_SERVER) && (mode != NTP_MODE_BROADCAST)) {
            throw new InvalidServerReplyException("untrusted mode: " + mode);
        }
        if ((stratum == NTP_STRATUM_DEATH) || (stratum > NTP_STRATUM_MAX)) {
            throw new InvalidServerReplyException("untrusted stratum: " + stratum);
        }
        if (transmitTime == 0) {
            throw new InvalidServerReplyException("zero transmitTime");
        }
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset+1];
        byte b2 = buffer[offset+2];
        byte b3 = buffer[offset+3];

        // convert signed bytes to unsigned values
        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

        return ((long)i0 << 24) + ((long)i1 << 16) + ((long)i2 << 8) + (long)i3;
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        // Special case: zero means zero.
        if (seconds == 0 && fraction == 0) {
            return 0;
        }

        /* SRPD: Add for Bug877241 @{ */
        if((offset == ORIGINATE_TIME_OFFSET) && (seconds < OFFSET_1900_TO_1970)){
            seconds += 0x100000000L;
        }
        /* @} */

        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
    }

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private void writeTimeStamp(byte[] buffer, int offset, long time) {
        // Special case: zero means zero.
        if (time == 0) {
            Arrays.fill(buffer, offset, offset + 8, (byte) 0x00);
            return;
        }

        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;

        // write seconds in big endian format
        buffer[offset++] = (byte)(seconds >> 24);
        buffer[offset++] = (byte)(seconds >> 16);
        buffer[offset++] = (byte)(seconds >> 8);
        buffer[offset++] = (byte)(seconds >> 0);

        long fraction = milliseconds * 0x100000000L / 1000L;
        // write fraction in big endian format
        buffer[offset++] = (byte)(fraction >> 24);
        buffer[offset++] = (byte)(fraction >> 16);
        buffer[offset++] = (byte)(fraction >> 8);
        // low order bits should be random data
        buffer[offset++] = (byte)(Math.random() * 255.0);
    }
}
