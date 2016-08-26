package com.android.settings;

import com.android.settings.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.app.ActionBar;

public class AutoBrightnessSettings extends PreferenceActivity
        implements OnPreferenceChangeListener, DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private static final String USE_ADJUSTMENT_KEY = "use_adjustment";
    private static final String RESET_GAIN_KEY = "reset_gain";
    private static final String IN_DARK_KEY = "in_dark";
    private static final String SHOW_SENSOR_USAGE_KEY = "show_sensor_usage";

    private CheckBoxPreference mUseAdjustment;
    private Preference mResetGain;
    private ListPreference mInDark;
    private CheckBoxPreference mShowSensorUsage;

    private Dialog mConfirmDialog;
    PowerManager mPower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actBar = getActionBar();
        if (actBar != null) {
            actBar.setDisplayHomeAsUpEnabled(true);
        }
        addPreferencesFromResource(R.xml.auto_brightness_settings);

        mUseAdjustment = (CheckBoxPreference)findPreference(USE_ADJUSTMENT_KEY);
        mResetGain = findPreference(RESET_GAIN_KEY);
        mInDark = (ListPreference)findPreference(IN_DARK_KEY);
        mInDark.setOnPreferenceChangeListener(this);
        mShowSensorUsage = (CheckBoxPreference)findPreference(SHOW_SENSOR_USAGE_KEY);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAllOptions();
        updateAllSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference == mInDark) {
            writeInDarkOptions(newValue);
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if(Utils.isMonkeyRunning()) {
            return false;
        }

        if(preference == mResetGain) {
            if (mConfirmDialog != null) dismissDialogs();
            mConfirmDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.auto_brightness_settings_reset_gain_confirm)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
        } else if(preference == mUseAdjustment) {
            writeAdjustmentUsageOptions();
        } else if(preference == mShowSensorUsage) {
            writeSensorUsageOptions();
        }

        updateAllSummary();
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mConfirmDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if(mPower == null) mPower = (PowerManager)getSystemService(Context.POWER_SERVICE);
                //mPower.resetAutoBrightnessAdj();
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (dialog == mConfirmDialog) {
            mConfirmDialog = null;
        }
    }

    private void updateAllOptions() {
        updateAdjustmentUsageOptions();
        updateInDarkOptions();
        updateSensorUsageOptions();
    }

    private void updateAdjustmentUsageOptions() {
        /*updateCheckBox(mUseAdjustment, Settings.Global.getInt(getContentResolver(),
                Settings.Global.ASUS_AUTO_BRIGHTNESS_ADJ, 1) != 0);*/
    }

    private void updateInDarkOptions() {
        int value = 2;//Settings.Global.getInt(getContentResolver(),
                //Settings.Global.ASUS_AUTO_BRIGHTNESS_POLICY, 2);
        CharSequence[] values = mInDark.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value == Integer.parseInt(values[i].toString())) {
                mInDark.setValueIndex(i);
                mInDark.setSummary(mInDark.getEntries()[i]);
                return;
            }
        }
        mInDark.setValueIndex(0);
        mInDark.setSummary(mInDark.getEntries()[0]);
    }

    private void updateSensorUsageOptions() {
        /*updateCheckBox(mShowSensorUsage, Settings.Global.getInt(getContentResolver(),
                Settings.Global.SHOW_LIGHT_SENSOR, 0) != 0);*/
    }

    private void updateCheckBox(CheckBoxPreference checkBox, boolean value) {
        checkBox.setChecked(value);
    }

    private void updateAllSummary() {
        mUseAdjustment.setSummary(mUseAdjustment.isChecked() ?
            R.string.auto_brightness_settings_adjustment_on :
            R.string.auto_brightness_settings_adjustment_off);
    }

    private void writeAdjustmentUsageOptions() {
        boolean value = mUseAdjustment.isChecked();
        /*Settings.Global.putInt(getContentResolver(),
                Settings.Global.ASUS_AUTO_BRIGHTNESS_ADJ, value ? 1 : 0);*/
    }

    private void writeInDarkOptions(Object newValue) {
        /*Settings.Global.putInt(getContentResolver(),
                Settings.Global.ASUS_AUTO_BRIGHTNESS_POLICY, Integer.parseInt(newValue.toString()));*/
        updateInDarkOptions();
    }

    private void writeSensorUsageOptions() {
        boolean value = mShowSensorUsage.isChecked();
        /*Settings.Global.putInt(getContentResolver(),
                Settings.Global.SHOW_LIGHT_SENSOR, value ? 1 : 0);*/
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.LightSensorService");
        if (value) {
            startService(service);
        } else {
            stopService(service);
        }
    }

    private void dismissDialogs() {
        if (mConfirmDialog != null) {
            mConfirmDialog.dismiss();
            mConfirmDialog = null;
        }
    }
}
