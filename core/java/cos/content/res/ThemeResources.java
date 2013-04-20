/*
 * Copyright (C) 2012 The ChameleonOS Project
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

package cos.content.res;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import cos.util.ImageUtils;
import cos.util.InputStreamLoader;

public class ThemeResources
{
    public static final boolean DEBUG_THEMES = false;
    private static final boolean DBG = DEBUG_THEMES;
    private static final String TAG = "ThemeResources";

    public static final String ADVANCE_LOCKSCREEN_NAME = "advance";
    public static final String CHAOS_FRAMEWORK_NAME = "framework-res";
    public static final String CHAOS_FRAMEWORK_PACKAGE = "android";
    public static final String MIUI_FRAMEWORK_NAME = "framework-miui-res";
    public static final String MIUI_FRAMEWORK_PACKAGE = "miui";
    public static final String ICONS_NAME = "icons";
    public static final String LOCKSCREEN_NAME = "lockscreen";
    public static final String LOCKSCREEN_WALLPAPER_NAME = "lock_wallpaper";

    public static final String SYSTEM_THEME_PATH = "/system/media/theme/default/";
    public static final String THEME_PATH = "/data/system/theme/";
    public static final MetaData[] THEME_PATHS = {
            new MetaData(THEME_PATH, true, true, true) };

    public static final String WALLPAPER_NAME = "wallpaper";
    public static final String sAppliedLockstyleConfigPath = THEME_PATH + File.separator + "config.config";
    private static Drawable sLockWallpaperCache;
    private static long sLockWallpaperModifiedTime;
    private static ThemeResourcesSystem sSystem;
    protected boolean mHasValue;
    protected boolean mHasWrapped;
    protected ThemeZipFile mPackageZipFile;
    protected Resources mResources;
    protected boolean mSupportWrapper;
    protected ThemeResources mWrapped;

    protected static Map<String, String> sMiuiToChaosPackageMappings;
    static {
        sMiuiToChaosPackageMappings = new HashMap();
        sMiuiToChaosPackageMappings.put("com.android.contacts", "com.android.phone");
    }

    // To offer better support for MIUI themes we will create a map of resources
    // that can be mapped back to a ChaOS resource.  The key for the map is the
    // package name.  The Map returned as the value uses the ChaOS resource name
    // as the key and the value returned is the MIUI resource name.
    protected static Map<String, Map<String, String>> sMiuiToChaosResourceMappings;
    static {
        sMiuiToChaosResourceMappings = new HashMap();
        Map<String, String> map = new HashMap();
        map.put("status_bar_close_off.9.png", "status_bar_close_on.9.png");
        map.put("stat_2g3g_off.png", "status_bar_toggle_3g2g.png");
        map.put("stat_2g3g_on.png", "status_bar_toggle_3g2g.png");
        map.put("stat_3g_on.png", "status_bar_toggle_3g2g.png");
        map.put("stat_airplane_off.png", "status_bar_toggle_flight_mode_off.png");
        map.put("stat_airplane_on.png", "status_bar_toggle_flight_mode_on.png");
        map.put("stat_bluetooth_off.png", "status_bar_toggle_bluetooth_off.png");
        map.put("stat_bluetooth_on.png", "status_bar_toggle_bluetooth_on.png");
        map.put("stat_brightness_auto.png", "status_bar_toggle_brightness_auto.png");
        map.put("stat_brightness_mid.png", "status_bar_toggle_brightness_half.png");
        map.put("stat_brightness_off.png", "status_bar_toggle_brightness_off.png");
        map.put("stat_brightness_on.png", "status_bar_toggle_brightness_on.png");
        map.put("stat_data_off.png", "status_bar_toggle_data_off.png");
        map.put("stat_data_on.png", "status_bar_toggle_data_on.png");
        map.put("stat_flashlight_off.png", "status_bar_toggle_torch_off.png");
        map.put("stat_flashlight_on.png", "status_bar_toggle_torch_on.png");
        map.put("stat_gps_off.png", "status_bar_toggle_gps_off.png");
        map.put("stat_gps_on.png", "status_bar_toggle_gps_on.png");
        map.put("stat_lock_screen_on.png", "status_bar_toggle_lock.png");
        map.put("stat_orientation_off.png", "status_bar_toggle_rotate_off.png");
        map.put("stat_orientation_on.png", "status_bar_toggle_rotate_on.png");
        map.put("stat_ring_off.png", "status_bar_toggle_mute_on.png");
        map.put("stat_ring_on.png", "status_bar_toggle_mute_off.png");
        map.put("stat_ring_vibrate_on.png", "status_bar_toggle_vibrate_on.png");
        map.put("stat_vibrate_on.png", "status_bar_toggle_vibrate_on.png");
        map.put("stat_sync_off.png", "status_bar_toggle_sync_off.png");
        map.put("stat_sync_on.png", "status_bar_toggle_sync_on.png");
        map.put("stat_wifi_off.png", "status_bar_toggle_wifi_off.png");
        map.put("stat_wifi_on.png", "status_bar_toggle_wifi_on.png");
        map.put("stat_sys_wifi_signal_1_fully.png", "stat_sys_wifi_signal_1.png");
        map.put("stat_sys_wifi_signal_2_fully.png", "stat_sys_wifi_signal_2.png");
        map.put("stat_sys_wifi_signal_3_fully.png", "stat_sys_wifi_signal_3.png");
        map.put("stat_sys_wifi_signal_4_fully.png", "stat_sys_wifi_signal_4.png");
        map.put("stat_sys_wifi_signal_1.png", "stat_sys_wifi_signal_1_alternative.png");
        map.put("stat_sys_wifi_signal_2.png", "stat_sys_wifi_signal_2_alternative.png");
        map.put("stat_sys_wifi_signal_3.png", "stat_sys_wifi_signal_3_alternative.png");
        map.put("stat_sys_wifi_signal_4.png", "stat_sys_wifi_signal_4_alternative.png");
        map.put("stat_sys_signal_0_fully.png", "stat_sys_signal_0.png");
        map.put("stat_sys_signal_1_fully.png", "stat_sys_signal_1.png");
        map.put("stat_sys_signal_2_fully.png", "stat_sys_signal_2.png");
        map.put("stat_sys_signal_3_fully.png", "stat_sys_signal_3.png");
        map.put("stat_sys_signal_4_fully.png", "stat_sys_signal_4.png");
        map.put("stat_sys_signal_0.png", "stat_sys_signal_0_alternative.png");
        map.put("stat_sys_signal_1.png", "stat_sys_signal_1_alternative.png");
        map.put("stat_sys_signal_2.png", "stat_sys_signal_2_alternative.png");
        map.put("stat_sys_signal_3.png", "stat_sys_signal_3_alternative.png");
        map.put("stat_sys_signal_4.png", "stat_sys_signal_4_alternative.png");
        map.put("notification_panel_bg.9.png", "tracking_view_list_style_bg.9.png");
        map.put("ic_brightness_max.png", "brightness_max.png");
        map.put("ic_brightness_min.png", "brightness_min.png");
        sMiuiToChaosResourceMappings.put("com.android.systemui", map);
        map = new HashMap();
        map.put("dial_num_0_wht.png", "dial_num_0_no_plus_wht.png");
        map.put("dial_num_1_wht.png", "dial_num_1_no_vm_wht.png");
        sMiuiToChaosResourceMappings.put("com.android.contacts", map);
    }

    protected ThemeResources(ThemeResources wrapped, Resources resources, String componentName, MetaData metaData) {
        mWrapped = wrapped;
        mResources = resources;
        mPackageZipFile = ThemeZipFile.getThemeZipFile(metaData, componentName, resources);
        if (DBG)
            if (mPackageZipFile == null) {
                Log.e(TAG, String.format("mPackageZipFile null for %s", componentName));
            }
        boolean supportWrapper = false;
        if (!"icons".equals(componentName))
            supportWrapper = true;
        
        if (sMiuiToChaosPackageMappings.containsKey(componentName)) {
            if (DBG)
                Log.i(TAG, "Loading wrapper " + sMiuiToChaosPackageMappings.get(componentName) + " for " + componentName);
            mWrapped = new ThemeResourcesPackage(null, resources, sMiuiToChaosPackageMappings.get(componentName), metaData);
            supportWrapper = true;
        }
        mSupportWrapper = supportWrapper;
        checkUpdate();
    }

    public static final void clearLockWallpaperCache() {
        sLockWallpaperModifiedTime = 0L;
        sLockWallpaperCache = null;
    }

    public static final Drawable getLockWallpaperCache(Context context) {
        Drawable drawable = null;
        File file = sSystem.getLockscreenWallpaper();
        if(file != null && file.exists())
            if(sLockWallpaperModifiedTime == file.lastModified()) {
                drawable = sLockWallpaperCache;
            } else {
                sLockWallpaperModifiedTime = file.lastModified();
                sLockWallpaperCache = null;
                try {
                    DisplayMetrics displaymetrics = Resources.getSystem().getDisplayMetrics();
                    Bitmap bitmap = ImageUtils.getBitmap(new InputStreamLoader(file.getAbsolutePath()), displaymetrics.widthPixels, displaymetrics.heightPixels);
                    sLockWallpaperCache = new BitmapDrawable(context.getResources(), bitmap);
                } catch(Exception exception) {
                } catch(OutOfMemoryError outofmemoryerror) {
                }
                drawable = sLockWallpaperCache;
            }
        return drawable;
    }


    public static ThemeResources getSystem(Resources resources) {
        if (sSystem == null)
            sSystem = ThemeResourcesSystem.getTopLevelThemeResources(resources);
        return sSystem;
    }

    public static ThemeResourcesSystem getSystem() {
        return sSystem;
    }

    public static ThemeResources getTopLevelThemeResources(Resources resources, String componentName) {
        ThemeResources themeResources = null;
        for(int i = 0; i < THEME_PATHS.length; i++) {
            themeResources = new ThemeResources(themeResources, 
                    resources, componentName, THEME_PATHS[i]);
        }

        return themeResources;
    }

    public boolean checkUpdate() {
        boolean result = mPackageZipFile.checkUpdate();
        mHasWrapped = (mWrapped != null && (mSupportWrapper || !mPackageZipFile.exists()));

        if(mHasWrapped)
            if(mWrapped.checkUpdate() || result)
                result = true;
            else
                result = false;
        mHasValue = hasValuesInner();
        return result;
    }

    public boolean containsEntry(String name) {
        boolean ret = mPackageZipFile.containsEntry(name);
        if ((!ret) && (!mPackageZipFile.exists()) && (mWrapped != null))
            ret = mWrapped.containsEntry(name);
        return ret;
    }

    public CharSequence getThemeCharSequence(int id) {
        return getThemeCharSequenceInner(id);
    }

    protected CharSequence getThemeCharSequenceInner(int id) {
        CharSequence ret = mPackageZipFile.getThemeCharSequence(id);
        if(ret == null && mHasWrapped)
            ret = mWrapped.getThemeCharSequenceInner(id);
        return ret;
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String fileName) {
        return getThemeFileStream(fileName);
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(String relativeFilePath) {
        ThemeZipFile.ThemeFileInfo info = getThemeFileStreamInner(relativeFilePath);
        if (info == null && !(this instanceof ThemeResourcesSystem)) {
            int index = relativeFilePath.indexOf("dpi/");
            if(index > 0) {
                String fileName = relativeFilePath.substring(index + 4);
                String prefix = relativeFilePath.substring(0, index + 4);
                if (DBG)
                    Log.i(TAG, "Checking for mapping of " + prefix + fileName + " for " + mPackageZipFile.mPackageName);
                Map<String, String> mapping = sMiuiToChaosResourceMappings.get(mPackageZipFile.mPackageName);
                if (mapping != null && mapping.containsKey(fileName)) {
                    if (DBG)
                        Log.i(TAG, "Mapping " + fileName + " to " + mapping.get(fileName));
                    info = getThemeFileStreamInner(prefix + mapping.get(fileName));
                }
            }
        }

        return info;
    }

    protected ThemeZipFile.ThemeFileInfo getThemeFileStreamInner(String relativeFilePath) {
        if (DBG)
            Log.i(TAG, "getThemeFileStreamInnter(" + relativeFilePath + ")");
        ThemeZipFile.ThemeFileInfo ret = mPackageZipFile.getInputStream(relativeFilePath);
        if(ret == null && mHasWrapped) {
            if (DBG)
                Log.i(TAG, "Checking wrapper for " + relativeFilePath);
            ret = mWrapped.getThemeFileStreamInner(relativeFilePath);
        }
        return ret;
    }

    public Integer getThemeInt(int id) {
        return getThemeIntInner(id);
    }

    protected Integer getThemeIntInner(int id) {
        Integer integer = mPackageZipFile.getThemeInt(id);
        if(integer == null && mHasWrapped)
            integer = mWrapped.getThemeIntInner(id);
        return integer;
    }

    public boolean hasValues() {
        return this.mHasValue;
    }

    protected boolean hasValuesInner() {
        return mPackageZipFile.hasValues() || mHasWrapped && mWrapped.hasValuesInner();
    }

    protected static final class MetaData {
        public boolean supportCharSequence;
        public boolean supportFile;
        public boolean supportInt;
        public String themePath;

        public MetaData(String themePath, boolean supportInt, boolean supportCharSequence, boolean supportFile) {
            this.themePath = themePath;
            this.supportInt = supportInt;
            this.supportCharSequence = supportCharSequence;
            this.supportFile = supportFile;
        }
    }
}
