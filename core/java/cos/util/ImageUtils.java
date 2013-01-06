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

package cos.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import libcore.io.Streams;

public class ImageUtils
{
    private static byte[] PNG_HEAD_FORMAT = {-119, 80, 78, 71, 13, 10, 26, 10};

    public static int computeSampleSize(InputStreamLoader streamLoader, int pixelSize)
    {
        int roundedSize = 1;
        if (pixelSize > 0)
        {
            BitmapFactory.Options options = getBitmapSize(streamLoader);
            double size = Math.sqrt(options.outWidth * options.outHeight / pixelSize);
            while (roundedSize * 2 <= size)
                roundedSize <<= 1;
        }
        return roundedSize;
    }

    public static boolean cropBitmapToAnother(Bitmap srcBmp, Bitmap destBmp, boolean recycleSrcBmp)
    {
        if(srcBmp != null && destBmp != null)
        {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();
            int destWidth = destBmp.getWidth();
            int destHeight = destBmp.getHeight();
            Matrix matrix = new Matrix();
            float ratio = Math.max((1.0F * (float)destWidth) / (float)srcWidth, (1.0F * (float)destHeight) / (float)srcHeight);
            matrix.setScale(ratio, ratio);
            matrix.postTranslate(((float)destWidth - ratio * (float)srcWidth) / 2.0F, ((float)destHeight - ratio * (float)srcHeight) / 2.0F);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            paint.setDither(true);
            (new Canvas(destBmp)).drawBitmap(srcBmp, matrix, paint);
            if(recycleSrcBmp)
                srcBmp.recycle();
        } else
        {
            return false;
        }
        return true;
    }


    public static final Bitmap getBitmap(InputStreamLoader streamLoader, int pixelSize)
    {
        BitmapFactory.Options options = getDefaultOptions();
        options.inSampleSize = computeSampleSize(streamLoader, pixelSize);
        Bitmap bitmap = null;
        int i = 0;
        while (i < 3)
        {
            int j = i + 1;
            try {
                Bitmap localBitmap = BitmapFactory.decodeStream(streamLoader.get(), null, options);
                bitmap = localBitmap;
                return bitmap;
            } catch (OutOfMemoryError oome) {
                System.gc();
                options.inSampleSize = (2 * options.inSampleSize);
                i = j;
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                return null;
            } catch (Exception e) {
            } finally {
                streamLoader.close();
            }
        }
        return null;
    }

    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight)
    {
        int pixelSize = 2 * (destWidth * destHeight);
        if ((destWidth <= 0) || (destHeight <= 0))
            pixelSize = -1;
        Bitmap destBmp = getBitmap(streamLoader, pixelSize);
        if (pixelSize > 0)
            destBmp = scaleBitmapToDesire(destBmp, destWidth, destHeight, true);
        return destBmp;
    }

    public static Bitmap getBitmap(InputStreamLoader streamLoader, int destWidth, int destHeight, Bitmap reusedBitmap)
    {
        Bitmap srcBitmap = null;
        if(reusedBitmap != null && !reusedBitmap.isRecycled()) {
            BitmapFactory.Options options = getBitmapSize(streamLoader);
            if(options.outWidth != reusedBitmap.getWidth() || options.outHeight != reusedBitmap.getHeight()) {
                options = getDefaultOptions();
                options.inBitmap = reusedBitmap;
                options.inSampleSize = 1;
                try {
                    srcBitmap = BitmapFactory.decodeStream(streamLoader.get(), null, options);
                    streamLoader.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if(srcBitmap == null)
                reusedBitmap.recycle();
        }

        if(srcBitmap != null)
        {
            if(destWidth > 0 && destHeight > 0)
                srcBitmap = scaleBitmapToDesire(srcBitmap, destWidth, destHeight, true);
        } else
        {
            srcBitmap = getBitmap(streamLoader, destWidth, destHeight);
        }
        return srcBitmap;
    }

    public static final BitmapFactory.Options getBitmapSize(String filePath)
    {
        return getBitmapSize(new InputStreamLoader(filePath));
    }

    public static final BitmapFactory.Options getBitmapSize(InputStreamLoader streamLoader)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {        
            BitmapFactory.decodeStream(streamLoader.get(), null, options);
            streamLoader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        return options;
    }

    public static BitmapFactory.Options getDefaultOptions()
    {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inDither = false;
        opt.inJustDecodeBounds = false;
        opt.inSampleSize = 1;
        opt.inScaled = false;
        return opt;
    }

    public static boolean isPngFormat(InputStreamLoader streamLoader) {
        boolean ret = false;
        boolean flag1;
        try {
            InputStream is = streamLoader.get();
            byte head[] = new byte[PNG_HEAD_FORMAT.length];
            if(is.read(head) < head.length) {
                streamLoader.close();
                return false;
            }
            ret = isPngFormat(head);

        } catch (Exception e) {
            ret = false;
        } finally {
            if(streamLoader != null)
                streamLoader.close();
        }

        return ret;
    }

    public static boolean isPngFormat(byte pngHead[]) {
        if(pngHead == null || pngHead.length < PNG_HEAD_FORMAT.length)
            return false;

        for(int i = 0; i < PNG_HEAD_FORMAT.length; i++)
            if(pngHead[i] != PNG_HEAD_FORMAT[i])
                return false;

        return true;
    }

    public static boolean saveBitmapToLocal(InputStreamLoader streamLoader, String path, int destWidth, int destHeight)
    {
        boolean result = false;
        if(streamLoader == null || path == null || destWidth < 1 || destHeight < 1)
            result = false;
        else {
            android.graphics.BitmapFactory.Options options = getBitmapSize(streamLoader);
            if(options.outWidth > 0 && options.outHeight > 0) {
                if(options.outWidth == destWidth && options.outHeight == destHeight) {
                    result = saveToFile(streamLoader, path);
                } else {
                    Bitmap destBmp = getBitmap(streamLoader, destWidth, destHeight);
                    if(destBmp != null) {
                        result = saveToFile(destBmp, path);
                        destBmp.recycle();
                    }
                }
            }
        }

        return result;
    }

    public static boolean saveToFile(Bitmap bitmap, String path) {
        return saveToFile(bitmap, path, false);
    }

    public static boolean saveToFile(Bitmap bitmap, String path, boolean saveToPng)
    {
        if (bitmap == null)
            return false;
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            Bitmap.CompressFormat compressFormat;
            if(saveToPng)
                compressFormat = Bitmap.CompressFormat.PNG;
            else
                compressFormat = Bitmap.CompressFormat.JPEG;
            bitmap.compress(compressFormat, 100, outputStream);
            outputStream.close();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    private static boolean saveToFile(InputStreamLoader streamLoader, String path)
    {
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            Streams.copy(streamLoader.get(), outputStream);
            outputStream.close();
            streamLoader.close();
            return true;
        } catch (Exception e) {
            if (streamLoader != null)
                streamLoader.close();
            e.printStackTrace();
        }
        return false;
    }

    public static Bitmap scaleBitmapToDesire(Bitmap srcBmp, int destWidth, int destHeight, boolean recycleSrcBmp)
    {
        Bitmap destBmp = null;
        try
        {
            int srcWidth = srcBmp.getWidth();
            int srcHeight = srcBmp.getHeight();
            if ((srcWidth == destWidth) && (srcHeight == destHeight))
            {
                destBmp = srcBmp;
            }
            else
            {
                Bitmap.Config config = Bitmap.Config.ARGB_8888;
                if (srcBmp.getConfig() != null)
                    config = srcBmp.getConfig();
                destBmp = Bitmap.createBitmap(destWidth, destHeight, config);
                cropBitmapToAnother(srcBmp, destBmp, recycleSrcBmp);
            }
        }
        catch (OutOfMemoryError oome)
        {
        }
        catch (Exception e)
        {
        }
        return destBmp;
    }
}
