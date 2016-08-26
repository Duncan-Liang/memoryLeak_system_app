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

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Config;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class SettingsGeoNamesCcByActivity extends AlertActivity {

    private static final String TAG = "SettingsGeoNamesCcByActivity";
    private static final boolean LOGV = false || Config.LOGV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.geo_names_view);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("geonames_cc_by.html")))) {
            StringBuilder data = new StringBuilder(1024);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                data.append(line);
            }
            if (TextUtils.isEmpty(data)) {
                showErrorAndFinish();
            } else {
                TextView legalView = (TextView) findViewById(R.id.geo_names_view);
                legalView.setText(Html.fromHtml(data.toString()));
            }
        } catch (FileNotFoundException e) {
            showErrorAndFinish();
        } catch (IOException e) {
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }
}
