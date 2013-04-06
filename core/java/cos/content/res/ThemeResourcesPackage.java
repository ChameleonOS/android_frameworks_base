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
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public final class ThemeResourcesPackage extends ThemeResources
{
    private static final Map<String, WeakReference<ThemeResourcesPackage>> sPackageResources = new HashMap();

    protected ThemeResourcesPackage(ThemeResourcesPackage wrapped, Resources resources,
            String packageName, ThemeResources.MetaData metaData) {
        super(wrapped, resources, packageName, metaData);
    }

    public static ThemeResourcesPackage getThemeResources(Resources resources, String packageName) {
        ThemeResourcesPackage themeResources = null;
        if (sPackageResources.containsKey(packageName))
            themeResources = (ThemeResourcesPackage)((WeakReference)sPackageResources.get(packageName)).get();
        if (themeResources == null)
            synchronized (sPackageResources) {
                if (sPackageResources.containsKey(packageName))
                    themeResources = (ThemeResourcesPackage)((WeakReference)sPackageResources.get(packageName)).get();
                if (themeResources == null) {
                    themeResources = getTopLevelThemeResources(resources, packageName);
                    sPackageResources.put(packageName, new WeakReference(themeResources));
                }
            }
        return themeResources;
    }

    public static ThemeResourcesPackage getTopLevelThemeResources(Resources resources, String packageName) {
        ThemeResourcesPackage themeResources = null;
        for(int i = 0; i < THEME_PATHS.length; i++) {
            themeResources = new ThemeResourcesPackage(themeResources, resources, packageName, THEME_PATHS[i]);
        }

        return themeResources;
    }

    public CharSequence getThemeCharSequence(int id) {
        CharSequence ret = super.getThemeCharSequence(id);
        if (ret == null)
            ret = getSystem().getThemeCharSequence(id);
        return ret;
    }

    public ThemeZipFile.ThemeFileInfo getThemeFileStream(int cookieType, String fileName) {
        ThemeZipFile.ThemeFileInfo info;
        if (1 == cookieType)
        {
            info = getThemeFileStream("framework-res/" + fileName);
            if (info == null)
                info = getSystem().getThemeFileStream(cookieType, fileName);
        } else if (2 == cookieType) {
            info = getSystem().getThemeFileStream(cookieType, fileName);
        } else
            info = getThemeFileStream(fileName);
        return info;
    }

    public Integer getThemeInt(int id) {
        Integer ret = super.getThemeInt(id);
        if (ret == null)
            ret = getSystem().getThemeInt(id);
        return ret;
    }

    public boolean hasValues() {
        return (super.hasValues() || getSystem().hasValues());
    }
}
