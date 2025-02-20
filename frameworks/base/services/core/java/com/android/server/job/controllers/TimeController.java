/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.job.controllers;

import static com.android.server.job.JobSchedulerService.sElapsedRealtimeClock;

import android.annotation.Nullable;
import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.app.usage.UsageStatsManagerInternal;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;

import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.StateControllerProto;

import com.android.server.power.sprdpower.PowerController;
import com.android.server.power.sprdpower.AppIdleHelper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

/**
 * This class sets an alarm for the next expiring job, and determines whether a job's minimum
 * delay has been satisfied.
 */
public final class TimeController extends StateController {
    private static final String TAG = "JobScheduler.Time";
    private static final boolean DEBUG = JobSchedulerService.DEBUG
            || Log.isLoggable(TAG, Log.DEBUG);

    /** Deadline alarm tag for logging purposes */
    private final String DEADLINE_TAG = "*job.deadline*";
    /** Delay alarm tag for logging purposes */
    private final String DELAY_TAG = "*job.delay*";

    private long mNextJobExpiredElapsedMillis;
    private long mNextDelayExpiredElapsedMillis;

    private final boolean mChainedAttributionEnabled;

    private AlarmManager mAlarmService = null;
    /** List of tracked jobs, sorted asc. by deadline */
    private final List<JobStatus> mTrackedJobs = new LinkedList<>();

    public TimeController(JobSchedulerService service) {
        super(service);

        mNextJobExpiredElapsedMillis = Long.MAX_VALUE;
        mNextDelayExpiredElapsedMillis = Long.MAX_VALUE;
        mChainedAttributionEnabled = WorkSource.isChainedBatteryAttributionEnabled(mContext);

        // ---NOTE: Bug #627645 low power Feature BEG---
        mPowerControllerHelper = new PowerControllerHelper();
        // ---NOTE: Bug #627645 low power Feature END---
    }

    /**
     * Check if the job has a timing constraint, and if so determine where to insert it in our
     * list.
     */
    @Override
    public void maybeStartTrackingJobLocked(JobStatus job, JobStatus lastJob) {
        if (job.hasTimingDelayConstraint() || job.hasDeadlineConstraint()) {
            maybeStopTrackingJobLocked(job, null, false);

            // First: check the constraints now, because if they are already satisfied
            // then there is no need to track it.  This gives us a fast path for a common
            // pattern of having a job with a 0 deadline constraint ("run immediately").
            // Unlike most controllers, once one of our constraints has been satisfied, it
            // will never be unsatisfied (our time base can not go backwards).
            final long nowElapsedMillis = sElapsedRealtimeClock.millis();
            if (job.hasDeadlineConstraint() && evaluateDeadlineConstraint(job, nowElapsedMillis)) {
                return;
            } else if (job.hasTimingDelayConstraint() && evaluateTimingDelayConstraint(job,
                    nowElapsedMillis)) {
                if (!job.hasDeadlineConstraint()) {
                    // If it doesn't have a deadline, we'll never have to touch it again.
                    return;
                }
            }

            boolean isInsert = false;
            ListIterator<JobStatus> it = mTrackedJobs.listIterator(mTrackedJobs.size());
            while (it.hasPrevious()) {
                JobStatus ts = it.previous();
                if (ts.getLatestRunTimeElapsed() < job.getLatestRunTimeElapsed()) {
                    // Insert
                    isInsert = true;
                    break;
                }
            }
            if (isInsert) {
                it.next();
            }
            it.add(job);
            job.setTrackingController(JobStatus.TRACKING_TIME);
            maybeUpdateAlarmsLocked(
                    job.hasTimingDelayConstraint() ? job.getEarliestRunTime() : Long.MAX_VALUE,
                    job.hasDeadlineConstraint() ? job.getLatestRunTimeElapsed() : Long.MAX_VALUE,
                    deriveWorkSource(job.getSourceUid(), job.getSourcePackageName()));
        }
    }

    /**
     * When we stop tracking a job, we only need to update our alarms if the job we're no longer
     * tracking was the one our alarms were based off of.
     */
    @Override
    public void maybeStopTrackingJobLocked(JobStatus job, JobStatus incomingJob,
            boolean forUpdate) {
        if (job.clearTrackingController(JobStatus.TRACKING_TIME)) {
            if (mTrackedJobs.remove(job)) {
                checkExpiredDelaysAndResetAlarm();
                checkExpiredDeadlinesAndResetAlarm();
            }
        }
    }

    /**
     * Determines whether this controller can stop tracking the given job.
     * The controller is no longer interested in a job once its time constraint is satisfied, and
     * the job's deadline is fulfilled - unlike other controllers a time constraint can't toggle
     * back and forth.
     */
    private boolean canStopTrackingJobLocked(JobStatus job) {
        return (!job.hasTimingDelayConstraint() ||
                (job.satisfiedConstraints&JobStatus.CONSTRAINT_TIMING_DELAY) != 0) &&
                (!job.hasDeadlineConstraint() ||
                        (job.satisfiedConstraints&JobStatus.CONSTRAINT_DEADLINE) != 0);
    }

    private void ensureAlarmServiceLocked() {
        if (mAlarmService == null) {
            mAlarmService = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
    }

    /**
     * Checks list of jobs for ones that have an expired deadline, sending them to the JobScheduler
     * if so, removing them from this list, and updating the alarm for the next expiry time.
     */
    private void checkExpiredDeadlinesAndResetAlarm() {
        synchronized (mLock) {
            long nextExpiryTime = Long.MAX_VALUE;
            int nextExpiryUid = 0;
            String nextExpiryPackageName = null;
            final long nowElapsedMillis = sElapsedRealtimeClock.millis();

            Iterator<JobStatus> it = mTrackedJobs.iterator();
            while (it.hasNext()) {
                JobStatus job = it.next();
                if (!job.hasDeadlineConstraint()) {
                    continue;
                }

                // ---Bug #627645 low power Feature BEG---
                if (mPowerControllerHelper != null
                    && !mPowerControllerHelper.evaluateConstraint(job)) {
                    continue;
                }
                // ---Bug #627645 low power Feature END---

                if (evaluateDeadlineConstraint(job, nowElapsedMillis)) {
                    mStateChangedListener.onRunJobNow(job);
                    it.remove();
                } else {  // Sorted by expiry time, so take the next one and stop.
                    nextExpiryTime = job.getLatestRunTimeElapsed();
                    nextExpiryUid = job.getSourceUid();
                    nextExpiryPackageName = job.getSourcePackageName();
                    break;
                }
            }
            setDeadlineExpiredAlarmLocked(nextExpiryTime,
                    deriveWorkSource(nextExpiryUid, nextExpiryPackageName));
        }
    }

    private boolean evaluateDeadlineConstraint(JobStatus job, long nowElapsedMillis) {
        final long jobDeadline = job.getLatestRunTimeElapsed();

        if (jobDeadline <= nowElapsedMillis) {
            if (job.hasTimingDelayConstraint()) {
                job.setTimingDelayConstraintSatisfied(true);
            }
            job.setDeadlineConstraintSatisfied(true);
            return true;
        }
        return false;
    }

    /**
     * Handles alarm that notifies us that a job's delay has expired. Iterates through the list of
     * tracked jobs and marks them as ready as appropriate.
     */
    private void checkExpiredDelaysAndResetAlarm() {
        synchronized (mLock) {
            final long nowElapsedMillis = sElapsedRealtimeClock.millis();
            long nextDelayTime = Long.MAX_VALUE;
            int nextDelayUid = 0;
            String nextDelayPackageName = null;
            boolean ready = false;
            Iterator<JobStatus> it = mTrackedJobs.iterator();
            while (it.hasNext()) {
                final JobStatus job = it.next();
                if (!job.hasTimingDelayConstraint()) {
                    continue;
                }

                // ---Bug #627645 low power Feature BEG---
                if (mPowerControllerHelper != null
                    && !mPowerControllerHelper.evaluateConstraint(job)) {
                    continue;
                }
                // ---Bug #627645 low power Feature END---

                if (evaluateTimingDelayConstraint(job, nowElapsedMillis)) {
                    if (canStopTrackingJobLocked(job)) {
                        it.remove();
                    }
                    if (job.isReady()) {
                        ready = true;
                    }
                } else if (!job.isConstraintSatisfied(JobStatus.CONSTRAINT_TIMING_DELAY)) {
                    // If this job still doesn't have its delay constraint satisfied,
                    // then see if it is the next upcoming delay time for the alarm.
                    final long jobDelayTime = job.getEarliestRunTime();
                    if (nextDelayTime > jobDelayTime) {
                        nextDelayTime = jobDelayTime;
                        nextDelayUid = job.getSourceUid();
                        nextDelayPackageName = job.getSourcePackageName();
                    }
                }
            }
            if (ready) {
                mStateChangedListener.onControllerStateChanged();
            }
            setDelayExpiredAlarmLocked(nextDelayTime,
                    deriveWorkSource(nextDelayUid, nextDelayPackageName));
        }
    }

    private WorkSource deriveWorkSource(int uid, @Nullable String packageName) {
        if (mChainedAttributionEnabled) {
            WorkSource ws = new WorkSource();
            ws.createWorkChain()
                    .addNode(uid, packageName)
                    .addNode(Process.SYSTEM_UID, "JobScheduler");
            return ws;
        } else {
            return packageName == null ? new WorkSource(uid) : new WorkSource(uid, packageName);
        }
    }

    private boolean evaluateTimingDelayConstraint(JobStatus job, long nowElapsedMillis) {
        final long jobDelayTime = job.getEarliestRunTime();
        if (jobDelayTime <= nowElapsedMillis) {
            job.setTimingDelayConstraintSatisfied(true);
            return true;
        }
        return false;
    }

    private void maybeUpdateAlarmsLocked(long delayExpiredElapsed, long deadlineExpiredElapsed,
            WorkSource ws) {
        if (delayExpiredElapsed < mNextDelayExpiredElapsedMillis) {
            setDelayExpiredAlarmLocked(delayExpiredElapsed, ws);
        }
        if (deadlineExpiredElapsed < mNextJobExpiredElapsedMillis) {
            setDeadlineExpiredAlarmLocked(deadlineExpiredElapsed, ws);
        }
    }

    /**
     * Set an alarm with the {@link android.app.AlarmManager} for the next time at which a job's
     * delay will expire.
     * This alarm <b>will</b> wake up the phone.
     */
    private void setDelayExpiredAlarmLocked(long alarmTimeElapsedMillis, WorkSource ws) {
        alarmTimeElapsedMillis = maybeAdjustAlarmTime(alarmTimeElapsedMillis);
        mNextDelayExpiredElapsedMillis = alarmTimeElapsedMillis;
        updateAlarmWithListenerLocked(DELAY_TAG, mNextDelayExpiredListener,
                mNextDelayExpiredElapsedMillis, ws);
    }

    /**
     * Set an alarm with the {@link android.app.AlarmManager} for the next time at which a job's
     * deadline will expire.
     * This alarm <b>will</b> wake up the phone.
     */
    private void setDeadlineExpiredAlarmLocked(long alarmTimeElapsedMillis, WorkSource ws) {
        alarmTimeElapsedMillis = maybeAdjustAlarmTime(alarmTimeElapsedMillis);
        mNextJobExpiredElapsedMillis = alarmTimeElapsedMillis;
        updateAlarmWithListenerLocked(DEADLINE_TAG, mDeadlineExpiredListener,
                mNextJobExpiredElapsedMillis, ws);
    }

    private long maybeAdjustAlarmTime(long proposedAlarmTimeElapsedMillis) {
        final long earliestWakeupTimeElapsed = sElapsedRealtimeClock.millis();
        if (proposedAlarmTimeElapsedMillis < earliestWakeupTimeElapsed) {
            return earliestWakeupTimeElapsed;
        }
        return proposedAlarmTimeElapsedMillis;
    }

    private void updateAlarmWithListenerLocked(String tag, OnAlarmListener listener,
            long alarmTimeElapsed, WorkSource ws) {
        ensureAlarmServiceLocked();
        if (alarmTimeElapsed == Long.MAX_VALUE) {
            mAlarmService.cancel(listener);
        } else {
            if (DEBUG) {
                Slog.d(TAG, "Setting " + tag + " for: " + alarmTimeElapsed);
            }
            mAlarmService.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeElapsed,
                    AlarmManager.WINDOW_HEURISTIC, 0, tag, listener, null, ws);
        }
    }

    // Job/delay expiration alarm handling

    private final OnAlarmListener mDeadlineExpiredListener = new OnAlarmListener() {
        @Override
        public void onAlarm() {
            if (DEBUG) {
                Slog.d(TAG, "Deadline-expired alarm fired");
            }
            checkExpiredDeadlinesAndResetAlarm();
        }
    };

    private final OnAlarmListener mNextDelayExpiredListener = new OnAlarmListener() {
        @Override
        public void onAlarm() {
            if (DEBUG) {
                Slog.d(TAG, "Delay-expired alarm fired");
            }
            checkExpiredDelaysAndResetAlarm();
        }
    };

    @Override
    public void dumpControllerStateLocked(IndentingPrintWriter pw,
            Predicate<JobStatus> predicate) {
        final long nowElapsed = sElapsedRealtimeClock.millis();
        pw.println("Elapsed clock: " + nowElapsed);

        pw.print("Next delay alarm in ");
        TimeUtils.formatDuration(mNextDelayExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.print("Next deadline alarm in ");
        TimeUtils.formatDuration(mNextJobExpiredElapsedMillis, nowElapsed, pw);
        pw.println();
        pw.println();

        for (JobStatus ts : mTrackedJobs) {
            if (!predicate.test(ts)) {
                continue;
            }
            pw.print("#");
            ts.printUniqueId(pw);
            pw.print(" from ");
            UserHandle.formatUid(pw, ts.getSourceUid());
            pw.print(": Delay=");
            if (ts.hasTimingDelayConstraint()) {
                TimeUtils.formatDuration(ts.getEarliestRunTime(), nowElapsed, pw);
            } else {
                pw.print("N/A");
            }
            pw.print(", Deadline=");
            if (ts.hasDeadlineConstraint()) {
                TimeUtils.formatDuration(ts.getLatestRunTimeElapsed(), nowElapsed, pw);
            } else {
                pw.print("N/A");
            }
            pw.println();
        }
    }

    @Override
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId,
            Predicate<JobStatus> predicate) {
        final long token = proto.start(fieldId);
        final long mToken = proto.start(StateControllerProto.TIME);

        final long nowElapsed = sElapsedRealtimeClock.millis();
        proto.write(StateControllerProto.TimeController.NOW_ELAPSED_REALTIME, nowElapsed);
        proto.write(StateControllerProto.TimeController.TIME_UNTIL_NEXT_DELAY_ALARM_MS,
                mNextDelayExpiredElapsedMillis - nowElapsed);
        proto.write(StateControllerProto.TimeController.TIME_UNTIL_NEXT_DEADLINE_ALARM_MS,
                mNextJobExpiredElapsedMillis - nowElapsed);

        for (JobStatus ts : mTrackedJobs) {
            if (!predicate.test(ts)) {
                continue;
            }
            final long tsToken = proto.start(StateControllerProto.TimeController.TRACKED_JOBS);
            ts.writeToShortProto(proto, StateControllerProto.TimeController.TrackedJob.INFO);

            proto.write(StateControllerProto.TimeController.TrackedJob.HAS_TIMING_DELAY_CONSTRAINT,
                    ts.hasTimingDelayConstraint());
            proto.write(StateControllerProto.TimeController.TrackedJob.DELAY_TIME_REMAINING_MS,
                    ts.getEarliestRunTime() - nowElapsed);

            proto.write(StateControllerProto.TimeController.TrackedJob.HAS_DEADLINE_CONSTRAINT,
                    ts.hasDeadlineConstraint());
            proto.write(StateControllerProto.TimeController.TrackedJob.TIME_REMAINING_UNTIL_DEADLINE_MS,
                    ts.getLatestRunTimeElapsed() - nowElapsed);

            proto.end(tsToken);
        }

        proto.end(mToken);
        proto.end(token);
    }

    //
    //  ----------------- PowerController helper -- low power Feature BEG----------------------
    //

    private final PowerControllerHelper mPowerControllerHelper;

    // impletement functions that need for PowerController
    private final class PowerControllerHelper
        extends AppIdleHelper.Listener {

        private PowerController.LocalService mPowerControllerInternal;
        private boolean mAppIdleListenerRegistered = false;

        public PowerControllerHelper() {
        }

        // return true: constraint is satisfied
        //   false: is not satisfied
        public boolean evaluateConstraint(JobStatus job) {
            if (job == null) return true;

            if (!job.isAppNotIdleSatisfied()) {
                if (DEBUG) Slog.d(TAG, "evaluateConstraint job for: " + job.getSourcePackageName()
                    + " sourceUid:" + job.getSourceUid() + " is idle!!");
                return false;
            }

            if (job.hasAppStartedConstraint()
                && !job.isAppStartedConstraintSatisfied()) {
                Slog.d(TAG, "evaluateConstraint job for: " + job.getSourcePackageName()
                    + " sourceUid:" + job.getSourceUid() + " app is not started!!");
                return false;
            }

            checkAndRegisterAppIdleListener();

            return true;
        }


        @Override
        public void onAppIdleStateChanged(String packageName, int uid, boolean idle) {
            if (DEBUG) Slog.d(TAG, "App: " + packageName + " is idle:" + idle);
            if (packageName == null) return;

            synchronized (mLock) {
                final long nowElapsedMillis = SystemClock.elapsedRealtime();
                long nextDelayTime = Long.MAX_VALUE;
                int nextDelayUid = 0;
                String nextDelayPackageName = null;
                boolean ready = false;
                Iterator<JobStatus> it = mTrackedJobs.iterator();
                while (it.hasNext()) {
                    final JobStatus job = it.next();

                    if (packageName != null
                        && !packageName.equals(job.getSourcePackageName())) {
                        continue;
                    }

                    if (DEBUG) Slog.d(TAG, "onAppIdleStateChanged: setAppNotIdleConstraintRequired: "
                        + idle + " job:" + job + " for: " + job.getSourcePackageName() + " sourceUid:" + job.getSourceUid());

                    job.setAppNotIdleConstraintRequired(idle);

                    if (idle) {
                        continue;
                    }

                    if (!job.hasTimingDelayConstraint()) {
                        continue;
                    }

                    if (evaluateTimingDelayConstraint(job, nowElapsedMillis)) {
                        if (canStopTrackingJobLocked(job)) {
                            if (DEBUG) Slog.d(TAG, "onAppIdleStateChanged job: "
                                + job + " for: " + job.getSourcePackageName() + " sourceUid:" + job.getSourceUid()
                                + " is remove");
                            it.remove();
                        }
                        if (job.isReady()) {
                            if (DEBUG) Slog.d(TAG, "onAppIdleStateChangedjob: "
                                + job + " for: " + job.getSourcePackageName() + " sourceUid:" + job.getSourceUid()
                                + " is Ready");
                            ready = true;
                        }
                    } else if (!job.isConstraintSatisfied(JobStatus.CONSTRAINT_TIMING_DELAY)) {
                        // If this job still doesn't have its delay constraint satisfied,
                        // then see if it is the next upcoming delay time for the alarm.
                        final long jobDelayTime = job.getEarliestRunTime();
                        if (nextDelayTime > jobDelayTime) {
                            nextDelayTime = jobDelayTime;
                            nextDelayUid = job.getSourceUid();
                            nextDelayPackageName = job.getSourcePackageName();
                        }
                    }
                }

                if (ready) {
                    mStateChangedListener.onControllerStateChanged();
                }

                if (DEBUG) Slog.d(TAG, "onAppIdleStateChangedjob: setDelayExpiredAlarmLocked: nextDelayTime:"
                    + nextDelayTime + " mNextDelayExpiredElapsedMillis:" + mNextDelayExpiredElapsedMillis
                    + " sourceUid:" + nextDelayUid);

                setDelayExpiredAlarmLocked(nextDelayTime,
                        deriveWorkSource(nextDelayUid, nextDelayPackageName));

            }

        }


        private boolean checkAndRegisterAppIdleListener() {
            if (mAppIdleListenerRegistered) return true;

            if (mPowerControllerInternal == null) {
                mPowerControllerInternal = LocalServices.getService(PowerController.LocalService.class);
            }

            if (mPowerControllerInternal != null) {
                if (mPowerControllerInternal.addAppIdleListener(this)) {
                    Slog.d(TAG, "checkAndRegisterAppIdleListener: success!");
                    mAppIdleListenerRegistered = true;
                }
            }
            return mAppIdleListenerRegistered;
        }
    }
    //
    //  ----------------- PowerController helper -- low power Feature END----------------------
    //

}
