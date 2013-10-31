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
import android.os.BatteryManager;

import com.android.systemui.R;

import java.io.File;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class TemperatureTile extends QuickStatsTile {
    public static final String TEMP_FILE = "/sys/devices/virtual/thermal/thermal_zone0/temp";
    public static final String[] BATTERY_TEMP_FILES = {
            "/sys/class/power_supply/battery/temp",
            "/sys/class/power_supply/battery/batt_temp"
    };
    public static String BATTERY_TEMP_FILE = null;

    private int mCpuTemp;
    private int mBatteryTemp;

    public TemperatureTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_temperature;
        BATTERY_TEMP_FILE = getBatteryTempFile();
        mQSC.registerAction(Intent.ACTION_BATTERY_CHANGED, this);
        mQSC.registerPeriodicUpdate(this);
        updateResources();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            mBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        }
        updateResources();
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

    private synchronized void updateTile() {
        try {
            final String temp = QuickStatsUtils.fileReadOneLine(TEMP_FILE);
            mCpuTemp = Integer.valueOf(temp);
        } catch (NumberFormatException e) {
            mCpuTemp = -1;
        }

        if (BATTERY_TEMP_FILE != null) {
            try {
                final String temp = QuickStatsUtils.fileReadOneLine(BATTERY_TEMP_FILE);
                mBatteryTemp = Integer.valueOf(temp) / 10;
            } catch (NumberFormatException e) {
                mBatteryTemp = -1;
            }
        }

        if (mCpuTemp != -1 && mBatteryTemp != -1) {
            mLabel = mContext.getString(R.string.quick_stats_temperature_both_label,
                    mBatteryTemp, mCpuTemp);
        } else if (mCpuTemp != -1) {
            mLabel = mContext.getString(R.string.quick_stats_temperature_cpu_label,
                    mCpuTemp);
        } else if (mBatteryTemp != -1) {
            mLabel = mContext.getString(R.string.quick_stats_temperature_battery_label,
                    mBatteryTemp);
        }
    }

    private String getBatteryTempFile() {
        File f;
        for (String s : BATTERY_TEMP_FILES) {
            f = new File(s);
            if (f.exists()) {
                return s;
            }
        }

        return null;
    }
}
