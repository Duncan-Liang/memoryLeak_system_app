/**
 *  add by Irene Chen
 */

package com.android.settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.util.Log;
//add by xiongshigui@wind-mobi.com for #114456 start
import android.os.SystemProperties;
//add by xiongshigui@wind-mobi.com for #114456 end

public class DisplayWallpaperSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener{
    private static final String KEY_HOME_SCREEN = "home";
    private static final String KEY_LOCK_SCREEN = "lock_screen";
    private static final String KEY_HOME_LOCKSCREEN = "home_lockscreen";
    private PreferenceScreen mHome;
    private PreferenceScreen mLockScreen;
    private PreferenceScreen mHomeLockScreen;
    //add by xiongshigui@wind-mobi.com for #114456 start
    private static boolean WIND_DEF_ASUS_VLIFE_LOCKSCREEN = SystemProperties.get("ro.wind.def.vlife.lockscreen").equals("1");
    //add by xiongshigui@wind-mobi.com for #114456 end
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.display_personalize_settings);
        mHome = (PreferenceScreen) findPreference(KEY_HOME_SCREEN);
        mLockScreen = (PreferenceScreen) findPreference(KEY_LOCK_SCREEN);
        mHomeLockScreen = (PreferenceScreen) findPreference(KEY_HOME_LOCKSCREEN);

        mHome.setOnPreferenceClickListener(this);
        mLockScreen.setOnPreferenceClickListener(this);
        mHomeLockScreen.setOnPreferenceClickListener(this);

        // youxiaoyan@wind-mobi.com for delete lockScreenWallpaperSettings  2016/4/28 begin
        if (WIND_DEF_ASUS_VLIFE_LOCKSCREEN) {
            removePreference(KEY_LOCK_SCREEN);
            removePreference(KEY_HOME_LOCKSCREEN);
        }
        // youxiaoyan@wind-mobi.com for delete lockScreenWallpaperSettings  2016/4/28 end
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mHome) {
            startWallpaper(Settings.System.WALLPAPER_HOME);
        } else if (preference == mLockScreen) {
            startWallpaper(Settings.System.WALLPAPER_LOCKSCREEN);
        } else if(preference == mHomeLockScreen) {
            startWallpaper(Settings.System.WALLPAPER_BOTH);
        }
        return true;
    }

    private void startWallpaper(int dst) {
        Intent pickWallpaper = null;
        if(dst == Settings.System.WALLPAPER_HOME) {
            pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        } else if(dst == Settings.System.WALLPAPER_LOCKSCREEN) {
            pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER_LOCKSCREEN);
        } else if(dst == Settings.System.WALLPAPER_BOTH) {
            pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER_BOTH);
        }
        Intent chooser = Intent.createChooser(pickWallpaper,
                getText(R.string.wallpaper_settings_fragment_title));
        startActivity(chooser);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WALLPAPER_TYPE;
    }
}
