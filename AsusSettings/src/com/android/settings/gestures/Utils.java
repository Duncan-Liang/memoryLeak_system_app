/*
 * Filename : Utils.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Debug;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Utils {

    public static final boolean DEBUG = true;
    public static final boolean GESTURES_FEATURE_SUPPORTED = SystemProperties.get("ro.wind_gestures_supported").equals("1");
    private static final String TAG = "Utils";
    private static HashMap<String, Integer> mGesturesIndex = new HashMap<String, Integer>();
    private static HashMap<String, String> mGesturesSegs = new HashMap<String, String>();

    private static Utils mInstance = new Utils();

    private Utils() {
    }

    public static Utils getInstance() {
        return mInstance;
    }

    static {
        mGesturesIndex.put(GesturesSettings.DESTURES_ENABLED, 0);
        mGesturesIndex.put(GesturesSettings.GESTURE_DOUBLE_CLICK, 1);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_C, 2);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_E, 3);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_W, 4);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_S, 5);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_Z, 6);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_V, 7);
        mGesturesIndex.put(GesturesSettings.KEY_GESTURE_UP, 8);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_C, Settings.System.GESTURE_C);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_E, Settings.System.GESTURE_E);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_W, Settings.System.GESTURE_W);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_S, Settings.System.GESTURE_S);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_Z, Settings.System.GESTURE_Z);
        mGesturesSegs.put(GesturesSettings.KEY_GESTURE_V, Settings.System.GESTURE_V);
    }


    public static boolean isGestureEnabled(ContentResolver resolver, String key) {
        if (resolver != null) {
            String enabledList = Settings.System.getString(resolver, Settings.System.GESTURES_ENABLED);
            if (key == null || mGesturesIndex == null || enabledList == null
                    || enabledList.substring(0, 1).equals("0")) {
                return false;
            }

            int length = enabledList.length();
            int index = mGesturesIndex.get(key);

            if (index < length && index >= 0) {
                String value = enabledList.substring(index, index + 1);
                if (DEBUG) {
                    Log.i(TAG, "isEnabled: key=" + key + ", index=" + index + ", value=" + value + ", enabledList=" + enabledList);
                }
                if (value == null || value.equals("0")) {
                    return false;
                } else if (value.equals("1")) {
                    return true;
                }
            } else {
                return false;
            }

        }

        return false;
    }

    public static boolean isGestureChecked(ContentResolver resolver, String key) {
        if (resolver != null) {
            String enabledList = Settings.System.getString(resolver, Settings.System.GESTURES_ENABLED);
            if (key == null || mGesturesIndex == null || enabledList == null) {
                return false;
            }

            int length = enabledList.length();
            int index = mGesturesIndex.get(key);

            if (index < length && index >= 0) {
                String value = enabledList.substring(index, index + 1);
                if (DEBUG) {
                    Log.i(TAG, "isEnabled: key=" + key + ", index=" + index + ", value=" + value + ", enabledList=" + enabledList);
                }
                if (value == null || value.equals("0")) {
                    return false;
                } else if (value.equals("1")) {
                    return true;
                }
            } else {
                return false;
            }

        }

        return false;

    }

    public static boolean enableGesture(Context context, String key, boolean enabled) {
        Log.d(TAG, "enableGesture: " + Debug.getCallers(3, "\n"));
        if (context != null) {
            int index = mGesturesIndex.get(key);
            if (key.equals(GesturesSettings.DESTURES_ENABLED)) {
                enableDriver(context, enabled);
            } else {
                enableGesturesItem(context, key, enabled);
            }
            String enabledList = Settings.System.getString(context.getContentResolver(), Settings.System.GESTURES_ENABLED);
            int length = enabledList.length();
            if (index < length && index >= 0) {
                StringBuffer buffer = new StringBuffer(enabledList);

                if (enabled) {
                    buffer.replace(index, index + 1, "1");
                } else {
                    buffer.replace(index, index + 1, "0");
                }
                Settings.System.putString(context.getContentResolver(), Settings.System.GESTURES_ENABLED, buffer.toString());
            } else {
                return false;
            }
        }
        return true;
    }

    private static String getContentFromFile(Context context, String path) {
        FileReader fr = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        try {
            fr = new FileReader(path);
            br = new BufferedReader(fr);
            String content = "";
            while ((content = br.readLine()) != null) {
                content = content.trim();
                sb.append(content.charAt(content.length() - 1));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "getContentFromFile: content = " + sb.toString());
        return sb.toString();
    }

    public static void enableGesturesItem(Context context, String key, boolean enabled) {
        String enabledList = Settings.System.getString(context.getContentResolver(), Settings.System.GESTURES_ENABLED);
        String path = "/proc/android_touch/GESTURE";
        String gesturesDriverList = getContentFromFile(context, path);
        int index = mGesturesIndex.get(key);
        int length = enabledList.length();

        if (index >= 0 && index < length) {
            StringBuffer buffer = new StringBuffer(gesturesDriverList);
            switch (index) {
                case 1:  // doubel click
                    index = 0;
                    break;
                case 2:  //c, F13
                    index = 5;
                    break;
                case 3:  // e, F14
                    index = 12;
                    break;
                case 4: //w, F15
                    index = 11;
                    break;
                case 5: //s, F18
                    index = 9;
                    break;
                case 6: //z, F19
                    index = 6;
                    break;
                case 7: //v, F20
                    index = 10;
                    break;
                case 8: // swipe up
                    index = 1;
                    break;
                default:
                    break;
            }
            if (enabled) {
                buffer.replace(index, index + 1, "1");
            } else {
                buffer.replace(index, index + 1, "0");
            }
            gesturesDriverList = buffer.toString();
            Log.d(TAG, "enabledList11111 = " + gesturesDriverList);
        }

        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(path);
            Log.i(TAG, "printWriter " + printWriter);
            printWriter.write(gesturesDriverList.toCharArray());
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean setGestureIntent(ContentResolver resolver, String key, String info) {

        String data_seg = null;

        if (key == null) {
            return false;
        }

        data_seg = mGesturesSegs.get(key);
        if (data_seg != null) {
            Settings.System.putString(resolver, data_seg, info);
        } else {
            return false;
        }
        return true;
    }

    public static String getGestureIntent(ContentResolver resolver, String key) {
        String data_seg = null;

        if (key == null) {
            return null;
        }
        data_seg = mGesturesSegs.get(key);
        if (data_seg == null) {
            return null;
        }

        return Settings.System.getString(resolver, data_seg);
    }

    public static boolean isAppInstalled(Context context, Intent intent) {
        PackageManager manager = context.getPackageManager();
        if (manager != null) {
            List<ResolveInfo> resolveInfos = manager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolveInfos != null && resolveInfos.size() > 0) {
                return true;
            }
        }

        return false;
    }


    public static void enableDriver(Context context, boolean enabled) {
        String enableCode = "";
        if (enabled) {
            enableCode = "1";
        } else {
            enableCode = "0";
        }

        String path = "/proc/android_touch/SMWP";
        String path_2 = null;       // tp path
        Log.i(TAG, "enableDriver: enabled = " + enabled + " path: " + path + " path_2: " + path_2);

        File file = new File(path);
        if (!file.exists()) {
            path = path_2;
        }

        PrintWriter printWriter;
        try {
            Log.i(TAG, "file " + file);
            printWriter = new PrintWriter(path);
            Log.i(TAG, "printWriter " + printWriter);
            printWriter.write(enableCode.toCharArray());
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (String gestureKey : GesturesReceiver.mGestureCount) {
            initGesturesState(context, gestureKey, enableCode);
        }
    }

    private static void initGesturesState(Context context, String key, String enableCode) {
        String gesturesList = Settings.System.getString(context.getContentResolver(), Settings.System.GESTURES_ENABLED);
        String path = "/proc/android_touch/GESTURE";
        String gesturesDriverList = getContentFromFile(context, path);
        int index = mGesturesIndex.get(key);
        int length = gesturesList.length();

        if (index >= 0 && index < length) {
            StringBuilder buffer = new StringBuilder(gesturesDriverList);
            char enabled = gesturesList.charAt(index);
            switch (index) {
                case 1:  // doubel click
                    index = 0;
                    break;
                case 2:  //c, F13
                    index = 5;
                    break;
                case 3:  // e, F14
                    index = 12;
                    break;
                case 4: //w, F15
                    index = 11;
                    break;
                case 5: //s, F18
                    index = 9;
                    break;
                case 6: //z, F19
                    index = 6;
                    break;
                case 7: //v, F20
                    index = 10;
                    break;
                case 8: // swipe up
                    index = 1;
                    break;
                default:
                    break;
            }
            if (enableCode.equals("0")) {
                buffer.replace(index, index + 1, "0");
            } else if (enabled == '1') {
                buffer.replace(index, index + 1, "1");
            } else {
                buffer.replace(index, index + 1, "0");
            }
            gesturesDriverList = buffer.toString();
        }

        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(path);
            Log.i(TAG, "printWriter " + printWriter);
            printWriter.write(gesturesDriverList.toCharArray());
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void onPackageRemoved(Context context, String packageName) {
        ContentResolver resolver = context.getContentResolver();
        Set<String> set = mGesturesSegs.keySet();
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String info = getGestureIntent(resolver, key);
            if (info != null && !info.isEmpty()) {
                String[] classInfo = info.split("/");
                if (packageName.equals(classInfo[0])) {
                    setGestureIntent(resolver, key, "");
                    enableGesture(context, key, false);
                }
            }
        }
    }
}
