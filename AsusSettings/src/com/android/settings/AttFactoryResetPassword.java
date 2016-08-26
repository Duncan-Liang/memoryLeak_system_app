package com.android.settings;

import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

public class AttFactoryResetPassword extends PreferenceActivity {

    public static final String IS_SIM_CARD_READY = "isSimCardReady";
    public static final String SHAREDPREFERENCES_NAME = "att_factory_reset_password";
    public static final String SHAREDPREFERENCES_PASSWORD_KEY = "att_facotry_reset_password_key";
    public static final String SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE =  "default_value";
    public static final int PASSWORD_LENGTH = 4;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, AttFactoryResetPasswordFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (AttFactoryResetPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO: Fix on phones
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.att_factory_reset_password_title);
        showBreadCrumbs(msg, msg);
    }

    public static class AttFactoryResetPasswordFragment extends Fragment implements OnClickListener, OnEditorActionListener {

        private static final String TAG = "AttFactoryResetPassword";
        private static final String CONFIRM_STATE = "confirm_state";
        private static final String TEMP_PASSWORD = "temp_password";
        private static final boolean DEBUG = false;

        private View mContentView = null;
        private TextView textViewHeader = null;
        private TextView editTextPassword = null;
        private Button buttonCancel = null;
        private Button buttonOk = null;

        private State confirmState = State.ENTER_OLD_PASSWORD;
        private String tempPassword = "";

        private enum State {
            NO_SIM_CARD,
            ENTER_OLD_PASSWORD,
            ENTER_NEW_PASSWORD,
            CONFIRM_NEW_PASSWORD;
        }

        private enum Mode {
            TABLET,
            PHONE;
        }

        // required constructor for fragments
        public AttFactoryResetPasswordFragment() {

        }

        // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
        private KeyboardView mKeyboardView;
        private PasswordEntryKeyboardHelper mKeyboardHelper;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (DEBUG) {
                Log.d(TAG, "Enter onCreate state");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (DEBUG) {
                Log.d(TAG, "onCreateView");
            }

            mContentView = inflater.inflate(R.layout.att_factory_reset_password, null);
            textViewHeader = (TextView) mContentView.findViewById(R.id.headerText);
            editTextPassword = (TextView) mContentView.findViewById(R.id.password_entry);
            buttonCancel = (Button) mContentView.findViewById(R.id.cancel_button);
            buttonOk = (Button) mContentView.findViewById(R.id.next_button);

            buttonCancel.setOnClickListener(this);
            buttonOk.setOnClickListener(this);

            editTextPassword.setOnEditorActionListener(this);  // default will hide the keyboard if this is not registered
            editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editTextPassword.setFilters(new InputFilter[] {new InputFilter.LengthFilter(PASSWORD_LENGTH)});
            editTextPassword.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                    if (confirmState == State.NO_SIM_CARD) {
                        textViewHeader.setText(R.string.att_factory_reset_password_header_no_sim_card);
                    } else if (confirmState == State.ENTER_OLD_PASSWORD) {
                        textViewHeader.setText(R.string.att_factory_reset_password_header_enter_old);
                    } else if (confirmState == State.CONFIRM_NEW_PASSWORD) {
                        textViewHeader.setText(R.string.att_factory_reset_password_header_confirm_new);
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(editTextPassword.getText().length() == PASSWORD_LENGTH)
                        buttonOk.setEnabled(true);
                    else
                        buttonOk.setEnabled(false);
                }
            });

            // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
            final Activity activity = getActivity();
            mKeyboardView = (PasswordEntryKeyboardView) mContentView.findViewById(R.id.keyboard);
            mKeyboardHelper = new PasswordEntryKeyboardHelper(activity, mKeyboardView, editTextPassword);
            mKeyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
            mKeyboardView.requestFocus();
            int currentType = editTextPassword.getInputType();
            editTextPassword.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

            if (savedInstanceState == null) {
                if (DEBUG) {
                    Log.d(TAG, "savedInstanceState == null");
                }
//                Bundle bundle = getArguments();
//                if (bundle != null) {
//                    boolean isSimCardReady = bundle.getBoolean(IS_SIM_CARD_READY, true);
//                    if (DEBUG) {
//                        Log.d(TAG, "isSimCardReady = " + isSimCardReady);
//                    }
//                    if (!isSimCardReady) {
//                        confirmState = State.NO_SIM_CARD;
//                    }
//                }
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
                if (sharedPreferences != null) {
                    String password = sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE);
                    if (DEBUG) {
                        Log.d(TAG, "password stored in SharedPreferences = " + password);
                    }
                    if (password.equals(SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE) == true) {
                        confirmState = State.ENTER_NEW_PASSWORD;
                    }
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG, "savedInstanceState != null");
                }
                confirmState = State.values()[savedInstanceState.getInt(CONFIRM_STATE)];
                tempPassword = savedInstanceState.getString(TEMP_PASSWORD);
            }
            if (DEBUG) {
                Log.d(TAG, "confirmState = " + confirmState);
                Log.d(TAG, "tempPassword = " + tempPassword);
            }
            changeStateUi(false);

            return mContentView;
        }

        @Override
        public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            if (DEBUG) {
                Log.d(TAG, "Enter onSaveInstanceState");
            }
            savedInstanceState.putInt(CONFIRM_STATE, confirmState.ordinal());
            savedInstanceState.putString(TEMP_PASSWORD, tempPassword);
        }

        @Override
        public void onStart() {
            super.onStart();
            if (DEBUG) {
                Log.d(TAG, "Enter onStart state");
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (DEBUG) {
                Log.d(TAG, "Enter onResume state");
            }
            mKeyboardView.requestFocus();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (DEBUG) {
                Log.d(TAG, "Enter onPause state");
            }
            mKeyboardView.requestFocus();
        }

        @Override
        public void onStop() {
            super.onStop();
            if (DEBUG) {
                Log.d(TAG, "Enter onStop state");
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (DEBUG) {
                Log.d(TAG, "Enter onDestroy state");
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
                if (sharedPreferences != null) {
                    String password = sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE);
                    Log.d(TAG, "password stored in SharedPreferences = " + password);
                }
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.cancel_button:
                    if (DEBUG) {
                        Log.d(TAG, "onClick() cancel button");
                    }
//                    if (getMode(getActivity()) == Mode.TABLET) {
//                        getFragmentManager().popBackStack();
//                    } else {
//                        getActivity().finish();
//                    }
                    getActivity().finish();
                    break;

                case R.id.next_button:
                    if (DEBUG) {
                        Log.d(TAG, "onClick() ok button, state = " + confirmState);
                    }
                    handleNext();
                    break;

                default:
//                  super.onClick(v);
                    break;
            }
        }

        public void handleNext() {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);

            switch (confirmState) {
                case NO_SIM_CARD:
                    if (sharedPreferences != null) {
                        String password = sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE);
                        if (editTextPassword.getText().toString().equals(password) == true) {
                            // got to MasterClear
                            Preference preference = new Preference(getActivity());
                            preference.setFragment(MasterClear.class.getName());
                            preference.setTitle(R.string.master_clear_title);
                            ((PreferenceActivity) getActivity()).onPreferenceStartFragment(null, preference);
                            getActivity().finish();  //don't come back to this fragment from MasterClear
                        } else {
                            // show error message
                            if (DEBUG) {
                                Log.d(TAG, "error, enter " + editTextPassword.getText().toString() + " != " + password);
                            }
                            showErrorMessage();
                        }
                    }
                    break;

                case ENTER_OLD_PASSWORD:
                    if (sharedPreferences != null) {
                        String password = sharedPreferences.getString(SHAREDPREFERENCES_PASSWORD_KEY, SHAREDPREFERENCES_PASSWORD_DEFAULT_VALUE);
                        if (editTextPassword.getText().toString().equals(password) == true) {
                            // got to ENTER_NEW_PASSWORD
                            confirmState = State.ENTER_NEW_PASSWORD;
                            changeStateUi(true);
                        } else {
                            // show error message
                            if (DEBUG) {
                                Log.d(TAG, "error, enter " + editTextPassword.getText().toString() + " != " + password);
                            }
                            showErrorMessage();
                        }
                    }
                    break;

                case ENTER_NEW_PASSWORD:
                    confirmState = State.CONFIRM_NEW_PASSWORD;
                    tempPassword = editTextPassword.getText().toString();
                    if (DEBUG) {
                        Log.d(TAG, "tempPassword = " + tempPassword);
                    }
                    changeStateUi(true);
                    break;

                case CONFIRM_NEW_PASSWORD:
                    if (DEBUG) {
                        Log.d(TAG, "tempPassword = " + tempPassword);
                    }
                    if (editTextPassword.getText().toString().equals(tempPassword) == true) {
                        // save the password
                        if (sharedPreferences != null) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SHAREDPREFERENCES_PASSWORD_KEY, tempPassword);
                            editor.commit();
                        }
//                        if (getMode(getActivity()) == Mode.TABLET) {
//                            getFragmentManager().popBackStack();
//                        } else {
//                            getActivity().finish();
//                        }
                        getActivity().finish();
                    } else {
                        // show error message
                        if (DEBUG) {
                            Log.d(TAG, "error, enter " + editTextPassword.getText().toString() + " != " + tempPassword);
                        }
                        showErrorMessage();
                    }
                    break;

                default:
                    break;
            }
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                if (DEBUG) {
                    Log.d(TAG, "press softwarekeyboard ENTER key, state = " + confirmState);
                }
//                if (editTextPassword.getText().length() == PASSWORD_LENGTH) {
//                    buttonOk.performClick();
//                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//                }
                handleNext();
                return true;
            }
            return false;
        }

        public void changeStateUi(boolean isSoftKeyboardShowForced) {
            if (confirmState == State.NO_SIM_CARD) {
                textViewHeader.setText(R.string.att_factory_reset_password_header_no_sim_card);
                buttonOk.setText(R.string.lockpassword_ok_label);
            } else if (confirmState == State.ENTER_OLD_PASSWORD) {
                textViewHeader.setText(R.string.att_factory_reset_password_header_enter_old);
                buttonOk.setText(R.string.lockpassword_ok_label);
            } else if (confirmState == State.ENTER_NEW_PASSWORD) {
                textViewHeader.setText(R.string.att_factory_reset_password_header_enter_new);
                buttonOk.setText(R.string.lockpassword_continue_label);
            } else if (confirmState == State.CONFIRM_NEW_PASSWORD) {
                textViewHeader.setText(R.string.att_factory_reset_password_header_confirm_new);
                buttonOk.setText(R.string.lockpassword_ok_label);
            }
            editTextPassword.setText("");

//            if (isSoftKeyboardShowForced) {
//                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//            } else {
//                // auto show the soft keyboard when fragment is created
//                Mode mode = getMode(getActivity());
//                if (DEBUG) {
//                    Log.d(TAG, "changeStateUi(), mode = " + mode);
//                }
//                switch (mode) {
//                    case TABLET:
//                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//                        break;
//                    case PHONE:
//                    default:
//                        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//                        break;
//                }
//            }
//
//            // fix TT-346704
//            editTextPassword.requestFocus();
        }

        public void showErrorMessage() {
            textViewHeader.setText(R.string.lockpassword_confirm_passwords_dont_match);
//            editTextPassword.setText("");
//            Selection.setSelection((Spannable) editTextPassword.getText(), 0, editTextPassword.getText().length());
        }

        public Mode getMode(Context context) {
            if (((context.getResources()).getConfiguration().screenLayout
                    & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                return Mode.TABLET;
            }
            return Mode.PHONE;
        }
    }
}
