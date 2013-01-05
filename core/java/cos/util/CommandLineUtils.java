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

import android.text.TextUtils;
import java.io.InputStream;

public class CommandLineUtils {
    static Object sLock = new Object();

    public static String addQuoteMark(String str) {
        if ((!TextUtils.isEmpty(str)) && (str.charAt(0) != '"') && (!str.contains("*")))
            str = "\"" + str + "\"";
        return str;
    }

    public static boolean chmod(String file, String mode, String user) {
        return run(user, "busybox chmod %s %s", mode, addQuoteMark(file));
    }

    public static boolean chown(String file, String targetUser, String targetGroup, String user) {
        return run(user, "busybox chown %s.%s %s", targetUser, targetGroup, addQuoteMark(file));
    }

    public static boolean cp(String src, String dest, String user) {
        return run(user, "busybox cp -rf %s %s", addQuoteMark(src), addQuoteMark(dest));
    }

    public static boolean mkdir(String dir, String user) {
        return run(user, "busybox mkdir -p %s", addQuoteMark(dir));
    }

    public static boolean mv(String src, String dest, String user) {
        return run(user, "busybox mv -f %s %s", addQuoteMark(src), addQuoteMark(dest));
    }

    public static boolean rm(String file, String user) {
        return run(user, "busybox rm -r %s", addQuoteMark(file));
    }

    public static boolean run(String user, String format, String... args) {
        return run(false, user, format, args);
    }

    public static boolean run(boolean async, String user, String format, String... args) {
        String cmd;
        boolean result;
        if(args.length > 0)
            cmd = String.format(format, (Object)args);
        else
            cmd = format;
        if(TextUtils.isEmpty(user)) {
            return runInner(async, "sh", "-c", cmd);
        } else {
            return runInner(async, "/system/xbin/invoke-as", String.format("-u %s", user), cmd);
        }
    }

    public static InputStream runAndOutput(String user, String format, String... args) {
        String cmd;
        InputStream inputstream;
        if(args.length > 0)
            cmd = String.format(format, (Object)args);
        else
            cmd = format;
        if(TextUtils.isEmpty(user)) {
            return runAndOutputInner("sh", "-c", cmd);
        } else {
            return runAndOutputInner("/system/xbin/invoke-as", String.format("-u %s", user), cmd);
        }
    }

    private static InputStream runAndOutputInner(String... cmd) {
        InputStream result = null;
        try {
            synchronized (sLock) {
                Process p = Runtime.getRuntime().exec(cmd);
                result = p.getInputStream();
                if (p.waitFor() != 0) {
                    result.close();
                    result = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static boolean runInner(boolean async, String... cmd) {
        boolean result = true;
        try
        {
            synchronized (sLock)
            {
                Process p = Runtime.getRuntime().exec(cmd);
                if (!async)
                {
                    if (p.waitFor() != 0)
                        result = false;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    public static boolean symlink(String oldpath, String newpath, String user) {
        return run(user, "busybox ln -sf %s %s", addQuoteMark(oldpath), addQuoteMark(newpath));
    }
}
