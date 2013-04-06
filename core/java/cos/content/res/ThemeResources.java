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
import cos.util.ImageUtils;
import cos.util.InputStreamLoader;

public class ThemeResources
{
    public static final boolean DEBUG_THEMES = false;
    private static final boolean DBG = DEBUG_THEMES;
    private static final String TAG = "ThemeResources";

    public static final String ADVANCE_LOCKSCREEN_NAME = "advance";
    public static final String FRAMEWORK_NAME = "framework-res";
    public static final String FRAMEWORK_PACKAGE = "android";
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
        return getThemeFileStreamInner(relativeFilePath);
    }

    protected ThemeZipFile.ThemeFileInfo getThemeFileStreamInner(String relativeFilePath) {
        ThemeZipFile.ThemeFileInfo ret = mPackageZipFile.getInputStream(relativeFilePath);
        if(ret == null && mHasWrapped)
            ret = mWrapped.getThemeFileStreamInner(relativeFilePath);
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
