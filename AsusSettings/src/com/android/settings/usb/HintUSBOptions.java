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
package com.android.settings.usb;


import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.android.settings.R;

/**
 * The class is from Usb dialog patch.
 *
 * For Verizon
 * Show a dialog which is not cancelable
 * User can bypass it by:
 * pressing the button (default behavior for the user)
 * Home (user can not re-enter this dialog since it is exclude from recent apps)
 * Recent App (user can re-enter this dialog if it is not cancelled)
 *
 * In AndroidManifest.xml: android:excludeFromRecents="true"
 * Once cancelled, it will not be shown even by pressing the Recent App
 */
public class HintUSBOptions extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.usb_hint_option_dialog_title)
                .setMessage(R.string.usb_hint_option_message)
                .setCancelable(false)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.usb_hint_option_got_it,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();
        mDialog.show();
    }
}