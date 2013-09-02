/*
 * Copyright (C) 2013 The ChameleonOS Project
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

public class ThemeResources {
    public static final boolean DEBUG_THEMES = false;
    private static final boolean DBG = DEBUG_THEMES;
    private static final String TAG = "ThemeResources";

    public static final String ADVANCE_LOCKSCREEN_NAME = "advance";
    public static final String CHAOS_FRAMEWORK_NAME = "framework-res";
    public static final String CHAOS_FRAMEWORK_PACKAGE = "android";
    public static final String MIUI_FRAMEWORK_NAME = "framework-miui-res";
    public static final String MIUI_FRAMEWORK_PACKAGE = "miui";
    public static final String BOOTANI_NAME = "boots";
    public static final String FONTS_NAME = "fonts";
    public static final String ICONS_NAME = "icons";
    public static final String RINGTONES_NAME = "ringtones";
    public static final String WALLPAPER_NAME = "wallpaper/default_wallpaper.jpg";
    public static final String LOCKSCREEN_NAME = "lockscreen";
    public static final String LOCKSCREEN_WALLPAPER_NAME = "wallpaper/default_lock_wallpaper.jpg";
    public static final String MMS_PACKAGE = "com.android.mms";
    public static final String LAUNCHER_PACKAGE = "org.chameleonos.chaoslauncher";
    public static final String CONTACTS_PACKAGE = "com.android.contacts";
    public static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    public static final String DIALER_PACKAGE = "com.android.dialer";

    public static final String THEME_PATH = "/data/system/theme/";
    public static final MetaData THEME_PATH_DATA =
            new MetaData(THEME_PATH, true, true, true);

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

    // To offer better support for MIUI themes we will create a map of resources
    // that can be mapped back to a ChaOS resource.  The key for the map is the
    // package name.  The Map returned as the value uses the ChaOS resource name
    // as the key and the value returned is the MIUI resource name.
    protected static Map<String, Map<String, String>> sMiuiToChaosResourceMappings;

    static {
        sMiuiToChaosPackageMappings = new HashMap();
        sMiuiToChaosPackageMappings.put("framework-res", "com.android.systemui");
        sMiuiToChaosPackageMappings.put("android", "com.android.systemui");
        sMiuiToChaosPackageMappings.put("com.android.contacts", "com.android.phone");
        sMiuiToChaosPackageMappings.put("com.android.mms", "framework-miui-res");
        sMiuiToChaosPackageMappings.put("com.android.systemui", "framework-miui-res");
        sMiuiToChaosPackageMappings.put("com.android.dialer", "com.android.contacts");

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
        map.put("ic_qs_vibrate_on.png", "status_bar_toggle_vibrate_on.png");
        map.put("ic_qs_vibrate_off.png", "status_bar_toggle_vibrate_off.png");
        map.put("ic_qs_torch_on.png", "status_bar_toggle_torch_on.png");
        map.put("ic_qs_torch_off.png", "status_bar_toggle_torch_off.png");
        map.put("ic_qs_sync_on.png", "status_bar_toggle_sync_on.png");
        map.put("ic_qs_sync_off.png", "status_bar_toggle_sync_off.png");
        map.put("ic_qs_orientation_on.png", "status_bar_toggle_rotate_on.png");
        map.put("ic_qs_orientation_off.png", "status_bar_toggle_rotate_off.png");
        map.put("ic_qs_silent.png", "status_bar_toggle_mute_on.png");
        map.put("ic_qs_ring_on.png", "status_bar_toggle_mute_off.png");
        map.put("ic_qs_ring_vibrate_on.png", "status_bar_toggle_mute_off.png");
        map.put("ic_qs_airplane_on.png", "status_bar_toggle_flight_mode_on.png");
        map.put("ic_qs_airplane_off.png", "status_bar_toggle_flight_mode_off.png");
        map.put("ic_qs_brightness_auto_off.png", "status_bar_toggle_brightness_on.png");
        map.put("ic_qs_brightness_auto_on.png", "status_bar_toggle_brightness_auto.png");
        map.put("ic_qs_bluetooth_on.png", "status_bar_toggle_bluetooth_on.png");
        map.put("ic_qs_bluetooth_neutral.png", "status_bar_toggle_bluetooth_on.png");
        map.put("ic_qs_bluetooth_not_connected.png", "status_bar_toggle_bluetooth_on.png");
        map.put("ic_qs_bluetooth_off.png", "status_bar_toggle_bluetooth_off.png");
        map.put("ic_qs_gps_on.png", "status_bar_toggle_gps_on.png");
        map.put("ic_qs_gps_neutral.png", "status_bar_toggle_gps_on.png");
        map.put("ic_qs_location.png", "status_bar_toggle_gps_on.png");
        map.put("ic_qs_gps_off.png", "status_bar_toggle_gps_off.png");
        map.put("ic_notify_quicksettings_pressed.png", "toggle_settings_p.png");
        map.put("ic_notify_quicksettings_normal.png", "toggle_settings_n.png");
        map.put("ic_notify_open_pressed.png", "toggle_settings_p.png");
        map.put("ic_notify_open_normal.png", "toggle_settings_n.png");
        sMiuiToChaosResourceMappings.put("com.android.systemui", map);

        map = new HashMap();
        map.put("dial_num_0_wht.png", "dial_num_0_no_plus_wht.png");
        map.put("dial_num_1_wht.png", "dial_num_1_no_vm_wht.png");
        sMiuiToChaosResourceMappings.put("com.android.contacts", map);

        map = new HashMap();
        map.put("ic_menu_call.png", "call_btn_n.png");
        map.put("ic_dialog_attach.png", "insert_attachment_button_n.png");
        map.put("ic_menu_search_holo_dark.png", "ic_btn_search.png");
        sMiuiToChaosResourceMappings.put("com.android.mms", map);

        map = new HashMap();
        map.put("notification_bg_normal.9.png", "notification_item_bg_n.9.png");
        map.put("notification_bg_low_normal.9.png", "notification_item_bg_n.9.png");
        map.put("notification_bg_pressed.9.png", "notification_item_bg_p.9.png");
        map.put("notification_bg_low_pressed.9.png", "notification_item_bg_p.9.png");
        sMiuiToChaosResourceMappings.put("framework-res", map);
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
                Log.i(TAG, "Loading wrapper " + sMiuiToChaosPackageMappings.get(componentName)
                        + " for " + componentName);
            mWrapped = ThemeResourcesPackage.getThemeResources(resources,
                    sMiuiToChaosPackageMappings.get(componentName), componentName);
            supportWrapper = true;
        }
        mSupportWrapper = supportWrapper;
        checkUpdate();
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
        return new ThemeResources(null,
                resources, componentName, THEME_PATH_DATA);
    }

    public boolean checkUpdate() {
        mHasValue = hasValuesInner();
        return mPackageZipFile.checkUpdate() ||
                (mWrapped != null && mWrapped.checkUpdate());
    }

    public boolean containsEntry(String name) {
        return mPackageZipFile.containsEntry(name) ||
                (mWrapped != null && mWrapped.containsEntry(name));
    }

    public CharSequence getThemeCharSequence(String name) {
        return getThemeCharSequenceInner(name);
    }

    protected CharSequence getThemeCharSequenceInner(String name) {
        CharSequence ret = mPackageZipFile.getThemeCharSequence(name);
        if (ret == null && mHasWrapped)
            ret = mWrapped.getThemeCharSequenceInner(name);
        return ret;
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String fileName) {
        return getThemeFileStream(fileName);
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(String relativeFilePath) {
        ThemeZipFile.ThemeFileInfo info = getThemeFileStreamInner(relativeFilePath);

        if (info == null) {
            int index = relativeFilePath.indexOf("dpi/");
            if (index > 0) {
                String fileName = relativeFilePath.substring(index + 4);
                String prefix = relativeFilePath.substring(0, index + 4);
                if (DBG)
                    Log.i(TAG, "Checking for mapping of " + prefix + fileName + " for "
                            + mPackageZipFile.mPackageName);
                Map<String, String> mapping = sMiuiToChaosResourceMappings.get(mPackageZipFile.mPackageName);
                if (mapping != null && mapping.containsKey(fileName)) {
                    if (DBG)
                        Log.i(TAG, "Mapping " + fileName + " to " + mapping.get(fileName));
                    info = getThemeFileStreamInner(prefix + mapping.get(fileName));
                }
            }
        }

        if (info == null && !(this instanceof ThemeResourcesSystem)) {
            int index = relativeFilePath.indexOf("framework-res/");
            if (index >= 0) {
                String fileName = relativeFilePath.substring(index + 14);
                if (DBG)
                    Log.i(TAG, "Checking for overridden framework-res drawable " + fileName);
                info = getThemeFileStreamInner(fileName);
            }
        }

        return info;
    }

    protected ThemeZipFile.ThemeFileInfo getThemeFileStreamInner(String relativeFilePath) {
        if (DBG)
            Log.i(TAG + ":" + mPackageZipFile.mPackageName, "getThemeFileStreamInner(" + relativeFilePath + ")");
        ThemeZipFile.ThemeFileInfo info = null;
        if (!((this instanceof ThemeResourcesSystem) && relativeFilePath.contains("stat_sys_battery")))
            info = mPackageZipFile.getInputStream(relativeFilePath);

        if (info == null && mWrapped != null) {
            if (DBG)
                Log.i(TAG, "Checking wrapper for " + relativeFilePath);
            info = mWrapped.getThemeFileStreamInner(relativeFilePath);
            if (info == null) {
                int index = relativeFilePath.indexOf("dpi/");
                if (index > 0) {
                    String fileName = relativeFilePath.substring(index + 4);
                    String prefix = relativeFilePath.substring(0, index + 4);
                    if (DBG)
                        Log.i(TAG, "Checking for mapping of " + prefix + fileName + " for "
                                + mPackageZipFile.mPackageName);
                    Map<String, String> mapping = sMiuiToChaosResourceMappings.get(mPackageZipFile.mPackageName);
                    if (mapping != null && mapping.containsKey(fileName)) {
                        if (DBG)
                            Log.i(TAG, "Mapping " + fileName + " to " + mapping.get(fileName));
                        info = getThemeFileStreamInner(prefix + mapping.get(fileName));
                    }
                }
            }
        }
        return info;
    }

    public Integer getThemeInt(String name) {
        return getThemeIntInner(name, true);
    }

    public Integer getThemeInt(String name, boolean checkWrapped) {
        return getThemeIntInner(name, checkWrapped);
    }

    protected Integer getThemeIntInner(String name, boolean checkWrapped) {
        if (DBG)
            Log.i(TAG + ":" + mPackageZipFile.mPackageName, "getThemeIntInner(" + name + ")");
        Integer integer = mPackageZipFile.getThemeInt(name);
        if (integer == null && mWrapped != null && checkWrapped)
            integer = mWrapped.getThemeIntInner(name, false);
        if (integer == null && !(this instanceof ThemeResourcesSystem))
            integer = sSystem.getThemeInt(name, false);
        if (DBG)
            Log.i(TAG + ":" + mPackageZipFile.mPackageName, "getThemeIntInner=" + integer);
        return integer;
    }

    public boolean hasValues() {
        return this.mHasValue || (mWrapped != null && mWrapped.mHasValue);
    }

    protected boolean hasValuesInner() {
        return mPackageZipFile.hasValues() || (mHasWrapped && mWrapped.hasValuesInner());
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
