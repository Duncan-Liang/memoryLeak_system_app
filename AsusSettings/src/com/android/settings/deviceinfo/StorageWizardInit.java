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

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import com.android.settings.R;
/**add by xulinchao@wind-mobi.com 2016.06.29 start for remove Internal choice**/
import android.view.View;
import android.os.SystemProperties;
/**add by xulinchao@wind-mobi.com 2016.06.29 end for remove Internal choice**/

public class StorageWizardInit extends StorageWizardBase {
    private RadioButton mRadioExternal;
    private RadioButton mRadioInternal;

    private boolean mIsPermittedToAdopt;
    /**add by xulinchao@wind-mobi.com 2016.06.29 start for remove Internal choice**/
    private static final boolean WIND_DEF_ADAPT_FOR_ASUS_APK_CN = SystemProperties.get("ro.wind.def.adapt_asus_apk_cn").equals("1");
    /**add by xulinchao@wind-mobi.com 2016.06.29 end for remove Internal choice**/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init);

        mIsPermittedToAdopt = UserManager.get(this).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        setIllustrationInternal(true);
        setHeaderText(R.string.storage_wizard_init_title, mDisk.getDescription());

        mRadioExternal = (RadioButton) findViewById(R.id.storage_wizard_init_external_title);
        mRadioInternal = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);

        mRadioExternal.setOnCheckedChangeListener(mRadioListener);
        mRadioInternal.setOnCheckedChangeListener(mRadioListener);

        findViewById(R.id.storage_wizard_init_external_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);
        /**add by xulinchao@wind-mobi.com 2016.06.29 start remove Internal choice **/
        if(WIND_DEF_ADAPT_FOR_ASUS_APK_CN){
            findViewById(R.id.storage_wizard_init_internal_summary).setVisibility(View.GONE);
            mRadioInternal.setVisibility(View.GONE);
        }
        /**add by xulinchao@wind-mobi.com 2016.06.29 end remove Internal choice **/
        getNextButton().setEnabled(false);

        if (!mDisk.isAdoptable()) {
            // If not adoptable, we only have one choice
            mRadioExternal.setChecked(true);
            onNavigateNext();
            finish();
        }

        // TODO: Show a message about why this is disabled for guest and that only an admin user
        // can adopt an sd card.
        if (!mIsPermittedToAdopt) {
            mRadioInternal.setEnabled(false);
        }
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == mRadioExternal) {
                    mRadioInternal.setChecked(false);
                    setIllustrationInternal(false);
                } else if (buttonView == mRadioInternal) {
                    mRadioExternal.setChecked(false);
                    setIllustrationInternal(true);
                }
                getNextButton().setEnabled(true);
            }
        }
    };

    @Override
    public void onNavigateNext() {
        if (mRadioExternal.isChecked()) {
            if (mVolume != null && mVolume.getType() == VolumeInfo.TYPE_PUBLIC
                    && mVolume.getState() != VolumeInfo.STATE_UNMOUNTABLE) {
                // Remember that user made decision
                mStorage.setVolumeInited(mVolume.getFsUuid(), true);

                final Intent intent = new Intent(this, StorageWizardReady.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                startActivity(intent);

            } else {
                // Gotta format to get there
                final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
                startActivity(intent);
            }

        } else if (mRadioInternal.isChecked()) {
            final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            startActivity(intent);
        }
    }
}
