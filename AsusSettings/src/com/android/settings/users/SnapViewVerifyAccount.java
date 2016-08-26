
package com.android.settings.users;

import java.io.IOException;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.R;
import com.weibo.net.AccessToken;
import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboDialogParameter;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboHttpUtility;
import com.weibo.net.WeiboParameters;

public class SnapViewVerifyAccount extends Activity {

    private static Context mContext;
    public static final String TAG = "VerifyAccount";
    private UserManager mUserManager;
    private String mAccountName;
    private String mQuestionIndex;
    private static Weibo mweibo;
    private int mDialogNum = 0;
    private final int MESSAGE_WEIBO_COMPLETE = 0;
    public final static int MESSAGE_SECURITY_QUESTION = 1;

    private static final int DIALOG_VERIFY_GOOGLE_ACCOUNT = 1;
    private static final int DIALOG_RESET_PASSWORD = 2;
    private static final int DIALOG_SELECT_ACTION = 3;
    private static final int DIALOG_DELETE_SNAPVIEW = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        mContext = SnapViewVerifyAccount.this;
        mAccountName = SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.ACCOUNT);
        mQuestionIndex = SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.QUESTION);
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if ((mAccountName == null || mAccountName.equals("")) && mQuestionIndex == null) { // No Rescue account and no security question
            finish();
        }
//        else if (!mUserManager.isSnapViewExist()) { // SnapView is not exists
//            SnapViewUtils.resetSnapViewGlobalValues(getContentResolver());
//            finish();
//        }
        else if (SnapViewUtils.isNetworkConnected(this)) {
            showDialog(DIALOG_VERIFY_GOOGLE_ACCOUNT);
        }
    }

    public void weiboAuthorize() {
        mweibo = Weibo.getInstance();
        mweibo.setupConsumerConfig(SnapViewUtils.WEIBO_CONSUMER_KEY, SnapViewUtils.WEIBO_CONSUMER_SECRET);
        mweibo.setRedirectUrl(SnapViewUtils.WEIBO_REDIRECT_URL);

        mweibo.setWeiboDialogParameter(setupWeiboDialogParam());
        mweibo.authorize(this, new AuthDialogListener());
    }

    class AuthDialogListener implements WeiboDialogListener {
        public void onComplete(Bundle values) {
            mDialogNum++;
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");
            // set Weibo Access Token
            AccessToken accessToken = new AccessToken(token, SnapViewUtils.WEIBO_CONSUMER_SECRET);
            accessToken.setExpiresIn(expires_in);
            mweibo.setAccessToken(accessToken);

            if (mDialogNum == 1) {
            new Thread() {
                @Override
                public void run() {
                    String uid = getUID();
                    if(uid != null) {
                        Message msg = new Message();
                        msg.what = MESSAGE_WEIBO_COMPLETE;
                        msg.obj = uid;
                        handler.sendMessage(msg);
                    }
                }
            }.start();
            }
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
            String resp = mweibo.request(this, url, bundle, WeiboHttpUtility.HTTPMETHOD_GET, mweibo.getAccessToken());
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

    private void handleVerifyAccount() {
        Account userAccount = new Account(mAccountName, "com.google");
        AccountManager accountManager = AccountManager.get(this);
        accountManager.confirmCredentials(userAccount, null, this,
                new AccountManagerCallback<Bundle>() {
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            final Bundle result = future.getResult();
                            final boolean verified = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);
                            if (verified) {
                                showDialog(DIALOG_RESET_PASSWORD);
                            } else {
                                finish();
                            }
                        } catch (OperationCanceledException e) {
                            Log.w(TAG, e.toString());
                            e.printStackTrace();
                        } catch (AuthenticatorException e) {
                            Log.w(TAG, e.toString());
                            e.printStackTrace();
                        } catch (IOException e) {
                            Log.w(TAG, e.toString());
                            e.printStackTrace();
                        } catch (Exception e) {
                            Log.w(TAG, e.toString());
                            e.printStackTrace();
                        }
                    }
                }, null);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_VERIFY_GOOGLE_ACCOUNT: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this,
                        R.style.Theme_AlertDialog)
                        .setTitle(R.string.user_snapview_message_verify_account_dlg_title)
                        .setMessage(R.string.user_snapview_message_verify_account_dlg_text)
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (SnapViewUtils.isCNSKU()) {
                                    if (mAccountName != null) {
                                        weiboAuthorize();
                                    } else {
                                        Intent intent = new Intent(SnapViewVerifyAccount.this, SecurityQuestion.class);
                                        intent.putExtra(SecurityQuestion.KEY_IS_ANSWERING, true);
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    handleVerifyAccount();
                                }
                            }
                        });
                return builder.create();
            }
            case DIALOG_RESET_PASSWORD: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog)
                        .setTitle(R.string.user_snapview_message_reset_pwd_dlg_title)
                        .setMessage(R.string.user_snapview_message_reset_pwd_dlg_text)
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showDialog(DIALOG_SELECT_ACTION);
                            }
                        });
                return builder.create();
            }
            case DIALOG_SELECT_ACTION: {
                 AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog)
                        .setTitle(R.string.select_account)
                        .setItems(
                                new String[] {
                                        getString(R.string.personal_space),
                                        getString(R.string.user_snapview_settings_delete_self)
                                },
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            Intent intent = new Intent();
                                            intent.setComponent(new ComponentName(
                                                    "com.android.settings",
                                                    "com.android.settings.ChooseLockGeneric"));
                                            intent.putExtra("retrieve_snapview_password", true);
                                            startActivity(intent);
                                            finish();
                                        } else if (which == 1) {
                                            showDialog(DIALOG_DELETE_SNAPVIEW);
                                        } else {

                                        }
                                    }
                                });
                return builder.create();
            }
            case DIALOG_DELETE_SNAPVIEW: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog)
                        .setTitle(R.string.user_snapview_settings_delete_self)
                        .setMessage(R.string.user_snapview_reset_confirm)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeSnapView();
                                SnapViewUtils.resetSnapViewGlobalValues(getContentResolver());
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                showDialog(DIALOG_SELECT_ACTION);
                            }
                        });
                return builder.create();
            }
            default:
                return null;
        }
    }

    private void removeSnapView() {
        try {
            ActivityManagerNative.getDefault().switchUser(UserHandle.USER_OWNER);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = userManager.getUsers(true);
            for (UserInfo user : users) {
//                if (user.isSnapView()) {
                if (false) {
                    userManager.removeUser(user.id);
                    break;
                }
            }
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove SnapView");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WEIBO_COMPLETE: {
                    String uid = (String) msg.obj;
                    if (mAccountName.equals(uid)) {
//                      showDialog(DIALOG_RESET_PASSWORD);
                        new AlertDialog.Builder(SnapViewVerifyAccount.this, R.style.Theme_AlertDialog)
                          .setTitle(R.string.user_snapview_message_reset_pwd_dlg_title)
                          .setMessage(R.string.user_snapview_message_reset_pwd_dlg_text)
                          .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                  showDialog(DIALOG_SELECT_ACTION);
                              }
                          }).show();
                  } else {
                      new AlertDialog.Builder(SnapViewVerifyAccount.this, R.style.Theme_AlertDialog)
                          .setTitle(R.string.user_snapview_message_verify_account_fail_dlg_title)
                          .setMessage(R.string.user_snapview_message_verify_account_fail_dlg_message)
                          .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                              }
                          }).show();
                  }
                }
                break;

                default:
                    break;
            }
        }
    };
}
