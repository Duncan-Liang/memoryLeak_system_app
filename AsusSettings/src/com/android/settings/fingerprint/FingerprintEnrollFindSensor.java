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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
//add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
//add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensor extends FingerprintEnrollBase {

    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
    private static String TAG = "FingerprintEnrollFindSensor";
    private static boolean DEBUG = true;

    public static final String EXTRA_KEY_SENEOR_POSITION = "fingerprint_sensor_position";
    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin

    private static final int CONFIRM_REQUEST = 1;
    private static final int ENROLLING = 2;
    public static final String EXTRA_KEY_LAUNCHED_CONFIRM = "launched_confirm_lock";

    private FingerprintLocationAnimationView mAnimation;
    private boolean mLaunchedConfirmLock;

    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
    private TextView mMessage;
    private AsusFindFingerprintSensorView mSensorPositionView;
    private boolean mPosition = true; //True == front, False == back
    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_find_sensor);
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        setHeaderText(R.string.asus_security_settings_fingerprint_enroll_find_sensor_title);
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
        mLaunchedConfirmLock = savedInstanceState != null && savedInstanceState.getBoolean(
                EXTRA_KEY_LAUNCHED_CONFIRM);
        if (mToken == null && !mLaunchedConfirmLock) {
            launchConfirmLock();
        }
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        mSensorPositionView = (AsusFindFingerprintSensorView)findViewById(R.id.find_sensor_view);
        mMessage = (TextView)findViewById(R.id.find_sensor_text);
        mPosition = AsusFindFingerprintSensorView.SENSOR_FRONT.equals(mSensorPositionView.getPosition());
        if(mPosition){
            mMessage.setText(R.string.asus_security_settings_fingerprint_enroll_find_sensor_message_front);
        }else{
            mMessage.setText(R.string.asus_security_settings_fingerprint_enroll_find_sensor_message_back);
        }

        TextView hintText = (TextView) findViewById(R.id.hint_message);
        if(mPosition){
            hintText.setVisibility(View.VISIBLE);
        }
        //mAnimation = (FingerprintLocationAnimationView) findViewById(
        //        R.id.fingerprint_sensor_location_animation);
        //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end
    }

    @Override
    protected void onStart() {
        super.onStart();
        //delete by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        //mAnimation.startAnimation();
        //delete by sunxiaolong@wind-mobi.com for asus's printfinger patch end
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mAnimation.stopAnimation();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
    }

    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case 831: //KeyEvent.KEYCODE_FINGERPRINT_TAP
                case 832: //KeyEvent.KEYCODE_FINGERPRINT_DTAP
                case 833: //KeyEvent.KEYCODE_FINGERPRINT_LONGPRESS
                case 827: //KeyEvent.KEYCODE_FINGERPRINT_SWIPE_UP
                case 828: //KeyEvent.KEYCODE_FINGERPRINT_SWIPE_DOWN
                case 829: //KeyEvent.KEYCODE_FINGERPRINT_SWIPE_LEFT
                case 830: //KeyEvent.KEYCODE_FINGERPRINT_SWIPE_RIGHT
                    if(DEBUG) Log.d(TAG, "get fingerprint keycode to next");
                    onNextButtonClick();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    //add by sunxiaolong@wind-mobi.com for asus's printfinger patch end


    @Override
    protected void onNextButtonClick() {
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        Intent intent = getEnrollingIntent();
        startActivityForResult(intent, ENROLLING);
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK) {
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
            } else {
                finish();
            }
        } else if (requestCode == ENROLLING) {
            if (resultCode == RESULT_FINISHED) {
                setResult(RESULT_FINISHED);
                finish();
            } else if (resultCode == RESULT_SKIP) {
                setResult(RESULT_SKIP);
                finish();
            } else if (resultCode == RESULT_TIMEOUT) {
                setResult(RESULT_TIMEOUT);
                finish();
            } else {
                FingerprintManager fpm = getSystemService(FingerprintManager.class);
                int enrolled = fpm.getEnrolledFingerprints().size();
                int max = getResources().getInteger(
                        com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
                if (enrolled >= max) {
                    finish();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void launchConfirmLock() {
        long challenge = getSystemService(FingerprintManager.class).preEnroll();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                getString(R.string.security_settings_fingerprint_preference_title),
                null, null, challenge)) {

            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        } else {
            mLaunchedConfirmLock = true;
        }
    }

    @Override
    protected int getMetricsCategory() {
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
        return MetricsLogger.FINGERPRINT_FIND_SENSOR_SETUP;
        //modify by sunxiaolong@wind-mobi.com for asus's printfinger patch end
    }
}
