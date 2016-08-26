// xuyi@wind-mobi.com 20160525 add for BlueLightFilter begin

package com.android.settings.bluelightfilter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.android.settings.bluelightfilter.Constants;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.Preference.BaseSavedState;
import android.provider.Settings;
import com.android.settings.R;

public class BluelightFilterPreference extends Preference{
    private static final String TAG = "BluelightFilterPreference";
    private BluelightFilterSwitchEnabler mEnabler;
    private Switch mSwitch;
    private Context mContext;
    private boolean mRestoredOldState;

    /*private ContentObserver mBluelight_Switch_Observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onSwitchChanged();
        }
    };*/

    public BluelightFilterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_bluelight_filter_switch);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        mEnabler = new BluelightFilterSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_bluelight_filter);
        mEnabler.setSwitch(mSwitch);
        mSwitch.setVisibility(View.VISIBLE);

        /*mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), true,
                mBluelight_Switch_Observer);*/
        return view;
    }
    
    /*private void onSwitchChanged() {
    	int switch_on = Settings.System.getInt(mContext.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
    	mSwitch.setChecked(switch_on==1);
    }*/
}

// xuyi@wind-mobi.com 20160525 add for BlueLightFilter end
