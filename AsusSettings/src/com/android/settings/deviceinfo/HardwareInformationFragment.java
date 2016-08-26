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

package com.android.settings.deviceinfo;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.firmware.FirmwareManager;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.util.RunScript;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// sunhuihui@wind-mobi.com Feature#100649 16-4-14 begin
// sunhuihui@wind-mobi.com Feature#100649 16-4-14 end

public class HardwareInformationFragment extends SettingsPreferenceFragment implements Indexable,OnPreferenceClickListener {

    private static final String LOG_TAG = "HardwareInformationFragment";

    protected static final String KEY_DOCK_VERSION = "dock_version";
    protected static final String KEY_PAD_EC_VERSION = "pad_ec_version";
    protected static final String KEY_CAMERA_VERSION = "camera_version";
    protected static final String KEY_SCALAR_VERSION = "scalar_version";
    protected static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    protected static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    protected static final String KEY_TOUCH_VERSION = "touch_version";
    protected static final String KEY_BATTERY_VERSION = "battery_version";
    protected static final String KEY_PF_STATION_PAD_EC_VERSION = "padfone_station_pad_ec_version";
    protected static final String KEY_PF_STATION_TOUCH_VERSION = "padfone_station_touch_version";
    protected static final String KEY_PF_STATION_CAMERA_VERSION = "padfone_station_camera_version";
    protected static final String KEY_STYLUS_VERSION = "stylus_version";
    protected static final String KEY_WIFI_VERSION = "wifi_version";
    protected static final String KEY_GPS_VERSION = "gps_version";
    protected static final String KEY_BLUETOOTH_VERSION = "bt_version";
    protected static final String KEY_IAFW_VERSION = "iafw_version";
    protected static final String KEY_KBCFW_VERSION = "kbcfw_version";
    protected static final String KEY_CPU_FREQENCY = "cpu_frequency";
    protected static final String KEY_MEMORY_SIZE = "memory_size";
    private static final String PROPERTY_CPU_FREQUENCY = "ro.cpufreq";
    private static final String PROPERTY_MEMORY_SIZE = "ro.memsize";

    private static final int MSG_PFS_FW_STATE = 0;

    static final int TAPS_TO_BE_A_DEVELOPER = 7;


    //Porting from servicecommon +++
    /**
     * Broadcast Action:  A sticky broadcast for changes in the physical
     * paddock state of the device.
     *
     * <p>The intent will have the following extra values:
     * <ul>
     *   <li><em>{@link #EXTRA_PADDOCK_STATE}</em> - the current paddock
     *       state, indicating which paddock the device is physically in.</li>
     * </ul>
     * <p>This is intended for monitoring the current physical paddock state.
     * @hide
     */
    public static final String ACTION_PADDOCK_EVENT =
            "android.intent.action.PADDOCK_EVENT";

    /**
     * Used as an int extra field in {@link android.content.Intent#ACTION_PADDOCK_EVENT}
     * intents to request the paddock state.  Possible values are
     * EXTRA_PADDOCK_STATE_HDMI_ADD
     * EXTRA_PADDOCK_STATE_HDMI_REMOVE
     * @hide
     */
    public static final String EXTRA_PADDOCK_STATE = "android.intent.extra.PADDOCK_STATE";

    /**
     * Used as an int value for {@link android.content.Intent#EXTRA_PADDOCK_STATE}
     * to represent that the device is not in a paddock
     * @hide
     */
    public static final int EXTRA_PADDOCK_STATE_HDMI_REMOVE = 0;

    /**
     * Used as an int value for {@link android.content.Intent#EXTRA_PADDOCK_STATE}
     * to represent that the device is in a paddock
     * @hide
     */
    public static final int EXTRA_PADDOCK_STATE_HDMI_ADD = 1;
    //Porting from servicecommon ---

//    long[] mHits = new long[3];
//    int mDevHitCountdown;
//    Toast mDevHitToast;

    //+++Ken1_yu
    private boolean mHasGpsFeature;
    private boolean mHasWifiFeature;
    private boolean mHasBtFeature;
    //---

    long mFWPrefHitTimeMillis = 0L;
    int mFWPrefHitType = -1;
    int mFWSuccessHits = 0;

    //+++Mist Liao
    private boolean mHasDockECFeature = false;
    //---

    //+++Jim3 Chen
    private boolean mHasPFStationPadECFeature= false;
    private boolean mHasPFStationTouchFeature= false;
    private boolean mHasPFStationCameraFeature= false;

    private boolean mHasPadECVerFile = false;
    private boolean mHasCameraVerFile = false;
    private boolean mHasTouchVerFile = false;
    private boolean mHasScalarVerFile = false;
    //---

    // +++Tony Hsu
    private boolean mHasStylusVersionFile = false;
    private boolean mHasIAfwVersionFile = false;
    private boolean mHasKBCfwVersionFile = false;

    private int mRetryCount = 0;
    // ---

    private boolean mHasBatteryVerFile = false;

    private int mDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    private int mPFStationState = EXTRA_PADDOCK_STATE_HDMI_REMOVE;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_hardware);

        PackageManager pm = getPackageManager();


        //+++Mist Liao
//        mHasDockECFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_FIRMWARE_DOCK_EC);
        //---

        //+++Jim3 Chen
//        mHasPFStationPadECFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_FIRMWARE_PADFONE_STATION_PAD_EC);
//        mHasPFStationTouchFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_FIRMWARE_PADFONE_STATION_TOUCH);
//        mHasPFStationCameraFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_FIRMWARE_PADFONE_STATION_CAMERA_ISP);

        //comment out for porting+++
        mHasPadECVerFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_PAD);
        mHasCameraVerFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_CAMERA);
        mHasTouchVerFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_TOUCH);
        mHasScalarVerFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_SCALAR);
        //---

        // +++Tony Hsu
        mHasStylusVersionFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_STYLUS);
        mHasIAfwVersionFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_IAFW);
        mHasKBCfwVersionFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_KBCFW);
        // ---

        mHasBatteryVerFile = FirmwareManager.canShowFirmwareVersion(FirmwareManager.TYPE_BATTERY);

        setValueSummary(KEY_WIFI_VERSION, "wifi.version.driver");
        setValueSummary(KEY_BLUETOOTH_VERSION, "bt.version.driver");
        setValueSummary(KEY_GPS_VERSION, "gps.version.driver");

        //+++ Ken1_Yu
        // sunhuihui@wind-mobi.com modify begin 2016/5/12
        setPropertySummary(getPreferenceScreen(), KEY_CPU_FREQENCY,
                PROPERTY_CPU_FREQUENCY);
        // findPreference(KEY_CPU_FREQENCY).setSummary(getMaxCpuFreq());
        // sunhuihui@wind-mobi.com modify end  2016/5/12
        // sunhuihui@wind-mobi.com Feature#100649 16-4-14 begin
        // setPropertySummary(getPreferenceScreen(), KEY_MEMORY_SIZE, PROPERTY_MEMORY_SIZE);
        setStringSummary(KEY_MEMORY_SIZE, getMemoryInfo());
        // sunhuihui@wind-mobi.com Feature#100649 16-4-14 end

        //--
        // Remove Equipment id preference if FCC ID is not set by RIL
        setPropertySummary(getPreferenceScreen(), KEY_EQUIPMENT_ID,
                PROPERTY_EQUIPMENT_ID);

        findPreference(KEY_CAMERA_VERSION).setEnabled(true);
        findPreference(KEY_PAD_EC_VERSION).setEnabled(true);
        findPreference(KEY_TOUCH_VERSION).setEnabled(true);
        findPreference(KEY_BATTERY_VERSION).setEnabled(true);
        findPreference(KEY_DOCK_VERSION).setEnabled(true);
        findPreference(KEY_SCALAR_VERSION).setEnabled(true);
        findPreference(KEY_PF_STATION_PAD_EC_VERSION).setEnabled(true);
        findPreference(KEY_PF_STATION_TOUCH_VERSION).setEnabled(true);
        findPreference(KEY_PF_STATION_CAMERA_VERSION).setEnabled(true);

        // if(!mHasTouchVerFile)
        // getPreferenceScreen().removePreference(findPreference(KEY_TOUCH_VERSION));

        if(!mHasBatteryVerFile)
            getPreferenceScreen().removePreference(findPreference(KEY_BATTERY_VERSION));

        if (!mHasDockECFeature)
            getPreferenceScreen().removePreference(findPreference(KEY_DOCK_VERSION));

        if (!mHasPadECVerFile)
            getPreferenceScreen().removePreference(findPreference(KEY_PAD_EC_VERSION));

        if (!mHasCameraVerFile)
            getPreferenceScreen().removePreference(findPreference(KEY_CAMERA_VERSION));

        if (!mHasScalarVerFile)
            getPreferenceScreen().removePreference(findPreference(KEY_SCALAR_VERSION));

        if (!mHasPFStationPadECFeature)
            getPreferenceScreen().removePreference(findPreference(KEY_PF_STATION_PAD_EC_VERSION));

        if (!mHasPFStationTouchFeature)
            getPreferenceScreen().removePreference(findPreference(KEY_PF_STATION_TOUCH_VERSION));

        if (!mHasPFStationCameraFeature)
            getPreferenceScreen().removePreference(findPreference(KEY_PF_STATION_CAMERA_VERSION));

        // Ken +++ Checking Gps,wifi,bluetooth Feature to remove preference.
        mHasGpsFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        if (!mHasGpsFeature && findPreference(KEY_GPS_VERSION)!=null) {
            getPreferenceScreen().removePreference(findPreference(KEY_GPS_VERSION));
        }

        mHasWifiFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
        if (!mHasWifiFeature && findPreference(KEY_WIFI_VERSION)!=null) {
            getPreferenceScreen().removePreference(findPreference(KEY_WIFI_VERSION));
        }

        mHasBtFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        if (!mHasBtFeature && findPreference(KEY_BLUETOOTH_VERSION)!=null) {
            getPreferenceScreen().removePreference(findPreference(KEY_BLUETOOTH_VERSION));
        }
        // ken ---

        // +++Tony Hsu
        if (!mHasStylusVersionFile) {
            getPreferenceScreen().removePreference(findPreference(KEY_STYLUS_VERSION));
        } else {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_STYLUS);
            if (!TextUtils.isEmpty(version)) {
                setStringSummary(KEY_STYLUS_VERSION, version);
            } else {
                setStringSummary(KEY_STYLUS_VERSION,
                        getResources().getString(R.string.device_info_default));
            }
        }

        if (!mHasIAfwVersionFile) {
            getPreferenceScreen().removePreference(findPreference(KEY_IAFW_VERSION));
        } else {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_IAFW);
            if (!TextUtils.isEmpty(version)) {
                setStringSummary(KEY_IAFW_VERSION, version);
            } else {
                setStringSummary(KEY_IAFW_VERSION,
                        getResources().getString(R.string.device_info_default));
            }
        }

        if (!mHasKBCfwVersionFile) {
            getPreferenceScreen().removePreference(findPreference(KEY_KBCFW_VERSION));
        } else {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_KBCFW);
            if (!TextUtils.isEmpty(version)) {
                setStringSummary(KEY_KBCFW_VERSION, version);
            } else {
                setStringSummary(KEY_KBCFW_VERSION,
                        getResources().getString(R.string.device_info_default));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mHasTouchVerFile) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_TOUCH);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_TOUCH_VERSION, version);
            } else {
                setStringSummary(KEY_TOUCH_VERSION, getResources().getString(R.string.dock_not_available));
            }
        } else {
            // sunhuihui@wind-mobi.com Feature#100649 16-4-14 begin
            setStringSummary(KEY_TOUCH_VERSION, getTpVersion());
            // sunhuihui@wind-mobi.com Feature#100649 16-4-14 end
        }

        if (mHasDockECFeature) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_DOCK);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_DOCK_VERSION, version);
            } else {
                setStringSummary(KEY_DOCK_VERSION, getResources().getString(R.string.dock_not_available));
            }
        }

        if (mHasPadECVerFile) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_PAD);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_PAD_EC_VERSION, version);
            } else {
                setStringSummary(KEY_PAD_EC_VERSION, getResources().getString(R.string.device_info_default));
            }
        }

        if (mHasCameraVerFile) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_CAMERA);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_CAMERA_VERSION, version);
            } else {
                setStringSummary(KEY_CAMERA_VERSION, getResources().getString(R.string.device_info_default));
            }
        }


        // Register dock intent when has keyboard feature
        if (mHasDockECFeature) {
            // Register for dock events
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_DOCK_EVENT);
            getActivity().registerReceiver(mDockReceiver, filter);
        }

        if (mHasScalarVerFile) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_SCALAR);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_SCALAR_VERSION, version);
            } else {
                setStringSummary(KEY_SCALAR_VERSION, getResources().getString(R.string.device_info_default));
            }
        }

        if (mHasPFStationPadECFeature || mHasPFStationTouchFeature || mHasPFStationCameraFeature) {
            Log.d(LOG_TAG, "Register PADDOCK state");
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PADDOCK_EVENT);
            Intent intent = getActivity().registerReceiver(mPFStationReceiver, filter);
            if (intent != null) {
                mPFStationState = intent.getIntExtra(EXTRA_PADDOCK_STATE,
                        EXTRA_PADDOCK_STATE_HDMI_REMOVE);
            }
        }


        if (mHasPFStationPadECFeature) {
            if (mPFStationState == EXTRA_PADDOCK_STATE_HDMI_ADD) {
                String version = FirmwareManager
                        .getFirmwareVersion(FirmwareManager.TYPE_PFS_PAD_EC);
                if (!TextUtils.isEmpty(version)) {
                    setStringSummary(KEY_PF_STATION_PAD_EC_VERSION, version);
                }
            } else {
                setStringSummary(KEY_PF_STATION_PAD_EC_VERSION,
                        getResources().getString(R.string.dock_not_available));
            }
        }

        if (mHasPFStationTouchFeature) {
            if (mPFStationState == EXTRA_PADDOCK_STATE_HDMI_ADD) {
                String version = FirmwareManager
                        .getFirmwareVersion(FirmwareManager.TYPE_PFS_TOUCH);
                if (!TextUtils.isEmpty(version)) {
                    setStringSummary(KEY_PF_STATION_TOUCH_VERSION, version);
                }
            } else {
                setStringSummary(KEY_PF_STATION_TOUCH_VERSION,
                        getResources().getString(R.string.dock_not_available));
            }
        }

        if (mHasPFStationCameraFeature) {
            if (mPFStationState == EXTRA_PADDOCK_STATE_HDMI_ADD) {
                String version = FirmwareManager
                        .getFirmwareVersion(FirmwareManager.TYPE_PFS_CAMERA);
                if (!TextUtils.isEmpty(version)) {
                    setStringSummary(KEY_PF_STATION_CAMERA_VERSION, version);
                }
            } else {
                setStringSummary(KEY_PF_STATION_CAMERA_VERSION,
                        getResources().getString(R.string.dock_not_available));
            }
        }

        if(mHasBatteryVerFile) {
            String version = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_BATTERY);
            if (version != null && !TextUtils.isEmpty(version)) {
                setStringSummary(KEY_BATTERY_VERSION, version);
            } else {
                setStringSummary(KEY_BATTERY_VERSION, getResources().getString(R.string.device_info_default));
            }
        }

        // Register for firmware version intent
        IntentFilter filter = new IntentFilter();
        filter.addAction(FirmwareManager.ACTION_VERSION_CHANGE);
        getActivity().registerReceiver(mFirmwareVersionReceiver, filter);


        //+++ Jim3 Chen
        // Reset FWPref press relative every time the activity resumed.
        mFWPrefHitTimeMillis = 0L;
        mFWPrefHitType = -1;
        mFWSuccessHits = 0;

    }

    @Override
    public void onPause() {
        super.onPause();
        // Only display EC version on device which has keyboard feature
        if (mHasDockECFeature) {
            getActivity().unregisterReceiver(mDockReceiver);
        }

        if (mHasPFStationPadECFeature || mHasPFStationTouchFeature || mHasPFStationCameraFeature) {
            mPFSHandler.removeMessages(MSG_PFS_FW_STATE);
            getActivity().unregisterReceiver(mPFStationReceiver);
        }

        // Unregister receiver
        getActivity().unregisterReceiver(mFirmwareVersionReceiver);
    }

    private final BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve current sticky dock event broadcast.
            mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                    Intent.EXTRA_DOCK_STATE_UNDOCKED);
            String dockname = FirmwareManager.getFirmwareVersion(FirmwareManager.TYPE_DOCK);
            Log.d(LOG_TAG, "Dock State:" + mDockState + " Dock Version:" + dockname);
            if (dockname != null) {
                // && (mDockState == Intent.EXTRA_DOCK_STATE_KEYBOARD)) {
                setStringSummary(KEY_DOCK_VERSION, dockname);
            } else {
                setStringSummary(KEY_DOCK_VERSION,
                        getResources().getString(R.string.dock_not_available));
            }
        }
    };

    private final BroadcastReceiver mPFStationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPFStationState = intent.getIntExtra(EXTRA_PADDOCK_STATE,
                    EXTRA_PADDOCK_STATE_HDMI_REMOVE);
            if (mPFStationState == EXTRA_PADDOCK_STATE_HDMI_ADD) {
                Log.d(LOG_TAG, "Plug in PFS");
                mPFSHandler.sendEmptyMessageDelayed(MSG_PFS_FW_STATE, 2000);
            } else {
                Log.d(LOG_TAG, "Out of PFS");
                if (mHasPFStationPadECFeature) {
                    setStringSummary(KEY_PF_STATION_PAD_EC_VERSION,
                            getString(R.string.dock_not_available));
                }
                if (mHasPFStationTouchFeature) {
                    setStringSummary(KEY_PF_STATION_TOUCH_VERSION,
                            getString(R.string.dock_not_available));
                }
                if (mHasPFStationCameraFeature) {
                    setStringSummary(KEY_PF_STATION_CAMERA_VERSION,
                            getString(R.string.dock_not_available));
                }
            }
        }
    };

    private void updateFirmwareVersion(Intent intent) {
        int type = intent.getIntExtra(FirmwareManager.EXTRA_TYPE, 0);
        String version = intent.getStringExtra(FirmwareManager.EXTRA_VERSION);
        Log.d("FirmwareManagerService", "type:" + type + " version:" + version);
        if (type == FirmwareManager.TYPE_DOCK) {
            setStringSummary(KEY_DOCK_VERSION, version);
        } else if (mHasPadECVerFile && type == FirmwareManager.TYPE_PAD) {
            setStringSummary(KEY_PAD_EC_VERSION, version);
        } else if (mHasCameraVerFile && type == FirmwareManager.TYPE_CAMERA) {
            setStringSummary(KEY_CAMERA_VERSION, version);
        } else if (mHasScalarVerFile && type == FirmwareManager.TYPE_SCALAR) {
            setStringSummary(KEY_SCALAR_VERSION, version);
        } else if (mHasPFStationPadECFeature && type == FirmwareManager.TYPE_PFS_PAD_EC) {
            setStringSummary(KEY_PF_STATION_PAD_EC_VERSION, version);
        } else if (mHasPFStationTouchFeature && type == FirmwareManager.TYPE_PFS_TOUCH) {
            setStringSummary(KEY_PF_STATION_TOUCH_VERSION, version);
        } else if (mHasPFStationCameraFeature && type == FirmwareManager.TYPE_PFS_CAMERA) {
            setStringSummary(KEY_PF_STATION_CAMERA_VERSION, version);
        }
    }

    private final BroadcastReceiver mFirmwareVersionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateFirmwareVersion(intent);
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_PAD_EC_VERSION)) {
            if (mFWPrefHitType != 0) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 0;
            updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_DOCK_VERSION)) {
            if (mFWPrefHitType != 1) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 1;
            updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_CAMERA_VERSION)) {
            if (mFWPrefHitType != 2) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 2;
            updateFWPrefHit();
          } else if (preference.getKey().equals(KEY_TOUCH_VERSION)) {
              if (mFWPrefHitType != 3) {
                  mFWSuccessHits = 0;
                  mFWPrefHitTimeMillis = 0L;
              }
              mFWPrefHitType = 3;
              updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_SCALAR_VERSION)) {
            if (mFWPrefHitType != 3) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 3;
            updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_PF_STATION_PAD_EC_VERSION)) {
            if (mFWPrefHitType != 3) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 3;
            updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_PF_STATION_TOUCH_VERSION)) {
            if (mFWPrefHitType != 3) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 3;
            updateFWPrefHit();
        } else if (preference.getKey().equals(KEY_PF_STATION_CAMERA_VERSION)) {
            if (mFWPrefHitType != 3) {
                mFWSuccessHits = 0;
                mFWPrefHitTimeMillis = 0L;
            }
            mFWPrefHitType = 3;
            updateFWPrefHit();
        }

        if (mFWPrefHitType != -1 && mFWSuccessHits > 5) {
            mFWPrefHitType = -1;
            mFWSuccessHits = 0;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateFWPrefHit() {
        long hitTime  = SystemClock.uptimeMillis();
        if ((hitTime - mFWPrefHitTimeMillis) < 300) {
            mFWSuccessHits++;
        } else {
            mFWSuccessHits = 1;
        }
        mFWPrefHitTimeMillis = hitTime;
    }

    private void setStringSummary(String preference, String value) {
        try {
            if (findPreference(preference) != null)
                findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            if (findPreference(preference) != null)
                findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private Handler mPFSHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PFS_FW_STATE:
                    Log.d(LOG_TAG, "msg.what: " + msg.what);
                    String pfsPadEC = FirmwareManager
                            .getFirmwareVersion(FirmwareManager.TYPE_PFS_PAD_EC);
                    String pfsTouch = FirmwareManager
                            .getFirmwareVersion(FirmwareManager.TYPE_PFS_TOUCH);
                    String pfsCamera = FirmwareManager
                            .getFirmwareVersion(FirmwareManager.TYPE_PFS_CAMERA);

                    Log.d(LOG_TAG, "PFStation State:" + mPFStationState
                            + " PFStation Pad EC Version:" + pfsPadEC
                            + " PFStation Touch Version:" + pfsTouch
                            + " PFStation Camera Version:" + pfsCamera);

                    if ((mHasPFStationPadECFeature && TextUtils.isEmpty(pfsPadEC))
                            || (mHasPFStationTouchFeature && TextUtils.isEmpty(pfsTouch))
                            || (mHasPFStationCameraFeature && TextUtils.isEmpty(pfsCamera))) {
                        if (mRetryCount < 3) {
                            Log.d(LOG_TAG, "One of the PFS fw version get fail at "
                                    + mRetryCount + 1
                                    + "round");
                            mRetryCount += 1;
                            mPFSHandler.sendEmptyMessageDelayed(MSG_PFS_FW_STATE, 2000);
                        }
                    } else {
                        mRetryCount = 0;

                        if (mHasPFStationPadECFeature) {
                            setStringSummary(KEY_PF_STATION_PAD_EC_VERSION, pfsPadEC);
                        }
                        if (mHasPFStationTouchFeature) {
                            setStringSummary(KEY_PF_STATION_TOUCH_VERSION, pfsTouch);
                        }
                        if (mHasPFStationCameraFeature) {
                            setStringSummary(KEY_PF_STATION_CAMERA_VERSION, pfsCamera);
                        }
                    }
                    break;
            }
        }
    };

    private void setPropertySummary(PreferenceGroup preferenceGroup,
            String preference, String property) {
        if (findPreference(preference) == null) return;
        String property_content = SystemProperties.get(property);
        if (TextUtils.isEmpty(property_content)) {
            // Property is missing so remove preference from group
            try {
                    preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        } else {
            findPreference(preference).setSummary(property_content);
        }
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();

                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }

                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
    };

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.DEVICEINFO;
    }

    // sunhuihui@wind-mobi.com Feature#100649 16-4-14 begin
    private String getMemoryInfo() {
        String memProperty = SystemProperties.get(PROPERTY_MEMORY_SIZE);
        if (!TextUtils.isEmpty(memProperty)) {
            return memProperty;
        }
        String strMeminfo = RunScript.runIt("cat /proc/meminfo");
        int ramSize = 0;
        final double SIZE_1G_BY_KB = 1024 * 1024;
        if (!TextUtils.isEmpty(strMeminfo)) {
            String[] lines = strMeminfo.split("\n");
            String[] t = lines[0].split("[\\s]+");
            ramSize = (int) Math.round(Long.parseLong(t[1]) / SIZE_1G_BY_KB);

            return ramSize + "GB";
        }
        return getResources().getString(R.string.device_info_default);
    }

    private String getTpVersion() {
        FileInputStream fis = null;
        byte[] bytes = new byte[256];
        int count;
        String tpType = "";
        try {
            fis = new FileInputStream("/sys/class/wind_device/device_info/ctp_info");
            count = fis.read(bytes);
            fis.close();
            tpType = new String(bytes, 0, count);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String[] arrays = tpType.split("fwvr:");
            tpType = arrays[arrays.length - 1];
        } catch (ArrayIndexOutOfBoundsException e) {
            tpType = getResources().getString(R.string.device_info_default);
        }
        return tpType;
    }

    private final static String CPUINFO_MAX_FREQFILEPATH = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";
    public static String getMaxCpuFreq() {
        int result = 0;
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(CPUINFO_MAX_FREQFILEPATH);
            br = new BufferedReader(fr);
            String text = br.readLine();
            result = Integer.parseInt(text.trim());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null)
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return String.format("%.2f%s",result/(1024*1024F),"GHz");
    }
    // sunhuihui@wind-mobi.com Feature#100649 16-4-14 end
}