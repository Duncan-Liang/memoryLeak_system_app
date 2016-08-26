/*
 * Copyright (c) 2014 ASUSTek Computer Inc. All rights reserved.
 */

package com.android.settings.users;

import com.android.settings.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.TextUtils.TruncateAt;

public class ColorChooserGrid extends LinearLayout {

    private Context mContext;

    private TextView mTitleView;
    private FrameLayout mFramelayout;
    private Button mChooser;

    public ColorChooserGrid(Context context, boolean showText) {
        super(context);
        mContext = context;
        this.setOrientation(LinearLayout.VERTICAL);
        this.setGravity(Gravity.CENTER_HORIZONTAL);
        int margin = (int)mContext.getResources().getDimension(R.dimen.color_chooser_margin);
        mTitleView = new TextView(mContext);
        mChooser = new Button(mContext);
        mFramelayout = new FrameLayout(mContext);
        // FrameLayout
        LinearLayout.LayoutParams llf = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llf.weight = 6;

        // Button
        FrameLayout.LayoutParams llb = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        llb.gravity = Gravity.CENTER;
        llb.setMargins(margin, margin, margin, margin);

        mFramelayout.addView(mChooser,llb);
        addView(mFramelayout, llf);
        if (showText) {
            // textview
            mTitleView.setMaxLines(1);
            mTitleView.setEllipsize(TruncateAt.END);
            mTitleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams llt = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            llt.weight = 1;
            llt.setMargins(0, margin, margin, margin);
            addView(mTitleView, llt);
        }

    }

    public void setBg(Drawable d) {
        mChooser.setBackground(d);
    }

    public void setTag(Object o) {
        mChooser.setTag(o);
    }

    public void setOnClickListener(OnClickListener l) {
        mChooser.setOnClickListener(l);
    }

    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    public void onFinishFlate() {
        super.onFinishInflate();
    }

    public void setButtonSelected(boolean isSelect) {
        mChooser.setSelected(isSelect);
    }
}
