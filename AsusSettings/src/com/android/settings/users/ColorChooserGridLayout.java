/*
 * Copyright (c) 2014 ASUSTek Computer Inc. All rights reserved.
 */

package com.android.settings.users;

import java.util.ArrayList;

import com.android.settings.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.view.View;

interface ColorChooserActivityCallback {
    public void onColorSelected(int color, String title);
    public int getSelectedColor();
}

public class ColorChooserGridLayout extends GridLayout implements View.OnClickListener {

    static class ColorInfo {
        int color;
        String title;
        public ColorInfo(int c, String s) {
            color = c;
            title = s;
        }
    }

    private ColorChooserActivityCallback mCallback;

    private int mLayoutWidth, mLayoutHeight;

    private int mMaxRow, mMaxColumn;

    private Context mContext;

    private boolean mShowText;

    private View mSelectedView = null;
    private int  mCurrentColor;
    public static boolean sUserDefined;

    public static final int COLOR_NUM = 15;

    private ArrayList<ColorInfo> mColorInfos = new ArrayList<ColorInfo>();

    public ColorChooserGridLayout(Context context) {
        this(context, null);
    }

    public ColorChooserGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorChooserGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        Resources r = mContext.getResources();
        mMaxRow = r.getInteger(R.integer.color_chooser_grid_row);
        mMaxColumn = r.getInteger(R.integer.color_chooser_grid_column);
        setColumnCount(mMaxColumn);
        setRowCount(mMaxRow);
        mShowText = false;
        String spKey = DropDownIconPreference2.getSnapViewColorSharedPreferenceKey();
        SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
        int userDefinedPos = (mMaxRow * mMaxColumn < COLOR_NUM) ? mMaxRow * mMaxColumn : mMaxRow * mMaxColumn -1;
        int userDefinedColor = sp.getInt("USER_DEFINED_COLOR", 0);
        if (userDefinedColor != 0) {
            ColorTint.updateColor(userDefinedColor, userDefinedPos);
        }
        for (int i = 0; i < COLOR_NUM; i++) {
            int color = ColorTint.getColorList().get(i);
            if (i == mMaxRow * mMaxColumn - 1) {
                mColorInfos.add(new ColorInfo(color, "customize"));
            } else {
                mColorInfos.add(new ColorInfo(color, null));
            }
        }
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mLayoutWidth = getWidth();
                mLayoutHeight = getHeight();
                initGrids();
            }
        });
    }

    private void initGrids() {
        int mGridWidth = mLayoutWidth / mMaxColumn;
        int mGridHeight = mLayoutHeight / mMaxRow;
        mGridWidth = mGridHeight = Math.max(mGridWidth, mGridHeight);
        sUserDefined = false;
        mCurrentColor = getCurrentColor();
        for (int i = 0; i < mMaxRow * mMaxColumn && i < mColorInfos.size(); i++) {
            ColorChooserGrid cmg = new ColorChooserGrid(mContext, mShowText);
            GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
            gl.width = mGridWidth;
            gl.height = mGridHeight;
            cmg.setLayoutParams(gl);
            cmg.setTag(mColorInfos.get(i));
            cmg.setOnClickListener(this);
            if (isColorFocused(mColorInfos.get(i).color)) {
                if (i == mMaxRow * mMaxColumn - 1) {
                    sUserDefined = true;
                } else {
                    callbackToDialog(mColorInfos.get(i).color, null);
                }
                cmg.setButtonSelected(true);
                mSelectedView = cmg;
            }
            cmg.setBg(getStateListDrawable(mColorInfos.get(i).color));
            if (mColorInfos.get(i).title != null)
                cmg.setTitle(mColorInfos.get(i).title);
            addView(cmg);
        }
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                resetMargin();
            }
        });
    }

    private void resetMargin() {
        LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams)this
                .getLayoutParams();
        lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        this.setLayoutParams(lp);
    }

    public void setCallback(ColorChooserActivityCallback callback) {
        mCallback = callback;
    }

    @Override
    public void onClick(View v) {
        if (mSelectedView != null) {
            if (mSelectedView instanceof ColorChooserGrid) {
                ((ColorChooserGrid) mSelectedView).setButtonSelected(false);
            } else {
                mSelectedView.setSelected(false);
            }
        }
        mSelectedView = v;
        mSelectedView.setSelected(true);
        callbackToDialog(((ColorInfo)mSelectedView.getTag()).color, ((ColorInfo)mSelectedView.getTag()).title);
        sUserDefined = (((ColorInfo)mSelectedView.getTag()).title != null);
    }

    private void callbackToDialog(int color, String title) {
        if (mCallback != null) {
            mCallback.onColorSelected(color, title);
        }
    }

    public void setFocusToUserDefinedButton() {
        sUserDefined = true;
        ColorChooserGrid cmg = (ColorChooserGrid) getChildAt(mMaxRow * mMaxColumn -1);
        if (mSelectedView != null) {
            if (mSelectedView instanceof ColorChooserGrid) {
                ((ColorChooserGrid) mSelectedView).setButtonSelected(false);
            } else {
                mSelectedView.setSelected(false);
            }
        }
        cmg.setButtonSelected(true);
        mSelectedView = cmg;
    }

    public boolean isColorFocused(int color) {
        return (mCurrentColor | 0xFF000000) == (color | 0xFF000000);
    }

    public int getCurrentColor() {
        if (mCallback != null) {
            return mCallback.getSelectedColor();
        }
        return 0;
    }

    public Drawable getDrawable(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color | 0xFF000000);
        gd.setCornerRadius(3);
        return gd;
    }

    public Drawable getPressedDrawable(int color) {
        GradientDrawable gd = new GradientDrawable();
        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                3, mContext.getResources().getDisplayMetrics());
        gd.setColor(color | 0xFF000000);
        gd.setCornerRadius(3);
        gd.setStroke((int)strokeWidth, Color.parseColor("#66000000"));
        return gd;
    }

    public StateListDrawable getStateListDrawable(int color) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[] {android.R.attr.state_pressed},
                getPressedDrawable(color));
        stateListDrawable.addState(new int[] {android.R.attr.state_selected},
                getPressedDrawable(color));
        stateListDrawable.addState(new int[] {}, getDrawable(color));
        return stateListDrawable;
    }
}
