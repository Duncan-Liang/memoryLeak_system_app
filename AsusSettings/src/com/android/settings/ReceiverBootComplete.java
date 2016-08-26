package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
// xiongshigui@wind-mobi.com 2016/8/21 add begin
import android.os.SystemProperties;
// xiongshigui@wind-mobi.com 2016/8/21 add end

public class ReceiverBootComplete extends BroadcastReceiver {

    private static final String TAG = "ReceiverBootComplete";
    private static final boolean DEBUG = false;

    // xiongshigui@wind-mobi.com 2016/8/21 add begin
    private static final boolean WIND_FOTA_DEL_FP_SUPPORTED = "1".equals(SystemProperties.get("ro.wind.def.fota.del.finger"));
    private static final boolean WIND_IS_FOTA_END_VERSION = SystemProperties.get("ro.build.wind.version").contains("ASUS_X008D_WW_V1.0B18");
    // xiongshigui@wind-mobi.com 2016/8/21 add end

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "ReceiverBootComplete onReceive.");

        if (WIND_FOTA_DEL_FP_SUPPORTED && WIND_IS_FOTA_END_VERSION) {
            //Intent newIntent = new Intent("com.wind.intent.action.REMOVE_ALL_FPS");
            //newIntent.setPackage(context.getPackageName());
            Intent newIntent = new Intent();
            newIntent.setComponent(new ComponentName("com.android.settings","com.android.settings.fingerprint.RemoveFingerprintsService"));
            context.startService(newIntent);
        }

        String REG_SKU = android.os.SystemProperties.get("ro.product.name", "");
        Log.d(TAG, "ro.product.name: " + REG_SKU);
        if (REG_SKU.toLowerCase().startsWith("att") || DEBUG) {
            Log.i(TAG, "Enable UsageManager.");
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(
                    new ComponentName(context, UsageManagerSettings.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            Log.i(TAG, "Disable UsageManager.");
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(
                    new ComponentName(context, UsageManagerSettings.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
