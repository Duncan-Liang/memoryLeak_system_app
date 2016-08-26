package com.android.settings.ethernet;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

// sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
import com.mediatek.settings.FeatureOption;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
// sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.StaticIpConfiguration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Switch;

import java.net.Inet4Address;

public class EthernetSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, TextWatcher, Indexable {
    private static final String TAG = "EthernetSettings";

    private static final int CONNECTION_TYPE_DHCP = 0;
    private static final int CONNECTION_TYPE_STATIC = 1;

    private static final String KEY_ETHERNET_POLICY = "eth_policy";
    private static final String KEY_CONNECTION_TYPE = "eth_conn_type";
    private static final String KEY_DHCP_IP_ADDRESS = "eth_dhcp_ip_address";
    private static final String KEY_STATIC_IP_ADDRESS = "eth_static_ip_address";
    private static final String KEY_NETWORK_PREFIX_LENGTH = "eth_network_prefix_length";
    private static final String KEY_GATEWAY = "eth_gateway";
    private static final String KEY_DNS1 = "eth_dns1";
    private static final String KEY_DNS2 = "eth_dns2";

    private static final String ACTION_ETHERNET_ENABLE = "com.android.settings.ethernet.ENABLE";
    private static final String ACTION_ETHERNET_CONFIG_CHANGED = "com.android.settings.ethernet.CONFIG_CHANGED";

    private Context mContext;

    private SwitchBar mEthernetSwitch;
    private CheckBoxPreference mEthernetPolicy;

    private ListPreference mConnectionType;
    private Preference mDhcpIpAddress;
    private EditTextPreference mStaticIpAddress;
    private EditTextPreference mNetworkPrefixLength;
    private EditTextPreference mGateway;
    private EditTextPreference mDns1;
    private EditTextPreference mDns2;

    private EditTextPreference mForegroundEditTextPreference;

    private ConnectivityManager mCm;
    private EthernetManager mEm;
    private ContentResolver mCr;
    private IpConfiguration mIpConfig;
    private StaticIpConfiguration mStaticIpConfig;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.ethernet_settings);
        super.onActivityCreated(savedInstanceState);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEm = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        mCr = mContext.getContentResolver();

        SettingsActivity activity = (SettingsActivity) getActivity();
        mEthernetSwitch = activity.getSwitchBar();
        mEthernetSwitch.show();
        // sunhuihui@wind-mobi.com modify begin from usb dialog patch. from usb dialog patch Feature#110139 2016/6/27
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            if (mEm != null) {
                updateEthernetSwitch(0); //mEm.getEthernetState());
            }
        } else {
            mEthernetSwitch.setEnabled(EthUtils.isEthernetAvailable(mEm));
            mEthernetSwitch.setChecked(EthUtils.isEthernetAvailable(mEm) && EthUtils.isEthernetEnabled(mCr));
        }
        // sunhuihui@wind-mobi.com modify end from usb dialog patch. from usb dialog patch Feature#110139 2016/6/27
        mEthernetSwitch.addOnSwitchChangeListener(this);

        mEthernetPolicy = (CheckBoxPreference) findPreference(KEY_ETHERNET_POLICY);
        mEthernetPolicy.setOnPreferenceChangeListener(this);

        mConnectionType = (ListPreference) findPreference(KEY_CONNECTION_TYPE);
        if (mConnectionType != null) {
            mConnectionType.setOnPreferenceChangeListener(this);
        }

        mDhcpIpAddress = (Preference) findPreference(KEY_DHCP_IP_ADDRESS);
        mStaticIpAddress = (EditTextPreference) findPreference(KEY_STATIC_IP_ADDRESS);
        mStaticIpAddress.setOnPreferenceChangeListener(this);
        mStaticIpAddress.getEditText().addTextChangedListener(this);
        mNetworkPrefixLength = (EditTextPreference) findPreference(KEY_NETWORK_PREFIX_LENGTH);
        mNetworkPrefixLength.setOnPreferenceChangeListener(this);
        mNetworkPrefixLength.getEditText().addTextChangedListener(this);
        mGateway = (EditTextPreference) findPreference(KEY_GATEWAY);
        mGateway.setOnPreferenceChangeListener(this);
        mGateway.getEditText().addTextChangedListener(this);
        mDns1 = (EditTextPreference) findPreference(KEY_DNS1);
        mDns1.setOnPreferenceChangeListener(this);
        mDns1.getEditText().addTextChangedListener(this);
        mDns2 = (EditTextPreference) findPreference(KEY_DNS2);
        mDns2.setOnPreferenceChangeListener(this);
        mDns2.getEditText().addTextChangedListener(this);

        // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            updateEthernetFields();
        } else {
            setFieldsEnabled(EthUtils.isEthernetAvailable(mEm) && EthUtils.isEthernetEnabled(mCr));
        }
        // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mEm != null) {
            mEm.addListener(mEthernetListener);
        }
        if (mCm != null) {
            NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build();
            mCm.registerNetworkCallback(request, mNetworkCallback);
        }

        // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
        boolean isUsb_dialog_feature = false;
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            if (mEm != null) {
                updateEthernetSwitch(0);//mEm.getEthernetState());
                updateEthernetFields();
            }
            isUsb_dialog_feature = (mEm != null); //&& (mEm.getEthernetSleepPolicy() == EthernetManager.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND);
        } else {
            mEthernetSwitch.setChecked(EthUtils.isEthernetAvailable(mEm) && EthUtils.isEthernetEnabled(mCr));
        }

        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG ? isUsb_dialog_feature : EthUtils.getDatabase(mCr, Settings.System.ETHERNET_CONNECT_POLICY, EthUtils.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND)
                == EthUtils.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND) {
            mEthernetPolicy.setChecked(false);
        } else {
            mEthernetPolicy.setChecked(true);
        }
        readIpConfiguration();

        /*if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            getActivity().registerReceiver(mEthernetStateReceiver, new IntentFilter(EthernetManager.ETHERNET_STATE_CHANGED_ACTION));
        }*/
        // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEm != null) {
            mEm.removeListener(mEthernetListener);
        }
        if (mCm != null) {
            mCm.unregisterNetworkCallback(mNetworkCallback);
        }

        // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
        /*
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            getActivity().unregisterReceiver(mEthernetStateReceiver);
        }
        */
        // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEthernetSwitch.removeOnSwitchChangeListener(this);
        mEthernetSwitch.hide();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.ETHERNET;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            writeIpConfiguration();
            readIpConfiguration();
        }

        // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
        if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            /*synchronized (mEthernetSwitchByCode) {
                if ((mEm != null) && (mEm.isAvailable()) && !mEthernetSwitchByCode) {
                    mEm.setEthernetEnabled(isChecked);
                }
            }*/
        } else {
            if (EthUtils.isEthernetAvailable(mEm)) {
                EthUtils.setDatabase(mCr, Settings.System.ETHERNET_ENABLE, isChecked ? EthUtils.ETHERNET_ENABLED : EthUtils.ETHERNET_DISABLED);
                mContext.sendBroadcast(new Intent(ACTION_ETHERNET_ENABLE));
            }
            setFieldsEnabled(EthUtils.isEthernetAvailable(mEm) && EthUtils.isEthernetEnabled(mCr));
        }
        // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
        readIpConfiguration();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference.getKey().equals(KEY_CONNECTION_TYPE)) {
            return true;
        } else if (preference.getKey().equals(KEY_ETHERNET_POLICY)) {
            // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
            if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
                /*if (mEm != null) {
                    if (mEthernetPolicy.isChecked()) {
                        mEm.setEthernetSleepPolicy(EthernetManager.ETHERNET_POLICY_KEEP_ON_WHEN_SUSPEND);
                    } else {
                        mEm.setEthernetSleepPolicy(EthernetManager.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND);
                    }
                }*/
            } else {
                if (mEthernetPolicy.isChecked()) {
                    EthUtils.setDatabase(mCr, Settings.System.ETHERNET_CONNECT_POLICY, EthUtils.ETHERNET_POLICY_KEEP_ON_WHEN_SUSPEND);
                } else {
                    EthUtils.setDatabase(mCr, Settings.System.ETHERNET_CONNECT_POLICY, EthUtils.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND);
                }
            }
            // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
            return true;
        } else if (preference instanceof EditTextPreference) {
            mForegroundEditTextPreference = (EditTextPreference) preference;
            setDialogPositiveEnabled(mForegroundEditTextPreference, false);

            if (mForegroundEditTextPreference.getEditText().getText().toString() != null) {
                mForegroundEditTextPreference.getEditText().setSelection(mForegroundEditTextPreference.getEditText().getText().toString().length());
            }

            if (mForegroundEditTextPreference.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
                if (EthUtils.validateNetPrefixConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                    setDialogPositiveEnabled(mForegroundEditTextPreference, true);
                }
            } else {
                if (EthUtils.validateIpConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                    setDialogPositiveEnabled(mForegroundEditTextPreference, true);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key == null) return true;

        if (key.equals(KEY_CONNECTION_TYPE)) {
            boolean useStaticIp = false;
            try {
                useStaticIp = (Integer.parseInt((String) newValue) == CONNECTION_TYPE_STATIC);
            } catch (NumberFormatException e) {
            }
            showStaticIpSettings(useStaticIp);
            writeIpConfiguration();
            // readIpConfiguration();
        } else if (preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setText((String) newValue);
            writeIpConfiguration();
            handleEmptyField((EditTextPreference) preference);
        }
        return true;
    }

    private void showStaticIpSettings(boolean show) {
        mConnectionType.setValue(show ? String.valueOf(CONNECTION_TYPE_STATIC) : String.valueOf(CONNECTION_TYPE_DHCP));
        mConnectionType.setSummary(show ? mConnectionType.getEntries()[CONNECTION_TYPE_STATIC] : mConnectionType.getEntries()[CONNECTION_TYPE_DHCP]);
        if (show) {
            getPreferenceScreen().removePreference(mDhcpIpAddress);
            getPreferenceScreen().addPreference(mStaticIpAddress);
            getPreferenceScreen().addPreference(mNetworkPrefixLength);
            getPreferenceScreen().addPreference(mGateway);
            getPreferenceScreen().addPreference(mDns1);
            getPreferenceScreen().addPreference(mDns2);
        } else {
            getPreferenceScreen().addPreference(mDhcpIpAddress);
            getPreferenceScreen().removePreference(mStaticIpAddress);
            getPreferenceScreen().removePreference(mNetworkPrefixLength);
            getPreferenceScreen().removePreference(mGateway);
            getPreferenceScreen().removePreference(mDns1);
            getPreferenceScreen().removePreference(mDns2);
        }
    }

    private void setFieldsEnabled(boolean enable) {
        if ((mEthernetPolicy == null) || (mConnectionType == null) || (mDhcpIpAddress == null) || (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        mEthernetPolicy.setEnabled(enable);
        mConnectionType.setEnabled(enable);
        updateDhcpIpAddress();
        mStaticIpAddress.setEnabled(enable);
        mNetworkPrefixLength.setEnabled(enable);
        mGateway.setEnabled(enable);
        mDns1.setEnabled(enable);
        mDns2.setEnabled(enable);
    }

    private void readIpConfiguration() {
        if ((mConnectionType == null) || (mDhcpIpAddress == null) || (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        mIpConfig = mEm.getConfiguration();
        if (mIpConfig.getIpAssignment() == IpConfiguration.IpAssignment.DHCP) {
            mConnectionType.setValue(String.valueOf(CONNECTION_TYPE_DHCP));
            mConnectionType.setSummary(mConnectionType.getEntries()[CONNECTION_TYPE_DHCP]);
            showStaticIpSettings(false);
        } else {
            mConnectionType.setValue(String.valueOf(CONNECTION_TYPE_STATIC));
            mConnectionType.setSummary(mConnectionType.getEntries()[CONNECTION_TYPE_STATIC]);
            showStaticIpSettings(true);
        }
        mStaticIpConfig = mIpConfig.getStaticIpConfiguration();
        if (mStaticIpConfig != null) {
            if ((mStaticIpConfig.ipAddress != null) && (mStaticIpConfig.ipAddress.getAddress() instanceof Inet4Address)) {
                mStaticIpAddress.setText(EthUtils.inet4AddressToString((Inet4Address) mStaticIpConfig.ipAddress.getAddress()));
                mNetworkPrefixLength.setText(Integer.valueOf(mStaticIpConfig.ipAddress.getNetworkPrefixLength()).toString());
            }
            if ((mStaticIpConfig.gateway != null) && (mStaticIpConfig.gateway instanceof Inet4Address)) {
                mGateway.setText(EthUtils.inet4AddressToString((Inet4Address) mStaticIpConfig.gateway));
            }
            if (mStaticIpConfig.dnsServers != null) {
                if ((mStaticIpConfig.dnsServers.size() > 0) && (mStaticIpConfig.dnsServers.get(0) instanceof Inet4Address)) {
                    mDns1.setText(EthUtils.inet4AddressToString((Inet4Address) mStaticIpConfig.dnsServers.get(0)));
                }
                if ((mStaticIpConfig.dnsServers.size() > 1) && (mStaticIpConfig.dnsServers.get(1) instanceof Inet4Address)) {
                    mDns2.setText(EthUtils.inet4AddressToString((Inet4Address) mStaticIpConfig.dnsServers.get(1)));
                }
            }
        }

        handleEmptyField(mStaticIpAddress);
        handleEmptyField(mNetworkPrefixLength);
        handleEmptyField(mGateway);
        handleEmptyField(mDns1);
        handleEmptyField(mDns2);
        updateDhcpIpAddress();
    }

    private void writeIpConfiguration() {
        if ((mEm == null) || (mConnectionType == null) || (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        try {
            boolean useDhcp = (Integer.parseInt(mConnectionType.getValue()) == CONNECTION_TYPE_DHCP);
            if (mStaticIpConfig == null) {
                mStaticIpConfig = new StaticIpConfiguration();
            }
            // Static IP and network mask
            Inet4Address staticIpAddress = null;
            int networkPrefixLength = -1;
            if ((mStaticIpAddress.getText() != null) && (mStaticIpAddress.getText().length() > 0)) {
                staticIpAddress = EthUtils.strToInet4Address(mStaticIpAddress.getText());
            }
            if ((mNetworkPrefixLength.getText() != null) && (mNetworkPrefixLength.getText().length() > 0)) {
                networkPrefixLength = Integer.parseInt(mNetworkPrefixLength.getText());
            }
            if ((staticIpAddress != null) && (networkPrefixLength > 0)) {
                mStaticIpConfig.ipAddress = new LinkAddress(staticIpAddress, networkPrefixLength);
            }
            // Gateway
            Inet4Address gateway = null;
            if ((mGateway.getText() != null) && (mGateway.getText().length() > 0)) {
                gateway = EthUtils.strToInet4Address(mGateway.getText());
            }
            if (gateway != null) {
                mStaticIpConfig.gateway = gateway;
            }
            mStaticIpConfig.dnsServers.clear();
            // DNS 1
            Inet4Address dns1 = null;
            if ((mDns1.getText() != null) && (mDns1.getText().length() > 0)) {
                dns1 = EthUtils.strToInet4Address(mDns1.getText());
            }
            if (dns1 != null) {
                mStaticIpConfig.dnsServers.add(dns1);
            }
            // DNS 2
            Inet4Address dns2 = null;
            if ((mDns2.getText() != null) && (mDns2.getText().length() > 0)) {
                dns2 = EthUtils.strToInet4Address(mDns2.getText());
            }
            if ((dns1 != null) && (dns2 != null)) {
                mStaticIpConfig.dnsServers.add(dns2);
            }

            if (mIpConfig == null) {
                mIpConfig = new IpConfiguration();
            }
            mIpConfig.setIpAssignment(useDhcp ? IpConfiguration.IpAssignment.DHCP : IpConfiguration.IpAssignment.STATIC);
            mIpConfig.setProxySettings(IpConfiguration.ProxySettings.NONE);
            mIpConfig.setStaticIpConfiguration(useDhcp ? null : mStaticIpConfig);
            mEm.setConfiguration(mIpConfig);
        } catch (NumberFormatException e) {
        }
        // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
        if (!FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
            mContext.sendBroadcast(new Intent(ACTION_ETHERNET_CONFIG_CHANGED));
        }
        // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mForegroundEditTextPreference == null) {
            return;
        }
        if (mForegroundEditTextPreference.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
            if (EthUtils.validateNetPrefixConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                setDialogPositiveEnabled(mForegroundEditTextPreference, true);
            } else {
                setDialogPositiveEnabled(mForegroundEditTextPreference, false);
            }
        } else {
            if (EthUtils.validateIpConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                setDialogPositiveEnabled(mForegroundEditTextPreference, true);
            } else {
                setDialogPositiveEnabled(mForegroundEditTextPreference, false);
            }
        }
    }

    // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
    /*private final BroadcastReceiver mEthernetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ethernetState = intent.getExtras().getInt(EthernetManager.EXTRA_ETHERNET_STATE);
            updateEthernetSwitch(ethernetState);
            updateEthernetFields();
        }
    };*/

    private Boolean mEthernetSwitchByCode = false;

    private void updateEthernetSwitch(final int ethernetState) {
        if (mEthernetSwitch == null || mEm == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                synchronized (mEthernetSwitchByCode) {
                    mEthernetSwitchByCode = true;
                    boolean ethernetAvailable = EthUtils.isEthernetAvailable(mEm);
                    //boolean ethernetEnabled = (ethernetState == EthernetManager.ETHERNET_STATE_ENABLING) || (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED);
                    mEthernetSwitch.setEnabled(ethernetAvailable);// && (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED) || (ethernetState == EthernetManager.ETHERNET_STATE_DISABLED));
                    mEthernetSwitch.setChecked(ethernetAvailable);// && ethernetEnabled);
                    mEthernetSwitchByCode = false;
                }
            }
        });
    }

    private void updateEthernetFields() {
        // int ethernetState = (mEm != null) ? mEm.getEthernetState() : EthernetManager.ETHERNET_STATE_UNKNOWN;
        final boolean enable = EthUtils.isEthernetAvailable(mEm); // && (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED);
        if ((mEthernetPolicy == null) || (mConnectionType == null) || (mDhcpIpAddress == null) || (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mEthernetPolicy.setEnabled(enable);
                mConnectionType.setEnabled(enable);
                updateDhcpIpAddress();
                mStaticIpAddress.setEnabled(enable);
                mNetworkPrefixLength.setEnabled(enable);
                mGateway.setEnabled(enable);
                mDns1.setEnabled(enable);
                mDns2.setEnabled(enable);
            }
        });
    }
    // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27

    private void setDialogPositiveEnabled(EditTextPreference editPref, boolean enable) {
        if (editPref == null) {
            return;
        }
        AlertDialog alertDialog = (AlertDialog) editPref.getDialog();
        if (alertDialog == null) {
            return;
        }
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enable);
    }

    private void handleEmptyField(EditTextPreference editPref) {
        if (editPref == null) {
            return;
        }
        if ((editPref.getText() != null) && (editPref.getText().length() > 0)) {
            editPref.setSummary(editPref.getText());
            return;
        }
        if (editPref.getKey().equals(KEY_STATIC_IP_ADDRESS)) {
            editPref.setSummary(R.string.ethernet_ip_address_not_set);
        } else if (editPref.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
            editPref.setSummary(R.string.ethernet_network_prefix_length_not_set);
        } else if (editPref.getKey().equals(KEY_GATEWAY)) {
            editPref.setSummary(R.string.ethernet_gateway_not_set);
        } else if (editPref.getKey().equals(KEY_DNS1)) {
            editPref.setSummary(R.string.ethernet_dns1_not_set);
        } else if (editPref.getKey().equals(KEY_DNS2)) {
            editPref.setSummary(R.string.ethernet_dns2_not_set);
        }
    }

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            updateDhcpIpAddress();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            updateDhcpIpAddress();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            updateDhcpIpAddress();
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            updateDhcpIpAddress();
        }

        @Override
        public void onLost(Network network) {
            updateDhcpIpAddress();
        }
    };

    private void updateDhcpIpAddress() {
        if (mDhcpIpAddress == null) {
            return;
        }
        final String ipAddress = EthUtils.getEthernetIpAddresses(mCm);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // sunhuihui@wind-mobi.com modify begin. from usb dialog patch Feature#110139 2016/6/27
                if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
                    // int ethernetState = (mEm != null) ? mEm.getEthernetState() : EthernetManager.ETHERNET_STATE_UNKNOWN;
                    String ipAddress = EthernetUtils.getEthernetIpAddresses(mCm);
                    if (EthUtils.isEthernetAvailable(mEm)) { //&& (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED) && !TextUtils.isEmpty(ipAddress)) {
                        mDhcpIpAddress.setSummary(ipAddress);
                    } else {
                        mDhcpIpAddress.setSummary(mContext.getString(R.string.status_unavailable));
                    }
                } else {
                    mDhcpIpAddress.setSummary(TextUtils.isEmpty(ipAddress) ? mContext.getString(R.string.status_unavailable) : ipAddress);
                }
                // sunhuihui@wind-mobi.com modify end. from usb dialog patch Feature#110139 2016/6/27
            }
        });
    }

    private EthernetManager.Listener mEthernetListener = new EthernetManager.Listener() {
        @Override
        public void onAvailabilityChanged(boolean isAvailable) {
            Log.d(TAG, "EthernetManager.Listener.onAvailabilityChanged");
            if (FeatureOption.WIND_DEF_ASUS_USB_DIALOG) {
                if (mEm != null) {
                    updateEthernetSwitch(0);//mEm.getEthernetState());
                    updateEthernetFields();
                }
            } else {
                mEthernetSwitch.setEnabled(EthUtils.isEthernetAvailable(mEm));
                mEthernetSwitch.setChecked(EthUtils.isEthernetAvailable(mEm) && EthUtils.isEthernetEnabled(mCr));
            }
        }
    };
}

