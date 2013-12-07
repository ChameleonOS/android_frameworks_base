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

package com.android.systemui.usb;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.battery.BatteryMeterLayout;

public class BaseBatteryView extends View {
    protected BatteryMeterLayout.BatteryTracker mTracker;

    public BaseBatteryView(Context context) {
        super(context);
    }

    public BaseBatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseBatteryView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setBatteryTracker(BatteryMeterLayout.BatteryTracker tracker) {
        mTracker = tracker;
    }
}
