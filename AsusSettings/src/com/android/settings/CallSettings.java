//xiongshigui@wind-mobi.com 20160525 merge begin
package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class CallSettings extends Fragment {

    private static final String TAG = "CallSettings";
    public static final String CALL_SETTINGS_PACKAGE = "com.android.phone";
    //A: huangyouzhong@wind-mobi.com 20160412 for 100655 -s
    //public static final String CALL_SETTINGS_CLASS = "com.android.phone.CallFeaturesSetting";
    public static final String CALL_SETTINGS_CLASS = "com.android.phone.settings.PhoneAccountSettingsActivity";
    //A: huangyouzhong@wind-mobi.com 20160412 for 100655 -e
    private boolean isCallSettingsTop(Context context) {
        int showLimit = 1;
        boolean running = false;

        // get the first task available
        ActivityManager mgr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = mgr.getRunningTasks(showLimit);

        if (tasks != null && tasks.size() > 0) {
            RunningTaskInfo firstTask = tasks.get(0);
            Log.d(TAG, "First task baseActivity: " + firstTask.baseActivity.getClassName());
            Log.d(TAG, "First task topActivity: " + firstTask.topActivity.getClassName());
            if (firstTask.topActivity.getClassName().equals(CALL_SETTINGS_CLASS)) {
                running = true;
            }
        }

        Log.d(TAG, "isCallSettingsTop() ? " + running);
        return running;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration config = getResources().getConfiguration();
        boolean xlargeScreen = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
        boolean isCallSettingsTop = isCallSettingsTop(getActivity());
        Log.d(TAG, "onCreate(): xlargeScreen = " + xlargeScreen + ", isCallSettingsTop = " + isCallSettingsTop);

        if (true) {//!xlargeScreen && !isCallSettingsTop) {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.setClassName(CALL_SETTINGS_PACKAGE, CALL_SETTINGS_CLASS);
            Log.d(TAG, "onCreate(): startActivity on Phone for intent = " + intent + " when CallFeaturesSetting is not on top");
            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Configuration config = getResources().getConfiguration();
        boolean xlargeScreen = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

        if (true) {//!xlargeScreen) {
            Activity activity = getActivity();
            if (activity != null) {
                String classtName = activity.getComponentName().getClassName();
                Log.d(TAG, "onStart(): classtName = " + classtName);
                // activity is of type com.android.settings.SubSettings on Phone and com.android.settings.Settings on Pad
                if (classtName.equals("com.android.settings.SubSettings")) {
                    Log.d(TAG, "onStart(): unregisterReceiver and finish on Phone mode for activity = " + activity);
                    //Settings settings = (Settings) activity;
                    // BroadcastReceiver receiver = settings.getReceiver();
                    // unregisterReceiver here to avoid leaked IntentReceiver (android.app.IntentReceiverLeaked) since it is registered in Settings onCreate
                    // activity.unregisterReceiver(receiver);
                    activity.finish();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }
}
//xiongshigui@wind-mobi.com 20160525 merge end