/*
 * Copyright (C) 2007 The Android Open Source Project
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
package com.android.settings.lockscreen;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class AsusLSDBHelper {
    private static final String TAG = "AsusLSDBHelper";
    private static final boolean DEBUG_FLAG = true;//AsusLSUtils.DEBUG_FLAG;

    public static final String AUTHORITY = "com.asus.keyguard.asuslockscreenprovider";
    public static final String URL_ENABLE_WEATHER_ANIMATION = "EnableWeatherAnimation";
    public static final String URL_DEVICE_SUPPORT_WEATHER_ANIMATION = "DeviceSupportWeatherAnimation";
    public static final String URL_DEVICE_SUPPORT_LS_WALLPAPER = "DeviceSupportLSWallpaper";
    public static final String URL_DEVICE_SUPPORT_LS_THEME = "DeviceSupportLSTheme";
    public static final String URL_DEVICE_SUPPORT_LS_CLOCK_WIDGET = "DeviceSupportLSClockWidget";

    public static final String[] COLUMS_EWA = new String[]{
            URL_ENABLE_WEATHER_ANIMATION
    };
    public static final String[] COLUMS_DSWA = new String[]{
            URL_DEVICE_SUPPORT_WEATHER_ANIMATION
    };
    public static final String[] COLUMS_DSLSW = new String[]{
            URL_DEVICE_SUPPORT_LS_WALLPAPER
    };
    public static final String[] COLUMS_DSLST = new String[]{
            URL_DEVICE_SUPPORT_LS_THEME
    };
    public static final String[] COLUMS_DSLSCW = new String[]{
            URL_DEVICE_SUPPORT_LS_CLOCK_WIDGET
    };
    public static final String URI_WEATHER_ANIMATION_SETTING =
            "content://" + AUTHORITY + "/" + URL_ENABLE_WEATHER_ANIMATION;
    public static final String URI_DEVICE_SUPPORT_WEATHER_ANIMATION_SETTING =
            "content://" + AUTHORITY + "/" + URL_DEVICE_SUPPORT_WEATHER_ANIMATION;
    public static final String URI_DEVICE_SUPPORT_LS_WALLPAPER_SETTING =
            "content://" + AUTHORITY + "/" + URL_DEVICE_SUPPORT_LS_WALLPAPER;
    public static final String URI_DEVICE_SUPPORT_LS_THEME_SETTING =
            "content://" + AUTHORITY + "/" + URL_DEVICE_SUPPORT_LS_THEME;
    public static final String URL_DEVICE_SUPPORT_LS_CLOCK_WIDGET_SETTING =
            "content://" + AUTHORITY + "/" + URL_DEVICE_SUPPORT_LS_CLOCK_WIDGET;

    // Preference Weather Animation
    public static final int VALUE_WEATHER_ANIMATION_NONE = -1;
    public static final int VALUE_WEATHER_ANIMATION_DISABLE = 0;
    public static final int VALUE_WEATHER_ANIMATION_ENABLE = 1;
    // For Clock Widget
    public static final int VALUE_LS_CLOCK_WIDGET_SHOW = 0;
    public static final int VALUE_LS_CLOCK_WIDGET_HIDE = 1;

    public static boolean getWeatherAnimationSetting(Context context) {
        boolean enableWA = false;
        int enable = VALUE_WEATHER_ANIMATION_NONE;
        Cursor cursor = null;
        try {
            Uri uri = getUriWithUserId(URI_WEATHER_ANIMATION_SETTING);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isAfterLast()) {
                int index = cursor.getColumnIndex(COLUMS_EWA[0]);
                if (index >= 0) {
                    enable = cursor.getInt(index);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getEnableWA E: " + e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
        enableWA = (enable == VALUE_WEATHER_ANIMATION_ENABLE);
        if (DEBUG_FLAG) Log.i(TAG, "getEnableWA: " + enableWA);
        return enableWA;
    }

    public static void setWeatherAnimationSetting(Context context, boolean enable) {
        try {
            Uri uri = getUriWithUserId(URI_WEATHER_ANIMATION_SETTING);
            int enableWA = enable ? VALUE_WEATHER_ANIMATION_ENABLE : VALUE_WEATHER_ANIMATION_DISABLE;
            ContentValues values = new ContentValues();
            values.put(URL_ENABLE_WEATHER_ANIMATION, enableWA);
            int result = context.getContentResolver().update(uri, values, null, null);
            if (DEBUG_FLAG) Log.i(TAG, "setEnableWA: " + enableWA + " uri: " + result);
        } catch (Exception e) {
            Log.w(TAG, "setEnableWA E: " + e);
        }
    }

    public static boolean getDeviceSupportWeatherAnimation(Context context) {
        boolean supportWA = false;
        int support = -1;
        Cursor cursor = null;
        try {
            Uri uri = getUriWithUserId(URI_DEVICE_SUPPORT_WEATHER_ANIMATION_SETTING);
            if (DEBUG_FLAG) Log.i(TAG, "getSupportWA: " + uri);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isAfterLast()) {
                int index = cursor.getColumnIndex(COLUMS_DSWA[0]);
                if (index >= 0) {
                    support = cursor.getInt(index);
                }
            }
            if (DEBUG_FLAG) Log.i(TAG, "getSupportWA: " + cursor + ", " + support);
        } catch (Exception e) {
            Log.w(TAG, "getSupportWA E: " + e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
        supportWA = (support == 1);
        if (DEBUG_FLAG) Log.i(TAG, "getSupportWA: " + supportWA);
        return supportWA;
    }

    public static boolean getDeviceSupportLSWallpaper(Context context) {
        boolean supportLSW = false;
        int support = -1;
        Cursor cursor = null;
        try {
            Uri uri = getUriWithUserId(URI_DEVICE_SUPPORT_LS_WALLPAPER_SETTING);
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLSW: " + uri);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isAfterLast()) {
                int index = cursor.getColumnIndex(COLUMS_DSLSW[0]);
                if (index >= 0) {
                    support = cursor.getInt(index);
                }
            }
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLSW: " + cursor + ", " + support);
        } catch (Exception e) {
            Log.w(TAG, "getSupportLSW E: " + e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
        supportLSW = (support == 1);
        if (DEBUG_FLAG) Log.i(TAG, "getSupportLSW: " + supportLSW);
        return supportLSW;
    }

    public static boolean getDeviceSupportLSTheme(Context context) {
        boolean supportLST = false;
        int support = -1;
        Cursor cursor = null;
        try {
            Uri uri = getUriWithUserId(URI_DEVICE_SUPPORT_LS_THEME_SETTING);
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLST: " + uri);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isAfterLast()) {
                int index = cursor.getColumnIndex(COLUMS_DSLST[0]);
                if (index >= 0) {
                    support = cursor.getInt(index);
                }
            }
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLST: " + cursor + ", " + support);
        } catch (Exception e) {
            Log.w(TAG, "getSupportLST E: " + e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
        supportLST = (support == 1);
        if (DEBUG_FLAG) Log.i(TAG, "getSupportLST: " + supportLST);
        return supportLST;
    }

    public static final String ASUS_SETTING_KEYGARD_BAR_CLOCK_SHOW = "asus_keyguard_bar_clock_show";

    public static boolean getLSClockWidgetSetting(Context context) {
        boolean showLSCW = true;
        try {
            showLSCW = (Settings.Secure.getIntForUser(
                    context.getContentResolver(),
                    ASUS_SETTING_KEYGARD_BAR_CLOCK_SHOW,
                    VALUE_LS_CLOCK_WIDGET_SHOW,
                    UserHandle.USER_CURRENT) == VALUE_LS_CLOCK_WIDGET_SHOW);
        } catch (Exception e) {
            Log.w(TAG, "getLSClockWidget: " + e);
        }
        return showLSCW;
    }

    public static void setLSClockWidgetSetting(Context context, boolean show) {
        if (DEBUG_FLAG) Log.i(TAG, "setLSClockWidget: " + show);
        try {
            Settings.Secure.putIntForUser(
                    context.getContentResolver(),
                    ASUS_SETTING_KEYGARD_BAR_CLOCK_SHOW,
                    show ? VALUE_LS_CLOCK_WIDGET_SHOW : VALUE_LS_CLOCK_WIDGET_HIDE,
                    UserHandle.USER_CURRENT);
        } catch (Exception e) {
            Log.w(TAG, "setLSClockWidget: " + e);
        }
    }

    public static boolean getDeviceSupportLSClockWidget(Context context) {
        //return getDeviceSupportLSTheme(context);
        boolean supportLSCW = false;
        int support = -1;
        Cursor cursor = null;
        try {
            Uri uri = getUriWithUserId(URL_DEVICE_SUPPORT_LS_CLOCK_WIDGET_SETTING);
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLSCW: " + uri);
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isAfterLast()) {
                int index = cursor.getColumnIndex(COLUMS_DSLSCW[0]);
                if (index >= 0) {
                    support = cursor.getInt(index);
                }
            }
            if (DEBUG_FLAG) Log.i(TAG, "getSupportLSCW: " + cursor + ", " + support);
        } catch (Exception e) {
            Log.w(TAG, "getSupportLSCW E: " + e);
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
            }
        }
        supportLSCW = (support == 1);
        if (DEBUG_FLAG) Log.i(TAG, "getSupportLSCW: " + supportLSCW);
        return supportLSCW;
    }

    private static Uri getUriWithUserId(String strUri) {
        Uri inUri = Uri.parse(strUri);
        return ContentProvider.maybeAddUserId(
                ContentProvider.getUriWithoutUserId(inUri),
                ActivityManager.getCurrentUser());
    }
}