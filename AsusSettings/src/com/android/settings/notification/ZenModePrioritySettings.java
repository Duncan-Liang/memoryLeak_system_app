/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.service.notification.ZenModeConfig;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.search.Indexable;
// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
import android.os.SystemProperties;
import com.android.settings.Utils;
// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s

public class ZenModePrioritySettings extends ZenModeSettingsBase implements Indexable {
    private static final String KEY_REMINDERS = "reminders";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_REPEAT_CALLERS = "repeat_callers";

    private static final int SOURCE_NONE = -1;

    private boolean mDisableListeners;
    private SwitchPreference mReminders;
    private SwitchPreference mEvents;
    private DropDownPreference mMessages;
    private DropDownPreference mCalls;
    private SwitchPreference mRepeatCallers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.zen_mode_priority_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mReminders = (SwitchPreference) root.findPreference(KEY_REMINDERS);
        mReminders.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ALLOW_REMINDERS, val);
                if (val == mConfig.allowReminders) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowReminders=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowReminders = val;
                return setZenModeConfig(newConfig);
            }
        });

        mEvents = (SwitchPreference) root.findPreference(KEY_EVENTS);
        mEvents.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mDisableListeners) return true;
                final boolean val = (Boolean) newValue;
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ALLOW_EVENTS, val);
                if (val == mConfig.allowEvents) return true;
                if (DEBUG) Log.d(TAG, "onPrefChange allowEvents=" + val);
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowEvents = val;
                return setZenModeConfig(newConfig);
            }
        });

        mMessages = (DropDownPreference) root.findPreference(KEY_MESSAGES);
        addSources(mMessages);
        mMessages.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object newValue) {
                if (mDisableListeners) return true;
                final int val = (Integer) newValue;
                MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ALLOW_MESSAGES, val);
                final boolean allowMessages = val != SOURCE_NONE;
                final int allowMessagesFrom = val == SOURCE_NONE ? mConfig.allowMessagesFrom : val;
                if (allowMessages == mConfig.allowMessages
                        && allowMessagesFrom == mConfig.allowMessagesFrom) {
                    return true;
                }
                if (DEBUG) Log.d(TAG, "onPrefChange allowMessages=" + allowMessages
                        + " allowMessagesFrom=" + ZenModeConfig.sourceToString(allowMessagesFrom));
                final ZenModeConfig newConfig = mConfig.copy();
                newConfig.allowMessages = allowMessages;
                newConfig.allowMessagesFrom = allowMessagesFrom;
                return setZenModeConfig(newConfig);
            }
        });

        mCalls = (DropDownPreference) root.findPreference(KEY_CALLS);
		// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (Utils.isVoiceCapable(mContext)) {// chunghung_lin@asus.com
            addSources(mCalls);
            mCalls.setCallback(new DropDownPreference.Callback() {
                @Override
                public boolean onItemSelected(int pos, Object newValue) {
                    if (mDisableListeners) return true;
                    final int val = (Integer) newValue;
                    MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ALLOW_CALLS, val);
                    final boolean allowCalls = val != SOURCE_NONE;
                    final int allowCallsFrom = val == SOURCE_NONE ? mConfig.allowCallsFrom : val;
                    if (allowCalls == mConfig.allowCalls
                            && allowCallsFrom == mConfig.allowCallsFrom) {
                        return true;
                    }
                    if (DEBUG) Log.d(TAG, "onPrefChange allowCalls=" + allowCalls
                            + " allowCallsFrom=" + ZenModeConfig.sourceToString(allowCallsFrom));
                    final ZenModeConfig newConfig = mConfig.copy();
                    newConfig.allowCalls = allowCalls;
                    newConfig.allowCallsFrom = allowCallsFrom;
                    return setZenModeConfig(newConfig);
                }
            });
        } else {
            root.removePreference(mCalls);
            mCalls = null;
        }

        mRepeatCallers = (SwitchPreference) root.findPreference(KEY_REPEAT_CALLERS);

        if (Utils.isVoiceCapable(mContext)) {// chunghung_lin@asus.com
            mRepeatCallers.setSummary(mContext.getString(R.string.zen_mode_repeat_callers_summary2,
                    mContext.getResources().getInteger(com.android.internal.R.integer
                            .config_zen_repeat_callers_threshold)));
            mRepeatCallers.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (mDisableListeners) return true;
                    final boolean val = (Boolean) newValue;
                    MetricsLogger.action(mContext, MetricsLogger.ACTION_ZEN_ALLOW_REPEAT_CALLS, val);
                    if (val == mConfig.allowRepeatCallers) return true;
                    if (DEBUG) Log.d(TAG, "onPrefChange allowRepeatCallers=" + val);
                    final ZenModeConfig newConfig = mConfig.copy();
                    newConfig.allowRepeatCallers = val;
                    return setZenModeConfig(newConfig);
                }
            });
        } else {
            root.removePreference(mRepeatCallers);
            mRepeatCallers = null;
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e

        updateControls();
    }

    @Override
    protected void onZenModeChanged() {
        // don't care
    }

    @Override
    protected void onZenModeConfigChanged() {
        updateControls();
    }

    private void updateControls() {
        mDisableListeners = true;
        if (mCalls != null) {
            mCalls.setSelectedValue(mConfig.allowCalls ? mConfig.allowCallsFrom : SOURCE_NONE);
        }
        mMessages.setSelectedValue(mConfig.allowMessages ? mConfig.allowMessagesFrom : SOURCE_NONE);
        mReminders.setChecked(mConfig.allowReminders);
        mEvents.setChecked(mConfig.allowEvents);
		// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        if (mRepeatCallers != null) {
            mRepeatCallers.setChecked(mConfig.allowRepeatCallers);
            mRepeatCallers.setEnabled(!mConfig.allowCalls
                    || mConfig.allowCallsFrom != ZenModeConfig.SOURCE_ANYONE);
        }
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
        mDisableListeners = false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_PRIORITY;
    }

    private static void addSources(DropDownPreference pref) {
		// wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -s
        pref.addItem(R.string.zen_mode_from_anyone3, ZenModeConfig.SOURCE_ANYONE);
        pref.addItem(R.string.zen_mode_from_contacts_only, ZenModeConfig.SOURCE_CONTACT);
        pref.addItem(R.string.zen_mode_from_favorites_only, ZenModeConfig.SOURCE_STAR);
        // wangyan@wind-mobi.com add 2016/05/10 Feature#110152 -e
        pref.addItem(R.string.zen_mode_from_none, SOURCE_NONE);
    }

}
