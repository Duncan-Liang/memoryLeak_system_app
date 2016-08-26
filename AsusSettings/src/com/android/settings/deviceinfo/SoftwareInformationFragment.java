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
import com.android.settings.DevelopmentSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.util.ResCustomizeConfig;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SELinux;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoftwareInformationFragment extends SettingsPreferenceFragment implements Indexable,OnPreferenceClickListener {

    private static final String LOG_TAG = "SoftwareInformationFragment";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";

    protected static final String KEY_KERNEL_VERSION = "kernel_version";
    protected static final String KEY_BUILD_NUMBER = "build_number";
    protected static final String KEY_SELINUX_STATUS = "selinux_status";
    protected static final String KEY_BASEBAND_VERSION = "baseband_version";

    protected static final String KEY_APP_VERSION = "ro.build.app.version";
    protected static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String VERSION_USERDEBUG = "UD";
//    private static final int MSG_PFS_FW_STATE = 0;

    // youxiaoyan@wind-mobi.com feature#110139 2016/8/9 begin
    private static final String COUNTRY_CODE = Build.COUNTRYCODE.toLowerCase();
    private static final String SKU = Build.ASUSSKU.toLowerCase();
    private static final String WW_SKU = "ww";
    private static final String INDONESIA_COUNTRY_CODE = "id";
    private static final String INDONESIA_TIMEZOME = "WIB";
    private static final String PC_NAME = "Tsm-0@tsm0-Z9PE-D8";
    // youxiaoyan@wind-mobi.com feature#110139 2016/8/9 end

    static final int TAPS_TO_BE_A_DEVELOPER = 7;

    int mDevHitCountdown;
    Toast mDevHitToast;

    private int mRetryCount = 0;
    // ---

    //private int mPFStationState = DockManager.EXTRA_PADDOCK_STATE_HDMI_REMOVE;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_software);

        PackageManager pm = getPackageManager();

        setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        String DeviceInfoDefault = getResources().getString(R.string.device_info_default);
        //for CTA: Kernel version: Append UD when userdebug
        //         Build number: BSP customized
        if (isCTA() && Utils.isCNSKU()) {
            setStringSummary(KEY_KERNEL_VERSION,
                    getFormattedKernelVersion().concat(
                            Build.TYPE.equals("userdebug") ? " " + VERSION_USERDEBUG : ""));
            setStringSummary(KEY_BUILD_NUMBER,
                    ResCustomizeConfig.getProperty(KEY_BUILD_NUMBER, DeviceInfoDefault));
        } else {
            setStringSummary(KEY_KERNEL_VERSION, getFormattedKernelVersion());
            setStringSummary(KEY_BUILD_NUMBER, generateFormatBuildNumber());
        }

        findPreference(KEY_BUILD_NUMBER).setEnabled(true);

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        setPropertySummary(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;

    }

    @Override
    public void onPause() {
        super.onPause();
        // Only display EC version on device which has keyboard feature

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
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
        if(findPreference(preference) == null) return ;
        try {
                findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        // youxiaoyan@wind-mobi.com feature#110139 2016/8/9 begin
        /*return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
            m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
            m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
            */

        //hard code change PC name & Timezome for indonesia in WW-sku
        if (SKU.equals(WW_SKU) && COUNTRY_CODE.equals(INDONESIA_COUNTRY_CODE)) {
            //replace timezome to WIB
            String[] splitGroupFour = m.group(4).split(" ");
            splitGroupFour[4] = INDONESIA_TIMEZOME;
            StringBuilder kernelInfoGropFour = new StringBuilder();
            for (String s : splitGroupFour) {
                kernelInfoGropFour.append(s + " ");
            }
            return m.group(1) + "\n" +                  // 3.0.31-g6fb96c9
                    PC_NAME + " " + m.group(3) + "\n" + // Tsm-0@tsm0-Z9PE-D8 #1
                    kernelInfoGropFour.toString();      // Thu Jun 28 11:02:39 WIB 2012
        } else {
            return m.group(1) + "\n" +                     // 3.0.31-g6fb96c9
                    m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
                    m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
        }
        // youxiaoyan@wind-mobi.com feature#110139 2016/8/9 end
    }

    private boolean isCTA(){
        return ResCustomizeConfig.getBooleanConfig("isCTA", getResources()
                .getBoolean(R.bool.def_isCTA));
    }

    private String generateFormatBuildNumber(){
        String amaxVersion = SystemProperties.get(KEY_APP_VERSION);
        String productname = SystemProperties.get("ro.product.name", "");
        return amaxVersion.isEmpty() || productname.toLowerCase().startsWith("att") ? Build.DISPLAY
                : new StringBuilder(Build.DISPLAY).append("\n")
                        .append(amaxVersion).append("\n")
                        .append(Utils.getSKUEncode())
                        .append(Utils.getCountryEncode(getActivity()))
                        .append(Utils.getCIDEncode(getActivity())).toString();
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
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
        };

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.DEVICEINFO;
    }
}

