/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.powerwidget;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BrightnessSlider;

import java.lang.Runnable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VolumePanel extends FrameLayout {
    private static final String TAG = "VolumePanel";

    private Context mContext;
    private boolean mExpanded = false;
    private AudioManager mAudioManager;
    private Ringtone mRingtone;
    private Handler mHandler;
    private ImageView mToggleButton;
    private ScrollView mScrollView;

    // These arrays must all match in length and order
    private static final int[] LAYOUT_ID = new int[] {
        R.id.media_volume_layout,
        R.id.ringer_volume_layout,
        R.id.notification_volume_layout,
        R.id.alarm_volume_layout
    };

    private static final int[] SEEKBAR_ID = new int[] {
        R.id.media_volume_seekbar,
        R.id.ringer_volume_seekbar,
        R.id.notification_volume_seekbar,
        R.id.alarm_volume_seekbar
    };

    private static final int[] SEEKBAR_TYPE = new int[] {
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM
    };

    private static final int[] STREAM_MIN_RES_ID = new int[] {
        R.drawable.ic_media_volume_min,
        R.drawable.ic_ringer_volume_min,
        R.drawable.ic_notification_volume_min,
        R.drawable.ic_alarm_volume_min
    };

    private static final int[] STREAM_MAX_RES_ID = new int[] {
        R.drawable.ic_media_volume_max,
        R.drawable.ic_ringer_volume_max,
        R.drawable.ic_notification_volume_max,
        R.drawable.ic_alarm_volume_max
    };

    public VolumePanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
        mRingtone = RingtoneManager.getRingtone(mContext, getMediaVolumeUri(getContext()));
    }

    @Override
    protected void onFinishInflate() {
        setupWidget();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setupWidget() {
        Log.i(TAG, "Setting up volume panel");

        for (int i = 0; i < LAYOUT_ID.length; i++) {
            View layout = findViewById(LAYOUT_ID[i]);
            ((ImageView)layout.findViewById(R.id.volume_min)).setImageResource(STREAM_MIN_RES_ID[i]);
            ((ImageView)layout.findViewById(R.id.volume_max)).setImageResource(STREAM_MAX_RES_ID[i]);
            SeekBar sb = (SeekBar)layout.findViewById(SEEKBAR_ID[i]);
            sb.setMax(mAudioManager.getStreamMaxVolume(SEEKBAR_TYPE[i]));
            sb.setProgress(mAudioManager.getStreamVolume(SEEKBAR_TYPE[i]));
            sb.setId(i);
            sb.setOnSeekBarChangeListener(mSeekBarListener);
        }

        mToggleButton = (ImageView)findViewById(R.id.expanded_toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpanded = !mExpanded;
                toggleVolumes(mExpanded);
                if (mScrollView != null) {
                    mScrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                }
            }
        });
    }
    
    public void setScrollView(ScrollView scrollView) {
        mScrollView = scrollView;
    }

    public void toggleVolumes(boolean expanded) {
        for (int i = 0; i < LAYOUT_ID.length; i++) {
            if (isVoiceCapable(mContext) || LAYOUT_ID[i] != R.id.ringer_volume_layout)
                findViewById(LAYOUT_ID[i]).setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        mToggleButton.setImageResource(expanded ? R.drawable.ic_extended_toggles_collapse :
                R.drawable.ic_extended_toggles_expand);
        mExpanded = expanded;
    }

    SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mAudioManager.setStreamVolume(SEEKBAR_TYPE[seekBar.getId()], progress, 0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int index = seekBar.getId();
            if (!isSamplePlaying()) {
                mRingtone.setStreamType(SEEKBAR_TYPE[index]);
                startSample();
            }
        }
    };

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    private boolean isSamplePlaying() {
        return mRingtone != null && mRingtone.isPlaying();
    }

    private void startSample() {
        if (mRingtone != null) {
            mRingtone.play();
        }
    }

    private void stopSample() {
        if (mRingtone != null) {
            mRingtone.stop();
        }
    }

    private Uri getMediaVolumeUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName()
                + "/" + R.raw.media_volume);
    }
}
