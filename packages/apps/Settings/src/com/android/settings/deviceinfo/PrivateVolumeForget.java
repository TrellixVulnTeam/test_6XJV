/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class PrivateVolumeForget extends SettingsPreferenceFragment {
    private static final String TAG_FORGET_CONFIRM = "forget_confirm";

    private VolumeRecord mRecord;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final StorageManager storage = getActivity().getSystemService(StorageManager.class);
        String fsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
        if (fsUuid == null) {
            fsUuid = getIntent().getStringExtra(VolumeRecord.EXTRA_FS_UUID);
        }
        // Passing null will crash the StorageManager, so let's early exit.
        Log.d(TAG_FORGET_CONFIRM, "fsUuid:" + fsUuid);
        if (fsUuid == null) {
            getActivity().finish();
            return null;
        }

        mRecord = storage.findRecordByUuid(fsUuid);
        if (mRecord == null) {
            getActivity().finish();
            return null;
        }

        final View view = inflater.inflate(R.layout.storage_internal_forget, container, false);
        final TextView body = (TextView) view.findViewById(R.id.body);
        final Button confirm = (Button) view.findViewById(R.id.confirm);

        body.setText(TextUtils.expandTemplate(getText(R.string.storage_internal_forget_details),
                mRecord.getNickname()));
        confirm.setOnClickListener(mConfirmListener);

        return view;
    }

    private final OnClickListener mConfirmListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            ForgetConfirmFragment.show(PrivateVolumeForget.this, mRecord.getFsUuid());
        }
    };

    public static class ForgetConfirmFragment extends InstrumentedDialogFragment {

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_VOLUME_FORGET;
        }

        public static void show(Fragment parent, String fsUuid) {
            final Bundle args = new Bundle();
            args.putString(VolumeRecord.EXTRA_FS_UUID, fsUuid);

            final ForgetConfirmFragment dialog = new ForgetConfirmFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_FORGET_CONFIRM);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager storage = context.getSystemService(StorageManager.class);

            final String fsUuid = getArguments().getString(VolumeRecord.EXTRA_FS_UUID);
            final VolumeRecord record = storage.findRecordByUuid(fsUuid);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(TextUtils.expandTemplate(
                    getText(R.string.storage_internal_forget_confirm_title), record.getNickname()));
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_internal_forget_confirm), record.getNickname()));

            builder.setPositiveButton(R.string.storage_menu_forget,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    /* Bug901745: Rename sdcard after forget volume operation cause settings crash {@ */
                    boolean isAvailable = isInsertInternalSdCardExsit(storage,fsUuid);
                    Log.d(TAG_FORGET_CONFIRM, " isAvailable = " + isAvailable);
                    if (!isAvailable) {
                        storage.forgetVolume(fsUuid);
                    }
                    /* @} */
                    getActivity().finish();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();
        }
    }

    /* Bug901745: Rename sdcard after forget volume operation cause settings crash {@ */
    public static boolean isInsertInternalSdCardExsit(StorageManager mStorageManager,String fsUuid){
        boolean isExist = false;
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                Log.d(TAG_FORGET_CONFIRM, " isMounted =" + vol.isMountedReadable() + " fsUuid =" + fsUuid + " vol.getFsUuid()=" + vol.getFsUuid());
                if (vol.isMountedReadable() && fsUuid != null && fsUuid.equals(vol.getFsUuid())) {
                    isExist = true;
                }
            }
        }
        return isExist;
   }
   /* @} */
}
