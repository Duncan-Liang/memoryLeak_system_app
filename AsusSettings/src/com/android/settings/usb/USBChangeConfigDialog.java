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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

/**
 * Class from Usb dialog patch.
 */
public final class USBChangeConfigDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        buildDialog();
        registerReceiver(mUsbDeviceReceiver,
                new IntentFilter(UsbManager.ACTION_USB_STATE));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbDeviceReceiver);
        super.onDestroy();
    }

    /**
     * BUTTON_POSITIVE:
     * Use MTP for file transfers
     * TODO: check if MTP is not supported
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                manager.setUsbDataUnlocked(true);
                manager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
            default:
                break;
        }
    }

    /**
     * TODO: cancelable or not
     */
    private void buildDialog() {
        final AlertController.AlertParams p = mAlertParams;

        p.mTitle = getString(R.string.usb_change_config_dialog_title);
        p.mMessage = getString(R.string.usb_change_config_dialog_message);
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.cancel);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;

        setupAlert();
    }

    /**
     * If USB_DATA_UNLOCKED is true:
     * Assume MTP / PTP / MIDI is enabled -> dismiss the dialog
     * TODO: check if MTP / PTP / MIDI is enabled
     */
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            if (UsbManager.ACTION_USB_STATE.equals(intent.getAction())) {
                if (!intent.getBooleanExtra(UsbManager.USB_CONNECTED, false) ||
                        intent.getBooleanExtra(
                                UsbManager.USB_DATA_UNLOCKED, false)) dismiss();
            }
        }
    };
}
