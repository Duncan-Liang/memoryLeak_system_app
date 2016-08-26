
package com.android.settings.zenmotion;

import com.android.settings.switchenabler.MotionGestureSwitchEnabler;
import com.android.settings.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class ZenMotionGestureSwitchPreference extends Preference {

    private static final String TAG = "ZenMotionGestureSwitchPreference";
    private final Context mContext;
    private MotionGestureSwitchEnabler mEnabler;
    private Switch mSwitch;

    public ZenMotionGestureSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_motion_gesture_switch);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        mEnabler = new MotionGestureSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_motion);
        mEnabler.setSwitch(mSwitch);

        return view;
    }

    public void handleStateChanged() {
        mEnabler.handleMainSwitchChanged();
    }

    public void resume() {
        if (null != mEnabler) mEnabler.resume();
    }

    public void pause() {
        if (null != mEnabler) mEnabler.pause();
    }

}
