
package com.android.settings;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

public class UserSwitchService extends IntentService {

    private static final String TAG = "UserSwitchService";
    private static final String USER_ID = "new_user_id";

    public UserSwitchService() {
        super("UserSwitchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");
        final int userId = intent.getIntExtra(USER_ID,UserHandle.myUserId());
        writeUserDB(userId);
    }

    private void writeUserDB(int userId) {

        writeTouchDB(userId);
        writeMotionDB(userId);
    }

    private void writeTouchDB(int userId){
        final ContentResolver resolver = getContentResolver();
        final PackageManager pm = getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP)) {
            final int doubleTap = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_DOUBLE_TAP, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_DOUBLE_TAP, doubleTap, userId);
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP)) {
            final String gestureType1 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE1_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE1_APP, gestureType1, userId);

            final String gestureType2 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE2_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE2_APP, gestureType2, userId);

            final String gestureType3 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE3_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE3_APP, gestureType3, userId);

            final String gestureType4 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE4_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE4_APP, gestureType4, userId);

            final String gestureType5 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE5_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE5_APP, gestureType5, userId);

            final String gestureType6 = Settings.System.getStringForUser(resolver,
                    Settings.System.GESTURE_TYPE6_APP, UserHandle.USER_OWNER);
            Settings.System.putStringForUser(resolver,
                    Settings.System.GESTURE_TYPE6_APP, gestureType6, userId);
        }
    }

    private void writeMotionDB(int userId){
        final ContentResolver resolver = getContentResolver();
        final PackageManager pm = getPackageManager();

        // Main switch
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE)) {
            final int motionSettings = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_GESTURE_SETTINGS, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_GESTURE_SETTINGS, motionSettings, userId);
        }

        // Shake
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_FLICK)) {
            final int shake = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_SHAKE, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_SHAKE, shake, userId);
        }
        // Flip
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_TERMINAL)) {
            final int flip = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_FLIP, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_FLIP, flip, userId);
        }
        // Hands up
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_EARTOUCH)) {
            final int handsUp = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_HAND_UP, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_HAND_UP, handsUp, userId);
        }
        // Double click
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_TAPPING)) {
            final int doubleClick = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_DOUBLE_CLICK, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_DOUBLE_CLICK, doubleClick, userId);
        }
        // Moving
        if (pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_INSTANTACTIVITY)) {
            final int moving = Settings.System.getIntForUser(resolver,
                    Settings.System.ASUS_MOTION_WALKING, 0, UserHandle.USER_OWNER);
            Settings.System.putIntForUser(resolver,
                    Settings.System.ASUS_MOTION_WALKING, moving, userId);
        }
    }
}
