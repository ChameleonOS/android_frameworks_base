/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
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

package com.android.systemui.chaos.lab.quickstats;

import android.annotation.ChaosLab;
import android.annotation.ChaosLab.Classification;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.WifiIcons;

import static android.net.wifi.WifiManager.WIFI_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_STATE_UNKNOWN;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class WiFiTile extends QuickStatsTile {
    private static final int INET_CONDITION_THRESHOLD = 50;
    private static final int ICON_DIVISOR = 100 / WifiIcons.WIFI_LEVEL_COUNT;

    private boolean mWifiConnected;
    private boolean mWifiNotConnected;
    private int mSignalStrength;
    private int mInetCondition;
    private String mSsid = "";

    public WiFiTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mQSC.registerAction(WifiManager.WIFI_STATE_CHANGED_ACTION, this);
        mQSC.registerAction(WifiManager.RSSI_CHANGED_ACTION, this);
        mQSC.registerAction(ConnectivityManager.INET_CONDITION_ACTION, this);
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WIFI_STATE_UNKNOWN);
            mWifiConnected = (state == WIFI_STATE_ENABLED || state == WIFI_STATE_ENABLING);
            mWifiNotConnected = (state == WIFI_STATE_DISABLED || state == WIFI_STATE_DISABLING);
            if (mWifiConnected) {
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wm.getConnectionInfo();
                if (info != null) {
                    mSignalStrength = rssiToSignalStrength(info.getRssi());
                    mSsid = info.getSSID();
                    if (mSsid != null) {
                        mSsid = mSsid.substring(1, mSsid.length() - 1);
                    } else {
                        mSsid = mContext.getString(R.string.quick_stats_wifi_no_network);
                    }
                }
            }
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wm.getConnectionInfo();
            if (info != null) {
                mSignalStrength = rssiToSignalStrength(info.getRssi());
            }
        } else if (ConnectivityManager.INET_CONDITION_ACTION.equals(action)) {
            int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);
            mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);
        } else {
            return;
        }
        updateResources();
    }

    private int rssiToSignalStrength(int rssi) {
        return Math.min(100, 2 * (rssi + 100));
    }

    private synchronized void updateTile() {
        if (mWifiConnected) {
            mDrawable = WifiIcons.QS_WIFI_SIGNAL_STRENGTH[mInetCondition]
                    [signalStrengthToIconIndex(mSignalStrength)];
            mLabel = mContext.getString(R.string.quick_stats_wifi_connected_label,
                    mSsid,
                    mSignalStrength);
        } else if (mWifiNotConnected) {
            mDrawable = R.drawable.ic_qs_wifi_0;
            mLabel = mContext.getString(R.string.quick_stats_wifi_not_connected);
        } else {
            mDrawable = R.drawable.ic_qs_wifi_no_network;
            mLabel = mContext.getString(R.string.quick_stats_wifi_off_label);
        }
    }

    private int signalStrengthToIconIndex(int signalStrength) {
        return Math.min(WifiIcons.WIFI_LEVEL_COUNT - 1, signalStrength / ICON_DIVISOR);
    }

}
