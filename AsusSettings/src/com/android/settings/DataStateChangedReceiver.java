// youxiaoyan@wind-mobi.com bug#108352 2016/4/29 begin
package com.android.settings;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.internal.telephony.dataconnection.DataSubSelector;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.provider.*;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;

public class DataStateChangedReceiver extends BroadcastReceiver {
    public String TAG = "DataStateChangedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        /**modify by xylinchao@wind-mobi.com 2016.06.10 start**/
        TelephonyManager tm=TelephonyManager.from(context);
        Log.d(TAG, "onReceive: action="+action);
        Log.d(TAG, "onReceive: dataEnable="+tm.getDataEnabled());
        if(action.equals(DataSubSelector.ACTION_MOBILE_DATA_ENABLE)) {
            if(tm.getDataEnabled()){
                // pengfugen@wind-mobi.com 2016/06/12 for 106730 start
                if(SystemProperties.get("ro.wind.def.asus.systemui").equals("1")) {
                    setDataSlotIdForAsus(context, tm.getDataEnabled());
                }
                // pengfugen@wind-mobi.com 2016/06/12 106730 end
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA, 1 );
            }else{
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.MOBILE_DATA, 0 );
            }
        } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
            // pengfugen@wind-mobi.com 2016/06/12 for 106730 start
            if(SystemProperties.get("ro.wind.def.asus.systemui").equals("1")) {
                setDataSlotIdForAsus(context, tm.getDataEnabled());
            }
            // pengfugen@wind-mobi.com 2016/06/12 106730 end
        }
//        PhoneConstants.DataState state = getMobileDataState(intent);
//        Log.d(TAG, "onReceive action is " + action);
//        if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
//            Log.d(TAG, "onReceive DataState is " + state);
//            if( state == PhoneConstants.DataState.CONNECTED ){
//                android.provider.Settings.Global.putInt(context.getContentResolver(), android.provider.Settings.Global.MOBILE_DATA, 1);
//            } else if( state == PhoneConstants.DataState.DISCONNECTED ){
//                android.provider.Settings.Global.putInt(context.getContentResolver(), android.provider.Settings.Global.MOBILE_DATA, 0);
//            }
//        }
//    }
//
//    private PhoneConstants.DataState getMobileDataState(Intent intent) {
//        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
//        if (str != null) {
//            return Enum.valueOf(PhoneConstants.DataState.class, str);
//        } else {
//            return PhoneConstants.DataState.DISCONNECTED;
//        }
        /**modify by xylinchao@wind-mobi.com 2016.06.10 end**/
    }

    // pengfugen@wind-mobi.com 2016/06/12 for 106730 start
    private void setDataSlotIdForAsus(Context context, boolean dataEnable) {
        if (dataEnable) {
            int phoneSubId = SubscriptionManager.getDefaultDataSubId();
            int preSlotId = Integer.parseInt(SystemProperties.get("persist.asus.mobile_slot", "-1"));
            int curSlotId = SubscriptionManager.from(context).getSlotId(phoneSubId);
            Log.d(TAG, "setDataSlotIdForAsus(pfg): preSlotId = " + preSlotId + " curSlotId = " + curSlotId + " phoneSubId = " + phoneSubId);
            if ((curSlotId != preSlotId) && SubscriptionManager.isValidSlotId(curSlotId)) {
                SystemProperties.set("persist.asus.mobile_slot", "" + curSlotId);
            }
        }
    }
    // pengfugen@wind-mobi.com 2016/06/12 for 106730 end
}

// youxiaoyan@wind-mobi.com bug#108352 2016/4/29 end
