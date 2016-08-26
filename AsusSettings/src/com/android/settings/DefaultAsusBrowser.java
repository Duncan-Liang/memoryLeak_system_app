//youxiaoyan@wind-mobi.com 20160324 for feature100634 -s
package com.android.settings;

import com.android.internal.telephony.SmsApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;


public class DefaultAsusBrowser extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int myUserId;
        final PackageManager pm;

        pm = context.getPackageManager();
        myUserId = UserHandle.myUserId();

        String action = intent.getAction();
        Log.v("DefaultAsusBrowser action = ", action);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals("android.intent.action.ACTION_BOOT_IPO")) {
            String packageName = pm.getDefaultBrowserPackageName(myUserId);
            if (!TextUtils.isEmpty(packageName)) {
                pm.setDefaultBrowserPackageName("com.asus.browser", myUserId);
            }

            // youxiaoyan@wind-mobi.com bug#108251 2016/5/25 begin
            SharedPreferences mPreferences;
            SharedPreferences.Editor mEditor;
            mPreferences = context.getSharedPreferences("com.android.settings_preferences", Context.MODE_PRIVATE);
            if (!mPreferences.getBoolean("flag", false)) {
                SmsApplication.setDefaultApplication("com.asus.cnmessage", context);
                mEditor = mPreferences.edit();
                mEditor.putBoolean("flag", true);
                mEditor.commit();
            }
            // youxiaoyan@wind-mobi.com bug#108251 2016/5/25 end
        }
    }
}
//youxiaoyan@wind-mobi.com 20160324 feature100634 -e