package com.android.settings.fingerprint;


import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.provider.Settings; //qiancheng add

public class AsusFingerprintSettings extends SettingsPreferenceFragment {

    private static final String TAG = "AsusFingerprintSettings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;


    private static final int DISABLE_MODE = 0;
    private static final int ENABLE_MODE = 1;

    private static final String SYSTEM_PROPERTY_FINGERPRINT_POSITION = "ro.hardware.fp_position";
    private static final String FINGERPRINT_POSITION = SystemProperties.get(SYSTEM_PROPERTY_FINGERPRINT_POSITION).toLowerCase();
    private static final String BACK_POSITION = "back";

    private static final String KEY_FINGERPRINT_SWITCH_CATEGORY = "asus_fingerprint_touch_control_category";
    private static final String KEY_FINGERPRINT_LONG_PRESS_SWITCH = "fingerprint_answer_call_long_press";
    private static final String KEY_FINGERPRINT_LAUNCH_CAMERA_SWITCH = "fingerprint_launch_camera";
    private static final String KEY_FINGERPRINT_TAKE_PHOTO_SWITCH = "fingerprint_take_photo";
    private static final String KEY_FINGERPRINT_MANAGEMENT = "asus_fingerprint_feature";

    private SwitchPreference mFingerprintLongPress = null;
    private SwitchPreference mLaunchCamera = null;
    private SwitchPreference mTakePhoto = null;
    private Preference mFingerprintManagement = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_fingerprint_operation_settings);

        PreferenceGroup switch_items = (PreferenceGroup)findPreference(KEY_FINGERPRINT_SWITCH_CATEGORY);
        mFingerprintLongPress = (SwitchPreference) findPreference(KEY_FINGERPRINT_LONG_PRESS_SWITCH);
        mLaunchCamera = (SwitchPreference) findPreference(KEY_FINGERPRINT_LAUNCH_CAMERA_SWITCH);
        mTakePhoto = (SwitchPreference) findPreference(KEY_FINGERPRINT_TAKE_PHOTO_SWITCH);
       if (!isBackFingerprintSensor() && !DEBUG_OPEN_ALL_FEATURE){
            switch_items.removePreference(mLaunchCamera);
            switch_items.removePreference(mTakePhoto);
        }
        mFingerprintManagement = findPreference(KEY_FINGERPRINT_MANAGEMENT);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mFingerprintLongPress) {
            boolean isEnable = !isFingerprintIDAnswerCallOpen();
            if(isFingerprintIDAnswerCallOpen()){
                writeLongPress(DISABLE_MODE);
                mFingerprintLongPress.setChecked(isEnable);
                mFingerprintLongPress.setEnabled(isEnable);
            } else {
                int value = readLongPress();
                mFingerprintLongPress.setEnabled(isEnable);
                mFingerprintLongPress.setChecked(value == ENABLE_MODE);
            }
        }
        if (null != mLaunchCamera) {
            mLaunchCamera.setChecked(readLaunchCameraMode() == ENABLE_MODE);
        }
        if (null != mTakePhoto) {
            mTakePhoto.setChecked(readTakePhotoMode() == ENABLE_MODE);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        //phone feature
        if (preference == mFingerprintLongPress) {
            if (mFingerprintLongPress.isChecked()) {
                writeLongPress(ENABLE_MODE);
            } else {
                writeLongPress(DISABLE_MODE);
            }
        }

        //camera feature
        if(preference == mLaunchCamera){
            if (mLaunchCamera.isChecked()) {
                writeLaunchCameraMode(ENABLE_MODE);
            } else {
                writeLaunchCameraMode(DISABLE_MODE);
            }
        }
        if(preference == mTakePhoto){
            if (mTakePhoto.isChecked()) {
                writeTakePhotoMode(ENABLE_MODE);
            } else {
                writeTakePhotoMode(DISABLE_MODE);
            }
        }

        if (preference == mFingerprintManagement) {
            Intent intent = new Intent();
            String clazz= FingerprintSettings.class.getName();
            intent.setClassName("com.android.settings", clazz);
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT;
    }


    private int readLongPress() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.ASUS_FINGERPRINT_LONG_PRESS, DISABLE_MODE);
    }

    private void writeLongPress(int value) {
        log("Write Long press value--" + Settings.Global.ASUS_FINGERPRINT_LONG_PRESS + ": " + value);
        Settings.Global.putInt(getContentResolver(), Settings.Global.ASUS_FINGERPRINT_LONG_PRESS, value);
    }

    private int readLaunchCameraMode() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.ASUS_FINGERPRINT_SELFIE_CAMERA, DISABLE_MODE);
    }

    private void writeLaunchCameraMode(int value) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.ASUS_FINGERPRINT_SELFIE_CAMERA, value);
    }

    private int readTakePhotoMode() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.ASUS_FINGERPRINT_TAKE_PHOTO, DISABLE_MODE);
    }

    private void writeTakePhotoMode(int value) {
        Settings.Global.putInt(getContentResolver(), Settings.Global.ASUS_FINGERPRINT_TAKE_PHOTO, value);
    }

    private boolean isBackFingerprintSensor() {
        if (!FINGERPRINT_POSITION.isEmpty()) {
            return FINGERPRINT_POSITION.equals(BACK_POSITION);
        } else {
            return false;
        }
    }

    private boolean isFingerprintIDAnswerCallOpen() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.ASUS_FINGERPRINT_ANSWER_CALL, 0) != 0;
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

}