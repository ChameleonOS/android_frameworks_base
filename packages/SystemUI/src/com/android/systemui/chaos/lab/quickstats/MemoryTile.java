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
import android.app.ActivityManager;
import android.content.Context;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class MemoryTile extends QuickStatsTile {

    public MemoryTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_memory;
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
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(mi);
        long free = mi.availMem;
        long used = mi.totalMem - free;
        String freeMemory = "" + (free / 1048576L) + "M";
        String usedMemory = "" + (used / 1048576L) + "M";

        mLabel = mContext.getString(R.string.quick_stats_memory_label,
                usedMemory, freeMemory);
    }

}
