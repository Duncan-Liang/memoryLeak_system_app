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

package com.android.settings.fingerprint;


import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo; //asus add for fingerprint
import android.preference.*; 	//asus add for fingerprint
import android.provider.Settings; //asus add for fingerprint
import com.android.settings.SettingsPreferenceFragment;	//asus add for fingerprint
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;

import java.util.List;
//add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.CheckBox;
import com.android.settings.Utils;
//add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings {

    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

    /**
     * Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
     * will be useful if the user accidentally entered this flow.
     */
    protected static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

    /**
     * Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
     * device was left idle. This is used to clear the credential token to require the user to
     * re-enter their pin/pattern/password before continuing.
     */
    protected static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;

    private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.fingerprint_manage_category_title);
        setTitle(msg);
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
        private static final int MAX_RETRY_ATTEMPTS = 20;
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
		//asus add for fingerprint 20160701 start
        //++ ASUS fingerprint key
        private SwitchPreference mUnlockDevice;

        private static final String KEY_FINGERPRINT_MANAGEMENT_CATEGORY = "fingerprint_management";
        private static final String KEY_FINGERPRINT_UNLOCK_SWITCH = "unlock_device_with_fingerprint";

        private static final String UNLOCK_DEVICE_ENABLED = "unlock_device_with_fingerprint";

        private static final int ADD_FINGERPRINT_FROM_UNLOCK_REQUEST = 10001;
        //-- ASUS fingerprint key
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
		// Asus fingerprint key for Phone
        private static final String KEY_SECURE_WITH_FINGERPRINT_CATEGORY = "secure_with_fingerprint";
        private SwitchPreference mAnswerCall = null;
        private SwitchPreference mSnapCall = null;
        private static final String KEY_FINGERPRINT_ANSWER_CALL_SWITCH = "answer_call_with_fingerprint";
        private static final String KEY_FINGERPRINT_SNAP_CALL_SWITCH = "snap_call_with_fingerprint";
        private static final String ASUS_FINGERPRINT_ANSWER_CALL = "asus_fingerprint_id_answer_call";
        private static final String ASUS_FINGERPRINT_SNAP_CALL = "asus_fingerprint_id_snap_call";
        private static final String INTENT_SNAP_CALL_EXIST = "com.asus.contacts.SNAP_CALL_EXIST";
		//asus add for fingerprint 20160701 start
		
        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        private static final String KEY_SKIP_SHOW_CALL_LOCK_DETAIL = "skip_show_call_lock_detail";
        private static final String KEY_SKIP_SHOW_LONG_TAP_WARNING = "skip_show_long_tap_warning";
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private CancellationSignal mFingerprintCancel;
        private boolean mInFingerprintLockout;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private Drawable mHighlightDrawable;

        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
            }

            @Override
            public void onAuthenticationFailed() {
                mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
            };

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                        .sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                        .sendToTarget();
            }
        };
        private RemovalCallback mRemoveCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                        updateAddPreference();
                        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                        updateFingerprintUseFor();  //update asus feature toggle.
                        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        mFingerprintCancel = null;
                        highlightFingerprintItem(msg.arg1);
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_FAIL:
                        // No action required... fingerprint will allow up to 5 of these
                    break;
                    case MSG_FINGER_AUTH_ERROR:
                        handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */ );
                    break;
                    case MSG_FINGER_AUTH_HELP: {
                        // Not used
                    }
                    break;
                }
            };
        };

        private void stopFingerprint() {
            if (mFingerprintCancel != null && !mFingerprintCancel.isCanceled()) {
                mFingerprintCancel.cancel();
            }
            mFingerprintCancel = null;
        }

        /**
         * @param errMsgId
         */
        protected void handleError(int errMsgId, CharSequence msg) {
            mFingerprintCancel = null;
            switch (errMsgId) {
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                    return; // Only happens if we get preempted by another activity. Ignored.
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                    mInFingerprintLockout = true;
                    // We've been locked out.  Reset after 30s.
                    if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                        mHandler.postDelayed(mFingerprintLockoutReset,
                                LOCKOUT_DURATION);
                    }
                    // Fall through to show message
                default:
                    // Activity can be null on a screen rotation.
                    final Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, msg , Toast.LENGTH_SHORT);
                    }
                break;
            }
            retryFingerprint(); // start again
        }

        private void retryFingerprint() {
            if (!mInFingerprintLockout) {
                //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                if(mFingerprintManager.hasEnrolledFingerprints()) {
                    mFingerprintCancel = new CancellationSignal();
                    mFingerprintManager.authenticate(null, mFingerprintCancel, 0 /* flags */,
                            mAuthCallback, null);
                }
                //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            }
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
            }

            Activity activity = getActivity();
            mFingerprintManager = (FingerprintManager) activity.getSystemService(
                    Context.FINGERPRINT_SERVICE);

            // Need to authenticate a session token if none
            if (mToken == null && mLaunchedConfirm == false) {
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            if(SystemProperties.get("ro.wind.def.adapt_asus_apk_cn").equals("1")) {
                TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(
                        R.layout.fingerprint_settings_footer, null);
                v.setText(LearnMoreSpan.linkify(getText(isFingerprintDisabled()
                                ? R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled
                                : R.string.security_settings_fingerprint_enroll_disclaimer),
                        getString(getHelpResource())));
                v.setMovementMethod(new LinkMovementMethod());
                getListView().addFooterView(v);
                getListView().setFooterDividersEnabled(false);
            }
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
        }

        private boolean isFingerprintDisabled() {
            final DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                PreferenceGroup group = (PreferenceGroup)getPreferenceScreen().findPreference(
                        KEY_FINGERPRINT_MANAGEMENT_CATEGORY);
                if (!group.removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
        }

        /**
         * Important!
         *
         * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
         * logic or adding/removing preferences here.
         */
        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            root = getPreferenceScreen();
            PreferenceGroup items = (PreferenceGroup)root.findPreference(
                    KEY_FINGERPRINT_MANAGEMENT_CATEGORY);
            addFingerprintItemPreferences(items);
            mUnlockDevice = (SwitchPreference) root.findPreference(
                    KEY_FINGERPRINT_UNLOCK_SWITCH);
            // Asus fingerprint key for Phone
            PreferenceGroup switch_items = (PreferenceGroup)root.findPreference(KEY_SECURE_WITH_FINGERPRINT_CATEGORY);
            mAnswerCall = (SwitchPreference) root.findPreference(KEY_FINGERPRINT_ANSWER_CALL_SWITCH);
            mSnapCall = (SwitchPreference) root.findPreference(KEY_FINGERPRINT_SNAP_CALL_SWITCH);

            List<ResolveInfo> appsInfo = getPackageManager().queryBroadcastReceivers(
                    new Intent(INTENT_SNAP_CALL_EXIST), 0);
            if (appsInfo.isEmpty()) { // receiver doesn't exist
                switch_items.removePreference(mSnapCall);
            }
            // +++ ian_tsai@20160627 remove preference if voice is not capable
            if (!Utils.isVoiceCapable(getActivity())) {
                switch_items.removePreference(mAnswerCall);
            }
            if(SystemProperties.get("ro.wind.def.adapt_asus_apk_cn").equals("1")) {
                root.removePreference(switch_items);
            }
            // --- ian_tsai@20160627 remove preference if voice is not capable
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            return root;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            if (root != null) {
                root.removeAll();
            }
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            addPreference.setTitle(R.string.asus_security_settings_fingerprint_enroll_introduction_title);
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            addPreference.setIcon(R.drawable.ic_add_24dp);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
            updateAddPreference();
        }

        private void updateAddPreference() {
            /* Disable preference if too many fingerprints added */
            final int max = getContext().getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            boolean tooMany = mFingerprintManager.getEnrolledFingerprints().size() >= max;
            CharSequence maxSummary = tooMany ?
                    getContext().getString(R.string.fingerprint_add_max, max) : "";
            Preference addPreference = findPreference(KEY_FINGERPRINT_ADD);
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(!tooMany);
			
			// Asus fingerprint key for Phone
            boolean isEnable = mFingerprintManager.getEnrolledFingerprints().size() > 0;

             if (mAnswerCall != null) {
                 mAnswerCall.setChecked(Settings.Global.getInt(
                         getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, 0) != 0);
 
                 mAnswerCall.setEnabled(isEnable);
             }
 
             if (mSnapCall != null) {
                 mSnapCall.setChecked(Settings.Global.getInt(
                         getContentResolver(), ASUS_FINGERPRINT_SNAP_CALL, 0) != 0);
 
                 mSnapCall.setEnabled(isEnable);
             }
			//asus add end
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            updatePreferences();
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint();
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            updateFingerprintUseFor();
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

        }

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        private void updateFingerprintUseFor(){
            boolean isEnable = mFingerprintManager.getEnrolledFingerprints().size() > 0;
            if (mUnlockDevice != null) {
                if(isFingerprintDisabled()){
                    mUnlockDevice.setEnabled(false);
                    mUnlockDevice.setSummary(R.string.asus_security_settings_fingerprint_disabled_on_lockscreen);
                }else{
                    mUnlockDevice.setChecked(Settings.Secure.getInt(
                            getContentResolver(), UNLOCK_DEVICE_ENABLED, 1) != 0);
                    mUnlockDevice.setEnabled(isEnable);
                }
            }

            // Asus fingerprint key for Phone
            if (mAnswerCall != null) {
                mAnswerCall.setChecked(Settings.Global.getInt(
                        getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, 0) != 0);

                mAnswerCall.setEnabled(isEnable);
            }

            if (mSnapCall != null) {
                mSnapCall.setChecked(Settings.Global.getInt(
                        getContentResolver(), ASUS_FINGERPRINT_SNAP_CALL, 0) != 0);

                mSnapCall.setEnabled(isEnable);
            }
        }
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
                Intent intent = new Intent();
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                //intent.setClassName("com.android.settings",
                //        FingerprintEnrollEnrolling.class.getName());
                //Asus fingerprint flow alawys show find sensor view
                intent.setClassName("com.android.settings",
                        FingerprintEnrollFindSensor.class.getName());
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp =fpref.getFingerprint();
                showRenameDeleteDialog(fp);
                return super.onPreferenceTreeClick(preferenceScreen, pref);
            }
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            //++ Asus features add here
            else if (KEY_FINGERPRINT_UNLOCK_SWITCH.equals(key)){
                if(mUnlockDevice.isEnabled()){
                    Settings.Secure.putInt(getContentResolver(),UNLOCK_DEVICE_ENABLED, isToggled(pref)? 1: 0);
                }
            }
            //-- Asus features add here
            // Asus fingerprint key for Phone
            else if (KEY_FINGERPRINT_ANSWER_CALL_SWITCH.equals(key)){
                if (mAnswerCall.isEnabled()) {
                    // +++ ian_tsai@20160608 show dialog when user turn on incoming call lock
                    if (isToggled(pref)) {
                        ShowCallLockDetailDialog(pref);
                    } else {
                        Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                    }
                    // --- ian_tsai@20160608 show dialog when user turn on incoming call lock
                }
            }
            else if (KEY_FINGERPRINT_SNAP_CALL_SWITCH.equals(key)){
                if(mSnapCall.isEnabled()){
                    Settings.Global.putInt(getContentResolver(),ASUS_FINGERPRINT_SNAP_CALL, isToggled(pref)? 1: 0);
                }
            }
            //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end

            return true;
        }
		//me add 3
		//asus add start
		/*private boolean isToggled(Preference pref) {
			return ((SwitchPreference) pref).isChecked();
		}*/
		private boolean isToggled(Preference pref) {
            if(pref instanceof SwitchPreference)
                return ((SwitchPreference) pref).isChecked();
            return false;
        }
		//asus add end 
        private void showRenameDeleteDialog(final Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog(getContext());
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                // TODO
            } else {
                Log.v(TAG, "Unknown key:" + key);
            }
            return result;
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    if (data != null) {
                        mToken = data.getByteArrayExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    }
                    //qiancheng@wind-mobi.com 20160729 add for bug 123334 start
                    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                    //maybeLauncheIntroduction();
                    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
                    //qiancheng@wind-mobi.com 20160729 add for bug 123334 end
                }
            } else if (requestCode == ADD_FINGERPRINT_REQUEST) {
                if (resultCode == RESULT_TIMEOUT) {
                    Activity activity = getActivity();
                    activity.setResult(RESULT_TIMEOUT);
                    activity.finish();
                }
            }

            if (mToken == null) {
                // Didn't get an authentication, finishing
                getActivity().finish();
            }
        }

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        private void maybeLauncheIntroduction(){
            FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                    Context.FINGERPRINT_SERVICE);
            final List<Fingerprint> items = fpm.getEnrolledFingerprints();
            final int fingerprintCount = items != null ? items.size() : 0;
            if (fingerprintCount == 0){
                Intent intent = new Intent();
                String clazz = FingerprintEnrollIntroduction.class.getName();
                intent.setClassName("com.android.settings", clazz);
                //fix double confirm password and token are inconsistent
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivity(intent);
            }
        }
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (getActivity().isFinishing()) {
                int result = mFingerprintManager.postEnroll();
                if (result < 0) {
                    Log.w(TAG, "postEnroll failed: result = " + result);
                }
            }
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            final Drawable highlight = getHighlightDrawable();
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            if(fpref == null){
                Log.d(TAG, "highlightFingerprintItem fingerprint id " + fpId + " is not found");
                return;
            }
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            if (highlight != null) {
                final View view = fpref.getView();
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                if(view != null) {
                    final int centerX = view.getWidth() / 2;
                    final int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setBackground(highlight);
                    view.setPressed(true);
                    view.setPressed(false);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.setBackground(null);
                        }
                    }, RESET_HIGHLIGHT_DELAY_MS);
                }
                //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge)) {
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
                //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);  //fix issue:
                //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }

        private void deleteFingerPrint(Fingerprint fingerPrint) {
            mFingerprintManager.remove(fingerPrint, mRemoveCallback);
        }

        private void renameFingerPrint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, newName);
            updatePreferences();
        }

        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                mInFingerprintLockout = false;
                retryFingerprint();
            }
        };

        public static class RenameDeleteDialog extends DialogFragment {
            // wangyan@wind-mobi.com modify 2016/06/09 for bug #115505 -s
            private Context mContext;
            // wangyan@wind-mobi.com modify 2016/06/09 for bug #115505 -e
            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;

            public RenameDeleteDialog(Context context) {
                mContext = context;
            }

            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
            // wangyan@wind-mobi.com add 2016/06/09 for bug #115505 -s
            public RenameDeleteDialog() {
                super();
                mContext = getActivity();
            }
            // wangyan@wind-mobi.com add 2016/06/09 for bug #115505 -e
            //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    mFingerName = savedInstanceState.getString("fingerName");
                    mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
                    mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(R.layout.fingerprint_rename_dialog)
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!newName.equals(name)) {
                                            if (DEBUG) {
                                                Log.v(TAG, "rename " + name + " to " + newName);
                                            }
                                            MetricsLogger.action(getContext(),
                                                    MetricsLogger.ACTION_FINGERPRINT_RENAME,
                                                    mFp.getFingerId());
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getFingerId(),
                                                    newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteClick(dialog);
                                    }
                                })
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mDialogTextField = (EditText) alertDialog.findViewById(
                                R.id.fingerprint_rename_field);
                        CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
                        mDialogTextField.setText(name);
                        if (mTextHadFocus == null) {
                            mDialogTextField.selectAll();
                        } else {
                            mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
                        }
                    }
                });
                if (mTextHadFocus == null || mTextHadFocus) {
                    // Request the IME
                    alertDialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                return alertDialog;
            }

            private void onDeleteClick(DialogInterface dialog) {
                if (DEBUG) Log.v(TAG, "Removing fpId=" + mFp.getFingerId());
                MetricsLogger.action(getContext(), MetricsLogger.ACTION_FINGERPRINT_DELETE,
                        mFp.getFingerId());
                FingerprintSettingsFragment parent
                        = (FingerprintSettingsFragment) getTargetFragment();
                if (parent.mFingerprintManager.getEnrolledFingerprints().size() > 1) {
                    parent.deleteFingerPrint(mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", mFp);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(),
                            ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (mDialogTextField != null) {
                    outState.putString("fingerName", mDialogTextField.getText().toString());
                    outState.putBoolean("textHadFocus", mDialogTextField.hasFocus());
                    outState.putInt("startSelection", mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
                }
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {

            private Fingerprint mFp;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_last_delete_title)
                        .setMessage(R.string.fingerprint_last_delete_message)
                        .setPositiveButton(R.string.fingerprint_last_delete_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FingerprintSettingsFragment parent
                                                = (FingerprintSettingsFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                return alertDialog;
            }
        }

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        // +++ ian_tsai@20160608 show dialog when user turn on incoming call lock
        public void ShowCallLockDetailDialog(final Preference pref) {
            boolean isDialogSkip = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(KEY_SKIP_SHOW_CALL_LOCK_DETAIL, false);
            Log.d(FingerprintSettingsFragment.TAG, "ShowCallLockDetailDialog isDialogSkip: " + String.valueOf(isDialogSkip));

            if (isDialogSkip) {
                Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                ShowLongTapWarningDialog();
            } else {
                final View checkboxLayout = getActivity().getLayoutInflater().inflate(R.layout.app_checkbox_never_show_again, null);
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(checkboxLayout)
                        .setMessage(R.string.asus_call_lock_detail)
                        .setPositiveButton(R.string.okay,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                        if (dontShowAgain.isChecked()) {
                                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().
                                                    putBoolean(KEY_SKIP_SHOW_CALL_LOCK_DETAIL, true).commit();
                                        }

                                        ShowLongTapWarningDialog();
                                        Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                        if (dontShowAgain.isChecked()) {
                                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().
                                                    putBoolean(KEY_SKIP_SHOW_CALL_LOCK_DETAIL, true).commit();
                                        }
                                        if (pref instanceof SwitchPreference) {
                                            ((SwitchPreference) pref).setChecked(false);
                                            Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        //A: huangyouzhong@wind-mobi.com 20160818 for 110108 -s
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (pref instanceof SwitchPreference) {
                                    ((SwitchPreference) pref).setChecked(false);
                                    Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                }
                            }
                        })
                        //A: huangyouzhong@wind-mobi.com 20160818 for 110108 -e
                        .show();
            }

        }
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        public void ShowLongTapWarningDialog() {
            boolean isLongTapMode = ((Settings.Global.getInt(getContentResolver(),
                    Settings.Global.ASUS_FINGERPRINT_LONG_PRESS, 0)) != 0);
            boolean isDialogSkip = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(KEY_SKIP_SHOW_LONG_TAP_WARNING, false);
            Log.d(FingerprintSettingsFragment.TAG, "ShowLongTapWarningDialog isLongTapMode: " + String.valueOf(isLongTapMode) + " isDialogSkip: " + String.valueOf(isDialogSkip));

            if (!isLongTapMode || isDialogSkip) {
                return;
            }
            final View checkboxLayout = getActivity().getLayoutInflater().inflate(R.layout.app_checkbox_never_show_again, null);

            final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(checkboxLayout)
                    .setMessage(R.string.asus_long_tap_warning)
                    .setPositiveButton(R.string.okay,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                    if (dontShowAgain.isChecked()) {
                                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().
                                                putBoolean(KEY_SKIP_SHOW_LONG_TAP_WARNING, true).commit();
                                    }
                                    dialog.dismiss();
                                }
                            })
                    .show();

        }
        // --- ian_tsai@20160608 show dialog when user turn on incoming call lock
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
    }

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            this(context, attrs, defStyleAttr, 0);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            this(context, attrs, com.android.internal.R.attr.preferenceStyle);
        }

        public FingerprintPreference(Context context) {
            this(context, null);
        }

        public View getView() { return mView; }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mView = view;
        }
    };

    private static class LearnMoreSpan extends URLSpan {

        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private LearnMoreSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
            try {
                ((Activity) ctx).startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(FingerprintSettingsFragment.TAG,
                        "Actvity was not found for intent, " + intent.toString());
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation : spans) {
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = new LearnMoreSpan(uri);
                builder.setSpan(link, start, end, msg.getSpanFlags(link));
            }
            return builder;
        }
    }
}
