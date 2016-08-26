package com.android.settings.users;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.android.settings.AsusNoNetworkDialog;
import com.android.settings.R;

public class SnapViewUtils {
    private Context mContext;
    /* Weibo developer info provided from ZenUIServices team*/
    public static final String WEIBO_CONSUMER_KEY = "18427081";
    public static final String WEIBO_CONSUMER_SECRET = "ba272af4aeeb371dcc3fd3f523dc8895";
    public static final String WEIBO_REDIRECT_URL = "http://www.asus.com";

    public SnapViewUtils (Context mContext)
    {
        this.mContext = mContext;
    }

    private static boolean isCNProduct() {
        String SKU = android.os.SystemProperties.get("ro.product.name", "");
        return ((SKU != null) && SKU.toLowerCase().startsWith("cn")) ? true : false;
    }

    private static boolean isCNBuild() {
        String SKU = android.os.SystemProperties.get("ro.build.asus.sku", "");
        if (SKU != null) {
            return (SKU.toLowerCase().startsWith("cn") ||
                    SKU.toLowerCase().startsWith("cucc") ||
                    SKU.toLowerCase().startsWith("cta") ||
                    SKU.toLowerCase().startsWith("iqy")) ? true : false;
        } else {
            return false;
        }
    }

    public static boolean isCNSKU() {
        return (isCNProduct() || isCNBuild()) ? true : false;
    }

    public static String getAccountType(Context c) {
        if (isCNSKU())
            return c.getResources().getString(R.string.weibo);
        else
            return c.getResources().getString(R.string.google);
    }

    public static String getAccountType2(Context c) {
        if (isCNSKU())
            return c.getResources().getString(R.string.weibo);
        else
            return c.getResources().getString(R.string.gmail);
    }

    public static String getCombined2String(Context c, int res1, String str2) {
        String str1 = (String) c.getResources().getText(res1);
        return String.format(str1, str2);
    }

    public static String getCombined3String(Context c, int res1, String str2) {
        String str1 = (String) c.getResources().getText(res1);
        return String.format(str1, str2, str2);
    }

    public static String parseUID(String s)
    {
        String uid = null;
        JSONObject json;
        try {
            json = new JSONObject(s);
            uid = json.getString("uid");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return uid;
    }

    public static void resetSnapViewGlobalValues(ContentResolver cr) {
        SnapViewProviderUtil.Secure.putAccount(cr, SnapViewProviderUtil.ACCOUNT, null);
        SnapViewProviderUtil.Secure.putAccount(cr, SnapViewProviderUtil.QUESTION, null);
        SnapViewProviderUtil.Secure.putAccount(cr, SnapViewProviderUtil.ANSWER, null);
        // Settings.Global.putInt(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_AVATAR, 0);
        // Settings.Global.putInt(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_LOCK, 2);
        // Settings.Global.putInt(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_NOTIFY, 0);
        // Settings.Global.putInt(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_HINT_COLOR, 0xeda00b);
        // Settings.Global.putString(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY, null);
        // Settings.Global.putInt(cr,
        // Settings.Global.USER_SNAPVIEW_SETTINGS_DUMMY_NOTIFY_ICON, 2);
    }

    /**
     * Check if there is a valid network connection, whether it is via mobile data, Wifi, or others.
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null)
            return false;

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) {
            showNoNetworkDialog(context);
            return false;
        }
        NetworkInfo info = connMgr.getActiveNetworkInfo();
        if (info != null && info.isConnected())
            return true;
        else {
            showNoNetworkDialog(context);
            return false;
        }
    }

    private static void showNoNetworkDialog(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, AsusNoNetworkDialog.class);
        context.startActivity(intent);
    }
}
