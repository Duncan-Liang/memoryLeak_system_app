/*
 * Filename : GesturesSettings.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

import java.util.ArrayList;

public final class GesturesSettings extends SettingsPreferenceFragment implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "GesturesSettings";
    private static final boolean DEBUG = Utils.DEBUG;
    public static final String DESTURES_ENABLED = "gestures_enabled";
    public static final String GESTURE_DOUBLE_CLICK = "gesture_double_click";
    public static final String KEY_GESTURE_C = "gesture_c";
    public static final String KEY_GESTURE_E = "gesture_e";
    public static final String KEY_GESTURE_W = "gesture_w";
    public static final String KEY_GESTURE_O = "gesture_o";
    public static final String KEY_GESTURE_UP = "gesture_up";
    public static final String KEY_GESTURE_S = "gesture_s";
    public static final String KEY_GESTURE_Z = "gesture_z";
    public static final String KEY_GESTURE_V = "gesture_v";
    public static final String CAMERA_PACKAGENAME = "com.asus.camera";
    public static final String CAMERA_FRONT_CLASSNAME = "com.asus.camera.CameraApp.frontcamera";
    public static final String CAMERA_BACK_CLASSNAME = "com.asus.camera.CameraApp";

    private final ArrayList<Preference> mAllPrefs = new ArrayList<Preference>();

    //private Switch mEnabledSwitch;
    private SwitchBar mSwitchBar;
    private GestureEnabler mGestureEnabler;

    protected int getMetricsCategory() {
        return MetricsLogger.GESTURES_SETTINGS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.gestures_settings);

        final int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            GestureSwitchPreference preference = (GestureSwitchPreference) getPreferenceScreen().getPreference(i);
            if (preference != null) {
                initState(preference);
                mAllPrefs.add(preference);
            }
        }
    }

    private void initialAllState() {

        final int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; i++) {
            GestureSwitchPreference preference = (GestureSwitchPreference) getPreferenceScreen().getPreference(i);
            if (preference != null) {
                initState(preference);
            }
        }
    }

    private void initState(GestureSwitchPreference preference) {
        boolean checked = getCheckedState(preference.getKey());
        if (DEBUG) {
            Log.i(TAG, "preference key=" + preference.getKey() + ", enabled=" + checked);
        }
        preference.setChecked(checked);
        boolean enabled = getState(DESTURES_ENABLED);
        preference.setEnabled(enabled);

    }

    public void setGesturesEnabled(boolean isChecked) {
        saveState(DESTURES_ENABLED, isChecked);
        setPrefsEnabledState(isChecked);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Activity activity = getActivity();
        SettingsActivity activity = (SettingsActivity) getActivity();
        LayoutInflater inflater = (LayoutInflater) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSwitchBar = activity.getSwitchBar();
        mGestureEnabler = new GestureEnabler(activity, mSwitchBar, this);
        mGestureEnabler.setupSwitchBar();
        //mEnabledSwitch = (Switch)inflater.inflate(com.mediatek.internal.R.layout.imageswitch_layout, null);
        //mEnabledSwitch.setOnCheckedChangeListener(this);
        boolean checked = getState(DESTURES_ENABLED);
        //mEnabledSwitch.setChecked(checked);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGestureEnabler.teardownSwitchBar();
    }

    @Override
    public void onStart() {
        super.onStart();
        Activity activity = getActivity();
        if (activity instanceof PreferenceActivity) {
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            //mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            /*activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
              Gravity.CENTER_VERTICAL | Gravity.END));*/
        }

    }

    @Override
    public void onResume() {
        if (mGestureEnabler != null) {
            mGestureEnabler.resume(getActivity());
        }
        super.onResume();
        View root = getView();
        if (root != null) {
            ListView lv = (ListView) root.findViewById(android.R.id.list);
            if (lv != null) {
                lv.setPadding(0, 0, 0, 0);
            }
        }
        initialAllState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mGestureEnabler != null) {
            mGestureEnabler.pause();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mSwitchBar.getSwitch()) {
            if (isChecked) {
                saveState(DESTURES_ENABLED, true);
                setPrefsEnabledState(true);
            } else {
                saveState(DESTURES_ENABLED, false);
                setPrefsEnabledState(isChecked);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < mAllPrefs.size(); i++) {
            Preference pref = mAllPrefs.get(i);

            pref.setEnabled(enabled);
        }
    }

    public void saveState(String key, boolean enabled) {
        Utils.enableGesture(getContext(), key, enabled);
    }

    public boolean getState(String key) {
        return Utils.isGestureEnabled(getContentResolver(), key);
    }

    private boolean getCheckedState(String key) {
        return Utils.isGestureChecked(getContentResolver(), key);
    }
}



