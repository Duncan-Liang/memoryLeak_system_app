/*
 * Filename : Utils.java
 * Detail description
 *
 *
 * Author: wangyan@wind-mobi.com add Feature#110195
 * created at 2016/06/24
 */

package com.android.settings.zenmotion;

import android.os.SystemProperties;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class Utils {

    private static final String TAG = "Zenmotion.Utils";
    private static final String PERSIST_ASUS_GESTURE_TYPE = "persist.asus.gesture.type";
    private static final String DEFAULT_ASUS_GESTURE_TYPE = "1111111";
    public static final boolean WIND_DEF_ASUS_GESTURES = SystemProperties.get("ro.wind.def.asus.gestures").equals("1");

    private static HashMap<String, Integer> mGesturesIndex = new HashMap<String, Integer>();
    private static Utils mInstance = new Utils();

    private Utils() {
    }

    public static Utils getInstance() {
        return mInstance;
    }

    static {
        mGesturesIndex.put(AppLaunchListPreference.KEY_SWIPE_UP, 0);
        mGesturesIndex.put(AppLaunchListPreference.KEY_DOUBLE_ON, 1);
        mGesturesIndex.put(AppLaunchListPreference.KEY_W_LAUNCH, 2);
        mGesturesIndex.put(AppLaunchListPreference.KEY_S_LAUNCH, 3);
        mGesturesIndex.put(AppLaunchListPreference.KEY_E_LAUNCH, 4);
        mGesturesIndex.put(AppLaunchListPreference.KEY_C_LAUNCH, 5);
        mGesturesIndex.put(AppLaunchListPreference.KEY_Z_LAUNCH, 6);
        mGesturesIndex.put(AppLaunchListPreference.KEY_V_LAUNCH, 7);
    }

    private static void DriverGesturesMain(boolean enabled) {
        String enableCode;
        if (enabled) {
            enableCode = "1";
        } else {
            enableCode = "0";
        }

        String path = "/proc/android_touch/SMWP";
        String path_2 = null;       // tp path

        File file = new File(path);
        if (!file.exists()) {
            path = path_2;
        }

        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(path);
            printWriter.write(enableCode.toCharArray());
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void DriverGesturesItem(String key, boolean enableCode) {
        String path = "/proc/android_touch/GESTURE";
        String gesturesDriverList = getContentFromFile(path);
        StringBuilder buffer = new StringBuilder(gesturesDriverList);
        int index = mGesturesIndex.get(key);

        switch (index) {
            case 0:     // swipe up
                index = 1;
                break;
            case 1:  // doubel click
                index = 0;
                break;
            case 2: //w, F15
                index = 11;
                break;
            case 3: //s, F18
                index = 9;
                break;
            case 4:  // e, F14
                index = 12;
                break;
            case 5:  //c, F13
                index = 5;
                break;
            case 6: //z, F19
                index = 6;
                break;
            case 7: //v, F20
                index = 10;
                break;
            default:
                return;
        }

        if (enableCode) {
            buffer.replace(index, index + 1, "1");
        } else {
            buffer.replace(index, index + 1, "0");
        }
        gesturesDriverList = buffer.toString();

        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(path);
            printWriter.write(gesturesDriverList.toCharArray());
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String getContentFromFile(String path) {
        BufferedReader br;
        FileReader fr = null;
        StringBuilder sb = new StringBuilder();

        try {
            fr = new FileReader(path);
            br = new BufferedReader(fr);
            String content;
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

    private static void setDriverGesturesDoubleAndSwipe() {
        int swipe = SystemProperties.getInt(AsusTouchSettings.PERSIST_ASUS_SWIPE, 0);
        int dlick = SystemProperties.getInt(AsusTouchSettings.PERSIST_ASUS_DLICK, 0);

        boolean swipeEnabled = (swipe == 1 ? true : false);
        boolean doubleEnabled = (dlick == 1 ? true : false);

        DriverGesturesItemSwitch(AppLaunchListPreference.KEY_SWIPE_UP, swipeEnabled);
        DriverGesturesItemSwitch(AppLaunchListPreference.KEY_DOUBLE_ON, doubleEnabled);
    }

    private static void setDriverGesturesMainSwitch() {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
        char enabled = opString.charAt(0);

        boolean isEnable = (enabled == '1' ? true : false);

        DriverGesturesMainSwitch(isEnable);
    }

    private static void SetDriverGesturesItemSwitch() {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);

        char enabled;
        String key = null;
        boolean isEnable;
        for (int i = opString.length() - 1; i > 0; i--)
        {
            switch (i) {
                case 1:
                    key = "w_launch";
                    break;
                case 2:
                    key = "s_launch";
                    break;
                case 3:
                    key = "e_launch";
                    break;
                case 4:
                    key = "c_launch";
                    break;
                case 5:
                    key = "z_launch";
                    break;
                case 6:
                    key = "v_launch";
                    break;
                default:
                    break;
            }
            enabled =  opString.charAt(i);

            if (enabled == '1') {
                isEnable = true;
            } else {
                isEnable = false;
            }

            if (key != null) {
                DriverGesturesItemSwitch(key, isEnable);
            }
        }
    }

    public static void initGesturesItem() {
        setDriverGesturesMainSwitch();
        setDriverGesturesDoubleAndSwipe();
    }

    public static void DriverGesturesMainSwitch(boolean enabled) {
        DriverGesturesMain(enabled);

        if (enabled) {
            SetDriverGesturesItemSwitch();
        }
    }

    public static void DriverGesturesItemSwitch(String key, boolean enabled) {
        DriverGesturesItem(key, enabled);
    }

}
