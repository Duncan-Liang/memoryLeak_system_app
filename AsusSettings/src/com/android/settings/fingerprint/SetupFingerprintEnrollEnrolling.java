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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.util.SystemBarHelper;
import com.android.setupwizardlib.view.NavigationBar;

public class SetupFingerprintEnrollEnrolling extends FingerprintEnrollEnrolling
        implements NavigationBar.NavigationBarListener {

    private static final String TAG_DIALOG = "dialog";

    @Override
    protected Intent getFinishIntent() {
        final Intent intent = new Intent(this, SetupFingerprintEnrollFinish.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void initViews() {
        SetupWizardUtils.setImmersiveMode(this);

        final View buttonBar = findViewById(R.id.button_bar);
        if (buttonBar != null) {
            buttonBar.setVisibility(View.GONE);
        }

        final NavigationBar navigationBar = getNavigationBar();
        navigationBar.setNavigationBarListener(this);
        navigationBar.getNextButton().setText(R.string.skip_label);
        navigationBar.getBackButton().setVisibility(View.GONE);
    }

    @Override
    protected Button getNextButton() {
        return getNavigationBar().getNextButton();
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        new SkipDialog().show(getFragmentManager(), TAG_DIALOG);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT_ENROLLING_SETUP;
    }

    public static class SkipDialog extends DialogFragment {

        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) == null) {
                super.show(manager, tag);
            }
        }

        public SkipDialog() {
            // no-arg constructor for fragment
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.setup_fingerprint_enroll_enrolling_skip_title)
                    .setMessage(R.string.setup_fingerprint_enroll_enrolling_skip_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.skip_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        activity.setResult(RESULT_SKIP);
                                        activity.finish();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.setup_fingerprint_enroll_enrolling_stay_button,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                    .create();
            SystemBarHelper.hideSystemBars(dialog);
            return dialog;
        }
    }
}
