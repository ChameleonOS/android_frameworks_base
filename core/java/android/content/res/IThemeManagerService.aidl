/**
 * Copyright (c) 2007, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.res;

import android.graphics.Bitmap;

/** {@hide} */
interface IThemeManagerService
{
    void applyDefaultTheme();
    void applyTheme(String themeURI, in List<String> excludedItems, boolean applyFont, boolean scaleBoot, boolean removeExistingTheme);
    void removeTheme(boolean removeFonts);
    void removeThemeAndApply(boolean removeFonts);
    void applyInstalledTheme();
    void applyInstalledThemeReboot();
    void applyThemeIcons(String themeURI);
    void applyThemeWallpaper(String themeURI);
    void applyThemeSystemUI(String themeURI);
    void applyThemeFramework(String themeURI);
    void applyThemeRingtone(String themeURI);
    void applyThemeLockscreenWallpaper(String themeURI);
    void applyThemeBootanimation(String themeURI, boolean scale);
    void applyThemeMms(String themeURI);
    void applyThemeFont(String themeURI);
    void applyThemeFontReboot(String themeURI);
    void applyThemeContacts(String themeURI);
    void applyThemeDialer(String themeURI);
    void updateSystemUI();
    void saveCustomizedIcon(String fileName, in Bitmap icon);

    void resetThemeIcons();
    void resetThemeWallpaper();
    void resetThemeSystemUI();
    void resetThemeFramework();
    void resetThemeRingtone();
    void resetThemeLockscreenWallpaper();
    void resetThemeBootanimation();
    void resetThemeMms();
    void resetThemeFont();
    void resetThemeFontReboot();
    void resetThemeContacts();
    void resetThemeDialer();
}

