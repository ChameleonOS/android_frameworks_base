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

/** {@hide} */
interface IThemeManagerService
{
    void applyTheme(String themeURI);
    void removeTheme();
    void removeThemeAndApply();
    void applyInstalledTheme();
    void applyThemeIcons();
    void applyThemeWallpaper();
    void applyThemeSystemUI();
    void applyThemeFramework();
    void applyThemeRingtones();
    void applyThemeLockscreen();
    void applyThemeBootanimation();
    void applyThemeMms();
    void applyThemeFont();

    void resetThemeIcons();
    void resetThemeWallpaper();
    void resetThemeSystemUI();
    void resetThemeFramework();
    void resetThemeRingtones();
    void resetThemeLockscreen();
    void resetThemeBootanimation();
    void resetThemeMms();
    void resetThemeFont();
}

