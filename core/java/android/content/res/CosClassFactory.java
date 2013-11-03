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
import android.os.IBinder;
import android.util.DisplayMetrics;

// Referenced classes of package android.content.res:
//            CosResources, CosTypedArray, Resources, AssetManager, 
//            Configuration, CompatibilityInfo, TypedArray

/** {@hide} */
public class CosClassFactory {

    private CosClassFactory() {
    }

    static Resources newResources() {
        return new CosResources();
    }

    public static Resources newResources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
        return new CosResources(assets, metrics, config);
    }

    public static Resources newResources(AssetManager assets, DisplayMetrics metrics, Configuration config,
            CompatibilityInfo compInfo, IBinder token) {
        return new CosResources(assets, metrics, config, compInfo, token);
    }

    static TypedArray newTypedArray(Resources resources, int data[], int indices[], int len) {
        return new CosTypedArray(resources, data, indices, len);
    }
}
