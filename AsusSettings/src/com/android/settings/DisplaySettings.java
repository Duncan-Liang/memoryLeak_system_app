/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DropDownPreference.Callback;

// xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
import com.android.settings.bluelightfilter.BluelightFilterPreference;
import com.android.settings.bluelightfilter.Constants;
import com.android.settings.bluelightfilter.SeekBarDialog;
import android.content.pm.ApplicationInfo;
// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.settings.DisplaySettingsExt;
import com.mediatek.settings.FeatureOption;

import java.util.ArrayList;
import java.util.List;

// xuyi@wind-mobi.com 20160530 add for LED being
import android.preference.CheckBoxPreference;
import java.io.File;
import android.provider.Settings.SettingNotFoundException;
// xuyi@wind-mobi.com 20160530 add for LED end

// xuyi@wind-mobi.com 20160530 add for auto rotate begin
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
// xuyi@wind-mobi.com 20160530 add for auto rotate end

// xuyi@wind-mobi.com 20160601 add for settings wallpaper begin
import com.android.settings.util.AMAXReflector;
import android.content.Intent;
// xuyi@wind-mobi.com 20160601 add for settings wallpaper end

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_MODE = "night_mode";
    // xuyi@wind-mobi.com 20160530 add  for LED begin
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    // xuyi@wind-mobi.com 20160530 add  for LED end
    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;
	
	// xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
    public static final String ASUS_SPLENDID_SCREEN_MODE_OPTION = "asus_splendid_screen_mode_option";
    public static final String ASUS_SPLENDID_SCREEN_MODE_CURRENT_RES_ID = "asus_splendid_screen_mode_current_res_id";
    public static final String ASUS_SPLENDID_PACKAGE_NAME = "com.asus.splendid";
    public static final int SCREEN_MODE_OPTION_BALANCE = 0;
    public static final int SCREEN_MODE_OPTION_READING = 1;
    public static final int SCREEN_MODE_OPTION_VIVID = 2;
    public static final int SCREEN_MODE_OPTION_CUSTOMIZED = 3;
	// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

    // xuyi@wind-mobi.com 20160601 add for settings wallpaper begin
    private static final String KEY_WALLPAPER_SETTINGS = "wallpaper";
    private PreferenceScreen mWallpaperPreference;
    // xuyi@wind-mobi.com 20160601 add for settings wallpaper end

    private WarnedListPreference mFontSizePref;
	
    // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
    private PreferenceScreen mScreenColorModeScreen;
    private ListPreference mPictureQualityMemcPreference;
    private int mScreenColorMode;
    private BluelightFilterPreference mBluelightFilterScreen;
    private int mBluelightFilterMode;
    private int mBluelightLevel;
    private mBluelightFilterModeObserver mBluelightFilterModeObserver;
    private BluelightLevelObserver mBluelightLevelObserver;
    private ScreenModeOptionObserver mScreenModeOptionObserver;
    private Resources mSplendidRes;
	// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;

    // xuyi@wind-mobi.com 20160530 add for LED begin
    private CheckBoxPreference mNotificationPulse;
    // xuyi@wind-mobi.com 20160530 add for LED end

    // xuyi@wind-mobi.com 20160530 add for auto rotate being
    private CheckBoxPreference mAutoRotatePreference;
    // xuyi@wind-mobi.com 20160530 add for auto rotate end

    ///M: MTK feature
    private DisplaySettingsExt mDisplaySettingsExt;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);
        ///M: MTK feature @{
        mDisplaySettingsExt = new DisplaySettingsExt(getActivity());
        mDisplaySettingsExt.onCreate(getPreferenceScreen());
        /// @}
        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        // xuyi@wind-mobi.com 20160530 modify for remove auto-brightness begin
        /*
        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }
        */
        removePreference(KEY_AUTO_BRIGHTNESS);
        // xuyi@wind-mobi.com 20160530 modify for remove auto-brightness end

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        // xuyi@wind-mobi.com 20160530 add for LED begin
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        try {
            mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
            mNotificationPulse.setOnPreferenceChangeListener(this);

            File file = new File("/sys/class/leds/red/brightness");
            if (!file.exists()) {
                removePreference(KEY_NOTIFICATION_PULSE);
            }
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            removePreference(KEY_NOTIFICATION_PULSE);
        }
        // xuyi@wind-mobi.com 20160530 add for LED end

        if (isDozeAvailable(activity)) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE);
        }

        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
        mBluelightFilterScreen = (BluelightFilterPreference) findPreference("bluelight_filter_mode");
        mScreenColorModeScreen = (PreferenceScreen) findPreference("screen_color_mode");
        if (!getPackageManager().hasSystemFeature("asus.hardware.display.splendid")) {
            getPreferenceScreen().removePreference(mBluelightFilterScreen);
            getPreferenceScreen().removePreference(mScreenColorModeScreen);
            mBluelightFilterScreen = null;
            mScreenColorModeScreen = null;
        } else {
            Boolean notShowBluelightFilter = checkIfSplendidExist(ASUS_SPLENDID_PACKAGE_NAME,
                    getActivity().getApplicationContext());
            if (notShowBluelightFilter) {
                getPreferenceScreen().removePreference(mBluelightFilterScreen);
                mBluelightFilterScreen = null;
                mSplendidRes = getSplendidRes();
                mScreenModeOptionObserver = new ScreenModeOptionObserver(new Handler());
            } else {
                getPreferenceScreen().removePreference(mScreenColorModeScreen);
                mScreenColorModeScreen = null;
                mBluelightFilterModeObserver = new mBluelightFilterModeObserver(new Handler());
                mBluelightLevelObserver = new BluelightLevelObserver(new Handler());
            }
        }
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter end
		
        if (isTapToWakeAvailable(getResources())) {
            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TAP_TO_WAKE);
        }

        // xuyi@wind-mobi.com 20160530 modify for auto rotate begin
        /*
        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            DropDownPreference rotatePreference =
                    (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            rotatePreference.addItem(activity.getString(R.string.display_auto_rotate_rotate),
                    false);
            int rotateLockedResourceId;
            // The following block sets the string used when rotation is locked.
            // If the device locks specifically to portrait or landscape (rather than current
            // rotation), then we use a different string to include this information.
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else {
                if (RotationPolicy.getRotationLockOrientation(activity)
                        == Configuration.ORIENTATION_PORTRAIT) {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_portrait;
                } else {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_landscape;
                }
            }
            rotatePreference.addItem(activity.getString(rotateLockedResourceId), true);
            /// M: ALPS01751214 Rotate function on statusbar cannot matched in display settings
            mDisplaySettingsExt.setRotatePreference(rotatePreference);
            rotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(activity) ?
                    1 : 0);
            rotatePreference.setCallback(new Callback() {
                @Override
                public boolean onItemSelected(int pos, Object value) {
                    final boolean locked = (Boolean) value;
                    MetricsLogger.action(getActivity(), MetricsLogger.ACTION_ROTATION_LOCK,
                            locked);
                    RotationPolicy.setRotationLock(activity, locked);
                    return true;
                }
            });
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }
        */
        if (RotationPolicy.isRotationSupported(getActivity())) {
            mAutoRotatePreference = (CheckBoxPreference) findPreference(KEY_AUTO_ROTATE);
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }
        // xuyi@wind-mobi.com 20160530 modify for auto rotate end

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }

        // xuyi@wind-mobi.com 20160601 add for settings wallpaper begin
        mWallpaperPreference = (PreferenceScreen) findPreference(KEY_WALLPAPER_SETTINGS);
        if (isExtandWallpaperSettings(activity.getApplicationContext())) {
            mWallpaperPreference.setFragment("com.android.settings.DisplayWallpaperSettings");
        } else {
            mWallpaperPreference.setIntent(new Intent(Intent.ACTION_SET_WALLPAPER));
        }
        // xuyi@wind-mobi.com 20160601 add for settings wallpaper end
    }

    // xuyi@wind-mobi.com 20160601 add for settings wallpaper begin
    private static boolean isExtandWallpaperSettings(Context context) {
        return AMAXReflector.isSupportIntent("ACTION_SET_WALLPAPER_LOCKSCREEN", false)
                && !Utils.disableLockWallpaperOptionsOnLauncher(context);
    }
    // xuyi@wind-mobi.com 20160601 add for settings wallpaper end

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
		//Add by yinlili@wind-mobi.com 20160721 -s for bug#122780
		long mCurrentTimeout = 0L;
		//Add by yinlili@wind-mobi.com 20160721 -e for bug#122780
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            }
            // xuyi@wind-mobi.com 20160530 add for never timeout begin
            else if (currentTimeout == 0L) {
                summary = getString(R.string.never_timeout_summary);
            }
            // xuyi@wind-mobi.com 20160530 add for never timeout end
            else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    // xuyi@wind-mobi.com 20160530 modify for never timeout begin
                    if (currentTimeout >= timeout && timeout != 0L) {
                    // xuyi@wind-mobi.com 20160530 modify for never timeout end
                        best = i;
						//Add by yinlili@wind-mobi.com 20160721 -s for bug#122780
						mCurrentTimeout = timeout;
						//Add by yinlili@wind-mobi.com 20160721 -e for bug#122780
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
				//Add by yinlili@wind-mobi.com 20160721 -s for bug#122780
				mScreenTimeoutPreference.setValue(String.valueOf(mCurrentTimeout));
				//Add by yinlili@wind-mobi.com 20160721 -e for bug#122780
            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
		
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
        if (mScreenColorModeScreen != null && mScreenModeOptionObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(ASUS_SPLENDID_SCREEN_MODE_OPTION), false,
                    mScreenModeOptionObserver);
        }
        
        if (mBluelightFilterScreen != null && mBluelightFilterModeObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), false,
                    mBluelightFilterModeObserver);
        }
        if (mBluelightFilterScreen != null && mBluelightLevelObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.BLUELIGHT_FILTER_LEVEL), false,
                    mBluelightLevelObserver);
        }
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

        // xuyi@wind-mobi.com 20160530 add for auto rotate begin
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        // xuyi@wind-mobi.com 20160530 add for auto rotate end

        // xuyi@wind-mobi.com 20160530 add for never timeout begin
        final long currentTimeout = Settings.System.getLong(getContentResolver(), SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        updateTimeoutPreferenceDescription(currentTimeout);
        // xuyi@wind-mobi.com 20160530 add for never timeout end

        mDisplaySettingsExt.onResume();
    }

    /// M: add MTK extended @{
    @Override
    public void onPause() {
        super.onPause();
		
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
        if (mScreenColorModeScreen != null && mScreenModeOptionObserver != null) {
            getContentResolver().unregisterContentObserver(mScreenModeOptionObserver);
        }
        if (mBluelightFilterScreen != null && mBluelightFilterModeObserver != null) {
            getContentResolver().unregisterContentObserver(mBluelightFilterModeObserver);
        }
        if (mBluelightFilterScreen != null && mBluelightLevelObserver != null) {
            getContentResolver().unregisterContentObserver(mBluelightLevelObserver);
        }
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

        // xuyi@wind-mobi.com 20160530 add for auto rotate begin
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        // xuyi@wind-mobi.com 20160530 add for auto rotate end

        mDisplaySettingsExt.onPause();
    }
    /// @}

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
		
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
        if (mScreenColorModeScreen != null) {
            mScreenColorMode = Settings.System.getInt(getContentResolver(),
                    ASUS_SPLENDID_SCREEN_MODE_OPTION, SCREEN_MODE_OPTION_BALANCE); //default balance mode
            updateScreenColorModeTitle();
        }
        
        if (mBluelightFilterScreen != null) { //++Carol_Wang
            mBluelightFilterMode = Settings.System.getInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
            mBluelightLevel = (mBluelightFilterMode == 1) ? Settings.System.getInt(getContentResolver(),
                    Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF) : -1; //default off
            updateBluelightFilterModeTitle();
        }
        // xuyi@wind-mobi.com 20160525 add for BlueLightFilter end
		
        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }

        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }

        // xuyi@wind-mobi.com 20160530 add for auto rotate begig
        if (mAutoRotatePreference != null) {
            updateLockScreenRotationCheckbox();
        }
        // xuyi@wind-mobi.com 20160530 add for auto rotate end
    }

    // xuyi@wind-mobi.com 20160530 add for LED begin
    private void updateLEDIndicatorSummary() {
        if (mNotificationPulse != null) {
            mNotificationPulse.setSummary(mNotificationPulse.isChecked() ?
                    mNotificationPulse.getContext().getString(R.string.accessibility_feature_state_on) :
                    mNotificationPulse.getContext().getString(R.string.accessibility_feature_state_off));
        }
    }
    // xuyi@wind-mobi.com 20160530 add for LED end

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
    private void updateBluelightFilterModeTitle() { // ++Carol_Wang
        switch (mBluelightLevel) {
            case Constants.BLUELIGHT_FILTER_MODE_OFF:
                mBluelightFilterScreen
                        .setSummary(getString(R.string.splendid_bluelight_filter_mode_off));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK:
                mBluelightFilterScreen.setSummary(
                        getString(R.string.splendid_bluelight_filter_mode_level) + " 1");
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD01:
                mBluelightFilterScreen.setSummary(
                        getString(R.string.splendid_bluelight_filter_mode_level) + " 2");
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD02:
                mBluelightFilterScreen.setSummary(
                        getString(R.string.splendid_bluelight_filter_mode_level) + " 3");
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD03:
                mBluelightFilterScreen.setSummary(
                        getString(R.string.splendid_bluelight_filter_mode_level) + " 4");
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RDSTRONG:
                mBluelightFilterScreen.setSummary(
                        getString(R.string.splendid_bluelight_filter_mode_level) + " 5");
                break;
            default:
                mBluelightFilterScreen
                        .setSummary(getString(R.string.splendid_bluelight_filter_mode_off));
                break;
        }
    }

    private void updateScreenColorModeTitle() {
        int modeSummary =-1;
        int resIdOfSplendid = 0;
        switch (mScreenColorMode) {
        case SCREEN_MODE_OPTION_BALANCE:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("balance_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_balance_mode_text;
            break;
        case SCREEN_MODE_OPTION_READING:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("reading_mode_text_L", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_reading_mode_text;
            break;
        case SCREEN_MODE_OPTION_VIVID:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("vivid_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_vivid_mode_text;
            break;
        case SCREEN_MODE_OPTION_CUSTOMIZED:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("customized_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_customized_mode_text;
            break;
        default:
            if(mSplendidRes != null) {
                try {
                    resIdOfSplendid = Settings.System.getInt(getContentResolver(),ASUS_SPLENDID_SCREEN_MODE_CURRENT_RES_ID , -1);
                    if (resIdOfSplendid > 0)
                        mSplendidRes.getString(resIdOfSplendid);
                } catch (NotFoundException e) {
                    resIdOfSplendid = -1;
                    e.printStackTrace();
                }
            }
        }
        if(mSplendidRes != null && resIdOfSplendid >0)
            mScreenColorModeScreen.setSummary(mSplendidRes.getString(resIdOfSplendid));
        else if(modeSummary == -1 || resIdOfSplendid == -1)
            mScreenColorModeScreen.setSummary(R.string.splendid_balance_mode_text);
        else
            mScreenColorModeScreen.setSummary(modeSummary);
    }

    private Resources getSplendidRes() {
        PackageManager manager = getPackageManager();
        Resources splendidResources = null;
        try {
            splendidResources = manager.getResourcesForApplication(ASUS_SPLENDID_PACKAGE_NAME);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to load splendid resource:"+e.getMessage());
        }
        return splendidResources;
    }
	// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end
	
    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ///M: add MTK feature @{
        mDisplaySettingsExt.onPreferenceClick(preference);
        /// @}
		
		// xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
        if (preference == mBluelightFilterScreen) {
            int option = Settings.System.getInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
            if (option == 1) {
                SeekBarDialog sbd = new SeekBarDialog(getActivity().getApplicationContext(), getActivity());
                sbd.show();
            }
        }
		// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

        // xuyi@wind-mobi.com 20160530 add for LED begin
        else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
            updateLEDIndicatorSummary();
            return true;
        }
        // xuyi@wind-mobi.com 20160530 add for LED end

        // xuyi@wind-mobi.com 20160530 add for auto rotate begin
        else if (preference == mAutoRotatePreference) {
            handleLockScreenRotationPreferenceClick();
        }
        // xuyi@wind-mobi.com 20160530 add for auto rotate end

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
    private class ScreenModeOptionObserver extends ContentObserver {
        public ScreenModeOptionObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("ScreenModeOption", "ScreenModeOptionObserver onChange");
            int currentScreenMode = Settings.System.getInt(getContentResolver(), ASUS_SPLENDID_SCREEN_MODE_OPTION, SCREEN_MODE_OPTION_BALANCE);
            if (mScreenColorMode != currentScreenMode) {
                mScreenColorMode = currentScreenMode;
                updateScreenColorModeTitle();
            }
        }
    }
	// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)
                            /// M: exclude Daydream when device is low ram device
                            || FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    if (!isTapToWakeAvailable(context.getResources())) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    // xuyi@wind-mobi.com 20160530 add for LED begin
                    if (!isLEDAvailable()) {
                        result.add(KEY_NOTIFICATION_PULSE);
                    }
                    // xuyi@wind-mobi.com 20160530 add for LED end
                    return result;
                }
            };
			
    // xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin
    public boolean checkIfSplendidExist(String appName, Context context) { //++Carol_Wang;
        if (appName == null || "".equals(appName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(appName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private class mBluelightFilterModeObserver extends ContentObserver {
        public mBluelightFilterModeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("BluelightFilterMode", "BluelightFilterModeObserver onChange");
            int currentScreenMode = Settings.System.getInt(getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
            //liuyang@wind-mobi.com 20160805 modified for bug#125281 begin
            //int currentBluelightLevel = Settings.System.getInt(getContentResolver(), Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF);
            int currentBluelightLevel = Settings.System.getInt(getContentResolver(), Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK);
            //liuyang@wind-mobi.com 20160805 modified for bug#125281 end
            if (mBluelightFilterMode != currentScreenMode) {
                mBluelightFilterMode = currentScreenMode;
                mBluelightLevel = (mBluelightFilterMode == 1) ? currentBluelightLevel : -1; //default off
                updateBluelightFilterModeTitle();
            }
        }
    }

    private class BluelightLevelObserver extends ContentObserver {
        public BluelightLevelObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("BluelightLevel", "BluelightLevel onChange");
            int currentScreenMode = Settings.System.getInt(getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
            int currentBluelightLevel = Settings.System.getInt(getContentResolver(), Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF);
            if (currentScreenMode == 1 && mBluelightLevel != currentBluelightLevel) {
                mBluelightFilterMode = currentScreenMode;
                mBluelightLevel = currentBluelightLevel;
                updateBluelightFilterModeTitle();
            }
        }
    }
    // xuyi@wind-mobi.com 20160525 add for BlueLightFilter end

    // xuyi@wind-mobi.com 20160530 add for LED begin
    private static boolean isLEDAvailable() {
        boolean ret = true;
        File file = new File("/sys/class/leds/red/brightness");
        if (!file.exists()) {
            ret = false;
        }
        Log.d(TAG, "isLEDAvailable ret: " + ret);
        return ret;
    }
    // xuyi@wind-mobi.com 20160530 add for LED end

    // xuyi@wind-mobi.com 20160530 add for auto rotate being
    private void handleLockScreenRotationPreferenceClick() {
        //RotationPolicy.setRotationLockForAccessibility(
        //Don't rotate screen when disable Auto-rotate screen
        RotationPolicy.setRotationLock(getActivity(),
                !mAutoRotatePreference.isChecked());
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mAutoRotatePreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }

    private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            updateLockScreenRotationCheckbox();
        }
    };
    // xuyi@wind-mobi.com 20160530 add for auto rotate end

}