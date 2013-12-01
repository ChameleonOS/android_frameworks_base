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

package com.android.systemui.battery;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.systemui.DemoMode;
import com.android.systemui.R;

public class BatteryMeterLayout extends FrameLayout implements DemoMode {
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    private static final int STYLE_NONE = 0;
    private static final int STYLE_BATTERY_METER = 1;
    private static final int STYLE_BATTERY_METER_WITH_TEXT = 2;
    private static final int STYLE_CIRCLE_BATTERY = 3;
    private static final int STYLE_CIRCLE_BATTERY_WITH_TEXT = 4;
    private static final int STYLE_PIE_BATTERY = 5;
    private static final int STYLE_PIE_BATTERY_WITH_TEXT = 6;

    private BatteryMeterView mBatteryMeterView;
    private CircleBatteryView mCircleBatteryView;
    private PieBatteryView mPieBatteryView;
    private int mStyle = STYLE_BATTERY_METER;

    private SettingsObserver mSettingsObserver;

    private class SettingsObserver extends ContentObserver {

        public SettingsObserver() {
            super(new Handler());
        }

        public void observe() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_BATTERY_STYLE),
                    false, this);
            onChange(true);
        }

        public void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            final ContentResolver resolver = mContext.getContentResolver();
            mStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_BATTERY_STYLE,
                    STYLE_BATTERY_METER);

            switch (mStyle) {
                case STYLE_NONE:
                    mBatteryMeterView.setVisibility(GONE);
                    mCircleBatteryView.setVisibility(GONE);
                    mPieBatteryView.setVisibility(GONE);
                    break;
                case STYLE_BATTERY_METER_WITH_TEXT:
                case STYLE_BATTERY_METER:
                    mBatteryMeterView.showPercentage(mStyle == STYLE_BATTERY_METER_WITH_TEXT);
                    mBatteryMeterView.setVisibility(VISIBLE);
                    mCircleBatteryView.setVisibility(GONE);
                    mPieBatteryView.setVisibility(GONE);
                    break;
                case STYLE_CIRCLE_BATTERY_WITH_TEXT:
                case STYLE_CIRCLE_BATTERY:
                    mCircleBatteryView.showPercentage(mStyle == STYLE_CIRCLE_BATTERY_WITH_TEXT);
                    mCircleBatteryView.setVisibility(VISIBLE);
                    mBatteryMeterView.setVisibility(GONE);
                    mPieBatteryView.setVisibility(GONE);
                    break;
                case STYLE_PIE_BATTERY_WITH_TEXT:
                case STYLE_PIE_BATTERY:
                    mPieBatteryView.showPercentage(mStyle == STYLE_PIE_BATTERY_WITH_TEXT);
                    mPieBatteryView.setVisibility(VISIBLE);
                    mBatteryMeterView.setVisibility(GONE);
                    mCircleBatteryView.setVisibility(GONE);
                    break;
            }
            postInvalidate();
        }
    }

    public class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                setContentDescription(
                        context.getString(R.string.accessibility_battery_level, level));
                postInvalidate();
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC : 0);
                            dummy.putExtra("testmode", true);
                        }
                        getContext().sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        postDelayed(this, 200);
                    }
                });
            }
        }
    }

    private BatteryTracker mTracker = new BatteryTracker();

    public BatteryTracker getBatteryTracker() {
        return mTracker;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_LEVEL_TEST);
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            // preload the battery level
            mTracker.onReceive(getContext(), sticky);
        }
        mSettingsObserver.observe();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mTracker);
        mSettingsObserver.unobserve();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        switch (mStyle) {
            case STYLE_BATTERY_METER:
            case STYLE_BATTERY_METER_WITH_TEXT:
                mBatteryMeterView.invalidate();
                break;
            case STYLE_CIRCLE_BATTERY:
            case STYLE_CIRCLE_BATTERY_WITH_TEXT:
                mCircleBatteryView.invalidate();
            case STYLE_PIE_BATTERY:
            case STYLE_PIE_BATTERY_WITH_TEXT:
                mPieBatteryView.invalidate();
        }
    }

    public BatteryMeterLayout(Context context) {
        this(context, null);
    }

    public BatteryMeterLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSettingsObserver = new SettingsObserver();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.battery_meter);
        mBatteryMeterView.setBatteryTracker(mTracker);

        mCircleBatteryView = (CircleBatteryView) findViewById(R.id.circle_battery);
        mCircleBatteryView.setBatteryTracker(mTracker);

        mPieBatteryView = (PieBatteryView) findViewById(R.id.pie_battery);
        mPieBatteryView.setBatteryTracker(mTracker);
    }

    private boolean mDemoMode;
    private BatteryTracker mDemoTracker = new BatteryTracker();

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
            mDemoTracker.level = mTracker.level;
            mDemoTracker.plugged = mTracker.plugged;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            postInvalidate();
        } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                mDemoTracker.plugged = Boolean.parseBoolean(plugged);
            }
            postInvalidate();
        }
    }

}
