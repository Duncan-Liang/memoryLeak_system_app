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

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;

import android.util.Log;

public class AsusLSUtils {

    public static final boolean DEBUG_FLAG = false;
    private static final String TAG = "AsusLSUtils";

    public static Intent getSettingLSWallpaperIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.asus.launcher",
                "com.asus.themeapp.ThemeAppActivity"));
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("tabPosition", 1);
        return intent;
    }

    public static Intent getSettingLSThemeIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.asus.themeapp",
                "com.asus.themeapp.ThemeAppActivity"));
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("from", "com.android.systemui.lockscreen");
        return intent;
    }

    private static final String THEME_APP_PACKAGE_NAME = "com.asus.themeapp";

    public static boolean isThemeAppEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(THEME_APP_PACKAGE_NAME, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "isThemeAppEnabled E=" + e);
            return false;
        }
    }
}