package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ApplicationAdapter extends ArrayAdapter<ResolveInfo> {
    private List<ResolveInfo> appsList = null;
    private Context context;
    private PackageManager packageManager;

    public ApplicationAdapter(Context context, int textViewResourceId,
                              List<ResolveInfo> appsList) {
        super(context, textViewResourceId, appsList);
        this.context = context;
        this.appsList = appsList;
        packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return ((null != appsList) ? appsList.size() : 0);
    }

    @Override
    public ResolveInfo getItem(int position) {
        return ((null != appsList) ? appsList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.xml.asus_lockscreen_app_list_row, null);
        }
        ResolveInfo data = appsList.get(position);
        if (null != data) {
            TextView appName = (TextView) view.findViewById(R.id.app_name);
            ImageView iconview = (ImageView) view.findViewById(R.id.app_icon);
            appName.setText(data.loadLabel(packageManager));

            ComponentName name = new ComponentName(data.activityInfo.packageName, data.activityInfo.name);
            try {
                ActivityInfo activityInfo = packageManager.getActivityInfo(
                        name, 0);
                iconview.setImageDrawable(activityInfo.loadIcon(packageManager));
            } catch (final NameNotFoundException e) {
            }
        }
        return view;
    }
};