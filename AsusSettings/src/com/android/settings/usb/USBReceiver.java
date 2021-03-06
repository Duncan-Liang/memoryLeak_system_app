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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.Utils;
import com.mediatek.settings.FeatureOption;

/**
 * Class from Usb dialog patch.
 * Care the issue of plugging DAC
 * AOA (Android Open Accessory)
 */
public class USBReceiver extends BroadcastReceiver{
    private static final String TAG = "USBReceiver";
    private static final String SHARED_PREFERENCE_NAME = "stateUSBReceiver";
    private static final String PREFERENCE_LAST_CONNECTED_STATE = "connected";
    private static final String PREFERENCE_HAS_SHOWN = "hasShown";
    private static final String SYSTEM_PROPERTY_FACTORY_ADB_ON = "factory.adbon";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            Log.i(TAG, "onReceive: don't support Usb dialog feature. do nothing and exit");
            return;
        }
        // Factory auto-test will execute "fastboot oem adb_enable 1" to turn it on.
        // We bypass this situation to make testing process smoothly.
        if (SystemProperties.getInt(SYSTEM_PROPERTY_FACTORY_ADB_ON, 0) == 1) return;

        if (null == context || null == intent) return;
        // Bypass when Setup Wizard is still running.
        if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) == 0) return;
        if (!UsbManager.ACTION_USB_STATE.equals(intent.getAction())) return;

        boolean connected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);

        SharedPreferences stateSharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCE_NAME, 0);
        boolean lastConnectedState = stateSharedPreferences.getBoolean(
                PREFERENCE_LAST_CONNECTED_STATE, false);
        SharedPreferences.Editor editor = stateSharedPreferences.edit();
        if (connected && !lastConnectedState) {
            // Uncomment it for debugging (thus the dialog can be shown)
            //editor.putBoolean(PREFERENCE_HAS_SHOWN, false).commit();
            // Replace this line for debugging using non-Verizon devices
            //if (!Utils.isVerizonSKU()) {
            if (Utils.isVerizonSKU()) {
                if (stateSharedPreferences.getBoolean(PREFERENCE_HAS_SHOWN, false)) return;
                context.startActivity(new Intent(context, HintUSBOptions.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                context.startActivity(new Intent(context, USBChangeConfigDialog.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            editor.putBoolean(PREFERENCE_HAS_SHOWN, true).commit();
            // Uncomment it for debugging (thus the dialog can be shown)
            //editor.putBoolean(PREFERENCE_HAS_SHOWN, false).commit();
        }
        editor.putBoolean(PREFERENCE_LAST_CONNECTED_STATE, connected).commit();
    }
}
