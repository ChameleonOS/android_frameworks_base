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
import android.os.Environment;
import android.os.StatFs;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class StorageTile extends QuickStatsTile {

    private static final long UNITS = 1024L;

    public StorageTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_storage;
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
        StatFs statInternal = new StatFs("/data/");
        String internalTotal = humanReadableByteCount(statInternal.getTotalBytes());
        String internalUsed = humanReadableByteCount(
                statInternal.getTotalBytes() - statInternal.getFreeBytes());
        String externalTotal = null;
        String externalUsed = null;
        if (!Environment.isExternalStorageEmulated()) {
            StatFs statExternal = new StatFs(Environment.getExternalStorageDirectory().getPath());
            externalTotal = humanReadableByteCount(statExternal.getTotalBytes());
            externalUsed =humanReadableByteCount(
                    statExternal.getTotalBytes() - statExternal.getFreeBytes());
        }

        if (externalTotal != null && externalUsed != null) {
            mLabel = mContext.getString(R.string.quick_stats_storage_both_label,
                    internalUsed, internalTotal, externalUsed, externalTotal);
        } else {
            mLabel = mContext.getString(R.string.quick_stats_storage_internal_label,
                    internalUsed, internalTotal);
        }
    }

    public static String humanReadableByteCount(long bytes) {
        if (bytes < UNITS) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(UNITS));
        char pre = ("KMGTPE").charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(UNITS, exp), pre);
    }
}
