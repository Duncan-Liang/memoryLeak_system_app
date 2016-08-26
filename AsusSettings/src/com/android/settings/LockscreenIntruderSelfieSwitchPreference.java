package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.preference.SwitchPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.service.trust.TrustAgentService;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.util.EASUtils;

import java.util.ArrayList;
import java.util.List;

import android.preference.Preference;

public class LockscreenIntruderSelfieSwitchPreference extends
        SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener {
    // +++jesson_yi@asus.com 2015.06.09
    /**
     * Lockscreen enable Intruder selfie Settings Switch ( To decide whether to shoot the intruder's
     * picture at the wrong password )
     * @hide
     */
    public static final String ASUS_LOCKSCREEN_INTRUDER_SELFIE = "asus_lockscreen_intruder_selfie";
    // +++jesson_yi@asus.com 2015.06.09
    /**
     * Lockscreen Save the number of incorrect password ( To decide the times that the wrong
     * password have appeared. Then to shoot the intruder's picture )
     * @hide
     */
    public static final String ASUS_LOCKSCREEN_INCORRECT_CODE_ENTRIES = "asus_lockscreen_incorrect_code_entries";
    // +++jesson_yi@asus.com 2015.06.09
    /**
     * Lockscreen Determining whether the intruder image is automatically saved.
     * @hide
     */
    public static final String ASUS_LOCKSCREEN_INTRUDER_SAVE_AUTO = "asus_lockscreen_intruder_save_auto";

    private ListPreference mIncorrect_entries;
    private SwitchPreference mIntruderselfie;
    private SwitchPreference mIntruderSaveAuto;
    private static final String KEY_LOCK_INCORRECT_CODE_ENTRIES = "Incorrect_code_entries";
    private static final String KEY_LOCK_INTRUDER_SELFIE = "Intruder_selfie";
    private static final String KEY_LOCK_SAVE_THE_PHOTO_AUTOMATICALLY = "Save_the_photo_automatically";
    private LockscreenIntruderselfieSwitch mIntruderselfieSwitch = null;
    private LockscreenIntruderSaveAutoSwitch mIntruderSaveAutoSwitch = null;
    private boolean mIsIntruderselfieEnabled;
    private boolean mIsIntruderSaveAutoEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private PreferenceScreen createPreferenceHierarchy() {

        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        // Add Android LockScreen Settings
        addPreferencesFromResource(R.xml.lockscreen_intruder_selfie);
        root = getPreferenceScreen();
        mIncorrect_entries = (ListPreference) root
                .findPreference(KEY_LOCK_INCORRECT_CODE_ENTRIES);
        if (mIncorrect_entries != null) {
            setupIncorrectEntriesPreference();
            updateIncorrectEntriesPreferenceSummary();
        }
        mIntruderselfieSwitch = (LockscreenIntruderselfieSwitch) root
                .findPreference(KEY_LOCK_INTRUDER_SELFIE);
        mIntruderSaveAutoSwitch = (LockscreenIntruderSaveAutoSwitch) root
                .findPreference(KEY_LOCK_SAVE_THE_PHOTO_AUTOMATICALLY);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        updatePreference();
    }

    @Override
    public void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    public void onDetach() {
        // TODO Auto-generated method stub
        super.onDetach();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        final ContentResolver res = getContentResolver();
        final String key = preference.getKey();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mIncorrect_entries) {
            int entries = Integer.parseInt((String) value);
            try {
                Settings.System.putInt(getContentResolver(),
                        ASUS_LOCKSCREEN_INCORRECT_CODE_ENTRIES,
                        entries);
            } catch (NumberFormatException e) {
                Log.e("TAG",
                        "could not persist intruder incorrect code entries setting",
                        e);
            }
            updateIncorrectEntriesPreferenceSummary();
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.switch_intruder_selfie) {
            if (buttonView instanceof Switch) {
                if (mIsIntruderselfieEnabled != isChecked) {
                    mIsIntruderselfieEnabled = isChecked;
                    mIntruderSaveAutoSwitch
                            .setEnabled(mIsIntruderselfieEnabled);
                    mIncorrect_entries.setEnabled(mIsIntruderselfieEnabled);
                    int status = (mIsIntruderselfieEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            ASUS_LOCKSCREEN_INTRUDER_SELFIE,
                            status);
                    mIntruderselfieSwitch
                            .setSwitchChecked(mIsIntruderselfieEnabled);
                }
            }
        } else if (id == R.id.switch_intruder_save_auto) {
            if (buttonView instanceof Switch) {
                if (mIsIntruderSaveAutoEnabled != isChecked) {
                    mIsIntruderSaveAutoEnabled = isChecked;
                    int status = (mIsIntruderSaveAutoEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            ASUS_LOCKSCREEN_INTRUDER_SAVE_AUTO,
                            status);
                    mIntruderSaveAutoSwitch
                            .setSwitchChecked(mIsIntruderSaveAutoEnabled);
                }
            }
        }
    }

    private void handleStateChanged() {
        mIsIntruderselfieEnabled = (Settings.System.getInt(
                getContentResolver(),
                ASUS_LOCKSCREEN_INTRUDER_SELFIE, 1) == 1);
        mIsIntruderSaveAutoEnabled = (Settings.System.getInt(
                getContentResolver(),
                ASUS_LOCKSCREEN_INTRUDER_SAVE_AUTO, 1) == 1);
    }

    private void updatePreference() {
        handleStateChanged();
        if (mIntruderselfieSwitch != null) {
            mIntruderselfieSwitch.setSwitchChecked(mIsIntruderselfieEnabled);
            mIntruderselfieSwitch.setOnSwitchCheckedChangeListener(this);
            mIntruderSaveAutoSwitch.setEnabled(mIsIntruderselfieEnabled);
            mIncorrect_entries.setEnabled(mIsIntruderselfieEnabled);
        }
        if (mIntruderSaveAutoSwitch != null) {
            mIntruderSaveAutoSwitch
                    .setSwitchChecked(mIsIntruderSaveAutoEnabled);
            mIntruderSaveAutoSwitch.setOnSwitchCheckedChangeListener(this);
        }
    }

    private void setupIncorrectEntriesPreference() {
        int currentIncorrectEntries = Settings.System.getInt(
                getContentResolver(),
                ASUS_LOCKSCREEN_INCORRECT_CODE_ENTRIES, 2);
        mIncorrect_entries.setValue(String.valueOf(currentIncorrectEntries));
        mIncorrect_entries.setOnPreferenceChangeListener(this);
    }

    private void updateIncorrectEntriesPreferenceSummary() {
        // Update summary message with current value
        int currentIncorrectEntries = Settings.System.getInt(
                getContentResolver(),
                ASUS_LOCKSCREEN_INCORRECT_CODE_ENTRIES, 2);
        final CharSequence[] entries = mIncorrect_entries.getEntries();
        final CharSequence[] values = mIncorrect_entries.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            int time = Integer.parseInt(values[i].toString());
            if (currentIncorrectEntries >= time) {
                best = i;
            }
        }
        mIncorrect_entries.setSummary(entries[best].toString());
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.LOCKSCREEN;
    }
}
