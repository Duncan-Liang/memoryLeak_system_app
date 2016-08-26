package com.android.settings.cta;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.R.integer;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import java.util.ArrayList;

public class CtaSettings extends SettingsPreferenceFragment {
    private static final String KEY_PHONE_CALL = "phone_call";
    private static final String KEY_SEND_SMS = "send_sms";
    private static final String KEY_SEND_MMS = "send_mms";
    private static final String KEY_MOBILE_NETWORK = "mobile_network";
    private static final String KEY_WLAN = "wlan";
    private static final String KEY_READ_CONTACTS = "read_contacts";
    private static final String KEY_CALL_LOG = "read_call_log";
    private static final String KEY_READ_MMS = "read_mms";
    private static final String KEY_READ_SMS = "read_sms";
    private static final String KEY_READ_MMS_SMS = "read_mms_sms";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_CALL_RECORDER = "call_recorder";
    private static final String KEY_SOUND_RECORDER = "sound_recorder";
    private static final String KEY_CAMERA = "camera";
    private static final String KEY_BLUETOOTH = "bluetooth";
    private static final String KEY_USE_NETWORK = "use_network";
    private static final String KEY_WRITE_CONTACTS = "write_contacts";
    private static final String KEY_WRITE_CALL_LOG = "write_call_log";
    private static final String KEY_WRITE_MMS = "write_mms";
    private static final String KEY_WRITE_SMS = "write_sms";
    private static final String KEY_NFC = "nfc";
    private static final String CTA_TYPE = "CTA_TYPE";
    private static final String CTAITEMLISTACTIVITY = "com.android.settings.cta.CtaItemListActivity";
    private static final String CTA_CATEGORY_DATA = "cta_category_data";

    private Preference mPhonecallPref;
    private Preference mSendSmsPref;
    private Preference mSendMmsPref;
    private Preference mMobileNetworkPref;
    private Preference mWlanPref;
    private Preference mReadContactsPref;
    private Preference mReadCallLogPref;
    private Preference mReadMmsPref;
    private Preference mReadSmsPref;
    private Preference mReadMmsSmsPref;
    private Preference mLocationPref;
    private Preference mCallRecorderPref;
    private Preference mSoundRecorderPref;
    private Preference mCameraPref;
    private Preference mBluetoothPref;
    private Preference mUseNetwork;
    private Preference mWrieContactsPref;
    private Preference mWrieCallLogPref;
    private Preference mWrieMmsPref;
    private Preference mWrieSmsPref;
    private Preference mNfcPref;

    private static final int CTA_PHONE_CALL = 0;
    private static final int CTA_SEND_MMS = 1;
    private static final int CTA_SEND_SMS = 2;
    private static final int CTA_READ_CONTACTS = 3;
    private static final int CTA_READ_CALL_LOG = 4;
    private static final int CTA_READ_MMS = 5;
    private static final int CTA_READ_SMS = 6;
    private static final int CTA_READ_MMS_SMS = 7;
    private static final int CTA_MOBILE_NETWORK = 8;
    private static final int CTA_WLAN = 9;
    private static final int CTA_LOCATION = 10;
    private static final int CTA_CALL_RECORDER = 11;
    private static final int CTA_SOUND_RECORDER = 12;
    private static final int CTA_CAMERA = 13;
    private static final int CTA_BLUETOOTH = 14;
    private static final int CTA_COST = 15;
    private static final int CTA_USE_NETWORK = 16;
    private static final int CTA_WRITE_CONTACTS = 17;
    private static final int CTA_WRITE_CALL_LOG = 18;
    private static final int CTA_WRITE_MMS = 19;
    private static final int CTA_WRITE_SMS = 20;
    private static final int CTA_NFC = 21;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cta_prefs);
        findPreference();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    private void findPreference() {
        mPhonecallPref = findPreference(KEY_PHONE_CALL);
        mSendSmsPref = findPreference(KEY_SEND_SMS);
        mSendMmsPref = findPreference(KEY_SEND_MMS);
        mMobileNetworkPref = findPreference(KEY_MOBILE_NETWORK);
        mWlanPref = findPreference(KEY_WLAN);
        mReadContactsPref = findPreference(KEY_READ_CONTACTS);
        mReadCallLogPref = findPreference(KEY_CALL_LOG);
        mReadMmsPref = findPreference(KEY_READ_MMS);
        mReadSmsPref = findPreference(KEY_READ_SMS);
        //mReadMmsSmsPref = findPreference(KEY_READ_MMS_SMS);
        mLocationPref = findPreference(KEY_LOCATION);
        //mCallRecorderPref = findPreference(KEY_CALL_RECORDER);
        mSoundRecorderPref = findPreference(KEY_SOUND_RECORDER);
        mCameraPref = findPreference(KEY_CAMERA);
        mBluetoothPref = findPreference(KEY_BLUETOOTH);
        mUseNetwork = findPreference(KEY_USE_NETWORK);
        mWrieContactsPref = findPreference(KEY_WRITE_CONTACTS);
        mWrieCallLogPref = findPreference(KEY_WRITE_CALL_LOG);
        mWrieMmsPref = findPreference(KEY_WRITE_MMS);
        mWrieSmsPref = findPreference(KEY_WRITE_SMS);
        mNfcPref = findPreference(KEY_NFC);
        final PreferenceGroup ctacategorydata = (PreferenceGroup)
                findPreference(CTA_CATEGORY_DATA);
        if(NfcAdapter.getDefaultAdapter(getActivity()) == null){
            ctacategorydata.removePreference(mNfcPref);
        }
    }

    private void startCtaItemList(int acttype) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getActivity(), CTAITEMLISTACTIVITY));
        intent.putExtra(CTA_TYPE, acttype);
        getActivity().startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        // TODO Auto-generated method stub
        if(preference == mPhonecallPref){
            startCtaItemList(CTA_PHONE_CALL);
        }else if(preference == mSendSmsPref){
            startCtaItemList(CTA_SEND_SMS);
        }else if(preference == mSendMmsPref){
            startCtaItemList(CTA_SEND_MMS);
        }else if(preference == mReadContactsPref){
            startCtaItemList(CTA_READ_CONTACTS);
        }else if(preference == mReadCallLogPref){
            startCtaItemList(CTA_READ_CALL_LOG);
        }else if(preference == mReadMmsPref){
            startCtaItemList(CTA_READ_MMS);
        }else if(preference == mReadSmsPref){
            startCtaItemList(CTA_READ_SMS);
        }else if(preference == mReadMmsSmsPref){
            startCtaItemList(CTA_READ_MMS_SMS);
        }else if(preference == mMobileNetworkPref){
            startCtaItemList(CTA_MOBILE_NETWORK);
        }else if(preference == mWlanPref){
            startCtaItemList(CTA_WLAN);
        }else if(preference == mLocationPref){
            startCtaItemList(CTA_LOCATION);
        }else if(preference == mCallRecorderPref){
            startCtaItemList(CTA_CALL_RECORDER);
        }else if(preference == mSoundRecorderPref){
            startCtaItemList(CTA_SOUND_RECORDER);
        }else if(preference == mCameraPref){
            startCtaItemList(CTA_CAMERA);
        }else if(preference == mBluetoothPref){
            startCtaItemList(CTA_BLUETOOTH);
        }else if(preference == mUseNetwork){
            startCtaItemList(CTA_USE_NETWORK);
        }else if(preference == mWrieContactsPref){
            startCtaItemList(CTA_WRITE_CONTACTS);
        }else if(preference == mWrieCallLogPref){
            startCtaItemList(CTA_WRITE_CALL_LOG);
        }else if(preference == mWrieMmsPref){
            startCtaItemList(CTA_WRITE_MMS);
        }else if(preference == mWrieSmsPref){
            startCtaItemList(CTA_WRITE_SMS);
        }else if(preference == mNfcPref){
            startCtaItemList(CTA_NFC);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}