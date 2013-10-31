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
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class ProcessorTile extends QuickStatsTile {
    public static final String FREQ_CUR_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

    public ProcessorTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_processor;
        mQSC.registerPeriodicUpdate(this);
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
        String currentFrequency = QuickStatsUtils.fileReadOneLine(FREQ_CUR_FILE);
        currentFrequency = currentFrequency != null ? toMHz(currentFrequency) : "";
        String currentGovernor = QuickStatsUtils.fileReadOneLine(GOV_FILE);

        mLabel = mContext.getString(R.string.quick_stats_processor_label,
                currentFrequency, currentGovernor);
    }

    private String toMHz(String mhzString) {
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).toString();
    }

}
