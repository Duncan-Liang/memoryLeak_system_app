/*
 * Filename : GesturesReceiver.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import java.util.List;


public class GesturesReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = Utils.DEBUG; 
    private static final String TAG = "GesturesReceiver";

    private PackageManager mPackageManager;
    private ContentResolver mContentResolver;
    private static String mGestureKey = "";
    public static final String[] mGestureCount = {
            GesturesSettings.GESTURE_DOUBLE_CLICK,
            GesturesSettings.KEY_GESTURE_C,
            GesturesSettings.KEY_GESTURE_E,
            GesturesSettings.KEY_GESTURE_W,
            GesturesSettings.KEY_GESTURE_S,
            GesturesSettings.KEY_GESTURE_Z,
            GesturesSettings.KEY_GESTURE_V,
            GesturesSettings.KEY_GESTURE_UP};

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Utils.GESTURES_FEATURE_SUPPORTED) {
            return;
        }

        String action = intent.getAction();
        Bundle data = intent.getExtras();
        if (DEBUG) {
            Log.i(TAG, "action:" + action);
        }

        if (mContentResolver == null) {
            mContentResolver = context.getContentResolver();
        }

        if (action.equals("com.android.intent.gestures")) {
            if (data != null) {
                int keyCode = data.getInt("keyCode");
                String gestureKey = null;
                Log.d(TAG, "keyCode" + keyCode);
                if (keyCode == KeyEvent.KEYCODE_F13) {
                    //c, F13
                    gestureKey = GesturesSettings.KEY_GESTURE_C;
                } else if (keyCode == KeyEvent.KEYCODE_F14) {
                    //e, F14
                    gestureKey = GesturesSettings.KEY_GESTURE_E;
                } else if (keyCode == KeyEvent.KEYCODE_F15) {
                    //w, F15
                    gestureKey = GesturesSettings.KEY_GESTURE_W;
                } else if (keyCode == KeyEvent.KEYCODE_F16) {
                    //o, F16
                    gestureKey = GesturesSettings.KEY_GESTURE_O;
                } else if (keyCode == KeyEvent.KEYCODE_F17) {
                    //up, F17
                    gestureKey = GesturesSettings.KEY_GESTURE_UP;
                } else if (keyCode == KeyEvent.KEYCODE_F18) {
                    //s, F18
                    gestureKey = GesturesSettings.KEY_GESTURE_S;
                } else if (keyCode == KeyEvent.KEYCODE_F19) {
                    //z, F19
                    gestureKey = GesturesSettings.KEY_GESTURE_Z;
                } else if (keyCode == KeyEvent.KEYCODE_F20) {
                    //v, F20
                    gestureKey = GesturesSettings.KEY_GESTURE_V;
                } else if (keyCode == KeyEvent.KEYCODE_F21) {
                    //double click, F21
                    gestureKey = GesturesSettings.GESTURE_DOUBLE_CLICK;
                } else {
                    //we only handle the keyCode pre-defined
                    return;
                }

                ContentResolver resolver = context.getContentResolver();

                boolean enabled = Utils.getInstance().isGestureEnabled(resolver, gestureKey);
                if (DEBUG) {
                    Log.i(TAG, "keyCode:" + keyCode + ", key=" + gestureKey + ", enabled=" + enabled);
                }

                if (enabled) {
                    if (GesturesSettings.GESTURE_DOUBLE_CLICK.equals(gestureKey) || GesturesSettings.KEY_GESTURE_UP.equals(gestureKey)) {
                        unLockScreen(context);
                    } else if (wakeUpCamera(context, gestureKey)) {
                       // Don't do anything
                    } else {
                        unLockScreen(context);
                        mGestureKey = gestureKey;
                    }
                }
            }
        } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
            if (!mGestureKey.equals("")) {
                if (mPackageManager == null) {
                    mPackageManager = context.getPackageManager();
                }

                //unlock and launch app
                ContentResolver resolver = context.getContentResolver();
                String gestureInfo = Utils.getGestureIntent(resolver, mGestureKey);
                String[] classInfo;
                if (gestureInfo != null && !gestureInfo.equals("")) {
                    classInfo = gestureInfo.split("/");
                    String packageName = classInfo[0];
                    String className = classInfo[1];
                    if (DEBUG) {
                        Log.i(TAG, "packageName:" + packageName + ", className=" + className);
                    }

                    Intent gestureIntent;
                    if (packageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && className.equals(GesturesSettings.CAMERA_FRONT_CLASSNAME)) {
                        gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
                        className = GesturesSettings.CAMERA_BACK_CLASSNAME;
                        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        am.forceStopPackage(packageName);
                    } else if (packageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && className.equals(GesturesSettings.CAMERA_BACK_CLASSNAME)) {
                        gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_BACK_CAMERA");
                        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                        am.forceStopPackage(packageName);
                    } else {
                        gestureIntent = new Intent();
                    }

                    ComponentName componentName = new ComponentName(packageName, className);
                    gestureIntent.setComponent(componentName);
                    if (isAppInstalled(gestureIntent)) {
                        //unLockScreen(context);
                        gestureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        context.startActivity(gestureIntent);
                    } else {
                        //if app doesnot installed,set to disabled	
                        Utils.enableGesture(context, mGestureKey, false);
                    }
                }
                mGestureKey = "";
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (Utils.GESTURES_FEATURE_SUPPORTED) {
                boolean gestureState = Utils.getInstance().isGestureEnabled(mContentResolver, GesturesSettings.DESTURES_ENABLED);
                Utils.enableDriver(context, gestureState);
                for (String gestureKey : mGestureCount) {
                    boolean gestureItemState = Utils.getInstance().isGestureEnabled(mContentResolver, gestureKey);
                    Utils.enableGesturesItem(context, gestureKey, gestureItemState);
                }
            }
        } else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {

        } else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            if (packageName == null || packageName.length() == 0) {
                return;
            }
            Utils.onPackageRemoved(context, packageName);
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

    private boolean wakeUpCamera(Context context, String gestureKey) {
        if (!isLockScreen(context)) {
            return false;
        }
        wakeUpScreen(context);
        if (!gestureKey.equals("")) {
            if (mPackageManager == null) {
                mPackageManager = context.getPackageManager();
            }

            //unlock and launch app
            ContentResolver resolver = context.getContentResolver();
            String gestureInfo = Utils.getGestureIntent(resolver, gestureKey);
            String[] classInfo;
            if (gestureInfo != null && !gestureInfo.equals("")) {
                classInfo = gestureInfo.split("/");
                String packageName = classInfo[0];
                String className = classInfo[1];

                Intent gestureIntent;
                if (packageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && className.equals(GesturesSettings.CAMERA_FRONT_CLASSNAME)) {
                    gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
                } else if (packageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && className.equals(GesturesSettings.CAMERA_BACK_CLASSNAME)) {
                    gestureIntent = new Intent("com.asus.camera.action.STILL_IMAGE_BACK_CAMERA");
                } else {
                    return false;
                }

                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                am.forceStopPackage(packageName);

                className = "com.asus.camera.SecureCameraApp";

                ComponentName componentName = new ComponentName(packageName, className);
                gestureIntent.setComponent(componentName);
                if (isAppInstalled(gestureIntent)) {
                    gestureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    context.startActivity(gestureIntent);
                } else {
                    Utils.enableGesture(context, mGestureKey, false);
                }
                return true;
            }
            return false;
        }

        return false;
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


}