/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.ExceptionUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseIntArray;

import com.android.internal.os.BinderCallsStats;
import com.android.internal.os.BinderInternal;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.FunctionalUtils.ThrowingSupplier;

import libcore.io.IoUtils;
import libcore.util.NativeAllocationRegistry;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for a remotable object, the core part of a lightweight
 * remote procedure call mechanism defined by {@link IBinder}.
 * This class is an implementation of IBinder that provides
 * standard local implementation of such an object.
 *
 * <p>Most developers will not implement this class directly, instead using the
 * <a href="{@docRoot}guide/components/aidl.html">aidl</a> tool to describe the desired
 * interface, having it generate the appropriate Binder subclass.  You can,
 * however, derive directly from Binder to implement your own custom RPC
 * protocol or simply instantiate a raw Binder object directly to use as a
 * token that can be shared across processes.
 *
 * <p>This class is just a basic IPC primitive; it has no impact on an application's
 * lifecycle, and is valid only as long as the process that created it continues to run.
 * To use this correctly, you must be doing so within the context of a top-level
 * application component (a {@link android.app.Service}, {@link android.app.Activity},
 * or {@link android.content.ContentProvider}) that lets the system know your process
 * should remain running.</p>
 *
 * <p>You must keep in mind the situations in which your process
 * could go away, and thus require that you later re-create a new Binder and re-attach
 * it when the process starts again.  For example, if you are using this within an
 * {@link android.app.Activity}, your activity's process may be killed any time the
 * activity is not started; if the activity is later re-created you will need to
 * create a new Binder and hand it back to the correct place again; you need to be
 * aware that your process may be started for another reason (for example to receive
 * a broadcast) that will not involve re-creating the activity and thus run its code
 * to create a new Binder.</p>
 *
 * @see IBinder
 */
public class Binder implements IBinder {
    /*
     * Set this flag to true to detect anonymous, local or member classes
     * that extend this Binder class and that are not static. These kind
     * of classes can potentially create leaks.
     */
    private static final boolean FIND_POTENTIAL_LEAKS = false;
    /** @hide */
    public static final boolean CHECK_PARCEL_SIZE = false;
    static final String TAG = "Binder";

    /** @hide */
    public static boolean LOG_RUNTIME_EXCEPTION = false; // DO NOT SUBMIT WITH TRUE

    /**
     * Control whether dump() calls are allowed.
     */
    private static volatile String sDumpDisabled = null;

    private static IBinder gSecurityService = null;

    /**
     * Global transaction tracker instance for this process.
     */
    private static volatile TransactionTracker sTransactionTracker = null;

    /**
     * Guestimate of native memory associated with a Binder.
     */
    private static final int NATIVE_ALLOCATION_SIZE = 500;

    private static native long getNativeFinalizer();

    // Use a Holder to allow static initialization of Binder in the boot image, and
    // possibly to avoid some initialization ordering issues.
    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(
                Binder.class.getClassLoader(), getNativeFinalizer(), NATIVE_ALLOCATION_SIZE);
    }

    // Transaction tracking code.

    /**
     * Flag indicating whether we should be tracing transact calls.
     */
    private static volatile boolean sTracingEnabled = false;

    /**
     * Enable Binder IPC tracing.
     *
     * @hide
     */
    public static void enableTracing() {
        sTracingEnabled = true;
    }

    /**
     * Disable Binder IPC tracing.
     *
     * @hide
     */
    public static void disableTracing() {
        sTracingEnabled = false;
    }

    /**
     * Check if binder transaction tracing is enabled.
     *
     * @hide
     */
    public static boolean isTracingEnabled() {
        return sTracingEnabled;
    }

    /**
     * Get the binder transaction tracker for this process.
     *
     * @hide
     */
    public synchronized static TransactionTracker getTransactionTracker() {
        if (sTransactionTracker == null)
            sTransactionTracker = new TransactionTracker();
        return sTransactionTracker;
    }

    /** {@hide} */
    static volatile boolean sWarnOnBlocking = false;

    /**
     * Warn if any blocking binder transactions are made out from this process.
     * This is typically only useful for the system process, to prevent it from
     * blocking on calls to external untrusted code. Instead, all outgoing calls
     * that require a result must be sent as {@link IBinder#FLAG_ONEWAY} calls
     * which deliver results through a callback interface.
     *
     * @hide
     */
    public static void setWarnOnBlocking(boolean warnOnBlocking) {
        sWarnOnBlocking = warnOnBlocking;
    }

    /**
     * Allow blocking calls on the given interface, overriding the requested
     * value of {@link #setWarnOnBlocking(boolean)}.
     * <p>
     * This should only be rarely called when you are <em>absolutely sure</em>
     * the remote interface is a built-in system component that can never be
     * upgraded. In particular, this <em>must never</em> be called for
     * interfaces hosted by package that could be upgraded or replaced,
     * otherwise you risk system instability if that remote interface wedges.
     *
     * @hide
     */
    public static IBinder allowBlocking(IBinder binder) {
        try {
            if (binder instanceof BinderProxy) {
                ((BinderProxy) binder).mWarnOnBlocking = false;
            } else if (binder != null && binder.getInterfaceDescriptor() != null
                    && binder.queryLocalInterface(binder.getInterfaceDescriptor()) == null) {
                Log.w(TAG, "Unable to allow blocking on interface " + binder);
            }
        } catch (RemoteException ignored) {
        }
        return binder;
    }

    /**
     * Reset the given interface back to the default blocking behavior,
     * reverting any changes made by {@link #allowBlocking(IBinder)}.
     *
     * @hide
     */
    public static IBinder defaultBlocking(IBinder binder) {
        if (binder instanceof BinderProxy) {
            ((BinderProxy) binder).mWarnOnBlocking = sWarnOnBlocking;
        }
        return binder;
    }

    /**
     * Inherit the current {@link #allowBlocking(IBinder)} value from one given
     * interface to another.
     *
     * @hide
     */
    public static void copyAllowBlocking(IBinder fromBinder, IBinder toBinder) {
        if (fromBinder instanceof BinderProxy && toBinder instanceof BinderProxy) {
            ((BinderProxy) toBinder).mWarnOnBlocking = ((BinderProxy) fromBinder).mWarnOnBlocking;
        }
    }

    /**
     * Raw native pointer to JavaBBinderHolder object. Owned by this Java object. Not null.
     */
    private final long mObject;

    private IInterface mOwner;
    private String mDescriptor;

    /**
     * Return the ID of the process that sent you the current transaction
     * that is being processed.  This pid can be used with higher-level
     * system services to determine its identity and check permissions.
     * If the current thread is not currently executing an incoming transaction,
     * then its own pid is returned.
     */
    public static final native int getCallingPid();

    /**
     * Return the Linux uid assigned to the process that sent you the
     * current transaction that is being processed.  This uid can be used with
     * higher-level system services to determine its identity and check
     * permissions.  If the current thread is not currently executing an
     * incoming transaction, then its own uid is returned.
     */
    public static final native int getCallingUid();

    /**
     * Return the UserHandle assigned to the process that sent you the
     * current transaction that is being processed.  This is the user
     * of the caller.  It is distinct from {@link #getCallingUid()} in that a
     * particular user will have multiple distinct apps running under it each
     * with their own uid.  If the current thread is not currently executing an
     * incoming transaction, then its own UserHandle is returned.
     */
    public static final @NonNull UserHandle getCallingUserHandle() {
        return UserHandle.of(UserHandle.getUserId(getCallingUid()));
    }
    /**
     * @hide
     */
    public static int  dojudge(int uid,  String name, int oprID, int    oprType, String param) {
        if (gSecurityService == null) {
            gSecurityService = ServiceManager.getService("security") ;
        }
        Parcel data =  Parcel.obtain() ;
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken("android.os.ISecurityService");
        data.writeInt(uid);
        data.writeString(name);
        data.writeInt(oprID);
        data.writeInt(oprType);
        data.writeString(param);
        try {
            gSecurityService.transact(3, data, reply, 0);
            reply.readExceptionCode();
        }
        catch (android.os.RemoteException e)
        {
        }
        return reply.readInt();
    }
    /**
     * Reset the identity of the incoming IPC on the current thread.  This can
     * be useful if, while handling an incoming call, you will be calling
     * on interfaces of other objects that may be local to your process and
     * need to do permission checks on the calls coming into them (so they
     * will check the permission of your own local process, and not whatever
     * process originally called you).
     *
     * @return Returns an opaque token that can be used to restore the
     * original calling identity by passing it to
     * {@link #restoreCallingIdentity(long)}.
     *
     * @see #getCallingPid()
     * @see #getCallingUid()
     * @see #restoreCallingIdentity(long)
     */
    public static final native long clearCallingIdentity();

    /**
     * Restore the identity of the incoming IPC on the current thread
     * back to a previously identity that was returned by {@link
     * #clearCallingIdentity}.
     *
     * @param token The opaque token that was previously returned by
     * {@link #clearCallingIdentity}.
     *
     * @see #clearCallingIdentity
     */
    public static final native void restoreCallingIdentity(long token);

    /**
     * Convenience method for running the provided action enclosed in
     * {@link #clearCallingIdentity}/{@link #restoreCallingIdentity}
     *
     * Any exception thrown by the given action will be caught and rethrown after the call to
     * {@link #restoreCallingIdentity}
     *
     * @hide
     */
    public static final void withCleanCallingIdentity(@NonNull ThrowingRunnable action) {
        long callingIdentity = clearCallingIdentity();
        Throwable throwableToPropagate = null;
        try {
            action.runOrThrow();
        } catch (Throwable throwable) {
            throwableToPropagate = throwable;
        } finally {
            restoreCallingIdentity(callingIdentity);
            if (throwableToPropagate != null) {
                throw ExceptionUtils.propagate(throwableToPropagate);
            }
        }
    }

    /**
     * Convenience method for running the provided action enclosed in
     * {@link #clearCallingIdentity}/{@link #restoreCallingIdentity} returning the result
     *
     * Any exception thrown by the given action will be caught and rethrown after the call to
     * {@link #restoreCallingIdentity}
     *
     * @hide
     */
    public static final <T> T withCleanCallingIdentity(@NonNull ThrowingSupplier<T> action) {
        long callingIdentity = clearCallingIdentity();
        Throwable throwableToPropagate = null;
        try {
            return action.getOrThrow();
        } catch (Throwable throwable) {
            throwableToPropagate = throwable;
            return null; // overridden by throwing in finally block
        } finally {
            restoreCallingIdentity(callingIdentity);
            if (throwableToPropagate != null) {
                throw ExceptionUtils.propagate(throwableToPropagate);
            }
        }
    }

    /**
     * Sets the native thread-local StrictMode policy mask.
     *
     * <p>The StrictMode settings are kept in two places: a Java-level
     * threadlocal for libcore/Dalvik, and a native threadlocal (set
     * here) for propagation via Binder calls.  This is a little
     * unfortunate, but necessary to break otherwise more unfortunate
     * dependencies either of Dalvik on Android, or Android
     * native-only code on Dalvik.
     *
     * @see StrictMode
     * @hide
     */
    public static final native void setThreadStrictModePolicy(int policyMask);

    /**
     * Gets the current native thread-local StrictMode policy mask.
     *
     * @see #setThreadStrictModePolicy
     * @hide
     */
    public static final native int getThreadStrictModePolicy();

    /**
     * Flush any Binder commands pending in the current thread to the kernel
     * driver.  This can be
     * useful to call before performing an operation that may block for a long
     * time, to ensure that any pending object references have been released
     * in order to prevent the process from holding on to objects longer than
     * it needs to.
     */
    public static final native void flushPendingCommands();

    /**
     * Add the calling thread to the IPC thread pool.  This function does
     * not return until the current process is exiting.
     */
    public static final void joinThreadPool() {
        BinderInternal.joinThreadPool();
    }

    /**
     * Returns true if the specified interface is a proxy.
     * @hide
     */
    public static final boolean isProxy(IInterface iface) {
        return iface.asBinder() != iface;
    }

    /**
     * Call blocks until the number of executing binder threads is less
     * than the maximum number of binder threads allowed for this process.
     * @hide
     */
    public static final native void blockUntilThreadAvailable();

    /**
     * Default constructor initializes the object.
     */
    public Binder() {
        mObject = getNativeBBinderHolder();
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, mObject);

        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Binder> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Binder class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }
    }

    /**
     * Convenience method for associating a specific interface with the Binder.
     * After calling, queryLocalInterface() will be implemented for you
     * to return the given owner IInterface when the corresponding
     * descriptor is requested.
     */
    public void attachInterface(@Nullable IInterface owner, @Nullable String descriptor) {
        mOwner = owner;
        mDescriptor = descriptor;
    }

    /**
     * Default implementation returns an empty interface name.
     */
    public @Nullable String getInterfaceDescriptor() {
        return mDescriptor;
    }

    /**
     * Default implementation always returns true -- if you got here,
     * the object is alive.
     */
    public boolean pingBinder() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * Note that if you're calling on a local binder, this always returns true
     * because your process is alive if you're calling it.
     */
    public boolean isBinderAlive() {
        return true;
    }

    /**
     * Use information supplied to attachInterface() to return the
     * associated IInterface if it matches the requested
     * descriptor.
     */
    public @Nullable IInterface queryLocalInterface(@NonNull String descriptor) {
        if (mDescriptor != null && mDescriptor.equals(descriptor)) {
            return mOwner;
        }
        return null;
    }

    /**
     * Control disabling of dump calls in this process.  This is used by the system
     * process watchdog to disable incoming dump calls while it has detecting the system
     * is hung and is reporting that back to the activity controller.  This is to
     * prevent the controller from getting hung up on bug reports at this point.
     * @hide
     *
     * @param msg The message to show instead of the dump; if null, dumps are
     * re-enabled.
     */
    public static void setDumpDisabled(String msg) {
        sDumpDisabled = msg;
    }

    /**
     * Default implementation is a stub that returns false.  You will want
     * to override this to do the appropriate unmarshalling of transactions.
     *
     * <p>If you want to call this, call transact().
     *
     * <p>Implementations that are returning a result should generally use
     * {@link Parcel#writeNoException() Parcel.writeNoException} and
     * {@link Parcel#writeException(Exception) Parcel.writeException} to propagate
     * exceptions back to the caller.</p>
     *
     * @param code The action to perform.  This should
     * be a number between {@link #FIRST_CALL_TRANSACTION} and
     * {@link #LAST_CALL_TRANSACTION}.
     * @param data Marshalled data being received from the caller.
     * @param reply If the caller is expecting a result back, it should be marshalled
     * in to here.
     * @param flags Additional operation flags.  Either 0 for a normal
     * RPC, or {@link #FLAG_ONEWAY} for a one-way RPC.
     *
     * @return Return true on a successful call; returning false is generally used to
     * indicate that you did not understand the transaction code.
     */
    protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply,
            int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(getInterfaceDescriptor());
            return true;
        } else if (code == DUMP_TRANSACTION) {
            ParcelFileDescriptor fd = data.readFileDescriptor();
            String[] args = data.readStringArray();
            if (fd != null) {
                try {
                    dump(fd.getFileDescriptor(), args);
                } finally {
                    IoUtils.closeQuietly(fd);
                }
            }
            // Write the StrictMode header.
            if (reply != null) {
                reply.writeNoException();
            } else {
                StrictMode.clearGatheredViolations();
            }
            return true;
        } else if (code == SHELL_COMMAND_TRANSACTION) {
            ParcelFileDescriptor in = data.readFileDescriptor();
            ParcelFileDescriptor out = data.readFileDescriptor();
            ParcelFileDescriptor err = data.readFileDescriptor();
            String[] args = data.readStringArray();
            ShellCallback shellCallback = ShellCallback.CREATOR.createFromParcel(data);
            ResultReceiver resultReceiver = ResultReceiver.CREATOR.createFromParcel(data);
            try {
                if (out != null) {
                    shellCommand(in != null ? in.getFileDescriptor() : null,
                            out.getFileDescriptor(),
                            err != null ? err.getFileDescriptor() : out.getFileDescriptor(),
                            args, shellCallback, resultReceiver);
                }
            } finally {
                IoUtils.closeQuietly(in);
                IoUtils.closeQuietly(out);
                IoUtils.closeQuietly(err);
                // Write the StrictMode header.
                if (reply != null) {
                    reply.writeNoException();
                } else {
                    StrictMode.clearGatheredViolations();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Implemented to call the more convenient version
     * {@link #dump(FileDescriptor, PrintWriter, String[])}.
     */
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) {
        FileOutputStream fout = new FileOutputStream(fd);
        PrintWriter pw = new FastPrintWriter(fout);
        try {
            doDump(fd, pw, args);
        } finally {
            pw.flush();
        }
    }

    void doDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final String disabled = sDumpDisabled;
        if (disabled == null) {
            try {
                dump(fd, pw, args);
            } catch (SecurityException e) {
                pw.println("Security exception: " + e.getMessage());
                throw e;
            } catch (Throwable e) {
                // Unlike usual calls, in this case if an exception gets thrown
                // back to us we want to print it back in to the dump data, since
                // that is where the caller expects all interesting information to
                // go.
                pw.println();
                pw.println("Exception occurred while dumping:");
                e.printStackTrace(pw);
            }
        } else {
            pw.println(sDumpDisabled);
        }
    }

    /**
     * Like {@link #dump(FileDescriptor, String[])}, but ensures the target
     * executes asynchronously.
     */
    public void dumpAsync(@NonNull final FileDescriptor fd, @Nullable final String[] args) {
        final FileOutputStream fout = new FileOutputStream(fd);
        final PrintWriter pw = new FastPrintWriter(fout);
        Thread thr = new Thread("Binder.dumpAsync") {
            public void run() {
                try {
                    dump(fd, pw, args);
                } finally {
                    pw.flush();
                }
            }
        };
        thr.start();
    }

    /**
     * Print the object's state into the given stream.
     *
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param fout The file to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    protected void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter fout,
            @Nullable String[] args) {
    }

    /**
     * @param in The raw file descriptor that an input data stream can be read from.
     * @param out The raw file descriptor that normal command messages should be written to.
     * @param err The raw file descriptor that command error messages should be written to.
     * @param args Command-line arguments.
     * @param callback Callback through which to interact with the invoking shell.
     * @param resultReceiver Called when the command has finished executing, with the result code.
     * @throws RemoteException
     * @hide
     */
    public void shellCommand(@Nullable FileDescriptor in, @Nullable FileDescriptor out,
            @Nullable FileDescriptor err,
            @NonNull String[] args, @Nullable ShellCallback callback,
            @NonNull ResultReceiver resultReceiver) throws RemoteException {
        onShellCommand(in, out, err, args, callback, resultReceiver);
    }

    /**
     * Handle a call to {@link #shellCommand}.  The default implementation simply prints
     * an error message.  Override and replace with your own.
     * <p class="caution">Note: no permission checking is done before calling this method; you must
     * apply any security checks as appropriate for the command being executed.
     * Consider using {@link ShellCommand} to help in the implementation.</p>
     * @hide
     */
    public void onShellCommand(@Nullable FileDescriptor in, @Nullable FileDescriptor out,
            @Nullable FileDescriptor err,
            @NonNull String[] args, @Nullable ShellCallback callback,
            @NonNull ResultReceiver resultReceiver) throws RemoteException {
        FileOutputStream fout = new FileOutputStream(err != null ? err : out);
        PrintWriter pw = new FastPrintWriter(fout);
        pw.println("No shell command implementation.");
        pw.flush();
        resultReceiver.send(0, null);
    }

    /**
     * Default implementation rewinds the parcels and calls onTransact.  On
     * the remote side, transact calls into the binder to do the IPC.
     */
    public final boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply,
            int flags) throws RemoteException {
        if (false) Log.v("Binder", "Transact: " + code + " to " + this);

        if (data != null) {
            data.setDataPosition(0);
        }
        boolean r = onTransact(code, data, reply, flags);
        if (reply != null) {
            reply.setDataPosition(0);
        }
        return r;
    }

    /**
     * Local implementation is a no-op.
     */
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) {
    }

    /**
     * Local implementation is a no-op.
     */
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return true;
    }

    static void checkParcel(IBinder obj, int code, Parcel parcel, String msg) {
        if (CHECK_PARCEL_SIZE && parcel.dataSize() >= 800*1024) {
            // Trying to send > 800k, this is way too much
            StringBuilder sb = new StringBuilder();
            sb.append(msg);
            sb.append(": on ");
            sb.append(obj);
            sb.append(" calling ");
            sb.append(code);
            sb.append(" size ");
            sb.append(parcel.dataSize());
            sb.append(" (data: ");
            parcel.setDataPosition(0);
            sb.append(parcel.readInt());
            sb.append(", ");
            sb.append(parcel.readInt());
            sb.append(", ");
            sb.append(parcel.readInt());
            sb.append(")");
            Slog.wtfStack(TAG, sb.toString());
        }
    }

    private static native long getNativeBBinderHolder();
    private static native long getFinalizer();

    // Entry point from android_util_Binder.cpp's onTransact
    private boolean execTransact(int code, long dataObj, long replyObj,
            int flags) {
        BinderCallsStats binderCallsStats = BinderCallsStats.getInstance();
        BinderCallsStats.CallSession callSession = binderCallsStats.callStarted(this, code);
        Parcel data = Parcel.obtain(dataObj);
        Parcel reply = Parcel.obtain(replyObj);
        // theoretically, we should call transact, which will call onTransact,
        // but all that does is rewind it, and we just got these from an IPC,
        // so we'll just call it directly.
        boolean res;
        // Log any exceptions as warnings, don't silently suppress them.
        // If the call was FLAG_ONEWAY then these exceptions disappear into the ether.
        final boolean tracingEnabled = Binder.isTracingEnabled();
        try {
            if (tracingEnabled) {
                Trace.traceBegin(Trace.TRACE_TAG_ALWAYS, getClass().getName() + ":" + code);
            }
            res = onTransact(code, data, reply, flags);
        } catch (RemoteException|RuntimeException e) {
            if (LOG_RUNTIME_EXCEPTION) {
                Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
            }
            if ((flags & FLAG_ONEWAY) != 0) {
                if (e instanceof RemoteException) {
                    Log.w(TAG, "Binder call failed.", e);
                } else {
                    Log.w(TAG, "Caught a RuntimeException from the binder stub implementation.", e);
                }
            } else {
                // Clear the parcel before writing the exception
                reply.setDataSize(0);
                reply.setDataPosition(0);
                reply.writeException(e);
            }
            res = true;
        } finally {
            if (tracingEnabled) {
                Trace.traceEnd(Trace.TRACE_TAG_ALWAYS);
            }
        }
        checkParcel(this, code, reply, "Unreasonably large binder reply buffer");
        reply.recycle();
        data.recycle();

        // Just in case -- we are done with the IPC, so there should be no more strict
        // mode violations that have gathered for this thread.  Either they have been
        // parceled and are now in transport off to the caller, or we are returning back
        // to the main transaction loop to wait for another incoming transaction.  Either
        // way, strict mode begone!
        StrictMode.clearGatheredViolations();
        binderCallsStats.callEnded(callSession);

        return res;
    }
}

/**
 * Java proxy for a native IBinder object.
 * Allocated and constructed by the native javaObjectforIBinder function. Never allocated
 * directly from Java code.
 */
final class BinderProxy implements IBinder {
    // See android_util_Binder.cpp for the native half of this.

    // Assume the process-wide default value when created
    volatile boolean mWarnOnBlocking = Binder.sWarnOnBlocking;

    /*
     * Map from longs to BinderProxy, retaining only a WeakReference to the BinderProxies.
     * We roll our own only because we need to lazily remove WeakReferences during accesses
     * to avoid accumulating junk WeakReference objects. WeakHashMap isn't easily usable
     * because we want weak values, not keys.
     * Our hash table is never resized, but the number of entries is unlimited;
     * performance degrades as occupancy increases significantly past MAIN_INDEX_SIZE.
     * Not thread-safe. Client ensures there's a single access at a time.
     */
    private static final class ProxyMap {
        private static final int LOG_MAIN_INDEX_SIZE = 8;
        private static final int MAIN_INDEX_SIZE = 1 <<  LOG_MAIN_INDEX_SIZE;
        private static final int MAIN_INDEX_MASK = MAIN_INDEX_SIZE - 1;
        // Debuggable builds will throw an AssertionError if the number of map entries exceeds:
        private static final int CRASH_AT_SIZE = 20_000;

        /**
         * We next warn when we exceed this bucket size.
         */
        private int mWarnBucketSize = 20;

        /**
         * Increment mWarnBucketSize by WARN_INCREMENT each time we warn.
         */
        private static final int WARN_INCREMENT = 10;

        /**
         * Hash function tailored to native pointers.
         * Returns a value < MAIN_INDEX_SIZE.
         */
        private static int hash(long arg) {
            return ((int) ((arg >> 2) ^ (arg >> (2 + LOG_MAIN_INDEX_SIZE)))) & MAIN_INDEX_MASK;
        }

        /**
         * Return the total number of pairs in the map.
         */
        private int size() {
            int size = 0;
            for (ArrayList<WeakReference<BinderProxy>> a : mMainIndexValues) {
                if (a != null) {
                    size += a.size();
                }
            }
            return size;
        }

        /**
         * Return the total number of pairs in the map containing values that have
         * not been cleared. More expensive than the above size function.
         */
        private int unclearedSize() {
            int size = 0;
            for (ArrayList<WeakReference<BinderProxy>> a : mMainIndexValues) {
                if (a != null) {
                    for (WeakReference<BinderProxy> ref : a) {
                        if (ref.get() != null) {
                            ++size;
                        }
                    }
                }
            }
            return size;
        }

        /**
         * Remove ith entry from the hash bucket indicated by hash.
         */
        private void remove(int hash, int index) {
            Long[] keyArray = mMainIndexKeys[hash];
            ArrayList<WeakReference<BinderProxy>> valueArray = mMainIndexValues[hash];
            int size = valueArray.size();  // KeyArray may have extra elements.
            // Move last entry into empty slot, and truncate at end.
            if (index != size - 1) {
                keyArray[index] = keyArray[size - 1];
                valueArray.set(index, valueArray.get(size - 1));
            }
            valueArray.remove(size - 1);
            // Just leave key array entry; it's unused. We only trust the valueArray size.
        }

        /**
         * Look up the supplied key. If we have a non-cleared entry for it, return it.
         */
        BinderProxy get(long key) {
            int myHash = hash(key);
            Long[] keyArray = mMainIndexKeys[myHash];
            if (keyArray == null) {
                return null;
            }
            ArrayList<WeakReference<BinderProxy>> valueArray = mMainIndexValues[myHash];
            int bucketSize = valueArray.size();
            for (int i = 0; i < bucketSize; ++i) {
                long foundKey = keyArray[i];
                if (key == foundKey) {
                    WeakReference<BinderProxy> wr = valueArray.get(i);
                    BinderProxy bp = wr.get();
                    if (bp != null) {
                        return bp;
                    } else {
                        remove(myHash, i);
                        return null;
                    }
                }
            }
            return null;
        }

        private int mRandom;  // A counter used to generate a "random" index. World's 2nd worst RNG.

        /**
         * Add the key-value pair to the map.
         * Requires that the indicated key is not already in the map.
         */
        void set(long key, @NonNull BinderProxy value) {
            int myHash = hash(key);
            ArrayList<WeakReference<BinderProxy>> valueArray = mMainIndexValues[myHash];
            if (valueArray == null) {
                valueArray = mMainIndexValues[myHash] = new ArrayList<>();
                mMainIndexKeys[myHash] = new Long[1];
            }
            int size = valueArray.size();
            WeakReference<BinderProxy> newWr = new WeakReference<>(value);
            // First look for a cleared reference.
            // This ensures that ArrayList size is bounded by the maximum occupancy of
            // that bucket.
            for (int i = 0; i < size; ++i) {
                if (valueArray.get(i).get() == null) {
                    valueArray.set(i, newWr);
                    Long[] keyArray = mMainIndexKeys[myHash];
                    keyArray[i] = key;
                    if (i < size - 1) {
                        // "Randomly" check one of the remaining entries in [i+1, size), so that
                        // needlessly long buckets are eventually pruned.
                        int rnd = Math.floorMod(++mRandom, size - (i + 1));
                        if (valueArray.get(i + 1 + rnd).get() == null) {
                            remove(myHash, i + 1 + rnd);
                        }
                    }
                    return;
                }
            }
            valueArray.add(size, newWr);
            Long[] keyArray = mMainIndexKeys[myHash];
            if (keyArray.length == size) {
                // size >= 1, since we initially allocated one element
                Long[] newArray = new Long[size + size / 2 + 2];
                System.arraycopy(keyArray, 0, newArray, 0, size);
                newArray[size] = key;
                mMainIndexKeys[myHash] = newArray;
            } else {
                keyArray[size] = key;
            }
            if (size >= mWarnBucketSize) {
                final int totalSize = size();
                Log.v(Binder.TAG, "BinderProxy map growth! bucket size = " + size
                        + " total = " + totalSize);
                mWarnBucketSize += WARN_INCREMENT;
                if (Build.IS_DEBUGGABLE && totalSize >= CRASH_AT_SIZE) {
                    // Use the number of uncleared entries to determine whether we should
                    // really report a histogram and crash. We don't want to fundamentally
                    // change behavior for a debuggable process, so we GC only if we are
                    // about to crash.
                    final int totalUnclearedSize = unclearedSize();
                    if (totalUnclearedSize >= CRASH_AT_SIZE) {
                        dumpProxyInterfaceCounts();
                        dumpPerUidProxyCounts();
                        Runtime.getRuntime().gc();
                        throw new AssertionError("Binder ProxyMap has too many entries: "
                                + totalSize + " (total), " + totalUnclearedSize + " (uncleared), "
                                + unclearedSize() + " (uncleared after GC). BinderProxy leak?");
                    } else if (totalSize > 3 * totalUnclearedSize / 2) {
                        Log.v(Binder.TAG, "BinderProxy map has many cleared entries: "
                                + (totalSize - totalUnclearedSize) + " of " + totalSize
                                + " are cleared");
                    }
                }
            }
        }

        /**
         * Dump a histogram to the logcat. Used to diagnose abnormally large proxy maps.
         */
        private void dumpProxyInterfaceCounts() {
            Map<String, Integer> counts = new HashMap<>();
            for (ArrayList<WeakReference<BinderProxy>> a : mMainIndexValues) {
                if (a != null) {
                    for (WeakReference<BinderProxy> weakRef : a) {
                        BinderProxy bp = weakRef.get();
                        String key;
                        if (bp == null) {
                            key = "<cleared weak-ref>";
                        } else {
                            try {
                                key = bp.getInterfaceDescriptor();
                            } catch (Throwable t) {
                                key = "<exception during getDescriptor>";
                            }
                        }
                        Integer i = counts.get(key);
                        if (i == null) {
                            counts.put(key, 1);
                        } else {
                            counts.put(key, i + 1);
                        }
                    }
                }
            }
            Map.Entry<String, Integer>[] sorted = counts.entrySet().toArray(
                    new Map.Entry[counts.size()]);
            Arrays.sort(sorted, (Map.Entry<String, Integer> a, Map.Entry<String, Integer> b)
                    -> b.getValue().compareTo(a.getValue()));
            Log.v(Binder.TAG, "BinderProxy descriptor histogram (top ten):");
            int printLength = Math.min(10, sorted.length);
            for (int i = 0; i < printLength; i++) {
                Log.v(Binder.TAG, " #" + (i + 1) + ": " + sorted[i].getKey() + " x"
                        + sorted[i].getValue());
            }
        }

        /**
         * Dump per uid binder proxy counts to the logcat.
         */
        private void dumpPerUidProxyCounts() {
            SparseIntArray counts = BinderInternal.nGetBinderProxyPerUidCounts();
            if (counts.size() == 0) return;
            Log.d(Binder.TAG, "Per Uid Binder Proxy Counts:");
            for (int i = 0; i < counts.size(); i++) {
                final int uid = counts.keyAt(i);
                final int binderCount = counts.valueAt(i);
                Log.d(Binder.TAG, "UID : " + uid + "  count = " + binderCount);
            }
        }

        // Corresponding ArrayLists in the following two arrays always have the same size.
        // They contain no empty entries. However WeakReferences in the values ArrayLists
        // may have been cleared.

        // mMainIndexKeys[i][j] corresponds to mMainIndexValues[i].get(j) .
        // The values ArrayList has the proper size(), the corresponding keys array
        // is always at least the same size, but may be larger.
        // If either a particular keys array, or the corresponding values ArrayList
        // are null, then they both are.
        private final Long[][] mMainIndexKeys = new Long[MAIN_INDEX_SIZE][];
        private final ArrayList<WeakReference<BinderProxy>>[] mMainIndexValues =
                new ArrayList[MAIN_INDEX_SIZE];
    }

    private static ProxyMap sProxyMap = new ProxyMap();

    /**
      * Dump proxy debug information.
      *
      * Note: this method is not thread-safe; callers must serialize with other
      * accesses to sProxyMap, in particular {@link #getInstance(long, long)}.
      *
      * @hide
      */
    private static void dumpProxyDebugInfo() {
        if (Build.IS_DEBUGGABLE) {
            sProxyMap.dumpProxyInterfaceCounts();
            // Note that we don't call dumpPerUidProxyCounts(); this is because this
            // method may be called as part of the uid limit being hit, and calling
            // back into the UID tracking code would cause us to try to acquire a mutex
            // that is held during that callback.
        }
    }

    /**
     * Return a BinderProxy for IBinder.
     * This method is thread-hostile!  The (native) caller serializes getInstance() calls using
     * gProxyLock.
     * If we previously returned a BinderProxy bp for the same iBinder, and bp is still
     * in use, then we return the same bp.
     *
     * @param nativeData C++ pointer to (possibly still empty) BinderProxyNativeData.
     * Takes ownership of nativeData iff <result>.mNativeData == nativeData, or if
     * we exit via an exception.  If neither applies, it's the callers responsibility to
     * recycle nativeData.
     * @param iBinder C++ pointer to IBinder. Does not take ownership of referenced object.
     */
    private static BinderProxy getInstance(long nativeData, long iBinder) {
        BinderProxy result;
        try {
            result = sProxyMap.get(iBinder);
            if (result != null) {
                return result;
            }
            result = new BinderProxy(nativeData);
        } catch (Throwable e) {
            // We're throwing an exception (probably OOME); don't drop nativeData.
            NativeAllocationRegistry.applyFreeFunction(NoImagePreloadHolder.sNativeFinalizer,
                    nativeData);
            throw e;
        }
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(result, nativeData);
        // The registry now owns nativeData, even if registration threw an exception.
        sProxyMap.set(iBinder, result);
        return result;
    }

    private BinderProxy(long nativeData) {
        mNativeData = nativeData;
    }

    /**
     * Guestimate of native memory associated with a BinderProxy.
     * This includes the underlying IBinder, associated DeathRecipientList, and KeyedVector
     * that points back to us. We guess high since it includes a GlobalRef, which
     * may be in short supply.
     */
    private static final int NATIVE_ALLOCATION_SIZE = 1000;

    // Use a Holder to allow static initialization of BinderProxy in the boot image, and
    // to avoid some initialization ordering issues.
    private static class NoImagePreloadHolder {
        public static final long sNativeFinalizer = getNativeFinalizer();
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(
                BinderProxy.class.getClassLoader(), sNativeFinalizer, NATIVE_ALLOCATION_SIZE);
    }

    public native boolean pingBinder();
    public native boolean isBinderAlive();

    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }

    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Binder.checkParcel(this, code, data, "Unreasonably large binder buffer");

        if (mWarnOnBlocking && ((flags & FLAG_ONEWAY) == 0)) {
            // For now, avoid spamming the log by disabling after we've logged
            // about this interface at least once
            mWarnOnBlocking = false;
            Log.w(Binder.TAG, "Outgoing transactions from this process must be FLAG_ONEWAY",
                    new Throwable());
        }

        final boolean tracingEnabled = Binder.isTracingEnabled();
        if (tracingEnabled) {
            final Throwable tr = new Throwable();
            Binder.getTransactionTracker().addTrace(tr);
            StackTraceElement stackTraceElement = tr.getStackTrace()[1];
            Trace.traceBegin(Trace.TRACE_TAG_ALWAYS,
                    stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName());
        }
        try {
            return transactNative(code, data, reply, flags);
        } finally {
            if (tracingEnabled) {
                Trace.traceEnd(Trace.TRACE_TAG_ALWAYS);
            }
        }
    }

    private static native long getNativeFinalizer();
    public native String getInterfaceDescriptor() throws RemoteException;
    public native boolean transactNative(int code, Parcel data, Parcel reply,
            int flags) throws RemoteException;
    public native void linkToDeath(DeathRecipient recipient, int flags)
            throws RemoteException;
    public native boolean unlinkToDeath(DeathRecipient recipient, int flags);

    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(DUMP_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(fd);
        data.writeStringArray(args);
        try {
            transact(DUMP_TRANSACTION, data, reply, FLAG_ONEWAY);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args, ShellCallback callback,
            ResultReceiver resultReceiver) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeFileDescriptor(in);
        data.writeFileDescriptor(out);
        data.writeFileDescriptor(err);
        data.writeStringArray(args);
        ShellCallback.writeToParcel(callback, data);
        resultReceiver.writeToParcel(data, 0);
        try {
            transact(SHELL_COMMAND_TRANSACTION, data, reply, 0);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static final void sendDeathNotice(DeathRecipient recipient) {
        if (false) Log.v("JavaBinder", "sendDeathNotice to " + recipient);
        try {
            recipient.binderDied();
        }
        catch (RuntimeException exc) {
            Log.w("BinderNative", "Uncaught exception from death notification",
                    exc);
        }
    }

    /**
     * C++ pointer to BinderProxyNativeData. That consists of strong pointers to the
     * native IBinder object, and a DeathRecipientList.
     */
    private final long mNativeData;
}
