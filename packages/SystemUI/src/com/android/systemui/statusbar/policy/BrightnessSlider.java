/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.IWindowManager;
import android.widget.SeekBar;

import com.android.systemui.R;

public class BrightnessSlider implements SeekBar.OnSeekBarChangeListener {
    static final String TAG = "BrightnessSlider";
    private int mScreenBrightnessDim;
    private static final int MAXIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_ON;

    private Context mContext;
    private IPowerManager mPower;
    private boolean mIsTracking = false;
    private SeekBar mControl;

    public BrightnessSlider(Context context, SeekBar control) {
        mControl = control;
        control.setOnSeekBarChangeListener(this);

        mContext = context;

        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        mScreenBrightnessDim = context.getResources().getInteger(
                com.android.internal.R.integer.config_screenBrightnessDim);

        int value;
        try {
            value = Settings.System.getInt(mContext.getContentResolver(), 
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException ex) {
            value = MAXIMUM_BACKLIGHT;
        }

        mControl.setMax(MAXIMUM_BACKLIGHT - mScreenBrightnessDim);
        mControl.setProgress(value - mScreenBrightnessDim);

        SettingsObserver observer = new SettingsObserver(new Handler());
        observer.observe();
    }

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
        }        
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, 
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        final int val = progress + mScreenBrightnessDim;
        setBrightness(val);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    
    public void onStopTrackingTouch(SeekBar seekBar) {
        final int val = mControl.getProgress() + mScreenBrightnessDim;
        setBrightness(val);

        AsyncTask.execute(new Runnable() {
            public void run() {
                Settings.System.putInt(mContext.getContentResolver(), 
                        Settings.System.SCREEN_BRIGHTNESS, val);
            }
        });
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_BRIGHTNESS), false, this);
            update();
        }

        @Override
        public void onChange(boolean selfChange) {
            if (!selfChange)
                update();
        }

        public void update() {
            int value;
            try {
                value = Settings.System.getInt(mContext.getContentResolver(), 
                        Settings.System.SCREEN_BRIGHTNESS);
            } catch (SettingNotFoundException ex) {
                value = MAXIMUM_BACKLIGHT;
            }

            mControl.setProgress(value - mScreenBrightnessDim);
        }
    }
}

