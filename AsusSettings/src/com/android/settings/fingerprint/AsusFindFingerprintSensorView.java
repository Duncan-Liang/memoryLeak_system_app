//add by sunxiaolong@wind-mobi.com for asus's printfinger patch begin
package com.android.settings.fingerprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;
import android.os.SystemProperties;

import com.android.settings.R;

public class AsusFindFingerprintSensorView extends View {

    private Movie mMovie;
    private long mMovieStart;

    public static final String  SENSOR_FRONT = "front";
    public static final String  SENSOR_BACK = "back";

    private String mode = SENSOR_FRONT;

    public AsusFindFingerprintSensorView(Context context) {
        super(context);
        init(context);
    }

    public AsusFindFingerprintSensorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsusFindFingerprintSensorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setFocusable(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mode = SystemProperties.get("ro.hardware.fp_position");
        if(SENSOR_FRONT.equals(mode)){
            setGifResource(context, R.drawable.fingerprint_position_front_gif);
        } else {
            setGifResource(context, R.drawable.fingerprint_position_back_gif);
        } 
    }

    public void setGifResource(Context context, int resId) {
        java.io.InputStream is;
        is = context.getResources().openRawResource(resId);
        mMovie = Movie.decodeStream(is);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        long now = android.os.SystemClock.uptimeMillis();
        if (0 == mMovieStart) { // first time
            mMovieStart = now;
        }

        if (null == mMovie) {
            super.onDraw(canvas);
        } else {
            int dur = mMovie.duration();
            if (dur == 0) {
                dur = 5000;
            }

            int relTime = (int) ((now - mMovieStart) % dur);
            mMovie.setTime(relTime);

            float scaleWidth = (float)getWidth()/(float)mMovie.width() ;
            float scaleHeight = (float)getHeight()/(float)mMovie.height() ;
            float scale =  Math.min(scaleHeight, scaleWidth);

            int movieWidth = (int) (mMovie.width()*scale);
            int movieHeight = (int) (mMovie.height()*scale);

            int sx = getWidth()/2 - movieWidth/2 ;
            int sy = getHeight() - movieHeight;
            canvas.scale(scale, scale);
            mMovie.draw(canvas, sx, sy);
            invalidate();
        }

    }

    public String getPosition(){
        return mode;
    }
}
//add by sunxiaolong@wind-mobi.com for asus's printfinger patch end