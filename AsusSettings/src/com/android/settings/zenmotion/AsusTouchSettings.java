package com.android.settings.zenmotion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settings.zenmotion.AppLaunchListPreference.AppLaunchListSwitchChangedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsusTouchSettings extends SettingsPreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener,
        SwitchBar.OnSwitchChangeListener, AppLaunchListSwitchChangedListener, Indexable {

    private static final String TAG = "AsusTouchSettings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;
    private static final String KEY_WAKE_UP_CATEGORY = "asus_touch_gesture_wake_up_category";
    private static final String KEY_SLEEP_CATEGORY = "asus_touch_gesture_sleep_category";
    private static final String KEY_DOUBLE_TAP_ON = "double_tap_on";
    private static final String KEY_DOUBLE_TAP_OFF = "double_tap_off";
    private static final String KEY_SWIPE_UP = "swipe_up_to_wake_up";

    private static final String[] ALL_KEY_LAUNCH = {
            AppLaunchListPreference.KEY_W_LAUNCH,
            AppLaunchListPreference.KEY_S_LAUNCH,
            AppLaunchListPreference.KEY_E_LAUNCH,
            AppLaunchListPreference.KEY_C_LAUNCH,
            AppLaunchListPreference.KEY_Z_LAUNCH,
            AppLaunchListPreference.KEY_V_LAUNCH
    };

    public static final String PERSIST_ASUS_DLICK = "persist.asus.dclick";
    public static final String PERSIST_ASUS_GESTURE_TYPE = "persist.asus.gesture.type";
    public static final String DEFAULT_ASUS_GESTURE_TYPE = "1111111";
    public static final String DISABLE_ASUS_GESTURE_TYPE = "0000000";

    public static final String PERSIST_ASUS_SWIPE = "persist.asus.swipeup";
    private static final String SWIPE_UP_DATA = "swipe_up_on";
    private static final int DISABLE_SWIPE_UP_MODE = 0;
    private static final int ENABLE_SWIPE_UP_MODE = 1;

    private static final int OP_ALL = 1 << 6;
    private static final int OP_W = 1 << 5;
    private static final int OP_S = 1 << 4;
    private static final int OP_E = 1 << 3;
    private static final int OP_C = 1 << 2;
    private static final int OP_Z = 1 << 1;
    private static final int OP_V = 1 << 0;


    // +++ Double tap mode settings
    private static final int DISABLE_DOUBLE_TAP_MODE = 0;
    private static final int ENABLE_DOUBLE_TAP_MODE = 1;
    private static final String ZEN_MOTION_DATA = "zen_motion_data";
    private static final String DOUBLE_TAP_ON_DATA = "double_tap_on";
    private static final String DOUBLE_TAP_OFF_DATA = "double_tap_off";


    private OnSwitchChangedListener mCallBack;
    private SwitchPreference mDoubleTapOn = null;
    private SwitchPreference mDoubleTapOff = null;
    private SwitchPreference mSwipeUP = null;
    private final List<AppLaunchListPreference> mListPreferences = new ArrayList<>(6);

    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    private SharedPreferences mZenMotionSharesPreferences;

    public interface OnSwitchChangedListener {
        public void onTouchSwitchChanged(boolean isChecked);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCallBack = new AsusZenMotionSettings();
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_touch_operation_settings);

        final boolean isSupportDoubleTap = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP);

        final boolean isSupportGestureLunchApp = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP);

        final PreferenceCategory sleepCategory = (PreferenceCategory) findPreference(
                KEY_SLEEP_CATEGORY);
        mDoubleTapOn = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_ON);
        mDoubleTapOff = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_OFF);
        mSwipeUP = (SwitchPreference) findPreference(KEY_SWIPE_UP);

        if (!isSupportDoubleTap && !DEBUG_OPEN_ALL_FEATURE) {
            removePreference(KEY_WAKE_UP_CATEGORY);
            sleepCategory.removePreference(mDoubleTapOn);
        }

        if (isSupportGestureLunchApp || DEBUG_OPEN_ALL_FEATURE) {
            initAllListPreference();
        } else {
            if (isSupportDoubleTap) {
                // remove all the touch gesture
                sleepCategory.removeAll();
            } else {
                removePreference(KEY_SLEEP_CATEGORY);
            }
        }

        mZenMotionSharesPreferences = getActivity().getSharedPreferences(ZEN_MOTION_DATA,
                Context.MODE_PRIVATE);
    }

    private void handleMainSwitchStateChanged() {
        boolean enable = getSystemPropGestureIsChecked()
                || readDoubleTapOn()  == ENABLE_DOUBLE_TAP_MODE
                || readDoubleTapOff() == ENABLE_DOUBLE_TAP_MODE ;
        setMainSwitchChecked(enable);
        setIsEnableView(enable);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        handleMainSwitchStateChanged();
        if (null != mDoubleTapOn) {
            int value = mSwitch.isChecked() ? readDoubleTapOn() : loadLastDoubleTapOn();
            mDoubleTapOn.setChecked(value == ENABLE_DOUBLE_TAP_MODE);
            Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, (value == ENABLE_DOUBLE_TAP_MODE));
        }
        if (null != mDoubleTapOff) {
            int value = mSwitch.isChecked() ? readDoubleTapOff() : loadLastDoubleTapOff();
            mDoubleTapOff.setChecked(value == ENABLE_DOUBLE_TAP_MODE);
        }
        if (null != mSwipeUP) {
            int value = mSwitch.isChecked() ? readSwipeUp() : loadLastSwipeUp();
            mSwipeUP.setChecked(value == ENABLE_SWIPE_UP_MODE);
            Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, (value == ENABLE_SWIPE_UP_MODE));
        }
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDoubleTapOn) {
            if (mDoubleTapOn.isChecked()) {
                mDoubleTapOn.setChecked(false);
                createDoubleTapOnModeConfirmDialog().show();
            } else {
                writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                saveLastDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, false);
            }
        } else if (preference == mDoubleTapOff) {
            if (mDoubleTapOff.isChecked()) {
                mDoubleTapOff.setChecked(false);
                createDoubleTapOffModeConfirmDialog().show();
            } else {
                writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                saveLastDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
            }
        } else if (preference == mSwipeUP) {
            if (mSwipeUP.isChecked()) {
                mSwipeUP.setChecked(true);
               // createSwipeUpModeConfirmDialog().show();
                writeSwipeUp(ENABLE_SWIPE_UP_MODE);
                saveLastSwipeUp(ENABLE_SWIPE_UP_MODE);
                Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, true);
            } else {
                writeSwipeUp(DISABLE_SWIPE_UP_MODE);
                saveLastSwipeUp(DISABLE_SWIPE_UP_MODE);
                Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, false);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    }

    private void initAllListPreference() {
        // Get persist.asus.gesture.type and ensure it is not null and its length == 7
        String type = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
        int index = 0;
        if (type.length() != 7) {
            type = DEFAULT_ASUS_GESTURE_TYPE;
            writeSystemPropGesture(type);
        }
        mListPreferences.clear();
        for (String key : ALL_KEY_LAUNCH) {
            AppLaunchListPreference listPreference = (AppLaunchListPreference) findPreference(key);
            listPreference.setOnPreferenceChangeListener(this);
            listPreference.setAppLaunchListSwitchChangedListener(this);
            listPreference.setSummary(R.string.loading_notification_apps);
            listPreference.setEnabled(false);
            switch (listPreference.getKey()) {
                case AppLaunchListPreference.KEY_W_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(1)));
                    index = 1;
                    break;
                case AppLaunchListPreference.KEY_S_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(2)));
                    index = 2;
                    break;
                case AppLaunchListPreference.KEY_E_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(3)));
                    index = 3;
                    break;
                case AppLaunchListPreference.KEY_C_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(4)));
                    index = 4;
                    break;
                case AppLaunchListPreference.KEY_Z_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(5)));
                    index = 5;
                    break;
                case AppLaunchListPreference.KEY_V_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(6)));
                    index = 6;
                    break;
            }
            if (index > 0) {
                Utils.DriverGesturesItemSwitch(listPreference.getKey(), '1' == (type.charAt(index)));
            }
            listPreference.setPackageNames();
            mListPreferences.add(listPreference);
        }
    }

    private int readDoubleTapOn() {
        return SystemProperties.getInt(PERSIST_ASUS_DLICK, DISABLE_DOUBLE_TAP_MODE);
    }

    private int readSwipeUp() {
        return SystemProperties.getInt(PERSIST_ASUS_SWIPE, ENABLE_SWIPE_UP_MODE);
    }

    private int readDoubleTapOff() {
        return Settings.System.getInt(getContentResolver(),
               Settings.System.ASUS_DOUBLE_TAP, DISABLE_DOUBLE_TAP_MODE);
    }

    private void writeDoubleTapOn(int value) {
        boolean enabled;

        try {
            log("Write double tap on value--" + value);
            SystemProperties.set(PERSIST_ASUS_DLICK, Integer.toString(value));
            enabled = (value == 1 ? true : false);
            Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, enabled);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void writeSwipeUp(int value) {
        boolean enabled;

        try {
            SystemProperties.set(PERSIST_ASUS_SWIPE, Integer.toString(value));
            enabled = (value == 1 ? true : false);
            Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, enabled);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void writeDoubleTapOff(int value) {
        log("Write double tap off value--" + Settings.System.ASUS_DOUBLE_TAP + ": " + value);
        Settings.System.putInt(getContentResolver(), Settings.System.ASUS_DOUBLE_TAP, value);
    }

    private int loadLastDoubleTapOn() {
        return mZenMotionSharesPreferences.getInt(DOUBLE_TAP_ON_DATA, DISABLE_DOUBLE_TAP_MODE);
    }

    private int loadLastSwipeUp() {
        return mZenMotionSharesPreferences.getInt(SWIPE_UP_DATA, ENABLE_SWIPE_UP_MODE);
    }

    private int loadLastDoubleTapOff() {
        return mZenMotionSharesPreferences.getInt(DOUBLE_TAP_OFF_DATA, DISABLE_DOUBLE_TAP_MODE);
    }

    private void saveLastDoubleTapOn(int value) {
        mZenMotionSharesPreferences.edit().putInt(DOUBLE_TAP_ON_DATA, value).apply();
    }

    private void saveLastSwipeUp(int value) {
        mZenMotionSharesPreferences.edit().putInt(SWIPE_UP_DATA, value).apply();
    }

    private void saveLastDoubleTapOff(int value) {
        mZenMotionSharesPreferences.edit().putInt(DOUBLE_TAP_OFF_DATA, value).apply();
    }


    private String convertToFormatString(int value) {
        return String.format("%7s", Integer.toBinaryString(value)).replace(' ', '0');
    }

    private void writeAllDB(boolean checked) {
        int decimalValue = Integer.parseInt(SystemProperties.get(
                PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE), 2);

        if (checked) {
            decimalValue = decimalValue | OP_ALL;
            writeDoubleTapOn(loadLastDoubleTapOn());
            writeDoubleTapOff(loadLastDoubleTapOff());
            writeSwipeUp(loadLastSwipeUp());
        } else {
            decimalValue = decimalValue & ~OP_ALL;
            saveLastDoubleTapOn(readDoubleTapOn());
            saveLastDoubleTapOff(readDoubleTapOff());
            saveLastSwipeUp(readSwipeUp());
            writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
            writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
            writeSwipeUp(DISABLE_SWIPE_UP_MODE);
        }
        writeSystemPropGesture(convertToFormatString(decimalValue));

        Utils.DriverGesturesMainSwitch(checked);

    }

    private void writeAppGestureDB(Preference preference, Object newValue) {
        int operation = 0;
        for (AppLaunchListPreference listPreference : mListPreferences) {
            if (listPreference.isChecked()) {
                String key = listPreference.getKey();
                if (AppLaunchListPreference.KEY_W_LAUNCH.equals(key)) operation |= OP_W;
                else if (AppLaunchListPreference.KEY_S_LAUNCH.equals(key)) operation |= OP_S;
                else if (AppLaunchListPreference.KEY_E_LAUNCH.equals(key)) operation |= OP_E;
                else if (AppLaunchListPreference.KEY_C_LAUNCH.equals(key)) operation |= OP_C;
                else if (AppLaunchListPreference.KEY_Z_LAUNCH.equals(key)) operation |= OP_Z;
                else if (AppLaunchListPreference.KEY_V_LAUNCH.equals(key)) operation |= OP_V;
            }
        }
        writeSystemPropGesture((0 == operation)
                ? DISABLE_ASUS_GESTURE_TYPE
                : Integer.toBinaryString(OP_ALL | operation));
    }

    private void writeSystemPropGesture(String opString) {
        log("Write system property--" + PERSIST_ASUS_GESTURE_TYPE + " : " + opString);
        try {
            SystemProperties.set(PERSIST_ASUS_GESTURE_TYPE, opString);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AppLaunchListPreference && newValue != null) {
            AppLaunchListPreference listPreference = (AppLaunchListPreference) preference;
            String appSettingKey = listPreference.getSettingsSystemKey();
            String appSettingValue = newValue.toString();
            log("onPreferenceChange AppLaunchListPreference newValue: Change AppLaunch--"
                    + appSettingKey + ": " + appSettingValue);
            Settings.System.putString(getContentResolver(), appSettingKey, appSettingValue);
            listPreference.updateEntry(newValue.toString());
            if (!listPreference.isChecked()) {
                listPreference.setSwitchChecked(true);
            }
            writeAppGestureDB(preference, newValue);
        }
        return false;
    }

    private AlertDialog createDoubleTapOnModeConfirmDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        log("DoubleTapOnModeConfirmDialog PositiveButton onclick");
                        writeDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOn.setChecked(true);
                        Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        log("DoubleTapOnModeConfirmDialog NegativeButton onclick");
                        writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOn.setChecked(false);
                        Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, false);
                        break;
                }
            }
        };

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.double_tap_on_confirm_dialog_message)
                .setTitle(R.string.double_tap_on_confirm_dialog_title)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .create();
    }

    private AlertDialog createSwipeUpModeConfirmDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        writeSwipeUp(ENABLE_SWIPE_UP_MODE);
                        saveLastSwipeUp(ENABLE_SWIPE_UP_MODE);
                        mSwipeUP.setChecked(true);
                        Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        writeSwipeUp(DISABLE_SWIPE_UP_MODE);
                        saveLastDoubleTapOn(DISABLE_SWIPE_UP_MODE);
                        mSwipeUP.setChecked(false);
                        Utils.DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, false);
                        break;
                }
            }
        };

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.swipe_up_confirm_dialog_message)
                .setTitle(R.string.swipe_up_confirm_dialog_title)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .create();
    }


    private AlertDialog createDoubleTapOffModeConfirmDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        log("DoubleTapOffModeConfirmDialog PositiveButton onclick");
                        writeDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOff.setChecked(true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        log("DoubleTapOffModeConfirmDialog NegativeButton onclick");
                        writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOff.setChecked(false);
                        break;
                }
            }
        };

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.double_tap_off_confirm_dialog_message)
                .setTitle(R.string.double_tap_off_confirm_dialog_title)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .create();
    }

    private void setIsEnableView(boolean isEnable) {
        if (null != mDoubleTapOn) {
            mDoubleTapOn.setEnabled(isEnable);
        }
        if (null != mDoubleTapOff) {
            mDoubleTapOff.setEnabled(isEnable);
        }
        if (null != mSwipeUP) {
            mSwipeUP.setEnabled(isEnable);
        }
        if (null != mListPreferences) {
            for (AppLaunchListPreference listPreference : mListPreferences) {
                listPreference.setEnabled(isEnable);
            }
        }
    }

    private void setMainSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
        Utils.DriverGesturesMainSwitch(checked);
    }

    // for Main Switch
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (mStateMachineEvent) return;
        writeAllDB(isChecked);
        setIsEnableView(isChecked);
        mCallBack.onTouchSwitchChanged(isChecked);
    }

    @Override
    protected int getMetricsCategory() {
        return com.android.internal.logging.MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public void onAppLaunchListSwitchChanged(String key, boolean isChecked) {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,
                DEFAULT_ASUS_GESTURE_TYPE);
        StringBuilder opStringBuilder = new StringBuilder(opString);
        switch (key) {
            case AppLaunchListPreference.KEY_W_LAUNCH:
                opStringBuilder.setCharAt(1, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_S_LAUNCH:
                opStringBuilder.setCharAt(2, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_E_LAUNCH:
                opStringBuilder.setCharAt(3, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_C_LAUNCH:
                opStringBuilder.setCharAt(4, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_Z_LAUNCH:
                opStringBuilder.setCharAt(5, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_V_LAUNCH:
                opStringBuilder.setCharAt(6, isChecked ? '1' : '0');
                break;
            default:
                break;
        }

        if (key != null) {
            Utils.DriverGesturesItemSwitch(key, isChecked);
        }

        int decimalValue = Integer.parseInt(opStringBuilder.toString(), 2);
        if (OP_ALL == decimalValue) { // 1000000
            opString = DISABLE_ASUS_GESTURE_TYPE;
        } else {
            if (decimalValue < OP_ALL && decimalValue > 0) { // 0xxxxxx -> 1xxxxxx
                opStringBuilder.setCharAt(0, '1');
            }
            opString = opStringBuilder.toString();
        }
        writeSystemPropGesture(String.format("%7s", opString.replace(' ', '0')));
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER;
    static {
        SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.asus_touch_operation_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                PackageManager pm = context.getPackageManager();
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP)) {
                    keys.add(KEY_DOUBLE_TAP_ON);
                    keys.add(KEY_SWIPE_UP);
                    keys.add(KEY_DOUBLE_TAP_OFF);
                }
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP)) {
                    for (String key : ALL_KEY_LAUNCH) {
                        keys.add(key);
                    }
                }
                return keys;
            }
        };
    }

    protected static boolean getSystemPropGestureIsChecked() {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,DEFAULT_ASUS_GESTURE_TYPE);
        return (Integer.parseInt(opString, 2) & OP_ALL) != 0;
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
