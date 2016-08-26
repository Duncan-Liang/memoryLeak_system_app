// xuyi@wind-mobi.com 20160530 add for Asus brightness dialogue begin

/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import com.android.settings.util.ResCustomizeConfig;

import java.util.UUID;
import com.android.settings.utils.DeviceConf;

import com.android.settings.util.ResCustomizeConfig;    //+++ tsungching_lin: for binary release
import android.os.SystemProperties;

public class AsusBrightnessPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {
    private static final String TAG = "AsusBrightnessPreference";
    // If true, enables the use of the screen auto-brightness adjustment setting.
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT = true;
            //PowerManager.useScreenAutoBrightnessAdjustmentFeature();

    private final int mScreenBrightnessMinimum;
    private final int mScreenBrightnessMaximum;
    private final int mScreenBrightnessRange;

    private SeekBar mSeekBar;
    private ViewGroup mSeekBarText;
    private CheckBox mCheckBox;

    private int mOldBrightness;
    private int mOldAutomatic;

    private boolean mAutomaticAvailable;
    private boolean mAutomaticMode;

    private int mCurBrightness = -1;

    private boolean mRestoredOldState;
    private boolean isBrightnessDialogTouched = false;

    private static int mShowDialogOrientation;
    private static int mSaveInstanceOrientation;
    private static boolean isScreenRoation = false;
    private Configuration mConfiguration;
    private IPowerManager mPower;
    private boolean mTempAutoMode = false;
    private boolean mIsTracking = false;



    //+++ sheng-en_fann@asus.com, dynamic gain array setting
    private final float DEFAULT_GAIN = 1.0f;
    private final float[] DEFAULT_GAIN_ARRAY = {0.4f, 0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f};
    private float[] GAIN_ARRAY = DEFAULT_GAIN_ARRAY;
    private float mGain;
    private float mOldGain;
    private static DeviceConf mDeviceConf = new DeviceConf("brightness");
    private static final String KEY_GAIN_ARRAY = "gainArray";
    //---
    //+++ tsungching_lin@asus.com: BL_isIndorrSaving
    private boolean mbIsIndoorSaving = false;
    private int THRESHOLD_HIGH = SystemProperties.getInt("persist.asus.lux_threshold_h", 3000);
    private int THRESHOLD_LOW = SystemProperties.getInt("persist.asus.lux_threshold_l", 2000);
    private float RATIO = SystemProperties.getInt("persist.asus.bri_ratio", 80) * 1.0f / 100;
    private boolean mOutdoor = false;
    //--- tsungching_lin@asus.com: BL_isIndorrSaving

    // Backlight range is from 0 - 255. Need to make sure that user
    // doesn't set the backlight to 0 and get stuck  56     // doesn't set the backlight to 0 and get stuck
    private int mScreenBrightnessDim =
                    getContext().getResources().getInteger(ResCustomizeConfig.getIdentifier(getContext(), "integer", "config_screenBrightnessDim"));
    private static final int MINIMUM_BACKLIGHT_OFFSET = 10;
    private static final int MINIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_OFF + MINIMUM_BACKLIGHT_OFFSET;
    private static final int MAXIMUM_BACKLIGHT = android.os.PowerManager.BRIGHTNESS_ON;

    private static final int SEEK_BAR_RANGE =  MAXIMUM_BACKLIGHT -MINIMUM_BACKLIGHT;

    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mCurBrightness = -1;
            onBrightnessChanged();
        }
    };

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };

    private ContentObserver mAutoBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            float gain = Settings.System.getFloat(getContext().getContentResolver(), Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, DEFAULT_GAIN);
            if(gain != mGain) {
                onAutoBrightnessChanged();
            }
        }
    };

    public AsusBrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mbIsIndoorSaving = ResCustomizeConfig.getBooleanConfig("BL_isIndoorSaving", false);
        Log.d("BrightnessPreference", "BrightnessPreference mbIsIndoorSaving="+mbIsIndoorSaving);
        //+++ sheng-en_fann@asus.com, dynamic gain array setting
        try {
            GAIN_ARRAY = mDeviceConf.getFloatArray(KEY_GAIN_ARRAY, DEFAULT_GAIN_ARRAY);
        }
        catch(Exception e) {
            GAIN_ARRAY = DEFAULT_GAIN_ARRAY;
        }
        //---

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mScreenBrightnessMinimum = pm.getMinimumScreenBrightnessSetting();
        mScreenBrightnessMaximum = pm.getMaximumScreenBrightnessSetting();
        mScreenBrightnessRange = mScreenBrightnessMaximum - mScreenBrightnessMinimum;

        PackageManager pgm = context.getPackageManager();
        mAutomaticAvailable = pgm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        setDialogLayoutResource(R.layout.preference_dialog_brightness);
        //setDialogIcon(R.drawable.ic_settings_display_l); +++ sheng-en_fann@asus.com
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mBrightnessModeObserver);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ), true,
                mAutoBrightnessObserver);

        mConfiguration = this.getContext().getResources().getConfiguration();
        mShowDialogOrientation = mConfiguration.orientation;

        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mCurBrightness = -1;
        //mSeekBar = getSeekBar(view);
        mSeekBar = (SeekBar)view.findViewById(R.id.seekbar);
        mSeekBar.setMax(SEEK_BAR_RANGE);
        mOldBrightness = getBrightness();
        mSeekBar.setProgress(mOldBrightness);
        mSeekBarText = (ViewGroup)view.findViewById(R.id.seekbar_text);

        mCheckBox = (CheckBox) view.findViewById(R.id.automatic_mode);
        PackageManager pm = this.getContext().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT)) {
            mCheckBox.setVisibility(View.GONE);
            mSeekBar.setEnabled(true);
        } else if (mAutomaticAvailable) {
            mCheckBox.setOnCheckedChangeListener(this);
            mOldAutomatic = getBrightnessMode(0);
            mAutomaticMode = mOldAutomatic == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

            mOldGain = getAutoBrightnessGain();
            mGain = mOldGain;
            /*try {
                mOldGain = mPower.getAutoBrightnessGain();
                mGain = mOldGain;
            }
            catch(RemoteException e) {
                mOldGain = DEFAULT_GAIN;
                mGain = DEFAULT_GAIN;
            }*/

            if(mAutomaticMode) registerAutoBrightness(mAutomaticMode);
            mCheckBox.setChecked(mAutomaticMode);
            //mSeekBar.setEnabled(!mAutomaticMode
            //        || USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        } else {
            mSeekBar.setEnabled(true);
        }
        mSeekBar.setOnSeekBarChangeListener(this);

        if (mbIsIndoorSaving) {
            if(!mAutomaticMode) {
                startGetLux();
            }
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        if(!mIsTracking) return;
        if(mAutomaticMode) {
            try {
                if(progress >= 0 && progress < GAIN_ARRAY.length) {
                    mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(GAIN_ARRAY[progress]);
                } else {
                    Log.d("BrightnessPreference", "onProgressChanged progress="+progress+" index out of range.");
                }
            }
            catch(RemoteException e) {}
        } else {
            setBrightness(progress, false);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsTracking = true;
        mTempAutoMode = mAutomaticMode;
        if (mAutomaticMode) {
        /*
            try {
                mPower.pauseAutoBrightness();
            }
            catch(RemoteException e) {}
            */
        }
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
        isBrightnessDialogTouched = true;
        if(mAutomaticMode && mIsTracking) {
            int progress = seekBar.getProgress();
            if(progress >= 0 && progress < GAIN_ARRAY.length) {
                setAutoBrightnessGain(GAIN_ARRAY[progress]);
            } else {
                Log.d("BrightnessPreference", "onStopTrackingTouch progress="+progress+" index out of range.");
            }
            /*try {
                if(progress >= 0 && progress < GAIN_ARRAY.length) {
                    mPower.setAutoBrightnessGain(GAIN_ARRAY[progress]);
                } else {
                    Log.d("BrightnessPreference", "onStopTrackingTouch progress="+progress+" index out of range.");
                }
            }
            catch(RemoteException e) {}*/
        } else {
            setBrightnessOnStopTrackingTouch(seekBar);
        }
        mIsTracking = false;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        registerAutoBrightness(isChecked);
        if(!isChecked) {
            mCurBrightness = -1;
            mSeekBar.setProgress(getBrightness());
            //mSeekBar.setVisibility(!isChecked? View.VISIBLE : View.GONE);
            //mSeekBarText.setVisibility(!isChecked? View.VISIBLE : View.GONE);
            setBrightness(mSeekBar.getProgress(), false);
        }
    }

    private void setBrightnessOnStopTrackingTouch(SeekBar seekBar){        
        setBrightness(seekBar.getProgress(),true);
    }

    private void putBrightnessSetting(int brightness){
        final ContentResolver resolver = getContext().getContentResolver();
        //brightness = (brightness * range)/SEEK_BAR_RANGE + mScreenBrightnessMinimum;
        boolean bIsIndoorProperty = SystemProperties.getInt("persist.asus.inoutdoor", 1) == 1;
        if (mbIsIndoorSaving && bIsIndoorProperty) {
            if(mOutdoor && brightness == MAXIMUM_BACKLIGHT) {
                brightness = MAXIMUM_BACKLIGHT;
            } else {
                brightness = (int)(Math.pow(((brightness - MINIMUM_BACKLIGHT_OFFSET) * 1.0 / SEEK_BAR_RANGE), 2.2) * mScreenBrightnessRange + mScreenBrightnessMinimum);
                brightness *= RATIO;
            }
        } else {  // normal case
            brightness = (int)(Math.pow((brightness * 1.0 / SEEK_BAR_RANGE), 2.2) * mScreenBrightnessRange + mScreenBrightnessMinimum);
        }

        Settings.System.putInt(resolver,Settings.System.SCREEN_BRIGHTNESS,brightness);
    }

    private int getBrightness() {
        int mode = getBrightnessMode(0);
        float brightness = 0;
        if (USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT
                && mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            brightness = Settings.System.getFloat(getContext().getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, DEFAULT_GAIN);
            brightness = (brightness+1)/2;
        } else {
            if (mCurBrightness < 0) {
                brightness = Settings.System.getInt(getContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 100);
            } else {
                brightness = mCurBrightness;
            }
            boolean bIsIndoorProperty = SystemProperties.getInt("persist.asus.inoutdoor", 1) == 1;
            if (mbIsIndoorSaving && bIsIndoorProperty) {
                if(mOutdoor && brightness == MAXIMUM_BACKLIGHT) {
                    brightness = 1;
                } else {
                    brightness /= RATIO;
                    brightness = (brightness - mScreenBrightnessMinimum) / mScreenBrightnessRange;
                    brightness = (float)Math.pow(brightness, 1.0 / 2.2);
                }
            } else {
                brightness = (brightness - mScreenBrightnessMinimum) / mScreenBrightnessRange;
                brightness = (float)Math.pow(brightness, 1.0 / 2.2);
            }
        }
        return (int)Math.ceil(brightness*SEEK_BAR_RANGE);
    }

    private int getBrightnessMode(int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private void onBrightnessChanged() {
         int brightness = getBrightness();
         mSeekBar.setProgress(brightness);
         if(!isBrightnessDialogTouched ){
             mOldBrightness = brightness;
             isBrightnessDialogTouched = false;
         }
    }

    private void onBrightnessModeChanged() {
        boolean checked = getBrightnessMode(0)
                == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        mCheckBox.setChecked(checked);
        if(!checked) {
            mSeekBar.setProgress(getBrightness());
            //mSeekBar.setEnabled(!checked || USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        }
    }

    private void onAutoBrightnessChanged() {
        mGain = getAutoBrightnessGain();
        /*try {
            mGain = mPower.getAutoBrightnessGain();
        }
        catch(RemoteException e) {
            mOldGain = DEFAULT_GAIN;
            mGain = DEFAULT_GAIN;
        }*/

        for(int i = 0; i < GAIN_ARRAY.length - 1; i++) {
                if(mGain >= GAIN_ARRAY[i] && mGain < GAIN_ARRAY[i + 1]) {
                    mSeekBar.setProgress(i);
                    break;
                }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = getContext().getContentResolver();

        if (positiveResult) {
            setBrightness(mSeekBar.getProgress(), true);
        } else {
           if(!isScreenRoation)restoreOldState();
        }
        isScreenRoation=false;
        resolver.unregisterContentObserver(mBrightnessObserver);
        resolver.unregisterContentObserver(mBrightnessModeObserver);
        resolver.unregisterContentObserver(mAutoBrightnessObserver);
        if (mbIsIndoorSaving) {
            endGetLux();
        }
    }

    private void restoreOldState() {
        if (mRestoredOldState) return;

        if (mAutomaticAvailable) {
            setMode(mOldAutomatic);
            setAutoBrightnessGain(mOldGain);
            /*try {
                mPower.setAutoBrightnessGain(mOldGain);
            }
            catch(RemoteException e) {}*/
        }
        if (!mAutomaticAvailable || mOldAutomatic == 0) {
            boolean bIsIndoorProperty = SystemProperties.getInt("persist.asus.inoutdoor", 1) == 1;
            if (mbIsIndoorSaving && bIsIndoorProperty) {
                putBrightnessSetting(mOldBrightness + MINIMUM_BACKLIGHT);
                //setBrightness(mOldBrightness, false);
            } else {
                putBrightnessSetting(mOldBrightness);
                setBrightness(mOldBrightness, false);
            }
        }
        mRestoredOldState = true;
        mCurBrightness = -1;
    }

    private void setBrightness(int brightness, boolean write) {
        //brightness = (brightness * range)/SEEK_BAR_RANGE + mScreenBrightnessMinimum;
        if(mAutomaticMode) return; //+++ sheng-en_fann@asus.com, do not set brightness value to db when auto mode is on
        boolean bIsIndoorProperty = SystemProperties.getInt("persist.asus.inoutdoor", 1) == 1;
        if (mbIsIndoorSaving && bIsIndoorProperty) {
            if(mOutdoor && brightness == SEEK_BAR_RANGE) {
                brightness = MAXIMUM_BACKLIGHT;
            } else {
                brightness = (int)(Math.pow((brightness * 1.0 / SEEK_BAR_RANGE), 2.2) * mScreenBrightnessRange + mScreenBrightnessMinimum);
                brightness *= RATIO;
            }
        } else {
            brightness = (int)(Math.pow((brightness * 1.0 / SEEK_BAR_RANGE), 2.2) * mScreenBrightnessRange + mScreenBrightnessMinimum);
        }
        try {
            if (mPower != null) {
                mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
            }
            if (write) {
                mCurBrightness = -1;
                final ContentResolver resolver = getContext().getContentResolver();
                Settings.System.putInt(resolver,
                        Settings.System.SCREEN_BRIGHTNESS, brightness);
            } else {
                mCurBrightness = brightness;
                }
        } catch (RemoteException doe) {
        }
    }

    private void setMode(int mode) {
        mAutomaticMode = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
        if (mbIsIndoorSaving) {
            if(!mAutomaticMode) {
                startGetLux();
            } else {
                endGetLux();
            }
        }
    }

    private void registerAutoBrightness(boolean enabled) {
        if (enabled) {
            mSeekBar.setMax(GAIN_ARRAY.length - 1);
            boolean match = false;
            for(int i = 0; i < GAIN_ARRAY.length - 1; i++) {
                if(mGain >= GAIN_ARRAY[i] && mGain < GAIN_ARRAY[i + 1]) {
                    match = true;
                    mSeekBar.setProgress(i);
                    break;
                }
            }
            if(!match) {
                if(mGain < GAIN_ARRAY[0]) mSeekBar.setProgress(0);
                if(mGain >= GAIN_ARRAY[GAIN_ARRAY.length - 1]) mSeekBar.setProgress(GAIN_ARRAY.length - 1);
            }
        } else {
            mSeekBar.setMax(SEEK_BAR_RANGE);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.automatic = mCheckBox.isChecked();
        myState.progress = mSeekBar.getProgress();
        myState.oldAutomatic = mOldAutomatic == 1;
        myState.oldGain = mOldGain;
        myState.oldProgress = mOldBrightness;
        myState.curBrightness = mCurBrightness;

        mSaveInstanceOrientation = mConfiguration.orientation;
        isScreenRoation =(mSaveInstanceOrientation==mShowDialogOrientation)?false:true;

        // Restore the old state when the activity or dialog is being paused
       if((mSaveInstanceOrientation==mShowDialogOrientation))restoreOldState();
       getDialog().dismiss();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOldBrightness = myState.oldProgress;
        mOldAutomatic = myState.oldAutomatic ? 1 : 0;
        mOldGain = myState.oldGain;
        setMode(myState.automatic ? 1 : 0);
        setBrightness(myState.progress, false);
        mCurBrightness = myState.curBrightness;
        isScreenRoation = true;
    }

    private float getAutoBrightnessGain() {
        final ContentResolver resolver = getContext().getContentResolver();
        return Settings.System.getFloat(resolver, Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, DEFAULT_GAIN);
    }

    private void setAutoBrightnessGain(float gain) {
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putFloat(resolver, Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, gain);
    }

    private static class SavedState extends BaseSavedState {

        boolean automatic;
        boolean oldAutomatic;
        float oldGain;
        int progress;
        int oldProgress;
        int curBrightness;

        public SavedState(Parcel source) {
            super(source);
            automatic = source.readInt() == 1;
            progress = source.readInt();
            oldAutomatic = source.readInt() == 1;
            oldGain = source.readFloat();
            oldProgress = source.readInt();
            curBrightness = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(automatic ? 1 : 0);
            dest.writeInt(progress);
            dest.writeInt(oldAutomatic ? 1 : 0);
            dest.writeFloat(oldGain);
            dest.writeInt(oldProgress);
            dest.writeInt(curBrightness);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    //+++ tsungching_lin@asus.com: BL_isIndorrSaving
    private void startGetLux() {
        SensorManager sensorMgr = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorMgr.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorMgr.registerListener(lightSensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void endGetLux() {
        SensorManager sensorMgr = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        sensorMgr.unregisterListener(lightSensorEventListener);
    }

    SensorEventListener lightSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            boolean mTemp = mOutdoor;
            int brighness = getBrightness();
            if(!mTemp) {
                mTemp = (event.values[0] >= THRESHOLD_HIGH);
            } else {
                mTemp = !(event.values[0] <= THRESHOLD_LOW);
            }
            if(mOutdoor && !mTemp && brighness >= SEEK_BAR_RANGE) {
                //putBrightnessSetting();
                Settings.System.putInt(getContext().getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,(int)(MAXIMUM_BACKLIGHT * RATIO));
            }
            mOutdoor = mTemp;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
    //--- tsungching_lin@asus.com: BL_isIndorrSaving
}

// xuyi@wind-mobi.com 20160530 add for Asus brightness dialogue end

