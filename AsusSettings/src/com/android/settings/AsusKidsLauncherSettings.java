// xuyi@wind-mobi.com 20160525 add for KidsMode begin

package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import com.android.internal.logging.MetricsLogger;

public class AsusKidsLauncherSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String KEY_KIDS_LAUNCHER_SWITCHS = "asus_kids_launcher_switch";

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private SwitchPreference mSwitch;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_kids_launcher_settings);
        PreferenceScreen root = getPreferenceScreen();
        mSwitch = (SwitchPreference) root.findPreference(KEY_KIDS_LAUNCHER_SWITCHS);
        mSwitch.setOnPreferenceClickListener(this);
    }

    public void onResume() {
        super.onResume();
        mSwitch.setChecked(false);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.asus.kidslauncher", "com.asus.kidslauncher.AsusKidsLauncherEntry");
        startActivity(intent);
        return true;
    }

    public static boolean hasKidsMode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.asus.kidslauncher", PackageManager.GET_META_DATA);
            if (!info.applicationInfo.enabled) {
                return false;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    protected int getMetricsCategory(){
        return MetricsLogger.MAIN_SETTINGS;
    }
}

// xuyi@wind-mobi.com 20160525 add for KidsMode end
