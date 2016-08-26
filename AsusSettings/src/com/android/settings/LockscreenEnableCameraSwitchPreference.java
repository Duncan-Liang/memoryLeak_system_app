package com.android.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

public class LockscreenEnableCameraSwitchPreference extends Preference {
    static final String TAG = "LockscreenEnableCameraSwitchPreference";
    private Switch mSwitch;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;
    public LockscreenEnableCameraSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_lockscreen_enable_camera_widget_switch);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        mSwitch = (Switch) view.findViewById(R.id.switch_enable_lockscreen_camera_widget);
        mSwitch.setOnCheckedChangeListener(mListener);
        setSwitchChecked(mIsChecked);
        return view;
    }

    public void setOnSwitchCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public void setSwitchChecked(boolean checked) {
        Log.d(TAG, "setSwitchChecked()");
        mIsChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public boolean isChecked(){
        return mIsChecked;
    }

}
