package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;

public class BatteryTile extends QuickSettingsTile implements BatteryStateChangeCallback{
    private BatteryController mController;

    private int mBatteryLevel = 0;
    private boolean mPluggedIn = false;
    private Drawable mBatteryIcon;

    public BatteryTile(Context context, QuickSettingsController qsc, BatteryController controller) {
        super(context, qsc);

        mController = controller;

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSettingsActivity(Intent.ACTION_POWER_USAGE_SUMMARY);
            }
        };
    }

    @Override
    void onPostCreate() {
        updateTile();
        mController.addStateChangedCallback(this);
        super.onPostCreate();
    }

    @Override
    public void onDestroy() {
        mController.removeStateChangedCallback(this);
        super.onDestroy();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        final int drawableResId = (mPluggedIn && mBatteryLevel < 100)
                ? R.drawable.qs_sys_battery_charging : R.drawable.qs_sys_battery;

        mBatteryIcon = mContext.getResources().getDrawable(drawableResId);

        if (mBatteryLevel == 100) {
            mLabel = mContext.getString(R.string.quick_settings_battery_charged_label);
        } else if (mPluggedIn) {
            mLabel = mContext.getString(R.string.quick_settings_battery_charging_label,
                    mBatteryLevel);
        } else {
            mLabel = mContext.getString(R.string.status_bar_settings_battery_meter_format,
                    mBatteryLevel);
        }
    }

    @Override
    void updateQuickSettings() {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        ImageView iv = (ImageView) mTile.findViewById(R.id.image);

        tv.setText(mLabel);
        iv.setImageDrawable(mBatteryIcon);
        iv.setImageLevel(mBatteryLevel);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn) {
        mBatteryLevel = level;
        mPluggedIn = pluggedIn;
        updateResources();
    }
}
