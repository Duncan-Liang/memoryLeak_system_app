package com.android.settings.zenmotion;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
// sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 begin
import android.os.SystemProperties;
// sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 end
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchIndexableResource;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;  
import java.util.List;

import com.android.internal.logging.MetricsLogger;

public class AsusZenMotionSettings extends SettingsPreferenceFragment implements
        OnPreferenceClickListener,
        CompoundButton.OnCheckedChangeListener, AsusMotionSettings.OnSwitchChangedListener,
        AsusTouchSettings.OnSwitchChangedListener, Indexable {
    private static final String TAG = "AsusZenMotionSettings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;

    private static final String KEY_MOTION_GESTURE = "asus_motion_gesture";
    private static final String KEY_TOUCH_GESTURE = "asus_touch_gesture";
    private static final String KEY_ONEHAND_MODE = "asus_onehand_mode";
    // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -s
    private static final String KEY_GESTURES_SETTINGS = "gestures_settings";
    // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -e
    private static ZenMotionGestureSwitchPreference sMotionPreference = null;
    private static ZenTouchGestureSwitchPreference sTouchPreference = null;
    private static Preference mOneHandMode = null;
    // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -s
    private static Preference mGesturesSettings = null;
    private static final boolean WIND_GESTURES_SUPPORTED = SystemProperties.get("ro.wind_gestures_supported").equals("1");
    // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -e

    // sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 begin
    public static final boolean WIND_DEF_ONE_HAND_CONTROL = SystemProperties
            .getInt("ro.wind.def.one.hand.control", 0) == 1;

    // sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 end
    // pengfugen@wind-mobi.com Feature#102863 rm motion gesture start
    private static final boolean WIND_DEF_RM_MOTION_GESTURE = SystemProperties.get("ro.wind.def.rm_mo_gesture").equals("1");
    // pengfugen@wind-mobi.com Feature#102863 rm motion gesture end

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_zenmotion_operation_settings);
        // pengfugen@wind-mobi.com Feature#102863 rm motion gesture start
        if (!WIND_DEF_RM_MOTION_GESTURE && (getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE)
                || DEBUG_OPEN_ALL_FEATURE)) {
        // pengfugen@wind-mobi.com Feature#102863 rm motion gesture end
            sMotionPreference = (ZenMotionGestureSwitchPreference) findPreference(KEY_MOTION_GESTURE);
        } else {
            removePreference(KEY_MOTION_GESTURE);
        }

        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP)
                || getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP)
                || DEBUG_OPEN_ALL_FEATURE) {
            sTouchPreference = (ZenTouchGestureSwitchPreference) findPreference(KEY_TOUCH_GESTURE);
        } else {
            removePreference(KEY_TOUCH_GESTURE);
        }

        // sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 begin
        if (WIND_DEF_ONE_HAND_CONTROL && getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_WHOLE_SYSTEM_ONEHAND)) {
            mOneHandMode = (Preference) findPreference(KEY_ONEHAND_MODE);
        } else {
            removePreference(KEY_ONEHAND_MODE);
        }
        // sunhuihui@wind-mobi.com Feature# remove one hand control 16-3-31 end

        // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -s
        if (WIND_GESTURES_SUPPORTED) {
            mGesturesSettings = (Preference)findPreference(KEY_GESTURES_SETTINGS);
        } else {
            removePreference(KEY_GESTURES_SETTINGS);
        }
        // wangyan@wind-mobi.com add 2016/04/02 for Feature #100625 -e
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        if (sMotionPreference != null) {
            sMotionPreference.resume();

        }
        if (sTouchPreference != null) {
            sTouchPreference.resume();
        }
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (sMotionPreference != null) {
            sMotionPreference.pause();
        }

        if (sTouchPreference != null) {
            sTouchPreference.pause();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub

    }

    // AsusMotionSettings
    @Override
    public void onMotionSwitchChanged(boolean isChecked) {
        // TODO Auto-generated method stub
        if(DEBUG) Log.d(TAG, "CALLBACK()...onMotionSwitchChanged: " + isChecked);
        if (null == sMotionPreference) {
            if(DEBUG) Log.d(TAG, "CALLBACK()...onSwitchChanged: null == sMotionPreference");
            sMotionPreference = (ZenMotionGestureSwitchPreference) findPreference(KEY_MOTION_GESTURE);
        }

        if (null != sMotionPreference)
            sMotionPreference.handleStateChanged();
    }

    @Override
    public void onTouchSwitchChanged(boolean isChecked) {
        // TODO Auto-generated method stub
        if(DEBUG) Log.d(TAG, "CALLBACK()...onTouchSwitchChanged: " + isChecked);
        if (null == sTouchPreference) {
            if(DEBUG) Log.d(TAG, "CALLBACK()...onTouchSwitchChanged: null == sTouchPreference");
            sTouchPreference = (ZenTouchGestureSwitchPreference) findPreference(KEY_TOUCH_GESTURE);
        }
        if (null != sTouchPreference)
            sTouchPreference.handleStateChanged();
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.MAIN_SETTINGS;
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
                sir.xmlResId = R.xml.asus_zenmotion_operation_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<>();
                PackageManager pm = context.getPackageManager();
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE)) {
                    keys.add(KEY_MOTION_GESTURE);
                }
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP) &&
                    !pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP)) {
                    keys.add(KEY_TOUCH_GESTURE);
                }
                return keys;
            }
        };
    }
}
