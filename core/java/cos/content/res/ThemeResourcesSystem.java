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

public final class ThemeResourcesSystem extends ThemeResources {
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;
    private static final String TAG = "ThemeResourcesSystem";
    private static ThemeResources sIcons;
    private static ThemeResources sSystemUI;
    protected String mThemePath;

    protected ThemeResourcesSystem(ThemeResourcesSystem wrapped,
                                   Resources resources,
                                   ThemeResources.MetaData metaData) {
        super(wrapped, resources, "framework-res", metaData);
        if (DBG)
            Log.d(TAG, String.format("Creating ThemeResourcesSystem for %s", metaData.themePath));
        mThemePath = metaData.themePath;
    }

    private ThemeZipFile.ThemeFileInfo getThemeFileStreamSystem(String relativeFilePath, String name) {
        return getThemeFileStreamInner(relativeFilePath);
    }

    public static ThemeResourcesSystem getTopLevelThemeResources(Resources resources) {
        if (sIcons == null) {
            sIcons = ThemeResources.getTopLevelThemeResources(resources, "icons");
            IconCustomizer.loadFilters();
        }
        return new ThemeResourcesSystem(null, resources, THEME_PATH_DATA);
    }

    public boolean checkUpdate() {
        if (sIcons.checkUpdate()) {
            IconCustomizer.loadFilters();
        }
        if (sSystemUI != null)
            sSystemUI.checkUpdate();
        return super.checkUpdate();
    }

    public Bitmap getIcon(Resources resources, String name) {
        if (DBG)
            Log.d(TAG, String.format("getIcon for %s", name));
        Bitmap bitmap = null;
        ThemeZipFile.ThemeFileInfo themefileinfo = null;
        BitmapFactory.Options options = null;
        try {
            themefileinfo = getIconStream(name);
            if (themefileinfo == null)
                return null;
            options = new android.graphics.BitmapFactory.Options();
            if (themefileinfo.mDensity > 0)
                options.inDensity = themefileinfo.mDensity;
            bitmap = BitmapFactory.decodeStream(themefileinfo.mInput, null, options);
        } catch (OutOfMemoryError oome) {
            oome.printStackTrace();
        } finally {
            try {
                if (themefileinfo != null)
                    themefileinfo.mInput.close();
            } catch (IOException ioe) {
            }
        }

        return bitmap;
    }

    public ThemeZipFile.ThemeFileInfo getIconStream(String relativeFilePath) {
        return sIcons.getThemeFileStream(relativeFilePath);
    }

    public File getLockscreenWallpaper() {
        File file = new File(mThemePath + "lock_wallpaper");
        if (((file == null) || (!file.exists())) && (mWrapped != null))
            file = ((ThemeResourcesSystem) mWrapped).getLockscreenWallpaper();
        return file;
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String relativeFilePath) {
        String name = relativeFilePath.substring(1 + relativeFilePath.lastIndexOf('/'));
        if (DBG) Log.d(TAG, String.format("getThemeFileStream(%d, %s)", cookieType, relativeFilePath));

        return getThemeFileStreamSystem(relativeFilePath, name);
    }

    public boolean hasIcon(String name) {
        return sIcons.containsEntry(name);
    }

    public boolean hasValues() {
        return super.hasValues();
    }

    public void resetIcons() {
        sIcons.checkUpdate();
    }
}
