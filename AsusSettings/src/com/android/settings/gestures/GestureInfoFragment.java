/*
 * Filename : GestureInfoFragment.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.io.IOException;
import java.util.List;

public class GestureInfoFragment extends SettingsPreferenceFragment implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final String TAG = "GestureInfoFragment";
    private static final boolean DEBUG = Utils.DEBUG;
    private TextView mCategoryApp;
    private ImageView mIcon;
    private TextView mLabel;
    private ImageView mDemo;
    private String mPackageName;
    private String mClassName;
    private String mPath;
    private PackageManager mPackageManager;
    private Switch mEnabledSwitch;
    private String mKey;
    private LinearLayout mApplicationInfo;

    public static int mIndex = 0;

    protected int getMetricsCategory() {
        return MetricsLogger.GESTURES_SETTINGS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DEBUG) {
            Log.i(TAG, "onCreate");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.i(TAG, "onActivityCreated");
        }
        Activity activity = getActivity();
        mPackageManager = activity.getPackageManager();
        LayoutInflater inflater = (LayoutInflater) activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEnabledSwitch = (Switch) inflater.inflate(com.mediatek.internal.R.layout.imageswitch_layout, null);
        mEnabledSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG) {
            Log.i(TAG, "onStart");
        }
        Activity activity = getActivity();
        if (activity instanceof PreferenceActivity) {
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG) {
            Log.i(TAG, "onCreateView");
        }
        final View view = inflater.inflate(R.layout.gesture_info, container, false);
        mIcon = (ImageView) view.findViewById(R.id.application_icon);
        mLabel = (TextView) view.findViewById(R.id.application_label);
        mDemo = (ImageView) view.findViewById(R.id.demo);
        mCategoryApp = (TextView) view.findViewById(R.id.category_app);
        mApplicationInfo = (LinearLayout) view.findViewById(R.id.application_info);
        mApplicationInfo.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.i(TAG, "onResume");
        }
        View root = getView();
        if (root != null) {
            ViewGroup parent = (ViewGroup) root.getParent();
            if (parent != null) {
                parent.setPadding(0, 0, 0, 0);
            }
        }

        Bundle bundle = getArguments();
        mKey = bundle.getString("key");
        String defaultHint = bundle.getString("hint");
        String gestureInfo = Utils.getGestureIntent(getContentResolver(), mKey);
        String[] classInfo;
        if (gestureInfo != null && !gestureInfo.isEmpty()) {
            classInfo = gestureInfo.split("/");
            mPackageName = classInfo[0];
            mClassName = classInfo[1];
            if (mPackageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && mClassName.equals(GesturesSettings.CAMERA_FRONT_CLASSNAME)) {
                mClassName = GesturesSettings.CAMERA_BACK_CLASSNAME;
                mIndex = 1;
            } else if (mPackageName.equals(GesturesSettings.CAMERA_PACKAGENAME) && mClassName.equals(GesturesSettings.CAMERA_BACK_CLASSNAME)) {
                mIndex = 0;
            }
        } else {
            mPackageName = bundle.getString("packageName");
            mClassName = bundle.getString("className");
        }

        if (mPackageName != null && !mPackageName.isEmpty() && mClassName != null && !mClassName.isEmpty()) {
            ComponentName componentName = new ComponentName(mPackageName, mClassName);
            Intent intent = new Intent();
            intent.setComponent(componentName);
            List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (DEBUG) {
                Log.i(TAG, "resolveInfos=" + resolveInfos + ", size=" + resolveInfos.size());
            }
            if (resolveInfos != null && resolveInfos.size() > 0) {
                ResolveInfo info = resolveInfos.get(0);
                String appLabel = (String) info.loadLabel(mPackageManager);
                Drawable appIcon = info.loadIcon(mPackageManager);
                if (DEBUG) {
                    Log.i(TAG, "appLabel=" + appLabel + ", appIcon=" + appIcon);
                }
                if (mIcon != null) {
                    mIcon.setImageDrawable(appIcon);
                }
                if (mLabel != null) {
                    if (mClassName.equals("com.asus.zenlife.zlweather.ZLWeatherActivity")) {
                        mLabel.setText(AppsPanelFrament.getWeatherTitle(getContext(), appLabel));
                    } else {
                        mLabel.setText(appLabel);
                    }
                }
            } else {
                mLabel.setText(defaultHint);
            }
        } else {
            mLabel.setText(defaultHint);
        }

        if (mLabel != null && mKey != null && (mKey.equals(GesturesSettings.GESTURE_DOUBLE_CLICK) || mKey.equals(GesturesSettings.KEY_GESTURE_UP))) {
            mCategoryApp.setText(R.string.gesture_dc_introduction);
        }

        mPath = bundle.getString("gifPath");
        if (mPath != null && (!mPath.equals(""))) {
            Bitmap bit = null;
            try {
                bit = BitmapFactory.decodeStream(getActivity().getAssets().open(mPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mDemo.setImageBitmap(bit);
        }
        if (DEBUG) {
            Log.i(TAG, "mPackageName=" + mPackageName + ", mClassName=" + mClassName +
                    ",defaultHint=" + defaultHint + ",mPath=" + mPath);
        }
        boolean checked = getState(mKey);
        mEnabledSwitch.setChecked(checked);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (DEBUG) {
            Log.i(TAG, "onStop");
        }
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.i(TAG, "onDestroy");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) {
            Log.i(TAG, "onPause");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch) {
            saveState(isChecked);
        }
    }


    public void saveState(boolean enabled) {
        if (mKey != null) {
            Utils.enableGesture(getContext(), mKey, enabled);
        }
    }

    public boolean getState(String key) {
        return Utils.isGestureEnabled(getContentResolver(), key);
    }

    @Override
    public void onClick(View view) {
        if (mKey != null && (mKey.equals(GesturesSettings.GESTURE_DOUBLE_CLICK) || mKey.equals(GesturesSettings.KEY_GESTURE_UP))) {
            return;
        }
        Bundle args = new Bundle();
        args.putString("key", mKey);
        ((SettingsActivity) getActivity()).startPreferencePanel(AppsPanelFrament.class.getName(),
                args, R.string.gesture_apps_title, null, null, 0);
    }

}
