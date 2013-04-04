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

package android.app;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import cos.content.res.ExtraConfiguration;
import cos.content.res.IconCustomizer;
import cos.content.res.ThemeResources;
import cos.content.res.ThemeResourcesSystem;

import java.io.File;

public class ThemeHelper {
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;

    // TODO: change out framework-miui-res.apk for our own if needed
    public static final String MIUI_RES_PATH = "/system/framework/framework-miui-res.apk";
    private static final String MIUI_SCREENSHOT_MODE_RES_PATH = "/data/system/themeScreenshotMode";
    private static final String TAG = "ThemeHelper";

    public static void addExtraAssetPaths(AssetManager am) {
        am.addAssetPath(MIUI_RES_PATH);
    }

    public static void copyExtraConfigurations(Configuration srcConfig, Configuration desConfig) {
        desConfig.extraConfig.mThemeChanged = srcConfig.extraConfig.mThemeChanged;
    }

    public static Drawable getDrawable(PackageManager pm, String packageName, int resid, 
            ApplicationInfo appInfo, PackageItemInfo info, boolean customized) {
        Drawable drawable;
        if(!customized || info == null)
            drawable = pm.getDrawable(packageName, resid, appInfo);
        else
            drawable = getDrawable(pm, packageName, resid, appInfo, info.name);
        return drawable;
    }

    public static Drawable getDrawable(PackageManager pm, String packageName, int resid, 
            ApplicationInfo appInfo, String activityName) {
        String fileName = IconCustomizer.getFileName(packageName, activityName);
        if (DBG)
            Log.d(TAG, String.format("getDrawable: Get drawable for %s.%s, filename=%s", packageName, activityName, fileName));
        ApplicationPackageManager.ResourceName resourcename = new ApplicationPackageManager.ResourceName(fileName, resid);
        Drawable drawable = ApplicationPackageManager.getCachedIcon(resourcename);
        if(drawable != null) {
            if (DBG) Log.d(TAG, String.format("getDrawable: Returning cached drawable for %s.%s", packageName, activityName));
            return drawable;
        }
        
        if(IconCustomizer.isExclude(packageName) && ThemeResources.getSystem().hasIcon("icon_mask.png"))
            drawable = pm.getDrawable(packageName, resid, appInfo);
        if(drawable != null) {
            ApplicationPackageManager.putCachedIcon(resourcename, drawable);
        } else {
            drawable = IconCustomizer.getCustomizedIconDrawable(packageName, activityName);
            if(drawable == null) {
                drawable = pm.getDrawable(packageName, resid, appInfo);
                if(drawable != null) {
                    Log.d(TAG, "getDrawable: Generate customized icon for " + fileName);
                    drawable = IconCustomizer.generateIconDrawable(drawable);
                    IconCustomizer.saveCustomizedIconBitmap(fileName, ((BitmapDrawable)drawable).getBitmap());
                }
            }
        }

        return drawable;
    }

    public static void handleExtraConfigurationChanges(int changes) {
        if ((0x80000000 & changes) != 0) {
            Canvas.freeCaches();
            IconCustomizer.clearCache();
        }
    }

    public static void handleExtraConfigurationChanges(int changes, 
            Configuration config, Context context, Handler handler) {
        if ((0x80000000 & changes) != 0) {
            ExtraConfiguration.addNeedRestartActivity(config.extraConfig.mThemeChangedFlags);
            handleExtraConfigurationChanges(changes);
        }
    }

    public static boolean isCompatibilityMode(int appFlags) {
        return ((0x8000000 & appFlags) != 0);
    }

    public static boolean isCustomizedIcon(IntentFilter filter)
    {
        if (filter != null)
        {
            for(int j = 0; j < filter.countCategories(); j++)
                if ("android.intent.category.LAUNCHER".equals(filter.getCategory(j)))
                    return true;
        }

        return false;
    }

    public static boolean isScreenshotMode() {
        return (new File(MIUI_SCREENSHOT_MODE_RES_PATH)).exists();
    }

    public static boolean needRestartActivity(String packageName, int changes, Configuration config)
    {
        return (changes == 0x80000000 &&
            !ExtraConfiguration.removeNeedRestartActivity(packageName) && 
            !ExtraConfiguration.needRestartActivity(packageName, config.extraConfig.mThemeChangedFlags));
    }

    public static Integer parseDimension(String dimension)
    {
        int intPos = -4;
        int dotPos = -3;
        int fractionPos = -2;
        int unitPos = -1;
        int n = 0;
        String units;
        String value;
        for (n = 0; n < dimension.length(); n++) {
            char c = dimension.charAt(n);
            if ((intPos == -4) && (c >= '0') && (c <= '9'))
                intPos = n;
            if ((dotPos == -3) && (c == '.'))
                dotPos = n;
            if ((dotPos != -3) && (c >= '0') && (c <= '9'))
                fractionPos = n;
            if ((unitPos == -1) && (c >= 'a') && (c <= 'z'))
                unitPos = n;
        }
        value = dimension.substring(0, unitPos);
        units = dimension.substring(unitPos);

        if (DBG)
            Log.d(TAG, String.format("parseDimension(%s): value=%s, units=%s", dimension, value, units));
        
        try {
        	int dim = floatToComplex(Float.parseFloat(value));
        	DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        	if (units.equals("dp") || units.equals("dip")) {
        		dim |= (TypedValue.COMPLEX_UNIT_DIP << TypedValue.COMPLEX_UNIT_SHIFT) & TypedValue.COMPLEX_UNIT_MASK;
        	} else if (units.equals("sp")) {
        		dim |= (TypedValue.COMPLEX_UNIT_SP << TypedValue.COMPLEX_UNIT_SHIFT) & TypedValue.COMPLEX_UNIT_MASK;
        	} else if (units.equals("in")) {
        		dim |= (TypedValue.COMPLEX_UNIT_IN << TypedValue.COMPLEX_UNIT_SHIFT) & TypedValue.COMPLEX_UNIT_MASK;
        	} else if (units.equals("mm")) {
        		dim |= (TypedValue.COMPLEX_UNIT_MM << TypedValue.COMPLEX_UNIT_SHIFT) & TypedValue.COMPLEX_UNIT_MASK;
        	} else if (units.equals("pt")) {
        		dim |= (TypedValue.COMPLEX_UNIT_PT << TypedValue.COMPLEX_UNIT_SHIFT) & TypedValue.COMPLEX_UNIT_MASK;
        	}
            if (DBG)
                Log.i(TAG, String.format("parseDimension: returning %d for %s.", dim, dimension));
        	return dim;
        } catch (NumberFormatException e) {
            if (DBG) Log.e(TAG, "parseDimension: NumberFormatException", e);
        	return 0;
        }
    }

    /**
     * converts a floating point value into a complex int value used when expressing
     * dimensions in a TypedValue.
     */
    private static int floatToComplex(float value) {
        int ret = 0;
        boolean neg = value < 0;
        if (neg)
            value = -value;
        long bits = (long)(value*(1<<23)+0.5f);
        int radix;
        int shift;
        if ((bits & 0x7fffff) == 0) {
            radix = TypedValue.COMPLEX_RADIX_23p0;
            shift = 23;
        } else if ((bits & 0xffffffffff800000L) == 0) {
            radix = TypedValue.COMPLEX_RADIX_0p23;
            shift = 0;
        } else if ((bits & 0xffffffff80000000L) == 0) {
            radix = TypedValue.COMPLEX_RADIX_8p15;
            shift = 8;
        } else if ((bits&0xffffff8000000000L) == 0) {
            radix = TypedValue.COMPLEX_RADIX_16p7;
            shift = 16;
        } else {
            radix = TypedValue.COMPLEX_RADIX_23p0;
            shift = 23;
        }

        int mantissa = (int)((bits >> shift) & TypedValue.COMPLEX_MANTISSA_MASK);
        if (neg)
            mantissa = (-mantissa) & TypedValue.COMPLEX_MANTISSA_MASK;

        ret |= (radix<<TypedValue.COMPLEX_RADIX_SHIFT) | (mantissa << TypedValue.COMPLEX_MANTISSA_SHIFT);

        return ret;
    }
}
