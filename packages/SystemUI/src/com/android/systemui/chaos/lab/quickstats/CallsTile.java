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
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class CallsTile extends QuickStatsTile {
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    public CallsTile(Context context, QuickStatsController qsc) {
        super(context, qsc);
    }

    @Override
    void onPostCreate() {
        mDrawable = R.drawable.ic_qstats_calls;
        mQSC.registerObservedContent(CallLog.Calls.CONTENT_URI, this);
        updateResources();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateResources();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        int out = 0;
        int in = 0;
        int missed = 0;
        int totalDuration = 0;
        final ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while(cursor.moveToNext()) {
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                switch (type) {
                    case CallLog.Calls.INCOMING_TYPE:
                        in++;
                        totalDuration +=
                                cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                        break;
                    case CallLog.Calls.OUTGOING_TYPE:
                        out++;
                        totalDuration +=
                                cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                        break;
                    case CallLog.Calls.MISSED_TYPE:
                        missed++;
                        break;
                }
            }
            cursor.close();
        }
        String duration = formatElapsedTime(totalDuration);
        mLabel = mContext.getString(R.string.quick_stats_calls_label,
                in, out, missed, duration);
    }

    /**
     * Returns elapsed time for the given millis, in the following format:
     * 2d 5h 40m 29s
     * @return the formatted elapsed time
     */
    private String formatElapsedTime(int seconds) {
        StringBuilder sb = new StringBuilder();

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
