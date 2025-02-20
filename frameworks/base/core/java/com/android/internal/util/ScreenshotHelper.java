package com.android.internal.util;

import android.annotation.NonNull;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

public class ScreenshotHelper {
    private static final String TAG = "ScreenshotHelper";

    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_SCREENSHOT_SERVICE =
            "com.android.systemui.screenshot.TakeScreenshotService";
    private static final String SYSUI_SCREENSHOT_ERROR_RECEIVER =
            "com.android.systemui.screenshot.ScreenshotServiceErrorReceiver";

    // Time until we give up on the screenshot & show an error instead.
    //UNISOC: fix for bug 942870
    private final int SCREENSHOT_TIMEOUT_MS = ActivityManager.isLowRamDeviceStatic() ? 20000 : 10000;

    private final Object mScreenshotLock = new Object();
    private ServiceConnection mScreenshotConnection = null;
    private final Context mContext;

    public ScreenshotHelper(Context context) {
        mContext = context;
    }

    /**
     * Request a screenshot be taken.
     *
     * @param screenshotType The type of screenshot, for example either
     *                       {@link android.view.WindowManager.TAKE_SCREENSHOT_FULLSCREEN}
     *                       or {@link android.view.WindowManager.TAKE_SCREENSHOT_SELECTED_REGION}
     * @param hasStatus {@code true} if the status bar is currently showing. {@code false} if not.
     * @param hasNav {@code true} if the navigation bar is currently showing. {@code false} if not.
     * @param handler A handler used in case the screenshot times out
     */
    public void takeScreenshot(final int screenshotType, final boolean hasStatus,
            final boolean hasNav, @NonNull Handler handler) {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            final ComponentName serviceComponent = new ComponentName(SYSUI_PACKAGE,
                    SYSUI_SCREENSHOT_SERVICE);
            final Intent serviceIntent = new Intent();

            final Runnable mScreenshotTimeout = new Runnable() {
                @Override public void run() {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != null) {
                            mContext.unbindService(mScreenshotConnection);
                            mScreenshotConnection = null;
                            notifyScreenshotError();
                        }
                    }
                }
            };

            serviceIntent.setComponent(serviceComponent);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, screenshotType);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(handler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        handler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = hasStatus ? 1: 0;
                        msg.arg2 = hasNav ? 1: 0;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Couldn't take screenshot: " + e);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != null) {
                            mContext.unbindService(mScreenshotConnection);
                            mScreenshotConnection = null;
                            handler.removeCallbacks(mScreenshotTimeout);
                            notifyScreenshotError();
                        }
                    }
                }
            };
            if (mContext.bindServiceAsUser(serviceIntent, conn,
                    Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
                    UserHandle.CURRENT)) {
                mScreenshotConnection = conn;
                handler.postDelayed(mScreenshotTimeout, SCREENSHOT_TIMEOUT_MS);
            }
        }
    }

    /**
     * Notifies the screenshot service to show an error.
     */
    private void notifyScreenshotError() {
        // If the service process is killed, then ask it to clean up after itself
        final ComponentName errorComponent = new ComponentName(SYSUI_PACKAGE,
                SYSUI_SCREENSHOT_ERROR_RECEIVER);
        // Broadcast needs to have a valid action.  We'll just pick
        // a generic one, since the receiver here doesn't care.
        Intent errorIntent = new Intent(Intent.ACTION_USER_PRESENT);
        errorIntent.setComponent(errorComponent);
        errorIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT |
                Intent.FLAG_RECEIVER_FOREGROUND);
        mContext.sendBroadcastAsUser(errorIntent, UserHandle.CURRENT);
    }

}
