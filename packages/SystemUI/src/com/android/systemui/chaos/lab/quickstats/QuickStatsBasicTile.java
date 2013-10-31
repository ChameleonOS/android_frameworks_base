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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
class QuickStatsBasicTile extends QuickStatsTileView {
    private final TextView mTextView;
    private final ImageView mImageView;

    public QuickStatsBasicTile(Context context) {
        this(context, null);
    }

    public QuickStatsBasicTile(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT,
            context.getResources().getDimensionPixelSize(R.dimen.quick_stats_cell_height)
        ));
        setBackgroundResource(R.drawable.qstats_tile_background);
        addView(LayoutInflater.from(context).inflate(
                R.layout.qstats_tile_basic, null),
                new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT));
        mTextView = (TextView) findViewById(R.id.text);
        mImageView = (ImageView) findViewById(R.id.image);
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public TextView getTextView() {
        return mTextView;
    }

    public void setImageDrawable(Drawable drawable) {
        mImageView.setImageDrawable(drawable);
    }

    public void setImageResource(int resId) {
        mImageView.setImageResource(resId);
    }

    public void setText(CharSequence text) {
        mTextView.setText(text);
    }

    public void setTextResource(int resId) {
        mTextView.setText(resId);
    }
}
