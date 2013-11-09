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

package android.content.res;

import android.content.res.CompatibilityInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;

import cos.content.res.ExtraConfiguration;
import cos.content.res.ThemeResources;
import cos.content.res.ThemeResourcesPackage;
import cos.content.res.ThemeResourcesSystem;
import cos.content.res.ThemeZipFile;

import java.io.IOException;
import java.io.InputStream;

/** {@hide} */
public final class CosResources extends Resources {

    public static final boolean DBG = ThemeResources.DEBUG_THEMES;
    public static final String TAG = "CosResources";

    public static final int sCookieTypeFramework = 1;
    public static final int sCookieTypeMiui = 2;
    public static final int sCookieTypeOther = 3;
    private SparseArray<CharSequence> mCharSequences = new SparseArray();
    private SparseIntArray mCookieType = new SparseIntArray();
    private boolean mHasValues;
    private SparseArray<Integer> mIntegers = new SparseArray();
    private SparseArray<Boolean> mSkipFiles = new SparseArray();
    private ThemeResources mThemeResources;
    private boolean mIsThemeCompatibilityModeEnabled = false;

    CosResources() {
        init(null);
    }

    public CosResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        super(assets, metrics, config);
        init(null);
    }

    public CosResources(AssetManager assets, DisplayMetrics metrics, Configuration config,
            CompatibilityInfo compInfo, IBinder token) {
        super(assets, metrics, config, compInfo, token);
        init(null);
    }

    private int getCookieType(int cookie) {
        int type = mCookieType.get(cookie);
        if (DBG)
            Log.d(TAG, String.format("getCookieType(%d) - %d", cookie, type));
        if(type == 0) {
            String name = mAssets.getCookieName(cookie);
            Log.d(TAG, String.format("cookie name=%s", name));
            if("/system/framework/framework-res.apk".equals(name))
                type = sCookieTypeFramework;
            else if("/system/framework/framework-miui-res.apk".equals(name))
                type = sCookieTypeMiui;
            else
                type = sCookieTypeOther;
            mCookieType.put(cookie, type);
        }
        return type;
    }

    private TypedArray replaceTypedArray(TypedArray array) {
        if((mThemeResources == null || mHasValues) && !mIsThemeCompatibilityModeEnabled) {
            int data[] = array.mData;
            int index = 0;
            while(index < data.length)  {
                int type = data[index + 0];
                int id = data[index + 3];
                if((type >= TypedValue.TYPE_FIRST_INT && 
                        type <= TypedValue.TYPE_LAST_INT) || type == TypedValue.TYPE_DIMENSION) {
                    Integer themeInteger = getThemeInt(id);
                    if(themeInteger != null)
                        data[index + 1] = themeInteger.intValue();
                }
                index += 6;
            }
        }
        return array;
    }

    public CharSequence getText(int id)
            throws Resources.NotFoundException {
        CharSequence cs = getThemeCharSequence(id);
        if(cs == null)
            cs = super.getText(id);
        return cs;
    }

    public CharSequence getText(int id, CharSequence def) {
        CharSequence cs = getThemeCharSequence(id);
        if(cs == null)
            cs = super.getText(id, def);
        return cs;
    }

    CharSequence getThemeCharSequence(int id) {
        CharSequence value = null;
        if(mHasValues) {
            int index = mCharSequences.indexOfKey(id);
            if(index >= 0) {
                value = (CharSequence)mCharSequences.valueAt(index);
            } else {
                if (id > 0) {
                    String name = getResourceEntryName(id);
                    value = mThemeResources.getThemeCharSequence(name);
                    if (DBG)
                        Log.i(TAG, "getThemeCharSequence(" + id + "): " + name + ", " + value);
                    if (value != null)
                        mCharSequences.put(id, value);
                }
            }
        }
        return value;
    }

    Integer getThemeInt(int id) {
        Integer value = null;
        if(mHasValues) {
            int index = mIntegers.indexOfKey(id);
            if(index >= 0) {
                value = (Integer)mIntegers.valueAt(index);
            } else {
                if (id > 0) {
                    String name = getResourceEntryName(id);
                    value = mThemeResources.getThemeInt(name);
                    if (DBG)
                        Log.i(TAG, "getThemeInt(0x" + Integer.toHexString(id) + "): " + name + ", " + value);
                    if (value != null)
                        mIntegers.put(id, value);
                }
            }
        }
        return value;
    }

    public void getValue(int id, TypedValue outValue, boolean resolveRefs)
            throws Resources.NotFoundException {
        super.getValue(id, outValue, resolveRefs);
        if (mIsThemeCompatibilityModeEnabled)
            return;
        if((outValue.type >= TypedValue.TYPE_FIRST_INT && outValue.type <= TypedValue.TYPE_LAST_INT)
                || outValue.type == TypedValue.TYPE_DIMENSION) {
            Integer integer = getThemeInt(id);
            if(integer != null)
                outValue.data = integer.intValue();
        }
    }

    public void init(String packageName) {
        if(TextUtils.isEmpty(packageName) || "android".equals(packageName) || "miui".equals(packageName)) {
            mThemeResources = ThemeResources.getSystem(this);
        } else {
            if (DBG)
                Log.d(TAG, String.format("Getting ThemeResourcesPackages.getThemeResources() for %s", packageName));
            mThemeResources = ThemeResourcesPackage.getThemeResources(this, packageName);
        }
        mHasValues = mThemeResources.hasValues();
    }

    public void init(String packageName, boolean isThemeCompatibilityModeEnabled) {
        if(TextUtils.isEmpty(packageName) || "android".equals(packageName) || "miui".equals(packageName)) {
            mThemeResources = ThemeResources.getSystem(this);
        } else {
            if (DBG)
                Log.d(TAG, String.format("Getting ThemeResourcesPackages.getThemeResources() for %s", packageName));
            mThemeResources = ThemeResourcesPackage.getThemeResources(this, packageName);
        }
        mHasValues = mThemeResources.hasValues();
        mIsThemeCompatibilityModeEnabled = isThemeCompatibilityModeEnabled;
    }

    Drawable loadOverlayDrawable(TypedValue value, int id) {
        if (DBG) Log.d(TAG, String.format("loadOverlayDrawable(%d, %d)", value.type, id));
        if(mSkipFiles.get(id) != null && mSkipFiles.get(id).equals(Boolean.TRUE) || mIsThemeCompatibilityModeEnabled)
            return null;

        Drawable drawable = null;
        String file;
        ThemeZipFile.ThemeFileInfo info;
        try {
            file = value.string.toString();
            info = mThemeResources.getThemeFileStream(getCookieType(value.assetCookie), file);
            if (info == null && file.contains("-sw")) {
                file = file.substring(0, file.indexOf('-')) + file.substring(file.lastIndexOf('-'));
                info = mThemeResources.getThemeFileStream(getCookieType(value.assetCookie), file);
            }
            if(info != null) {
                BitmapFactory.Options opts = null;
                if(info.mDensity > 0 && value.density != 65535) {
                    opts = new BitmapFactory.Options();
                    opts.inDensity = info.mDensity;
                }
                InputStream inputstream = info.mInput;
                if(file.endsWith(".9.png"))
                    inputstream = SimulateNinePngUtil.convertIntoNinePngStream(inputstream);
                drawable = Drawable.createFromResourceStream(this, value, inputstream, file, opts);
                info.mInput.close();
            } else {
                if (DBG)
                    Log.d(TAG, "loadOverlayDrawable: info was null for " + file);
                mSkipFiles.put(id, Boolean.valueOf(true));
            }
        } catch (IOException e) {
            drawable = null;
        }

        return drawable;
    }

    public final Resources.Theme newTheme() {
        return new CosTheme();
    }

    public TypedArray obtainAttributes(AttributeSet set, int[] attrs) {
        return replaceTypedArray(super.obtainAttributes(set, attrs));
    }

    public TypedArray obtainTypedArray(int id)
            throws Resources.NotFoundException {
        return replaceTypedArray(super.obtainTypedArray(id));
    }

    public InputStream openRawResource(int id, TypedValue value)
            throws Resources.NotFoundException {
        if (DBG) Log.d(TAG, String.format("openRawResource(%d, %d)", id, value.type));
        InputStream is = null;
        if(mSkipFiles.get(id) != null) 
            return super.openRawResource(id, value);

        ThemeZipFile.ThemeFileInfo info;
        getValue(id, value, true);
        String file = value.string.toString();
        info = mThemeResources.getThemeFileStream(getCookieType(value.assetCookie), file);
        if(info == null) {
            mSkipFiles.put(id, Boolean.valueOf(true));
            is = super.openRawResource(id, value);
        } else
            is = info.mInput;

        return is;
    }

    public void updateConfiguration(Configuration config, DisplayMetrics metrics, CompatibilityInfo compat) {
        Configuration currentConfig = getConfiguration();
        int configChanges;
        if(currentConfig != null && config != null)
            configChanges = currentConfig.diff(config);
        else
            configChanges = 0;
        super.updateConfiguration(config, metrics, compat);
        if(mThemeResources != null && ((configChanges & 0x200) != 0
                || ExtraConfiguration.needNewResources(configChanges))) {
            if(ThemeResources.getSystem().checkUpdate())
                Resources.clearPreloadedCache();
            mIntegers.clear();
            mCharSequences.clear();
            mSkipFiles.clear();
            mThemeResources.checkUpdate();
            mHasValues = mThemeResources.hasValues();
        }
    }

    public final class CosTheme extends Resources.Theme {
        public CosTheme() {
            super();
        }

        public TypedArray obtainStyledAttributes(int resid, int[] attrs)
            throws Resources.NotFoundException {
            return CosResources.this.replaceTypedArray(super.obtainStyledAttributes(resid, attrs));
        }

        public TypedArray obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes) {
            return CosResources.this.replaceTypedArray(super.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes));
        }

        public TypedArray obtainStyledAttributes(int[] attrs) {
            return CosResources.this.replaceTypedArray(super.obtainStyledAttributes(attrs));
        }
    }
}

