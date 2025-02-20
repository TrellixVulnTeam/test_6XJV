/*
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

package com.android.documentsui.picker;

import android.app.Activity;
import android.net.Uri;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.PairedTask;
import com.android.documentsui.base.State;
import com.android.documentsui.PlugInDrm.DocumentsUIPlugInDrm;
import android.util.Log;
class SetLastAccessedStackTask extends PairedTask<Activity, Void, Void> {

    private final LastAccessedStorage mLastAccessed;
    private final DocumentStack mStack;
    private final Runnable mCallback;
    private int check_ret;
    private State mState;
    private PickActivity  mActivity;
    private Uri[] mDocs;
    private static final String TAG = "SetLastAccessedStackTask";
    SetLastAccessedStackTask(
            Activity activity,
            Uri[] docs,
            LastAccessedStorage lastAccessed,
            DocumentStack stack,
            Runnable callback) {

        super(activity);
        mLastAccessed = lastAccessed;
        mStack = stack;
        mCallback = callback;
        mActivity =(PickActivity) activity;
        mState = mActivity.getDisplayState();
        mDocs = docs;
    }

    @Override
    protected Void run(Void... params) {
        mLastAccessed.setLastAccessed(mOwner, mStack);
        /*
         * Add for documentui_DRM
         *@{
         */
        try {
            if (mState.action == State.ACTION_OPEN || mState.action == State.ACTION_GET_CONTENT) {
                check_ret = DocumentsUIPlugInDrm.getInstance().checkDrmError(mActivity, mDocs, mState.acceptMimes);
            }
        } catch (Exception e) {
            Log.d(TAG, "checkDrmError error: ", e);
        }
        /*@}*/
        return null;
    }

    @Override
    protected void finish(Void result) {
        /*
         * Add for documentui_DRM
         *@{
         */
        if (mState.action == State.ACTION_OPEN || mState.action == State.ACTION_GET_CONTENT) {
            if (DocumentsUIPlugInDrm.getInstance().alertDrmError(check_ret)){
                return;
            }
        }
        /*@}*/
        mCallback.run();
    }
}
