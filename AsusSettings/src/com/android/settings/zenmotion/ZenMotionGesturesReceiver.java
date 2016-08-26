/*
 * Filename : GesturesReceiver.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.zenmotion;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import java.util.List;


public class ZenMotionGesturesReceiver extends BroadcastReceiver {
    private static final String TAG = "ZenMotionGesturesReceiver";

    private PackageManager mPackageManager;
    private ContentResolver mContentResolver;
    private static String mGestureKey;
    private static String mGestureInfo;
    private static final String[] mGestureCount = {
            Settings.System.ASUS_SWIPE_UP,
            Settings.System.ASUS_DOUBLE_TAP,
            Settings.System.GESTURE_TYPE1_APP,      //w, F15
            Settings.System.GESTURE_TYPE2_APP,     //s, F18
            Settings.System.GESTURE_TYPE3_APP,     //e, F14
            Settings.System.GESTURE_TYPE4_APP,     //c, F13
            Settings.System.GESTURE_TYPE5_APP,     //z, F19
            Settings.System.GESTURE_TYPE6_APP,     //v, F20
    };

    private final String mMainCamera = "com.asus.camera/com.asus.camera.CameraApp";
    private final String mFrontCamera = "frontCamera";
    private final String CAMERA_PACKAGENAME = "com.asus.camera";
    private final String CAMERA_FRONT_CLASSNAME = "com.asus.camera.CameraApp.frontcamera";
    private final String CAMERA_BACK_CLASSNAME = "com.asus.camera.CameraApp";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle data = intent.getExtras();

        if (!Utils.WIND_DEF_ASUS_GESTURES) {
            return;
        }

        if (mContentResolver == null) {
            mContentResolver = context.getContentResolver();
        }

        if (action.equals("com.asus.launch_app_by_gesture")) {
            if (data != null) {
                int keyCode = data.getInt("keyCode");
                mGestureKey = getGestureKey(keyCode);

                Log.d(TAG, "onReceive: mGestureKey " + mGestureKey);
                if (mGestureKey == null || mGestureKey.equals("")) {
                    return;
                }

                mGestureInfo = Settings.System.getString(mContentResolver, mGestureKey);
                Log.d(TAG, "onReceive: mGestureInfo " + mGestureInfo);
                if (isDoubleAndSwipe(mGestureKey)) {
                    unLockScreen(context);
                    mGestureInfo = "";
                    mGestureKey = "";
                } else if (isLockScreen(context) && isCamera()) {
                    if (!TextUtils.isEmpty(mGestureInfo)) {
                        wakeUpScreen(context);
                        wakeUpCamera(context);
                        mGestureInfo = "";
                        mGestureKey = "";
                    }
                } else {
                    if (mGestureInfo != null && !mGestureInfo.equals("")) {
                        unLockScreen(context);
                    }
                }
            }
        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            Log.d(TAG, "onReceive: ACTION_USER_PRESENT mGestureInfo " + mGestureInfo);
            if (!TextUtils.isEmpty(mGestureInfo)) {
                if (mPackageManager == null) {
                    mPackageManager = context.getPackageManager();
                }

                if (isCamera()) {
                    wakeUpCamera(context);
                } else {
                    isMobilemanager(mGestureInfo);
                    String[] classInfo;
                    if (mGestureInfo != null && !mGestureInfo.equals("")) {
                        classInfo = mGestureInfo.split("/");
                        String packageName = classInfo[0];
                        String className = classInfo[1];

                        Intent gestureIntent;
                        gestureIntent = new Intent();

                        ComponentName componentName = new ComponentName(packageName, className);
                        gestureIntent.setComponent(componentName);
                        if (isAppInstalled(gestureIntent)) {
                            //unLockScreen(context);
                            gestureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            context.startActivity(gestureIntent);
                        }
                    }
                }
                mGestureKey = "";
                mGestureInfo = "";
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "onReceive: ACTION_BOOT_COMPLETED");
            if (Utils.WIND_DEF_ASUS_GESTURES) {
                Utils.initGesturesItem();
            }
        }
    }

    private boolean isLockScreen(Context context) {
        KeyguardManager kgm = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (kgm != null) {
            if (kgm.isKeyguardSecure()) {
                return true;
            }
        }
        return false;
    }


    private void wakeUpCamera(Context context) {
        //wakeUpScreen(context);
        if (mPackageManager == null) {
            mPackageManager = context.getPackageManager();
        }

        String[] classInfo;
        classInfo = mGestureInfo.split("/");
        String packageName = classInfo[0];
        String className = classInfo[1];

        Intent gestureIntent;
        if (packageName.equals(CAMERA_PACKAGENAME) && className.equals(CAMERA_FRONT_CLASSNAME)) {
            gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
            className = CAMERA_BACK_CLASSNAME;
        } else if (packageName.equals(CAMERA_PACKAGENAME) && className.equals(CAMERA_BACK_CLASSNAME)) {
            gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_BACK_CAMERA");
        } else {
            return;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackage(packageName);

        if (isLockScreen(context)) {
            className = "com.asus.camera.SecureCameraApp";
        }

        ComponentName componentName = new ComponentName(packageName, className);
        gestureIntent.setComponent(componentName);
        if (isAppInstalled(gestureIntent)) {
            gestureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(gestureIntent);
        }
    }

    private void wakeUpScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        //pm.dismissKeyguardLw();
        PowerManager.WakeLock wl = pm.newWakeLock(/*PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK*/
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "bright");
        wl.acquire();
        wl.release();
    }

    private void unLockScreen(Context context) {
        wakeUpScreen(context);

        try {
            WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
        } catch (RemoteException e) {
            Log.w(TAG, "Remote Exception", e);
        }
    }

    private boolean isAppInstalled(Intent intent) {
        if (mPackageManager != null) {
            List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfos != null && resolveInfos.size() > 0) {
                return true;
            }
        }
        return false;
    }


    private String isFrontCamera(Context context, String gestureKey) {
        if (mPackageManager == null) {
            mPackageManager = context.getPackageManager();
        }

        String gestureInfo = Settings.System.getString(mContentResolver, gestureKey);

        if (gestureInfo.equals(AppLaunchListPreference.APP_FRONT_CAMERA)) {
            gestureInfo = "com.asus.camera/com.asus.camera.CameraApp.frontcamera";
        } else {
            gestureInfo = "";
        }
        return gestureInfo;
    }

    private void isMobilemanager(String gestureInfo) {
        if (gestureInfo.equals(AppLaunchListPreference.APP_ASUS_BOOSTER)) {
            mGestureInfo = "com.asus.mobilemanager/com.asus.mobilemanager.task.BoostShortcut";
        }

    }

    private String getGestureKey(int keyCode) {
        String gestureKey;
        if (keyCode == KeyEvent.KEYCODE_F13) {              //c, F13
            gestureKey = mGestureCount[5];
        } else if (keyCode == KeyEvent.KEYCODE_F14) {       //e, F14
            gestureKey = mGestureCount[4];
        } else if (keyCode == KeyEvent.KEYCODE_F15) {       //w, F15
            gestureKey = mGestureCount[2];
        } else if (keyCode == KeyEvent.KEYCODE_F17) {        //up, F17
            gestureKey = mGestureCount[0];
        } else if (keyCode == KeyEvent.KEYCODE_F18) {       //s, F18
            gestureKey = mGestureCount[3];
        } else if (keyCode == KeyEvent.KEYCODE_F19) {       //z, F19
            gestureKey = mGestureCount[6];
        } else if (keyCode == KeyEvent.KEYCODE_F20) {       //v, F20
            gestureKey = mGestureCount[7];
        } else if (keyCode == KeyEvent.KEYCODE_F21) {       //double click, F21
            gestureKey = mGestureCount[1];
        } else {
            gestureKey = "";
        }
        return gestureKey;
    }

    private boolean isDoubleAndSwipe(String gestureKey) {
        if (mGestureCount[0].equals(gestureKey) || mGestureCount[1].equals(gestureKey)) {
            Log.d(TAG, "isDoubleAndSwipe: ");
            return true;
        }
        return false;
    }

    private boolean isCamera() {
        if (mGestureInfo != null && mGestureInfo.equals(mFrontCamera)) {
            mGestureInfo = "com.asus.camera/com.asus.camera.CameraApp.frontcamera";
            return true;
        } else if (mGestureInfo.equals(mMainCamera)) {
            return true;
        }
        return false;
    }
}