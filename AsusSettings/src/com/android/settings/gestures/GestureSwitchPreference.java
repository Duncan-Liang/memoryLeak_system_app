/*
 * Filename : GestureSwitchPreference.java
 * Detail description
 *
 *
 * Author:xuyongfeng@wind-mobi.com,
 * created at 2014/04/28
 */

package com.android.settings.gestures;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;

public class GestureSwitchPreference extends Preference {
    public static final String TAG = "GestureSwitchPreference";
    private static final boolean DEBUG = Utils.DEBUG;
    private String mPackageName;
    private String mClassName;
    private String mGifPath;
    private String mDefaultHint;

    private boolean mChecked = true;
    private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = new Listener();

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            saveState(isChecked);
            setChecked(isChecked);
            return;
        }
    }

    public GestureSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.xml.gesture_switch_preference);
        init(context, attrs);
    }

    public GestureSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.xml.gesture_switch_preference);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.GesturePreference);
        mPackageName = a.getString(R.styleable.GesturePreference_packageName);
        mClassName = a.getString(R.styleable.GesturePreference_className);
        mGifPath = a.getString(R.styleable.GesturePreference_path);
        mDefaultHint = a.getString(R.styleable.GesturePreference_default_hint);
        a.recycle();
        Bundle bundle = getExtras();
        if (mPackageName != null) {
            bundle.putString("packageName", mPackageName);
        }
        if (mClassName != null) {
            bundle.putString("className", mClassName);
        }
        if (mGifPath != null) {
            bundle.putString("gifPath", mGifPath);
        }
        if (mDefaultHint != null) {
            bundle.putString("hint", mDefaultHint);
        }
        if (DEBUG) {
            Log.i(TAG, "packageName=" + mPackageName + ", className=" + mClassName +
                    ", gifPath=" + mGifPath + ", key=" + getKey() + ", hint=" + mDefaultHint);
        }
        bundle.putString("key", getKey());
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch switchbutton = (Switch) view.findViewById(R.id.switch_);
        if (mSwitchChangeListener != null && switchbutton != null) {
            switchbutton.setClickable(true);
            switchbutton.setChecked(mChecked);
            switchbutton.setOnCheckedChangeListener(mSwitchChangeListener);
        }
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        notifyChanged();
    }

    public boolean isChecked() {
        return mChecked;

    }

    public void saveState(boolean enabled) {
        //save data
        Utils.enableGesture(getContext(), getKey(), enabled);
    }

}