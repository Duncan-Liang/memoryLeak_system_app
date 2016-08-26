package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import java.util.Observable;
import java.util.Observer;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.switchenabler.AsusOneHandOperationEnabler;
import com.android.settings.util.ResCustomizeConfig;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class AsusSelectShortcutSettings extends Activity
{
    private static final String TAG = "AsusSelectShortcutSettings";

    private ListView mListView;
    private PackageManager mPm;
    private ApplicationInfo mApplicationInfo;
    private String[] mAppName;
    private Drawable[] mAppIcon;
    private String[] mShortcutPackageName;
    private String[] mShortcutClassName;
    public static final boolean IS_WIFI_ONLY = "wifi-only".equals(SystemProperties.get("ro.carrier"));
    public static final boolean IS_KDDI = "kddi".equalsIgnoreCase(SystemProperties.get("ro.carrier"));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.xml.asus_lockscreen_shortcut_list);
        mListView = (ListView) findViewById(R.id.shortcut_list);
        mPm = getBaseContext().getPackageManager();
        mShortcutPackageName = new String[3];
        mShortcutClassName = new String[3];
        mAppIcon = new Drawable[3];
        mAppName = new String[3];
        loadShortcutInfo();
        mListView.setAdapter(new IconTextAdapter(this, android.R.layout.simple_list_item_1, mAppName, mAppIcon));
        mListView.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                    int position, long id){
                Intent intent = new Intent(getBaseContext(), AsusSelectAPShortcutSettings.class);
                Bundle bundle = new Bundle();
                bundle.putLong("rowId", id);
                bundle.putBoolean("isKeyguard", false);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        // ++ colorful bar
        Utils.inflateOriginalLayout(this, getLayoutInflater(),
                R.xml.asus_lockscreen_shortcut_list);
        Utils.updateColorfulTextView(this, R.color.actionbar_background,
                         R.id.shortcut_textViewColorful);
    }

    public class IconTextAdapter extends ArrayAdapter<String> {
        Drawable[] icons;
        String[] itemName;

        public IconTextAdapter(Context context, int textViewResourceId,
                String[] item_Name, Drawable[] images) {
            super(context, textViewResourceId, item_Name);

            icons = images;
            itemName = item_Name;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View rowview = convertView;
            if (rowview == null)
            {
              LayoutInflater inflater = getLayoutInflater();
                rowview = inflater.inflate(R.xml.asus_lockscreen_shortcut_list_row, null);
            }

            TextView Name = (TextView) rowview.findViewById(R.id.List_Item_Name);
            Name.setText(itemName[position]);

            ImageView icon = (ImageView) rowview.findViewById(R.id.List_Item_Icon);
            icon.setImageDrawable(icons[position]);

            return rowview;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadShortcutInfo();
        mListView.setAdapter(new IconTextAdapter(this, R.xml.asus_lockscreen_shortcut_list_row, mAppName, mAppIcon));
    }

    private void splitPackageNameAndClassName(){
        try{
            String settingShortcutString = Settings.System.getString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME);
            if (settingShortcutString == null) {
                Resources res = getBaseContext().getResources();
                int configStringRes = IS_WIFI_ONLY ? R.string.keyguard_default_shortcuts_wifi_only
                        : R.string.keyguard_default_shortcuts_telephony;
                settingShortcutString = res.getString(configStringRes);
                Settings.System.putString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME,
                        settingShortcutString);
            }
            Log.i("mytest","settingShortcutString = "+settingShortcutString);
        String[] packageAndClassName  = settingShortcutString.split(", ");

        for(int i = 0; i < 3; ++i) {
            try{
            String[] tmp = packageAndClassName[i].split("/");
            if (isAppInstalled(tmp[0]) || isAppOnSdcard(tmp[0])) {
                mShortcutPackageName[i] = tmp[0];
                mShortcutClassName[i] = tmp[1];
            } else {
                packageAndClassName[i] = handleShortcutIfAPPIsUninstalled(i);
                Log.i("mytest","packageAndClassName = "+packageAndClassName[i]);
                String[] tmpPackageAndClassName = packageAndClassName[i].split("/");
                mShortcutPackageName[i] = tmpPackageAndClassName[0];
                mShortcutClassName[i] = tmpPackageAndClassName[1];
            }
            }catch(Exception err){
                Log.d(TAG, "splitPackageNameAndClassName" + err);
            }
        }
        }catch(Exception err){
            Log.d(TAG, "splitPackageNameAndClassName" + err);
        }
    }

    public boolean isAppInstalled(String packageName) {
        PackageManager pm = getBaseContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }
    private boolean enableAPP2SD(){
        //TODO
        return false;//Environment.isSecondaryExternalStorageRemovable();
    }

    public boolean isAppOnSdcard(String packageName){
        if(!enableAPP2SD()){
            return false;
        }
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName,
                                    PackageManager.GET_UNINSTALLED_PACKAGES
                                    |PackageManager.GET_DISABLED_COMPONENTS
                                    |PackageManager.GET_SIGNATURES);
            if(info == null){
                return false;
            }
            if((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0
                /*&& (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0*/){
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private String handleShortcutIfAPPIsUninstalled(int index) {
        String settingShortcut = Settings.System.getString(getBaseContext().getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME);
        String appInfoString = null;
        Resources res = getBaseContext().getResources();
        int defaultShortcutList = IS_WIFI_ONLY ? R.string.keyguard_default_shortcuts_wifi_only
                : R.string.keyguard_default_shortcuts_telephony;
        if(isVZWSku())
            defaultShortcutList = R.string.keyguard_default_shortcuts_vzw;
        String configShortcutString = res
                .getString(defaultShortcutList);
        String[] configPackageAndClassName = configShortcutString.split(", ");
        String[] settingPackageAndClassName = settingShortcut.split(", ");

        for (int i = 0; i < 3; ++i) {
            boolean add = true;
            for (int j = 0; j < 3; ++j) {
                if (configPackageAndClassName[i]
                        .equals(settingPackageAndClassName[j])) {
                    add = false;
                    break;
                }
            }
            if (add) {
                appInfoString = configPackageAndClassName[i];
                String newSettingShortcut = "";
                for(int k = 0; k < 3; ++k) {
                    if (k != 0) {
                        newSettingShortcut = newSettingShortcut + ", ";
                    }

                    if (k == index) {
                        newSettingShortcut = newSettingShortcut + appInfoString;
                    } else {
                        newSettingShortcut = newSettingShortcut
                                + settingPackageAndClassName[k];
                    }
                }

                Settings.System.putString(getBaseContext().getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME, newSettingShortcut);
                return appInfoString;
            }
        }
        return appInfoString;
    }

    private void loadShortcutInfo(){
        splitPackageNameAndClassName();
        for(int i = 0; i < 3; ++i) {
            try {
                ComponentName name = new ComponentName(mShortcutPackageName[i],mShortcutClassName[i]);
                ActivityInfo activityInfo = mPm.getActivityInfo(name, PackageManager.GET_UNINSTALLED_PACKAGES
                        |PackageManager.GET_DISABLED_COMPONENTS
                        |PackageManager.GET_SIGNATURES);
                mAppIcon[i] = activityInfo.loadIcon(mPm);
                mAppName[i] = (String) activityInfo.loadLabel(mPm);
                //ComponentName name = new ComponentName(mShortcutPackageName[i],mShortcutClassName[i]);
                /*ApplicationInfo appInfo = mPm.getApplicationInfo(mShortcutPackageName[i], 0);
                Log.i(TAG,"mShortcutPackageName ="+mShortcutPackageName[i]);
                //ActivityInfo activityInfo = mPm.getActivityInfo(name, 0);
                mAppIcon[i] = mPm.getApplicationIcon(appInfo);//activityInfo.loadIcon(mPm);
                mAppName[i] = (String) mPm.getApplicationLabel(appInfo);//(String) activityInfo.loadLabel(mPm);
*/            } catch (final NameNotFoundException e) {
                if(isAppOnSdcard(mShortcutPackageName[i])){
                    mAppIcon[i] = getResources().getDrawable(ResCustomizeConfig.getIdentifier(getBaseContext(), "drawable", "sym_app_on_sd_unavailable_icon"));
                    mAppName[i] = mShortcutPackageName[i];
                } else {
                    mAppIcon[i] = null;
                    mAppName[i] = mShortcutPackageName[i];
                }
            }
        }
    }

    public boolean isVZWSku(){
        String device = android.os.Build.DEVICE.toUpperCase(Locale.US);
        String sku = android.os.SystemProperties.get("ro.build.asus.sku").toUpperCase(Locale.US);
        return (device.equals("P008") && sku.startsWith("VZW"));
    }
}
