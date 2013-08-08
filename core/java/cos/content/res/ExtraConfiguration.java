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

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExtraConfiguration
        implements java.lang.Comparable<Object> {
    public static final String CONTACTS_PKG_NAME = "com.android.contacts";

    public static final String DIALER_PKG_NAME = "com.android.dialer";

    public static final String LAUNCHER_PKG_NAME = "org.chameleonos.chaoslauncher";

    public static final String MMS_PKG_NAME = "com.android.mms";

    public static final String SETTINGS_PKG_NAME = "com.android.settings";

    public static final String SYSTEMUI_PKG_NAME = "com.android.systemui";


    public static final long THEME_FLAG_FRAMEWORK = 0x00000001;
    public static final long THEME_FLAG_WALLPAPER = 0x00000002;
    public static final long THEME_FLAG_LOCKSCREEN = 0x00000004;
    public static final long THEME_FLAG_ICON = 0x00000008;
    public static final long THEME_FLAG_FONT = 0x00000010;
    public static final long THEME_FLAG_BOOT_ANIMATION = 0x00000020;
    public static final long THEME_FLAG_BOOT_AUDIO = 0x00000040;
    public static final long THEME_FLAG_MMS = 0x00000080;
    public static final long THEME_FLAG_RINGTONE = 0x00000100;
    public static final long THEME_FLAG_NOTIFICATION = 0x00000200;
    public static final long THEME_FLAG_ALARM = 0x00000400;
    public static final long THEME_FLAG_CONTACT = 0x00000800;
    public static final long THEME_FLAG_LOCKSTYLE = 0x00001000;
    public static final long THEME_FLAG_STATUSBAR = 0x00002000;
    public static final long THEME_FLAG_LAUNCHER = 0x00004000;
    public static final long THEME_FLAG_AUDIO_EFFECT = 0x00008000;
    public static final long THEME_FLAG_CLOCK = 0x00010000;
    public static final long THEME_FLAG_PHOTO_FRAME = 0x00020000;
    public static final long THEME_FLAG_FONT_FALLBACK = 0x00040000;
    public static final long THEME_FONTS_FLAG = 0x00040010;
    public static final long THEME_FLAG_LAST = 0x00040000;
    public static final long THEME_FLAG_THIRD_APP = 0x10000000;

    public static final long SYSTEM_INTRESTE_CHANGE_FLAG = THEME_FLAG_THIRD_APP |
            THEME_FLAG_FONT_FALLBACK | THEME_FLAG_LAUNCHER | THEME_FLAG_STATUSBAR |
            THEME_FLAG_LOCKSTYLE | THEME_FLAG_CONTACT | THEME_FLAG_MMS |
            THEME_FLAG_FONT | THEME_FLAG_ICON | THEME_FLAG_FRAMEWORK; //0x10047899;

    private static final long THEME_LAUNCHER_FLAGS = THEME_FLAG_LAUNCHER |
            THEME_FLAG_FONT | THEME_FLAG_ICON | THEME_FLAG_FRAMEWORK;

    private static final long THEME_CONTACTS_FLAGS = THEME_FLAG_CONTACT |
            THEME_FLAG_FONT | THEME_FLAG_FRAMEWORK;

    private static final long THEME_MMS_FLAGS = THEME_FLAG_MMS |
            THEME_FLAG_FONT | THEME_FLAG_FRAMEWORK;

    private static final long THEME_SETTINGS_FLAGS = THEME_FLAG_FONT |
            THEME_FLAG_ICON | THEME_FLAG_FRAMEWORK;

    private static final long THEME_STATUSBAR_FLAGS = THEME_FLAG_STATUSBAR |
            THEME_FLAG_FONT | THEME_FLAG_ICON | THEME_FLAG_FRAMEWORK;

    private static final Set<String> needRestartActivitySet = Collections.synchronizedSet(new HashSet());
    public int mThemeChanged;
    public long mThemeChangedFlags;

    public static void addNeedRestartActivity(long flags) {
        if (needRestartLauncher(flags))
            needRestartActivitySet.add(LAUNCHER_PKG_NAME);
        if (needRestartSettings(flags))
            needRestartActivitySet.add(SETTINGS_PKG_NAME);
        if (needRestartMms(flags))
            needRestartActivitySet.add(MMS_PKG_NAME);
        if (needRestartContacts(flags)) {
            needRestartActivitySet.add(CONTACTS_PKG_NAME);
            needRestartActivitySet.add(DIALER_PKG_NAME);
        }
    }

    public static boolean needNewResources(int configChanges) {
        return ((0x80000000 & configChanges) != 0);
    }

    public static boolean needRestart3rdApp(long themeChangeFlags) {
        return ((0x10000011 & themeChangeFlags) != 0L);
    }

    public static boolean needRestartActivity(String packageName, long paramLong) {
        if (packageName != null) {
            if (packageName.startsWith(LAUNCHER_PKG_NAME))
                return needRestartLauncher(paramLong);
            else if (packageName.startsWith(SETTINGS_PKG_NAME))
                return needRestartSettings(paramLong);
            else if (packageName.startsWith(MMS_PKG_NAME))
                return needRestartMms(paramLong);
            else if (packageName.startsWith(CONTACTS_PKG_NAME) || packageName.startsWith((DIALER_PKG_NAME)))
                return needRestartContacts(paramLong);
            else
                return needRestart3rdApp(paramLong);
        }

        return false;
    }

    public static boolean needRestartContacts(long flags) {
        return ((THEME_CONTACTS_FLAGS & flags) != 0L);
    }

    public static boolean needRestartLauncher(long flags) {
        return ((THEME_LAUNCHER_FLAGS & flags) != 0L);
    }

    public static boolean needRestartMms(long flags) {
        return ((THEME_MMS_FLAGS & flags) != 0L);
    }

    public static boolean needRestartSettings(long flags) {
        return ((THEME_SETTINGS_FLAGS & flags) != 0L);
    }

    public static boolean needRestartStatusBar(long flags) {
        return ((THEME_STATUSBAR_FLAGS & flags) != 0L);
    }

    public static boolean removeNeedRestartActivity(String pkgName) {
        return needRestartActivitySet.remove(pkgName);
    }

    public int compareTo(Object that) {
        if (!(that instanceof ExtraConfiguration)) {
            throw new ClassCastException();
        } else {
            return compareTo((ExtraConfiguration) that);
        }
    }

    public int compareTo(ExtraConfiguration that) {
        return this.mThemeChanged - that.mThemeChanged;
    }

    public int diff(ExtraConfiguration config) {
        if (mThemeChanged < config.mThemeChanged)
            return 0x80000000;

        return 0;
    }

    public int hashCode() {
        return this.mThemeChanged + (int) mThemeChangedFlags;
    }

    public void readFromParcel(Parcel parcel) {
        mThemeChanged = parcel.readInt();
        mThemeChangedFlags = parcel.readLong();
    }

    public void setTo(ExtraConfiguration newConfig) {
        mThemeChanged = newConfig.mThemeChanged;
        mThemeChangedFlags = newConfig.mThemeChangedFlags;
    }

    public void setToDefaults() {
        mThemeChanged = 0;
        mThemeChangedFlags = 0L;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" themeChanged=");
        sb.append(mThemeChanged);
        sb.append(" themeChangedFlags=");
        sb.append(mThemeChangedFlags);
        return sb.toString();
    }

    public int updateFrom(ExtraConfiguration delta) {
        int i = 0;
        if (mThemeChanged < delta.mThemeChanged) {
            i = 0x0 | 0x80000000;
            mThemeChanged = delta.mThemeChanged;
            mThemeChangedFlags = delta.mThemeChangedFlags;
        }
        return i;
    }

    public void updateTheme(long flags) {
        mThemeChanged = (1 + mThemeChanged);
        mThemeChangedFlags = flags;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mThemeChanged);
        dest.writeLong(mThemeChangedFlags);
    }
}
