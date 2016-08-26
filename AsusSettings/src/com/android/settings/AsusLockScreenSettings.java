/*
 * Copyright (C) 2011 The Android Open Source Project
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


import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
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

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils.TrustAgentComponentInfo;
import com.android.settings.util.EASUtils;
import com.android.internal.logging.MetricsLogger;

import java.util.ArrayList;
import java.util.List;

import android.preference.Preference;

//xuyi@wind-mobi.com 20160518 add for CM_weather begin
import android.os.SystemProperties;
//xuyi@wind-mobi.com 20160518 add for CM_weather end

// sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patches. 2016/7/21 begin
import com.android.settings.lockscreen.AsusLSDBHelper;
import com.android.settings.lockscreen.AsusLSUtils;
// sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patches. 2016/7/21 end
/**
 * Asus lockScreen settings.
 */
public class AsusLockScreenSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener{

    static final String TAG = "AsusLockScreenSettings";
    private static final Intent TRUST_AGENT_INTENT =
            new Intent(TrustAgentService.SERVICE_INTERFACE);

    //xuyi@wind-mobi.com 20160518 add for CM_weather begin
    private static final boolean WIND_DEF_CM_WEATHER = SystemProperties.get("ro.wind.def.cm.weather").equals("1");
    //xuyi@wind-mobi.com 20160518 add for CM_weather end
    //add by sunxiaolong@wind-mobi.com for asus request start
    private static final boolean WIND_DEF_ASUS_E280L_APK = SystemProperties.get("ro.wind.def.adapt_asus_apk_cn").equals("1");
    private static final int MY_USER_ID = UserHandle.myUserId();
    //add by sunxiaolong@wind-mobi.com for asus request end

    // Android Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING =
        "biometric_weak_improve_matching";
    private static final String KEY_BIOMETRIC_WEAK_LIVELINESS = "biometric_weak_liveliness";
    private static final String KEY_LOCK_ENABLED = "lockenabled";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_LOCK_AFTER_TIMEOUT = "lock_after_timeout";
    private static final String KEY_OWNER_INFO_SETTINGS = "owner_info_settings";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST = 124;
    private static final int CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF = 125;

    // Misc Settings
    private static final String KEY_POWER_INSTANTLY_LOCKS = "power_button_instantly_locks";

    private LockPatternUtils mLockPatternUtils;
    private ListPreference mLockAfter;
    private CheckBoxPreference mBiometricWeakLiveliness;
    private SwitchPreference mVisiblePattern;
    private SwitchPreference mPowerButtonInstantlyLocks;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private CheckBoxPreference mEnableKeyguardWidgets;

    // +++
    static final int SET_WALLPAPER_REQUEST = 129;

    private static final String KEY_DISPLAY = "lockscreen_setting_display";
    private static final String KEY_SHORTCUT = "lockscreen_shortcuts_display";
    private static final String KEY_WALLPAPER = "lock_screen_wallpaper_display";
    private static final String KEY_SHORTCUT_SWITCH = "lockscreen_shortcuts_display";
    private static final String KEY_STATUSBAR_SWITCH = "lockscreen_statusbar_display";
    private static final String KEY_WHATS_NEXT_SWITCH = "lockscreen_whats_next_widget";
    private static final String KEY_INSTANT_CAMERA_SWITCH = "lockscreen_instant_camera_widget";
    private static final String KEY_ENABLE_CAMERA_SWITCH = "lockscreen_enable_camera_widget";
    private static final String ASUS_LOCKSCREEN_WALLPAPER_FRAGMENT = "com.android.settings.AsusLockscreenWallpaper";
    private static final String ASUS_LOCKSCREEN_MULTIUSER_FRAGMENT = "com.android.settings.users.UserSettings";
    private static final String KEY_SKIP_SLIDE_SWITCH = "lockscreen_skip_slide";

    private static final String KEY_INTRUDER_SELFIE_SWITCH = "lockscreen_intruder_selfie";
	
	//xuyi@wind-mobi.com 20160518 add for CM_weather begin
	private static final String KEY_DISABLE_CM_WEATHER_SWITCH = "lockscreen_disable_weather_info_page";
	private LockscreenDisableWeatherInfoPageSwitchPreference mDisableWeatherInfoPageSwitchPref = null;
	private boolean mIsCMWeatherEnable;
	//xuyi@wind-mobi.com 20160518 add for CM_weather end

    // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 begin
    // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
    private static final String KEY_LOCKSCREEN_WALLPAPER_SWITCH = "lockscreen_wallpaper_settings";
    private static final String KEY_LOCKSCREEN_THEME_SWITCH = "lockscreen_theme_settings";
    private static final String KEY_WEATHER_ANIMATION_SWITCH = "lockscreen_enable_weather_animation";
    private static final String KEY_LCOKSCREEN_CLOCK_WIDGET_SWITCH = "lockscreen_show_clock_widget";

    private LockscreenWeatherAnimationSwitchPreference mWeatherAnimationSwitchPref = null;
    private LockscreenClockWidgetSwitchPreference mLSClockWidgetSwitchPref = null;

    private boolean mIsWeatherAnimationEnabled;
    private boolean mShowClockWidget;
    // END:Evelyn_Chang@asus.com for ZenFone3.
    // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 end

    private LockscreenShortcutSwitchPreference mShortcutSwitchPref = null;
    private LockscreenStatusBarSwitchPreference mStatusBarSwitchPreference = null;
    private LockscreenWhatsNextSwitchPreference mWhatsNextSwitchPref = null;
    private LockscreenInstantCameraSwitchPreference mInstantCameraSwitchPref = null;
    private LockscreenEnableCameraSwitchPreference mEnableCameraSwitchPref = null;
    private LockscreenSkipSlideSwitchPreference mSkipSlideSwitchPref = null;
    private boolean mIsShortcutEnabled;
    private boolean mIsStatusbarEnabled;
    private boolean mIsWhatsNextEnabled;
    private boolean mIsInstantCameraEnabled;
    private boolean mIsWhatsNextEnabledInSecurityMode;
    private boolean mIsCameraSettingEnabled;
    private boolean mIsSkipSlide;
    private static final boolean ONLY_ONE_TRUST_AGENT = true;
    // ---

    // +++ vivian_sun 2014.10.16 skip slid
    //private static final String KEY_ENABLE_SLIDE = "lockscreen_skip_slide";
    //private CheckBoxPreference mDisableSlidePref = null;
    // --- vivian_sun
    private boolean mIsPrimary;

    DevicePolicyManager mDPM;

    private Preference mIntruderSelfie;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        Intent intent = getActivity().getIntent();
        boolean intruder = intent.getBooleanExtra("intruder", false);
        if(intruder){
            startIntruder();
            intruder = false;
            getActivity(). getIntent().removeExtra("intruder");
        }

        try {
            mIsStatusbarEnabled = (Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.ASUS_LOCKSCREEN_DISPLAY_STATUS_BAR, 1, UserHandle.USER_CURRENT) == 1);
        } catch (Exception err) {
            Log.d(TAG, " query Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_STATUS_BAR err");
            mIsStatusbarEnabled = true;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {

        boolean isSlideMode = false;

        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        // Add Android LockScreen Settings
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // +++ Android LockScreen Settings
        // Add options for lock/unlock screen
        int resid = 0;
        if (!mLockPatternUtils.isSecure(UserHandle.myUserId())) {
            // if there are multiple users, disable "None" setting
            UserManager mUm = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = mUm.getUsers(true);
            final boolean singleUser = users.size() == 1;

            if (singleUser && mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId())) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                resid = R.xml.security_settings_chooser;
                isSlideMode = true;
            }
        }
        //TODO
        /*else if (mLockPatternUtils.usingBiometricWeak() &&
                mLockPatternUtils.isBiometricWeakInstalled()) {
            resid = R.xml.security_settings_biometric_weak;
        }*/ else {
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        addPreferencesFromResource(resid);

        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;

        /* Android 4.2:
        if (!mIsPrimary) {
            // Rename owner info settings
            Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (ownerInfoPref != null) {
                ownerInfoPref.setTitle(R.string.user_info_settings_title);
            }
        }*/
        // Android 4.3:
        if (!mIsPrimary) {
            // Rename owner info settings
            Preference ownerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
            if (ownerInfoPref != null) {
                if (UserManager.get(getActivity()).isLinkedUser()) {
                    ownerInfoPref.setTitle(R.string.profile_info_settings_title);
                } else {
                    ownerInfoPref.setTitle(R.string.user_info_settings_title);
                }
            }
        }

        // lock after preference
        mLockAfter = (ListPreference) root.findPreference(KEY_LOCK_AFTER_TIMEOUT);
        if (mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }

        // biometric weak liveliness
        mBiometricWeakLiveliness =
                (CheckBoxPreference) root.findPreference(KEY_BIOMETRIC_WEAK_LIVELINESS);

        // visible pattern
        mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        // lock instantly on power key press
        mPowerButtonInstantlyLocks = (SwitchPreference) root.findPreference(
                KEY_POWER_INSTANTLY_LOCKS);

        // don't display visible pattern if biometric and backup is not pattern
        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        //TODO
        if (/*resid == R.xml.security_settings_biometric_weak &&*/
                mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()) !=
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            if (securityCategory != null && mVisiblePattern != null) {
                securityCategory.removePreference(root.findPreference(KEY_VISIBLE_PATTERN));
            }
        }
        //add by sunxiaolong@wind-mobi.com for adding smart lock start
        if (securityCategory != null) {
            addTrustAgentSettings(securityCategory);
        }
        //add by sunxiaolong@wind-mobi.com for adding smart lock end
        // --- Android LockScreen Settings

        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        //TODO
        boolean mPrivateMode = Utils.isPrivateMode(um);
        int mPrivateUserId = Utils.getPrivateUserId(um);
        if(mPrivateMode && ActivityManager.getCurrentUser()== mPrivateUserId){
            return root;
        }
        // +++
        // Trust Agent preferences

//        //+++Jason_Uang, for AT&T EAS disable SmartLock issue
//        boolean hasEASAccountAndIsATTsku = EASUtils.hasEASAccountAndIsATTsku(getActivity());
//        //---Jason_Uang, for AT&T EAS disable SmartLock issue

//        if (securityCategory != null) {
//            //+++Jason_Uang, for AT&T EAS disable SmartLock issue
//            if (!hasEASAccountAndIsATTsku) {
//            //---Jason_Uang, for AT&T EAS disable SmartLock issue
//                final boolean hasSecurity = mLockPatternUtils.isSecure(UserHandle.myUserId());
//                ArrayList<TrustAgentComponentInfo> agents =
//                        getActiveTrustAgents(getPackageManager(), mLockPatternUtils);
//                for (int i = 0; i < agents.size(); i++) {
//                    final TrustAgentComponentInfo agent = agents.get(i);
//                    Preference trustAgentPreference =
//                            new Preference(securityCategory.getContext());
//                    trustAgentPreference.setKey(KEY_TRUST_AGENT);
//                    trustAgentPreference.setTitle(agent.title);
//                    trustAgentPreference.setSummary(agent.summary);
//                    // Create intent for this preference.
//                    Intent intent = new Intent();
//                    intent.setComponent(agent.componentName);
//                    intent.setAction(Intent.ACTION_MAIN);
//                    trustAgentPreference.setIntent(intent);
//                    // Add preference to the settings menu.
//                    root.addPreference(trustAgentPreference);
//                    if (!hasSecurity) {
//                        trustAgentPreference.setEnabled(false);
//                        trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
//                    }
//                }
//            }
//        }
        Preference trustAgentPreference = root.findPreference(KEY_TRUST_AGENT);
        if (mPowerButtonInstantlyLocks != null &&
                trustAgentPreference != null &&
                trustAgentPreference.getTitle().length() > 0) {
            mPowerButtonInstantlyLocks.setSummary(getString(
                    R.string.lockpattern_settings_power_button_instantly_locks_summary,
                    trustAgentPreference.getTitle()));
        }
        addPreferencesFromResource(R.xml.asus_lockscreen_display_settings);

        // Enable or disable keyguard widget checkbox based on DPM state
        mEnableKeyguardWidgets = (CheckBoxPreference) root.findPreference(KEY_ENABLE_WIDGETS);
        if (mEnableKeyguardWidgets != null) {
            if (ActivityManager.isLowRamDeviceStatic()) {
                // Widgets take a lot of RAM, so disable them on low-memory devices
                if (securityCategory != null) {
                    securityCategory.removePreference(root.findPreference(KEY_ENABLE_WIDGETS));
                    mEnableKeyguardWidgets = null;
                }
            } else {
                final boolean disabled = (0 != (mDPM.getKeyguardDisabledFeatures(null)
                        & DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL));
                mEnableKeyguardWidgets.setEnabled(!disabled);
            }
        }
        mShortcutSwitchPref = (LockscreenShortcutSwitchPreference) root.findPreference(KEY_SHORTCUT_SWITCH);
        mStatusBarSwitchPreference = (LockscreenStatusBarSwitchPreference) root.findPreference(KEY_STATUSBAR_SWITCH);
        mWhatsNextSwitchPref = (LockscreenWhatsNextSwitchPreference) root.findPreference(KEY_WHATS_NEXT_SWITCH);
        root.removePreference(mWhatsNextSwitchPref);
        mWhatsNextSwitchPref.setEnabled(isWhatsNextServiceEnabled());
        mInstantCameraSwitchPref = (LockscreenInstantCameraSwitchPreference) root.findPreference(KEY_INSTANT_CAMERA_SWITCH);
        // sunhuihui@wind-mobi.com Bug#106424 16-4-8 begin
        if (!SystemProperties.get("asus.hardware.instant_camera").equals("1")) {
            ((PreferenceCategory) findPreference("lockscreen_setting_camera")).removePreference(mInstantCameraSwitchPref);
        }
        // sunhuihui@wind-mobi.com Bug#106424 16-4-8 end
        mEnableCameraSwitchPref = (LockscreenEnableCameraSwitchPreference) root.findPreference(KEY_ENABLE_CAMERA_SWITCH);
        ((PreferenceCategory)findPreference("lockscreen_setting_camera")).removePreference(mEnableCameraSwitchPref);
        // ---
        // sunhuihui@wind-mobi.com Bug#105012 16-4-14 begin
        //modify by sunxiaolong@wind-mobi.com for asus request start
        if (WIND_DEF_ASUS_E280L_APK) {
        //modify by sunxiaolong@wind-mobi.com for asus request end
            root.removePreference(findPreference(KEY_OWNER_INFO_SETTINGS));
            root.removePreference(mShortcutSwitchPref);
            root.removePreference(findPreference("lockscreen_setting_camera"));
            root.removePreference(findPreference("lockscreen_setting_display"));
        }
        // sunhuihui@wind-mobi.com Bug#105012 16-4-14 end
        mSkipSlideSwitchPref = (LockscreenSkipSlideSwitchPreference) root.findPreference(KEY_SKIP_SLIDE_SWITCH);
        //xuyi@wind-mobi.com 20160518 add for CM_weather begin
        mDisableWeatherInfoPageSwitchPref = (LockscreenDisableWeatherInfoPageSwitchPreference) root.findPreference(KEY_DISABLE_CM_WEATHER_SWITCH);
        //xuyi@wind-mobi.com 20160518 add for CM_weather end
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mIntruderSelfie = root.findPreference(KEY_INTRUDER_SELFIE_SWITCH);
        mIntruderSelfie.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        activity.startPreferencePanel(LockscreenIntruderSelfieSwitchPreference.class.getName(), null,
                                R.string.lockscreen_intruder_selfie_title, null, AsusLockScreenSettings.this,
                                0);
                        return true;
                    }
                });
        if(!mLockPatternUtils.isSecure(UserHandle.myUserId())){
            root.removePreference(mSkipSlideSwitchPref);
            root.removePreference(mStatusBarSwitchPreference);
            root.removePreference(mIntruderSelfie);
        }
        // +++ vivian_sun 2014.10.16 skip slid
        /*mDisableSlidePref = (CheckBoxPreference) root.findPreference(KEY_ENABLE_SLIDE);
        if(!mLockPatternUtils.isSecure()){
            root.removePreference(mDisableSlidePref);
        }*/
        // --- vivian_sun

        //xuyi@wind-mobi.com 20160518 add for CM_weather begin
        if (WIND_DEF_CM_WEATHER) {
            if (!isCmWeatherEnable()) {
                root.removePreference(mDisableWeatherInfoPageSwitchPref);
            }
        } else {
            root.removePreference(mDisableWeatherInfoPageSwitchPref);
        }
        //xuyi@wind-mobi.com 20160518 add for CM_weather end

        root.removePreference(mIntruderSelfie);
        // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 begin
        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        Preference lsWallpaperPref = root.findPreference(KEY_LOCKSCREEN_WALLPAPER_SWITCH);
        if (lsWallpaperPref != null && !AsusLSDBHelper.getDeviceSupportLSWallpaper(getContext())) {
            root.removePreference(lsWallpaperPref);
        } else {
            if (AsusLSUtils.DEBUG_FLAG) {
                Log.w(TAG, "createPreferenceHierarchy: lsWallpaperPref=" + lsWallpaperPref);
            }
        }
        Preference lsThemePref = root.findPreference(KEY_LOCKSCREEN_THEME_SWITCH);
        if (lsThemePref != null &&
                (!AsusLSDBHelper.getDeviceSupportLSTheme(getContext()) ||
                        !AsusLSUtils.isThemeAppEnabled(this.getContext()))) {
            root.removePreference(lsThemePref);
        } else {
            if (AsusLSUtils.DEBUG_FLAG) {
                Log.w(TAG, "createPreferenceHierarchy: lsWallpaperPref=" + lsWallpaperPref);
            }
        }
        mWeatherAnimationSwitchPref =
                (LockscreenWeatherAnimationSwitchPreference) root.findPreference(KEY_WEATHER_ANIMATION_SWITCH);
        if (mWeatherAnimationSwitchPref != null &&
                !AsusLSDBHelper.getDeviceSupportWeatherAnimation(getContext())) {
            root.removePreference(mWeatherAnimationSwitchPref);
        }
        mLSClockWidgetSwitchPref =
                (LockscreenClockWidgetSwitchPreference) root.findPreference(KEY_LCOKSCREEN_CLOCK_WIDGET_SWITCH);
        if (mLSClockWidgetSwitchPref != null &&
                !AsusLSDBHelper.getDeviceSupportLSClockWidget(getContext())) {
            root.removePreference(mLSClockWidgetSwitchPref);
        }
        // END:Evelyn_Chang@asus.com for ZenFone3.
        // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 end
        return root;
    }

    //add by sunxiaolong@wind-mobi.com for smart lock start
    private void addTrustAgentSettings(PreferenceGroup securityCategory) {
        final boolean hasSecurity = mLockPatternUtils.isSecure(MY_USER_ID);
        ArrayList<TrustAgentComponentInfo> agents =
                getActiveTrustAgents(getPackageManager(), mLockPatternUtils, mDPM);
        for (int i = 0; i < agents.size(); i++) {
            final TrustAgentComponentInfo agent = agents.get(i);
            Preference trustAgentPreference =
                    new Preference(securityCategory.getContext());
            trustAgentPreference.setKey(KEY_TRUST_AGENT);
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            // Create intent for this preference.
            Intent intent = new Intent();
            intent.setComponent(agent.componentName);
            intent.setAction(Intent.ACTION_MAIN);
            trustAgentPreference.setIntent(intent);
            // Add preference to the settings menu.
            securityCategory.addPreference(trustAgentPreference);

            if (agent.disabledByAdministrator) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.trust_agent_disabled_device_admin);
            } else if (!hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
    }

    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            PackageManager pm, LockPatternUtils utils, DevicePolicyManager dpm) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(MY_USER_ID);

        boolean disableTrustAgents = (dpm.getKeyguardDisabledFeatures(null)
                & DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS) != 0;

        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                if (disableTrustAgents && dpm.getTrustAgentConfiguration(
                        null, TrustAgentUtils.getComponentName(resolveInfo)) == null) {
                    trustAgentComponentInfo.disabledByAdministrator = true;
                }
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }
    //add by sunxiaolong@wind-mobi.com for smart lock end
	//xuyi@wind-mobi.com 20160518 add for CM_weather begin
    public boolean isCmWeatherEnable() {
        int status = Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_CM_WEATHER_LOCAL_GTM,
                1);
        //M: huangyouzhong@wind-mobi.com 20160802 for 124937 -s:add try catch
        try {
            return (status == 1 && isInstallCMWeather("cmcm.weather.asus") && (!(isApplcationDisabled("cmcm.weather.asus"))));
        } catch (Exception e) {
            return false ;
        }
        //M: huangyouzhong@wind-mobi.com 20160802 for 124937 -e
    }

    public boolean isInstallCMWeather(String pk) {
        try {
            PackageManager pm = getActivity().getPackageManager();
            pm.getPackageInfo(pk, 0);
            Log.d(TAG, "isInstallCMWeather is true");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "isInstallCMWeather e = " + e);
        }
        return false;
    }

    private boolean isApplcationDisabled(String packageName) {
        PackageManager pm = getActivity().getPackageManager();
        return PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER == pm
                .getApplicationEnabledSetting(packageName);
    }
	//xuyi@wind-mobi.com 20160518 add for CM_weather end

    private void startIntruder(){
    final SettingsActivity activity = (SettingsActivity) getActivity();
    activity.startPreferencePanel(LockscreenIntruderSelfieSwitchPreference.class.getName(), null,
    R.string.lockscreen_intruder_selfie_title, null, AsusLockScreenSettings.this,
        0);
    activity.finish();
    }
    private static ArrayList<TrustAgentComponentInfo> getActiveTrustAgents(
            PackageManager pm, LockPatternUtils utils) {
        ArrayList<TrustAgentComponentInfo> result = new ArrayList<TrustAgentComponentInfo>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT,
                PackageManager.GET_META_DATA);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(UserHandle.myUserId());
        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            for (int i = 0; i < resolveInfos.size(); i++) {
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo == null) continue;
                if (!TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) continue;
                TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                if (trustAgentComponentInfo.componentName == null ||
                        !enabledTrustAgents.contains(
                                TrustAgentUtils.getComponentName(resolveInfo)) ||
                        TextUtils.isEmpty(trustAgentComponentInfo.title)) continue;
                result.add(trustAgentComponentInfo);
                if (ONLY_ONE_TRUST_AGENT) break;
            }
        }
        return result;
    }

    private boolean isWhatsNextServiceEnabled(){
        String AUTHORITY = "com.asus.sitd.whatsnext.contentprovider";
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
        Uri uri = Uri.withAppendedPath(CONTENT_URI, "whatsnextEnable");
        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
        boolean isWhatsNextEnable = false;
        if (null != cursor) {
            try {
                isWhatsNextEnable = cursor.getCount() > 0;
            } catch(Exception e){
                Log.w(TAG,"isWhatsNextServiceEnabled ex:",e);
                isWhatsNextEnable = false;
            } finally {
                cursor.close();
            }
        }
        return isWhatsNextEnable;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
     // +++ Arica, 20130813, If device is a DemoUnit, it can be locked by anyway. <Begin>
//        if (LiveDemoUnit.AccFlagRead() > 0) {
//            mChooseLockSettingsHelper.utils().clearLock(false);
//            mChooseLockSettingsHelper.utils().setLockScreenDisabled(true);
//        }
     // +++ Arica <end>
        createPreferenceHierarchy();

        // +++
        updatePreference();
        // ---

        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (mBiometricWeakLiveliness != null) {
            //TODO
            mBiometricWeakLiveliness.setChecked(
                    /*lockPatternUtils.isBiometricWeakLivelinessEnabled()*/false);
        }
        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled(UserHandle.myUserId()));
        }
        if (mPowerButtonInstantlyLocks != null) {
            mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks(UserHandle.myUserId()));
        }

        if (mEnableKeyguardWidgets != null) {
            //TODO
            mEnableKeyguardWidgets.setChecked(/*lockPatternUtils.getWidgetsEnabled()*/false);
        }

        // +++
        if(mShortcutSwitchPref != null){
            mShortcutSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
        if(mStatusBarSwitchPreference != null){
            mStatusBarSwitchPreference.setOnSwitchCheckedChangeListener(this);
        }
        if(mWhatsNextSwitchPref != null){
            mWhatsNextSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
        if(mInstantCameraSwitchPref != null){
            mInstantCameraSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
        if(mSkipSlideSwitchPref != null){
            mSkipSlideSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
		
		//xuyi@wind-mobi.com 20160518 add for CM_weather begin
		if(WIND_DEF_CM_WEATHER){
			if (mDisableWeatherInfoPageSwitchPref != null) {
				mDisableWeatherInfoPageSwitchPref.setOnSwitchCheckedChangeListener(this);
			}
		}
		//xuyi@wind-mobi.com 20160518 add for CM_weather end

        // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 begin
        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        if (mWeatherAnimationSwitchPref != null) {
            mWeatherAnimationSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
        if (mLSClockWidgetSwitchPref != null) {
            mLSClockWidgetSwitchPref.setOnSwitchCheckedChangeListener(this);
        }
        // END:Evelyn_Chang@asus.com for ZenFone3.
        // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 end
        if(mEnableCameraSwitchPref != null){
            mEnableCameraSwitchPref.setOnSwitchCheckedChangeListener(this);
        }

        // ---
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final ContentResolver res = getContentResolver();
        final String key = preference.getKey();
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
        	startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
        } else if (KEY_BIOMETRIC_WEAK_IMPROVE_MATCHING.equals(key)) {
            ChooseLockSettingsHelper helper =
                    new ChooseLockSettingsHelper(this.getActivity(), this);
            //TODO-third argument ""
            if (!helper.launchConfirmationActivity(
                    CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST, null, false)) {
                // If this returns false, it means no password confirmation is required, so
                // go ahead and start improve.
                // Note: currently a backup is required for biometric_weak so this code path
                // can't be reached, but is here in case things change in the future
                startBiometricWeakImprove();
            }
        } else if (KEY_BIOMETRIC_WEAK_LIVELINESS.equals(key)) {
            if (isToggled(preference)) {
                //TODO
                //lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
            } else {
                // In this case the user has just unchecked the checkbox, but this action requires
                // them to confirm their password.  We need to re-check the checkbox until
                // they've confirmed their password
                mBiometricWeakLiveliness.setChecked(true);
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
               //TODO-third argument
                if (!helper.launchConfirmationActivity(
                                CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF, null, false)) {
                    // If this returns false, it means no password confirmation is required, so
                    // go ahead and uncheck it here.
                    // Note: currently a backup is required for biometric_weak so this code path
                    // can't be reached, but is here in case things change in the future
                    //TODO
                    //lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
                    mBiometricWeakLiveliness.setChecked(false);
                }
            }
        } else if (KEY_LOCK_ENABLED.equals(key)) {
            //TODO
            //lockPatternUtils.setLockPatternEnabled(isToggled(preference));
        } else if (KEY_VISIBLE_PATTERN.equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(isToggled(preference),UserHandle.myUserId());
        } else if (KEY_POWER_INSTANTLY_LOCKS.equals(key)) {
            lockPatternUtils.setPowerButtonInstantlyLocks(isToggled(preference),UserHandle.myUserId());
        } else if (KEY_ENABLE_WIDGETS.equals(key)) {
            //TODO
            //lockPatternUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            //modify by sunxiaolong@wind-mobi.com for asus request start
        } else if (KEY_SHORTCUT_SWITCH.equals(key)){
                Intent intent = new Intent(getActivity(), AsusQuickAccessSettings.class);
                startActivity(intent);
            //modify by sunxiaolong@wind-mobi.com for asus request end
        } else if (KEY_WHATS_NEXT_SWITCH.equals(key)){
            mWhatsNextSwitchPref.setSwitchChecked(!mWhatsNextSwitchPref.isChecked());
        } else if (KEY_INSTANT_CAMERA_SWITCH.equals(key)){
            mInstantCameraSwitchPref.setSwitchChecked(!mInstantCameraSwitchPref.isChecked());
        } else if (KEY_ENABLE_CAMERA_SWITCH.equals(key)){
            mEnableCameraSwitchPref.setSwitchChecked(!mEnableCameraSwitchPref.isChecked());
        } else if (KEY_STATUSBAR_SWITCH.equals(key)){
            mStatusBarSwitchPreference.setSwitchChecked(!mStatusBarSwitchPreference.isChecked());
        } else if (KEY_SKIP_SLIDE_SWITCH.equals(key)) {
            mSkipSlideSwitchPref.setSwitchChecked(!mSkipSlideSwitchPref.isChecked());
        } else if (KEY_WALLPAPER.equals(key)){
//            startFragment(this, ASUS_LOCKSCREEN_WALLPAPER_FRAGMENT,
//                  SET_WALLPAPER_REQUEST, null);
        }
        // +++ vivian_sun 2014.10.16 skip slid
        /*else if (preference == mDisableSlidePref) {
            Settings.System.putInt(
                    getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED,
                    mDisableSlidePref.isChecked() ? 1 : 0);
        }*/
        // --- vivian_sun 2014.10.16 skip slid
        // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 begin
        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        else if (KEY_WEATHER_ANIMATION_SWITCH.equals(key)) {
            mWeatherAnimationSwitchPref.setSwitchChecked(!mWeatherAnimationSwitchPref.isChecked());
        } else if (KEY_LCOKSCREEN_CLOCK_WIDGET_SWITCH.equals(key)) {
            mLSClockWidgetSwitchPref.setSwitchChecked(!mLSClockWidgetSwitchPref.isChecked());
        }
        // END:Evelyn_Chang@asus.com for ZenFone3.
        // sunhuihui@wind-mobi.com modify Feature# merge lockscreen weather patch. 2016/7/21 end
        else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mLockAfter) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, timeout);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
        }
        return true;
    }

    private void setupLockAfterPreference() {
        // Compatible with pre-Froyo
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        mLockAfter.setValue(String.valueOf(currentTimeout));
        mLockAfter.setOnPreferenceChangeListener(this);
        final long adminTimeout = (mDPM != null ? mDPM.getMaximumTimeToLock(null) : 0);
        final long displayTimeout = Math.max(0,
                Settings.System.getInt(getContentResolver(), SCREEN_OFF_TIMEOUT, 0));
        if (adminTimeout > 0) {
            // This setting is a slave to display timeout when a device policy is enforced.
            // As such, maxLockTimeout = adminTimeout - displayTimeout.
            // If there isn't enough time, shows "immediately" setting.
            disableUnusableTimeouts(Math.max(0, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        // Update summary message with current value
        long currentTimeout = Settings.Secure.getLong(getContentResolver(),
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000);
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, entries[best]));
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        final CharSequence[] entries = mLockAfter.getEntries();
        final CharSequence[] values = mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            mLockAfter.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            mLockAfter.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.valueOf(mLockAfter.getValue());
            if (userPreference <= maxTimeout) {
                mLockAfter.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    private boolean isToggled(Preference pref) {
        return ((SwitchPreference) pref).isChecked();
    }

    public void startBiometricWeakImprove(){
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_IMPROVE_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            startBiometricWeakImprove();
            return;
        } else if (requestCode == CONFIRM_EXISTING_FOR_BIOMETRIC_WEAK_LIVELINESS_OFF &&
                resultCode == Activity.RESULT_OK) {
            final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
            //TODO
            //lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            // Setting the mBiometricWeakLiveliness checked value to false is handled when onResume
            // is called by grabbing the value from lockPatternUtils.  We can't set it here
            // because mBiometricWeakLiveliness could be null
            return;
        }
        createPreferenceHierarchy();
    }

    private void updatePreference() {
        handleStateChanged();
        if(mShortcutSwitchPref != null){
            mShortcutSwitchPref.setSwitchChecked(mIsShortcutEnabled);
        }
        if(mStatusBarSwitchPreference != null){
            mStatusBarSwitchPreference.setSwitchChecked(mIsStatusbarEnabled);
        }
        if(mWhatsNextSwitchPref != null){
            mWhatsNextSwitchPref.setSwitchChecked(mIsWhatsNextEnabled);
        }
        if(mInstantCameraSwitchPref != null){
            mInstantCameraSwitchPref.setSwitchChecked(mIsInstantCameraEnabled);
        }
        if(mSkipSlideSwitchPref != null){
            mSkipSlideSwitchPref.setSwitchChecked(mIsSkipSlide);
        }
		
		//xuyi@wind-mobi.com 20160518 add for CM_weather begin
		if(WIND_DEF_CM_WEATHER){
			if (mDisableWeatherInfoPageSwitchPref != null) {
				mDisableWeatherInfoPageSwitchPref.setSwitchChecked(mIsCMWeatherEnable);
			}
		}
		//xuyi@wind-mobi.com 20160518 add for CM_weather end
		
        if(mEnableCameraSwitchPref != null){
            mEnableCameraSwitchPref.setSwitchChecked(mIsCameraSettingEnabled);
        }
        // sunhuihui@wind-mobi.com modify Feature# lockscreen weather patch 2016/7/21 begin
        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        if (mWeatherAnimationSwitchPref != null) {
            mWeatherAnimationSwitchPref.setSwitchChecked(mIsWeatherAnimationEnabled);
        }
        if (mLSClockWidgetSwitchPref != null) {
            mLSClockWidgetSwitchPref.setSwitchChecked(mShowClockWidget);
        }
        // END:Evelyn_Chang@asus.com for ZenFone3.
        // sunhuihui@wind-mobi.com modify Feature# lockscreen weather patch 2016/7/21 end
    }

    private void handleStateChanged() {
        mIsShortcutEnabled = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, 1) == 1);
        mIsStatusbarEnabled = (Settings.System.getInt(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_DISPLAY_STATUS_BAR, 1) == 1);
        int status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_WHATSNEXT, 0);
        mIsWhatsNextEnabled = (status == 0 ? false : true);
        //liyong01@wind-mobi.com add for Feature#122543 -s      
        mIsInstantCameraEnabled = (Settings.System.getInt(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_INSTANT_CAMERA, 1) == 1);
       /* status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_INSTANT_CAMERA, 0);
        mIsInstantCameraEnabled = (status == 0 ? false : true);*/
        //liyong01@wind-mobi.com add for Feature#122543 -e  

        status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_ENABLE_CAMERA, 0);
        mIsCameraSettingEnabled = (status == 0 ? false : true);

        status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED, 0);
        mIsSkipSlide = (status == 0 ? false : true);
		
		//xuyi@wind-mobi.com 20160518 add for CM_weather begin
		if(WIND_DEF_CM_WEATHER){
			status = Settings.System.getInt(getContentResolver(),
			Settings.System.ASUS_LOCKSCREEN_CM_WEATHER_ENABLE, 1);
			mIsCMWeatherEnable = (status == 1 ? false : true);
		}
		//xuyi@wind-mobi.com 20160518 add for CM_weather end

        // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 begin
        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        mIsWeatherAnimationEnabled = AsusLSDBHelper.getWeatherAnimationSetting(getContext());
        mShowClockWidget = AsusLSDBHelper.getLSClockWidgetSetting(getContext());
        // END:Evelyn_Chang@asus.com for ZenFone3.
        // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 end
    }

   @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.switchshortcut) {
            if (buttonView instanceof Switch) {
                if (mIsShortcutEnabled != isChecked) {
                    mIsShortcutEnabled = isChecked;
                    int status = (mIsShortcutEnabled ? 1 : 0);
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, status);

                    mShortcutSwitchPref.setSwitchChecked(mIsShortcutEnabled);
                }
            }
        }else if (id == R.id.switch_statusbar) {
            if (buttonView instanceof Switch) {
                if (mIsStatusbarEnabled != isChecked) {
                    mIsStatusbarEnabled = isChecked;
                    int status = (mIsStatusbarEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_DISPLAY_STATUS_BAR, status);
                    mStatusBarSwitchPreference.setSwitchChecked(mIsStatusbarEnabled);
                }
            }
        }else if (id == R.id.switchwhatsnext) {
            if (buttonView instanceof Switch) {
                if (mIsWhatsNextEnabled != isChecked) {
                    mIsWhatsNextEnabled = isChecked;
                    int status = (mIsWhatsNextEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_WHATSNEXT, status);
                    mWhatsNextSwitchPref.setSwitchChecked(mIsWhatsNextEnabled);
                }
            }
        }else if (id == R.id.switch_instant_camera) {
            if (buttonView instanceof Switch) {
                if (mIsInstantCameraEnabled != isChecked) {
                    mIsInstantCameraEnabled = isChecked;
                    int status = (mIsInstantCameraEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_INSTANT_CAMERA, status);
                    //BEGIN: Steven_Chao@asus.com
                    Log.d(TAG, "mIsInstantCameraEnabled=" + mIsInstantCameraEnabled + ". Set persist.asus.instant_camera");
                    SystemProperties.set("persist.asus.instant_camera", mIsInstantCameraEnabled ? "1" : "0");
                    //END: Steven_Chao@asus.com
                    if(mInstantCameraSwitchPref != null){
                        mInstantCameraSwitchPref.setSwitchChecked(mIsInstantCameraEnabled);
                    }
                }
            }
        }else if (id == R.id.switch_enable_lockscreen_camera_widget) {
            if (buttonView instanceof Switch) {
                if (mIsCameraSettingEnabled != isChecked) {
                    mIsCameraSettingEnabled = isChecked;
                    int status = (mIsCameraSettingEnabled ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_ENABLE_CAMERA, status);
                    mEnableCameraSwitchPref.setSwitchChecked(mIsCameraSettingEnabled);
                }
            }
        }else if (id == R.id.switchskipslide) {
            if (buttonView instanceof Switch) {
                if (mIsSkipSlide != isChecked) {
                    mIsSkipSlide = isChecked;
                    int status = (mIsSkipSlide ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED, status);
                    mSkipSlideSwitchPref.setSwitchChecked(mIsSkipSlide);
                }
            }
        }
		//xuyi@wind-mobi.com 20160518 add for CM_weather begin
		else if (WIND_DEF_CM_WEATHER && id == R.id.switch_disable_cm_weather) {
            if (buttonView instanceof Switch) {
                if (mIsCMWeatherEnable != isChecked) {
                    mIsCMWeatherEnable = isChecked;
                    int status = (mIsCMWeatherEnable ? 0 : 1);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_CM_WEATHER_ENABLE,
                            status);
                    mDisableWeatherInfoPageSwitchPref
                            .setSwitchChecked(mIsCMWeatherEnable);
                }
            }
         }
        // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 begin
        else if (id == R.id.switch_weather_animation) {
            if (buttonView instanceof Switch) {
                if (mIsWeatherAnimationEnabled != isChecked) {
                    mIsWeatherAnimationEnabled = isChecked;
                    AsusLSDBHelper.setWeatherAnimationSetting(getContext(), mIsWeatherAnimationEnabled);
                }
            }
        } else if (id == R.id.switch_asus_lockscreen_clock_widget) {
            if (buttonView instanceof Switch) {
                if (mShowClockWidget != isChecked) {
                    mShowClockWidget = isChecked;
                    AsusLSDBHelper.setLSClockWidgetSetting(getContext(), mShowClockWidget);
                }
            }
        }
       // sunhuihui@wind-mobi.com modify Feature# merge weather patch. 2016/7/21 end
       //xuyi@wind-mobi.com 20160518 add for CM_weather end
    }

   @Override
   protected int getMetricsCategory() {
       // TODO Auto-generated method stub
       return MetricsLogger.LOCKSCREEN;
   }
}
