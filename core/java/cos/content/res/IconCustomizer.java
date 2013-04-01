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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.SystemProperties;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cos.util.CommandLineUtils;
import cos.util.DisplayUtils;

public class IconCustomizer
{
    private static final boolean DBG = false;
    private static final String TAG = "IconCustomizer";

    private static final int sAlphaShift = 24;
    private static HashMap<String, SoftReference<Bitmap>> sCache;
    private static final Canvas sCanvas = new Canvas();
    private static final int sColorShift = 8;
    private static final Resources sSystemResource = Resources.getSystem();
    private static final int sDensity = sSystemResource.getDisplayMetrics().densityDpi;
    private static final int[] sDensities = DisplayUtils.getBestDensityOrder(sDensity);
    public static final int sCustomizedIconHeight = scalePixel(90);
    public static final int sCustomizedIconWidth = scalePixel(90);
    private static Boolean sExcludeAll;
    private static Set<String> sExcludes;
    private static final int sIconWidth = scalePixel(72);
    private static final int sIconHeight = scalePixel(72);
    private static final Rect sOldBounds = new Rect();
    private static final String sPathPrefix = "/data/system/customized_icons/";
    private static final int sRGBMask = 0xFFFFFF;

    static
    {
        boolean i = true;
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
        sCache = new HashMap();
        i = !(SystemProperties.getInt("sys.ui.app-icon-background", 1) == 1);
        sExcludeAll = Boolean.valueOf(i);
    }

    private static int RGBToColor(int[] rgb) {
        return ((rgb[0] << sColorShift) + rgb[1] << sColorShift) + rgb[2];
    }

    public static void clearCache() {
        synchronized (sExcludeAll) {
            sExcludes = null;
            sCache.clear();
        }
    }

    public static void clearCustomizedIcons(String packageName) {
        if (DBG)
            Log.d(TAG, "Clearing customized icons");
        if (TextUtils.isEmpty(packageName)) {
            CommandLineUtils.rm(sPathPrefix + "*", "root");
            sCache.clear();
        } else {
            CommandLineUtils.rm(sPathPrefix + packageName + "*", "root");
            sCache.clear();
        }
    }

    private static int[] colorToRGB(int color) {
        int[] rgb = new int[3];
        rgb[0] = ((0xFF0000 & color) >> 16);
        rgb[1] = ((0xFF00 & color) >> sColorShift);
        rgb[2] = (color & 0xFF);
        return rgb;
    }

    private static Bitmap composeIcon(Bitmap base) {
        int baseWidth = base.getWidth();
        int baseHeight = base.getHeight();
        int[] basePixels = new int[baseWidth * baseHeight];
        boolean isComposed = false;
        base.getPixels(basePixels, 0, baseWidth, 0, 0, baseWidth, baseHeight);
        cutEdge(baseWidth, baseHeight, basePixels);

        if (DBG)
            Log.d(TAG, String.format("composeIcon: Creating bitmap of size (%d, %d)", sCustomizedIconWidth, sCustomizedIconHeight));
        Bitmap result = Bitmap.createBitmap(sCustomizedIconWidth, sCustomizedIconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Bitmap background = getCachedThemeIcon("icon_background.png");
        if (background != null) {
            if (DBG) Log.d(TAG, "composeIcon() drawing background");
            drawBackground(canvas, background, baseWidth, baseHeight, basePixels);
            isComposed = true;
        }

        Bitmap pattern = getCachedThemeIcon("icon_pattern.png");
        if (pattern != null) {
            if (DBG) Log.d(TAG, "composeIcon() drawing icon pattern");
            canvas.drawBitmap(pattern, 0.0F, 0.0F, null);
            isComposed = true;
        }
        canvas.drawBitmap(basePixels, 0, baseWidth, (sCustomizedIconWidth - baseWidth) / 2, 
                (sCustomizedIconHeight - baseHeight) / 2, baseWidth, baseHeight, true, null);

        Bitmap foreground = getCachedThemeIcon("icon_border.png");
        if (foreground != null) {
            if (DBG) Log.d(TAG, "composeIcon() drawing foreground");
            canvas.drawBitmap(foreground, 0.0F, 0.0F, null);
            isComposed = true;
        }
        if (result == null && DBG)
            Log.e(TAG, "composeIcon: result bitmap is null");

        if (isComposed == false)
            result = scaleBitmap(base);
        base.recycle();
        return result;
    }

    private static Bitmap composeShortcutIcon(Bitmap base) {
        int baseWidth = base.getWidth();
        int baseHeight = base.getHeight();
        int[] basePixels = new int[baseWidth * baseHeight];
        base.getPixels(basePixels, 0, baseWidth, 0, 0, baseWidth, baseHeight);
        base.recycle();
        cutEdge(baseWidth, baseHeight, basePixels);

        Bitmap result = Bitmap.createBitmap(sCustomizedIconWidth, sCustomizedIconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Bitmap background = getCachedThemeIcon("icon_shortcut.png");
        if (background != null)
            canvas.drawBitmap(background, 0.0F, 0.0F, null);
        canvas.drawBitmap(basePixels, 0, baseWidth, (sCustomizedIconWidth - baseWidth) / 2,
                (sCustomizedIconHeight - baseHeight) / 2, baseWidth, baseHeight, true, null);

        Bitmap foreground = getCachedThemeIcon("icon_shortcut_arrow.png");
        if (foreground != null)
            canvas.drawBitmap(foreground, 0.0F, 0.0F, null);
        return result;
    }

    private static void cutEdge(int baseWidth, int baseHeight, int[] basePixels) {
        Bitmap alphaCutter = getCachedThemeIcon("icon_mask.png");
        if (alphaCutter == null)
            return;

        int alphaWidth = alphaCutter.getWidth();
        int alphaHeight = alphaCutter.getHeight();
        if ((alphaWidth >= baseWidth) && (alphaHeight >= baseHeight)) {
            int[] arrayOfInt = new int[alphaWidth * alphaHeight];
            alphaCutter.getPixels(arrayOfInt, 0, baseWidth, (alphaWidth - baseWidth) / 2,
                    (alphaHeight - baseHeight) / 2, baseWidth, baseHeight);
            for (int k = -1 + baseWidth * baseHeight; k >= 0; k--)
                basePixels[k] &= sRGBMask + ((basePixels[k] >>> sAlphaShift) * (arrayOfInt[k] >>> sAlphaShift) / 255 << sAlphaShift);
        }
    }

    /* TODO: This is a mess and should be analyzed to understand what it really does */
    private static void drawBackground(Canvas canvas, Bitmap background, int baseWidth, int baseHeight, int[] basePixels) {
        int sum = 0;
        int[] sumRGB = new int[3];
        sumRGB[0] = 0;
        sumRGB[1] = 0;
        sumRGB[2] = 0;
        for (int j = -1 + baseWidth * baseHeight; j >= 0; j--)
        {
            int i8 = 0xFFFFFF & basePixels[j];
            if (i8 > 0)
            {
                int[] arrayOfInt6 = colorToRGB(i8);
                sumRGB[0] += arrayOfInt6[0];
                sumRGB[1] += arrayOfInt6[1];
                sumRGB[2] += arrayOfInt6[2];
                sum++;
            } 
        } 
        if (sum > 0)
        {
            sumRGB[0] /= sum;
            sumRGB[1] /= sum;
            sumRGB[2] /= sum;
        } 
        int color = RGBToColor(sumRGB);
        if (getSaturation(color) < 0.02D) {}
        int i3 = background.getWidth();;
        int i4 = background.getHeight();
        int[] arrayOfInt5 = new int[i3 * i4];
        float f;
        int i2 = 0;
        for (int z = 0;z < 2; z++)
        {
            int[] arrayOfInt4 = colorToRGB(i2);
            background.getPixels(arrayOfInt5, 0, i3, 0, 0, i3, i4);
            for (int i5 = -1 + i3 * i4; i5 >= 0; i5--)
            {
                int i6 = arrayOfInt5[i5];
                arrayOfInt5[i5] = (0xFF000000 & i6 | 0xFF0000 & (0xFF0000 & i6) * arrayOfInt4[0] >>> 8 | 0xFF00 & (0xFF00 & i6) * arrayOfInt4[1] >>> 8 | 0xFF & (i6 & 0xFF) * arrayOfInt4[2] >>> 8);
            } 
            int[][] arrayOfInt = new int[2][];
            int[] arrayOfInt2 = new int[2];
            arrayOfInt2[0] = 100;
            arrayOfInt2[1] = 110;
            arrayOfInt[0] = arrayOfInt2;
            int[] arrayOfInt3 = new int[2];
            arrayOfInt3[0] = 190;
            arrayOfInt3[1] = 275;
            arrayOfInt[1] = arrayOfInt3;
            int m = 0;
            for (int n = 0; n < arrayOfInt.length; n++) {
                m += arrayOfInt[n][1] - arrayOfInt[n][0];
            } 
            f = getHue(color) * m / 360.0F;
            int i1 = 0;
            while (i1 < arrayOfInt.length)
            {
                int i7 = arrayOfInt[i1][1] - arrayOfInt[i1][0];
                if (f > i7)
                {
                    f -= i7;
                    i1++;
                }
                else
                {
                    f += arrayOfInt[i1][0];
                } 
            } 
            i2 = setSaturation(setValue(setHue(color, f), 0.6F), 0.4F);
        } 
        canvas.drawBitmap(arrayOfInt5, 0, i3, 0, 0, i3, i4, true, null);
    }
 
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Canvas canvas = sCanvas;
        int i = sIconWidth;
        int j = sIconHeight;
        int k = i;
        int l = j;
        int i1;
        int j1;
        if(drawable instanceof PaintDrawable)
        {
            PaintDrawable paintdrawable = (PaintDrawable)drawable;
            paintdrawable.setIntrinsicWidth(i);
            paintdrawable.setIntrinsicHeight(j);
        } else
        if(drawable instanceof BitmapDrawable)
        {
            BitmapDrawable bitmapdrawable = (BitmapDrawable)drawable;
            if(bitmapdrawable.getBitmap().getDensity() == 0)
                bitmapdrawable.setTargetDensity(sSystemResource.getDisplayMetrics());
        }
        i1 = drawable.getIntrinsicWidth();
        j1 = drawable.getIntrinsicHeight();
        Bitmap bitmap;
        Canvas canvas1;
        int k1;
        int l1;
        if(i1 > 0 && i1 > 0)
            if(k < i1 || l < j1)
            {
                float f = (float)i1 / (float)j1;
                if(i1 > j1)
                    l = (int)((float)k / f);
                else
                if(j1 > i1)
                    k = (int)(f * (float)l);
            } else
            if(i1 < k && j1 < l)
            {
                k = i1;
                l = j1;
            }
        bitmap = Bitmap.createBitmap(i, j, android.graphics.Bitmap.Config.ARGB_8888);
        canvas1 = sCanvas;
        canvas1.setBitmap(bitmap);
        k1 = (i - k) / 2;
        l1 = (j - l) / 2;
        sOldBounds.set(drawable.getBounds());
        drawable.setBounds(k1, l1, k1 + k, l1 + l);
        drawable.draw(canvas1);
        drawable.setBounds(sOldBounds);
        return bitmap;
    }
    
    public static BitmapDrawable generateIconDrawable(Drawable base) {
        return scaleDrawable(composeIcon(drawableToBitmap(base)));
    } 
    
    public static BitmapDrawable generateShortcutIconDrawable(Drawable base) {
        return scaleDrawable(composeShortcutIcon(drawableToBitmap(base)));
    } 
    
    private static Bitmap getCachedThemeIcon(String path) {
        return getCachedThemeIcon(path, sCustomizedIconWidth, sCustomizedIconHeight);
    } 
    
    public static Bitmap getCachedThemeIcon(String path, int iconWidth, int iconHeight) {
        Bitmap localBitmap = null;
        SoftReference localSoftReference = (SoftReference)sCache.get(path);
        if (localSoftReference != null) {
            localBitmap = (Bitmap)localSoftReference.get();
        } 
        if (localBitmap == null)
        {
            localBitmap = getThemeIcon(path);
            sCache.put(path, new SoftReference(localBitmap));
        } 
        return scaleBitmap(localBitmap, iconWidth, iconHeight);
    } 
    
    public static BitmapDrawable getCustomizedIconDrawable(String packageName, String className) {
		String fileName = getFileName(packageName, className);
        if (DBG)
            Log.d(TAG, String.format("getCustomizedIconDrawable(%s, %s) - %s", packageName, className, fileName));
        Bitmap localBitmap = getThemeIcon(fileName);
        if (localBitmap == null)
        {
            String str1 = "/data/system/customized_icons/" + fileName;
            File localFile = new File(str1);
            if (localFile.exists())
            {
                localBitmap = BitmapFactory.decodeFile(str1);
                if (localBitmap == null) {
                    localFile.delete();
                } 
            } 
        } 
        return scaleDrawable(localBitmap);
    } 
    
    public static String getFileName(String packageName, String className) {
		String fileName = "";
        if (className == null) {
			fileName = String.format("%s.png", packageName);
		} else if (className.startsWith(packageName)) {
  			fileName = String.format("%s.png", packageName);
		} else {
			fileName = String.format("%s@%s.png", className, packageName);
		}
        return fileName;
    } 
    
    private static float getHue(int color) {
        int[] rgb = colorToRGB(color);
        int min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        int delta = max - min;
        float f;
        if (delta == 0)
            f = 0.0f;
        else
        {
            int m;
            for (m = 0; m < 2 && min != rgb[m]; m++);
            f = (float)(120 * ((m + 1) % 3) + (60.0f * (float)(rgb[((m + 2) % 3)] - min)) / (float)delta + (60.0f * (float)(max - rgb[((m + 1) % 3)])) / (float)delta);
        }

        return f;
    } 
    
    private static float getSaturation(int color) {
        int[] rgb = colorToRGB(color);
        int min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        float f;
        if ((max == 0) || (max == min))
            f = color;
        else
            f = (float)(max - min) / (float)max;

        return f;
    } 
    
    private static Bitmap getThemeIcon(String fileName) {
        Bitmap icon = null;
        for (int i = 0; i < sDensities.length; i++) {
            String str = DisplayUtils.getDrawbleDensityFolder(sDensities[i]) + fileName;
            icon = ThemeResources.getSystem().getIcon(sSystemResource, str);
            if (icon != null) {
                icon.setDensity(sDensities[i]);
                return icon;
            } else {
                icon = ThemeResources.getSystem().getIcon(sSystemResource, fileName);
                if (icon != null) {
                    icon.setDensity(240);
                    return icon;
                } 
            } 
        } 
        return icon;
    } 
    
    private static float getValue(int color) {
        int[] rgb = colorToRGB(color);
        return (float)Math.max(rgb[0], Math.max(rgb[1], rgb[2])) / 255.0f;
    } 
    
    public static boolean isExclude(String packageName) {
        if(sExcludes == null)
        {
          sExcludes = new HashSet();
          if(ThemeResources.getSystem().hasIcon("exclude_list.txt"))
          {
              sExcludes.add("com.android.browser");
              sExcludes.add("com.android.calendar");
              sExcludes.add("com.android.camera");
              sExcludes.add("com.android.contacts");
              sExcludes.add("com.android.deskclock");
              sExcludes.add("com.android.email");
              sExcludes.add("com.android.fileexplorer");
              sExcludes.add("com.android.gallery");
              sExcludes.add("com.android.launcher");
              sExcludes.add("com.android.mms");
              sExcludes.add("com.android.monitor");
              sExcludes.add("com.android.music");
              sExcludes.add("com.android.phone");
              sExcludes.add("com.android.providers.contacts");
              sExcludes.add("com.android.providers.downloads.ui");
              sExcludes.add("com.android.providers.telephony");
              sExcludes.add("com.android.quicksearchbox");
              sExcludes.add("com.android.settings");
              sExcludes.add("com.android.soundrecorder");
              sExcludes.add("com.android.spare_parts");
              sExcludes.add("com.android.stk");
              sExcludes.add("com.android.thememanager");
              sExcludes.add("com.android.updater");
          }
      }

      boolean flag;
      if(sExcludeAll.booleanValue() || sExcludes.contains(packageName))
          flag = true;
      else
          flag = false;
      return flag;
    } 
    
    public static void prepareCustomizedIcons(Context context) {
        prepareCustomizedIcons(context, null);
    } 
    
    public static void prepareCustomizedIcons(Context context, CustomizedIconsListener l) {
        if (DBG)
            Log.d(TAG, "Preparing customized icons");
        Intent launcherIntent = new Intent("android.intent.action.MAIN", null);
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        PackageManager pm = context.getPackageManager();
        List list = pm.queryIntentActivities(launcherIntent, 0);
        if (l != null) {
            l.beforePrepareIcon(list.size());
        } 
        int i = 0;
        Iterator iterator = list.iterator();
        while (iterator.hasNext())
        {
            ((ResolveInfo)iterator.next()).activityInfo.loadIcon(pm);
            if (l != null)
            {
                int j = i + 1;
                l.finishPrepareIcon(i);
                i = j;
            } 
        } 
        if (l != null) {
            l.finishAllIcons();
        } 
    } 
    
    public static void saveCustomizedIconBitmap(String fileName, Bitmap icon) {
        String pathName = ("/data/system/customized_icons/" + fileName);
        if (DBG)
            Log.d(TAG, String.format("saveCustomizedIconBitmap(%s, icon) - %s", fileName, pathName));
        File file = new File(pathName);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            FileUtils.setPermissions(pathName, 436, -1, -1);
        } catch (FileNotFoundException fileNotFoundException) {
            //fileNotFoundException.printStackTrace();
            return;
        } catch (IOException ioException) {
            //ioException.printStackTrace();
            return;
        }

        if (outputStream == null) {
            File parent = file.getParentFile();
            parent.mkdirs();
            FileUtils.setPermissions(parent.getPath(), 1023, -1, -1);
            try {
                outputStream = new FileOutputStream(file);
            } catch (Exception e) {
                //e.printStackTrace();
                return;
            }
        }

        icon.compress(CompressFormat.PNG, 100, outputStream);
        try {
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {}
    }

    
    private static Bitmap scaleBitmap(Bitmap icon) {
        return scaleBitmap(icon, sCustomizedIconWidth, sCustomizedIconHeight);
    } 
    
    private static Bitmap scaleBitmap(Bitmap icon, int iconWidth, int iconHeight) {
        Bitmap bitmap = null;
        if (icon == null)
            return null;

        if ((icon.getWidth() == iconWidth) || (icon.getHeight() == iconHeight))
            return icon;

        bitmap = Bitmap.createScaledBitmap(icon, iconWidth, iconHeight, true);
        bitmap.setDensity(sDensity);
        return bitmap;
    }
    
    private static BitmapDrawable scaleDrawable(Bitmap icon) {
        BitmapDrawable bd = null;
        if (icon != null)
        {
            Bitmap bitmap = scaleBitmap(icon);
            bd = new BitmapDrawable(sSystemResource, bitmap);
        } 
        return bd;
    } 
    
    private static int scalePixel(int px) {
        int density = sDensity;
        if (density <= 0)
            density = 240;
        if (sDensity == 320) {
            density = 360;
        } 
        return px * density / 240;
    } 
    
    private static int setHue(int color, float hue) {
        int[] rgb = colorToRGB(color);
        int min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        int range = max - min;
        if (range != 0) {
            while (hue < 0F) {
                hue += 360.0F;
            } 
            while (hue > 360.0F) {
                hue -= 360.0F;
            } 
            int m = (int)Math.floor(hue / 120.0F);
            float f = hue - m * 120;
            int n = (m + 2) % 3;
            rgb[n] = min;
            rgb[((n + 2) % 3)] = (int)(min + range * Math.min(f, 60.0F) / 60.0F);
            rgb[((n + 1) % 3)] = (int)(max - range * Math.max(0F, f - 60.0F) / 60.0F);
            color = RGBToColor(rgb);
        } 
        return color;
    } 
    
    private static int setSaturation(int color, float saturation) {
        int[] rgb = colorToRGB(color);
        int min = Math.min(rgb[0], Math.min(rgb[1], rgb[2]));
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        if ((max != 0) && (max != min)) {
            float currentSaturation = (float)(max - min) / (float)max;
            rgb[0] = (int)(max - saturation * max - rgb[0] / currentSaturation);
            rgb[1] = (int)(max - saturation * max - rgb[1] / currentSaturation);
            rgb[2] = (int)(max - saturation * max - rgb[2] / currentSaturation);
            color = RGBToColor(rgb);
        } 

        return color;
    } 
    
    private static int setValue(int color, float value) {
        int[] rgb = colorToRGB(color);
        int max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        if (max != 0) {
            float currentValue = (float)max / 255.0F;
            rgb[0] = (int)(value * rgb[0] / currentValue);
            rgb[1] = (int)(value * rgb[1] / currentValue);
            rgb[2] = (int)(value * rgb[2] / currentValue);
            color = RGBToColor(rgb);
        } 
        return color;
    } 
    
    public static abstract interface CustomizedIconsListener {
        public abstract void beforePrepareIcon(int paramInt);
        
        public abstract void finishAllIcons();
        
        public abstract void finishPrepareIcon(int paramInt);
    } 
} 
