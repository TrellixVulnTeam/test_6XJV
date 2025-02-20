/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.bluetooth;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Provides the public APIs to control the Bluetooth HID Device profile.
 *
 * <p>BluetoothHidDevice is a proxy object for controlling the Bluetooth HID Device Service via IPC.
 * Use {@link BluetoothAdapter#getProfileProxy} to get the BluetoothHidDevice proxy object.
 */
public final class BluetoothHidDevice implements BluetoothProfile {
    private static final String TAG = BluetoothHidDevice.class.getSimpleName();

    /**
     * Intent used to broadcast the change in connection state of the Input Host profile.
     *
     * <p>This intent will have 3 extras:
     *
     * <ul>
     *   <li>{@link #EXTRA_STATE} - The current state of the profile.
     *   <li>{@link #EXTRA_PREVIOUS_STATE}- The previous state of the profile.
     *   <li>{@link BluetoothDevice#EXTRA_DEVICE} - The remote device.
     * </ul>
     *
     * <p>{@link #EXTRA_STATE} or {@link #EXTRA_PREVIOUS_STATE} can be any of {@link
     * #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, {@link #STATE_CONNECTED}, {@link
     * #STATE_DISCONNECTING}.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission to receive.
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED";

    /**
     * Constant representing unspecified HID device subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS1_NONE = (byte) 0x00;
    /**
     * Constant representing keyboard subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS1_KEYBOARD = (byte) 0x40;
    /**
     * Constant representing mouse subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS1_MOUSE = (byte) 0x80;
    /**
     * Constant representing combo keyboard and mouse subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS1_COMBO = (byte) 0xC0;

    /**
     * Constant representing uncategorized HID device subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_UNCATEGORIZED = (byte) 0x00;
    /**
     * Constant representing joystick subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_JOYSTICK = (byte) 0x01;
    /**
     * Constant representing gamepad subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_GAMEPAD = (byte) 0x02;
    /**
     * Constant representing remote control subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_REMOTE_CONTROL = (byte) 0x03;
    /**
     * Constant representing sensing device subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_SENSING_DEVICE = (byte) 0x04;
    /**
     * Constant representing digitizer tablet subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_DIGITIZER_TABLET = (byte) 0x05;
    /**
     * Constant representing card reader subclass.
     *
     * @see #registerApp (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     *     BluetoothHidDeviceAppQosSettings, Executor, Callback)
     */
    public static final byte SUBCLASS2_CARD_READER = (byte) 0x06;

    /**
     * Constant representing HID Input Report type.
     *
     * @see Callback#onGetReport(BluetoothDevice, byte, byte, int)
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     * @see Callback#onInterruptData(BluetoothDevice, byte, byte[])
     */
    public static final byte REPORT_TYPE_INPUT = (byte) 1;
    /**
     * Constant representing HID Output Report type.
     *
     * @see Callback#onGetReport(BluetoothDevice, byte, byte, int)
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     * @see Callback#onInterruptData(BluetoothDevice, byte, byte[])
     */
    public static final byte REPORT_TYPE_OUTPUT = (byte) 2;
    /**
     * Constant representing HID Feature Report type.
     *
     * @see Callback#onGetReport(BluetoothDevice, byte, byte, int)
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     * @see Callback#onInterruptData(BluetoothDevice, byte, byte[])
     */
    public static final byte REPORT_TYPE_FEATURE = (byte) 3;

    /**
     * Constant representing success response for Set Report.
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_SUCCESS = (byte) 0;
    /**
     * Constant representing error response for Set Report due to "not ready".
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_NOT_READY = (byte) 1;
    /**
     * Constant representing error response for Set Report due to "invalid report ID".
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_INVALID_RPT_ID = (byte) 2;
    /**
     * Constant representing error response for Set Report due to "unsupported request".
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_UNSUPPORTED_REQ = (byte) 3;
    /**
     * Constant representing error response for Set Report due to "invalid parameter".
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_INVALID_PARAM = (byte) 4;
    /**
     * Constant representing error response for Set Report with unknown reason.
     *
     * @see Callback#onSetReport(BluetoothDevice, byte, byte, byte[])
     */
    public static final byte ERROR_RSP_UNKNOWN = (byte) 14;

    /**
     * Constant representing boot protocol mode used set by host. Default is always {@link
     * #PROTOCOL_REPORT_MODE} unless notified otherwise.
     *
     * @see Callback#onSetProtocol(BluetoothDevice, byte)
     */
    public static final byte PROTOCOL_BOOT_MODE = (byte) 0;
    /**
     * Constant representing report protocol mode used set by host. Default is always {@link
     * #PROTOCOL_REPORT_MODE} unless notified otherwise.
     *
     * @see Callback#onSetProtocol(BluetoothDevice, byte)
     */
    public static final byte PROTOCOL_REPORT_MODE = (byte) 1;

    /**
     * The template class that applications use to call callback functions on events from the HID
     * host. Callback functions are wrapped in this class and registered to the Android system
     * during app registration.
     */
    public abstract static class Callback {

        private static final String TAG = "BluetoothHidDevCallback";

        /**
         * Callback called when application registration state changes. Usually it's called due to
         * either {@link BluetoothHidDevice#registerApp (String, String, String, byte, byte[],
         * Executor, Callback)} or {@link BluetoothHidDevice#unregisterApp()} , but can be also
         * unsolicited in case e.g. Bluetooth was turned off in which case application is
         * unregistered automatically.
         *
         * @param pluggedDevice {@link BluetoothDevice} object which represents host that currently
         *     has Virtual Cable established with device. Only valid when application is registered,
         *     can be <code>null</code>.
         * @param registered <code>true</code> if application is registered, <code>false</code>
         *     otherwise.
         */
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            Log.d(
                    TAG,
                    "onAppStatusChanged: pluggedDevice="
                            + pluggedDevice
                            + " registered="
                            + registered);
        }

        /**
         * Callback called when connection state with remote host was changed. Application can
         * assume than Virtual Cable is established when called with {@link
         * BluetoothProfile#STATE_CONNECTED} <code>state</code>.
         *
         * @param device {@link BluetoothDevice} object representing host device which connection
         *     state was changed.
         * @param state Connection state as defined in {@link BluetoothProfile}.
         */
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.d(TAG, "onConnectionStateChanged: device=" + device + " state=" + state);
        }

        /**
         * Callback called when GET_REPORT is received from remote host. Should be replied by
         * application using {@link BluetoothHidDevice#replyReport(BluetoothDevice, byte, byte,
         * byte[])}.
         *
         * @param type Requested Report Type.
         * @param id Requested Report Id, can be 0 if no Report Id are defined in descriptor.
         * @param bufferSize Requested buffer size, application shall respond with at least given
         *     number of bytes.
         */
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            Log.d(
                    TAG,
                    "onGetReport: device="
                            + device
                            + " type="
                            + type
                            + " id="
                            + id
                            + " bufferSize="
                            + bufferSize);
        }

        /**
         * Callback called when SET_REPORT is received from remote host. In case received data are
         * invalid, application shall respond with {@link
         * BluetoothHidDevice#reportError(BluetoothDevice, byte)}.
         *
         * @param type Report Type.
         * @param id Report Id.
         * @param data Report data.
         */
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            Log.d(TAG, "onSetReport: device=" + device + " type=" + type + " id=" + id);
        }

        /**
         * Callback called when SET_PROTOCOL is received from remote host. Application shall use
         * this information to send only reports valid for given protocol mode. By default, {@link
         * BluetoothHidDevice#PROTOCOL_REPORT_MODE} shall be assumed.
         *
         * @param protocol Protocol Mode.
         */
        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            Log.d(TAG, "onSetProtocol: device=" + device + " protocol=" + protocol);
        }

        /**
         * Callback called when report data is received over interrupt channel. Report Type is
         * assumed to be {@link BluetoothHidDevice#REPORT_TYPE_OUTPUT}.
         *
         * @param reportId Report Id.
         * @param data Report data.
         */
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            Log.d(TAG, "onInterruptData: device=" + device + " reportId=" + reportId);
        }

        /**
         * Callback called when Virtual Cable is removed. After this callback is received connection
         * will be disconnected automatically.
         */
        public void onVirtualCableUnplug(BluetoothDevice device) {
            Log.d(TAG, "onVirtualCableUnplug: device=" + device);
        }
    }

    private Context mContext;
    private ServiceListener mServiceListener;
    private volatile IBluetoothHidDevice mService;
    private BluetoothAdapter mAdapter;

    private static class CallbackWrapper extends IBluetoothHidDeviceCallback.Stub {

        private final Executor mExecutor;
        private final Callback mCallback;

        CallbackWrapper(Executor executor, Callback callback) {
            mExecutor = executor;
            mCallback = callback;
        }

        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onAppStatusChanged(pluggedDevice, registered));
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onConnectionStateChanged(device, state));
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onGetReport(device, type, id, bufferSize));
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onSetReport(device, type, id, data));
        }

        @Override
        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onSetProtocol(device, protocol));
        }

        @Override
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onInterruptData(device, reportId, data));
        }

        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            clearCallingIdentity();
            mExecutor.execute(() -> mCallback.onVirtualCableUnplug(device));
        }
    }

    private final IBluetoothStateChangeCallback mBluetoothStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {

                public void onBluetoothStateChange(boolean up) {
                    Log.d(TAG, "onBluetoothStateChange: up=" + up);
                    synchronized (mConnection) {
                        if (up) {
                            try {
                                if (mService == null) {
                                    Log.d(TAG, "Binding HID Device service...");
                                    doBind();
                                }
                            } catch (IllegalStateException e) {
                                Log.e(TAG, "onBluetoothStateChange: could not bind to HID Dev "
                                        + "service: ", e);
                            } catch (SecurityException e) {
                                Log.e(TAG, "onBluetoothStateChange: could not bind to HID Dev "
                                        + "service: ", e);
                            }
                        } else {
                            Log.d(TAG, "Unbinding service...");
                            doUnbind();
                        }
                    }
                }
            };

    private final ServiceConnection mConnection =
            new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    Log.d(TAG, "onServiceConnected()");
                    mService = IBluetoothHidDevice.Stub.asInterface(service);
                    if (mServiceListener != null) {
                        mServiceListener.onServiceConnected(
                                BluetoothProfile.HID_DEVICE, BluetoothHidDevice.this);
                    }
                }

                public void onServiceDisconnected(ComponentName className) {
                    Log.d(TAG, "onServiceDisconnected()");
                    mService = null;
                    if (mServiceListener != null) {
                        mServiceListener.onServiceDisconnected(BluetoothProfile.HID_DEVICE);
                    }
                }
            };

    BluetoothHidDevice(Context context, ServiceListener listener) {
        mContext = context;
        mServiceListener = listener;
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.registerStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        doBind();
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothHidDevice.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp == null || !mContext.bindServiceAsUser(intent, mConnection, 0,
                mContext.getUser())) {
            Log.e(TAG, "Could not bind to Bluetooth HID Device Service with " + intent);
            return false;
        }
        Log.d(TAG, "Bound to HID Device Service");
        return true;
    }

    void doUnbind() {
        if (mService != null) {
            mService = null;
        }
        //UNISOC: Remove mService !=null to avoid memory leak
        try {
            mContext.unbindService(mConnection);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to unbind HidDevService", e);
        }
    }

    void close() {
        IBluetoothManager mgr = mAdapter.getBluetoothManager();
        if (mgr != null) {
            try {
                mgr.unregisterStateChangeCallback(mBluetoothStateChangeCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        synchronized (mConnection) {
            doUnbind();
        }
        mServiceListener = null;
    }

    /** {@inheritDoc} */
    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                return service.getConnectedDevices();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                return service.getDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public int getConnectionState(BluetoothDevice device) {
        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                return service.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return STATE_DISCONNECTED;
    }

    /**
     * Registers application to be used for HID device. Connections to HID Device are only possible
     * when application is registered. Only one application can be registered at one time. When an
     * application is registered, the HID Host service will be disabled until it is unregistered.
     * When no longer used, application should be unregistered using {@link #unregisterApp()}. The
     * app will be automatically unregistered if it is not foreground. The registration status
     * should be tracked by the application by handling callback from Callback#onAppStatusChanged.
     * The app registration status is not related to the return value of this method.
     *
     * @param sdp {@link BluetoothHidDeviceAppSdpSettings} object of HID Device SDP record. The HID
     *     Device SDP record is required.
     * @param inQos {@link BluetoothHidDeviceAppQosSettings} object of Incoming QoS Settings. The
     *     Incoming QoS Settings is not required. Use null or default
     *     BluetoothHidDeviceAppQosSettings.Builder for default values.
     * @param outQos {@link BluetoothHidDeviceAppQosSettings} object of Outgoing QoS Settings. The
     *     Outgoing QoS Settings is not required. Use null or default
     *     BluetoothHidDeviceAppQosSettings.Builder for default values.
     * @param executor {@link Executor} object on which callback will be executed. The Executor
     *     object is required.
     * @param callback {@link Callback} object to which callback messages will be sent. The Callback
     *     object is required.
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean registerApp(
            BluetoothHidDeviceAppSdpSettings sdp,
            BluetoothHidDeviceAppQosSettings inQos,
            BluetoothHidDeviceAppQosSettings outQos,
            Executor executor,
            Callback callback) {
        boolean result = false;

        if (sdp == null) {
            throw new IllegalArgumentException("sdp parameter cannot be null");
        }

        if (executor == null) {
            throw new IllegalArgumentException("executor parameter cannot be null");
        }

        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null");
        }

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                CallbackWrapper cbw = new CallbackWrapper(executor, callback);
                result = service.registerApp(sdp, inQos, outQos, cbw);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Unregisters application. Active connection will be disconnected and no new connections will
     * be allowed until registered again using {@link #registerApp
     * (BluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings,
     * BluetoothHidDeviceAppQosSettings, Executor, Callback)}. The registration status should be
     * tracked by the application by handling callback from Callback#onAppStatusChanged. The app
     * registration status is not related to the return value of this method.
     *
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean unregisterApp() {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.unregisterApp();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Sends report to remote host using interrupt channel.
     *
     * @param id Report Id, as defined in descriptor. Can be 0 in case Report Id are not defined in
     *     descriptor.
     * @param data Report data, not including Report Id.
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean sendReport(BluetoothDevice device, int id, byte[] data) {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.sendReport(device, id, data);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Sends report to remote host as reply for GET_REPORT request from {@link
     * Callback#onGetReport(BluetoothDevice, byte, byte, int)}.
     *
     * @param type Report Type, as in request.
     * @param id Report Id, as in request.
     * @param data Report data, not including Report Id.
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean replyReport(BluetoothDevice device, byte type, byte id, byte[] data) {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.replyReport(device, type, id, data);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Sends error handshake message as reply for invalid SET_REPORT request from {@link
     * Callback#onSetReport(BluetoothDevice, byte, byte, byte[])}.
     *
     * @param error Error to be sent for SET_REPORT via HANDSHAKE.
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean reportError(BluetoothDevice device, byte error) {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.reportError(device, error);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Gets the application name of the current HidDeviceService user.
     *
     * @return the current user name, or empty string if cannot get the name
     * {@hide}
     */
    public String getUserAppName() {
        final IBluetoothHidDevice service = mService;

        if (service != null) {
            try {
                return service.getUserAppName();
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return "";
    }

    /**
     * Initiates connection to host which is currently paired with this device. If the application
     * is not registered, #connect(BluetoothDevice) will fail. The connection state should be
     * tracked by the application by handling callback from Callback#onConnectionStateChanged. The
     * connection state is not related to the return value of this method.
     *
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean connect(BluetoothDevice device) {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.connect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }

    /**
     * Disconnects from currently connected host. The connection state should be tracked by the
     * application by handling callback from Callback#onConnectionStateChanged. The connection state
     * is not related to the return value of this method.
     *
     * @return true if the command is successfully sent; otherwise false.
     */
    public boolean disconnect(BluetoothDevice device) {
        boolean result = false;

        final IBluetoothHidDevice service = mService;
        if (service != null) {
            try {
                result = service.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
        }

        return result;
    }
}
