/*
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.server.storage;

import android.annotation.WorkerThread;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Slog;

import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageManagerService;

import dalvik.system.VMRuntime;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.text.Html;
import android.provider.Settings;

/**
 * Service that monitors and maintains free space on storage volumes.
 * <p>
 * As the free space on a volume nears the threshold defined by
 * {@link StorageManager#getStorageLowBytes(File)}, this service will clear out
 * cached data to keep the disk from entering this low state.
 */
public class DeviceStorageMonitorService extends SystemService {
    private static final String TAG = "DeviceStorageMonitorService";

    /**
     * Extra for {@link android.content.Intent#ACTION_BATTERY_CHANGED}:
     * Current int sequence number of the update.
     */
    public static final String EXTRA_SEQUENCE = "seq";
    public static final String EXTRA_CURRENT_LEVEL = "current_level";

    private static final int MSG_CHECK = 1;

    private static final long DEFAULT_LOG_DELTA_BYTES = DataUnit.MEBIBYTES.toBytes(64);
    private static final long DEFAULT_CHECK_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

    private static final int MSG_POP_DIALOG = 2;
    private static final int MSG_LOCALE_CHANGED = 3;
    private static final long NOTIFICATION_LEVEL_THRESHOLD = 200 * TrafficStats.MB_IN_BYTES;
    private static final long DIALOG_LEVEL_THRESHOLD = 100 * TrafficStats.MB_IN_BYTES;
    private static final long DEFAULT_POP_DLG_INTERVAL = DateUtils.HOUR_IN_MILLIS;
    private static final String ACTION_DEVICE_STORAGE_LOW_STATE = "sprd.intent.action.DEVICE_STORAGE_STATE";
    private static final String INTERNAL_STORAGE_SETTINGS = "com.unisoc.action.STORAGE_CLEAR";
    private static final String HOME_RECENT_ENABLED = "home_recent_key_enabled";

    private Notification.Builder mNotificationBuilder;
    private Notification mLowStorageNotification;
    private Notification mExtremLowStorageNotification;
    private boolean mBootCompleted = false;
    private StateEx mStateEx;

    // com.android.internal.R.string.low_internal_storage_view_text_no_boot
    // hard codes 250MB in the message as the storage space required for the
    // boot image.
    private static final long BOOT_IMAGE_STORAGE_REQUIREMENT = DataUnit.MEBIBYTES.toBytes(250);

    private NotificationManager mNotifManager;

    /** Sequence number used for testing */
    private final AtomicInteger mSeq = new AtomicInteger(1);
    /** Forced level used for testing */
    private volatile int mForceLevel = State.LEVEL_UNKNOWN;

    /** Map from storage volume UUID to internal state */
    private final ArrayMap<UUID, State> mStates = new ArrayMap<>();

    /**
     * State for a specific storage volume, including the current "level" that
     * we've alerted the user and apps about.
     */
    private static class State {
        protected static final int LEVEL_UNKNOWN = -1;
        protected static final int LEVEL_NORMAL = 0;
        protected static final int LEVEL_LOW = 1;
        protected static final int LEVEL_FULL = 2;

        /** Last "level" that we alerted about */
        public int level = LEVEL_NORMAL;
        /** Last {@link File#getUsableSpace()} that we logged about */
        public long lastUsableBytes = Long.MAX_VALUE;

        /**
         * Test if the given level transition is "entering" a specific level.
         * <p>
         * As an example, a transition from {@link #LEVEL_NORMAL} to
         * {@link #LEVEL_FULL} is considered to "enter" both {@link #LEVEL_LOW}
         * and {@link #LEVEL_FULL}.
         */
        private static boolean isEntering(int level, int oldLevel, int newLevel) {
            return newLevel >= level && (oldLevel < level || oldLevel == LEVEL_UNKNOWN);
        }

        /**
         * Test if the given level transition is "leaving" a specific level.
         * <p>
         * As an example, a transition from {@link #LEVEL_FULL} to
         * {@link #LEVEL_NORMAL} is considered to "leave" both
         * {@link #LEVEL_FULL} and {@link #LEVEL_LOW}.
         */
        private static boolean isLeaving(int level, int oldLevel, int newLevel) {
            return newLevel < level && (oldLevel >= level || oldLevel == LEVEL_UNKNOWN);
        }

        private static String levelToString(int level) {
            switch (level) {
                case State.LEVEL_UNKNOWN: return "UNKNOWN";
                case State.LEVEL_NORMAL: return "NORMAL";
                case State.LEVEL_LOW: return "LOW";
                case State.LEVEL_FULL: return "FULL";
                default: return Integer.toString(level);
            }
        }
    }

    private static class StateEx extends State {
        public int level = LEVEL_NORMAL;
        public int newLevel = LEVEL_NORMAL;
        private static boolean isEntering(int level, int oldLevel, int newLevel) {
            return newLevel == level && (oldLevel != level || oldLevel == LEVEL_UNKNOWN);
        }
        private static boolean isLeaving(int level, int oldLevel, int newLevel) {
            return newLevel < level && (oldLevel == level || oldLevel == LEVEL_UNKNOWN);
        }
    }

    private CacheFileDeletedObserver mCacheFileDeletedObserver;

    /**
     * This string is used for ServiceManager access to this class.
     */
    static final String SERVICE = "devicestoragemonitor";

    private static final String TV_NOTIFICATION_CHANNEL_ID = "devicestoragemonitor.tv";

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    private State findOrCreateState(UUID uuid) {
        State state = mStates.get(uuid);
        if (state == null) {
            state = new State();
            mStates.put(uuid, state);
        }
        return state;
    }

    /**
     * Core logic that checks the storage state of every mounted private volume.
     * Since this can do heavy I/O, callers should invoke indirectly using
     * {@link #MSG_CHECK}.
     */
    @WorkerThread
    private void check() {
        final StorageManager storage = getContext().getSystemService(StorageManager.class);
        final int seq = mSeq.get();

        // Check every mounted private volume to see if they're low on space
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            final File file = vol.getPath();
            final long fullBytes = storage.getStorageFullBytes(file);
            final long lowBytes = storage.getStorageLowBytes(file);

            // Automatically trim cached data when nearing the low threshold;
            // when it's within 150% of the threshold, we try trimming usage
            // back to 200% of the threshold.
            if (file.getUsableSpace() < (lowBytes * 3) / 2) {
                final PackageManagerService pms = (PackageManagerService) ServiceManager
                        .getService("package");
                try {
                    pms.freeStorage(vol.getFsUuid(), lowBytes * 2, 0);
                } catch (IOException e) {
                    Slog.w(TAG, e);
                }
            }

            // Send relevant broadcasts and show notifications based on any
            // recently noticed state transitions.
            final UUID uuid = StorageManager.convert(vol.getFsUuid());
            final State state = findOrCreateState(uuid);
            final long totalBytes = file.getTotalSpace();
            final long usableBytes = file.getUsableSpace();

            int oldLevel = state.level;
            int newLevel;
            if (mForceLevel != State.LEVEL_UNKNOWN) {
                // When in testing mode, use unknown old level to force sending
                // of any relevant broadcasts.
                oldLevel = State.LEVEL_UNKNOWN;
                newLevel = mForceLevel;
            } else if (usableBytes <= fullBytes) {
                newLevel = State.LEVEL_FULL;
            } else if (usableBytes <= lowBytes) {
                newLevel = State.LEVEL_LOW;
            } else if (StorageManager.UUID_DEFAULT.equals(uuid) && !isBootImageOnDisk()
                    && usableBytes < BOOT_IMAGE_STORAGE_REQUIREMENT) {
                newLevel = State.LEVEL_LOW;
            } else {
                newLevel = State.LEVEL_NORMAL;
            }

            // Log whenever we notice drastic storage changes
            if ((Math.abs(state.lastUsableBytes - usableBytes) > DEFAULT_LOG_DELTA_BYTES)
                    || oldLevel != newLevel) {
                EventLogTags.writeStorageState(uuid.toString(), oldLevel, newLevel,
                        usableBytes, totalBytes);
                state.lastUsableBytes = usableBytes;
            }
            // only overide UUID_DEFAULT data
            if(!StorageManager.UUID_DEFAULT.equals(uuid))
                updateNotifications(vol, oldLevel, newLevel);
            updateBroadcasts(vol, oldLevel, newLevel, seq);

            state.level = newLevel;

            if(StorageManager.UUID_DEFAULT.equals(uuid) && mBootCompleted) {
                String debugFreeMem = SystemProperties.get("debug.freemem");
                long debugUsableBytes = 0;
                if (!"".equals(debugFreeMem)) {
                    debugUsableBytes = Long.parseLong(debugFreeMem);
                }
                long minUsableBytes = (debugUsableBytes != 0 && debugUsableBytes < usableBytes) ? debugUsableBytes : usableBytes;
                String decryptState = SystemProperties.get("vold.decrypt");
                if(!decryptState.equals("trigger_restart_min_framework")){
                    updateStateEx(minUsableBytes);
                } else {
                    Slog.d(TAG, "enable fde, and now data is mounted with tempfs, skip check!");
                }
            }
        }

        // Loop around to check again in future; we don't remove messages since
        // there might be an immediate request pending.
        if (!mHandler.hasMessages(MSG_CHECK)) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHECK),
                    DEFAULT_CHECK_INTERVAL);
        }
    }


    private void updateStateEx(long usableBytes) {
        int oldLevel = mStateEx.level;
        int newLevel;
        if (usableBytes <= DIALOG_LEVEL_THRESHOLD) {
            newLevel = StateEx.LEVEL_FULL;
        } else if (usableBytes <= NOTIFICATION_LEVEL_THRESHOLD) {
            newLevel = StateEx.LEVEL_LOW;
        } else {
            newLevel = StateEx.LEVEL_NORMAL;
        }
        mStateEx.newLevel = newLevel;
        Slog.d(TAG, "updateStateEx oldLevel = " + oldLevel + ", newLevel = " + newLevel);
        updateExtremLowStorageNotification(oldLevel, newLevel);
        updateLowStorageNotification(oldLevel, newLevel);
        updateStateExBroadcasts(oldLevel, newLevel);
        mStateEx.level = newLevel;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                Slog.d(TAG, "receive ACTION_BOOT_COMPLETED");
                mBootCompleted = true;
                Settings.System.putInt(getContext().getContentResolver(), HOME_RECENT_ENABLED, 1);
            } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                mHandler.sendEmptyMessage(MSG_LOCALE_CHANGED);
            }
        }
    };

    public DeviceStorageMonitorService(Context context) {
        super(context);

        mHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mStateEx = new StateEx();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(mReceiver, filter);

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CHECK:
                        check();
                        return;
                    case MSG_POP_DIALOG:
                        startShowClearActivity();
                        return;
                    case MSG_LOCALE_CHANGED:
                        rebuildNotification();
                        return;
                }
            }
        };
    }

    private static boolean isBootImageOnDisk() {
        for (String instructionSet : InstructionSets.getAllDexCodeInstructionSets()) {
            if (!VMRuntime.isBootClassPathOnDisk(instructionSet)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        final Context context = getContext();
        mNotifManager = context.getSystemService(NotificationManager.class);

        mCacheFileDeletedObserver = new CacheFileDeletedObserver();
        mCacheFileDeletedObserver.startWatching();

        // Ensure that the notification channel is set up
        PackageManager packageManager = context.getPackageManager();
        boolean isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK);

        if (isTv) {
            mNotifManager.createNotificationChannel(new NotificationChannel(
                    TV_NOTIFICATION_CHANNEL_ID,
                    context.getString(
                        com.android.internal.R.string.device_storage_monitor_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH));
        }

        publishBinderService(SERVICE, mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, mLocalService);

        // Kick off pass to examine storage state
        mHandler.removeMessages(MSG_CHECK);
        mHandler.obtainMessage(MSG_CHECK).sendToTarget();
    }

    private final DeviceStorageMonitorInternal mLocalService = new DeviceStorageMonitorInternal() {
        @Override
        public void checkMemory() {
            // Kick off pass to examine storage state
            mHandler.removeMessages(MSG_CHECK);
            mHandler.obtainMessage(MSG_CHECK).sendToTarget();
        }

        @Override
        public boolean isMemoryLow() {
            return Environment.getDataDirectory().getUsableSpace() < getMemoryLowThreshold();
        }

        @Override
        public long getMemoryLowThreshold() {
            return getContext().getSystemService(StorageManager.class)
                    .getStorageLowBytes(Environment.getDataDirectory());
        }
    };

    private final Binder mRemoteService = new Binder() {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(getContext(), TAG, pw)) return;
            dumpImpl(fd, pw, args);
        }

        @Override
        public void onShellCommand(FileDescriptor in, FileDescriptor out,
                FileDescriptor err, String[] args, ShellCallback callback,
                ResultReceiver resultReceiver) {
            (new Shell()).exec(this, in, out, err, args, callback, resultReceiver);
        }
    };

    class Shell extends ShellCommand {
        @Override
        public int onCommand(String cmd) {
            return onShellCommand(this, cmd);
        }

        @Override
        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            dumpHelp(pw);
        }
    }

    static final int OPTION_FORCE_UPDATE = 1<<0;

    int parseOptions(Shell shell) {
        String opt;
        int opts = 0;
        while ((opt = shell.getNextOption()) != null) {
            if ("-f".equals(opt)) {
                opts |= OPTION_FORCE_UPDATE;
            }
        }
        return opts;
    }

    int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        switch (cmd) {
            case "force-low": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_LOW;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            case "force-not-low": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_NORMAL;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            case "reset": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_UNKNOWN;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            default:
                return shell.handleDefaultCommands(cmd);
        }
        return 0;
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device storage monitor service (devicestoragemonitor) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  force-low [-f]");
        pw.println("    Force storage to be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  force-not-low [-f]");
        pw.println("    Force storage to not be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze storage state, returning to current real values.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
    }

    void dumpImpl(FileDescriptor fd, PrintWriter _pw, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(_pw, "  ");
        if (args == null || args.length == 0 || "-a".equals(args[0])) {
            pw.println("Known volumes:");
            pw.increaseIndent();
            for (int i = 0; i < mStates.size(); i++) {
                final UUID uuid = mStates.keyAt(i);
                final State state = mStates.valueAt(i);
                if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                    pw.println("Default:");
                } else {
                    pw.println(uuid + ":");
                }
                pw.increaseIndent();
                pw.printPair("level", State.levelToString(state.level));
                pw.printPair("lastUsableBytes", state.lastUsableBytes);
                pw.println();
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
            pw.println();

            pw.printPair("mSeq", mSeq.get());
            pw.printPair("mForceState", State.levelToString(mForceLevel));
            pw.println();
            pw.println();

        } else {
            Shell shell = new Shell();
            shell.exec(mRemoteService, null, fd, null, args, null, new ResultReceiver(null));
        }
    }

    private void rebuildNotification() {
        if(mStateEx != null && mNotificationBuilder != null && (mLowStorageNotification != null || mExtremLowStorageNotification != null)) {
            switch(mStateEx.level) {
                case StateEx.LEVEL_LOW:
                    if(null != (mLowStorageNotification = createLowStorageNotification(StateEx.LEVEL_LOW))) {
                        mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE, UserHandle.ALL);
                        mNotifManager.notifyAsUser(null, SystemMessage.NOTE_LOW_STORAGE, mLowStorageNotification, UserHandle.ALL);
                    }
                    break;
                case StateEx.LEVEL_FULL:
                    if(null != (mExtremLowStorageNotification = createLowStorageNotification(StateEx.LEVEL_FULL))) {
                        mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE, UserHandle.ALL);
                        mNotifManager.notifyAsUser(null, SystemMessage.NOTE_LOW_STORAGE, mExtremLowStorageNotification, UserHandle.ALL);
                    }
                    break;
                default:
                    return;
            }
        }
    }

    private void creatLowStorageNotiBuilder(Context context) {
        mNotificationBuilder =
                new Notification.Builder(context, SystemNotificationChannels.ALERTS)
                        .setSmallIcon(com.android.internal.R.drawable.stat_notify_disk_full)
                        .setColor(context.getColor(
                                com.android.internal.R.color.system_notification_accent_color))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setCategory(Notification.CATEGORY_SYSTEM)
                        .extend(new Notification.TvExtender()
                                .setChannelId(TV_NOTIFICATION_CHANNEL_ID));
    }

    private Notification createLowStorageNotification(int level) {
        Context context = getContext();
        CharSequence title;
        CharSequence details;
        Intent lowMemIntent = new Intent(INTERNAL_STORAGE_SETTINGS);
        lowMemIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        lowMemIntent.putExtra("level", level);
        switch (level) {
            case StateEx.LEVEL_LOW:
                title = context.getText(com.android.internal.R.string.low_storage_view_title);
                details = context.getText(com.android.internal.R.string.low_storage_view_details);
                break;
            case StateEx.LEVEL_FULL:
                title = context.getText(com.android.internal.R.string.extrem_low_storage_view_title);
                details = context.getText(com.android.internal.R.string.extrem_low_storage_view_details);
                break;
            default:
                return null;
        }
        PendingIntent intent = PendingIntent.getActivityAsUser(getContext(), 0, lowMemIntent, 0,
                null, UserHandle.CURRENT);
        String lowTitle = "<b>" + title + "</b>";
        mNotificationBuilder.setTicker(Html.fromHtml(lowTitle))
                .setContentText(details)
                .setContentTitle(Html.fromHtml(lowTitle))
                .setStyle(new Notification.BigTextStyle()
                        .bigText(details))
                .setContentIntent(intent);
        Notification notification = mNotificationBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return  notification;
    }

    private void updateLowStorageNotification(int oldLevel, int newLevel) {
        final Context context = getContext();
        if(mNotificationBuilder == null) {
            creatLowStorageNotiBuilder(context);
        }
        if(StateEx.isEntering(StateEx.LEVEL_LOW, oldLevel, newLevel)) {
            if(null == (mLowStorageNotification = createLowStorageNotification(StateEx.LEVEL_LOW)))  return;
            mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
            mNotifManager.notifyAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    mLowStorageNotification, UserHandle.ALL);
        }
        else if (StateEx.isLeaving(StateEx.LEVEL_LOW, oldLevel, newLevel)) {
            mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
        }
    }

    private void updateExtremLowStorageNotification(int oldLevel, int newLevel) {
        final Context context = getContext();
        if(mNotificationBuilder == null) {
            creatLowStorageNotiBuilder(context);
        }
        if(StateEx.isEntering(StateEx.LEVEL_FULL, oldLevel, newLevel)) {
            if(null == (mExtremLowStorageNotification = createLowStorageNotification(StateEx.LEVEL_FULL)))  return;
            mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
            mNotifManager.notifyAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    mExtremLowStorageNotification, UserHandle.ALL);
            startShowClearActivity();
        }
        else if (StateEx.isLeaving(StateEx.LEVEL_FULL, oldLevel, newLevel)) {
            mNotifManager.cancelAsUser(null, SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
            //mHandler.removeMessages(MSG_POP_DIALOG);
        }
    }

    private void startShowClearActivity() {
        if(mBootCompleted) {
            Intent intent = new Intent(INTERNAL_STORAGE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            getContext().startActivityAsUser(intent, UserHandle.CURRENT);
            //postSendPopDialogMsgDelay();
        }

    }

    private void postSendPopDialogMsgDelay() {
        mHandler.removeMessages(MSG_POP_DIALOG);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_POP_DIALOG), DEFAULT_POP_DLG_INTERVAL);
    }

    private void updateNotifications(VolumeInfo vol, int oldLevel, int newLevel) {
        final Context context = getContext();
        final UUID uuid = StorageManager.convert(vol.getFsUuid());

        if (State.isEntering(State.LEVEL_LOW, oldLevel, newLevel)) {
            Intent lowMemIntent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
            lowMemIntent.putExtra(StorageManager.EXTRA_UUID, uuid);
            lowMemIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            final CharSequence title = context.getText(
                    com.android.internal.R.string.low_internal_storage_view_title);

            final CharSequence details;
            if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                details = context.getText(isBootImageOnDisk()
                        ? com.android.internal.R.string.low_internal_storage_view_text
                        : com.android.internal.R.string.low_internal_storage_view_text_no_boot);
            } else {
                details = context.getText(
                        com.android.internal.R.string.low_internal_storage_view_text);
            }

            PendingIntent intent = PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0,
                    null, UserHandle.CURRENT);
            Notification notification =
                    new Notification.Builder(context, SystemNotificationChannels.ALERTS)
                            .setSmallIcon(com.android.internal.R.drawable.stat_notify_disk_full)
                            .setTicker(title)
                            .setColor(context.getColor(
                                com.android.internal.R.color.system_notification_accent_color))
                            .setContentTitle(title)
                            .setContentText(details)
                            .setContentIntent(intent)
                            .setStyle(new Notification.BigTextStyle()
                                  .bigText(details))
                            .setVisibility(Notification.VISIBILITY_PUBLIC)
                            .setCategory(Notification.CATEGORY_SYSTEM)
                            .extend(new Notification.TvExtender()
                                    .setChannelId(TV_NOTIFICATION_CHANNEL_ID))
                            .build();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            mNotifManager.notifyAsUser(uuid.toString(), SystemMessage.NOTE_LOW_STORAGE,
                    notification, UserHandle.ALL);
        } else if (State.isLeaving(State.LEVEL_LOW, oldLevel, newLevel)) {
            mNotifManager.cancelAsUser(uuid.toString(), SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
        }
    }

    private void updateStateExBroadcasts(int oldLevel, int newLevel) {

        final Intent currentLevelIntent = new Intent(ACTION_DEVICE_STORAGE_LOW_STATE)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
                .putExtra(EXTRA_CURRENT_LEVEL, newLevel);

        boolean update = false;
        if (StateEx.isEntering(StateEx.LEVEL_LOW, oldLevel, newLevel)) {
            update = true;
        } else if (StateEx.isLeaving(StateEx.LEVEL_LOW, oldLevel, newLevel)) {
            update = true;
        } else if (StateEx.isEntering(StateEx.LEVEL_FULL, oldLevel, newLevel)) {
            update = true;
        } else if (StateEx.isLeaving(StateEx.LEVEL_FULL, oldLevel, newLevel)) {
            update = true;
        }
        if(update) {
            getContext().sendStickyBroadcastAsUser(currentLevelIntent, UserHandle.ALL);
        }
    }

    private void updateBroadcasts(VolumeInfo vol, int oldLevel, int newLevel, int seq) {
        if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, vol.getFsUuid())) {
            // We don't currently send broadcasts for secondary volumes
            return;
        }

        final Intent lowIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_LOW)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
                .putExtra(EXTRA_SEQUENCE, seq);
        final Intent notLowIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_OK)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
                .putExtra(EXTRA_SEQUENCE, seq);

        if (State.isEntering(State.LEVEL_LOW, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
        } else if (State.isLeaving(State.LEVEL_LOW, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notLowIntent, UserHandle.ALL);
        }

        final Intent fullIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_FULL)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT)
                .putExtra(EXTRA_SEQUENCE, seq);
        final Intent notFullIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_NOT_FULL)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT)
                .putExtra(EXTRA_SEQUENCE, seq);

        if (State.isEntering(State.LEVEL_FULL, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
        } else if (State.isLeaving(State.LEVEL_FULL, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notFullIntent, UserHandle.ALL);
        }
    }

    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), FileObserver.DELETE);
        }

        @Override
        public void onEvent(int event, String path) {
            EventLogTags.writeCacheFileDeleted(path);
        }
    }
}
