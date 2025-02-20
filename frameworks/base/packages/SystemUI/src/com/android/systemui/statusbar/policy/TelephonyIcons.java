/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileSignalController.MobileIconGroup;
import static com.android.systemui.statusbar.policy.NetworkControllerImpl.KEEP_AOSP;

class TelephonyIcons {
    //***** Data connection icons
    static final int FLIGHT_MODE_ICON = R.drawable.stat_sys_airplane_mode;
    public static final int ICON_VOLTE = R.drawable.stat_sys_volte;
    /* UNISOC: add feature statusbar signal cluster view. @{ */
    static final int ICON_LTE = KEEP_AOSP ? R.drawable.ic_lte_mobiledata : R.drawable.stat_sys_data_fully_connected_lte_ex;
    static final int ICON_LTE_PLUS = KEEP_AOSP ? R.drawable.ic_lte_plus_mobiledata : R.drawable.stat_sys_data_fully_connected_ltep_ex;
    static final int ICON_G = KEEP_AOSP ? R.drawable.ic_g_mobiledata : R.drawable.stat_sys_data_fully_connected_g_ex;
    static final int ICON_E = KEEP_AOSP ? R.drawable.ic_e_mobiledata : R.drawable.stat_sys_data_fully_connected_e_ex;
    static final int ICON_H = KEEP_AOSP ? R.drawable.ic_h_mobiledata : R.drawable.stat_sys_data_fully_connected_h_ex;
    static final int ICON_H_PLUS = KEEP_AOSP ? R.drawable.ic_h_plus_mobiledata : R.drawable.stat_sys_data_fully_connected_hp_ex;
    static final int ICON_3G = KEEP_AOSP ? R.drawable.ic_3g_mobiledata : R.drawable.stat_sys_data_fully_connected_3g_ex;
    static final int ICON_3G_PLUS = KEEP_AOSP ? R.drawable.ic_3g_mobiledata : R.drawable.stat_sys_data_fully_connected_3gp_ex;
    static final int ICON_4G = KEEP_AOSP ? R.drawable.ic_4g_mobiledata : R.drawable.stat_sys_data_fully_connected_4g_ex;
    static final int ICON_4G_PLUS = KEEP_AOSP ? R.drawable.ic_4g_plus_mobiledata : R.drawable.stat_sys_data_fully_connected_4gp_ex;
    static final int ICON_4G_LTE = R.drawable.stat_sys_data_fully_connected_4glte_ex;
    static final int ICON_4G_LTE_PLUS = R.drawable.stat_sys_data_fully_connected_4gltep_ex;
    static final int ICON_1X = KEEP_AOSP ? R.drawable.ic_1x_mobiledata : R.drawable.stat_sys_data_fully_connected_1x_ex;
    static final int ICON_SIGNAL_ZERO = R.drawable.stat_sys_signal_0_ex;
    static final int ICON_SIGNAL_ONE = R.drawable.stat_sys_signal_1_ex;
    static final int ICON_SIGNAL_TWO = R.drawable.stat_sys_signal_2_ex;
    static final int ICON_SIGNAL_THREE = R.drawable.stat_sys_signal_3_ex;
    static final int ICON_SIGNAL_FOUR = R.drawable.stat_sys_signal_4_ex;
    static final int ICON_NO_NETWORK = R.drawable.stat_sys_signal_null_ex;
    static final int ICON_STAT_SYS_VOWIFI_POOR = R.drawable.stat_sys_vowifi_poor_ex;
    static final int ICON_STAT_SYS_VOWIFI_FAIR = R.drawable.stat_sys_vowifi_fair_ex;
    static final int ICON_STAT_SYS_VOWIFI_GOOD = R.drawable.stat_sys_vowifi_good_ex;
    static final int ICON_STAT_SYS_HD_VOICE = R.drawable.stat_sys_hd_voice;

    /* @} */
    static final MobileIconGroup CARRIER_NETWORK_CHANGE = new MobileIconGroup(
            "CARRIER_NETWORK_CHANGE",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.carrier_network_change_mode,
            0,
            false);

    static final MobileIconGroup THREE_G = new MobileIconGroup(
            "3G",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_3g,
            TelephonyIcons.ICON_3G,
            true);

    static final MobileIconGroup WFC = new MobileIconGroup(
            "WFC",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            0, 0, false);

    static final MobileIconGroup UNKNOWN = new MobileIconGroup(
            "Unknown",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            0, 0, false);

    static final MobileIconGroup E = new MobileIconGroup(
            "E",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_edge,
            TelephonyIcons.ICON_E,
            false);

    static final MobileIconGroup ONE_X = new MobileIconGroup(
            "1X",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_cdma,
            TelephonyIcons.ICON_1X,
            true);

    static final MobileIconGroup G = new MobileIconGroup(
            "G",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_gprs,
            TelephonyIcons.ICON_G,
            false);

    static final MobileIconGroup H = new MobileIconGroup(
            "H",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_3_5g,
            TelephonyIcons.ICON_H,
            false);

    static final MobileIconGroup H_PLUS = new MobileIconGroup(
            "H+",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_3_5g_plus,
            TelephonyIcons.ICON_H_PLUS,
            false);

    static final MobileIconGroup FOUR_G = new MobileIconGroup(
            "4G",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_4g,
            TelephonyIcons.ICON_4G,
            true);

    static final MobileIconGroup FOUR_G_PLUS = new MobileIconGroup(
            "4G+",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0,0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_4g_plus,
            TelephonyIcons.ICON_4G_PLUS,
            true);

    static final MobileIconGroup LTE = new MobileIconGroup(
            "LTE",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_lte,
            TelephonyIcons.ICON_LTE,
            true);

    static final MobileIconGroup LTE_PLUS = new MobileIconGroup(
            "LTE+",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.data_connection_lte_plus,
            TelephonyIcons.ICON_LTE_PLUS,
            true);

    static final MobileIconGroup DATA_DISABLED = new MobileIconGroup(
            "DataDisabled",
            null,
            null,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            0,
            0,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.cell_data_off_content_description,
            0,
            false);
    /* UNISOC: add feature statusbar signal cluster view. @{ */
    public static final int[] TELEPHONY_SIGNAL_STRENGTH = {
            ICON_SIGNAL_ZERO,
            ICON_SIGNAL_ONE,
            ICON_SIGNAL_TWO,
            ICON_SIGNAL_THREE,
            ICON_SIGNAL_FOUR
    };
    /* @} */
}

