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
import android.content.Intent;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.android.server.power.ShutdownThread;

import cos.content.res.ExtraConfiguration;
import cos.util.CommandLineUtils;
import cos.util.TTFUtils;

import java.io.*;
import java.lang.Process;
import java.lang.RuntimeException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service responsible for handling applying full themes or parts of themes
 */
public class ThemeManagerService extends IThemeManagerService.Stub {

    public static final String THEME_DIR = "/data/system/theme/";
    public static final String CUSTOMIZED_ICONS_DIR = "/data/system/customized_icons";
    public static final String FONTS_DIR = "/data/fonts";

    public static final String ACTION_THEME_APPLIED = "com.android.server.ThemeManager.action.THEME_APPLIED";
    public static final String ACTION_THEME_NOT_APPLIED = "com.android.server.ThemeManager.action.THEME_NOT_APPLIED";

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

    public void applyDefaultTheme() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_DEFAULT;
        msg.arg1 = 0;
        mHandler.sendMessage(msg);
    }

    /**
     * Apply the theme specified by the URI string provided
     */
    public void applyTheme(String themeURI, boolean applyFont, boolean scaleBoot) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY;
        msg.arg1 = applyFont ? 1 : 0;
        msg.arg2 = scaleBoot ? 1 : 0;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void removeTheme(boolean removeFonts) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        mRemoveFonts = removeFonts;
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_REMOVE_THEME;
        mHandler.sendMessage(msg);
    }

    public void removeThemeAndApply(boolean removeFonts) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        mRemoveFonts = removeFonts;
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_REMOVE_THEME_APPLY;
        mHandler.sendMessage(msg);
    }

    public void applyInstalledTheme(){
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_CURRENT;
        mHandler.sendMessage(msg);
    }

    public void applyInstalledThemeReboot(){
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_CURRENT_REBOOT;
        mHandler.sendMessage(msg);
    }

    public void applyThemeIcons(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_ICONS;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeWallpaper(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_WALLPAPER;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeSystemUI(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_SYSTEMUI;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFramework(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FRAMEWORK;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeRingtone(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_RINGTONE;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeLockscreen(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_LOCKSCREEN;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeBootanimation(String themeURI, boolean scale) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_BOOTANIMATION;
        msg.arg1 = scale ? 1 : 0;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeMms(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_MMS;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFont(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FONT;
        mHandler.sendMessage(msg);
    }

    public void applyThemeFontReboot(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_FONT_REBOOT;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void applyThemeContacts(String themeURI) {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_APPLY_CONTACTS;
        msg.obj = themeURI;
        mHandler.sendMessage(msg);
    }

    public void updateSystemUI() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_UPDATE_SYSTEMUI;
        mHandler.sendMessage(msg);
    }

    public void resetThemeIcons() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_ICONS;
        mHandler.sendMessage(msg);
    }

    public void resetThemeWallpaper() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_WALLPAPER;
        mHandler.sendMessage(msg);
    }

    public void resetThemeSystemUI() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_SYSTEMUI;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFramework() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FRAMEWORK;
        mHandler.sendMessage(msg);
    }

    public void resetThemeRingtone() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_RINGTONE;
        mHandler.sendMessage(msg);
    }

    public void resetThemeLockscreen() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_LOCKSCREEN;
        mHandler.sendMessage(msg);
    }

    public void resetThemeBootanimation() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_BOOTANIMATION;
        mHandler.sendMessage(msg);
    }

    public void resetThemeMms() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_MMS;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFont() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FONT;
        mHandler.sendMessage(msg);
    }

    public void resetThemeFontReboot() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_FONT_REBOOT;
        mHandler.sendMessage(msg);
    }

    public void resetThemeContacts() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.ACCESS_THEME_SERVICE, null);
        Message msg = Message.obtain();
        msg.what = ThemeWorkerHandler.MESSAGE_RESET_CONTACTS;
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
                run(String.format("invoke-as -u root chmod 0775 %s", THEME_DIR));
            }

            Message msg = Message.obtain();
            msg.what = ThemeWorkerHandler.MESSAGE_APPLY_DEFAULT;
            msg.arg1 = 1;
            mHandler.sendMessage(msg);
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
                run(String.format("invoke-as -u root chmod 0775 %s", CUSTOMIZED_ICONS_DIR));
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
                run(String.format("invoke-as -u root chmod 0775 %s", FONTS_DIR));
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
    
        resetBootanimation();
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
            notifyThemeApplied();
        } catch (Exception e) {
            notifyThemeNotApplied();
        }
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
            run(String.format("invoke-as -u root chmod 0644 %s", "/data/local/bootanimation.zip"));
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

    private void removeBadFonts() {
        // go through and remove any invalid fonts to prevent system from hanging
        for (String s : (new File(FONTS_DIR)).list()) {
            if (!TTFUtils.isValidTtf(FONTS_DIR + "/" + s)) {
               (new File(FONTS_DIR + s)).delete();
            }
        }
    }

    private FileInputStream getFileInputStream(String themeUri)
            throws FileNotFoundException {
        Uri uri = Uri.parse(themeUri);
        ParcelFileDescriptor file = null;
        file = mContext.getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fd = file.getFileDescriptor();

        return new FileInputStream(fd);
    }

    private void extractFileFromTheme(String themeUri, String file, String dstPath)
            throws FileNotFoundException, IOException  {
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(getFileInputStream(themeUri)));
        ZipEntry ze = null;
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().equals(file)) {
                copyInputStream(zip,
                        new BufferedOutputStream(new FileOutputStream(dstPath + ze.getName())));
                (new File(dstPath + ze.getName())).setReadable(true, false);
                zip.closeEntry();
                break;
            }
        }
        zip.close();
    }

    private void extractDirectoryFromTheme(String themeUri, String file, String dstPath)
            throws FileNotFoundException, IOException  {
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(getFileInputStream(themeUri)));
        ZipEntry ze = null;
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().contains(file)) {
                if (ze.isDirectory()) {
                    // Assume directories are stored parents first then children
                    File dir = new File(dstPath + ze.getName());
                    dir.mkdir();
                    dir.setReadable(true, false);
                    dir.setWritable(true, false);
                    dir.setExecutable(true, false);
                    zip.closeEntry();
                    continue;
                }

                copyInputStream(zip,
                        new BufferedOutputStream(new FileOutputStream(dstPath + ze.getName())));
                (new File(dstPath + ze.getName())).setReadable(true, false);
                zip.closeEntry();
            }
        }
        zip.close();
    }

    private void extractBootAnimationFromTheme(String themeUri, String file, String dstPath, boolean scale)
            throws FileNotFoundException, IOException  {
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(getFileInputStream(themeUri)));
        ZipEntry ze = null;
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.getName().contains(file)) {
                if (ze.isDirectory()) {
                    // Assume directories are stored parents first then children
                    File dir = new File(dstPath + ze.getName());
                    dir.mkdir();
                    dir.setReadable(true, false);
                    dir.setWritable(true, false);
                    dir.setExecutable(true, false);
                    zip.closeEntry();
                    continue;
                }

                if (scale) {
                    scaleBootAnimation(zip, dstPath + ze.getName());
                }
                else
                    copyInputStream(zip,
                            new BufferedOutputStream(new FileOutputStream(dstPath + ze.getName())));
                (new File(dstPath + ze.getName())).setReadable(true, false);
                zip.closeEntry();
                break;
            }
        }
        zip.close();
    }

    private void notifyThemeApplied() {
        Intent intent = new Intent();
        intent.setAction(ACTION_THEME_APPLIED);
        mContext.sendBroadcast(intent);
    }

    private void notifyThemeNotApplied() {
        Intent intent = new Intent();
        intent.setAction(ACTION_THEME_NOT_APPLIED);
        mContext.sendBroadcast(intent);
    }

    private void scaleBootAnimation(InputStream input, String dst)
            throws IOException {
        OutputStream os = new FileOutputStream(dst);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
        ZipInputStream bootAni = new ZipInputStream(new BufferedInputStream(input));
        ZipEntry ze = null;

        zos.setMethod(ZipOutputStream.STORED);
        byte[] bytes = new byte[1024];
        int len = 0;
        CRC32 crc32 = new CRC32();
        while ((ze = bootAni.getNextEntry()) != null) {
            crc32.reset();
            ZipEntry entry = new ZipEntry(ze.getName());
            entry.setMethod(ZipEntry.STORED);
            entry.setCrc(ze.getCrc());
            entry.setSize(ze.getSize());
            entry.setCompressedSize(ze.getSize());
            Bitmap bmp = null;
            Log.d(TAG, "scaleBootAnimation - " + ze.getName());
            if (!ze.getName().equals("desc.txt")) {
                // just copy this entry straight over into the output zip
                zos.putNextEntry(entry);
                while ((len = bootAni.read(bytes)) > 0) {
                    zos.write(bytes, 0, len);
                }
            } else {
                String line = "";
                BufferedReader reader = new BufferedReader(new InputStreamReader(bootAni));
                String[] info = reader.readLine().split(" ");

                int scaledWidth = 0;
                int scaledHeight = 0;
                WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(dm);
                // just in case the device is in landscape orientation we will
                // swap the values since most (if not all) animations are portrait
                if (dm.widthPixels > dm.heightPixels) {
                    scaledWidth = dm.heightPixels;
                    scaledHeight = dm.widthPixels;
                } else {
                    scaledWidth = dm.widthPixels;
                    scaledHeight = dm.heightPixels;
                }

                int width = Integer.parseInt(info[0]);
                int height = Integer.parseInt(info[1]);

                if (width == height)
                    scaledHeight = scaledWidth;
                else {
                    float scale = (float)scaledWidth / (float)width;
                    int newHeight = (int)((float)height * scale);
                    if (newHeight < scaledHeight)
                        scaledHeight = newHeight;
                }

                crc32.reset();
                int size = 0;
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                line = String.format("%d %d %s\n", scaledWidth, scaledHeight, info[2]);
                buffer.put(line.getBytes());
                size += line.getBytes().length;
                crc32.update(line.getBytes());
                while ((line = reader.readLine()) != null) {
                    line = String.format("%s\n", line);
                    buffer.put(line.getBytes());
                    size += line.getBytes().length;
                    crc32.update(line.getBytes());
                }
                entry.setCrc(crc32.getValue());
                entry.setSize(size);
                entry.setCompressedSize(size);
                zos.putNextEntry(entry);
                zos.write(buffer.array(), 0, size);
            }
            zos.closeEntry();
        }
        zos.close();
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
        private static final int MESSAGE_APPLY_RINGTONE = 10;
        private static final int MESSAGE_APPLY_BOOTANIMATION = 11;
        private static final int MESSAGE_APPLY_MMS = 12;
        private static final int MESSAGE_APPLY_FONT = 13;
        private static final int MESSAGE_APPLY_FONT_REBOOT = 14;
        private static final int MESSAGE_APPLY_CONTACTS = 15;
        private static final int MESSAGE_APPLY_DEFAULT = 16;
        private static final int MESSAGE_RESET_ICONS = 20;
        private static final int MESSAGE_RESET_WALLPAPER = 21;
        private static final int MESSAGE_RESET_SYSTEMUI = 22;
        private static final int MESSAGE_RESET_FRAMEWORK = 23;
        private static final int MESSAGE_RESET_LOCKSCREEN = 24;
        private static final int MESSAGE_RESET_RINGTONE = 25;
        private static final int MESSAGE_RESET_BOOTANIMATION = 26;
        private static final int MESSAGE_RESET_MMS = 27;
        private static final int MESSAGE_RESET_FONT = 28;
        private static final int MESSAGE_RESET_FONT_REBOOT = 29;
        private static final int MESSAGE_RESET_CONTACTS = 30;
        private static final int MESSAGE_UPDATE_SYSTEMUI = 40;

        @Override
        public void handleMessage(Message msg) {
            String themeURI;
            boolean done = false;
            switch(msg.what) {
                case MESSAGE_APPLY:
                    try {
                        themeURI = (String)msg.obj;
                        boolean applyFont = msg.arg1 == 1;
                        boolean scaleBoot = msg.arg2 == 1;
                        if (msg.what == MESSAGE_APPLY && !TextUtils.isEmpty(themeURI)) {
                            Log.i(TAG, "applying theme " + themeURI);
                            try{
                                // clear out the old theme first
                                removeCurrentTheme();

                                if (applyFont && fontsDirExists()) {
                                    // remove the contents of FONTS_DIR
                                    File file = new File(FONTS_DIR);
                                    if (file.exists()) {
                                        for (File f : file.listFiles())
                                            delete(f);
                                    }
                                }

                                ZipInputStream zip = new ZipInputStream(new BufferedInputStream(getFileInputStream(themeURI)));
                                ZipEntry ze = null;
                                while ((ze = zip.getNextEntry()) != null) {
                                    if (ze.getName().contains("fonts/")) {
                                        if (ze.isDirectory()) {
                                        } else if (applyFont) {
                                            copyInputStream(zip,
                                                    new BufferedOutputStream(new FileOutputStream("/data/" + ze.getName())));
                                            (new File("/data/" + ze.getName())).setReadable(true, false);
                                        }
                                    } else {
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
                                        
                                        if (scaleBoot && ze.getName().contains("bootanimation.zip"))
                                            scaleBootAnimation(zip, "/data/system/theme/" + ze.getName());
                                        else
                                            copyInputStream(zip,
                                                    new BufferedOutputStream(
                                                    new FileOutputStream("/data/system/theme/" + ze.getName())));
                                        (new File("/data/system/theme/" + ze.getName())).setReadable(true, false);
                                    }
                                    zip.closeEntry();
                                }
            
                                zip.close();
                                setBootanimation();
                                setThemeWallpaper();
                                if (applyFont) {
                                    removeBadFonts();
                                    reboot();
                                }
                                // now notifiy activity manager of the configuration change
                                notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);

                                // restart launcher
                                killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
                            } catch (FileNotFoundException e) {
                                notifyThemeNotApplied();
                            } catch (IOException e) {
                                notifyThemeNotApplied();
                            }
                        }
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_DEFAULT:
                    boolean isSystemCall = msg.arg1 == 1;
                    try {
                        // clear out the old theme first
                        removeCurrentTheme();

                        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream("/system/media/default.ctz")));
                        ZipEntry ze = null;
                        while ((ze = zip.getNextEntry()) != null) {
                            if (ze.isDirectory()) {
                                // Assume directories are stored parents first then children
                                File dir = new File("/data/system/theme/" + ze.getName());
                                dir.mkdir();
                                dir.setReadable(true, false);
                                dir.setWritable(true, false);
                                dir.setExecutable(true, false);
                                zip.closeEntry();
                                continue;
                            }
                                        
                            copyInputStream(zip,
                                    new BufferedOutputStream(
                                    new FileOutputStream("/data/system/theme/" + ze.getName())));
                            (new File("/data/system/theme/" + ze.getName())).setReadable(true, false);
                            zip.closeEntry();
                        }
            
                        zip.close();
                        setBootanimation();
                        setThemeWallpaper();

                        if (!isSystemCall) {
                            // now notifiy activity manager of the configuration change
                            notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                        }

                        // restart launcher
                        killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
                    } catch (Exception e) {
                        if (!isSystemCall) 
                            notifyThemeNotApplied();
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
                        if (mRemoveFonts)
                            reboot();
                        // now notifiy activity manager of the configuration change
                        else
                            notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    } catch (IOException e) {}
                    break;
                case MESSAGE_APPLY_ICONS:
                    try {
                        themeURI = (String)msg.obj;
                        extractFileFromTheme(themeURI, "icons", THEME_DIR);
                        if (iconsDirExists()) {
                            // remove the contents of CUSTOMIZED_ICONS_DIR
                            File file = new File(CUSTOMIZED_ICONS_DIR);
                            if (file.exists()) {
                                for (File f : file.listFiles())
                                    delete(f);
                            }
                        }
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_ICON);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_WALLPAPER:
                    themeURI = (String)msg.obj;
                    try {
                        extractDirectoryFromTheme(themeURI, "wallpaper/", THEME_DIR);
                        setThemeWallpaper();
                        notifyThemeApplied();
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_SYSTEMUI:
                    try {
                        themeURI = (String)msg.obj;
                        extractFileFromTheme(themeURI, "com.android.systemui", THEME_DIR);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_STATUSBAR);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_FRAMEWORK:
                    try {
                        themeURI = (String)msg.obj;
                        extractFileFromTheme(themeURI, "framework-res", THEME_DIR);
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_LOCKSCREEN:
                    themeURI = (String)msg.obj;
                    break;
                case MESSAGE_APPLY_RINGTONE:
                    themeURI = (String)msg.obj;
                    try {
                        String fileName = themeURI.substring(themeURI.lastIndexOf('/') + 1);
                        copyInputStream(getFileInputStream(themeURI),
                                new BufferedOutputStream(
                                new FileOutputStream(THEME_DIR + fileName)));
                        (new File(THEME_DIR + fileName)).setReadable(true, false);
                        notifyThemeApplied();
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_BOOTANIMATION:
                    themeURI = (String)msg.obj;
                    boolean scaleBoot = msg.arg1 == 1;
                    try {
                        extractBootAnimationFromTheme(themeURI, "boots/", THEME_DIR, scaleBoot);
                        setBootanimation();
                        notifyThemeApplied();
                    } catch (Exception e) {
                        e.printStackTrace();
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_MMS:
                    try {
                        themeURI = (String)msg.obj;
                        extractFileFromTheme(themeURI, "com.android.mms", THEME_DIR);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_MMS);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_FONT:
                    try {
                        themeURI = (String)msg.obj;
                        extractDirectoryFromTheme(themeURI, "fonts/", "/data/");
                        removeBadFonts();
                        // now notifiy activity manager of the configuration change
                        notifyThemeUpdate(ExtraConfiguration.SYSTEM_INTRESTE_CHANGE_FLAG);

                        // restart launcher
                        killProcess(ExtraConfiguration.LAUNCHER_PKG_NAME);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_FONT_REBOOT:
                    try {
                        themeURI = (String)msg.obj;
                        extractDirectoryFromTheme(themeURI, "fonts/", "/data/");
                        removeBadFonts();
                        notifyThemeApplied();
                        reboot();
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_APPLY_CONTACTS:
                    try {
                        themeURI = (String)msg.obj;
                        extractFileFromTheme(themeURI, "com.android.contacts", THEME_DIR);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_CONTACT);
                    } catch (Exception e) {
                        notifyThemeNotApplied();
                    }
                    break;
                case MESSAGE_UPDATE_SYSTEMUI:
                    try {
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_STATUSBAR);
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
                case MESSAGE_RESET_RINGTONE:
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
                case MESSAGE_RESET_CONTACTS:
                    try {
                        File contacts = new File(THEME_DIR + "/com.android.contacts");
                        if (contacts.exists())
                            delete(contacts);
                        notifyThemeUpdate(ExtraConfiguration.THEME_FLAG_CONTACT);
                    } catch (Exception e) {}
                    break;
           }
        }
    }
}
