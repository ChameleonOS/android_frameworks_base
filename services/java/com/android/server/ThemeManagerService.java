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

package com.android.server;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.IThemeManagerService;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.server.power.ShutdownThread;

import cos.content.res.ExtraConfiguration;
import cos.util.CommandLineUtils;

import java.io.*;
import java.lang.Process;
import java.lang.RuntimeException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service responsible for handling applying full themes or parts of themes
 */
public class ThemeManagerService extends IThemeManagerService.Stub {

    public static final String THEME_DIR = "/data/system/theme";
    public static final String CUSTOMIZED_ICONS_DIR = "/data/system/customized_icons";
    public static final String FONTS_DIR = "/data/fonts";

    private static final String TAG = "ThemeService";
    private ThemeWorkerThread mWorker;
    private ThemeWorkerHandler mHandler;
    private Context mContext;
    private boolean mRemoveFonts = false;

    static Object sLock = new Object();

    public ThemeManagerService(Context context) {
        super();
        mContext = context;
        mWorker = new ThemeWorkerThread("ThemeServiceWorker");
        mWorker.start();
        Log.i(TAG, "Spawned worker thread");

        // create the themes directory if it does not exist
        createThemeDir();
        // create the customized icon directory if it does not exist
        createIconsDir();
        // create the fonts directory if it does not exist
        createFontsDir();
    }

    /**
     * Apply the theme specified by the URI string provided
     */
    public void applyTheme(String themeURI) {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void removeTheme(boolean removeFonts) {
        mRemoveFonts = removeFonts;
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_REMOVE_THEME;
        mHandler.sendMessage(msg);
    }

    public void removeThemeAndApply(boolean removeFonts) {
        mRemoveFonts = removeFonts;
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_REMOVE_THEME_APPLY;
        mHandler.sendMessage(msg);
    }

    public void applyInstalledTheme(){
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_CURRENT;
        mHandler.sendMessage(msg);
    }

    public void applyInstalledThemeReboot(){
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_CURRENT_REBOOT;
        mHandler.sendMessage(msg);
    }

    public void applyThemeIcons() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_ICONS;
        mHandler.sendMessage(msg);
    }

    public void applyThemeWallpaper() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_WALLPAPER;
        mHandler.sendMessage(msg);
    }

    public void applyThemeSystemUI() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_SYSTEMUI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFramework() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FRAMEWORK;
        mHandler.sendMessage(msg);
    }

    public void applyThemeRingtones() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_RINGTONES;
        mHandler.sendMessage(msg);
    }

    public void applyThemeLockscreen() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_LOCKSCREEN;
        mHandler.sendMessage(msg);
    }

    public void applyThemeBootanimation() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_BOOTANIMATION;
        mHandler.sendMessage(msg);
    }

    public void applyThemeMms() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_MMS;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFont() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FONT;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFontReboot() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FONT_REBOOT;
        mHandler.sendMessage(msg);
    }

    public void resetThemeIcons() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_ICONS;
        mHandler.sendMessage(msg);
    }

    public void resetThemeWallpaper() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_WALLPAPER;
        mHandler.sendMessage(msg);
    }

    public void resetThemeSystemUI() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_SYSTEMUI;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFramework() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FRAMEWORK;
        mHandler.sendMessage(msg);
    }

    public void resetThemeRingtones() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_RINGTONES;
        mHandler.sendMessage(msg);
    }

    public void resetThemeLockscreen() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_LOCKSCREEN;
        mHandler.sendMessage(msg);
    }

    public void resetThemeBootanimation() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_BOOTANIMATION;
        mHandler.sendMessage(msg);
    }

    public void resetThemeMms() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_MMS;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFont() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FONT;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFontReboot() {
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FONT_REBOOT;
        mHandler.sendMessage(msg);
    }

    /**
     * Simple copy routine given an input stream and an output stream
     */
    private void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        out.close();
    }

    /**
     * Checks if THEME_DIR exists and returns true if it does
     */
    private boolean themeDirExists() {
        return (new File(THEME_DIR)).exists();
    }

    private void createThemeDir() {
        if (!themeDirExists()) {
            Log.d(TAG, "Creating themes directory");
            File dir = new File(THEME_DIR);
            if(dir.mkdir()) {
                dir.setReadable(true, false);
                dir.setWritable(true, false);
                dir.setExecutable(true, false);
            }
        }
    }

    /**
     * Checks if CUSTOMIZED_ICONS_DIR exists and returns true if it does
     */
    private boolean iconsDirExists() {
        return (new File(CUSTOMIZED_ICONS_DIR)).exists();
    }

    private void createIconsDir() {
        if (!iconsDirExists()) {
            Log.d(TAG, "Creating icons directory");
            File dir = new File(CUSTOMIZED_ICONS_DIR);
            if(dir.mkdir()) {
                dir.setReadable(true, false);
                dir.setWritable(true, false);
                dir.setExecutable(true, false);
            }
        }
    }

    /**
     * Checks if CUSTOMIZED_ICONS_DIR exists and returns true if it does
     */
    private boolean fontsDirExists() {
        return (new File(FONTS_DIR)).exists();
    }

    private void createFontsDir() {
        if (!fontsDirExists()) {
            Log.d(TAG, "Creating fonts directory");
            File dir = new File(FONTS_DIR);
            if(dir.mkdir()) {
                dir.setReadable(true, false);
                dir.setWritable(true, false);
                dir.setExecutable(true, false);
            }
        }
    }

    private void delete(File file) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                delete(f);
        }
        if (!file.delete())
            Log.e(TAG, "Unable to delete " + file.getName());
    }

    private void removeCurrentTheme() throws IOException {
        // remove the contents of THEME_DIR
        File file = new File(THEME_DIR);
        if (file.exists()) {
            for (File f : file.listFiles())
                delete(f);
        }
        
        // remove the contents of CUSTOMIZED_ICONS_DIR
        file = new File(CUSTOMIZED_ICONS_DIR);
        if (file.exists()) {
            for (File f : file.listFiles())
                delete(f);
        }
    
        // remove the contents of FONTS_DIR
        if (mRemoveFonts) {
            file = new File(FONTS_DIR);
            if (file.exists()) {
                for (File f : file.listFiles())
                    delete(f);
            }
        }
    
        file = new File("/data/local/bootanimation.zip");
        if (file.exists())
            delete(file);
    }

    private static boolean run(String cmd) {
        boolean result = true;
        try
        {
            synchronized (sLock)
            {
                Process p = Runtime.getRuntime().exec(cmd);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    private static boolean isSymbolicLink(File f) throws IOException {
        return !f.getAbsolutePath().equals(f.getCanonicalPath());
    }

    private class ThemeWorkerThread extends Thread {
        public ThemeWorkerThread(String name) {
            super(name);
        }

        public void run() {
            Looper.prepare();
            mHandler = new ThemeWorkerHandler();
            Looper.loop();
        }
    }

    private void killProcess(String packageName) {
        IActivityManager am = ActivityManagerNative.getDefault();
        Log.d(TAG, "Attempting to kill " + packageName);
        try {
            am.forceStopPackage(packageName, UserHandle.getCallingUserId());
        } catch (Exception e) {}
    }

    private void notifyThemeUpdate(long configChange) {
        try {
            // now notifiy activity manager of the configuration change
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();
            config.extraConfig.updateTheme(configChange);
            am.updateConfiguration(config);
  
            // restart launcher
            if ((configChange & (ExtraConfiguration.THEME_FLAG_LAUNCHER | ExtraConfiguration.THEME_FLAG_ICON |
                    ExtraConfiguration.THEME_FLAG_FRAMEWORK)) != 0)
                killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
        } catch (Exception e) {}
    }

    private void setThemeWallpaper() {
        File f = new File(THEME_DIR + "/wallpaper/default_wallpaper.jpg");
        if (!f.exists())
            f = new File(THEME_DIR + "/wallpaper/default_wallpaper.png");
        if (!f.exists())
            return;

        WallpaperManager wm = WallpaperManager.getInstance(mContext);
        try {
            Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(f));
            if (bmp != null)
                wm.setBitmap(bmp);
        } catch(IOException e) {
        }
    }

    private void resetWallpaper() {
        try {
            File f = new File(THEME_DIR + "/wallpaper/default_wallpaper.jpg");
            if (!f.exists())
                f = new File(THEME_DIR + "/wallpaper/default_wallpaper.png");
            if (f.exists())
                delete(f);

            WallpaperManager wm = WallpaperManager.getInstance(mContext);
            wm.clear();
        } catch(IOException e) {
        }
    }

    private void setBootanimation() {
        try {
            run(String.format("invoke-as -u root chown system.system %s", "/data/local"));
            run(String.format("invoke-as -u root cp %s %s", THEME_DIR + "/boots/bootanimation.zip", "/data/local/bootanimation.zip"));
            run(String.format("invoke-as -u root chown root.root %s", "/data/local"));
        } catch (Exception e) {}
    }

    private void resetBootanimation() {
        try {
            run(String.format("invoke-as -u root chown system.system %s", "/data/local"));
            run(String.format("invoke-as -u root rm %s", "/data/local/bootanimation.zip"));
            run(String.format("invoke-as -u root chown root.root %s", "/data/local"));
        } catch (Exception e) {}
    }

    private void reboot() {
        ShutdownThread.reboot(mContext, null, false);
    }

    private class ThemeWorkerHandler extends Handler {
        private static final int MESSAGE_APPLY = 0;
        private static final int MESSAGE_APPLY_CURRENT = 1;
        private static final int MESSAGE_APPLY_CURRENT_REBOOT = 2;
        private static final int MESSAGE_REMOVE_THEME = 3;
        private static final int MESSAGE_REMOVE_THEME_APPLY = 4;
        private static final int MESSAGE_APPLY_ICONS = 5;
        private static final int MESSAGE_APPLY_WALLPAPER = 6;
        private static final int MESSAGE_APPLY_SYSTEMUI = 7;
        private static final int MESSAGE_APPLY_FRAMEWORK = 8;
        private static final int MESSAGE_APPLY_LOCKSCREEN = 9;
        private static final int MESSAGE_APPLY_RINGTONES = 10;
        private static final int MESSAGE_APPLY_BOOTANIMATION = 11;
        private static final int MESSAGE_APPLY_MMS = 12;
        private static final int MESSAGE_APPLY_FONT = 13;
        private static final int MESSAGE_APPLY_FONT_REBOOT = 14;
        private static final int MESSAGE_RESET_ICONS = 20;
        private static final int MESSAGE_RESET_WALLPAPER = 21;
        private static final int MESSAGE_RESET_SYSTEMUI = 22;
        private static final int MESSAGE_RESET_FRAMEWORK = 23;
        private static final int MESSAGE_RESET_LOCKSCREEN = 24;
        private static final int MESSAGE_RESET_RINGTONES = 25;
        private static final int MESSAGE_RESET_BOOTANIMATION = 26;
        private static final int MESSAGE_RESET_MMS = 27;
        private static final int MESSAGE_RESET_FONT = 28;
        private static final int MESSAGE_RESET_FONT_REBOOT = 29;

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_APPLY:
                    try {
                        String themeURI = (String)msg.obj;
                        if (msg.what == MESSAGE_APPLY && !TextUtils.isEmpty(themeURI)) {
                            Log.i(TAG, "applying theme " + themeURI);
                            ContentResolver cr = mContext.getContentResolver();
                            Uri uri = Uri.parse(themeURI);
                            ParcelFileDescriptor file = null;
                            try{
                                // clear out the old theme first
                                removeCurrentTheme();

                                file = cr.openFileDescriptor(uri, "r");
                                FileDescriptor fd = file.getFileDescriptor();
                                ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(fd)));
                                ZipEntry ze = null;
                                while ((ze = zip.getNextEntry()) != null) {
                                    if (ze.isDirectory()) {
                                        // Assume directories are stored parents first then children
                                        Log.d(TAG, "Creating directory /data/system/theme/" + ze.getName());
                                        File dir = new File("/data/system/theme/" + ze.getName());
                                        dir.mkdir();
                                        dir.setReadable(true, false);
                                        dir.setWritable(true, false);
                                        dir.setExecutable(true, false);
                                        zip.closeEntry();
                                        continue;
                                    }
            
                                    Log.d(TAG, "Creating file " + ze.getName());
                                    copyInputStream(zip,
                                            new BufferedOutputStream(new FileOutputStream("/data/system/theme/" + ze.getName())));
                                    (new File("/data/system/theme/" + ze.getName())).setReadable(true, false);
                                    zip.closeEntry();
                                }
            
                                zip.close();

                                // now notifiy activity manager of the configuration change
                                notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);

                                // restart launcher
                                killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
                            } catch (FileNotFoundException e) {
                                Log.e(TAG, "Exception in ThemeWorkerHandler.handleMessage:", e);
                            } catch (IOException e) {
                                Log.e(TAG, "Exception in ThemeWorkerHandler.handleMessage:", e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in ThemeWorkerHandler.handleMessage:", e);
                    }
                    break;
                case MESSAGE_APPLY_CURRENT:
                    setBootanimation();

                    notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    setThemeWallpaper();
                    break;
                case MESSAGE_APPLY_CURRENT_REBOOT:
                    setBootanimation();
                    setThemeWallpaper();

                    reboot();
                    break;
                case MESSAGE_REMOVE_THEME:
                    try {
                        removeCurrentTheme();
                    } catch (IOException e) {}
                    break;
                case MESSAGE_REMOVE_THEME_APPLY:
                    try {
                        removeCurrentTheme();
                        // now notifiy activity manager of the configuration change
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    } catch (IOException e) {}
                    break;
                case MESSAGE_APPLY_ICONS:
                    try {
                        if (iconsDirExists()) {
                            // remove the contents of CUSTOMIZED_ICONS_DIR
                            File file = new File(CUSTOMIZED_ICONS_DIR);
                            if (file.exists()) {
                                for (File f : file.listFiles())
                                    delete(f);
                            }
                        }
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_ICON);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_APPLY_WALLPAPER:
                    setThemeWallpaper();
                    break;
                case MESSAGE_APPLY_SYSTEMUI:
                    try {
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_STATUSBAR);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_APPLY_FRAMEWORK:
                    try {
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_APPLY_LOCKSCREEN:
                    break;
                case MESSAGE_APPLY_RINGTONES:
                    break;
                case MESSAGE_APPLY_BOOTANIMATION:
                    setBootanimation();
                    break;
                case MESSAGE_APPLY_MMS:
                    try {
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_MMS);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_APPLY_FONT:
                    try {
                        // now notifiy activity manager of the configuration change
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);

                        // restart launcher
                        killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_APPLY_FONT_REBOOT:
                    try {
                        reboot();
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_ICONS:
                    try {
                        File icons = new File(THEME_DIR + "/icons");
                        if (icons.exists())
                            delete(icons);
                        if (iconsDirExists()) {
                            // remove the contents of CUSTOMIZED_ICONS_DIR
                            File file = new File(CUSTOMIZED_ICONS_DIR);
                            if (file.exists()) {
                                for (File f : file.listFiles())
                                    delete(f);
                            }
                        }
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_ICON);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_WALLPAPER:
                    resetWallpaper();
                    break;
                case MESSAGE_RESET_SYSTEMUI:
                    try {
                        File systemUi = new File(THEME_DIR + "/com.android.systemui");
                        if (systemUi.exists())
                            delete(systemUi);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_STATUSBAR);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_FRAMEWORK:
                    try {
                        File framework = new File(THEME_DIR + "/framework-res");
                        if (framework.exists())
                            delete(framework);
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_LOCKSCREEN:
                    break;
                case MESSAGE_RESET_RINGTONES:
                    break;
                case MESSAGE_RESET_BOOTANIMATION:
                    resetBootanimation();
                    break;
                case MESSAGE_RESET_MMS:
                    try {
                        File mms = new File(THEME_DIR + "/com.android.mms");
                        if (mms.exists())
                            delete(mms);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_MMS);
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_FONT:
                    try {
                        if (fontsDirExists()) {
                            // remove the contents of FONTS_DIR
                            File file = new File(FONTS_DIR);
                            if (file.exists()) {
                                for (File f : file.listFiles())
                                    delete(f);
                            }
                        }
                    } catch (Exception e) {}
                    break;
                case MESSAGE_RESET_FONT_REBOOT:
                    try {
                        if (fontsDirExists()) {
                            // remove the contents of FONTS_DIR
                            File file = new File(FONTS_DIR);
                            if (file.exists()) {
                                for (File f : file.listFiles())
                                    delete(f);
                            }
                        }
                        reboot();
                    } catch (Exception e) {}
                    break;
            }
        }
    }
}
