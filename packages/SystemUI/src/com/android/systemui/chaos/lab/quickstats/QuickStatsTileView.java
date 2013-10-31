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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

/**
 *
 */
@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsTileView extends FrameLayout {

    private int mColSpan;
    private int mRowSpan;

    public QuickStatsTileView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mColSpan = 1;
        mRowSpan = 1;
    }

    void setColumnSpan(int span) {
        mColSpan = span;
    }

    int getColumnSpan() {
        return mColSpan;
    }

    public void setContent(int layoutId, LayoutInflater inflater) {
        inflater.inflate(layoutId, this);
    }

    @Override
    public void setVisibility(int vis) {
        super.setVisibility(vis);
    }
}
