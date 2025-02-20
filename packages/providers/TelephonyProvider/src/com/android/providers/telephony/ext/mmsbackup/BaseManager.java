
package com.android.providers.telephony.ext.mmsbackup;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony.Mms;

//import com.android.providers.telephony.MmsSmsDatabaseHelper;
import com.google.android.mms.pdu.PduHeaders;
import java.util.List;
import com.android.providers.telephony.ext.adapter.MmsSmsProviderAdapter;

public class BaseManager extends NotifyImpl {
    public final static String TAG = "BaseManager";
    private MmsManager mMmsManager = null;
    private SmsManager mSmsManager = null;
    Context mContext = null;

    public BaseManager(Context context) {
        mContext = context;
        BackupReceiver.setCallback(this);
    }

    protected MmsManager getMmsManager() {
        if (mMmsManager == null) {
            mMmsManager = new MmsManager();
            mMmsManager.SetCallBack(this, BaseManager.TAG);
            mMmsManager.SetParameter(MmsBackupService.TAG, getContext());
        }
        return mMmsManager;
    }

    public SmsManager getSmsManager() {
        if (mSmsManager == null) {
            mSmsManager = new SmsManager();
            mSmsManager.SetCallBack(this, BaseManager.TAG);
            mSmsManager.SetParameter(MmsBackupService.TAG, getContext());
        }
        return mSmsManager;
    }

    public Context getContext() {
        return mContext;
    }

    private boolean isEnabled(int categoryCode) {
        BackupLog.log(TAG, "isEnabled, categoryCode = " + categoryCode);
        SQLiteDatabase db = MmsSmsProviderAdapter.get(getContext()).getSQLiteDatabase();
        Cursor smsCursor = null;
        Cursor mmsCursor = null;
        try {
            boolean enableMms = false;
            boolean enableSms = false;
            if (categoryCode == SHORT_MESSAGE_CODE) {
                smsCursor = db.rawQuery("select _id from sms where type = 1 or type = 2", null);
                enableSms = (smsCursor != null && smsCursor.getCount() > 0) ? true : false;
                BackupLog.log(TAG, "enableSms = " + enableSms);
                return enableSms;
            } else if (categoryCode == MEDIA_MESSAGE_CODE) {
                mmsCursor = db.rawQuery("select _id from pdu where ( "
                        + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_INBOX + " or "
                        + Mms.MESSAGE_BOX + " = " + Mms.MESSAGE_BOX_SENT + " ) and "
                        + Mms.MESSAGE_TYPE + " != " + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND,
                        null);
                enableMms = (mmsCursor != null && mmsCursor.getCount() > 0) ? true : false;
                BackupLog.log(TAG, "enableMms = " + enableMms);
                return enableMms;
            }
        } finally {
            if (mmsCursor != null) {
                mmsCursor.close();
            }
            if (smsCursor != null) {
                smsCursor.close();
            }
        }
        return false;
    }

    private void initEnv() {
        BackupLog.log(TAG, "initEnv");
    }

    private int backup(int nValue, Object obj, List<Object> listObj) {
        BackupLog.log(TAG, "backup");
        switch (nValue) {
            case MmsBackupService.SHORT_MESSAGE_CODE:
                BackupLog.log(TAG, "backup sms");
                return getSmsManager().OnNotify(CMD_BACKUP, 0, 0, obj, listObj);
            case MmsBackupService.MEDIA_MESSAGE_CODE:
                return getMmsManager().OnNotify(CMD_BACKUP, 0, 0, obj, listObj);
            default:
                return FAILURE;
        }
    }

    private int restore(int nValue, Object obj) {
        BackupLog.log(TAG, "restore");
        switch (nValue) {
            case MmsBackupService.SHORT_MESSAGE_CODE:
                return getSmsManager().OnNotify(CMD_RESTORE, 0, 0, obj, null);
            case MmsBackupService.MEDIA_MESSAGE_CODE:
                return getMmsManager().OnNotify(CMD_RESTORE, 0, 0, obj, null);
            default:
                return FAILURE;
        }
    }

    private int cancel(int nValue) {
        BackupLog.log(TAG, "cancel nValue = " + nValue);
        switch (nValue) {
            case MmsBackupService.SHORT_MESSAGE_CODE:
                if (mSmsManager != null) {
                    getSmsManager().OnNotify(CMD_CANCEL, 0, 0, null, null);
                    // Cut off any backward callback;
                    getSmsManager().SetCallBack(new NotifyImpl(), null);
                    // Release the old one, will create a new one in the next
                    // operation;
                    mSmsManager = null;
                }
                return SUCC;
            case MmsBackupService.MEDIA_MESSAGE_CODE:
                if (mMmsManager != null) {
                    getMmsManager().OnNotify(CMD_CANCEL, 0, 0, null, null);
                    // Cut off any backward callback;
                    getMmsManager().SetCallBack(new NotifyImpl(), null);
                    // Release the old one, will create a new one in the next
                    // operation;
                    mMmsManager = null;
                }
                return SUCC;
            default:
                return FAILURE;
        }
    }

    @Override
    public int OnNotify(int nMsg, int nValue, long lValue, Object obj, List<Object> listObj) {
        switch (nMsg) {
            case CMD_CANCEL_ALL:
            {
                BackupLog.log("BackupReceiver", "===>>>Enter Cancel Flow======");
                cancel(MmsBackupService.SHORT_MESSAGE_CODE);
                cancel(MmsBackupService.MEDIA_MESSAGE_CODE);
                BackupLog.log("BackupReceiver", "===>>>Exist Cancel Flow======");
            }
            return SUCC;

            case CMD_GET_STATE:
                if (isEnabled(nValue)) {
                    ((InOutParameter) obj).setBoolean(true);
                }
                return SUCC;
            case CMD_BACKUP:
                cancel(nValue); // cancel any backup task first
                return backup(nValue, obj, listObj);
            case CMD_RESTORE:
                cancel(nValue); // cancel any restore task first
                return restore(nValue, obj);
            case CMD_CANCEL:
                return cancel(nValue);
            case CMD_INIT_ENV:
                initEnv();
                return SUCC;
                //
            case CMD_UPDATE_PROGRESS:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            case CMD_REPORT_RESUALT:
                return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
            default:
        }
        return GetCallBack().OnNotify(nMsg, nValue, lValue, obj, listObj);
    }

}
