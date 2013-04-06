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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class ThemeResourcesSystem extends ThemeResources
{
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;
    private static final String TAG = "ThemeResourcesSystem";
    private static ThemeResources sIcons;
    private static ThemeResources sLockscreen;

    private static ThemeResources sSystemUI;
    protected String mThemePath;

    protected ThemeResourcesSystem(ThemeResourcesSystem wrapped, Resources resources, ThemeResources.MetaData metaData) {
        super(wrapped, resources, "framework-res", metaData);
        if (DBG)
            Log.d(TAG, String.format("Creating ThemeResourcesSystem for %s", metaData.themePath));
        mThemePath = metaData.themePath;
    }

    private ThemeZipFile.ThemeFileInfo getThemeFileStreamMIUI(String relativeFilePath, String name) {
        ThemeZipFile.ThemeFileInfo info = null;
        if (name.startsWith("lock_screen_")) {
            info = sLockscreen.getThemeFileStream(relativeFilePath);
            if (info == null)
                info = sLockscreen.getThemeFileStream(name);
        } else if (name.startsWith("status_bar_toggle_")) {
            if (sSystemUI == null)
                sSystemUI = ThemeResources.getTopLevelThemeResources(mResources, "com.android.systemui");
            info = sSystemUI.getThemeFileStream(relativeFilePath);
        }

        return info;
    }

    private ThemeZipFile.ThemeFileInfo getThemeFileStreamSystem(String relativeFilePath, String name) {
        ThemeZipFile.ThemeFileInfo themefileinfo = null;

        themefileinfo = getThemeFileStreamInner(relativeFilePath);
        return themefileinfo;
    }

    public static ThemeResourcesSystem getTopLevelThemeResources(Resources resources) {
        sIcons = ThemeResources.getTopLevelThemeResources(resources, "icons");
        sLockscreen = ThemeResources.getTopLevelThemeResources(resources, "lockscreen");
        ThemeResourcesSystem themeresourcessystem = null;
        for(int i = 0; i < THEME_PATHS.length; i++) {
            themeresourcessystem = new ThemeResourcesSystem(themeresourcessystem, resources, THEME_PATHS[i]);
        }

        return themeresourcessystem;
    }

    public boolean checkUpdate() {
        sIcons.checkUpdate();
        sLockscreen.checkUpdate();
        if (sSystemUI != null)
            sSystemUI.checkUpdate();
        return super.checkUpdate();
    }

    public boolean containsAwesomeLockscreenEntry(String entry) {
        return sLockscreen.containsEntry("advance/" + entry);
    }

    public ThemeZipFile.ThemeFileInfo getAwesomeLockscreenFileStream(String name) {
        return sLockscreen.getThemeFileStream("advance/" + name);
    }

    public Bitmap getIcon(Resources resources, String name) {
        if (DBG)
            Log.d(TAG, String.format("getIcon for %s", name));
        Bitmap bitmap = null;
        ThemeZipFile.ThemeFileInfo themefileinfo = null;
        BitmapFactory.Options options = null;
        try {
            themefileinfo = getIconStream(name);
            if(themefileinfo == null)
                return null;
            options = new android.graphics.BitmapFactory.Options();
            if(themefileinfo.mDensity > 0)
                options.inDensity = themefileinfo.mDensity;
            bitmap = BitmapFactory.decodeStream(themefileinfo.mInput, null, options);
        } catch (OutOfMemoryError oome) {
            oome.printStackTrace();
        } finally {
            try{
                if (themefileinfo != null)
                    themefileinfo.mInput.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return bitmap;
    }

    public ThemeZipFile.ThemeFileInfo getIconStream(String relativeFilePath) {
        return sIcons.getThemeFileStream(relativeFilePath);
    }

    public ThemeZipFile.ThemeFileInfo getLockscreenStream(String relativeFilePath) {
        return sLockscreen.getThemeFileStream(relativeFilePath);
    }

    public File getLockscreenWallpaper() {
        File file = new File(mThemePath + "lock_wallpaper");
        if (((file == null) || (!file.exists())) && (mWrapped != null))
            file = ((ThemeResourcesSystem)mWrapped).getLockscreenWallpaper();
        return file;
    }

    public CharSequence getThemeCharSequence(int id) {
        CharSequence charsequence = sLockscreen.getThemeCharSequence(id);
        if(charsequence == null)
            charsequence = getThemeCharSequenceInner(id);
        return charsequence;
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String relativeFilePath) {
        String name = relativeFilePath.substring(1 + relativeFilePath.lastIndexOf('/'));
        ThemeZipFile.ThemeFileInfo themefileinfo;
        if (DBG) Log.d(TAG, String.format("getThemeFileStream(%d, %s)", cookieType, relativeFilePath));
        if(2 == cookieType)
            themefileinfo = getThemeFileStreamMIUI(relativeFilePath, name);
        else
            themefileinfo = getThemeFileStreamSystem(relativeFilePath, name);
        return themefileinfo;
    }

    public Integer getThemeInt(int id) {
        Integer ret = sLockscreen.getThemeInt(id);
        if (ret == null)
            ret = getThemeIntInner(id);
        return ret;
    }

    public boolean hasAwesomeLockscreen() {
        return sLockscreen.containsEntry("advance/manifest.xml");
    }

    public boolean hasIcon(String name) {
        return sIcons.containsEntry(name);
    }

    public boolean hasValues() {
        return (super.hasValues() || sLockscreen.hasValues());
    }

    public void resetIcons() {
        sIcons.checkUpdate();
    }
}
