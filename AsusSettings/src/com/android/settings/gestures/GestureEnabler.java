/*
 * Filename : GestureEnabler.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

public final class GestureEnabler implements SwitchBar.OnSwitchChangeListener {
    private static final String TAG = "GestureEnabler";
    private Context mContext;
    private GesturesSettings mCont;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mValidListener;

    public GestureEnabler(Context context, SwitchBar switchBar, GesturesSettings cont) {
        mContext = context;
        mCont = cont;
        mSwitchBar = switchBar;
        mSwitch = switchBar.getSwitch();
        mValidListener = false;
    }

    public void setupSwitchBar() {
        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        mSwitchBar.hide();
    }

    public void resume(Context context) {
        if (mContext != context) {
            mContext = context;
        }
        handleStateChanged(getGestureState());
        mSwitchBar.addOnSwitchChangeListener(this);
        mValidListener = true;
    }

    public void pause() {
        mSwitchBar.removeOnSwitchChangeListener(this);
        mValidListener = false;
    }

    void handleStateChanged(boolean enabled) {
        setChecked(enabled);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        saveGestureState(isChecked);
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitch.isChecked()) {
            if (mValidListener) {
                mSwitchBar.removeOnSwitchChangeListener(this);
            }
            mSwitch.setChecked(isChecked);
            if (mValidListener) {
                mSwitchBar.addOnSwitchChangeListener(this);
            }
        }
    }

    private boolean getGestureState() {
        boolean state = Utils.isGestureEnabled(mContext.getContentResolver(), GesturesSettings.DESTURES_ENABLED);
        return state;
    }

    private void saveGestureState(boolean enabled) {
        Utils.enableGesture(mContext, GesturesSettings.DESTURES_ENABLED, enabled);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Log.d(TAG, "onSwitchChanged to " + isChecked);

        if (isChecked) {
            // Reset switch to off
            switchView.setChecked(true);
            mCont.setGesturesEnabled(true);
        } else {
            switchView.setChecked(false);
            mCont.setGesturesEnabled(false);
        }
    }
}
