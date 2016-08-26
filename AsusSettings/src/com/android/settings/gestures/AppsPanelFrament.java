/*
 * Filename : AppsPanelFrament.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Collections;
import java.util.List;

public class AppsPanelFrament extends SettingsPreferenceFragment implements OnItemClickListener {

    private GridView mGridView;
    private Context mContext;
    private PackageManager mPackageManager;
    private List<ResolveInfo> mAllApps;
    private GridViewAdapter mAdapter;
    private String mKey;
    private String packageName;
    private String className;

    protected int getMetricsCategory() {
        return MetricsLogger.GESTURES_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle b) {
        super.onActivityCreated(b);
        mContext = getActivity();
        mPackageManager = getActivity().getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        mAllApps = mPackageManager.queryIntentActivities(intent, 0);

        Intent weatherIntent = new Intent();
        ComponentName componentName = ComponentName.unflattenFromString("com.asus.launcher3/com.asus.zenlife.zlweather.ZLWeatherActivity");
        weatherIntent.setComponent(componentName);
        List<ResolveInfo> weatherApps = mPackageManager.queryIntentActivities(weatherIntent, 0);
        mAllApps.addAll(weatherApps);

        Collections.sort(mAllApps, new ResolveInfo.DisplayNameComparator(mPackageManager));
        mAdapter = new GridViewAdapter(mContext, mAllApps);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gesture_apps_fragment, container, false);
        mGridView = (GridView) view.findViewById(R.id.gridView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle bundle = getArguments();
        mKey = bundle.getString("key");
    }

    public static String getWeatherTitle(Context context ,String defValue) {
        try {
            Resources res = context.getPackageManager().getResourcesForApplication("com.asus.launcher3");
            int weatherIdentifyId = res.getIdentifier("zl_theweather", "string", "com.asus.launcher3");

            String weatherTitle = null;
            if (weatherIdentifyId > 0) {
                weatherTitle = res.getString(weatherIdentifyId);
            }
            return weatherTitle;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return defValue;
    }

    class GridViewAdapter extends BaseAdapter {
        private List<ResolveInfo> mAllApps;
        private Context mContext;

        public GridViewAdapter(Context context, List<ResolveInfo> allApps) {
            mContext = context;
            mAllApps = allApps;
        }

        @Override
        public int getCount() {
            return mAllApps.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.gesture_application_item, null);
                holder = new ViewHolder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.label = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ResolveInfo info = mAllApps.get(position);
            Drawable appIcon = info.loadIcon(mPackageManager);
            if (appIcon != null) {
                holder.icon.setImageDrawable(appIcon);
            }

            CharSequence label = info.loadLabel(mPackageManager);
            if (label != null) {
                if (info.activityInfo.name.equals("com.asus.zenlife.zlweather.ZLWeatherActivity")) {
                    holder.label.setText(getWeatherTitle(mContext ,label.toString()));
                } else {
                    holder.label.setText(label);
                }
            }

            return convertView;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ResolveInfo info = mAllApps.get(position);
        packageName = info.activityInfo.packageName;
        className = info.activityInfo.name;

        if (packageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && className.equals(GesturesSettings.CAMERA_BACK_CLASSNAME)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.camera_switch);
            builder.setSingleChoiceItems(R.array.camera_switch_entries, GestureInfoFragment.mIndex,
                    new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            switch (which) {
                                case 0:
                                    // backcamera
                                    break;
                                case 1:
                                    // frontcamera
                                    className = GesturesSettings.CAMERA_FRONT_CLASSNAME;
                                    break;
                            }
                            dialog.dismiss();
                            if (mKey != null && packageName != null & className != null) {
                                Utils.setGestureIntent(getContentResolver(), mKey, packageName + "/" + className);
                            }
                            closePanel();
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog mDialog = builder.create();
            mDialog.show();
        } else {
            if (mKey != null && packageName != null & className != null) {
                Utils.setGestureIntent(getContentResolver(), mKey, packageName + "/" + className);
            }
            ((SettingsActivity) getActivity()).finishPreferencePanel(this, 0, null);
        }
    }

    private void closePanel(){
        ((SettingsActivity)getActivity()).finishPreferencePanel(this, 0, null);
    }

    class ViewHolder {
        public ImageView icon;
        public TextView label;
    }
}