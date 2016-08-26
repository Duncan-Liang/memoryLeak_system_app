package com.android.settings;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
//add by sunxiaolong@wind-mobi.com for asus reverse-charging begin
import android.preference.CheckBoxPreference;
import android.provider.Settings;
//add by sunxiaolong@wind-mobi.com for asus reverse-charging end

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;


public class PowerSettings extends SettingsPreferenceFragment
{
    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "PowerSettings";

    private static final String KEY_ON_AUTOSTART = "on_auto_start";
    private static final String KEY_ON_POWERSAVER = "on_power_saver";
    private static final String KEY_ON_POWERSTATISTICS = "on_power_statistics";
    //add by sunxiaolong@wind-mobi.com for asus reverse-charging begin
    private static final String KEY_ON_REVERGE_CHARGING = "reverse_charging_control";
    //add by sunxiaolong@wind-mobi.com for asus reverse-charging end

    // wangyan@wind-mobi.com add 2016/05/17 for Feature #110135 -s
    private static final boolean WIND_DEF_ASUS_POWERSAVER = SystemProperties.get("ro.wind.def.asus.powersaver").equals("1");
    // wangyan@wind-mobi.com add 2016/05/17 for Feature #110135 -e
    //add by sunxiaolong@wind-mobi.com for asus auto-start begin
    private static final boolean WIND_DEF_ASUS_AUTOSTART = SystemProperties.getBoolean("ro.wind.def.adapt_asus_apk_ww",false);
    //add by sunxiaolong@wind-mobi.com for asus auto-start end

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private Context mContext;

    private Preference mAutoStartPref;
    private Preference mPowerSaverPref;
    //add by sunxiaolong@wind-mobi.com for asus reverse-charging begin
    private CheckBoxPreference mReverseCharging;
    //add by sunxiaolong@wind-mobi.com for asus reverse-charging end
    //private Preference mPowerStatisticsPref;

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Logging.logd(TAG, "PowerSavingSettings onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        addPreferencesFromResource(com.android.settings.R.xml.power_settings);

        mAutoStartPref = findPreference(KEY_ON_AUTOSTART);
        //add by sunxiaolong@wind-mobi.com for asus auto-start begin
        mReverseCharging = (CheckBoxPreference) findPreference(KEY_ON_REVERGE_CHARGING);
        if (!WIND_DEF_ASUS_AUTOSTART) {
            removePreference(KEY_ON_AUTOSTART);
        }
        if(!SystemProperties.getBoolean("ro.wind.otg.reverse.charging",false)){
            removePreference(KEY_ON_REVERGE_CHARGING);
        }
        //add by sunxiaolong@wind-mobi.com for asus auto-start end
        //Check whether user is Owner or not to decide the existence of PowerSaver preference.
        if (UserHandle.USER_OWNER != ActivityManager.getCurrentUser()) {
            removePreference(KEY_ON_POWERSAVER);
        } else {
            mPowerSaverPref = findPreference(KEY_ON_POWERSAVER);
        }
        //mPowerStatisticsPref = findPreference(KEY_ON_POWERSTATISTICS);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        //Logging.logd(TAG, "PowerSavingSettings onPreferenceTreeClick");
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference == mPowerSaverPref) {
            Intent res = new Intent();
            // wangyan@wind-mobi.com add 2016/05/17 for Feature #110135 -s
            String mPackage;
            String mClass;
            if (WIND_DEF_ASUS_POWERSAVER) {
                mPackage = "com.asus.powersaver";
                mClass = ".PowerSaverSettings";
            } else {
                mPackage = "com.wind.windpowersaver";
                mClass = ".MainActivity";
            }
            // wangyan@wind-mobi.com add 2016/05/17 for Feature #110135 -e
            res.setComponent(new ComponentName(mPackage,mPackage+mClass));
            startActivity(res);
        }
        /*
        else if (preference == mPowerStatisticsPref) {
            //ashan_yang@asus.com TT300027
            Intent res = new Intent();
            String mPackage = "com.asus.powerstatistics";
            String mClass = ".PowerHistory";
            res.setComponent(new ComponentName(mPackage,mPackage+mClass));
            startActivity(res);
            //---
        }
        */
        else if (preference == mAutoStartPref) {
            Intent res = new Intent();
            String mPackage = "com.asus.mobilemanager";
            String mClass = ".MainActivity";
            res.setComponent(new ComponentName(mPackage,mPackage+mClass));
            res.putExtra("showNotice", true);
            startActivity(res);
        }
        //add by sunxiaolong@wind-mobi.com for asus reverse charge begin
        else if (preference == mReverseCharging) {
            if (mReverseCharging.isChecked()) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ASUS_OTG_REVERSE_CHARGE_ENABLE,1);
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ASUS_OTG_REVERSE_CHARGE_ENABLE,0);
            }
        }
        //add by sunxiaolong@wind-mobi.com for asus reverse charge end

        return true;
    }

    //add by sunxiaolong@wind-mobi.com for asus reverse charge begin
    @Override
    public void onResume() {
        super.onResume();
        if (mReverseCharging != null) {
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.ASUS_OTG_REVERSE_CHARGE_ENABLE, 1) == 1) {
                mReverseCharging.setChecked(true);
            } else {
                mReverseCharging.setChecked(false);
            }
        }
    }
    //add by sunxiaolong@wind-mobi.com for asus reverse charge end

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.MAIN_SETTINGS;
    }


}
