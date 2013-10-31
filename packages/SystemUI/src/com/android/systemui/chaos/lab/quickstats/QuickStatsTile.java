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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsTile {

    protected final Context mContext;
    protected QuickStatsContainerView mContainer;
    protected QuickStatsTileView mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected final int mTileLayout;
    protected int mDrawable;
    protected String mLabel;
    protected QuickStatsController mQSC;
    protected SharedPreferences mPrefs;

    public QuickStatsTile(Context context, QuickStatsController sic) {
        this(context, sic, R.layout.qstats_tile_basic);
    }

    public QuickStatsTile(Context context, QuickStatsController sic, int layout) {
        mContext = context;
        mDrawable = R.drawable.ic_notifications;
        mLabel = mContext.getString(R.string.quick_settings_label_enabled);
        mQSC = sic;
        mTileLayout = layout;
        mPrefs = mContext.getSharedPreferences("systeminfo", Context.MODE_PRIVATE);
    }

    public void setupQuickStatsTile(LayoutInflater inflater,
                                    QuickStatsContainerView container) {
        mTile = (QuickStatsTileView) inflater.inflate(
                R.layout.qstats_tile, container, false);
        mTile.setContent(mTileLayout, inflater);
        mContainer = container;
        mContainer.addView(mTile);
        onPostCreate();
        updateQuickStats();
    }

    public void setLabelVisibility(boolean visible) {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        if (tv != null) {
            tv.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        View sepPadding = mTile.findViewById(R.id.separator_padding);
        if (sepPadding != null) {
            sepPadding.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    void onPostCreate() {}

    public void onDestroy() {}

    public void onReceive(Context context, Intent intent) {}

    public void onChangeUri(ContentResolver resolver, Uri uri) {}

    public void onPause() {}

    public void onResume() {}

    public void updateResources() {
        if(mTile != null) {
            updateQuickStats();
        }
    }

    void updateQuickStats() {
        TextView tv = (TextView) mTile.findViewById(R.id.text);
        if (tv != null) {
            tv.setText(mLabel);
        }
        ImageView image = (ImageView) mTile.findViewById(R.id.image);
        if (image != null) {
            image.setImageResource(mDrawable);
        }
    }
}
