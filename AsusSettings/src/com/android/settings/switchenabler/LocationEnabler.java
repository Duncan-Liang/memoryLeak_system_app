package com.android.settings.switchenabler;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

public class LocationEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener {

    private Context mContext;
    private Switch mSwitch;

    private boolean mStateMachineEvent;
    private boolean mHasGPSfeature;

    private final IntentFilter mIntentFilter;

    public LocationEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mIntentFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            handleStateChanged();
        }
    };

    @Override
    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) return;
        final ContentResolver cr = mContext.getContentResolver();
        mHasGPSfeature = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        if (!mHasGPSfeature) {
            Settings.Secure.setLocationProviderEnabled(cr,
                    LocationManager.GPS_PROVIDER, false);
        } else {
            Settings.Secure.setLocationProviderEnabled(cr,
                    LocationManager.GPS_PROVIDER, isChecked);
        }
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.NETWORK_PROVIDER, isChecked);
        handleStateChanged();
    }

    protected boolean handleStateChanged() {
        ContentResolver res = mContext.getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER);
        boolean checked = (gpsEnabled || networkEnabled);
        setSwitchChecked(checked);
        return checked;
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }
}
