/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;
import com.android.settings.R;

import android.app.Fragment;
import android.content.Intent;
import android.inputmethodservice.KeyboardView;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import android.content.Context;
import android.preference.PreferenceActivity;

import java.util.List;

public class SnapViewDeleteConfirm extends PreferenceActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, SnapViewDeleteConfirmFragment.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (SnapViewDeleteConfirmFragment.class.getName().equals(fragmentName))
            return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Disable IME on our window since we provide our own keyboard
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        // WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.user_snapview_settings_delete_self);
        showBreadCrumbs(msg, msg);
    }

    public static class SnapViewDeleteConfirmFragment extends Fragment {

        private View mContentView;
        private Button mFinalButton;
        private TextView checkEditText;
        private TextView hintNumber;
        private String checkNumber;

        private int PASSWORD_LENGTH = 4;

        private static final String TAG = "SnapViewDeleteConfirm";

        private TextView hintMessage;

        // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
        private KeyboardView mKeyboardView;
        private PasswordEntryKeyboardHelper mKeyboardHelper;

        private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

            public void onClick(View v) {
                UserManager mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
                List<UserInfo> users = mUserManager.getUsers(true);
                for (UserInfo user : users) {
                    if (false/*user.isSnapView()*/) {
                        mUserManager.removeUser(user.id);
                        SnapViewUtils.resetSnapViewGlobalValues(getActivity().getContentResolver());
                        getActivity().finish();
                        break;
                    }
                }
            }
        };

        /**
         * Configure the UI for the final confirmation interaction
         */
        private void establishFinalConfirmationState(String saveNumber) {
            mFinalButton = (Button) mContentView.findViewById(R.id.execute_delete_snapview);
            mFinalButton.setOnClickListener(mFinalClickListener);

            checkEditText = (TextView) mContentView.findViewById(R.id.check_number);
            checkEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            hintNumber = (TextView) mContentView.findViewById(R.id.hint_number);

            if (TextUtils.isEmpty(saveNumber)) {
                checkNumber = getRandomNumber();
            } else {
                hintNumber.setText(" " + saveNumber + " : ");
                checkNumber = saveNumber;
            }

            // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
            mKeyboardView = (PasswordEntryKeyboardView) mContentView.findViewById(R.id.keyboard);
            mKeyboardHelper = new PasswordEntryKeyboardHelper(getActivity(), mKeyboardView, checkEditText);
            mKeyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
            mKeyboardView.requestFocus();

            checkEditText.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String inputValue = checkEditText.getText().toString();
                    if (s.length() == PASSWORD_LENGTH && inputValue.equals(checkNumber) == false) { // joey_lee
                        Toast.makeText(getActivity(), R.string.lockpassword_confirm_passwords_dont_match, Toast.LENGTH_LONG).show();
                    }
                    if (inputValue.equals(checkNumber)) {
                        mFinalButton.setEnabled(true);
                    } else {
                        mFinalButton.setEnabled(false);
                    }
                }
            });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mContentView = inflater.inflate(R.layout.snapview_delete_confirm, null);
            String number = null;
            if (savedInstanceState != null
                    && !TextUtils.isEmpty(savedInstanceState.getString("hintnumber"))) {
                number = savedInstanceState.getString("hintnumber");
            }
            establishFinalConfirmationState(number);
            return mContentView;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            mKeyboardView.requestFocus();
        }

        @Override
        public void onPause() {
            super.onPause();
            mKeyboardView.requestFocus();
        }

        public String getRandomNumber() {
            String randomNumber = "";
            for (int i = 0; i < 4; i++) {
                randomNumber += String.valueOf((int) (Math.random() * 10));
            }
            hintNumber.setText(" " + randomNumber + " : ");
            return randomNumber;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString("hintnumber", checkNumber);
        }
    }
}
