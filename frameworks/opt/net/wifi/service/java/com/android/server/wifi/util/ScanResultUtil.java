/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.wifi.util;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.hotspot2.NetworkDetail;

import java.io.PrintWriter;
import java.util.List;
/**
 * Scan result utility for any {@link ScanResult} related operations.
 * Currently contains:
 *   > Helper method for converting a ScanResult to a ScanDetail.
 *     Only fields that are supported in ScanResult are copied.
 *   > Helper methods to identify the encryption of a ScanResult.
 */
public class ScanResultUtil {
    private ScanResultUtil() { /* not constructable */ }

    /**
     * This method should only be used when the informationElements field in the provided scan
     * result is filled in with the IEs from the beacon.
     */
    public static ScanDetail toScanDetail(ScanResult scanResult) {
        NetworkDetail networkDetail = new NetworkDetail(scanResult.BSSID,
                scanResult.informationElements, scanResult.anqpLines, scanResult.frequency);
        return new ScanDetail(scanResult, networkDetail);
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a PSK network or not.
     * This checks if the provided capabilities string contains PSK encryption type or not.
     */
    public static boolean isScanResultForPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a EAP network or not.
     * This checks if the provided capabilities string contains EAP encryption type or not.
     */
    public static boolean isScanResultForEapNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WEP network or not.
     * This checks if the provided capabilities string contains WEP encryption type or not.
     */
    public static boolean isScanResultForWepNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WEP");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to an open network or not.
     * This checks if the provided capabilities string does not contain either of WEP, PSK or EAP
     * encryption types or not.
     */
    public static boolean isScanResultForOpenNetwork(ScanResult scanResult) {
        return !(isScanResultForWepNetwork(scanResult) || isScanResultForPskNetwork(scanResult)
                || isScanResultForEapNetwork(scanResult));
    }

    /**
     * Helper method to quote the SSID in Scan result to use for comparing/filling SSID stored in
     * WifiConfiguration object.
     */
    @VisibleForTesting
    public static String createQuotedSSID(String ssid) {
        return "\"" + ssid + "\"";
    }

    /**
     * Checks if the provided |scanResult| match with the provided |config|. Essentially checks
     * if the network config and scan result have the same SSID and encryption type.
     */
    /*
    public static boolean doesScanResultMatchWithNetwork(
            ScanResult scanResult, WifiConfiguration config) {
        // Add the double quotes to the scan result SSID for comparison with the network configs.
        String configSSID = createQuotedSSID(scanResult.SSID);
        if (TextUtils.equals(config.SSID, configSSID)) {
            if (ScanResultUtil.isScanResultForWapiPskNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForWapiPskNetwork(config)) {
                return true;
            }
            if (ScanResultUtil.isScanResultForWapiCertNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForWapiCertNetwork(config)) {
                return true;
            }
            if (ScanResultUtil.isScanResultForPskNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForPskNetwork(config)) {
                return true;
            }
            if (ScanResultUtil.isScanResultForEapNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForEapNetwork(config)) {
                return true;
            }
            if (ScanResultUtil.isScanResultForWepNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForWepNetwork(config)) {
                return true;
            }
            if (ScanResultUtil.isScanResultForOpenNetwork(scanResult)
                    && WifiConfigurationUtil.isConfigForOpenNetwork(config)) {
                return true;
            }
        }
        return false;
    }
    */

    /**
     * Creates a network configuration object using the provided |scanResult|.
     * This is used to create ephemeral network configurations.
     */
    public static WifiConfiguration createNetworkFromScanResult(ScanResult scanResult) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = createQuotedSSID(scanResult.SSID);
        setAllowedKeyManagementFromScanResult(scanResult, config);
        return config;
    }

    /**
     * Sets the {@link WifiConfiguration#allowedKeyManagement} field on the given
     * {@link WifiConfiguration} based on its corresponding {@link ScanResult}.
     */
    public static void setAllowedKeyManagementFromScanResult(ScanResult scanResult,
            WifiConfiguration config) {
        if (isScanResultForPskSha256Network(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.PSK_SHA256);
        } else if (isScanResultForEapSha256Network(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.EAP_SHA256);
        } else if (isScanResultForWapiPskNetwork(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WAPI_PSK);
        } else if (isScanResultForWapiCertNetwork(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WAPI_CERT);
        } else if (isScanResultForPskNetwork(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else if (isScanResultForEapNetwork(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        } else if (isScanResultForWepNetwork(scanResult)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
    }

    /**
     * Dump the provided scan results list to |pw|.
     */
    public static void dumpScanResults(PrintWriter pw, List<ScanResult> scanResults, long nowMs) {
        if (scanResults != null && scanResults.size() != 0) {
            pw.println("    BSSID              Frequency      RSSI           Age(sec)     SSID "
                    + "                                Flags");
            for (ScanResult r : scanResults) {
                long timeStampMs = r.timestamp / 1000;
                String age;
                if (timeStampMs <= 0) {
                    age = "___?___";
                } else if (nowMs < timeStampMs) {
                    age = "  0.000";
                } else if (timeStampMs < nowMs - 1000000) {
                    age = ">1000.0";
                } else {
                    age = String.format("%3.3f", (nowMs - timeStampMs) / 1000.0);
                }
                String ssid = r.SSID == null ? "" : r.SSID;
                String rssiInfo = "";
                if (ArrayUtils.size(r.radioChainInfos) == 1) {
                    rssiInfo = String.format("%5d(%1d:%3d)       ", r.level,
                            r.radioChainInfos[0].id, r.radioChainInfos[0].level);
                } else if (ArrayUtils.size(r.radioChainInfos) == 2) {
                    rssiInfo = String.format("%5d(%1d:%3d/%1d:%3d)", r.level,
                            r.radioChainInfos[0].id, r.radioChainInfos[0].level,
                            r.radioChainInfos[1].id, r.radioChainInfos[1].level);
                } else {
                    rssiInfo = String.format("%9d         ", r.level);
                }
                pw.printf("  %17s  %9d  %18s   %7s    %-32s  %s\n",
                        r.BSSID,
                        r.frequency,
                        rssiInfo,
                        age,
                        String.format("%1.32s", ssid),
                        r.capabilities);
            }
        }
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-PSK network or not.
     * This checks if the provided capabilities string contains WAPI-PSK encryption type or not.
     */
    public static boolean isScanResultForWapiPskNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-PSK");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a WAPI-PSK network or not.
     * This checks if the provided capabilities string contains WAPI-PSK encryption type or not.
     */
    public static boolean isScanResultForWapiCertNetwork(ScanResult scanResult) {
        return scanResult.capabilities.contains("WAPI-CERT");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a PSK SHA256 network or not.
     * This checks if the provided capabilities string contains PSK-SHA256 encryption type or not.
     */
    public static boolean isScanResultForPskSha256Network(ScanResult scanResult) {
        return scanResult.capabilities.contains("PSK-SHA256");
    }

    /**
     * Helper method to check if the provided |scanResult| corresponds to a EAP SHA256 network or not.
     * This checks if the provided capabilities string contains EAP-SHA256 encryption type or not.
     */
    public static boolean isScanResultForEapSha256Network(ScanResult scanResult) {
        return scanResult.capabilities.contains("EAP-SHA256");
    }
}
