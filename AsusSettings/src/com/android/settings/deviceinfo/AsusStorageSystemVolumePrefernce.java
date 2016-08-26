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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.storage.VolumeInfo;
import android.preference.Preference;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ProgressBar;

import com.android.settings.R;

import java.io.File;

/***
 * add by sunxiaolong@wind-mobi.com for asus's storage showing
 */

public class AsusStorageSystemVolumePrefernce extends Preference {

    private int mColor;
    private VolumeInfo mVolume;

    public AsusStorageSystemVolumePrefernce(Context context, VolumeInfo volumeInfo, int color) {
        super(context);
        mColor = color;
        mVolume = volumeInfo;
        setLayoutResource(R.layout.asus_system_storage_volume);
        setKey("asus_system_volume");
        setTitle(context.getString(R.string.storage_detail_system));
        long mSystemRetainBytes = 0L;
        if (volumeInfo.isMountedReadable()) {
            final File path = volumeInfo.getPath();
            long totalBytes = path.getTotalSpace(); // total of /data
            final long totalFlashBytes = totalBytes > 16 * TrafficStats.GB_IN_BYTES ? 32 * TrafficStats.GB_IN_BYTES
                    : 16 * TrafficStats.GB_IN_BYTES; // flash space
            final long totalOfdata = totalBytes;
            mSystemRetainBytes = totalFlashBytes - totalOfdata;
        }

        setSummary(context.getString(R.string.storage_volume_summary, Formatter.formatFileSize(context, mSystemRetainBytes),
                Formatter.formatFileSize(context, mSystemRetainBytes)));
        Drawable icon = context.getDrawable(R.drawable.ic_settings_storage);
        icon.mutate();
        icon.setTint(mColor);
        setIcon(icon);
    }

    @Override
    protected void onBindView(View view) {
        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mVolume.getType() == VolumeInfo.TYPE_PRIVATE) {
            progress.setVisibility(View.VISIBLE);
            progress.setProgress(100);
            progress.setProgressTintList(ColorStateList.valueOf(mColor));
        } else {
            progress.setVisibility(View.GONE);
        }
        super.onBindView(view);
    }
}
