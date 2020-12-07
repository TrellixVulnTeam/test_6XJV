package com.sprd.ext.unreadnotifier.email;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.sprd.ext.unreadnotifier.AppListPreference;
import com.sprd.ext.unreadnotifier.UnreadInfoManager;

import java.util.ArrayList;

/**
 * Created by SPRD on 1/25/17.
 */

public class EmailPreference extends AppListPreference {
    public EmailPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        item = UnreadInfoManager.getInstance(context).getItemByType(UnreadInfoManager.TYPE_EMAIL);
        if (item != null) {
            ArrayList<String> listValues = item.loadApps(item.mContext);
            item.setInstalledList(listValues);
            item.verifyDefaultCN(listValues, UnreadEmailItem.DEFAULT_CNAME);

            String pkgName = TextUtils.isEmpty(item.mCurrentCn) ? null
                    : ComponentName.unflattenFromString(item.mCurrentCn).getPackageName();
            setListValues(listValues, pkgName);
        }
    }

}
