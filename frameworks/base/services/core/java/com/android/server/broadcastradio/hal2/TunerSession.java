/**
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.server.broadcastradio.hal2;

import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.hardware.broadcastradio.V2_0.ConfigFlag;
import android.hardware.broadcastradio.V2_0.ITunerSession;
import android.hardware.broadcastradio.V2_0.Result;
import android.hardware.radio.ITuner;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.RemoteException;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Slog;

import java.util.List;
import java.util.Map;
import java.util.Objects;

class TunerSession extends ITuner.Stub {
    private static final String TAG = "BcRadio2Srv.session";
    private static final String kAudioDeviceName = "Radio tuner source";

    private final Object mLock = new Object();

    private final RadioModule mModule;
    private final ITunerSession mHwSession;
    private final TunerCallback mCallback;
    private boolean mIsClosed = false;
    private boolean mIsMuted = false;

    // necessary only for older APIs compatibility
    private RadioManager.BandConfig mDummyConfig = null;

    TunerSession(@NonNull RadioModule module, @NonNull ITunerSession hwSession,
            @NonNull TunerCallback callback) {
        mModule = Objects.requireNonNull(module);
        mHwSession = Objects.requireNonNull(hwSession);
        mCallback = Objects.requireNonNull(callback);
    }

    @Override
    public void close() {
        synchronized (mLock) {
            Slog.i(TAG, "call hidl close");
            checkNotClosedLocked();
            Utils.maybeRethrow(mHwSession::close);
	    //mHwSession.close();
            mIsClosed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return mIsClosed;
    }

    private void checkNotClosedLocked() {
        if (mIsClosed) {
            throw new IllegalStateException("Tuner is closed, no further operations are allowed");
        }
    }

    @Override
    public void setConfiguration(RadioManager.BandConfig config) {
        synchronized (mLock) {
            checkNotClosedLocked();
            mDummyConfig = Objects.requireNonNull(config);
            Slog.i(TAG, "Ignoring setConfiguration - not applicable for broadcastradio HAL 2.x");
            TunerCallback.dispatch(() -> mCallback.mClientCb.onConfigurationChanged(config));
        }
    }

    @Override
    public RadioManager.BandConfig getConfiguration() {
        synchronized (mLock) {
            checkNotClosedLocked();
            return mDummyConfig;
        }
    }

    @Override
    public void setMuted(boolean mute) {
        synchronized (mLock) {
            checkNotClosedLocked();
            if (mIsMuted == mute) return;
            mIsMuted = mute;
            Slog.w(TAG, "Mute via RadioService is not implemented - please handle it via app");
        }
    }

    @Override
    public boolean isMuted() {
        synchronized (mLock) {
            checkNotClosedLocked();
            return mIsMuted;
        }
    }

    @Override
    public void step(boolean directionDown, boolean skipSubChannel) throws RemoteException {
        synchronized (mLock) {
            checkNotClosedLocked();
            int halResult = mHwSession.step(!directionDown);
            Convert.throwOnError("step", halResult);
        }
    }

    @Override
    public void scan(boolean directionDown, boolean skipSubChannel) throws RemoteException {
        synchronized (mLock) {
            checkNotClosedLocked();
            int halResult = mHwSession.scan(!directionDown, skipSubChannel);
            Convert.throwOnError("step", halResult);
        }
    }

    @Override
    public void tune(ProgramSelector selector) throws RemoteException {
        synchronized (mLock) {
            checkNotClosedLocked();
            int halResult = mHwSession.tune(Convert.programSelectorToHal(selector));
            Convert.throwOnError("tune", halResult);
        }
    }

    @Override
    public void cancel() {
        synchronized (mLock) {
            checkNotClosedLocked();
            Utils.maybeRethrow(mHwSession::cancel);
        }
    }

    @Override
    public void cancelAnnouncement() {
        Slog.i(TAG, "Announcements control doesn't involve cancelling at the HAL level in 2.x");
    }

    @Override
    public Bitmap getImage(int id) {
        return mModule.getImage(id);
    }

    @Override
    public boolean startBackgroundScan() {
        Slog.i(TAG, "Explicit background scan trigger is not supported with HAL 2.x");
        TunerCallback.dispatch(() -> mCallback.mClientCb.onBackgroundScanComplete());
        return true;
    }

    @Override
    public void startProgramListUpdates(ProgramList.Filter filter) throws RemoteException {
        synchronized (mLock) {
            checkNotClosedLocked();
            int halResult = mHwSession.startProgramListUpdates(Convert.programFilterToHal(filter));
            Convert.throwOnError("startProgramListUpdates", halResult);
        }
    }

    @Override
    public void stopProgramListUpdates() throws RemoteException {
        synchronized (mLock) {
            checkNotClosedLocked();
            mHwSession.stopProgramListUpdates();
        }
    }

    @Override
    public boolean isConfigFlagSupported(int flag) {
        try {
            isConfigFlagSet(flag);
            return true;
        } catch (IllegalStateException ex) {
            return true;
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    @Override
    public boolean isConfigFlagSet(int flag) {
        Slog.v(TAG, "isConfigFlagSet " + ConfigFlag.toString(flag));
        synchronized (mLock) {
            checkNotClosedLocked();

            MutableInt halResult = new MutableInt(Result.UNKNOWN_ERROR);
            MutableBoolean flagState = new MutableBoolean(false);
            try {
                mHwSession.isConfigFlagSet(flag, (int result, boolean value) -> {
                    halResult.value = result;
                    flagState.value = value;
                });
            } catch (RemoteException ex) {
                throw new RuntimeException("Failed to check flag " + ConfigFlag.toString(flag), ex);
            }
            Convert.throwOnError("isConfigFlagSet", halResult.value);

            return flagState.value;
        }
    }

    @Override
    public void setConfigFlag(int flag, boolean value) throws RemoteException {
        Slog.v(TAG, "setConfigFlag " + ConfigFlag.toString(flag) + " = " + value);
        synchronized (mLock) {
            checkNotClosedLocked();
            int halResult = mHwSession.setConfigFlag(flag, value);
            Convert.throwOnError("setConfigFlag", halResult);
        }
    }

    @Override
    public Map setParameters(Map parameters) {
        synchronized (mLock) {
            checkNotClosedLocked();
            return Convert.vendorInfoFromHal(Utils.maybeRethrow(
                    () -> mHwSession.setParameters(Convert.vendorInfoToHal(parameters))));
        }
    }

    @Override
    public Map getParameters(List<String> keys) {
        synchronized (mLock) {
            checkNotClosedLocked();
            return Convert.vendorInfoFromHal(Utils.maybeRethrow(
                    () -> mHwSession.getParameters(Convert.listToArrayList(keys))));
        }
    }
}
