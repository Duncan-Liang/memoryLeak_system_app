
package com.android.settings.users;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

public class SecurityQuestion extends Activity {

    public static final String TAG = "SecurityQuestion";
    Spinner mQuestions;
    EditText mAnswer;
    EditText mConfirm;
    LinearLayout mController;
    TextView mOkButton;
    TextView mCancelButton;
    private boolean mIsAnsweringQuestion = false;
    private long mSelectedQuestionIndex = -1;
    private String mAnswerInput;
    public static final String KEY_IS_ANSWERING = "key_is_answering";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(R.layout.security_question);
        Bundle args = getIntent().getExtras();
        mIsAnsweringQuestion = args.getBoolean(KEY_IS_ANSWERING);

        mQuestions = (Spinner) findViewById(R.id.questions);
        String[] options = getResources().getStringArray(R.array.user_snapview_security_questions);
        SecurityQuestionOptionAdapter adapter = new SecurityQuestionOptionAdapter(
                SecurityQuestion.this, R.layout.security_question_option, options);
        mQuestions.setAdapter(adapter);
        mAnswer = (EditText) findViewById(R.id.answer);
        mAnswer.requestFocus();
        mAnswer.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(mAnswer, 0);
            }
        }, 200);
        mController = (LinearLayout) findViewById(R.id.controller);
        mOkButton = (TextView) findViewById(R.id.ok_button);
        mCancelButton = (TextView) findViewById(R.id.cancel_button);
        mConfirm = (EditText) findViewById(R.id.confirmation);

        if (!mIsAnsweringQuestion) {
            mQuestions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                        long id) {
                    mSelectedQuestionIndex = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            mConfirm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    preformOkButton();
                    return true;
                }
            });

            mOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    preformOkButton();
                }
            });
        } else {
            // +++ Change page description
            TextView page_description = (TextView) findViewById(R.id.description);
            page_description
                    .setText(getResources().getString(R.string.user_snapview_answering_question_description));
            // ---
            mSelectedQuestionIndex = Integer.valueOf(SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.QUESTION));
            mConfirm.setVisibility(View.GONE);
            mController.setVisibility(View.VISIBLE);
            mCancelButton.setText(getString(android.R.string.cancel));
            mOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    verifyAnswer();
                }
            });
            mAnswer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    verifyAnswer();
                    return true;
                }
            });
        }
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void preformOkButton() {
        String answer = mAnswer.getText().toString();
        String confirmed = mConfirm.getText().toString();

        if (answer.equals(confirmed)
                && (!TextUtils.isEmpty(answer) && !TextUtils.isEmpty(confirmed))) {

            // update db
            SnapViewProviderUtil.Secure.putAccount(getContentResolver(), SnapViewProviderUtil.ACCOUNT, null);
            SnapViewProviderUtil.Secure.putAccount(getContentResolver(), SnapViewProviderUtil.QUESTION, String.valueOf(mSelectedQuestionIndex));
            SnapViewProviderUtil.Secure.putAccount(getContentResolver(), SnapViewProviderUtil.ANSWER, answer);

//            Message msg = UserSettings.snapviewHandler.sendEmptyMessage(UserSettings.MESSAGE_SNAPVIEW_QUESTION_COMPLETE);
//            Message msg = new Message();
//            msg.what = MESSAGE_WEIBO_COMPLETE;
//            handler.sendMessage(msg);
            finish();
        } else {
             Toast.makeText(this, getString(R.string.user_snapview_invalid_answer), Toast.LENGTH_SHORT).show();
            mAnswer.setText("");
            mConfirm.setText("");
            mAnswer.requestFocus();
        }
    }

    private void verifyAnswer() {
        mAnswerInput = SnapViewProviderUtil.Secure.getAccount(getContentResolver(), SnapViewProviderUtil.ANSWER);
        String answer =
                mAnswer.getText().toString(); // +++ TT-601660 also check the question int //
        int questionIndex = mQuestions.getSelectedItemPosition();
        if (answer.equals(mAnswerInput) &&
                mSelectedQuestionIndex == questionIndex) {
            new AlertDialog.Builder(SecurityQuestion.this, R.style.Theme_AlertDialog)
            .setTitle(R.string.user_snapview_message_reset_pwd_dlg_title)
            .setMessage(R.string.user_snapview_message_reset_pwd_dlg_text)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
//                    showDialog(DIALOG_SELECT_ACTION);
                    new AlertDialog.Builder(SecurityQuestion.this, R.style.Theme_AlertDialog)
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
                                        new AlertDialog.Builder(SecurityQuestion.this, R.style.Theme_AlertDialog)
                                        .setTitle(R.string.user_snapview_settings_delete_self)
                                        .setMessage(R.string.user_snapview_reset_confirm)
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                removeSnapView();
                                                SnapViewUtils.resetSnapViewGlobalValues(getContentResolver());
                                                finish();
                                            }
                                        }).show();
                                    } else {

                                    }
                                }
                            }).show();
                }
            }).show();
        } else {
            mAnswer.setText("");
            mAnswer.requestFocus();

            new AlertDialog.Builder(SecurityQuestion.this, R.style.Theme_AlertDialog)
            .setTitle(R.string.user_snapview_message_verify_account_fail_dlg_title)
            .setMessage(R.string.user_snapview_message_verify_account_fail_dlg_message)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }
    }

    private void removeSnapView() {
        try {
            ActivityManagerNative.getDefault().switchUser(UserHandle.USER_OWNER);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            List<UserInfo> users = userManager.getUsers(true);
            for (UserInfo user : users) {
                if (false/*user.isSnapView()*/) {
                    userManager.removeUser(user.id);
                    break;
                }
            }
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove SnapView");
        }
    }

    public class SecurityQuestionOptionAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private String[] mData;

        public SecurityQuestionOptionAdapter(Context context, int textViewResourceId, String[] array) {
            super(context, textViewResourceId, array);
            this.mContext = context;
            this.mData = array;
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        class ViewHolder {
            public TextView textView;
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater= (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.security_question_option, null);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.textView = (TextView) view.findViewById(R.id.option);
                view.setTag(viewHolder);
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.textView.setText(mData[position]);

            return view;
        }
    }
}
