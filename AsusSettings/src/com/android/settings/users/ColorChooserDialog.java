/*
 * Copyright (c) 2013 ASUSTek Computer Inc. All rights reserved.
 */

package com.android.settings.users;

import org.xmlpull.v1.XmlPullParser;

import com.android.settings.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ColorChooserDialog extends DialogFragment implements
        ColorChooserActivityCallback, View.OnTouchListener {
    public interface ColorChooserDialogCallback {
        public void onColorSelected(int color);
        public int getSelectedColor();
    }

    private FrameLayout mFrameLayout;
    private Context mContext;
    private ImageView mColorPalette;
    private ImageView mChooseIcon;
    private Bitmap mBmp;
    private int mSelectedColor;
    private int mUserDefinedColor;
    private int mRow;
    private int mCol;
    private int mTop;
    private int mLeft;
    private int mRight;
    private int mBottom;
    private int mFrameHeight;
    private int mFrameWidth;
    private float mXoffset;
    private float mYoffset;
    private float mX;
    private float mY;

    private ColorChooserGridLayout mColorChooserGridLayout;

    private OnColorSelectedListener mColorSelectedListener;

    private ColorChooserDialogCallback mCallback;

    public ColorChooserDialog(Context context) {
        mContext = context;
    }

    public ColorChooserDialog(Context context, OnColorSelectedListener colorSelectedListener) {
        mContext = context;
        mColorSelectedListener = colorSelectedListener;
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        iniGetColorTint();
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(
                getActivity(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar));
        builder.setTitle(mContext.getText(R.string.user_snapview_settings_hint_notify));
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.color_chooser_dialog, null);
        mColorChooserGridLayout = (ColorChooserGridLayout) view.findViewById(R.id.color_chooser_colormaskgridlayout);
        mColorChooserGridLayout.setCallback(this);
        mRow = mContext.getResources().getInteger(R.integer.color_chooser_grid_row);
        mCol = mContext.getResources().getInteger(R.integer.color_chooser_grid_column);
        mFrameLayout = (FrameLayout) view.findViewById(R.id.color_chooser_container);
        mColorPalette = (ImageView) view.findViewById(R.id.color_mask_palette);
        mColorPalette.setScaleType(ImageView.ScaleType.FIT_XY);
        mChooseIcon = (ImageView) view.findViewById(R.id.color_choose_icon);
        ViewTreeObserver frame = mFrameLayout.getViewTreeObserver();
        frame.addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
            @Override
            public void onGlobalLayout() {
                mTop = mContext.getResources().getDimensionPixelSize(R.dimen.color_chooser_bar_padding_top);
                mLeft = mContext.getResources().getDimensionPixelSize(R.dimen.color_chooser_bar_padding);
                mRight = mContext.getResources().getDimensionPixelSize(R.dimen.color_chooser_bar_padding);
                mBottom = mContext.getResources().getDimensionPixelSize(R.dimen.color_chooser_bar_padding_bottom);
                mFrameHeight = mFrameLayout.getHeight();
                mFrameWidth = mFrameLayout.getWidth();
                measureImageSize();
                initImagePosition();
            }
        });
        mFrameLayout.setOnTouchListener(this);
        builder.setView(view);
        builder.setNegativeButton(mContext.getText(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        builder.setPositiveButton(mContext.getText(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(ColorChooserGridLayout.sUserDefined) {
                            mSelectedColor = mUserDefinedColor;
                            saveClickColor(mUserDefinedColor);
                            saveClickPosition(mX, mY);
                        }

                        if (mSelectedColor != 0) // already select color
                            mColorSelectedListener.onColorSelected(mSelectedColor);
                }});
        return builder.create();
    }

    private void iniGetColorTint() {
        ColorTint colortint = null;
        XmlPullParser parser = null;
        
        if (colortint == null) {
            parser = mContext.getResources().getXml(R.xml.colormask_color);
            colortint = new ColorTint(parser);
        }
    }

    @Override
    public void onColorSelected(int color, String title) {
        mSelectedColor = color;
        // select user defined color
        if (title != null) {
            saveClickColor(color);
            saveClickPosition(mX, mY);
        }
    }

    @Override
    public int getSelectedColor() {
        if (mCallback != null) {
            return mCallback.getSelectedColor();
        }
        return 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getY() > mTop && event.getY() < mFrameHeight - mBottom) {
            mY = event.getY();
        }
        if (event.getX() < mFrameWidth - mRight && event.getX() > mLeft) {
            mX = event.getX();
        }
        if (event.getY() <= mTop) {
            mY = mTop + 1;
        } else if (event.getY() >= mFrameHeight - mBottom) {
            mY = mFrameHeight - mBottom - 1;
        }

        if (event.getX() <= mLeft) {
            mX = mLeft + 1;
        } else if (event.getX() >= mFrameWidth - mRight) {
            mX = mFrameWidth - mRight - 1;
        }
        switch(event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            setFocusToUserDefinedButton();
            updateButtonColor(getPixelColor(mX, mY));
            updateChooseIconPosition(mX, mY);
            break;
        case MotionEvent.ACTION_MOVE:
            updateButtonColor(getPixelColor(mX, mY));
            updateChooseIconPosition(mX, mY);
            break;
        case MotionEvent.ACTION_UP:
            updateButtonColor(getPixelColor(mX, mY));
            updateChooseIconPosition(mX, mY);
            break;
        case MotionEvent.ACTION_CANCEL:
            break;
        }
        return true;
    }

    private void measureImageSize() {
        mColorPalette.setDrawingCacheEnabled(true);
        mBmp = Bitmap.createBitmap(mColorPalette.getDrawingCache());
        mColorPalette.setDrawingCacheEnabled(false);
        mXoffset = mChooseIcon.getWidth()/2;
        mYoffset = mChooseIcon.getHeight()/2;
    }

    private void initImagePosition() {
        String spKey = DropDownIconPreference2.getSnapViewColorSharedPreferenceKey();
        SharedPreferences sp = getActivity().getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
        mX = sp.getFloat("COLOR_BAR_X", 0);
        mY = sp.getFloat("COLOR_BAR_Y", 0);
        mUserDefinedColor = sp.getInt("USER_DEFINED_COLOR", 0);

        if (mUserDefinedColor == 0) {
            // first time enter dialog
            mX = mLeft + mXoffset;
            mY = mTop + mYoffset;
            updateButtonColor(getPixelColor(mX , mY));
            mChooseIcon.setTranslationX(mX - mXoffset);
            mChooseIcon.setTranslationY(mY - mYoffset);
        } else {
            updateButtonColor(mUserDefinedColor);
            mChooseIcon.setTranslationX(mX - mXoffset);
            mChooseIcon.setTranslationY(mY - mYoffset);
        }
    }

    private void updateButtonColor(int color) {
        ColorChooserGrid gridElement = (ColorChooserGrid) mColorChooserGridLayout.getChildAt(mRow * mCol - 1);
        gridElement.setBg(mColorChooserGridLayout.getStateListDrawable(color));
        gridElement.setTag(new ColorChooserGridLayout.ColorInfo(color, "customize"));
    }

    private void updateChooseIconPosition(float x, float y) {
        mChooseIcon.setTranslationX(x-mXoffset);
        mChooseIcon.setTranslationY(y-mYoffset);
    }

    private int getPixelColor(float x, float y) {
        int color = 0;
        try {
            color = mBmp.getPixel((int) x,(int) y);
        } catch (IllegalArgumentException e){
            Log.v("ColorChooserDialog", "Exception at  y must be < bitmap.height()");
        }

        if (color != 0) {
            mUserDefinedColor = color;
        }
        return mUserDefinedColor;
    }

    private void saveClickColor(int color) {
        int userDefinedPos = (mRow * mCol < ColorChooserGridLayout.COLOR_NUM) ? mRow * mCol : mRow * mCol -1;
        ColorTint.updateColor(color, userDefinedPos);
        String spKey = DropDownIconPreference2.getSnapViewColorSharedPreferenceKey();
        SharedPreferences sp = getActivity().getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
        sp.edit().putInt("USER_DEFINED_COLOR", color).commit();
    }

    private void saveClickPosition(float x, float y) {
        String spKey = DropDownIconPreference2.getSnapViewColorSharedPreferenceKey();
        SharedPreferences sp = getActivity().getSharedPreferences(spKey, Context.MODE_MULTI_PROCESS);
        sp.edit().putFloat("COLOR_BAR_X", x).commit();
        sp.edit().putFloat("COLOR_BAR_Y", y).commit();
    }

    private void setFocusToUserDefinedButton() {
        mColorChooserGridLayout.setFocusToUserDefinedButton();
    }

    public interface OnColorSelectedListener {
        public void onColorSelected(int color);
    }
}
