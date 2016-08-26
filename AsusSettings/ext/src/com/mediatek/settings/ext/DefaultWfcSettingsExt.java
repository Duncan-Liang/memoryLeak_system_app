package com.mediatek.settings.ext;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;


/**
 * Default implementation for WFC Settings plugin
 */
public class DefaultWfcSettingsExt implements IWfcSettingsExt {
    private static final String TAG = "DefaultWfcSettingsExt";

    public static final int RESUME = 0;
    public static final int PAUSE = 1;

    @Override
    /** Initialize plugin with essential values.
     * @param pf preferenceFragment
     * @return
     */
    public void initPlugin(PreferenceFragment pf) {
    }

    @Override
    /** get operator specific customized summary for WFC button.
     * Used in WirelessSettings
     * @param defaultSummaryResId default summary res id
     * @return res id of summary to be displayed
     */
    public String getWfcSummary(Context context, int defaultSummaryResId) {
        return context.getResources().getString(defaultSummaryResId);
    }

    /** Called on events like onResume/onPause etc from WirelessSettings.
    * @param event resume/puase etc.
    * @return
    */
    @Override
    public void onWirelessSettingsEvent(int event) {
    }

    /** Called on events like onResume/onPause etc from WfcSettings.
    * @param event resume/puase etc.
    * @return
    */
    @Override
    public void onWfcSettingsEvent(int event) {
    }

    @Override
    /** Add other WFC prefernce, if any.
    * @param
    * @return
    */
    public void addOtherCustomPreference() {
    }

    @Override
    /** Takes required action on wfc list preference on switch change.
     * @param root preference screen
     * @param wfcModePref AOSP wfcMode preference
     * @param wfcEnabled whether switch on/off
     * @param wfcMode current wfc mode
     * @return
     */
    public void updateWfcModePreference(PreferenceScreen root, ListPreference wfcModePref,
            boolean wfcEnabled, int wfcMode) {
        Log.d(TAG, "wfcEnabled:" + wfcEnabled + "wfcMode:" + wfcMode);
        if (wfcModePref != null) {
            wfcModePref.setSummary(getWfcModeSummary(root.getContext(), wfcMode));
            wfcModePref.setEnabled(wfcEnabled);
            if (wfcEnabled) {
                root.addPreference(wfcModePref);
            } else {
                root.removePreference(wfcModePref);
            }
        }
    }

    @Override
    /** Shows alert dialog to confirm whteher to turn on hotspot or not.
     * @param  context
     * @return  true: if alert is shown, false: if alert is not shown
     */
    public boolean showWfcTetheringAlertDialog(Context context) {
        return false;
    }

    @Override
    /** Customize the WFC settings preference.
     * Used in Wireless Settings
     * @param  context
     * @param  preferenceScreen
     * @return
     */
    public void customizedWfcPreference(Context context, PreferenceScreen preferenceScreen) {

    }

    private int getWfcModeSummary(Context context, int wfcMode) {
        int resId = com.android.internal.R.string.wifi_calling_off_summary;
        if (ImsManager.isWfcEnabledByUser(context)) {
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                    resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }
}
