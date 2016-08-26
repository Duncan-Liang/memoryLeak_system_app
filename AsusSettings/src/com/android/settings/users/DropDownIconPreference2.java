/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.users;

import android.app.Activity;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import com.android.settings.R;

public class DropDownIconPreference2 extends Preference {
    private final Context mContext;
    private final ArrayAdapter<String> mAdapter;
    private final Spinner mSpinner;
    private final ArrayList<Object> mValues = new ArrayList<Object>();

    private Callback mCallback;
    private TextView viewTitle, viewSummary;
    private ImageView viewButtonLine;
    private ImageButton viewButton;
    private int selectedIndex = 0;
    private int selectedColor = 0;

    private static final String SNAPVIEW_COLOR_PREFERENCES_KEY = "com.android.settings.users.snapview.color.prefs";

    public DropDownIconPreference2(Context context) {
        this(context, null);
    }

    public DropDownIconPreference2(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAdapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_dropdown_item);

        mSpinner = new Spinner(mContext);

        mSpinner.setVisibility(View.INVISIBLE);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                setSelectedItem(position, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // noop
            }
        });
        setPersistent(false);
    }

    public void setDropDownWidth(int dimenResId) {
        mSpinner.setDropDownWidth(mContext.getResources().getDimensionPixelSize(dimenResId));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setSelectedItem(int position, boolean doCallback) {
        final Object value = mValues.get(position);
        if (doCallback && mCallback != null && !mCallback.onItemSelected(position, value)) {
            return;
        }
        mSpinner.setSelection(position);
        if(viewSummary != null) viewSummary.setText(mAdapter.getItem(position));
        final boolean disableDependents = value == null;
        notifyDependencyChange(disableDependents);
    }

    public void setSelectedValue(Object value) {
        final int i = mValues.indexOf(value);
        if (i > -1) {
            selectedIndex = i;
            setSelectedItem(i, false);
        }
    }

    public void setSelectedColor(int color) {
        selectedColor = color;
        if(viewButtonLine != null) {
            ColorFilter filter = new LightingColorFilter(0, selectedColor | 0xFF000000);
            viewButtonLine.getDrawable().setColorFilter(filter);
        }
    }

    public void addItem(int captionResid, Object value) {
        addItem(mContext.getResources().getString(captionResid), value);
    }

    public void addItem(String caption, Object value) {
        mAdapter.add(caption);
        mValues.add(value);
    }

    public void clearItems(){
        mAdapter.clear();
        mValues.clear();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (view.equals(mSpinner.getParent())) return;
        if (mSpinner.getParent() != null) {
            ((ViewGroup)mSpinner.getParent()).removeView(mSpinner);
        }
        final ViewGroup vg = (ViewGroup)view;
        vg.addView(mSpinner, 0);
        final ViewGroup.LayoutParams lp = mSpinner.getLayoutParams();
        lp.width = 0;
        mSpinner.setLayoutParams(lp);

        viewTitle = (TextView) view.findViewById(R.id.prefTitle);
        viewSummary = (TextView) view.findViewById(R.id.prefSummary);
        viewButton = (ImageButton) view.findViewById(R.id.prefBtn);
        viewButtonLine = (ImageView) view.findViewById(R.id.prefBtn_line);

        if(viewTitle != null) viewTitle.setText(R.string.user_snapview_settings_hint_notify);
        setSelectedColor(selectedColor);
        if(viewButton != null) {
            viewButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ColorChooserDialog colorChooserDialog = new ColorChooserDialog(
                                mContext, new ColorChooserDialog.OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int color) {
                                setSelectedColor(color);
                                if (mCallback != null) {
                                    mCallback.onColorSelected(color);
                                }
                        }
                        });
                        colorChooserDialog.show(((Activity) mContext).getFragmentManager(), "ColorChooserDialog");
                    }
                });
        }

        if(view != null) {
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(v != viewButton) {
                        mSpinner.performClick();
                    }
                }
            });
        }
    }

    public interface Callback {
        boolean onItemSelected(int pos, Object value);
        boolean onColorSelected(int color);
    }

    public static String getSnapViewColorSharedPreferenceKey() {
        return SNAPVIEW_COLOR_PREFERENCES_KEY;
    }
}
