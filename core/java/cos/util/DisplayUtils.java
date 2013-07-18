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

public class DisplayUtils
{
    private static final int[] DENSITIES = {480, 320, 240, 160, 120, 1, 0};

    public static int[] getBestDensityOrder(int currentDensity) {
        /* TODO: figure out what they were trying to accomplish here
         *       and implement it, for now return DENSITIES as is
         */
        int i = 1;
        int[] densities = null;
        for (int j = 0; j < DENSITIES.length; j++) {
            if (currentDensity == DENSITIES[j])
                i = 0;
            densities = new int[i + DENSITIES.length];
            densities[0] = currentDensity;
            int k = i;
            int m = 1;
            while (k < DENSITIES.length) {
                if (currentDensity != DENSITIES[k]) {
                    int n = m + 1;
                    densities[m] = DENSITIES[k];
                    m = n;
                }
                k++;
            }
        }
        return densities;
    }

    public static String getDensityName(int density) {
        int i;
        int j;
        String name;
        switch (density) {
        case 120:
            name = "ldpi";
            break;
        case 160:
            name = "mdpi";
            break;
        case 240:
            name = "hdpi";
            break;
        case 320:
            name = "xhdpi";
            break;
        case 480:
            name = "xxhdpi";
            break;
        case 1:
            name = "nodpi";
            break;
        case 0:
            name = "";
            break;
        default:
            // find the closest named density to @param density
            i = DENSITIES.length - 1;
            for (j = i - 1; j > 0; j--)
                if (Math.abs(DENSITIES[j] - density) < Math.abs(DENSITIES[i] - density))
                    i = j;
            name = getDensityName(DENSITIES[i]);
            break;
        }

        return name;
    }

    public static String getDensitySuffix(int paramInt) {
        String str = getDensityName(paramInt);
        if (!str.equals(""))
            str = "-" + str;
        return str;
    }

    public static String getDrawbleDensityFolder(int paramInt) {
        return "res/" + getDrawbleDensityName(paramInt) + "/";
    }

    public static String getDrawbleDensityName(int paramInt) {
        return "drawable" + getDensitySuffix(paramInt);
    }
}
