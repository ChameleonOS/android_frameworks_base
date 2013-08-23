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

public class IconCustomizer {
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;
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
    private static Map<String, String> sIconMapping;
    private static final int sIconWidth = scalePixel(72);
    private static final int sIconHeight = scalePixel(72);
    private static final Rect sOldBounds = new Rect();
    private static final String sPathPrefix = "/data/system/customized_icons/";
    private static final int sRGBMask = 0xFFFFFF;

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
        sCache = new HashMap();
        sIconMapping = new HashMap();
        sExcludeAll = false;
        sIconMapping.put("com.android.contacts.activities.TwelveKeyDialer.png", "com.android.contacts.TwelveKeyDialer.png");
        sIconMapping.put("com.android.contacts.activities.DialtactsActivity.png", "com.android.phone.png");
        sIconMapping.put("com.android.phone.png", "com.android.contacts.TwelveKeyDialer.png");
        sIconMapping.put("com.android.camera.Camera.png", "com.android.camera.png");
        sIconMapping.put("com.android.camera.CameraLauncher.png", "com.android.camera.png");
        sIconMapping.put("com.android.dialer.DialtactsActivity.png", "com.android.phone.png");
    }

    public static void clearCache() {
        synchronized (sExcludeAll) {
            sCache.clear();
        }
    }

    private static Bitmap composeIcon(Bitmap base) {
        int baseWidth = base.getWidth();
        int baseHeight = base.getHeight();
        int[] basePixels = new int[baseWidth * baseHeight];
        boolean isComposed = false;
        base.getPixels(basePixels, 0, baseWidth, 0, 0, baseWidth, baseHeight);
        cutEdge(baseWidth, baseHeight, basePixels);

        if (DBG)
            Log.d(TAG, String.format("composeIcon: Creating bitmap of size (%d, %d)",
                    sCustomizedIconWidth, sCustomizedIconHeight));
        Bitmap result = Bitmap.createBitmap(sCustomizedIconWidth, sCustomizedIconHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Bitmap background = getCachedThemeIcon("icon_background.png");
        if (background != null) {
            if (DBG) Log.d(TAG, "composeIcon() drawing background");
            canvas.drawBitmap(background, 0, 0, null);
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
            int[] alphaPixels = new int[alphaWidth * alphaHeight];
            alphaCutter.getPixels(alphaPixels, 0, baseWidth, (alphaWidth - baseWidth) / 2,
                    (alphaHeight - baseHeight) / 2, baseWidth, baseHeight);
            for (int i = baseWidth * baseHeight - 1; i >= 0; i--)
                basePixels[i] &= sRGBMask + ((basePixels[i] >>> sAlphaShift) *
                        (alphaPixels[i] >>> sAlphaShift) / 255 << sAlphaShift);
        }
    }

    private static Bitmap drawableToBitmap(Drawable icon) {
        Canvas canvas = sCanvas;
        int targetWidth = sIconWidth;
        int targetHeight = sIconHeight;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(targetWidth);
            painter.setIntrinsicHeight(targetHeight);
        } else if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            if (bitmapDrawable.getBitmap().getDensity() == 0)
                bitmapDrawable.setTargetDensity(sSystemResource.getDisplayMetrics());
        }

        int width = targetWidth;
        int height = targetHeight;
        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();
        if (sourceWidth > 0 && sourceWidth > 0) {
            if (width < sourceWidth || height < sourceHeight) {
                float ratio = (float) sourceWidth / (float) sourceHeight;
                if (sourceWidth > sourceHeight)
                    height = (int) ((float) width / ratio);
                else if (sourceHeight > sourceWidth)
                    width = (int) (ratio * (float) height);
            } else if (sourceWidth < width && sourceHeight < height) {
                width = sourceWidth;
                height = sourceHeight;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, android.graphics.Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        int left = (targetWidth - width) / 2;
        int top = (targetHeight - height) / 2;
        sOldBounds.set(icon.getBounds());
        icon.setBounds(left, top, left + width, top + height);
        icon.draw(canvas);
        icon.setBounds(sOldBounds);
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
        Bitmap result = null;
        SoftReference soft = (SoftReference) sCache.get(path);
        if (soft != null) {
            result = (Bitmap) soft.get();
        }
        if (result == null) {
            result = getThemeIcon(path);
            sCache.put(path, new SoftReference(result));
        }
        return scaleBitmap(result, iconWidth, iconHeight);
    }

    public static BitmapDrawable getCustomizedIconDrawable(String packageName, String className) {
        String fileName = getFileName(packageName, className);
        if (DBG)
            Log.d(TAG, String.format("getCustomizedIconDrawable(%s, %s) - %s", packageName, className, fileName));
        Bitmap icon = getThemeIcon(fileName);
        if (icon == null) {
            String mappingName = (String) sIconMapping.get(fileName);
            if (mappingName != null) {
                icon = getThemeIcon(mappingName);
            }
        }
        if (icon == null) {
            String pathName = "/data/system/customized_icons/" + fileName;
            File localFile = new File(pathName);
            if (localFile.exists()) {
                icon = BitmapFactory.decodeFile(pathName);
                if (icon == null) {
                    localFile.delete();
                }
            }
        }
        return scaleDrawable(icon);
    }

    public static String getFileName(String packageName, String className) {
        String fileName = "";
        if (className == null) {
            fileName = String.format("%s.png", packageName);
        } else if (className.startsWith(packageName)) {
            fileName = String.format("%s.png", className);
            String mapped = (String) sIconMapping.get(fileName);
            if (mapped != null)
                fileName = mapped;
            else
                fileName = String.format("%s.png", packageName);
        } else {
            fileName = String.format("%s@%s.png", className, packageName);
        }
        return fileName;
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

    public static boolean isExclude(String packageName) {
        return false;
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
        while (iterator.hasNext()) {
            ((ResolveInfo) iterator.next()).activityInfo.loadIcon(pm);
            if (l != null) {
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
        } catch (Exception e) {
        }
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
        if (icon != null)
            return new BitmapDrawable(sSystemResource, scaleBitmap(icon));

        return null;
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

    public static abstract interface CustomizedIconsListener {
        public abstract void beforePrepareIcon(int paramInt);

        public abstract void finishAllIcons();

        public abstract void finishPrepareIcon(int paramInt);
    }
} 
