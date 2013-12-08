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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.IThemeManagerService;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.android.internal.util.XmlUtils;

import cos.util.DisplayUtils;

public class IconCustomizer {
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;
    private static final String TAG = "IconCustomizer";

    private static final String FILTERS_FILE = "icon_filters.xml";
	private static final String TAG_FILTER = "filter";

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
    private static Paint sFilteredPaint;

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
        Bitmap pattern = getCachedThemeIcon("icon_pattern.png");
        Bitmap foreground = getCachedThemeIcon("icon_border.png");
        isComposed = (background != null || pattern != null || foreground != null);
        if (isComposed) {
            if (background != null) {
                if (DBG) Log.d(TAG, "composeIcon() drawing background");
                canvas.drawBitmap(background, 0, 0, null);
            }

            if (pattern != null) {
                if (DBG) Log.d(TAG, "composeIcon() drawing icon pattern");
                canvas.drawBitmap(pattern, 0.0F, 0.0F, null);
            }
            canvas.drawBitmap(basePixels, 0, baseWidth, (sCustomizedIconWidth - baseWidth) / 2,
                    (sCustomizedIconHeight - baseHeight) / 2, baseWidth, baseHeight, true, sFilteredPaint);

            if (foreground != null) {
                if (DBG) Log.d(TAG, "composeIcon() drawing foreground");
                canvas.drawBitmap(foreground, 0.0F, 0.0F, null);
            }
        } else {
            canvas.drawBitmap(scaleBitmap(base), 0, 0, sFilteredPaint);
        }

        if (result == null && DBG)
            Log.e(TAG, "composeIcon: result bitmap is null");

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
                (sCustomizedIconHeight - baseHeight) / 2, baseWidth, baseHeight, true, sFilteredPaint);

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

    public static BitmapDrawable generateIconDrawable(Bitmap base) {
        return scaleDrawable(composeIcon(scaleBitmap(base, sIconWidth, sIconHeight)));
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
        IThemeManagerService ts = IThemeManagerService.Stub.asInterface(ServiceManager.getService("ThemeService"));
        try {
            ts.saveCustomizedIcon(fileName, icon);
        } catch (RemoteException e) {
            Log.w(TAG, "saveCustomizedIconBitmap", e);
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

    public static void loadFilters() {
        ColorMatrix cm = null;
        if (ThemeResources.getSystem() == null)
            return;
        ThemeZipFile.ThemeFileInfo info = ThemeResources.getSystem().getIconStream(FILTERS_FILE);
        if (info != null) {
            try {
                XmlPullParser xmlpullparser = XmlPullParserFactory.newInstance().newPullParser();
                xmlpullparser = XmlPullParserFactory.newInstance().newPullParser();
                BufferedInputStream bufferedinputstream = new BufferedInputStream(info.mInput, 8192);
                xmlpullparser.setInput(bufferedinputstream, null);
                cm = mergeFilters(xmlpullparser);
                bufferedinputstream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cm != null) {
            sFilteredPaint = new Paint();
            sFilteredPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        } else {
            sFilteredPaint = null;
        }
    }

    private static ColorMatrix mergeFilters(XmlPullParser xpp) {
        int eventType;
        int intValue;
        String tag = null;
        String attrName = null;
        String attr = null;

        ColorMatrix cm = new ColorMatrix();
        try {
            eventType = xpp.next();
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT)
                eventType = xpp.next();

            if (eventType != XmlPullParser.START_TAG)
                throw new XmlPullParserException("No start tag found!");
        } catch (Exception e) {
            Log.e(TAG, "mergeFilters()", e);
            return null;
        }
        try {
            while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
                try {
                    if (eventType == XmlPullParser.START_TAG) {
                        tag = xpp.getName().trim();
                        int attrCount = xpp.getAttributeCount();
                        while (attrCount-- > 0) {
                            attrName = xpp.getAttributeName(attrCount);
                            if (attrName.equals("name"))
                                attr = xpp.getAttributeValue(attrCount);
                        }
                        String content = xpp.nextText();
                        if (attr != null && content != null && content.length() > 0) {
                            content = content.trim();
                            if (TAG_FILTER.equals(tag)) {
                                if ("hue".equals(attr)) {
                                    intValue = getInt(content, 0);
                                    cm.postConcat(adjustHue(intValue));
                                } else if ("saturation".equals(attr)) {
                                    intValue = getInt(content, 100);
                                    cm.postConcat(adjustSaturation(intValue));
                                } else if ("invert".equals(attr)) {
                                    if ("true".equals(content)) {
                                        cm.postConcat(invertColors());
                                    }
                                } else if ("brightness".equals(attr)) {
                                    intValue = getInt(content, 100);
                                    cm.postConcat(adjustBrightness(intValue));
                                } else if ("contrast".equals(attr)) {
                                    intValue = getInt(content, 0);
                                    cm.postConcat(adjustContrast(intValue));
                                } else if ("alpha".equals(attr)) {
                                    intValue = getInt(content, 100);
                                    cm.postConcat(adjustAlpha(intValue));
                                } else if ("tint".equals(attr)) {
                                    intValue = Integer.valueOf(XmlUtils.convertValueToUnsignedInt(content, 0));
                                    cm.postConcat(applyTint(intValue));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "mergeFilters()", e);
                    return null;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cm;
    }

    private static int getInt(String value, int defaultValue) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static ColorMatrix adjustHue( float value )
    {
        ColorMatrix cm = new ColorMatrix();

        adjustHue(cm, value);

        return cm;
    }

    /**
     * @see http://groups.google.com/group/android-developers/browse_thread/thread/9e215c83c3819953
     * @see http://gskinner.com/blog/archives/2007/12/colormatrix_cla.html
     * @param cm
     * @param value
     */
    private static void adjustHue(ColorMatrix cm, float value)
    {
        value = cleanValue(value, 180f) / 180f * (float) Math.PI;
        if (value == 0)
        {
            return;
        }
        float cosVal = (float) Math.cos(value);
        float sinVal = (float) Math.sin(value);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        float[] mat = new float[]
        {
                lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                0f, 0f, 0f, 1f, 0f,
                0f, 0f, 0f, 0f, 1f };
        cm.postConcat(new ColorMatrix(mat));
    }

    private static float cleanValue(float p_val, float p_limit)
    {
        return Math.min(p_limit, Math.max(-p_limit, p_val));
    }

    private static ColorMatrix adjustSaturation( float saturation )
    {
        saturation = Math.min(Math.max(saturation / 100f, 0f), 2f);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(saturation);

        return cm;
    }

    private static ColorMatrix invertColors() {
        float[] colorMatrix_Negative = {
                -1.0f, 0, 0, 0, 255, //red
                0, -1.0f, 0, 0, 255, //green
                0, 0, -1.0f, 0, 255, //blue
                0, 0, 0, 1.0f, 0 //alpha
        };

        return new ColorMatrix(colorMatrix_Negative);
    }

    private static ColorMatrix adjustBrightness(float brightness) {
        brightness = Math.min(Math.max(brightness / 100f, 0f), 1f);
        ColorMatrix cm = new ColorMatrix();
        cm.setScale(brightness, brightness, brightness, 1f);

        return cm;
    }

    private static ColorMatrix adjustContrast(float contrast) {
        contrast = Math.min(Math.max(contrast / 100f, 0f), 1f) + 1f;
        float o = (-0.5f * contrast + 0.5f) * 255f;
        float[] colorMatrix_Contrast = {
                contrast, 0, 0, 0, o, //red
                0, contrast, 0, 0, o, //green
                0, 0, contrast, 0, o, //blue
                0, 0, 0, 1.0f, 0 //alpha
        };

        return new ColorMatrix(colorMatrix_Contrast);
    }

    private static ColorMatrix adjustAlpha(float alpha) {
        alpha = Math.min(Math.max(alpha / 100f, 0f), 1f);
        ColorMatrix cm = new ColorMatrix();
        cm.setScale(1f, 1f, 1f, alpha);

        return cm;
    }

    private static ColorMatrix applyTint(int color) {
        float alpha = ((color >> 24) & 0xff) / 255f;
        float red = ((color >> 16) & 0xff) * alpha;
        float green = ((color >> 8) & 0xff) * alpha;
        float blue = (color & 0xff) * alpha;
        float rscale = red / 255f;
        float gscale = green / 255f;
        float bscale = blue / 255f;

        float[] colorMatrix_Tint = {
                1f + rscale, 0, 0, 0, red, //red
                0, 1f + gscale, 0, 0, green, //green
                0, 0, 1f + bscale, 0, blue, //blue
                0, 0, 0, 1.0f, 0 //alpha
        };

        return new ColorMatrix(colorMatrix_Tint);
    }

    public static abstract interface CustomizedIconsListener {
        public abstract void beforePrepareIcon(int paramInt);

        public abstract void finishAllIcons();

        public abstract void finishPrepareIcon(int paramInt);
    }
} 
