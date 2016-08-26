//xiongshigui@wind-mobi.com 20160525 add begin
package com.android.settings.dms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;

import com.asus.DLNA.DMS.IDmsService;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.WirelessSettings;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DMSSettings extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener, TextWatcher, Indexable {

    public static final String DLNA_PACKAGE_NAME = "com.asus.DLNA";
    private static final String TAG = "DMSSettings";

    /* --------------------- Constant definitions -------------------- */
    // DMS Service action
    private static final String ACTION_DMS_SERVICE  = "com.asus.DLNA.DMS.DMSService";

    // preference key
    private static final String PREF_DMS_SETTINGS = "com.android.settings.dms";
    private static final String PREF_KEY_PHOTO_CBX = "checkPhoto";
    private static final String PREF_KEY_MUSIC_CBX = "checkMusic";
    private static final String PREF_KEY_VIDEO_CBX = "checkVideo";
    private static final String PREF_KEY_DEVNAME_ED = "editTextFriendlyname";
    private static final String PREF_KEY_SHAREFOLDER = "pref_sharefolder";
    private static final String PREF_KEY_IF_NO_WARNING = "no_warning_dialog";
    private static final String PREF_KEY_SHAREMEDIATYPE = "pref_sharemediatype";
    private static final String PREF_KEY_DMS_ADV_SETTINGS = "pref_dms_settings_adv";
    private static final String PREF_KEY_MDMSSTARTING_VALUE = "pref_mdmsstarting_value";

    // preference default value
    // private static final String PREF_DEFAULT_INTERNAL_STORAGE       = "/sdcard";
    private static final String PREF_DEFAULT_INTERNAL_STORAGE = Environment.getExternalStorageDirectory().toString();

    // DMS Service doDmsFunction key
    private static final int KEY_DMS_FUNC_ENABLE_DMS = 50;
    private static final int KEY_DMS_FUNC_DISABLE_DMS = 51;

    private static final int KEY_DMS_FUNC_IS_DMS_RUNNING = 52;
    private static final int KEY_DMS_FUNC_IS_DMS_UPDATING = 53;
    private static final int KEY_DMS_FUNC_GET_MEDIA_TYPE_AND_NAME = 54;
    private static final int KEY_DMS_FUNC_GET_SHARED_FOLDER = 55;

    private static final int KEY_DMS_FUNC_SET_MEDIA_TYPE = 56;
    private static final int KEY_DMS_FUNC_SET_FRIENDLY_NAME = 57;
    private static final int KEY_DMS_FUNC_ADD_SHARED_FOLDER = 58;
    private static final int KEY_DMS_FUNC_REMOVE_SHARED_FOLDER = 59;

    // DMS Service doDmsFunction bundle key
    private static final String BUN_KEY_DMS_FUNC_RESULT = "funcResult";
    private static final String BUN_KEY_SHARE_FOLDER_INDEX = "shareFolderIndex";
    private static final String BUN_KEY_SHARE_FOLDER_LIST = "shareFolderList";
    private static final String BUN_KEY_SHARE_FOLDER_NAMELIST = "shareFolderNameList";

    private static String ACTION_DLNASERVICE_REQUEST_PERMISSION = "com.asus.DLNA.DMC.action.REQUEST_PERMISSION";

    /* --------------------- Member variables -------------------- */
    private Context mContext;
    private PreferenceCategory mShareTypePrefCategory;
    private PreferenceCategory mDmsAdvanceSettingPrefCategory;
    private CheckBoxPreference mMusicPref;
    private CheckBoxPreference mPhotoPref;
    private CheckBoxPreference mVideoPref;
    private EditTextPreference mFriendlynamePref;
    private ListPreference mShareFolderPref;

    private ServiceConnection mDmsServiceConnection = new DmsServiceConnection();
    private static IDmsService mDmsService = null;

    private static final int UPDATE_DMS_STATUS = 10;
    private Handler mDmsStatusPollingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            if (msg.what == UPDATE_DMS_STATUS) {
                updateDmsStatus();
                mDmsStatusPollingHandler.sendMessageDelayed(mDmsStatusPollingHandler.obtainMessage(UPDATE_DMS_STATUS), 500);
            }
        }
    };

    private int mShareFolderPrefIndex = 0;
    private boolean mMusicEnabled = true;
    private boolean mPhotoEnabled = true;
    private boolean mVideoEnabled = true;
    private String mFriendlyName = "";
    private String mShareFolderPrefPath = "";
    private ArrayList<CharSequence> mFolderPathList = null;  //folder path
    private ArrayList<CharSequence> mFolderNameList = null;  //folder name

    private ProgressDialog mWaitingDialog = null;
    //++Sunny_Yuan
    private ProgressDialog mPreparingDialog = null;
    private boolean mActivityPaused = true;

    private SwitchBar mSwitchBar;
    private boolean mListeningToOnSwitchChange = false;
    private boolean mIsDMSRunning = true;
    private boolean mDMSStarting = false;
    private Messenger mDMSServiceMessenger = null;
    private boolean mDlnaPermissionGranted =false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        mContext = getActivity().getApplicationContext();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.dms_settings);

        Activity activity = getActivity();

        if (activity != null) {
            getActivity().setTitle(getString("dms"));
        }

        mShareTypePrefCategory = (PreferenceCategory) findPreference(PREF_KEY_SHAREMEDIATYPE);
        if (mShareTypePrefCategory != null) {
            mShareTypePrefCategory.setTitle(getString("sharemediatype"));
        }

        mDmsAdvanceSettingPrefCategory = (PreferenceCategory) findPreference(PREF_KEY_DMS_ADV_SETTINGS);
        if (mDmsAdvanceSettingPrefCategory != null) {
            mDmsAdvanceSettingPrefCategory.setTitle(getString("dms_settings_adv"));
        }

        mMusicPref = (CheckBoxPreference) findPreference(PREF_KEY_MUSIC_CBX);
        if (mMusicPref != null) {
            mMusicPref.setTitle(getString("musictype"));
        }

        mPhotoPref = (CheckBoxPreference) findPreference(PREF_KEY_PHOTO_CBX);
        if (mPhotoPref != null) {
            mPhotoPref.setTitle(getString("imagetype"));
        }

        mVideoPref = (CheckBoxPreference) findPreference(PREF_KEY_VIDEO_CBX);
        if (mVideoPref != null) {
            mVideoPref.setTitle(getString("videotype"));
        }

        mFriendlynamePref = (EditTextPreference) findPreference(PREF_KEY_DEVNAME_ED);
        if (mFriendlynamePref != null) {
            mFriendlynamePref.setTitle(getString("title_friendlyname"));
            mFriendlynamePref.setDialogTitle(getString("title_friendlyname"));
            mFriendlynamePref.setSummary(getString("desc_friendlyname"));

            //limit the display name length up to 32
            int maxLength = 32;
            InputFilter[] FilterArray = new InputFilter[1];
            FilterArray[0] = new InputFilter.LengthFilter(maxLength);
            mFriendlynamePref.getEditText().setFilters(FilterArray);
            mFriendlynamePref.getEditText().addTextChangedListener(this);
            mFriendlynamePref.setOnPreferenceChangeListener(this);
        }

        //prepare share folder list
        mShareFolderPref = (ListPreference) findPreference(PREF_KEY_SHAREFOLDER);
        if (mShareFolderPref != null) {
            mShareFolderPref.setTitle(getString("title_share"));
            mShareFolderPref.setDialogTitle(getString("desc_share"));
            mShareFolderPref.setSummary(getString("desc_share"));
            mShareFolderPref.setOnPreferenceChangeListener(this);
        }

        mFolderPathList = new ArrayList<CharSequence>();
        mFolderNameList = new ArrayList<CharSequence>();

        mWaitingDialog = createWaitingDialog();    //create waiting dialog
        mWaitingDialog.show();

        //Recovery previous preferences
        if (null != savedInstanceState) {
            mVideoEnabled = savedInstanceState.getBoolean(PREF_KEY_VIDEO_CBX, true);
            mMusicEnabled = savedInstanceState.getBoolean(PREF_KEY_MUSIC_CBX, true);
            mPhotoEnabled = savedInstanceState.getBoolean(PREF_KEY_PHOTO_CBX, true);
            mFriendlyName = savedInstanceState.getString(PREF_KEY_DEVNAME_ED, Build.MODEL);
            mDMSStarting = savedInstanceState.getBoolean(PREF_KEY_MDMSSTARTING_VALUE, false);

            mFriendlynamePref.setText(mFriendlyName);
            mFriendlynamePref.setSummary(mFriendlyName);

            mShareFolderPrefPath = savedInstanceState.getString(PREF_KEY_SHAREFOLDER, PREF_DEFAULT_INTERNAL_STORAGE);

            String[] folderList = savedInstanceState.getString(BUN_KEY_SHARE_FOLDER_LIST).split("\n");
            for (String s : folderList) {
                mFolderPathList.add(s);
            }
            String[] folderNameList = savedInstanceState.getString(BUN_KEY_SHARE_FOLDER_NAMELIST).split("\n");
            for (String s : folderNameList) {
                mFolderNameList.add(s);
            }

            setListPrefDataSource();
            mShareFolderPrefIndex = savedInstanceState.getInt(BUN_KEY_SHARE_FOLDER_INDEX, 0);
            mShareFolderPref.setValueIndex(mShareFolderPrefIndex);
        }

        if ((mMusicPref != null) && (mPhotoPref != null) && (mVideoPref != null) && (mFriendlynamePref != null)
                        && (mShareFolderPref != null)) {
                mMusicPref.setEnabled(false);
                mPhotoPref.setEnabled(false);
                mVideoPref.setEnabled(false);
                mFriendlynamePref.setEnabled(false);
                mShareFolderPref.setEnabled(false);
        }

        initWarningDialog();
        initNoNetworkDialog();

    }

    @Override
    public void onStart() {
        super.onStart();
        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.addOnSwitchChangeListener(mDmsEnabledListener);
            mListeningToOnSwitchChange = true;
        }
        mSwitchBar.show();
        mSwitchBar.setEnabled(false);

        //bind service
        mDmsService = null;
        Log.v(TAG, "Try to bind DMS Service");
        Intent intent = new Intent(ACTION_DMS_SERVICE);
        intent.setPackage(DLNA_PACKAGE_NAME);
        mContext.bindService(intent, mDmsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Log.v(TAG, "stop update UI");
        mActivityPaused = true;
        if (!mNeverShowWarningDialog && null != mDmsWarningDialog && mDmsWarningDialog.isShowing()) {
            mDmsWarningDialog.dismiss();
        } else if (mNoNetworkWarningDialog != null && mNoNetworkWarningDialog.isShowing()) {
            mNoNetworkWarningDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log.d(TAG, "start update UI");
        mActivityPaused = false;
        updateState();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mListeningToOnSwitchChange) {
            mSwitchBar.removeOnSwitchChangeListener(mDmsEnabledListener);
            mListeningToOnSwitchChange = false;
        }
        mSwitchBar.hide();

        //unbind service
        Log.v(TAG, "Try to unbind DMS Service");
        mContext.unbindService(mDmsServiceConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        //clear data
        if (null != mShareFolderPref) {
            mShareFolderPref.setOnPreferenceClickListener(null);
            mShareFolderPref.setOnPreferenceChangeListener(null);
            mShareFolderPref = null;
        }
        if (null != mFolderPathList) {
            mFolderPathList.clear();
            mFolderPathList = null;
        }
        if (null != mFolderNameList) {
            mFolderNameList.clear();
            mFolderNameList = null;
        }
        if (null != mWaitingDialog) {
            showWaitingDialog(false);
            mWaitingDialog.dismiss();
            mWaitingDialog = null;
        }
        //++Sunny_Yuan, TT-610348
        if (null != mPreparingDialog) {
            mPreparingDialog.dismiss();
            mPreparingDialog = null ;
        }

        mDmsStatusPollingHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            mDlnaPermissionGranted = true;
            Log.v(TAG, "Try to start DMS Service");
            Intent intent = new Intent(ACTION_DMS_SERVICE);
            intent.setPackage(DLNA_PACKAGE_NAME);
            mContext.startService(intent);

            if (!mNeverShowWarningDialog && null != mDmsWarningDialog && !mIsDMSRunning) {
                mDmsWarningDialog.show();
            } else {
                checkNetworkConnection();
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (mSwitchBar != null) {
                mSwitchBar.setChecked(false);
            }
        }
    }

    private void setListPrefDataSource() {
        //Log.d(TAG, "setListPrefDataSource");
        if (null != mShareFolderPref && null != mFolderNameList &&  null != mFolderPathList) {
            if (updateListPreferenceEntries()) {
                Dialog dialog = mShareFolderPref.getDialog();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        } else {
            Log.d(TAG, " handle of share folder list is null !!!");
        }
    }

    private boolean updateListPreferenceEntries() {
        CharSequence entriesArray[] = new CharSequence[mFolderNameList.size()];
        CharSequence entriesValueArray[] = new CharSequence[mFolderPathList.size()];
        mFolderNameList.toArray(entriesArray);
        mFolderPathList.toArray(entriesValueArray);

        if (mShareFolderPref.getEntries() != null && mShareFolderPref.getEntryValues() != null) {
            if (Arrays.equals(mShareFolderPref.getEntries(), entriesArray)
                    && Arrays.equals(mShareFolderPref.getEntryValues(), entriesValueArray)) {
                return false;
            } else {
                mShareFolderPref.setEntries(mFolderNameList.toArray(entriesArray));
                mShareFolderPref.setEntryValues(mFolderPathList.toArray(entriesValueArray));
                return true;
            }
        }

        mShareFolderPref.setEntries(mFolderNameList.toArray(entriesArray));
        mShareFolderPref.setEntryValues(mFolderPathList.toArray(entriesValueArray));
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String keyClicked = preference.getKey();
        // Log.d(TAG, "onPreferenceTreeClick:" + keyClicked);
        if (PREF_KEY_DEVNAME_ED.equals(keyClicked) || PREF_KEY_SHAREFOLDER.equals(keyClicked)) {
            return true;
        } else {
            updateState();
            dmsServiceFunction(KEY_DMS_FUNC_SET_MEDIA_TYPE);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(PREF_KEY_DEVNAME_ED)){
            //Log.d(TAG, "onPreferenceChange:" + txtFriendlyname);
            mFriendlyName = (String)newValue;
            mFriendlynamePref.setText(mFriendlyName);
            mFriendlynamePref.setSummary(mFriendlyName);
            dmsServiceFunction(KEY_DMS_FUNC_SET_FRIENDLY_NAME);
        }
        else if (preference.getKey().equals(PREF_KEY_SHAREFOLDER)) {
            if (mShareFolderPrefPath != null && !mShareFolderPrefPath.equals((String)newValue)) {
                mShareFolderPrefPath = (String)newValue;
                dmsServiceFunction(KEY_DMS_FUNC_ADD_SHARED_FOLDER);
                showWaitingDialog(1000); // 1sec
            }
        }
        return true;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String text = s.toString();
        // Do not allow to set empty name
        AlertDialog alertDialog = ((AlertDialog) mFriendlynamePref.getDialog());
        if (alertDialog != null) {
            alertDialog.getButton(
                    AlertDialog.BUTTON_POSITIVE).setEnabled(!text.isEmpty());
        }
    }

    private void dmsServiceFunction(int funcKey) {
        if (null == mDmsService) {
            return;
        }
        try {
            Bundle ret = mDmsService.doDmsFunction(KEY_DMS_FUNC_IS_DMS_RUNNING, null);
            boolean running = ret.getBoolean(BUN_KEY_DMS_FUNC_RESULT, false);
            Log.d(TAG, "Start DMS: " + running);
            if (!running) {
                mDmsStatusPollingHandler.sendMessageDelayed(mDmsStatusPollingHandler.obtainMessage(UPDATE_DMS_STATUS), 500);

                Message msg = Message.obtain();
                msg.what = KEY_DMS_FUNC_ENABLE_DMS;
                mDMSServiceMessenger.send(msg);

                mDMSStarting = true;
                showWaitingDialog(true);
                return;
            }
            if (mDMSServiceMessenger == null) {
                return;
            }
            switch(funcKey) {
                case KEY_DMS_FUNC_SET_MEDIA_TYPE: {
                    Bundle bun = new Bundle();
                    bun.putBoolean(PREF_KEY_VIDEO_CBX, mVideoEnabled);
                    bun.putBoolean(PREF_KEY_MUSIC_CBX, mMusicEnabled);
                    bun.putBoolean(PREF_KEY_PHOTO_CBX, mPhotoEnabled);
                    Message msg = Message.obtain();
                    msg.what = KEY_DMS_FUNC_SET_MEDIA_TYPE;
                    msg.setData(bun);
                    mDMSServiceMessenger.send(msg);
                    break;
                }
                case KEY_DMS_FUNC_SET_FRIENDLY_NAME: {
                    Bundle bun = new Bundle();
                    bun.putString(PREF_KEY_DEVNAME_ED, mFriendlyName);
                    Message msg = Message.obtain();
                    msg.what = KEY_DMS_FUNC_SET_FRIENDLY_NAME;
                    msg.setData(bun);
                    mDMSServiceMessenger.send(msg);
                    break;
                }
                case KEY_DMS_FUNC_ADD_SHARED_FOLDER: {
                    Bundle bun = new Bundle();
                    bun.putString(PREF_KEY_SHAREFOLDER, mShareFolderPrefPath);
                    Message msg = Message.obtain();
                    msg.what = KEY_DMS_FUNC_ADD_SHARED_FOLDER;
                    msg.setData(bun);
                    mDMSServiceMessenger.send(msg);
                    break;
                }
                default :
                    //default
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopService() {
        Log.d(TAG, "Stop DMS");
        Log.v(TAG, "Try to stop DMS Service");
        Intent intent = new Intent(ACTION_DMS_SERVICE);
        intent.setPackage(DLNA_PACKAGE_NAME);
        mContext.stopService(intent);
        if (null == mDmsService) {
            return;
        }
        try {
            Message msg = Message.obtain();
            msg.what = KEY_DMS_FUNC_DISABLE_DMS;
            mDMSServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, "onSaveInstanceState");

        savedInstanceState.putBoolean(PREF_KEY_PHOTO_CBX, mPhotoEnabled);
        savedInstanceState.putBoolean(PREF_KEY_MUSIC_CBX, mMusicEnabled);
        savedInstanceState.putBoolean(PREF_KEY_VIDEO_CBX, mVideoEnabled);
        savedInstanceState.putString(PREF_KEY_DEVNAME_ED, mFriendlyName);
        savedInstanceState.putString(PREF_KEY_SHAREFOLDER, mShareFolderPrefPath);
        savedInstanceState.putBoolean(PREF_KEY_MDMSSTARTING_VALUE, mDMSStarting);

        StringBuilder sFolderlist = new StringBuilder();
        for (int i = 0; i < mFolderPathList.size(); i++) {
            sFolderlist.append(mFolderPathList.get(i));
            if (i < (mFolderPathList.size()-1)) {
                sFolderlist.append("\n");
            }
        }
        savedInstanceState.putString(BUN_KEY_SHARE_FOLDER_LIST, sFolderlist.toString());

        StringBuilder sFolderNamelist = new StringBuilder();
        for (int i = 0; i < mFolderNameList.size(); i++) {
            sFolderNamelist.append(mFolderNameList.get(i));
            if (i < (mFolderNameList.size()-1)) {
                sFolderNamelist.append("\n");
            }
        }
        savedInstanceState.putString(BUN_KEY_SHARE_FOLDER_NAMELIST, sFolderNamelist.toString());
        savedInstanceState.putInt(BUN_KEY_SHARE_FOLDER_INDEX, mShareFolderPrefIndex);
    }

    private void updateState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        mMusicEnabled = prefs.getBoolean(PREF_KEY_MUSIC_CBX, true);
        mPhotoEnabled = prefs.getBoolean(PREF_KEY_PHOTO_CBX, true);
        mVideoEnabled = prefs.getBoolean(PREF_KEY_VIDEO_CBX, true);
        mFriendlyName = prefs.getString(PREF_KEY_DEVNAME_ED, Build.MODEL);

        mMusicPref.setChecked(mMusicEnabled);
        mPhotoPref.setChecked(mPhotoEnabled);
        mVideoPref.setChecked(mVideoEnabled);
        mFriendlynamePref.setSummary(mFriendlyName);
    }

    private ProgressDialog createWaitingDialog() {
        // Log.d(TAG, "createWaitingDialog");
        final ProgressDialog waitingDialog = new ProgressDialog(getActivity());
        waitingDialog.setMessage(getString("wait"));
        waitingDialog.setCanceledOnTouchOutside(false);
        waitingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (null != waitingDialog) {
                    Log.w(TAG, "press back key during showing waiting icon");
                    waitingDialog.hide();
                    if (mSwitchBar != null) {
                        mSwitchBar.getSwitch().invalidate();
                    }
                }
            }
        });
        return waitingDialog;
    }

    private boolean isWaitingDialogShowing = true;
    private void showWaitingDialog(boolean show) {
        if (null == mWaitingDialog) return;

        if (show) {
            mWaitingDialog.show();
        } else if (isWaitingDialogShowing == true) {
            mWaitingDialog.hide();
            if (mSwitchBar != null) {
                mSwitchBar.getSwitch().invalidate();
            }
        }
        isWaitingDialogShowing = show;
    }

    private void showWaitingDialog(long timeoutMillis) {
        //++Sunny_Yuan, TT-610348
        mPreparingDialog = createWaitingDialog();
        mPreparingDialog.show();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                try{
                    if (mPreparingDialog != null) {
                        mPreparingDialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, timeoutMillis);
    }

    private void updateDmsStatus() {
        if (mActivityPaused || null == mDmsService) return;

        try {
            Bundle ret = mDmsService.doDmsFunction(KEY_DMS_FUNC_IS_DMS_RUNNING, null);
            boolean running = ret.getBoolean(BUN_KEY_DMS_FUNC_RESULT, false);

            if (mDMSStarting) {
                if (running) {
                    mDMSStarting = false;
                } else {
                    return;
                }
            }

            if (mIsDMSRunning && !running) {
                mSwitchBar.setChecked(false);
            }
            mIsDMSRunning = running;

            if (mSwitchBar.isChecked() == running) {
                mSwitchBar.setEnabled(true);
                if (running) {
                    showWaitingDialog(false);
                    mMusicPref.setEnabled(true);
                    mPhotoPref.setEnabled(true);
                    mVideoPref.setEnabled(true);
                    mFriendlynamePref.setEnabled(true);
                    mShareFolderPref.setEnabled(true);
                }
            } else {
                mSwitchBar.setEnabled(false);
            }
            updateMediaTypeAndName();
            updateSharedFolder();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateMediaTypeAndName() {

        boolean isDmsEnabled = mSwitchBar.isChecked();
        mMusicPref.setEnabled(isDmsEnabled);
        mPhotoPref.setEnabled(isDmsEnabled);
        mVideoPref.setEnabled(isDmsEnabled);
        mFriendlynamePref.setEnabled(isDmsEnabled);

        try {
            Bundle ret = mDmsService.doDmsFunction(KEY_DMS_FUNC_GET_MEDIA_TYPE_AND_NAME, null);
            mMusicEnabled = ret.getBoolean(PREF_KEY_MUSIC_CBX, true);
            mPhotoEnabled = ret.getBoolean(PREF_KEY_PHOTO_CBX, true);
            mVideoEnabled = ret.getBoolean(PREF_KEY_VIDEO_CBX, true);

            mMusicPref.setChecked(mMusicEnabled);
            mPhotoPref.setChecked(mPhotoEnabled);
            mVideoPref.setChecked(mVideoEnabled);

            mFriendlyName = ret.getString(PREF_KEY_DEVNAME_ED, Build.MODEL);
            mFriendlynamePref.setText(mFriendlyName);
            mFriendlynamePref.setSummary(mFriendlyName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSharedFolder() {

        boolean isDmsEnabled = mSwitchBar.isChecked();
        mShareFolderPref.setEnabled(isDmsEnabled);

        try {
            Bundle ret = mDmsService.doDmsFunction(KEY_DMS_FUNC_GET_SHARED_FOLDER, null);
            mShareFolderPrefPath = ret.getString(PREF_KEY_SHAREFOLDER, PREF_DEFAULT_INTERNAL_STORAGE);
            mFolderPathList = ret.getCharSequenceArrayList(BUN_KEY_SHARE_FOLDER_LIST);
            mFolderNameList = ret.getCharSequenceArrayList(BUN_KEY_SHARE_FOLDER_NAMELIST);
            setListPrefDataSource();
            mShareFolderPrefIndex = ret.getInt(BUN_KEY_SHARE_FOLDER_INDEX, 0);
            mShareFolderPref.setValueIndex(mShareFolderPrefIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------- */
    class DmsServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder boundService ) {
            Log.e(TAG, "=====>> binded service" );
            Log.e(TAG, "DmsServiceConnection onServiceConnected" );
            mDmsService = IDmsService.Stub.asInterface((IBinder)boundService);
            try {
                mDMSServiceMessenger = new Messenger(mDmsService.getMessenger());
                // Initialize Preferences
                updateMediaTypeAndName();
                updateSharedFolder();

                // Used to know server status (When Service enabled, Call each 500ms updateDmsStatus())
                mDmsStatusPollingHandler.obtainMessage(UPDATE_DMS_STATUS).sendToTarget();

                //get the state of DMS and update the real UI Layout
                Bundle ret = mDmsService.doDmsFunction(KEY_DMS_FUNC_IS_DMS_RUNNING, null);
                mIsDMSRunning = ret.getBoolean(BUN_KEY_DMS_FUNC_RESULT, false);
                // Log.i(TAG, "binded service, DMS is running: " +  mIsDMSRunning);

                if (mSwitchBar != null) {
                    mSwitchBar.setChecked(mIsDMSRunning || mDMSStarting);
                    if (mDMSStarting) {
                        mWaitingDialog.show();
                    } else {
                        mWaitingDialog.hide();
                    }
                } else {
                    mWaitingDialog.hide();
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "DmsServiceConnection onServiceDisconnected" );
            mDmsService = null;
        }
    }

    private SwitchBar.OnSwitchChangeListener mDmsEnabledListener = new SwitchBar.OnSwitchChangeListener() {
        @Override
        public void onSwitchChanged(Switch switchView, boolean isChecked) {
            Log.d(TAG, "updateUILayout, isChecked: "+ isChecked + " mDMSStarting = " + mDMSStarting);
            if(isChecked) {
                if (!mDlnaPermissionGranted) {
                    startActivityForResult(new Intent(ACTION_DLNASERVICE_REQUEST_PERMISSION), 1);
                } else {
                    Log.v(TAG, "Try to start DMS Service");
                    Intent intent = new Intent(ACTION_DMS_SERVICE);
                    intent.setPackage(DLNA_PACKAGE_NAME);
                    mContext.startService(intent);

                    if (!mNeverShowWarningDialog && null != mDmsWarningDialog && !mIsDMSRunning) {
                        mDmsWarningDialog.show();
                    } else {
                        checkNetworkConnection();
                    }
                }
            } else {
                mMusicPref.setEnabled(false);
                mPhotoPref.setEnabled(false);
                mVideoPref.setEnabled(false);
                mFriendlynamePref.setEnabled(false);
                mShareFolderPref.setEnabled(false);
                stopService();
            }
        }
    };

    private AlertDialog mDmsWarningDialog;
    private AlertDialog mNoNetworkWarningDialog;
    private boolean mNeverShowWarningDialog = false;

    private void initWarningDialog() {
        //Log.d(TAG, "initWarningDialog");
        SharedPreferences settings = mContext.getSharedPreferences(PREF_DMS_SETTINGS, 0);
        mNeverShowWarningDialog = settings.getBoolean(PREF_KEY_IF_NO_WARNING, false);
        View v = View.inflate(getActivity(), R.layout.dms_warning_dialog, null);

        boolean isCNSKU = SystemProperties.get("ro.build.asus.sku", android.os.Build.UNKNOWN).equals("CN");
        String needNetworkMessage = getString(isCNSKU ? "dialog_need_network_CN" :"dialog_need_network");
        String costExtraPowerMessage = getString("dialog_cost_extra_power");

        CheckBox donotShowAgain = (CheckBox) v.findViewById(R.id.notshowbx);
        String text = getString("notagain");
        donotShowAgain.setText(text);
        donotShowAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mNeverShowWarningDialog = isChecked;
            }
        });

        AlertDialog.Builder dmsWarningBuilder = new AlertDialog.Builder(getActivity());
        dmsWarningBuilder.setIconAttribute(android.R.attr.alertDialogIcon)
        .setTitle(getString("dms"))
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                SharedPreferences settings = mContext.getSharedPreferences(PREF_DMS_SETTINGS, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(PREF_KEY_IF_NO_WARNING, mNeverShowWarningDialog);
                editor.commit();
                checkNetworkConnection();
            }
        })
        .setView(v)
        .setMessage(String.format("%s\n%s", needNetworkMessage, costExtraPowerMessage));

        mDmsWarningDialog = dmsWarningBuilder.create();
        mDmsWarningDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

    private void checkNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (null != info && info.isConnectedOrConnecting()) {
            dmsServiceFunction(0 /* Start DMS Service */);
        } else if (null != mNoNetworkWarningDialog) {
            if (mSwitchBar != null) {
                mSwitchBar.setChecked(false);
            }
            mNoNetworkWarningDialog.show();
        }
    }

    private void initNoNetworkDialog() {
        //Log.d(TAG, "initNoNetworkDialog");

        boolean isCNSKU = SystemProperties.get("ro.build.asus.sku", android.os.Build.UNKNOWN).equals("CN");

        AlertDialog.Builder noNetworkWarningBuilder = new AlertDialog.Builder(getActivity());
        noNetworkWarningBuilder.setIconAttribute(android.R.attr.alertDialogIcon)
        .setTitle(getString("dms"))
        .setMessage(getString(isCNSKU ? "dialog_no_network_CN" : "dialog_no_network"))
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("android.settings.WIFI_SETTINGS");
                startActivity(intent);
            }})
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mNoNetworkWarningDialog.dismiss();
            }
        });
        mNoNetworkWarningDialog = noNetworkWarningBuilder.create();
        mNoNetworkWarningDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    }

   public static boolean isSupportDLNA(Context context) {
        try {
            android.content.pm.ApplicationInfo info = context.getPackageManager().getApplicationInfo(DLNA_PACKAGE_NAME, 0);
            return ( info.enabled && ((android.content.pm.ApplicationInfo.FLAG_SYSTEM & info.flags) != 0) );
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getString(String resId) {
        return WirelessSettings.getString(mContext, DLNA_PACKAGE_NAME, resId);
    }
}
//xiongshigui@wind-mobi.com 20160525 add end