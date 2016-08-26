package com.android.settings.cta;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;

public class CtaTypeListAdapter extends BaseAdapter{

    Context mContext = null;
    final String[] mCtaTypeList;

    public CtaTypeListAdapter(final Context context, final String[] ctaTypeList) {
        super();
        mContext = context;
        mCtaTypeList = ctaTypeList;
    }

    @Override
    public int getCount() {
        return mCtaTypeList.length;
    }

    @Override
    public Object getItem(int position) {
        return mCtaTypeList[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.cta_type_item, parent, false);
        TextView textLabel = (TextView) view.findViewById(R.id.type_label);
        TextView textType = (TextView) view.findViewById(R.id.type);

        textLabel.setText(mCtaTypeList[position]);
        textType.setText(String.valueOf(position));
        view.setOnClickListener(mOnClickListener);

        return view;
    }

    OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView textType = (TextView) v.findViewById(R.id.type);
            int type = Integer.valueOf((String)textType.getText());

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(mContext, "com.android.settings.cta.CtaItemListActivity"));
            intent.putExtra("CTA_TYPE", type);
            mContext.startActivity(intent);
        }
    };

}
