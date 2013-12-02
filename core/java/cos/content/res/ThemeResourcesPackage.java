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
import android.util.Log;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ThemeResourcesPackage extends ThemeResources {
    private static final boolean DBG = ThemeResources.DEBUG_THEMES;
    private static final String TAG = "ThemeResourcesPackage";
    private static final Map<String, ThemeResourcesPackage>
            sPackageResources = new HashMap();

    protected ThemeResourcesPackage(ThemeResourcesPackage wrapped, Resources resources,
                                    String packageName, ThemeResources.MetaData metaData) {
        super(wrapped, resources, packageName, metaData);
    }

    public static ThemeResourcesPackage getThemeResources(Resources resources, String packageName) {
        if (DBG)
            Log.d(TAG, String.format("getThemeResources: packageName=%s", packageName));
        ThemeResourcesPackage themeResources = null;
        if (sPackageResources.containsKey(packageName))
            themeResources = sPackageResources.get(packageName);
        if (themeResources == null)
            synchronized (sPackageResources) {
                if (sPackageResources.containsKey(packageName))
                    themeResources = sPackageResources.get(packageName);
                if (themeResources == null) {
                    themeResources = getTopLevelThemeResources(resources, packageName);
                    sPackageResources.put(packageName, themeResources);
                }
            }
        return themeResources;
    }

    public static ThemeResourcesPackage getThemeResources(Resources resources,
            String packageName, String originatingPackageName) {
        if (DBG)
            Log.d(TAG, String.format("getThemeResources: packageName=%s, originatingPackageName=%s",
                    packageName, originatingPackageName));
        ThemeResourcesPackage themeResources = null;
        if (!packageName.equals(originatingPackageName) && sPackageResources.containsKey(packageName))
            themeResources = sPackageResources.get(packageName);
        if (themeResources == null)
            synchronized (sPackageResources) {
                themeResources = getTopLevelThemeResources(resources, packageName);
                if (packageName.equals(originatingPackageName))
                    sPackageResources.put(packageName, themeResources);
            }
        return themeResources;
    }

    public static ThemeResourcesPackage getTopLevelThemeResources(Resources resources, String packageName) {
        return new ThemeResourcesPackage(null, resources, packageName, THEME_PATH_DATA);
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String fileName) {
        ThemeZipFile.ThemeFileInfo info;
        if (1 == cookieType) {
            info = getThemeFileStream("framework-res/" + fileName);
            if (info == null)
                info = getSystem().getThemeFileStream(cookieType, fileName);
        } else if (2 == cookieType) {
            info = getSystem().getThemeFileStream(cookieType, fileName);
        } else
            info = getThemeFileStream(fileName);
        return info;
    }

    public boolean hasValues() {
        return (super.hasValues() || getSystem().hasValues());
    }
}
