package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
// sunhuihui@wind-mobi.com modify begin Feature#110139 2016/5/10
import android.app.ActivityManagerNative;
import android.os.UserManager;
import android.content.pm.UserInfo;
// sunhuihui@wind-mobi.com modify end Feature#110139 2016/5/10
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.graphics.drawable.Drawable;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.util.ResCustomizeConfig;

import android.database.ContentObserver;
import android.os.Handler;
//add by sunxiaolong@wind-mobi.com for game toolbar patch begin
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.content.ComponentName;
//add by sunxiaolong@wind-mobi.com for game toolbar patch end

public class CustomizeSettings extends SettingsPreferenceFragment
    implements Preference.OnPreferenceChangeListener
{
    private static final String TAG = "CustomizeSettings";
    private static final String KEY_RECENT_APPS_KEY_LONG_PRESS_CONTROL = "recent_apps_key_long_press_control_settings";
    private static final String KEY_RECENT_APPS_KEY_LONG_PRESS_CATEGORY = "recent_apps_key_long_press_category";
    private static final String SCREENSHOT = "screenshot";
    private static final String LONG_PRESSED_FUNC = "long_pressed_func";
    private static final int LONG_PRESSED_FUNC_SCREENSHOT = 0;
    private static final int LONG_PRESSED_FUNC_MENU = 1;
    private static final int LONG_PRESSED_FUNC_DEFAULT = LONG_PRESSED_FUNC_MENU;

    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private static final String KEY_GLOVE_MODE_SETTING = "asus_glove_mode_setting";
    private static final String KEY_GLOVE_MODE_SETTING_CATEGORY = "asus_glove_mode_category";
    //END: Jeffrey_Chiang@asus.com

    // sunhuihui@wind-mobi.com modify begin Feature#110139 2016/5/10
    private static final String KEY_QUICK_SETTINGS_EDITOR = "interface_setting_category";
    // sunhuihui@wind-mobi.com modify end Feature#110139 2016/5/10
    //add by sunxiaolong@wind-mobi.com for game toolbar patch begin
    //Game tool bar
    private static final String KEY_IN_APP_TOOLBAR_CATEGORY = "in_app_toolbar_category";
    private static final String KEY_GAME_TOOLBAR_APP_SETTINGS = "game_toolbar_app_settings";
    private PreferenceCategory mInAPPToolBar;
    private Preference mGameToolBar;
    //add by sunxiaolong@wind-mobi.com for game toolbar patch end

    private ListPreference mRecentAppsKeyLongPressControlPref;//END: Steven_Chao@asus.com Glove mode settings

    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private CheckBoxPreference mGloveModeSetting;
    private Handler mHandler = new Handler();
    private boolean mHasGloveModeHWFeature = false;
    private class GloveModeContentObserver extends ContentObserver {
        GloveModeContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean value = isGloveModeEnabled();
            Log.v(TAG, "Glove mode settings onChange. Value is " + value);
            setGloveModeCheckBox(value);
        }
    }
    private ContentObserver mGloveModeObserver = null;
    //END: Jeffrey_Chiang@asus.com Glove mode settings

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.customize_settings);

        boolean hasNaNavigationBar = false;
        try {
            hasNaNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (Exception e) {
        }

        if (!hasNaNavigationBar) {
            mRecentAppsKeyLongPressControlPref = (ListPreference)findPreference(KEY_RECENT_APPS_KEY_LONG_PRESS_CONTROL);
            int mode = getRecentAppsKeyLongPressControlMode();
            if (mRecentAppsKeyLongPressControlPref != null) {
                mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
                mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
                mRecentAppsKeyLongPressControlPref.setOnPreferenceChangeListener(this);
                Drawable icon = getResources().getDrawable(R.drawable.ic_sysbar_recent_default_resize_dark);
                mRecentAppsKeyLongPressControlPref.setDialogIcon(icon);
                Settings.System.putInt(getContentResolver(), SCREENSHOT,
                        (mode == LONG_PRESSED_FUNC_SCREENSHOT) ? 1 : 0);
                Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC, mode);
            }
        } else {
            PreferenceCategory recentKeyLongPressCategory = (PreferenceCategory) findPreference(KEY_RECENT_APPS_KEY_LONG_PRESS_CATEGORY);
            getPreferenceScreen().removePreference(recentKeyLongPressCategory);
        }

        //BEGIN: Steven_Chao@asus.com Glove mode settings
        PackageManager pm = getPackageManager();
        // mHasGloveModeHWFeature =
        // pm.hasSystemFeature(PackageManager.FEATURE_ASUS_GLOVE);
        if (!mHasGloveModeHWFeature) {
            PreferenceCategory gloveModeSettingCategory = (PreferenceCategory) findPreference(KEY_GLOVE_MODE_SETTING_CATEGORY);
            getPreferenceScreen().removePreference(gloveModeSettingCategory);
        } else {
            mGloveModeSetting = (CheckBoxPreference) findPreference(KEY_GLOVE_MODE_SETTING);
            mGloveModeObserver = new GloveModeContentObserver(mHandler);
        }
        //END: Steven_Chao@asus.com Glove mode settings
        //add by sunxiaolong@wind-mobi.com for game toolbar patch begin
        //+++ Game tool bar
        mInAPPToolBar = (PreferenceCategory) findPreference(KEY_IN_APP_TOOLBAR_CATEGORY);
        mGameToolBar = (Preference) findPreference(KEY_GAME_TOOLBAR_APP_SETTINGS);
        ApplicationInfo appInfo = null;
        boolean isFound = true;
        try {
            appInfo = pm.getApplicationInfo("com.asus.gamewidget", 0);
        } catch (NameNotFoundException ex) {
            isFound = false;
            ex.printStackTrace();
        }
        if (isFound) {
            int isEnableGameGenie = pm.getApplicationEnabledSetting("com.asus.gamewidget");
            if (isEnableGameGenie == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                isFound = false;
            }
        }

        if (!isFound) {
            mInAPPToolBar.removePreference(mGameToolBar);
        }
        if (mInAPPToolBar.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(mInAPPToolBar);
        }
        //--- Game tool bar
        //add by sunxiaolong@wind-mobi.com for game toolbar patch end
        // sunhuihui@wind-mobi.com modify begin Feature#110139 2016/5/10
        //+++joy_wen@asus.com Quick settings editor
        //Hide the section if it's not owner(admin)
        boolean isAdmin = false;
        try {
            int myUserId = ActivityManagerNative.getDefault().getCurrentUser().id;
            UserManager mCoverUserManager = (UserManager) getActivity().getApplicationContext().getSystemService(Context.USER_SERVICE);
            UserInfo user = mCoverUserManager.getUserInfo(myUserId);
            isAdmin = user.isAdmin();
        } catch (Exception e) {
        }
        if (!isAdmin) {
            PreferenceCategory gloveModeSettingCategory = (PreferenceCategory) findPreference(KEY_QUICK_SETTINGS_EDITOR);
            getPreferenceScreen().removePreference(gloveModeSettingCategory);
        }
        //---joy_wen@asus.com Quick settings editor
        // sunhuihui@wind-mobi.com modify end Feature#110139 2016/5/10
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue)
    {
        if(preference == mRecentAppsKeyLongPressControlPref){
            if (objValue instanceof String) {
                putRecentAppsKeyLongPressControlSetting(Integer.parseInt((String)objValue));
            }
        }
        return true;
    }

     @Override
    public void onResume() {
        super.onResume();
        updateState(true);
    //BEGIN: Steven_Chao@asus.com Glove mode settings
        if (mHasGloveModeHWFeature) {
            setGloveModeCheckBox(isGloveModeEnabled());
            registerGloveModeContentObserver();
        }
    //END: Steven_Chao@asus.com Glove mode settings
    }

    @Override
    public void onStop() {
        super.onStop();
    //BEGIN: Steven_Chao@asus.com Glove mode settings
        if (mHasGloveModeHWFeature) {
            unregisterGloveModeContentObserver();
        }
    //END: Steven_Chao@asus.com Glove mode settings
    }

    private void updateState(boolean force) {
        updateRecentAppsKeyLongPressControlSetting();

    }

    private void putRecentAppsKeyLongPressControlSetting(int mode) {
        Settings.System.putInt(getContentResolver(), SCREENSHOT,
                    (mode == LONG_PRESSED_FUNC_SCREENSHOT) ? 1 : 0);
        Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC, mode);
        mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
        mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
    }

    private void updateRecentAppsKeyLongPressControlSetting() {
        if(mRecentAppsKeyLongPressControlPref == null) return;
        int mode = getRecentAppsKeyLongPressControlMode();
        mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
        mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
        if (preference == mGloveModeSetting) {
            setGloveModeEnabled(mGloveModeSetting.isChecked());
        }
        // END: Jeffrey_Chiang@asus.com Glove mode settings
        //add by sunxiaolong@wind-mobi.com for game toolbar patch begin
        //Game tool bar
        if (preference == mGameToolBar) {
            Intent intent = new Intent("com.asus.gamewidget.action.SETTINGS");
            intent .putExtra("callfrom", "AsusSettings");
            getActivity().startActivity(intent);
        }
        //add by sunxiaolong@wind-mobi.com for game toolbar patch end
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private int getRecentAppsKeyLongPressControlMode() {
        return Settings.System.getInt(getContentResolver(), LONG_PRESSED_FUNC,
            LONG_PRESSED_FUNC_DEFAULT);
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.MAIN_SETTINGS;
    }


    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private void registerGloveModeContentObserver() {
        //getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.GLOVE_MODE), true, mGloveModeObserver);
    }

    private void unregisterGloveModeContentObserver() {
        //getContentResolver().unregisterContentObserver(mGloveModeObserver);
    }

    private void setGloveModeEnabled(boolean value) {
        //Settings.System.putInt(getContentResolver(),Settings.System.GLOVE_MODE, value?1:0);
    }

    private boolean isGloveModeEnabled() {
        return false;//Settings.System.getInt(getContentResolver(), Settings.System.GLOVE_MODE, 0) == 1;
    }

    private void setGloveModeCheckBox(boolean checked) {
        if (mGloveModeSetting != null) {
            mGloveModeSetting.setChecked(checked);
        }
    }
    // END: Jeffrey_Chiang@asus.com Glove mode settings

    public static boolean isGloveModeExist(Context context) {
        return false;//Settings.System.getInt(context.getContentResolver(), Settings.System.GLOVE_MODE, 0) == 1;
    }

    //when NavigationBar exist, it won't show recent app key in Customize Settings
    public static boolean isNavigationBarExist() {
        boolean hasNaNavigationBar = false;
        try {
            hasNaNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (Exception e) {
        }
        return hasNaNavigationBar;
    }

    public static boolean isSupportScreenShotSound() {
        return true;
    }
}
