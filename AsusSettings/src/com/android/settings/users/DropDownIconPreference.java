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

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import com.android.settings.R;

public class DropDownIconPreference extends ListPreference {

    private class CustomListPreferenceAdapter extends ArrayAdapter<IconItem> {
        private Context context;
        private int resource;
        private List<IconItem> iconItemList;

        public CustomListPreferenceAdapter(Context context, int resource,
            List<IconItem> objects) {
                super(context, resource, objects);
                this.context = context;
                this.resource = resource;
                this.iconItemList = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(resource, parent, false);
                holder = new ViewHolder();
                holder.iconImage = (ImageView) convertView.findViewById(R.id.iconImage);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.iconRadio);
                holder.position = position;
                holder.iconText = (TextView) convertView.findViewById(R.id.iconText);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.iconImage.setImageResource(iconItemList.get(position).iconRes);
            holder.iconText.setText(iconItemList.get(position).iconText);
            holder.radioButton.setChecked(iconItemList.get(position).isChecked);

            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ViewHolder holder = (ViewHolder) v.getTag();
                    for (int i = 0; i < iconItemList.size(); i++) {
                        if (i == holder.position)
                            iconItemList.get(i).isChecked = true;
                        else
                            iconItemList.get(i).isChecked = false;
                    }
                    getDialog().dismiss();
                }
            });

            return convertView;
        }
    }

    private static class IconItem {
        private int iconText;
        private int iconRes;
        private boolean isChecked;

        public IconItem(int iconText, int iconRes, boolean isChecked) {
            this.iconText = iconText;
            this.iconRes = iconRes;
            this.isChecked = isChecked;
        }
    }

    private static class ViewHolder {
        protected int position;
        protected ImageView iconImage;
        protected TextView iconText;
        protected RadioButton radioButton;
    }
    //----------------------------------------------------------------

    private final Context mContext;
    private Callback mCallback;

    private ImageView viewIcon;
    private TextView viewTitle, viewSummary;
    private int dummyItemSize = 3;
    private int[] dummyIcon = {R.drawable.stat_sys_dummynotify, R.drawable.asus_ic_subtle, R.drawable.stat_sys_dummynotify3};
    private int[] dummyText = {R.string.user_snapview_settings_item2, R.string.user_snapview_settings_item1, R.string.user_snapview_settings_item3};
    private List<IconItem> iconItemList;
    private int selectedIndex;

    public DropDownIconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setSelectedItem(int position) {
        int pos = position;
        if (position == 0) pos = 1;
        else if (position == 1) pos = 0;
        final Object value = pos;
        if (mCallback != null && !mCallback.onItemSelected(position, value)) {
            return;
        }
        updatePreferenceView(position);
        final boolean disableDependents = value == null;
        notifyDependencyChange(disableDependents);
    }

    public void setSelectedValue(Object value) {
        int i = (int)value;
        if (i == 0) i = 1;
        else if(i == 1) i = 0;
        if (i >= 0 && i < dummyItemSize) {
            selectedIndex = i;
        } else {
            selectedIndex = 0;
        }
    }

    private void updatePreferenceView(int position) {
        if(viewIcon != null) viewIcon.setImageResource(dummyIcon[position]);
        if(viewSummary != null) viewSummary.setText(dummyText[position]);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        viewIcon = (ImageView) view.findViewById(R.id.prefIcon);
        viewTitle = (TextView) view.findViewById(R.id.prefTitle);
        viewSummary = (TextView) view.findViewById(R.id.prefSummary);
        if(viewTitle != null) viewTitle.setText(R.string.user_snapview_settings_dummy_notify_icon);
        setSelectedItem(selectedIndex);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        builder.setTitle(R.string.user_snapview_settings_dummy_notify_icon);
        builder.setNegativeButton(null, null);
        builder.setPositiveButton(null, null);
        iconItemList = new ArrayList<IconItem>();

        for (int i = 0; i < dummyItemSize; i++) {
            boolean isSelected = (selectedIndex == i) ? true : false;
            IconItem item = new IconItem(dummyText[i], dummyIcon[i], isSelected);
            iconItemList.add(item);
        }

        CustomListPreferenceAdapter customListPreferenceAdapter = new CustomListPreferenceAdapter(
            mContext, R.layout.user_dummy_icon_item_picker, iconItemList);
        builder.setAdapter(customListPreferenceAdapter, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (iconItemList != null) {
            for (int i = 0; i < dummyItemSize; i++) {
                IconItem item = iconItemList.get(i);
                if (item.isChecked) {
                    selectedIndex = i;
                    setSelectedItem(i);
                    break;
                }
            }
        }
    }

    public interface Callback {
        boolean onItemSelected(int pos, Object value);
    }
}
