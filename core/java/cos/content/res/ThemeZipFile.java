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

import android.app.ThemeHelper;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import cos.util.DisplayUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public final class ThemeZipFile
{
    private static final String ALTERNATIVE_THEME_VALUE_FILE = "theme_values%s.xml";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    static boolean DBG = ThemeResources.DEBUG_THEMES;
    static String TAG = "ThemeZipFile";
    private static final String TAG_BOOLEAN = "bool";
    private static final String TAG_COLOR = "color";
    private static final String TAG_DIMEN = "dimen";
    private static final String TAG_DRAWABLE = "drawable";
    private static final String TAG_INTEGER = "integer";
    private static final String MIUI_TAG_ROOT = "MIUI_Theme_Values";
    private static final String COS_TAG_ROOT = "ChaOS_Theme_Values";
    private static final String TAG_STRING = "string";
    public static final String THEME_VALUE_FILE = "theme_values.xml";
    private static final String TRUE = "true";
    private static final int sDensity = DisplayMetrics.DENSITY_DEVICE;
    private static final int[] sDensities = DisplayUtils.getBestDensityOrder(sDensity);
    protected static final HashMap<String, ThemeZipFile> sThemeZipFiles = new HashMap<String, ThemeZipFile>();
    private SparseArray<CharSequence> mCharSequences = new SparseArray();
    private SparseArray<Integer> mIntegers = new SparseArray();
    private long mLastModifyTime = -1L;
    private ThemeResources.MetaData mMetaData;
    private String mPackageName;
    private String mPath;
    private Resources mResources;
    private ZipFile mZipFile;

    public ThemeZipFile(String zipFilePath, ThemeResources.MetaData metaData, String packageName, Resources resources) {
        if (DBG)
            Log.d(TAG, "create ThemeZipFile for " + zipFilePath);
        mResources = resources;
        mPackageName = packageName;
        mPath = zipFilePath;
        mMetaData = metaData;
    }

    private void clean() {
        if(DBG)
            Log.d(TAG, "clean for " + mPath);
        if(mZipFile != null)
        {
            try
            {
                mZipFile.close();
            }
            catch(Exception exception) { }
            mZipFile = null;
        }
        mIntegers.clear();
        mCharSequences.clear();
    }

    private ThemeFileInfo getInputStreamInner(String relativeFilePath) {
        ThemeFileInfo result = getZipInputStream(relativeFilePath);
        if (result == null) {
            int index = relativeFilePath.indexOf("dpi/");
            if(index > 0)
            {
                String suffix = relativeFilePath.substring(index + 3);
                for(; relativeFilePath.charAt(index) != '-'; index--);
                String prefix = relativeFilePath.substring(0, index);
                for(int j = 0; j < sDensities.length; j++)
                {
                    result = getZipInputStream(String.format("%s%s%s", prefix, 
                            DisplayUtils.getDensitySuffix(sDensities[j]), suffix));
                    if (result != null) {
                        if(sDensities[j] > 1)
                            result.mDensity = sDensities[j];
                        break;
                    }
                }
            }
        }
        return result;
    }

    private static final String getPackageName(String componentName) {
        if (!"framework-res".equals(componentName) && !"icons".equals(componentName)) {
            if ("framework-miui-res".equals(componentName) || "lockscreen".equals(componentName))
                componentName = "miui";
        } else {
            componentName = "android";
        }

        return componentName;
    }

    protected static ThemeZipFile getThemeZipFile(ThemeResources.MetaData metadata, String componentName, Resources resources) {
        ThemeZipFile zipFile = null;
        String path = metadata.themePath + componentName;
        WeakReference ref = new WeakReference<ThemeZipFile>(sThemeZipFiles.get(path));
        if(ref != null)
            zipFile = (ThemeZipFile)ref.get();
        else
            zipFile = null;
        if(zipFile == null) {
            synchronized(sThemeZipFiles) {
                ref = new WeakReference<ThemeZipFile>(sThemeZipFiles.get(path));
                if(ref != null)
                    zipFile = (ThemeZipFile)ref.get();
                if(zipFile == null) {
                    zipFile = new ThemeZipFile(path, metadata, getPackageName(componentName), resources);
                    ref = new WeakReference<ThemeZipFile>(zipFile);
                    sThemeZipFiles.put(path, (ThemeZipFile)ref.get());
                }
            }
        }

        return zipFile;
    }

    private ThemeFileInfo getZipInputStream(String relativeFilePath) {
        ThemeFileInfo themeFileInfo = null;
        if (!isValid()) {
            return null;
        }

        ZipEntry entry = null;
        try {
            if (relativeFilePath.endsWith("#*.png")) {
                String fuzzyIconName = relativeFilePath.substring(0, relativeFilePath.length() - "#*.png".length());
                Enumeration localEnumeration = this.mZipFile.entries();
                do {
                    if (!localEnumeration.hasMoreElements())
                        break;
                    entry = (ZipEntry)localEnumeration.nextElement();
                } while ((entry.isDirectory()) || (!entry.getName().startsWith(fuzzyIconName)));
            }

            if (entry == null)
                entry = mZipFile.getEntry(relativeFilePath);

            InputStream input = mZipFile.getInputStream(entry);
            if (input != null) {
                if (DBG) Log.d(TAG, String.format("getZipInputStream: %s", relativeFilePath));
                themeFileInfo = new ThemeFileInfo(input, entry.getSize());
            }
        } catch (Exception localException) {
        }

        return themeFileInfo;
    }

    private boolean isValid() {
        return mZipFile != null;
    }

    private void loadThemeValues() {
        if (!isValid())
            return;
        if(DBG)
            Log.d(TAG, "loadThemeValues for " + mPath);
        for (int i = 0; i < sDensities.length; i++) {
            String filename = String.format(ALTERNATIVE_THEME_VALUE_FILE,
                        DisplayUtils.getDensitySuffix(sDensities[i]));
            ThemeFileInfo info = getZipInputStream(filename);
            if(info != null) {
                try {
                    if (DBG) Log.d(TAG, String.format("loadThemeValues: parsing %s for %s", filename, mPath));
                    XmlPullParser xmlpullparser = XmlPullParserFactory.newInstance().newPullParser();
                    xmlpullparser = XmlPullParserFactory.newInstance().newPullParser();
                    BufferedInputStream bufferedinputstream = new BufferedInputStream(info.mInput, 8192);
                    xmlpullparser.setInput(bufferedinputstream, null);
                    mergeThemeValues(xmlpullparser);
                    bufferedinputstream.close();
                    break;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void mergeThemeValues(XmlPullParser xpp) {
        int eventType;
        try {
            eventType = xpp.next();
            while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT)
                eventType = xpp.next();
    		
            if (eventType != XmlPullParser.START_TAG)
                throw new XmlPullParserException("No start tag found!");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
                try {
                    if (eventType == XmlPullParser.START_TAG) {
                        String attr = null;
                        String pkg = null;
                        String tag = null;
                        String attrName = null;
                        tag = xpp.getName().trim();
                        int attrCount = xpp.getAttributeCount();
                        while (attrCount-- > 0) {
                            attrName = xpp.getAttributeName(attrCount);
                            if (attrName.equals("name"))
                                attr = xpp.getAttributeValue(attrCount);
                            else if (attrName.equals("package"))
                                pkg = xpp.getAttributeValue(attrCount);
                        }
                        String content = xpp.nextText();
                        if (attr != null && content != null && content.length() > 0) {
                            if (pkg == null)
                                pkg = mPackageName;

                            int resId = mResources.getIdentifier(attr, tag, pkg);
                            if (resId > 0) {
                                if (tag.equals(TAG_BOOLEAN)) {
                                    if (DBG)
                                        Log.d(TAG, String.format("parsing boolean %s, %s, %s, %s", attr, tag, pkg, content));
                                    int value = content.trim().equals(TRUE) ? 1 : 0;
                                    mIntegers.put(resId, Integer.valueOf(value));
                                } else if (tag.equals(TAG_COLOR) || tag.equals(TAG_INTEGER) || tag.equals(TAG_DRAWABLE)) {
                                    if (DBG)
                                        Log.d(TAG, String.format("parsing color/int/drawable %s, %s, %s, %s", attr, tag, pkg, content));
                                    if (mMetaData.supportInt && mIntegers.indexOfKey(resId) < 0)
                                        mIntegers.put(resId, Integer.valueOf(XmlUtils.convertValueToUnsignedInt(content.trim(), 0)));
                                } else if (tag.equals(TAG_STRING)) {
                                    if (DBG)
                                        Log.d(TAG, String.format("parsing string %s, %s, %s, %s", attr, tag, pkg, content));
                                    if (mMetaData.supportCharSequence && mCharSequences.indexOfKey(resId) < 0)
                                        mCharSequences.put(resId, content);
                                } else if (tag.equals(TAG_DIMEN)) {
                                    if (DBG)
                                        Log.d(TAG, String.format("parsing dimension %s, %s, %s, %s", attr, tag, pkg, content));
                                    Integer integer = ThemeHelper.parseDimension(content.toString());
                                    if (integer != null)
                                        mIntegers.put(resId, integer);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openZipFile() {
        if(DBG)
            Log.d(TAG, "openZipFile for " + this.mPath);
        File file = new File(mPath);
        mLastModifyTime = file.lastModified();
        if(mLastModifyTime != 0L) {
            try {
                mZipFile = new ZipFile(file);
            } catch(Exception e) {
                if (DBG) {
                    Log.e(TAG, "Unable to openzipfile " + this.mPath);
                    e.printStackTrace();
                }
                mZipFile = null;
            }
        } else {
            if (DBG)
                Log.e(TAG, "Unable to openzipfile, " + this.mPath + " does not exist!");
        }
    }

    public boolean checkUpdate() {
        if(DBG)
            Log.d(TAG, "checkUpdate for " + this.mPath);
        long lastModified = (new File(mPath)).lastModified();
    
        if(mZipFile == null || mLastModifyTime != lastModified) {
            synchronized(this) {
                clean();
                openZipFile();
                loadThemeValues();
                return true;
            }
        } else
            return false;
    }

    public boolean containsEntry(String relativePath) {
        return isValid() && mZipFile.getEntry(relativePath) != null;
    }

    public boolean exists() {
        return (new File(mPath)).exists();
    }

    public ThemeFileInfo getInputStream(String relativeFilePath) {
        InputStream is = null;
        if(mMetaData.supportFile)
            return getInputStreamInner(relativeFilePath);
        return null;
    }

    public CharSequence getThemeCharSequence(int id) {
        return (CharSequence)mCharSequences.get(id);
    }

    public Integer getThemeInt(int id) {
        return (Integer)mIntegers.get(id);
    }

    public boolean hasValues() {
        return mIntegers.size() > 0 || mCharSequences.size() > 0;
    }

    public class ThemeFileInfo {
        public int mDensity;
        public InputStream mInput;
        public long mSize;

        ThemeFileInfo(InputStream in, long size) {
            this.mInput = in;
            this.mSize = size;
        }
    }
}
