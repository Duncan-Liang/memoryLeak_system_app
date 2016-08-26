/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.SparseArray;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ZenRule;
import android.app.AlertDialog;
//add by sunxiaolong@wind-mobi.com for #112054 begin
import android.widget.ScrollView;
//add by sunxiaolong@wind-mobi.com for #112054 end

import com.android.settings.Utils;
import java.util.Arrays;
import java.util.Comparator;
// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e

public class ZenModeSettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_AUTOMATION_SETTINGS = "automation_settings";

    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
    private static final String KEY_OTHERS = "others";
    private static final String KEY_SEND_NOTIFICATIONS = "send_notifications";
    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e

    private Preference mPrioritySettings;

    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
    private PreferenceCategory mOthers;
    private SwitchPreference mSendNotifications;

    private static final String KEY_ZEN_MODE2 = "zen_mode2";
    private ListPreference mZenMode2;

    private Preference mAutomationSettings;

    private static final boolean WIND_DEF_ASUS_NOT_DISTURB = SystemProperties.get("ro.wind.def.asus.notdisturb").equals("1");
    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mPrioritySettings = root.findPreference(KEY_PRIORITY_SETTINGS);
        if (!isScheduleSupported(mContext)) {
            removePreference(KEY_AUTOMATION_SETTINGS);
        }

        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (WIND_DEF_ASUS_NOT_DISTURB) {
            mOthers = (PreferenceCategory) root.findPreference(KEY_OTHERS);
            mSendNotifications = (SwitchPreference) mOthers.findPreference(KEY_SEND_NOTIFICATIONS);
            mSendNotifications.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean val = (Boolean) newValue;
                    Global.putInt(mContext.getContentResolver(), "ZENMODE_SEND_NOTIFICATION", val ? 1 : 0);
                    return true;
                }
            });

            mZenMode2 = (ListPreference) root.findPreference(KEY_ZEN_MODE2);
            mZenMode2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int value = Integer.valueOf((String) newValue);
                    final int oldValue = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
                    if (value != oldValue) {
                        Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, value);
                        updateZenModeSummary(value);
                        mZenMode2.setValue(String.valueOf(value));
                        if (value != Global.ZEN_MODE_OFF) {
                            showConditionSelection(value, oldValue);
                        } else {
                            setZenMode(value, null);
                        }
                    }
                    return true;
                }
            });
            if (!Utils.isVoiceCapable(mContext)) {
                mZenMode2.setTitle(R.string.zen_mode_manual_turn_on);
                mZenMode2.setDialogTitle(R.string.zen_mode_manual_turn_on);
            }

            mAutomationSettings = root.findPreference(KEY_AUTOMATION_SETTINGS);
        } else {
            removePreference(KEY_ZEN_MODE2);
            removePreference("others");
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
    }

    @Override
    public void onResume() {
        super.onResume();

        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (WIND_DEF_ASUS_NOT_DISTURB) {
            updateZenModeSummary(Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF));
            final int value = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
            mZenMode2.setValue(String.valueOf(value));
            updateAutomationRuleSummary();
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
        updateControls();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
        updateControls();
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (WIND_DEF_ASUS_NOT_DISTURB) {
            final int sendNotification = Global.getInt(mContext.getContentResolver(), "ZENMODE_SEND_NOTIFICATION", 1);
            mSendNotifications.setChecked(sendNotification == 1);
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
    }

    private void updatePrioritySettingsSummary() {
        final boolean callers = mConfig.allowCalls || mConfig.allowRepeatCallers;
        String s = getResources().getString(R.string.zen_mode_alarms);
        s = appendLowercase(s, mConfig.allowReminders, R.string.zen_mode_reminders);
        s = appendLowercase(s, mConfig.allowEvents, R.string.zen_mode_events);
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (Utils.isVoiceCapable(mContext)) {
            s = appendLowercase(s, callers, R.string.zen_mode_selected_callers2);
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        mPrioritySettings.setSummary(s);
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s,
                    getResources().getString(resId).toLowerCase());
        }
        return s;
    }

    private static SparseArray<String> allKeyTitles(Context context) {
        final SparseArray<String> rt = new SparseArray<String>();
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        rt.put(R.string.zen_mode_important_category2, KEY_PRIORITY_SETTINGS);
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
        rt.put(R.string.zen_mode_automation_settings_title, KEY_AUTOMATION_SETTINGS);
        return rt;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    // Enable indexing of searchable data
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final SparseArray<String> keyTitles = allKeyTitles(context);
                    final int N = keyTitles.size();
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>(N);
                    final Resources res = context.getResources();
                    for (int i = 0; i < N; i++) {
                        final SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.key = keyTitles.valueAt(i);
                        data.title = res.getString(keyTitles.keyAt(i));
                        data.screenTitle = res.getString(R.string.zen_mode_settings_title);
                        result.add(data);
                    }
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final ArrayList<String> rt = new ArrayList<String>();
                    if (!isScheduleSupported(context)) {
                        rt.add(KEY_AUTOMATION_SETTINGS);
                    }
                    return rt;
                }
            };

    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
    private void updateZenModeSummary(int value) {
        if (mZenMode2 != null) {
            mZenMode2.setValueIndex(value);
            String summary = "";
            switch (value) {
                case Global.ZEN_MODE_OFF:
                    summary = getResources().getString(R.string.gadget_state_off);
                    break;
                case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                    summary = getResources().getString(R.string.zen_mode_option_important_interruptions2);
                    break;
                case Global.ZEN_MODE_NO_INTERRUPTIONS:
                    summary = getResources().getString(R.string.zen_mode_option_no_interruptions);
                    break;
                case Global.ZEN_MODE_ALARMS:
                    summary = getResources().getString(R.string.zen_mode_option_alarms);
                    break;
            }
            mZenMode2.setSummary(summary);
        }
    }

    protected void showConditionSelection(final int newSettingsValue, final int oldSettingsValue) {

        final ZenModeConditionSelection zenModeConditionSelection =
                new ZenModeConditionSelection(mContext, newSettingsValue);
        //add by sunxiaolong@wind-mobi.com for #112054 begin
        final ScrollView mScrollView = new ScrollView(mContext);
        mScrollView.addView(zenModeConditionSelection);
        //add by sunxiaolong@wind-mobi.com for #112054 end
        String title;
        switch (newSettingsValue) {
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                title = getResources().getString(R.string.zen_mode_option_no_interruptions);
                break;
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                title = getResources().getString(R.string.zen_mode_option_important_interruptions2);
                break;
            case Global.ZEN_MODE_ALARMS:
                title = getResources().getString(R.string.zen_mode_option_alarms);
                break;
            default:
                title = getResources().getString(R.string.gadget_state_off);
                break;
        }

        AlertDialog selectOption = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                //modify by sunxiaolong@wind-mobi.com for #112054 begin
                .setView(mScrollView)
                //.setView(zenModeConditionSelection)
                //modify by sunxiaolong@wind-mobi.com for #112054 end
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        zenModeConditionSelection.confirmCondition();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, oldSettingsValue);
                        updateZenModeSummary(oldSettingsValue);
                        dialog.dismiss();
                        mZenMode2.setValue(String.valueOf(oldSettingsValue));
                    }
                }).create();
        selectOption.show();
    }

    private static class ZenRuleInfo {
        String id;
        ZenRule rule;
    }

    private static final Comparator<ZenRuleInfo> RULE_COMPARATOR = new Comparator<ZenRuleInfo>() {
        @Override
        public int compare(ZenRuleInfo lhs, ZenRuleInfo rhs) {
            return key(lhs).compareTo(key(rhs));
        }

        private String key(ZenRuleInfo zri) {
            final ZenRule rule = zri.rule;
            final int type = ZenModeConfig.isValidScheduleConditionId(rule.conditionId) ? 1
                    : ZenModeConfig.isValidEventConditionId(rule.conditionId) ? 2
                    : 3;
            return type + rule.name;
        }
    };

    private ZenRuleInfo[] sortedRules() {
        final ArrayList<ZenRuleInfo> rte = new ArrayList<>();
        for (int i = 0; i < mConfig.automaticRules.size(); i++) {
            final ZenRuleInfo zri = new ZenRuleInfo();
            zri.id = mConfig.automaticRules.keyAt(i);
            zri.rule = mConfig.automaticRules.valueAt(i);
            if (zri.rule.enabled) rte.add(zri);
        }
        final ZenRuleInfo[] rt = rte.toArray(new ZenRuleInfo[0]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private String append(String s, boolean condition, String string) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s, string);
        }
        return s;
    }

    private void updateAutomationRuleSummary() {
        final ZenRuleInfo[] sortedRules = sortedRules();
        String summary = getResources().getString(R.string.zen_mode_in_activity)
                + (sortedRules.length != 0 ? sortedRules[0].rule.name : getResources().getString(R.string.zen_mode_from_none));
        for (int i = 1; i < sortedRules.length; i++) {
            summary = append(summary, true, sortedRules[i].rule.name);
        }
        mAutomationSettings.setSummary(summary);
    }
    // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
}
