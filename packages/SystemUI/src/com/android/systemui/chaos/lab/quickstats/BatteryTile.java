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
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;

import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class BatteryTile extends QuickStatsTile {
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private int mBatteryLevel = 0;
    private int mBatteryStatus;
    private String mChargerType;
    private String mUptime;
    private Drawable mBatteryIcon;

    public BatteryTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mQSC.registerAction(Intent.ACTION_BATTERY_CHANGED, this);
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                    0);
            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    mChargerType = mContext.getString(R.string.quick_stats_battery_charging_ac);
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    mChargerType = mContext.getString(R.string.quick_stats_battery_charging_usb);
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    mChargerType = mContext.getString(R.string.quick_stats_battery_charging_wireless);
                    break;
            }
            updateTile();
            updateQuickStats();
        }
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        final int drawableResId = R.drawable.qs_sys_battery;

        mBatteryIcon = mContext.getResources().getDrawable(drawableResId);

        if (mBatteryStatus == BatteryManager.BATTERY_STATUS_FULL) {
            mLabel = mContext.getString(R.string.quick_stats_battery_charged_label);
        } else if (mBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
            mLabel = mContext.getString(R.string.quick_stats_battery_charging_label,
                    mBatteryLevel, mChargerType);
        } else {
            mLabel = mContext.getString(R.string.status_bar_settings_battery_meter_format,
                    mBatteryLevel) + "\n" + getBatteryUpTime();
        }
    }

    @Override
    void updateQuickStats() {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        ImageView iv = (ImageView) mTile.findViewById(R.id.image);

        tv.setText(mLabel);
        iv.setImageDrawable(mBatteryIcon);
        iv.setImageLevel(mBatteryLevel);
    }

    private String getBatteryUpTime() {
        try {
            IBatteryStats batteryInfo = IBatteryStats.Stub.asInterface(
                    ServiceManager.getService("batteryinfo"));
            byte[] data = batteryInfo.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            BatteryStatsImpl stats = com.android.internal.os.BatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);
            long upTime = stats.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000,
                    BatteryStats.STATS_SINCE_CHARGED);
            return formatElapsedTime(upTime / 1000.0);
        } catch (RemoteException e) {
        }
        return "";
    }

    /**
     * Returns elapsed time for the given millis, in the following format:
     * 2d 5h 40m 29s
     * @param millis the elapsed time in milli seconds
     * @return the formatted elapsed time
     */
    private String formatElapsedTime(double millis) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000);

        int days = 0, hours = 0, minutes = 0;
        if (seconds > SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds > SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds > SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        if (days > 0) {
            sb.append(mContext.getString(R.string.quick_stats_battery_uptime_days,
                    days, hours, minutes, seconds));
        } else if (hours > 0) {
            sb.append(mContext.getString(R.string.quick_stats_battery_uptime_hours,
                    hours, minutes, seconds));
        } else if (minutes > 0) {
            sb.append(mContext.getString(R.string.quick_stats_battery_uptime_minutes,
                    minutes, seconds));
        } else {
            sb.append(mContext.getString(R.string.quick_stats_battery_uptime_seconds,
                    seconds));
        }
        return sb.toString();
    }
}
