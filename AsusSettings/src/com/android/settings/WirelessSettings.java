/*
 * Copyright (C) 2009 The Android Open Source Project
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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
// youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
import android.content.ComponentName;
import android.preference.Preference.OnPreferenceChangeListener;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import java.util.Collection;
// youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import com.mediatek.internal.telephony.cdma.CdmaFeatureOptionUtils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IRCSSettings;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.IWfcSettingsExt;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IRCSSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//liyong01@wind-mobi.com -s
import android.hardware.display.DisplayManager;
//liyong01@wind-mobi.com -e
// xiongshigui@wind-mobi.com 20160525 add begin
import com.android.settings.dms.DMSSettings;
// xiongshigui@wind-mobi.com 20160525 add end

// sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
import com.android.settings.ethernet.EthUtils;
import android.net.EthernetManager;
// sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 end
// M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings begin.
// --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting begin
import android.preference.PreferenceCategory;
// --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting end
// M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings end.

// youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
public class WirelessSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener,Indexable{
		// youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    private static final String TAG = "WirelessSettings";

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    /// M: Add MTK nfc seting @{
    private static final String KEY_MTK_TOGGLE_NFC = "toggle_mtk_nfc";
    private static final String ACTION_MTK_NFC = "mediatek.settings.NFC_SETTINGS";
    /// @}
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";
    private static final String KEY_MANAGE_MOBILE_PLAN = "manage_mobile_plan";
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
    private static final String KEY_SMS_APPLICATION = "sms_application";
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    private static final String KEY_TOGGLE_NSD = "toggle_nsd"; //network service discovery
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String KEY_WFC_SETTINGS = "wifi_calling_settings";

    //liyong01@wind-mobi.com -s
    private static final String KEY_PLAY_TO_SETTINGS = "playto_settings";
    //liyong01@wind-mobi.com -e

	// xiongshigui@wind-mobi.com 20160525 add begin
    // Wesley_Lee@asus.com
    private static final String KEY_DMS_SETTINGS = "dms_settings";
	// xiongshigui@wind-mobi.com 20160525 add end

    // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings begin.
    /** M: origin code , modified by cenxingcan@wind-mobi.com at 20160722 begin
    private static final String KEY_CELL_BROADCAST_CMAS_SETTINGS = "pref_key_gsm_umts_cmas";
    private static final String KEY_CELL_BROADCAST_SMSCB_SETTINGS = "pref_key_gsm_umts_smscb";
    M: origin code , modified by cenxingcan@wind-mobi.com at 20160722 end **/
    private static final String CMAS_COMPONENT = "com.android.cellbroadcastreceiver";
    private static final String CELL_BROADCAST_SMS_ACTIVITY = "com.android.cellbroadcastreceiver.GsmUmtsCellBroadcastSms";
    private static final String CELL_BROADCAST_SMSDUAL_ACTIVITY = "com.android.cellbroadcastreceiver.GsmUmtsCellBroadcastSmsDual";
    // +++ Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
    public static final String GSM_UMTS_CMAS  = "pref_key_gsm_umts_cmas";
    public static final String GSM_UMTS_SMSCB = "pref_key_gsm_umts_smscb";
    private Preference mGsmUmtsCMASPref;
    private Preference mGsmUmtsSMSCBPref;
    private PreferenceCategory mCMASPrefCategory;
    private PreferenceCategory mSMSCBPrefCategory;
    // --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
    // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings end.
	
    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private NfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private NsdEnabler mNsdEnabler;

    // sunhuihui@wind-mobi.com modify Feature#110139 Usb dialog patch. 2016/6/29 begin
    private Preference mEthernetPreference;
    private static final String KEY_ETHERNET_SETTINGS = "ethernet_settings";
    private EthernetManager mEm;
    // sunhuihui@wind-mobi.com modify Feature#110139 Usb dialog patch. 2016/6/29 end

    private ConnectivityManager mCm;
    private TelephonyManager mTm;
    private PackageManager mPm;
    private UserManager mUm;

    private static final int MANAGE_MOBILE_PLAN_DIALOG_ID = 1;
    private static final String SAVED_MANAGE_MOBILE_PLAN_MSG = "mManageMobilePlanMessage";

    private PreferenceScreen mButtonWfc;
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
    private AppListPreference mSmsApplicationPreference;
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    // liyong01@wind-mobi.com -s
    private static final int REMOTE_TARGET_STATE_NOT_CONNECTED = 0;
    private static final int REMOTE_TARGET_STATE_CONNECTING = 1;
    private static final int REMOTE_TARGET_STATE_CONNECTED = 2;
    private static final String ACTION_START_PLAYTO_SETTINGS = "com.asus.playto.action.PLAYTO_SETTINGS";
    private static final String ACTION_PLAYTO_STATUS_CHANGED = "com.asus.playto.action.PLAYTO_STATUS_CHANGED";
    private static final String KEY_ACTIVE_TARGET_STATUS = "active_target_status";
    private static final String KEY_ACTIVE_TARGET_FRIENDLY_NAME = "active_target_friendly_name";
    private static final String RES_PLAY_TO_APP_NAME = "app_name";
    private static final String RES_PLAY_TO_SUMMARY = "play_to_summary";
    private static final String RES_PLAY_TO_NOTIFICATION_CONNECTING_MESSAGE = "play_to_notification_connecting_message";
    private static final String RES_PLAY_TO_NOTIFICATION_CONNECTED_MESSAGE = "play_to_notification_connected_message";
    // liyong01@wind-mobi.com -e

    /// M: RCSE key&intent @{
    private static final String RCSE_SETTINGS_INTENT = "com.mediatek.rcse.RCSE_SETTINGS";
    private static final String KEY_RCSE_SETTINGS = "rcse_settings";
    /// @}

    /// M: Wfc plugin @{
    IWfcSettingsExt mWfcExt;
    /// @}


    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceFragment's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (preference == mAirplaneModePreference && Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode launch ECM app dialog
            startActivityForResult(
                new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                REQUEST_CODE_EXIT_ECM);
            return true;
        } else if (preference == findPreference(KEY_MANAGE_MOBILE_PLAN)) {
            onManageMobilePlanClick();
     //liyong01@wind-mobi.com -s
        }else if (preference == findPreference(KEY_PLAY_TO_SETTINGS)) {
            //liyong01@wind-mobi.com modify 20160615 -s
            Settings.Global.putInt( getContentResolver(), Settings.Global.WIFI_DISPLAY_ON, 1 );
           //liyong01@wind-mobi.com modify 20160615 -d
            Intent intent = new Intent(ACTION_START_PLAYTO_SETTINGS);
            intent.setPackage(DisplayManager.PLAYTO_PACKAGE_NAME);
            getActivity().startServiceAsUser(intent, UserHandle.CURRENT);
        }
      //liyong01@wind-mobi.com -e

        // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings begin.
        /** M: origin code , modified by cenxingcan@wind-mobi.com at 20160722 begin
        if(FeatureOption.WIND_DEF_ASUS_CELLBROADCAST){
            if (preference == findPreference(KEY_CELL_BROADCAST_CMAS_SETTINGS)
                    || preference == findPreference(KEY_CELL_BROADCAST_SMSCB_SETTINGS)) {
                startCBSettingActivity(getActivity(), preference.getKey());
            }
        }
		M: origin code , modified by cenxingcan@wind-mobi.com at 20160722 end **/
        if(FeatureOption.WIND_DEF_ASUS_CELLBROADCAST) {
            if (preference == mGsmUmtsCMASPref) {
                // +++ Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
                boolean isShowMultiSimSetting = Utils.isMultiSimEnabled(getActivity()) & Utils.isSupportSim2CellBroadcast();
                startCellBroadcastSetting(true, isShowMultiSimSetting);
            } else if (preference == mGsmUmtsSMSCBPref) {
                boolean isShowMultiSimSetting = Utils.isMultiSimEnabled(getActivity()) & Utils.isSupportSim2CellBroadcast();
                startCellBroadcastSetting(false, isShowMultiSimSetting);
                // --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
            }
        }
        // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings end.
        // Let the intents be launched by the Preference manager
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private String mManageMobilePlanMessage;
    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();

        NetworkInfo ni = mCm.getActiveNetworkInfo();
        if (mTm.hasIccCard() && (ni != null)) {
            // Check for carrier apps that can handle provisioning first
            Intent provisioningIntent = new Intent(TelephonyIntents.ACTION_CARRIER_SETUP);
            List<String> carrierPackages =
                    mTm.getCarrierPackageNamesForIntent(provisioningIntent);
            if (carrierPackages != null && !carrierPackages.isEmpty()) {
                if (carrierPackages.size() != 1) {
                    Log.w(TAG, "Multiple matching carrier apps found, launching the first.");
                }
                provisioningIntent.setPackage(carrierPackages.get(0));
                startActivity(provisioningIntent);
                return;
            }

            // Get provisioning URL
            String url = mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(url)) {
                Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "onManageMobilePlanClick: startActivity failed" + e);
                }
            } else {
                // No provisioning URL
                String operatorName = mTm.getSimOperatorName();
                if (TextUtils.isEmpty(operatorName)) {
                    // Use NetworkOperatorName as second choice in case there is no
                    // SPN (Service Provider Name on the SIM). Such as with T-mobile.
                    operatorName = mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(operatorName)) {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_unknown_sim_operator);
                    } else {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_no_provisioning_url, operatorName);
                    }
                } else {
                    mManageMobilePlanMessage = resources.getString(
                            R.string.mobile_no_provisioning_url, operatorName);
                }
            }
        } else if (mTm.hasIccCard() == false) {
            // No sim card
            mManageMobilePlanMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            // NetworkInfo is null, there is no connection
            mManageMobilePlanMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            log("onManageMobilePlanClick: message=" + mManageMobilePlanMessage);
            showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
        }
    }

    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
    private void updateSmsApplicationSetting() {
        log("updateSmsApplicationSetting:");
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getActivity(), true);
        if (appName != null) {
            String packageName = appName.getPackageName();

            CharSequence[] values = mSmsApplicationPreference.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (packageName.contentEquals(values[i])) {
                    mSmsApplicationPreference.setValueIndex(i);
                    mSmsApplicationPreference.setSummary(mSmsApplicationPreference.getEntries()[i]);
                    break;
                }
            }
        }
    }

    private void initSmsApplicationSetting() {
        log("initSmsApplicationSetting:");
        Collection<SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(getActivity());

        // If the list is empty the dialog will be empty, but we will not crash.
        int count = smsApplications.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (SmsApplicationData smsApplicationData : smsApplications) {
            packageNames[i] = smsApplicationData.mPackageName;
            i++;
        }
        String defaultPackageName = null;
        ComponentName appName = SmsApplication.getDefaultSmsApplication(getActivity(), true);
        if (appName != null) {
            defaultPackageName = appName.getPackageName();
        }
        mSmsApplicationPreference.setPackageNames(packageNames, defaultPackageName);
    }
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(mManageMobilePlanMessage)
                            .setCancelable(false)
                            .setPositiveButton(com.android.internal.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
                                    mManageMobilePlanMessage = null;
                                }
                            })
                            .create();
        }
        return super.onCreateDialog(dialogId);
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WIRELESS;
    }

    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
    private boolean isSmsSupported() {
        // Some tablet has sim card but could not do telephony operations. Skip those.
        return mTm.isSmsCapable();
    }

    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mManageMobilePlanMessage = savedInstanceState.getString(SAVED_MANAGE_MOBILE_PLAN_MSG);
        }
        log("onCreate: mManageMobilePlanMessage=" + mManageMobilePlanMessage);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mPm = getPackageManager();
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        addPreferencesFromResource(R.xml.wireless_settings);

        final int myUserId = UserHandle.myUserId();
        final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
        final boolean isRestrictedUser = mUm.getUserInfo(myUserId).isRestricted();
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end

        final Activity activity = getActivity();
        mAirplaneModePreference = (SwitchPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        SwitchPreference nfc = (SwitchPreference) findPreference(KEY_TOGGLE_NFC);
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference(KEY_ANDROID_BEAM_SETTINGS);
        SwitchPreference nsd = (SwitchPreference) findPreference(KEY_TOGGLE_NSD);
        /// M: Get MTK NFC setting preference
        PreferenceScreen mtkNfc = (PreferenceScreen) findPreference(KEY_MTK_TOGGLE_NFC);

        mAirplaneModeEnabler = new AirplaneModeEnabler(activity, mAirplaneModePreference);
        /// M:
        mNetworkSettingsPreference = (PreferenceScreen) findPreference(KEY_MOBILE_NETWORK_SETTINGS);

        mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);

        mButtonWfc = (PreferenceScreen) findPreference(KEY_WFC_SETTINGS);
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
        mSmsApplicationPreference = (AppListPreference) findPreference(KEY_SMS_APPLICATION);
        // Restricted users cannot currently read/write SMS.
        if (isRestrictedUser) {
            removePreference(KEY_SMS_APPLICATION);
        } else {
            mSmsApplicationPreference.setOnPreferenceChangeListener(this);
            initSmsApplicationSetting();
        }
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end

        // Remove NSD checkbox by default
        getPreferenceScreen().removePreference(nsd);
        //mNsdEnabler = new NsdEnabler(activity, nsd);

        String toggleable = Settings.Global.getString(activity.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        final boolean isWimaxEnabled = !isSecondaryUser && this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }

        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIFI)) {
            findPreference(KEY_VPN_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }
        // Disable VPN.
        if (isSecondaryUser || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_VPN)) {
            removePreference(KEY_VPN_SETTINGS);
        }

        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_BLUETOOTH)) {
            // No bluetooth-dependent items in the list. Code kept in case one is added later.
        }

        // Manually set dependencies for NFC when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_NFC)) {
            findPreference(KEY_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_ANDROID_BEAM_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
            /// M: Manually set dependencies for NFC
            findPreference(KEY_MTK_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove NFC if not available
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            mNfcEnabler = null;
            /// M: Remove MTK NFC setting
            getPreferenceScreen().removePreference(mtkNfc);
        } else {
            /// M: Remove NFC duplicate items @{
            if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                getPreferenceScreen().removePreference(nfc);
                getPreferenceScreen().removePreference(androidBeam);
                mNfcEnabler = null;
            } else {
                getPreferenceScreen().removePreference(mtkNfc);
            }
            /// @}
        }

        // Remove Mobile Network Settings and Manage Mobile Plan for secondary users,
        // if it's a wifi-only device, or if the settings are restricted.
        if (isSecondaryUser || Utils.isWifiOnly(getActivity())
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            removePreference(KEY_MOBILE_NETWORK_SETTINGS);
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }
        // Remove Mobile Network Settings and Manage Mobile Plan
        // if config_show_mobile_plan sets false.
        final boolean isMobilePlanEnabled = this.getResources().getBoolean(
                R.bool.config_show_mobile_plan);
        if (!isMobilePlanEnabled) {
            Preference pref = findPreference(KEY_MANAGE_MOBILE_PLAN);
            if (pref != null) {
                removePreference(KEY_MANAGE_MOBILE_PLAN);
            }
        }
		
	// xiongshigui@wind-mobi.com 20160525 add begin
        // Asus Jenpang begin: remove Manage Mobile Plan if mobile provisioning url is empty
        if (mCm!= null && TextUtils.isEmpty(mCm.getMobileProvisioningUrl())) {
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }
        // Asus Jenpang end: remove Manage Mobile Plan if mobile provisioning url is empty
	// xiongshigui@wind-mobi.com 20160525 add end

        //liyong01@wind-mobi.com -s
        // Update PlayTo preference: remove it if need
        Preference playToPref = (Preference) findPreference(KEY_PLAY_TO_SETTINGS);
        if (playToPref != null) {
            if (DisplayManager.isPlayToExist(activity)) {
                playToPref.setTitle(getString(activity, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_APP_NAME));
                playToPref.setSummary(getString(activity, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_SUMMARY));
            }
            else {
                getPreferenceScreen().removePreference(playToPref);
            }
        }
        //liyong01@wind-mobi.com -e
		// youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
        // Remove SMS Application if the device does not support SMS
        if (!isSmsSupported()) {
            removePreference(KEY_SMS_APPLICATION);
        }
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
        
        // Remove Airplane Mode settings if it's a stationary device such as a TV.
        if (mPm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
            removePreference(KEY_TOGGLE_AIRPLANE);
        }

        // Enable Proxy selector settings if allowed.
        Preference mGlobalProxy = findPreference(KEY_PROXY_SETTINGS);
        final DevicePolicyManager mDPM = (DevicePolicyManager)
                activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // proxy UI disabled until we have better app support
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);

        // Disable Tethering if it's not allowed or if it's a wifi-only device
        final ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (isSecondaryUser || !cm.isTetheringSupported()
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING)) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else {
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            p.setTitle(Utils.getTetheringLabel(cm));

            // Grey out if provisioning is not available.
            p.setEnabled(!TetherSettings
                    .isProvisioningNeededButUnavailable(getActivity()));
        }
        // M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings begin.
        if (FeatureOption.WIND_DEF_ASUS_CELLBROADCAST) {
            // +++ Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
            loadCellBroadcastPrefs();
            // --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
        }
        // M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings end.
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            mEthernetPreference = findPreference(KEY_ETHERNET_SETTINGS);
            if (mEthernetPreference != null && mCm.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
                mEm = (EthernetManager) getActivity().getSystemService(Context.ETHERNET_SERVICE);
                mEthernetPreference.setEnabled(EthUtils.isEthernetAvailable(mEm));
            } else {
                removePreference(KEY_ETHERNET_SETTINGS);
            }
        } else {
            removePreference(KEY_ETHERNET_SETTINGS);
        }
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 end

        // xiongshigui@wind-mobi.com 20160525 add begig
        // Remove DMSSettingsPreference if DLNAService package doesn't exist.
        // Get resource from DLNAService to set preference title and summary
        Preference dmsPref = findPreference(KEY_DMS_SETTINGS);
        if (dmsPref != null) {
            if (DMSSettings.isSupportDLNA(activity)) {
                dmsPref.setTitle(getString(activity, DMSSettings.DLNA_PACKAGE_NAME, "dms"));
                dmsPref.setSummary(getString(activity, DMSSettings.DLNA_PACKAGE_NAME, "desc_dms"));
            } else {
                getPreferenceScreen().removePreference(dmsPref);
            }
        }
        // xiongshigui@wind-mobi.com 20160525 add end

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                if (mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_CELL_BROADCAST_SETTINGS);
            if (ps != null) root.removePreference(ps);
        }

        /// M: Remove the entrance if RCSE not support. @{
        if (isAPKInstalled(activity, RCSE_SETTINGS_INTENT)) {
            Intent intent = new Intent(RCSE_SETTINGS_INTENT);
            findPreference(KEY_RCSE_SETTINGS).setIntent(intent);
        } else {
            Log.d(TAG, RCSE_SETTINGS_INTENT + " is not installed");
            getPreferenceScreen().removePreference(findPreference(KEY_RCSE_SETTINGS));
        }
        /// @}
       /// M: add the entrance RCS switch. @{
        IRCSSettings rcsExt = UtilsExt.getRcsSettingsPlugin(getActivity());
        rcsExt.addRCSPreference(getActivity(), getPreferenceScreen());
        /// @}

        /// M: for plug-in, make wfc setting plug-in @{
        mWfcExt = UtilsExt.getWfcSettingsPlugin(getActivity());
        /// @}
    }

   //liyong01@wind-mobi.com -s
    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mPlayToStatusReceiver, new IntentFilter(ACTION_PLAYTO_STATUS_CHANGED));
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
        if (mEm != null) {
            mEm.addListener(mEthernetListener);
        }
        // sunhuihui@wind-mobi.com modify Feature#110139 2016/6/29 end
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
        initSmsApplicationSetting();
        /// M: ALPS02243976, update sms summary
        updateSmsApplicationSetting();
        // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end
    }
  //liyong01@wind-mobi.com -e

    @Override
    public void onResume() {
        super.onResume();

        mAirplaneModeEnabler.resume();
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.resume();
        }
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
        if (mEthernetPreference != null) {
            mEthernetPreference.setEnabled(EthUtils.isEthernetAvailable(mEm));
        }
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 end

        // update WFC setting
        final Context context = getActivity();
        if (ImsManager.isWfcEnabledByPlatform(context)) {
            getPreferenceScreen().addPreference(mButtonWfc);

            mButtonWfc.setSummary(WifiCallingSettings.getWfcModeSummary(
                    context, ImsManager.getWfcMode(context)));
            /// M: for plug-in
            mWfcExt.initPlugin(this);
            mButtonWfc.setSummary(mWfcExt.getWfcSummary(context,
                    WifiCallingSettings.getWfcModeSummary(context,
                    ImsManager.getWfcMode(context))));
            mWfcExt.customizedWfcPreference(getActivity(), getPreferenceScreen());
        } else {
            removePreference(KEY_WFC_SETTINGS);
        }

        /// M: @{
        TelephonyManager telephonyManager =
            (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        updateMobileNetworkEnabled();
        IntentFilter intentFilter =
            new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        // listen to Carrier config change
        intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        getActivity().registerReceiver(mReceiver, intentFilter);
        /// @}

        /// M: WFC: get customized intent filter @{
        if (ImsManager.isWfcEnabledByPlatform(getActivity())) {
            mWfcExt.onWirelessSettingsEvent(DefaultWfcSettingsExt.RESUME);
        }
        /// @}
        // M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings begin.
		if (FeatureOption.WIND_DEF_ASUS_CELLBROADCAST) {
            // +++ Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
            updateCellBroadcastPrefEnabledState();
            // --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
        }
        // M: Added by cenxingcan@wind-mobi.com 20160722, to handle AsusCellBroadCast settings end.
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            outState.putString(SAVED_MANAGE_MOBILE_PLAN_MSG, mManageMobilePlanMessage);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mAirplaneModeEnabler.pause();
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.pause();
        }

        /// M:  @{
        TelephonyManager telephonyManager =
            (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        getActivity().unregisterReceiver(mReceiver);

        if (ImsManager.isWfcEnabledByPlatform(getActivity())) {
            mWfcExt.onWirelessSettingsEvent(DefaultWfcSettingsExt.PAUSE);
        }
        /// @}
    }

    //liyong01@wind-mobi.com -s
    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mPlayToStatusReceiver);
        // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
        if (mEm != null) {
            mEm.removeListener(mEthernetListener);
        }
        // sunhuihui@wind-mobi.com modify Feature#110139 2016/6/29 end
    }
   //liyong01@wind-mobi.com -e

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSmsApplicationPreference && newValue != null) {
            SmsApplication.setDefaultApplication(newValue.toString(), getActivity());
            updateSmsApplicationSetting();
            return true;
        }
        return false;
    }
    // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.wireless_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                result.add(KEY_TOGGLE_NSD);

                final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                final int myUserId = UserHandle.myUserId();
                final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;
                final boolean isWimaxEnabled = !isSecondaryUser
                        && context.getResources().getBoolean(
                        com.android.internal.R.bool.config_wimaxEnabled);
                if (!isWimaxEnabled
                        || um.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
                    result.add(KEY_WIMAX_SETTINGS);
                }

                if (isSecondaryUser) { // Disable VPN
                    result.add(KEY_VPN_SETTINGS);
                }

                // Remove NFC if not available
                final NfcManager manager = (NfcManager)
                        context.getSystemService(Context.NFC_SERVICE);
                if (manager != null) {
                    NfcAdapter adapter = manager.getDefaultAdapter();
                    if (adapter == null) {
                        result.add(KEY_TOGGLE_NFC);
                        result.add(KEY_ANDROID_BEAM_SETTINGS);
                        /// M: Remove MTK NFC setting
                        result.add(KEY_MTK_TOGGLE_NFC);
                    } else {
                        /// M: Remove NFC duplicate items @{
                        if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                            result.add(KEY_TOGGLE_NFC);
                            result.add(KEY_ANDROID_BEAM_SETTINGS);
                        } else {
                            result.add(KEY_MTK_TOGGLE_NFC);
                        }
                        /// @}
                    }
                }

                // Remove Mobile Network Settings and Manage Mobile Plan if it's a wifi-only device.
                if (isSecondaryUser || Utils.isWifiOnly(context)) {
                    result.add(KEY_MOBILE_NETWORK_SETTINGS);
                    result.add(KEY_MANAGE_MOBILE_PLAN);
                }

                // Remove Mobile Network Settings and Manage Mobile Plan
                // if config_show_mobile_plan sets false.
                final boolean isMobilePlanEnabled = context.getResources().getBoolean(
                        R.bool.config_show_mobile_plan);
                if (!isMobilePlanEnabled) {
                    result.add(KEY_MANAGE_MOBILE_PLAN);
                }
                // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 begin
                // Remove SMS Application if the device does not support SMS
                TelephonyManager tm =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                //if (!tm.isSmsCapable() || isRestrictedUser) {
                if (!tm.isSmsCapable()) {
                    result.add(KEY_SMS_APPLICATION);
                }
                // youxiaoyan@wind-mobi.com bug#111688 2016/5/26 end

                final PackageManager pm = context.getPackageManager();

                // Remove Airplane Mode settings if it's a stationary device such as a TV.
                if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                    result.add(KEY_TOGGLE_AIRPLANE);
                }

                // proxy UI disabled until we have better app support
                result.add(KEY_PROXY_SETTINGS);

                // Disable Tethering if it's not allowed or if it's a wifi-only device
                ConnectivityManager cm = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (isSecondaryUser || !cm.isTetheringSupported()) {
                    result.add(KEY_TETHER_SETTINGS);
                }

                // sunhuihui@wind-mobi.com modify Feature#110139 2016/6/29 begin
                // Disable Ethernet if it's not supported
                if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG && !cm.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
                    result.add(KEY_ETHERNET_SETTINGS);
                }
                // sunhuihui@wind-mobi.com modify Feature#110139 2016/6/29 end

                // Enable link to CMAS app settings depending on the value in config.xml.
                boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(
                        com.android.internal.R.bool.config_cellBroadcastAppLinks);
                try {
                    if (isCellBroadcastAppLinkEnabled) {
                        if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                            isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
                }
                if (isSecondaryUser || !isCellBroadcastAppLinkEnabled) {
                    result.add(KEY_CELL_BROADCAST_SETTINGS);
                }

                ///M: Reomve RCSE search if not support.
                if (!isAPKInstalled(context, RCSE_SETTINGS_INTENT)) {
                    result.add(KEY_RCSE_SETTINGS);
                }

                return result;
            }
        };

    ///M:
    private static boolean isAPKInstalled(Context context, String action) {
         Intent intent = new Intent(action);
         List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
         return !(apps == null || apps.size() == 0);
    }

    /// M: @{
    private PreferenceScreen mNetworkSettingsPreference;

    private void updateMobileNetworkEnabled() {
        // modify in a simple way to get whether there is sim card inserted
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(getActivity());
        TelephonyManager telephonyManager =
            (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int callState = telephonyManager.getCallState();
        int simNum = SubscriptionManager.from(getActivity()).getActiveSubscriptionInfoCount();
        Log.d(TAG, "callstate = " + callState + " simNum = " + simNum);
        if (simNum > 0 && callState == TelephonyManager.CALL_STATE_IDLE &&
                !miscExt.isWifiOnlyModeSet()) {
            mNetworkSettingsPreference.setEnabled(true);
        } else {
            /// M: for plug-in
            if (CdmaFeatureOptionUtils.isCT6MSupport()) {
                mNetworkSettingsPreference.setEnabled(CdmaFeatureOptionUtils
                        .isCTLteTddTestSupport());
            } else {
                mNetworkSettingsPreference.setEnabled(UtilsExt
                        .getSimManagmentExtPlugin(getActivity()).useCtTestcard() || false);
            }
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Log.d(TAG, "PhoneStateListener, new state=" + state);
            if (state == TelephonyManager.CALL_STATE_IDLE && getActivity() != null) {
                updateMobileNetworkEnabled();
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                Log.d(TAG, "ACTION_SIM_INFO_UPDATE received");
                updateMobileNetworkEnabled();
            // when received Carrier config changes, update WFC buttons
            } else if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)) {
                Log.d(TAG, "carrier config changed...");
                if (mButtonWfc != null) {
                    if (ImsManager.isWfcEnabledByPlatform(context)) {
                        Log.d(TAG, "wfc enabled, add WCF setting");
                        getPreferenceScreen().addPreference(mButtonWfc);
                        mWfcExt.initPlugin(WirelessSettings.this);
                        mButtonWfc.setSummary(mWfcExt.getWfcSummary(context,
                                WifiCallingSettings.getWfcModeSummary(context,
                                ImsManager.getWfcMode(context))));
                        mWfcExt.customizedWfcPreference(getActivity(), getPreferenceScreen());
                        mWfcExt.onWirelessSettingsEvent(DefaultWfcSettingsExt.RESUME);
                    } else {
                        Log.d(TAG, "wfc disabled, remove WCF setting");
                        mWfcExt.onWirelessSettingsEvent(DefaultWfcSettingsExt.PAUSE);
                        getPreferenceScreen().removePreference(mButtonWfc);
                    }
                }
            }
        }
    };
    /// @}

    //liyong01@wind-mobi.com -s
    private final BroadcastReceiver mPlayToStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Preference playToPref = (Preference) findPreference(KEY_PLAY_TO_SETTINGS);
            if (playToPref == null || !DisplayManager.isPlayToExist(context)) {
                return;
            }

            int playToState = intent.getExtras().getInt(KEY_ACTIVE_TARGET_STATUS);
            String deviceName = intent.getExtras().getString(KEY_ACTIVE_TARGET_FRIENDLY_NAME);
            switch (playToState) {
                case REMOTE_TARGET_STATE_NOT_CONNECTED:
                    playToPref.setSummary(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_SUMMARY));
                    break;
                case REMOTE_TARGET_STATE_CONNECTING:
                    playToPref.setSummary(String.format(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_NOTIFICATION_CONNECTING_MESSAGE), deviceName));
                    break;
                case REMOTE_TARGET_STATE_CONNECTED:
                    playToPref.setSummary(String.format(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_NOTIFICATION_CONNECTED_MESSAGE), deviceName));
                    break;
            }
        }
    };

    public static String getString(Context context, String packageName, String resId) {
        Resources res = null;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
            int resourceId = res.getIdentifier(String.format( "%s:string/%s", packageName, resId), null, null);
            if (resourceId != 0) {
                return "" + context.getPackageManager().getText(packageName, resourceId, null);
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "NameNotFoundException: " + packageName);
        }
        return resId;
    }
    //liyong01@wind-mobi.com -e

    // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 begin
    private EthernetManager.Listener mEthernetListener = new EthernetManager.Listener() {
        @Override
        public void onAvailabilityChanged(boolean isAvailable) {
            Log.d(TAG, "EthernetManager.Listener.onAvailabilityChanged");
            mEthernetPreference.setEnabled(EthUtils.isEthernetAvailable(mEm));
        }
    };
    // sunhuihui@wind-mobi.com modify Feature#110139 From usb dialog patch. 2016/6/29 end

    // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings begin.
	/**  M: origin code , modified by cenxingcan@wind-mobi.com at 20160722 begin
    private Intent getWirelessAlertsSettingIntent(String prefkey){
        final Intent mIntent = new Intent(Intent.ACTION_MAIN);
        mIntent.setComponent(null);
        if (prefkey.equals(KEY_CELL_BROADCAST_CMAS_SETTINGS)) {
            mIntent.setComponent(new ComponentName(CMAS_COMPONENT, CELL_BROADCAST_SETTING_ACTIVITY));
        } else if(prefkey.equals(KEY_CELL_BROADCAST_SMSCB_SETTINGS)) {
            mIntent.setComponent(new ComponentName(CMAS_COMPONENT, CELL_BROADCAST_SMSDUAL_ACTIVITY));
        }
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return mIntent;
    }

    private void startCBSettingActivity(Context mContext, String prefkey){
        try {
            mContext.startActivity(getWirelessAlertsSettingIntent(prefkey));
        } catch(final ActivityNotFoundException e) {
            // Handle so we shouldn't crash if the wireless alerts
            // implementation is broken.
            Log.e(TAG, "Failed to launch wireless alerts activity, e : " + e);
        }
    }
	M : origin code , modified by cenxingcan@wind-mobi.com at 20160722 end **/

    // +++ Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
    private void loadCellBroadcastPrefs() {
        final int myUserId = UserHandle.myUserId();
        final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;
        final boolean isUserRestriction = mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        final boolean isWifiOnly = Utils.isWifiOnly(getActivity());
        Log.d(TAG, "loadCellBroadcastPrefs, isSecondaryUser = " + isSecondaryUser
                + " , isUserRestriction = " + isUserRestriction + " , isWifiOnly = " + isWifiOnly);
        if (isSecondaryUser || isUserRestriction || isWifiOnly) {
            PreferenceScreen prefCBScreen = (PreferenceScreen)findPreference(
                    KEY_CELL_BROADCAST_SETTINGS);
            getPreferenceScreen().removePreference(prefCBScreen);
        } else {
            mCMASPrefCategory = (PreferenceCategory)findPreference("pref_key_cmas_settings");
            mSMSCBPrefCategory = (PreferenceCategory)findPreference("pref_key_smscb_settings");
            mGsmUmtsCMASPref = findPreference(GSM_UMTS_CMAS);
            mGsmUmtsSMSCBPref = findPreference(GSM_UMTS_SMSCB);
        }
    }

    private void updateCellBroadcastPrefEnabledState() {
        final int myUserId = UserHandle.myUserId();
        final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;
        final boolean isUserRestriction = mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        final boolean isWifiOnly = Utils.isWifiOnly(getActivity());
        if (isSecondaryUser || isUserRestriction || isWifiOnly) {
            if (mCMASPrefCategory != null) {
                mCMASPrefCategory.setEnabled(false);
            }
            if (mSMSCBPrefCategory != null) {
                mSMSCBPrefCategory.setEnabled(false);
            }
        } else {
            if (mCMASPrefCategory != null) {
                mCMASPrefCategory.setEnabled(true);
            }
            if (mSMSCBPrefCategory != null) {
                mSMSCBPrefCategory.setEnabled(true);
            }
        }
    }

    private void startCellBroadcastSetting(boolean configCmas, boolean dualSim) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        if (dualSim) {
            intent.setComponent(new ComponentName(
                    CMAS_COMPONENT,
                    CELL_BROADCAST_SMSDUAL_ACTIVITY));
        } else {
            intent.setComponent(new ComponentName(
                    CMAS_COMPONENT,
                    CELL_BROADCAST_SMS_ACTIVITY));
        }
        intent.putExtra("config_cmas", configCmas);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            Log.d(TAG, "startCellBroadcastSetting: configCmas=" + configCmas + ", dualSim=" + dualSim);
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Log.e(TAG, "" + ignored);
        }
    }

    // --- Gary_Hsu@asus.com,20160317: add for AsusCellBroadcast Setting
    // M: Added by cenxingcan@wind-mobi.com 20160715, to handle AsusCellBroadCast settings end.
}
