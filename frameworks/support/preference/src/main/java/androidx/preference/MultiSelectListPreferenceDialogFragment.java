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

package androidx.preference;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.internal.AbstractMultiSelectListPreference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MultiSelectListPreferenceDialogFragment extends PreferenceDialogFragment {

    private static final String SAVE_STATE_VALUES =
            "MultiSelectListPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED =
            "MultiSelectListPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES =
            "MultiSelectListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "MultiSelectListPreferenceDialogFragment.entryValues";

    private Set<String> mNewValues = new HashSet<>();
    private boolean mPreferenceChanged;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public static MultiSelectListPreferenceDialogFragment newInstance(String key) {
        final MultiSelectListPreferenceDialogFragment fragment =
                new MultiSelectListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final AbstractMultiSelectListPreference preference = getListPreference();

            // UNISOC: May be a nullable result from getListPreference.
            if (preference != null) {
                if (preference.getEntries() == null || preference.getEntryValues() == null) {
                    throw new IllegalStateException(
                            "MultiSelectListPreference requires an entries array and "
                                    + "an entryValues array.");
                }

                mNewValues.clear();
                mNewValues.addAll(preference.getValues());
                mPreferenceChanged = false;
                mEntries = preference.getEntries();
                mEntryValues = preference.getEntryValues();
            } else {
                // A useless dialog will be shown. Should we dismiss it?
                android.util.Log.w("MultiSelectListPreferenceDialogFragment",
                        "Can not find the preference in this "
                                + "MultiSelectListPreferenceDialogFragment"
                                + ", maybe you have call removePreference before showing. \n"
                                + "Dismiss invalid dialog");
                dismissAllowingStateLoss();
                return;
            }
        } else {
            mNewValues.clear();
            mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES));
            mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    private AbstractMultiSelectListPreference getListPreference() {
        return (AbstractMultiSelectListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        final int entryCount = mEntryValues.length;
        final boolean[] checkedItems = new boolean[entryCount];
        for (int i = 0; i < entryCount; i++) {
            checkedItems[i] = mNewValues.contains(mEntryValues[i].toString());
        }
        builder.setMultiChoiceItems(mEntries, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            mPreferenceChanged |= mNewValues.add(
                                    mEntryValues[which].toString());
                        } else {
                            mPreferenceChanged |= mNewValues.remove(
                                    mEntryValues[which].toString());
                        }
                    }
                });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final AbstractMultiSelectListPreference preference = getListPreference();
        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (preference.callChangeListener(values)) {
                preference.setValues(values);
            }
        }
        mPreferenceChanged = false;
    }
}
