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

package com.android.internal.util.chaos;

import android.annotation.ChaosLab;
import android.annotation.ChaosLab.Classification;

import java.util.ArrayList;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QStatsConstants {
    public static final String TILE_BATTERY = "tileBattery";
    public static final String TILE_WIFI = "tileWifi";
    public static final String TILE_MESSAGING = "tileMessaging";
    public static final String TILE_CALLS = "tileCalls";
    public static final String TILE_PROCESSOR = "tileProcessor";
    public static final String TILE_TEMPERATURE = "tileTemperature";
    public static final String TILE_STORAGE = "tileStorage";
    public static final String TILE_MEMORY = "tileMemory";

    public static final String TILE_DELIMITER = "|";
    public static ArrayList<String> TILES_DEFAULT = new ArrayList<String>();

    static {
        TILES_DEFAULT.add(TILE_BATTERY);
        TILES_DEFAULT.add(TILE_WIFI);
        TILES_DEFAULT.add(TILE_PROCESSOR);
        TILES_DEFAULT.add(TILE_TEMPERATURE);
        TILES_DEFAULT.add(TILE_STORAGE);
        TILES_DEFAULT.add(TILE_MEMORY);
        TILES_DEFAULT.add(TILE_MESSAGING);
        TILES_DEFAULT.add(TILE_CALLS);
    }
}
