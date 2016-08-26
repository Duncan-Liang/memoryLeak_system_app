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
package com.android.settings;

import android.app.Fragment;
import android.os.Bundle;

import android.util.Log;

import com.android.settings.lockscreen.AsusLSUtils;

public class LockscreenWallpaperPreference extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(AsusLSUtils.getSettingLSWallpaperIntent());
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("LSWallpaper", "startActivity E: " + e);
        }
        getActivity().finish();
    }
}