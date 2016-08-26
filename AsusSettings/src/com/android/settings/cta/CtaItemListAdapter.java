package com.android.settings.cta;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.cta.CtaItemListActivity.CtaItem;
import com.android.settings.R;

import java.util.List;


public class CtaItemListAdapter extends BaseAdapter{

    Context mContext = null;
    List<CtaItem> mCtaItemList;

    public CtaItemListAdapter(final Context context, final List<CtaItem> ctaItemList) {
        super();
        mContext = context;
        mCtaItemList = ctaItemList;
    }

    @Override
    public int getCount() {
        return mCtaItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mCtaItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.cta_list_item, parent, false);

        ImageView imageLogo = (ImageView) view.findViewById(R.id.logo);
        TextView textLabel = (TextView) view.findViewById(R.id.label);
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.checked);
        TextView pkgName = (TextView) view.findViewById(R.id.pkgName);
        TextView action = (TextView) view.findViewById(R.id.action);

        CtaItem item = mCtaItemList.get(position);
        imageLogo.setImageDrawable(item.mLogo);
        textLabel.setText(item.mLabel);
        pkgName.setText(item.mCaller);
        action.setText(String.valueOf(item.mAction));

        if (item.mAccept == 1) {
            checkBox.setChecked(true);
        }
        else {
            checkBox.setChecked(false);
        }

        view.setOnClickListener(mOnClickListener);
        view.setOnLongClickListener(mOnLongClickListener);

        return view;
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.checked);
            TextView textPkgName = (TextView) v.findViewById(R.id.pkgName);
            TextView textAction = (TextView) v.findViewById(R.id.action);
            String pkgName = (String) textPkgName.getText();
            int action = Integer.valueOf((String)textAction.getText());

            CtaProviderUtils providerUtils = new CtaProviderUtils(mContext);
            if (checkBox.isChecked()) {
                for (CtaItem item : mCtaItemList) {
                    if (item.mAction == action && item.mCaller.equals(pkgName)) {
                         item.mAccept = 0;
                    }
                }
                checkBox.setChecked(false);
                providerUtils.updatePermission(action, pkgName, 0);
            }
            else {
                for (CtaItem item : mCtaItemList) {
                    if (item.mAction == action && item.mCaller.equals(pkgName)) {
                         item.mAccept = 1;
                    }
                }
                checkBox.setChecked(true);
                providerUtils.updatePermission(action, pkgName, 1);
            }
        }
    };

    OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            TextView textLabel = (TextView) v.findViewById(R.id.label);
            TextView textPkgName = (TextView) v.findViewById(R.id.pkgName);
            TextView textAction = (TextView) v.findViewById(R.id.action);

            String label = (String)textLabel.getText();
            String pkgName = (String) textPkgName.getText();
            int action = Integer.valueOf((String)textAction.getText());

            final AlertDialog alertDialog = generateAlertDialog(action, pkgName, label);
            alertDialog.show();
            return true;
        }

    };

    private AlertDialog generateAlertDialog(final int action, final String packageName, final String label) {
        Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(R.string.dialog_title);
        builder.setMessage(R.string.dialog_message);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CtaProviderUtils providerUtils = new CtaProviderUtils(mContext);
                providerUtils.removePermission(action, packageName);
                CtaItem deleteItem = null;
                for (CtaItem item : mCtaItemList) {
                    if (item.mAction == action && item.mCaller.equals(packageName)) {
                        deleteItem = item;
                    }
                }
                if (deleteItem != null) {
                    mCtaItemList.remove(deleteItem);
                }
                CtaItemListAdapter.this.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }
}
