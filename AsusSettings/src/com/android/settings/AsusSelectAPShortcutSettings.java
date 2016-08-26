package com.android.settings;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AsusSelectAPShortcutSettings extends ListActivity {
    private static final String TAG = "AsusSelectAPShortcutSettings";

    private PackageManager mPm = null;
    private List<ResolveInfo> mApplist = null;
    private ApplicationAdapter mListadaptor = null;
    private LoadApplications mLoadApp = null;
    private String[] mShortcutPackageName;
    private String[] mShortcutClassName;
    private Long mId;
    private boolean mStartByKeyguard = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] packageAndClassName = Settings.System.getString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME).split(", ");

        mShortcutPackageName = new String[3];
        mShortcutClassName = new String[3];
        setClassAndPackageName(packageAndClassName);
        /*for(int i = 0; i< 3; ++i) {
            if(packageAndClassName[i] != ""){
                String[] tmp = packageAndClassName[i].split("/");
                mShortcutPackageName[i] = tmp[0];
                mShortcutClassName[i] = tmp[1];
            }else{
                mShortcutPackageName[i] = "";
                mShortcutClassName[i] = "";
            }
        }*/

        setContentView(R.xml.asus_lockscreen_app_list);
        mPm = getPackageManager();
        Bundle i = getIntent().getExtras();
        mId = i.getLong("rowId");
        mStartByKeyguard = i.getBoolean("isKeyguard");
        // ++ colorful bar
        Utils.inflateOriginalLayout(this, getLayoutInflater(),
                R.xml.asus_lockscreen_app_list);
        Utils.updateColorfulTextView(this, R.color.actionbar_background,
                R.id.locksreen_app_list_textViewColorful);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoadApp = new LoadApplications();
        mLoadApp.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ResolveInfo app = mApplist.get(position);
        try {
            String packageName = app.activityInfo.packageName;
            mShortcutPackageName[mId.intValue()] = packageName;
            String className = app.activityInfo.name;
            mShortcutClassName[mId.intValue()] = className;
            String shortcutNames = mShortcutPackageName[0] + "/" + mShortcutClassName[0];
            for (int i = 1; i < 3; ++i) {
                shortcutNames += ", " + mShortcutPackageName[i] + "/" + mShortcutClassName[i];
            }
            Settings.System.putString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME, shortcutNames);
            finish();

            if (mStartByKeyguard) {
                showLockScreen();
            }

        } catch (ActivityNotFoundException e) {
            Toast.makeText(AsusSelectAPShortcutSettings.this, e.getMessage(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(AsusSelectAPShortcutSettings.this, e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private List<ResolveInfo> checkForLaunchIntent(List<ResolveInfo> list) {
        ArrayList<ResolveInfo> Applist = new ArrayList<ResolveInfo>();
        for (ResolveInfo info : list) {
            try {
                boolean IsAdd = true;
                if (null != mPm.getLaunchIntentForPackage(info.activityInfo.packageName)) {
                    //++++ for user can change quickaccess icons order,this is a temporal solution
                    /*for (int i = 0; i < 3; ++i) {
                        if (mShortcutPackageName[i].equals(info.activityInfo.packageName)) {
                            if(mShortcutClassName[i].equals(info.activityInfo.name)) {
                                IsAdd = false;
                                break;
                            }
                        }
                    }*/
                    if (IsAdd) {
                        Applist.add(info);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Applist;
    }

    private void showLockScreen() {
        IBinder b = ServiceManager.getService(Context.WINDOW_SERVICE);
        IWindowManager iWm = IWindowManager.Stub.asInterface(b);
        try {
            iWm.lockNow(null);
        } catch (RemoteException e) {
        }
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            Intent launcher = new Intent(Intent.ACTION_MAIN);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            mApplist = checkForLaunchIntent(mPm.queryIntentActivities(launcher, 0));
            mListadaptor = new ApplicationAdapter(AsusSelectAPShortcutSettings.this,
                    R.xml.asus_lockscreen_app_list_row, mApplist);
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(mListadaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(AsusSelectAPShortcutSettings.this, null,
                    getString(R.string.settings_license_activity_loading));
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private void setClassAndPackageName(String[] packageAndClassName) {
        if (packageAndClassName == null) {
            for (int i = 0; i < 3; ++i) {
                mShortcutPackageName[i] = "";
                mShortcutClassName[i] = "";
            }
        } else {
            int len = packageAndClassName.length;
            for (int i = 0; i < 3; ++i) {
                if (i > len - 1) {
                    mShortcutPackageName[i] = "";
                    mShortcutClassName[i] = "";
                } else {
                    if (packageAndClassName[i] != "" && !packageAndClassName[i].equalsIgnoreCase("/")) {
                        String[] tmp = packageAndClassName[i].split("/");
                        mShortcutPackageName[i] = tmp[0];
                        mShortcutClassName[i] = tmp[1];
                    } else {
                        mShortcutPackageName[i] = "";
                        mShortcutClassName[i] = "";
                    }
                }
            }
        }
    }
}
