package com.mediatek.settings.sim;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;
//A: huangyouzhong@wind-mobi.com 20160805 for asus question -s
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.settings.Utils;
import android.os.UserHandle;
import android.content.Intent;
import android.content.ComponentName;
//A: huangyouzhong@wind-mobi.com 20160805 for asus question -e
/**
 * Radio power manager to control radio state.
 */
public class RadioPowerController {

    private static final String TAG = "RadioPowerController";
    private Context mContext;
    private static final int MODE_PHONE1_ONLY = 1;
    private ISimManagementExt mExt;
    private static RadioPowerController sInstance = null;

   /**
    * Constructor.
    * @param context Context
    */
    private RadioPowerController(Context context) {
        mContext = context;
        mExt = UtilsExt.getSimManagmentExtPlugin(mContext);
    }

    private static synchronized void createInstance(Context context) {
        if(sInstance == null) {
            sInstance = new RadioPowerController(context);
        }
    }

    public static RadioPowerController getInstance(Context context) {
        if(sInstance == null) {
            createInstance(context);
        }
        return sInstance;
    }

    public boolean needRadioSwitch(int subId, boolean turnOn) {
        boolean needSwitch = false;
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            boolean isRadioOn = TelephonyUtils.isRadioOn(subId, mContext);
            Log.d(TAG, "needRadioSwitch: subId: " + subId + ", isRadioOn: " + isRadioOn);
            if (isRadioOn != turnOn) {
                needSwitch = true;
            }
        }
        Log.d(TAG, "needRadioSwitch(" + subId + ")" + " : " + needSwitch + ", turnOn: " + turnOn);
        return needSwitch;
    }

    public boolean setRadionOn(int subId, boolean turnOn) {
        Log.d(TAG, "setRadionOn, turnOn: " + turnOn + ", subId = " + subId);
        boolean isSuccessful = false;
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return isSuccessful;
        }
        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(
                Context.TELEPHONY_SERVICE));
        try {
            if (telephony != null) {
                isSuccessful = telephony.setRadioForSubscriber(subId, turnOn);
                if (isSuccessful) {
                    updateRadioMsimDb(subId, turnOn);
                    /// M: for plug-in
                    mExt.setRadioPowerState(subId, turnOn);
                    //A: huangyouzhong@wind-mobi.com 20160805 for asus question -s
                    Intent intent = new Intent(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
                    intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    intent.putExtra(SubscriptionManager.UNIQUE_KEY_SUBSCRIPTION_ID, subId);
                    int slot = SubscriptionManager.getSlotId(subId);
                    intent.putExtra("intContent", (turnOn ? 1 : 0));
                    intent.putExtra("columnName", "sub_state");
                    Log.d(TAG, "send ACTION_SUBINFO_CONTENT_CHANGE slot:"+slot+" subId:"+subId+" turnOn:"+turnOn);
                    mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    if(turnOn) {
                        intent.setAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
                        if(Utils.isAsusApkCN()) {
                            intent.setComponent(new ComponentName("com.asus.cncontacts","com.android.contacts.simcardmanage.SimCardReceiver"));
                        } else {
                            intent.setComponent(new ComponentName("com.asus.contacts","com.android.contacts.simcardmanage.SimCardReceiver"));
                        }
                        intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE, IccCardConstants.INTENT_VALUE_ICC_LOADED);
                        intent.putExtra(PhoneConstants.SLOT_KEY, slot);
                        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
                        Log.d(TAG, "send ACTION_SIM_STATE_CHANGED");
                        mContext.sendBroadcastAsUser(intent, UserHandle.OWNER);
                    }
                    //A: huangyouzhong@wind-mobi.com 20160805 for asus question -e
                }
            } else {
                Log.d(TAG, "telephony is null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "setRadionOn, isSuccessful: " + isSuccessful);
        return isSuccessful;
    }

    private void updateRadioMsimDb(int subId, boolean turnOn) {
        int priviousSimMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        Log.i(TAG, "updateRadioMsimDb, The current dual sim mode is " + priviousSimMode
                + ", with subId = " + subId);
        int currentSimMode;
        boolean isPriviousRadioOn = false;
        int slot = SubscriptionManager.getSlotId(subId);
        int modeSlot = MODE_PHONE1_ONLY << slot;
        if ((priviousSimMode & modeSlot) > 0) {
            currentSimMode = priviousSimMode & (~modeSlot);
            isPriviousRadioOn = true;
        } else {
            currentSimMode = priviousSimMode | modeSlot;
            isPriviousRadioOn = false;
        }

        Log.d(TAG, "currentSimMode=" + currentSimMode + " isPriviousRadioOn =" + isPriviousRadioOn
                + ", turnOn: " + turnOn);
        if (turnOn != isPriviousRadioOn) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.MSIM_MODE_SETTING, currentSimMode);
        } else {
            Log.w(TAG, "quickly click don't allow.");
        }
    }
}