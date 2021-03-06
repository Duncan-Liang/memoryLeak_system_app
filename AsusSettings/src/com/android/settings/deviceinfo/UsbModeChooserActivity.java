/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

/**
 * UI for the USB chooser dialog.
 *
 */
public class UsbModeChooserActivity extends Activity {

    public static final int[] DEFAULT_MODES = {
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE,
        UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI,
        /// M: Add for Built-in CD-ROM and USB Mass Storage @{
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE,
        UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR
        /// M: @}
    };

    private UsbBackend mBackend;
    private AlertDialog mDialog;
    private LayoutInflater mLayoutInflater;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
       //zhangtao@wind-mobi.com 2016/7/23 add begain
       setTheme(android.R.style.Theme_Material_Light_Dialog_NoActionBar);
       //zhangtao@wind-mobi.com  2016/7/23 add end
        super.onCreate(savedInstanceState);

        mLayoutInflater = LayoutInflater.from(this);
        //zhangtao@wind-mobi.com 2016/7/23 modify begain
        /*
        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.usb_use)
                .setView(R.layout.usb_dialog_container)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();*/
        mDialog = new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_Dialog_NoActionBar)
                .setTitle(R.string.usb_use)
                .setView(R.layout.usb_dialog_container)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create();
        //zhangtao@wind-mobi.com 2016/7/23 modify end
        mDialog.show();

        LinearLayout container = (LinearLayout) mDialog.findViewById(R.id.container);

        mBackend = new UsbBackend(this);
        int current = mBackend.getCurrentMode();
        for (int i = 0; i < DEFAULT_MODES.length; i++) {
            if (mBackend.isModeSupported(DEFAULT_MODES[i])) {
                inflateOption(DEFAULT_MODES[i], current == DEFAULT_MODES[i], container);
            }
        }
    }

    private void inflateOption(final int mode, boolean selected, LinearLayout container) {

          //zhangtao@wind-mobi.com 2016/7/23 modify begain
          /*View v = mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);
          ((TextView) v.findViewById(android.R.id.title)).setText(getTitle(mode));
          ((TextView) v.findViewById(android.R.id.summary)).setText(getSummary(mode));

          v.setOnClickListener(new OnClickListener() {
              @Override
              public void onClick(View v) {
                  if (!ActivityManager.isUserAMonkey()) {
                      mBackend.setMode(mode);
                  }
                  mDialog.dismiss();
                  finish();
              }
          });
          ((Checkable) v).setChecked(selected);
          container.addView(v);*/
        if(getTitle(mode)!= R.string.usb_use_mass_storage && getTitle(mode)!= R.string.usb_use_built_in_cd_rom) {
            View v = mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);

        ((TextView) v.findViewById(android.R.id.title)).setText(getTitle(mode));
        ((TextView) v.findViewById(android.R.id.summary)).setText(getSummary(mode));

            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!ActivityManager.isUserAMonkey()) {
                        mBackend.setMode(mode);
                    }
                    mDialog.dismiss();
                    finish();
                }
            });
            ((Checkable) v).setChecked(selected);
            container.addView(v);
        }
        //zhangtao@wind-mobi.com 2016/7/23 modify end

    }

    private static int getSummary(int mode) {
        switch (mode) {
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_charging_only_desc;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_power_only_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_use_file_transfers_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_use_photo_transfers_desc;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_use_MIDI_desc;
            /// M: Add for Built-in CD-ROM and USB Mass Storage @{
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE:
                return R.string.usb_ums_summary;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR:
                return R.string.usb_bicr_summary;
            /// M: @}
        }
        return 0;
    }

    private static int getTitle(int mode) {
        switch (mode) {
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_charging_only;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_use_power_only;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_use_file_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_use_photo_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_use_MIDI;
            /// M: Add for Built-in CD-ROM and USB Mass Storage @{
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MASS_STORAGE:
                return R.string.usb_use_mass_storage;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_BICR:
                return R.string.usb_use_built_in_cd_rom;
            /// M: @}
        }
        return 0;
    }
}
