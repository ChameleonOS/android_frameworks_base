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

import static com.android.internal.util.chaos.QStatsConstants.*;

import android.annotation.ChaosLab;
import android.annotation.ChaosLab.Classification;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import com.android.systemui.statusbar.phone.PanelBar;

import com.android.internal.util.chaos.QStatsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsController {
    private static String TAG = "QuickStatsController";
    private static int PERIODIC_UPDATE_TIME = 2000;

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickStatsTile>> mReceiverMap
            = new HashMap<String, ArrayList<QuickStatsTile>>();
    public HashMap<Uri, ArrayList<QuickStatsTile>> mObserverMap
            = new HashMap<Uri, ArrayList<QuickStatsTile>>();
    public ArrayList<QuickStatsTile> mPeriodicRefreshList
            = new ArrayList<QuickStatsTile>();

    // Uris that need to be monitored for updating tile status
    private HashSet<Uri> mTileStatusUris = new HashSet<Uri>();

    private final Context mContext;
    private ArrayList<QuickStatsTile> mSystemInfoTiles;
    public PanelBar mBar;
    private final QuickStatsContainerView mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;
    private final String mSettingsString;
    private boolean mPaused;
    private PeriodicUpdateThread mUpdateThread;

    private static final int MSG_UPDATE_TILES = 1000;

    public QuickStatsController(Context context, QuickStatsContainerView container, String settings) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSG_UPDATE_TILES:
                        setupQuickStats();
                        break;
                }
            }
        };
        mSystemInfoTiles = new ArrayList<QuickStatsTile>();
        mSettingsString = settings;
    }

    void loadTiles() {
        // Filter items not compatible with device
        boolean mobileDataSupported = QStatsUtils.deviceSupportsMobileData(mContext);
        if (!mobileDataSupported) {
            TILES_DEFAULT.remove(TILE_MESSAGING);
            TILES_DEFAULT.remove(TILE_CALLS);
        }

        // Read the stored list of tiles
        ContentResolver resolver = mContext.getContentResolver();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        String tiles = Settings.System.getStringForUser(resolver,
                mSettingsString, UserHandle.USER_CURRENT);
        if (tiles == null) {
            Log.i(TAG, "Default tiles being loaded");
            tiles = TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
        }

        Log.i(TAG, "Tiles list: " + tiles);

        // Split out the tile names and add to the list
        for (String tile : tiles.split("\\|")) {
            QuickStatsTile sit = null;
            if (tile.equals(TILE_BATTERY)) {
                sit = new BatteryTile(mContext, this);
            } else if (tile.equals(TILE_WIFI)) {
                sit = new WiFiTile(mContext, this);
            } else if (tile.equals(TILE_MESSAGING)) {
                sit = new MessagingTile(mContext, this);
            } else if (tile.equals(TILE_CALLS)) {
                sit = new CallsTile(mContext, this);
            } else if (tile.equals(TILE_PROCESSOR)) {
                sit = new ProcessorTile(mContext, this);
            } else if (tile.equals(TILE_TEMPERATURE)) {
                sit = new TemperatureTile(mContext, this);
            } else if (tile.equals(TILE_MEMORY)) {
                sit = new MemoryTile(mContext, this);
            } else if (tile.equals(TILE_STORAGE)) {
                sit = new StorageTile(mContext, this);
            }

            if (sit != null) {
                sit.setupQuickStatsTile(inflater, mContainerView);
                mSystemInfoTiles.add(sit);
            }
        }
    }

    public void pause() {
        mPaused = true;
        for (QuickStatsTile t : mSystemInfoTiles) {
            t.onPause();
        }
        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
            mUpdateThread = null;
        }
    }

    public void resume() {
        mPaused = false;
        for (QuickStatsTile t : mSystemInfoTiles) {
            t.onResume();
        }

        mUpdateThread = new PeriodicUpdateThread();
        mUpdateThread.start();
    }

    public void shutdown() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
        }
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        for (QuickStatsTile sit : mSystemInfoTiles) {
            sit.onDestroy();
        }
        mSystemInfoTiles.clear();
        mContainerView.removeAllViews();
    }

    public void setupQuickStats() {
        shutdown();
        mReceiver = new QuickStatsBroadcastReceiver();
        mReceiverMap.clear();
        mObserver = new QuickStatsObserver(mHandler);
        mObserverMap.clear();
        mTileStatusUris.clear();
        mPeriodicRefreshList.clear();

        if (mUpdateThread != null) {
            mUpdateThread.interrupt();
            mUpdateThread = null;
        }

        loadTiles();
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
        for (Uri uri : mTileStatusUris) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickStatsObserver extends ContentObserver {
        public QuickStatsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (mTileStatusUris.contains(uri)) {
                mHandler.removeMessages(MSG_UPDATE_TILES);
                mHandler.sendEmptyMessage(MSG_UPDATE_TILES);
            } else {
                ContentResolver resolver = mContext.getContentResolver();
                if (mObserverMap != null && mObserverMap.get(uri) != null) {
                    for (QuickStatsTile tile : mObserverMap.get(uri)) {
                        tile.onChangeUri(resolver, uri);
                    }
                }
            }
        }
    }

    void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        for (String action : mReceiverMap.keySet()) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerInMap(Object item, QuickStatsTile tile, HashMap map) {
        if (map.keySet().contains(item)) {
            ArrayList list = (ArrayList) map.get(item);
            if (!list.contains(tile)) {
                list.add(tile);
            }
        } else {
            ArrayList<QuickStatsTile> list = new ArrayList<QuickStatsTile>();
            list.add(tile);
            map.put(item, list);
        }
    }

    public void registerAction(String action, QuickStatsTile tile) {
        registerInMap(action, tile, mReceiverMap);
    }

    public void registerObservedContent(Uri uri, QuickStatsTile tile) {
        registerInMap(uri, tile, mObserverMap);
    }

    public void registerPeriodicUpdate(QuickStatsTile tile) {
        if (!mPeriodicRefreshList.contains(tile))
            mPeriodicRefreshList.add(tile);
    }

    private class QuickStatsBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                for (QuickStatsTile t : mReceiverMap.get(action)) {
                    t.onReceive(context, intent);
                }
            }
        }
    };

    void setBar(PanelBar bar) {
        mBar = bar;
    }

    public void updateResources() {
        mContainerView.updateResources();
        for (QuickStatsTile t : mSystemInfoTiles) {
            t.updateResources();
        }
    }

    private Handler mPeriodicUpdateHandler = new Handler() {
        public void handleMessage(Message msg) {
            for (QuickStatsTile tile : mPeriodicRefreshList)
                tile.updateResources();
        }
    };

    private class PeriodicUpdateThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    mPeriodicUpdateHandler.sendEmptyMessage(0);
                    sleep(PERIODIC_UPDATE_TIME);
                }
            } catch (InterruptedException e) {
            }
        }
    };
}
