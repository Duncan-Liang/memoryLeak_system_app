/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.users;

import android.accounts.AccountManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.weibo.net.AccessToken;
import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboDialogParameter;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboHttpUtility;
import com.weibo.net.WeiboParameters;

public class SnapViewSettings extends SettingsPreferenceFragment
            implements OnPreferenceClickListener, EditUserInfoController.OnContentChangedCallback {
    private static final String TAG = "SnapViewSettings";

    private static final String KEY_SETTINGS_CATEGORY_LOCKSCREEN = "snapview_settings_category_lockscreen";
    private static final String KEY_SETTINGS_CATEGORY_VISUALHINT = "snapview_settings_category_visualhint";
    private static final String KEY_SETTINGS_CATEGORY_DUMMY = "snapview_settings_category_dummy";
    private static final String KEY_SETTINGS_CATEGORY_ACCOUNT = "snapview_settings_category_account";
    private static final String KEY_SUMMARY = "summary";
    private static final String KEY_SETTINGS_AVATAR = "user_snapview_settings_avatar";
    private static final String KEY_SETTINGS_HINT_LOCK = "user_snapview_settings_hint_lock";
    private static final String KEY_SETTINGS_HINT_NOTIFY = "user_snapview_settings_hint_notify";
    private static final String KEY_SETTINGS_DUMMY_NOTIFY = "user_snapview_settings_dummy_notify";
    private static final String KEY_SETTINGS_DUMMY_NOTIFY_ICON = "user_snapview_settings_dummy_notify_icon";
    private static final String KEY_SETTINGS_SET_LOCK = "user_snapview_settings_set_lock";
    private static final String KEY_SETTINGS_CHANGE_INFO = "user_snapview_settings_change_info";
    private static final String KEY_SETTINGS_DELETE_SELF = "user_snapview_settings_delete_self";
    private static final String KEY_SETTINGS_RESCUE_ACCOUNT = "user_snapview_settings_rescue_account";

    private static final int DIALOG_DUMMY_NOTIFY_EDITOR = 1;
    private static final int DIALOG_USER_PROFILE_EDITOR = 2;
    private static final int DIALOG_CONFIRM_REMOVE = 3;
    private static final int DIALOG_CHANGE_RESCUE_ACCOUNT = 4;
    private static final int DIALOG_CHOOSE_RESCUE_METHOD = 5;

    public static final int NEW_CHOOSE_ACCOUNT = 1;
    public static final String NULL_IN_STRING = "null";

    private Context mContext;
    private static Weibo mweibo;
    private PreferenceGroup mPreferenceCategory_lockscreen;
    private PreferenceGroup mPreferenceCategory_visualhint;
    private PreferenceGroup mPreferenceCategory_dummy;
    private PreferenceGroup mPreferenceCategory_account;
//    private CheckBoxPreference mPref_avatar;
    private DropDownPreference mPref_hint_lock;
    private DropDownIconPreference2 mPref_hint_notify;
    private Preference mPref_dummy_notify;
    private DropDownIconPreference mPref_dummy_notify_icon;
    private Preference mPref_set_lock;
    private Preference mPref_change_info;
    private Preference mPref_delete_self;
    private Preference mPref_rescue_account;

    private EditUserInfoController mEditUserInfoController =
            new EditUserInfoController();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mEditUserInfoController.onRestoreInstanceState(icicle);
        }
        mContext = getActivity();

        addPreferencesFromResource(R.xml.user_snapview_settings);

        mPreferenceCategory_lockscreen = (PreferenceGroup) findPreference(KEY_SETTINGS_CATEGORY_LOCKSCREEN);
        if(mPreferenceCategory_lockscreen != null) {
            mPref_set_lock = (Preference) mPreferenceCategory_lockscreen.findPreference(KEY_SETTINGS_SET_LOCK);

            mPref_set_lock.setOnPreferenceClickListener(this);
        }

        mPreferenceCategory_visualhint = (PreferenceGroup) findPreference(KEY_SETTINGS_CATEGORY_VISUALHINT);
        if(mPreferenceCategory_visualhint != null) {
//            mPref_avatar = (CheckBoxPreference) mPreferenceCategory_visualhint.findPreference(KEY_SETTINGS_AVATAR);
            mPref_hint_lock = (DropDownPreference) mPreferenceCategory_visualhint.findPreference(KEY_SETTINGS_HINT_LOCK);
            mPref_hint_notify = (DropDownIconPreference2) mPreferenceCategory_visualhint.findPreference(KEY_SETTINGS_HINT_NOTIFY);

//            setPrefAvatar();
            // Always set USER_SNAPVIEW_SETTINGS_AVATAR as 0
            // Settings.Global.putInt(getContentResolver(),
            // Settings.Global.USER_SNAPVIEW_SETTINGS_AVATAR, 0);
            setPrefHintLock();
            setPrefHintNotify();
        }

        mPreferenceCategory_dummy = (PreferenceGroup) findPreference(KEY_SETTINGS_CATEGORY_DUMMY);
        if(mPreferenceCategory_dummy != null) {
            mPref_dummy_notify = (Preference) mPreferenceCategory_dummy.findPreference(KEY_SETTINGS_DUMMY_NOTIFY);
            mPref_dummy_notify_icon = (DropDownIconPreference) mPreferenceCategory_dummy.findPreference(KEY_SETTINGS_DUMMY_NOTIFY_ICON);

//            mPref_dummy_notify.setSummary(Settings.Global.getString(mContext.getContentResolver(),
//                        Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY));
            mPref_dummy_notify.setOnPreferenceClickListener(this);
            setPrefDummyNotifyIcon();
        }

        mPreferenceCategory_account = (PreferenceGroup) findPreference(KEY_SETTINGS_CATEGORY_ACCOUNT);
        if(mPreferenceCategory_account != null) {
            mPref_change_info = (Preference) mPreferenceCategory_account.findPreference(KEY_SETTINGS_CHANGE_INFO);
            mPref_delete_self = (Preference) mPreferenceCategory_account.findPreference(KEY_SETTINGS_DELETE_SELF);
            mPref_rescue_account = (Preference) mPreferenceCategory_account.findPreference(KEY_SETTINGS_RESCUE_ACCOUNT);
            setRescureAccount();
            mPref_rescue_account.setOnPreferenceClickListener(this);

            mPref_change_info.setOnPreferenceClickListener(this);
            mPref_delete_self.setOnPreferenceClickListener(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mEditUserInfoController.onSaveInstanceState(outState);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mEditUserInfoController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mEditUserInfoController.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_CHOOSE_ACCOUNT && resultCode == SettingsActivity.RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null && !accountName.equals(NULL_IN_STRING)) {
                SnapViewProviderUtil.Secure.updateAccount(getActivity().getContentResolver(), SnapViewProviderUtil.ACCOUNT, accountName);
                mPref_rescue_account.setIcon(R.drawable.stat_sys_dummynotify3);
                mPref_rescue_account.setSummary(accountName);
            }
        }
    }

    private void removeThisUser() {
        try {
            ActivityManagerNative.getDefault().switchUser(UserHandle.USER_OWNER);
            ((UserManager) mContext.getSystemService(Context.USER_SERVICE))
                    .removeUser(UserHandle.myUserId());
            SnapViewUtils.resetSnapViewGlobalValues(getContentResolver());
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove self user");
        }
    }

    private void startPasswordPage() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.ChooseLockGeneric"));
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == mPref_dummy_notify) {
            showDialog(DIALOG_DUMMY_NOTIFY_EDITOR);
        } else if (pref == mPref_set_lock) {
            startPasswordPage();
        }  else if (pref == mPref_change_info) {
            showDialog(DIALOG_USER_PROFILE_EDITOR);
        } else if (pref == mPref_delete_self) {
            showDialog(DIALOG_CONFIRM_REMOVE);
        } else if (pref == mPref_rescue_account) {
            showDialog(DIALOG_CHANGE_RESCUE_ACCOUNT);
        }
        return false;
    }

    public void weiboAuthorize() {
        mweibo = Weibo.getInstance();
        mweibo.setupConsumerConfig(SnapViewUtils.WEIBO_CONSUMER_KEY, SnapViewUtils.WEIBO_CONSUMER_SECRET);
        mweibo.setRedirectUrl(SnapViewUtils.WEIBO_REDIRECT_URL);

        mweibo.setWeiboDialogParameter(setupWeiboDialogParam());
        mweibo.authorize(mContext, new AuthDialogListener());
    }

    class AuthDialogListener implements WeiboDialogListener {
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");

            AccessToken accessToken = new AccessToken(token, SnapViewUtils.WEIBO_CONSUMER_SECRET);
            accessToken.setExpiresIn(expires_in);
            mweibo.setAccessToken(accessToken);

            new Thread() {
                public void run() {
                   String uid = getUID();
                   if(uid != null) {
                       SnapViewProviderUtil.Secure.putAccount(getActivity().getContentResolver(), SnapViewProviderUtil.ACCOUNT, uid);
                       SnapViewProviderUtil.Secure.putAccount(getActivity().getContentResolver(), SnapViewProviderUtil.QUESTION, null);
                       SnapViewProviderUtil.Secure.putAccount(getActivity().getContentResolver(), SnapViewProviderUtil.ANSWER, null);
                   }
                }
            }.start();
        }

        public void onError(DialogError e) {
            Log.e(TAG, "Auth error : " + e.getMessage());
        }

        public void onCancel() {
        }

        public void onWeiboException(WeiboException e) {
            Log.e(TAG, "Auth exception : " + e.getMessage());
        }
    }

    protected String getUID() {
        String url = Weibo.SERVER + "account/get_uid.json";
        WeiboParameters bundle = new WeiboParameters();
        bundle.add("source", Weibo.getAppKey());
        String uid = null;
        try {
            String resp = mweibo.request(mContext, url, bundle, WeiboHttpUtility.HTTPMETHOD_GET, mweibo.getAccessToken());
            uid = SnapViewUtils.parseUID(resp);
        } catch (WeiboException e){
            e.printStackTrace();
        }
        return uid;
    }

    public WeiboDialogParameter setupWeiboDialogParam() {
        WeiboDialogParameter mParam = new WeiboDialogParameter();
//        mParam.setStyle(R.style.WeiboDialogStyle);
        mParam.setMessage(getString(R.string.settings_safetylegal_activity_loading));
        mParam.setMargin(R.dimen.dialog_left_margin, R.dimen.dialog_right_margin, R.dimen.dialog_top_margin, R.dimen.dialog_bottom_margin);
        mParam.setButtonMargin(R.dimen.dialog_btn_close_right_margin, R.dimen.dialog_btn_close_top_margin);
        return mParam;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (mContext == null) return null;
        switch (dialogId) {
            case DIALOG_DUMMY_NOTIFY_EDITOR: {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View content = inflater.inflate(R.layout.edit_dummy_notification_dialog, null);

                final String dummyText = "DUMMY_NOTIFY";
//                        Settings.Global.getString(mContext.getContentResolver(),
//                    Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY);
                final EditText dummyTextEditor = (EditText) content.findViewById(R.id.dummy_notification_editor);
                dummyTextEditor.setText(dummyText);
                //Set hint by SKU
                String hint = mContext.getResources().getString(
                                        SnapViewUtils.isCNSKU() ?
                                            R.string.cn_user_snapview_settings_dummy_default :
                                            R.string.user_snapview_settings_dummy_default);
                ((android.widget.TextView) content.findViewById(R.id.dummy_notification_editor)).setHint(hint);

                Dialog dlg = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.user_snapview_settings_dummy_notify)
                    .setView(content)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                String dummyTextNew = dummyTextEditor.getText().toString();
                                if(dummyTextNew == null || dummyTextNew.isEmpty()) {
                                    dummyTextNew = mContext.getResources().getString(
                                            android.os.SystemProperties.get("ro.product.name", "").toLowerCase().startsWith("cn_") ?
                                                    R.string.cn_user_snapview_settings_dummy_default :
                                                        R.string.user_snapview_settings_dummy_default);
                                }
                                if (dummyText == null
                                        || !dummyTextNew.equals(dummyText.toString())) {
//                                    Settings.Global.putString(mContext.getContentResolver(),
//                                        Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY, dummyTextNew);
                                    mPref_dummy_notify.setSummary(dummyTextNew);
                                 }
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .create();

                // Make sure the IME is up.
                dlg.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                return dlg;
            }

            case DIALOG_USER_PROFILE_EDITOR: {
                Dialog dlg = mEditUserInfoController.createDialog(
                        (Fragment) this,
                        (Drawable) null,
                        (CharSequence) null,
                        R.string.profile_info_settings_title,
                        this /* callback */,
                        android.os.Process.myUserHandle());
                return dlg;
            }

            case DIALOG_CONFIRM_REMOVE: {
                Dialog dlg =
                        Utils.createRemoveConfirmationDialog(mContext, UserHandle.myUserId(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeThisUser();
                                    }
                                }
                        );
                return dlg;
            }

            case DIALOG_CHANGE_RESCUE_ACCOUNT: {
                Dialog dlg = new AlertDialog.Builder(mContext)
                        .setTitle(R.string.user_snapview_settings_rescue_account)
                        .setCancelable(true)
                        .setMessage(SnapViewUtils.getCombined2String(mContext,
                                R.string.user_snapview_message_change_rescue_account_dlg_text,
                                SnapViewUtils.getAccountType2(mContext)))
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (SnapViewUtils.isNetworkConnected(getActivity())) {
                                    if (SnapViewUtils.isCNSKU()) {
                                        showDialog(DIALOG_CHOOSE_RESCUE_METHOD);
                                    } else {
                                        Intent intent = AccountManager.newChooseAccountIntent(null, null,
                                                new String[] {"com.google"}, false, null, null, null, null);
                                        startActivityForResult(intent, NEW_CHOOSE_ACCOUNT);
                                    }
                                }
                            }
                        })
                        .create();
                return dlg;
            }

            case DIALOG_CHOOSE_RESCUE_METHOD: {
                String[] rescueMethods = {
                        getResources().getString(R.string.weibo),
                        getResources().getString(R.string.user_snapview_offline_pwd)};
                Dialog dlg = new AlertDialog.Builder(mContext)
                .setTitle(R.string.user_snapview_choose_rescue_method_dlg_title)
                .setSingleChoiceItems(rescueMethods, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which){
                            case 0:
                                removeDialog(DIALOG_CHOOSE_RESCUE_METHOD);
                                weiboAuthorize();
                                break;
                            case 1:
                                removeDialog(DIALOG_CHOOSE_RESCUE_METHOD);
                                Intent intent = new Intent(getActivity(), SecurityQuestion.class);
                                intent.putExtra(SecurityQuestion.KEY_IS_ANSWERING, false);
                                startActivity(intent);
                                break;
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
                return dlg;
            }
            default:
                return null;
        }
    }

    /*private void setPrefAvatar() {
        final String strPrefAvatar = Settings.Global.USER_SNAPVIEW_SETTINGS_AVATAR;
        if (mPref_avatar == null) {
            Log.i(TAG, "Preference not found: " + mPref_avatar);
            return;
        }

        try {
            mPref_avatar.setChecked(Settings.Global.getInt(mContext.getContentResolver(), strPrefAvatar) == 1);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, strPrefAvatar + " not found");
        }

        mPref_avatar.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean)newValue;

                return Settings.Global.putInt(mContext.getContentResolver(), strPrefAvatar, val ? 1 : 0);
            }
        });
    }*/

    private void setPrefHintLock() {
        int iSelect = 0;
        final String strPrefHintLock = null; //Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_LOCK;
        if (mPref_hint_lock == null) {
            Log.i(TAG, "Preference not found: " + mPref_hint_lock);
            return;
        }

        mPref_hint_lock.addItem(R.string.user_snapview_settings_item_color, 0);
        mPref_hint_lock.addItem(R.string.user_snapview_settings_item_icon, 1);
        mPref_hint_lock.addItem(R.string.user_snapview_settings_item_none, 2);
        /*try {
            iSelect = Settings.Global.getInt(mContext.getContentResolver(), strPrefHintLock);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, strPrefHintLock + " not found");
        }*/
        if(iSelect > 2 || iSelect < 0)
            iSelect = 0;
        mPref_hint_lock.setSelectedValue(iSelect);

        mPref_hint_lock.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int val = (Integer) value;
                Settings.Global.putInt(mContext.getContentResolver(), strPrefHintLock, val);
                return true;
            }
        });
    }

    private void setPrefHintNotify() {
        int iSelect = 0;
        int iColor = 0;
        final String strPrefHintNotify = null;//Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_NOTIFY;
        final String strPrefHintColor = null;//Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_COLOR;
        if (mPref_hint_notify == null) {
            Log.i(TAG, "Preference not found: " + mPref_hint_notify);
            return;
        }

        mPref_hint_notify.addItem(R.string.user_snapview_settings_item2, 1);
        mPref_hint_notify.addItem(R.string.user_snapview_settings_item1, 0);
        try {
            iSelect = Settings.Global.getInt(mContext.getContentResolver(), strPrefHintNotify);
            iColor = Settings.Global.getInt(mContext.getContentResolver(), strPrefHintColor);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, strPrefHintNotify + " or " + strPrefHintColor + " not found");
        }
        if(iSelect > 1 || iSelect < 0)
            iSelect = 0;
        mPref_hint_notify.setSelectedValue(iSelect);
        mPref_hint_notify.setSelectedColor(iColor);
        mPref_hint_notify.setCallback(new DropDownIconPreference2.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int val = (Integer) value;
                Settings.Global.putInt(mContext.getContentResolver(), strPrefHintNotify, val);
                return true;
            }

            @Override
            public boolean onColorSelected(int color) {
                Settings.Global.putInt(mContext.getContentResolver(), strPrefHintColor, color);
                return true;
            }
        });
    }

    private void setPrefDummyNotifyIcon() {
        int iSelect = 0;
        final String strPrefDummyNotifyIcon = null; //Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY_ICON;
        if (mPref_dummy_notify_icon == null) {
            Log.i(TAG, "Preference not found: " + mPref_dummy_notify_icon);
            return;
        }

        try {
            iSelect = Settings.Global.getInt(mContext.getContentResolver(), strPrefDummyNotifyIcon);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, strPrefDummyNotifyIcon + " not found");
        }
        if(iSelect > 2 || iSelect < 0)
            iSelect = 0;
        mPref_dummy_notify_icon.setSelectedValue(iSelect);

        mPref_dummy_notify_icon.setCallback(new DropDownIconPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int val = (Integer) value;
                Settings.Global.putInt(mContext.getContentResolver(), strPrefDummyNotifyIcon, val);
                return true;
            }
        });
    }

    private void setRescureAccount() {
        String accountName = SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.ACCOUNT);
        String question = SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.QUESTION);
        if ((accountName == null || accountName.equals("")) && (question == null || question.equals(""))) {
            mPref_rescue_account.setIcon(R.drawable.asus_new_feature_icon);
            mPref_rescue_account.setSummary(R.string.user_snapview_message_none_rescue_account);
        } else if (SnapViewUtils.isCNSKU()) {
            mPref_rescue_account.setIcon(null);
            mPref_rescue_account.setSummary(R.string.user_snapview_settings_rescue_account_summary);
        } else {
            mPref_rescue_account.setIcon(null);
            mPref_rescue_account.setSummary(accountName);
        }
    }

    @Override
    public void onPhotoChanged(Drawable photo) {}

    @Override
    public void onLabelChanged(CharSequence label) {}

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MAIN_SETTINGS;
    }
}
