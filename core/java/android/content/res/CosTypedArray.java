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

public class CosTypedArray extends TypedArray
{
    private boolean mIsCosResources = getResources() instanceof CosResources;

    CosTypedArray(Resources resources, int[] data, int[] indices, int len) {
        super(resources, data, indices, len);
    }

    private CharSequence loadStringValueAt(int index) {
        CharSequence cs = null;
        if (this.mIsCosResources) {
            if (this.mData[index] == 3) {
                int type = this.mData[(index + 3)];
                cs = ((CosResources)getResources()).getThemeCharSequence(type);
            } else {
                cs = null;
            }
        }
        return cs;
    }

    public String getString(int index) {
        CharSequence cs = loadStringValueAt(index * 6);
        if(cs != null)
            return cs.toString();
        else
            return super.getString(index);
    }

    public CharSequence getText(int index) {
        CharSequence cs = loadStringValueAt(index * 6);
        if(cs == null)
            cs = super.getText(index);
        return cs;
    }
}

