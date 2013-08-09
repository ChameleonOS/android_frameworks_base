/*
 * Copyright (C) 2013 The ChameleonOS Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.R;

public class BatteryBar extends View {
    private static final long ANIMATION_FRAME_DELAY = 20;
    private static final long ANIMATION_RESTART_DELAY = 2000;

    private Handler mHandler;
    private Context mContext;
    private BatteryReceiver mBatteryReceiver = null;

    // state variables
    private boolean mAttached;      // whether or not attached to a window
    private boolean mActivated;     // whether or not activated due to system settings
    private boolean mBatteryPlugged;// whether or not battery is currently plugged
    private int     mBatteryStatus; // current battery status
    private int     mLevel;         // current battery level
    private int     mAnimOffset;    // current level of charging animation
    private boolean mIsAnimating;   // stores charge-animation status to reliably remove callbacks

    private int     mLowColor;
    private int     mHighColor;
    private Paint   mPaint;

    private int     mHeight;

    GradientDrawable mBarGradient;
    int[] mGradientColors;

    // runnable to invalidate view via mHandler.postDelayed() call
    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            if(mActivated && mAttached) {
                invalidate();
            }
        }
    };

    // observes changes in system battery settings and enables/disables view accordingly
    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_BATTERY), false, this);
            onChange(true);
        }

        @Override
        public void onChange(boolean selfChange) {
            int batteryStyle = (Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, 0));

            mActivated = (batteryStyle == BatteryController.BATTERY_STYLE_BAR);

            setVisibility(mActivated && isBatteryPresent() ? View.VISIBLE : View.GONE);
            if (mBatteryReceiver != null) {
                mBatteryReceiver.updateRegistration();
            }

            if (mActivated && mAttached) {
                invalidate();
            }
        }
    }

    // keeps track of current battery level and charger-plugged-state
    class BatteryReceiver extends BroadcastReceiver {
        private boolean mIsRegistered = false;

        public BatteryReceiver(Context context) {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                onBatteryStatusChange(intent);

                int visibility = View.VISIBLE;//mActivated && isBatteryPresent() ? View.VISIBLE : View.GONE;
                if (getVisibility() != visibility) {
                    setVisibility(visibility);
                }

                if (mActivated && mAttached) {
                    invalidate();
                }
            }
        }

        private void registerSelf() {
            if (!mIsRegistered) {
                mIsRegistered = true;

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                mContext.registerReceiver(mBatteryReceiver, filter);
            }
        }

        private void unregisterSelf() {
            if (mIsRegistered) {
                mIsRegistered = false;
                mContext.unregisterReceiver(this);
            }
        }

        private void updateRegistration() {
            if (mActivated && mAttached) {
                registerSelf();
            } else {
                unregisterSelf();
            }
        }
    }

    /***
     * Start of CircleBattery implementation
     */
    public BatteryBar(Context context) {
        this(context, null);
    }

    public BatteryBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;
        mHandler = new Handler();

        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        mBatteryReceiver = new BatteryReceiver(mContext);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        Resources res = getResources();

        mHeight = res.getDimensionPixelSize(com.android.systemui.R.dimen.battery_bar_height);

        mLowColor = res.getColor(R.color.holo_red_light);
        mHighColor = res.getColor(R.color.holo_blue_light);
        mPaint.setColor(mHighColor);

        mGradientColors = new int[2];
        mGradientColors[0] = mLowColor;
        mGradientColors[1] = mHighColor;

        mBarGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, mGradientColors);
    }

    protected int getLevel() {
        return mLevel;
    }

    protected int getBatteryStatus() {
        return mBatteryStatus;
    }

    protected boolean isBatteryPlugged() {
        return mBatteryPlugged;
    }

    protected boolean isBatteryPresent() {
        return true;
    }

    private boolean isBatteryStatusUnknown() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_UNKNOWN;
    }

    private boolean isBatteryStatusCharging() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    protected void onBatteryStatusChange(Intent intent) {
        mLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                                            BatteryManager.BATTERY_STATUS_UNKNOWN);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            mBatteryReceiver.updateRegistration();
            mHandler.postDelayed(mInvalidate, 250);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
            mBatteryReceiver.updateRegistration();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateChargeAnim();
        float width = (float)getWidth();
        float size = (mIsAnimating ? mAnimOffset : mLevel) / 100f;

        mGradientColors[1] = mixColors(mHighColor, mLowColor, size);
        mBarGradient.setBounds(0, 0, (int)(width*size), mHeight);
        mBarGradient.setColors(mGradientColors);
        mBarGradient.draw(canvas);
    }

    private int mixColors(int color1, int color2, float mix) {
        int[] rgb1 = colorToRgb(color1);
        int[] rgb2 = colorToRgb(color2);

        rgb1[0] = mixedValue(rgb1[0], rgb2[0], mix);
        rgb1[1] = mixedValue(rgb1[1], rgb2[1], mix);
        rgb1[2] = mixedValue(rgb1[2], rgb2[2], mix);
        rgb1[3] = mixedValue(rgb1[3], rgb2[3], mix);

        return rgbToColor(rgb1);
    }

    private int[] colorToRgb(int color) {
        int[] rgb = {(color & 0xFF000000) >> 24, (color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, (color & 0xFF)};
        return rgb;
    }

    private int rgbToColor(int[] rgb) {
        return (rgb[0] << 24) + (rgb[1] << 16) + (rgb[2] << 8) + rgb[3];
    }

    private int mixedValue(int val1, int val2, float mix) {
        return (int)Math.min((mix * val1 + (1f - mix) * val2), 255f);
    }

    /***
     * updates the animation counter
     * cares for timed callbacks to continue animation cycles
     * uses mInvalidate for delayed invalidate() callbacks
     */
    private void updateChargeAnim() {
        if (!isBatteryStatusCharging()) {
            if (mIsAnimating) {
                mIsAnimating = false;
                mAnimOffset = 0;
                mHandler.removeCallbacks(mInvalidate);
            }
            return;
        }

        mIsAnimating = true;
        long delay = ANIMATION_FRAME_DELAY;

        if (mAnimOffset >= mLevel) {
            mAnimOffset = 0;
        } else {
            mAnimOffset += 1;
            if (mAnimOffset >= mLevel) {
                mAnimOffset = mLevel;
                delay = ANIMATION_RESTART_DELAY;
            }
        }

        mHandler.removeCallbacks(mInvalidate);
        mHandler.postDelayed(mInvalidate, delay);
    }
}
