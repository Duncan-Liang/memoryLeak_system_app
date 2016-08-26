package com.android.settings;


import com.android.settings.DragGridView;
import com.android.settings.DragGridView.OnActionUpListener;
import com.android.settings.DragGridView.OnChanageListener;
import com.android.settings.util.ResCustomizeConfig;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
//add by sunxiaolong@wind-mobi.com for #118573 20160628 begin
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.LayerDrawable;
//add by sunxiaolong@wind-mobi.com for #118573 20160628 end

public class AsusQuickAccessSettings extends Activity {
    private static final String TAG = "AsusQuickAccessSettings";

    private LinearLayout mShortcutLayout;
    private ImageView[] mShortCut;
    private ImageView[] mEditViews;
    private ImageView[] mShortCutPreview;
    private PackageManager mPm;
    private ApplicationInfo mApplicationInfo;
    private String[] mAppName;
    private Drawable[] mAppIcon;
    private String[] mShortcutPackageName;
    private String[] mShortcutClassName;
    public static boolean IS_WIFI_ONLY = "wifi-only".equals(SystemProperties.get("ro.carrier"));
    public static final boolean IS_KDDI = "kddi".equalsIgnoreCase(SystemProperties.get("ro.carrier"));
    private Switch mSwitch;
    private boolean mIsShortcutEnabled;
    private WallpaperManager mWallpaperManager = null;
    private static final double SHORTCUT_RATIO = 0.54;
    private String[] mEssentialAppPackageNames;
    private String[] mEssentialAppClassNames;
    private TypedArray mEssentialAppNormalDrawables;
    private TypedArray mEssentialAppNormalDrawablesPre;
    private int[] mShortcutBackgroundResources = {
            R.id.shortcut_first_background, R.id.shortcut_second_background,
            R.id.shortcut_third_background};
    private int[] mShortcutBackgroundResourcesPre = {
            R.id.shortcut_first_background_preview, R.id.shortcut_second_background_preview,
            R.id.shortcut_third_background_preview};
    private ImageView[] mShortCutBackgroundPre;
    private List<HashMap<String, Object>> dataSourceList = new ArrayList<HashMap<String, Object>>();
    DragGridView mDragGridView;
    private SimpleAdapter mSimpleAdapter;
    private static final String ACTION_THEME_CHANGE = "asus.intent.action.THEME_CHANGE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.Theme_QuickAccess);//To change the ActionBar TextColor. by jesson_yi
        super.onCreate(savedInstanceState);
        IS_WIFI_ONLY = !isSupportPhone();
        setContentView(R.layout.asus_quickaccess_tutorial);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        mShortcutLayout = (LinearLayout) findViewById(R.id.shortcut_list);
        mEssentialAppPackageNames = getApplicationContext().getResources().getStringArray(
                R.array.asus_lockscreen_essential_app_package_name);
        mEssentialAppClassNames = getApplicationContext().getResources().getStringArray(
                R.array.asus_lockscreen_essential_app_class_name);
        mEssentialAppNormalDrawables = getApplicationContext().getResources().obtainTypedArray(
                R.array.asus_lockscreen_essential_app_drawable_normal);
        mEssentialAppNormalDrawablesPre = getApplicationContext().getResources().obtainTypedArray(
                R.array.asus_lockscreen_essential_app_drawable_normal_pre);
        mIsShortcutEnabled = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, 1) == 1);
        mSwitch = (Switch) findViewById(R.id.quick_access_switch);
        setSwitchChecked(mIsShortcutEnabled);
        mPm = getBaseContext().getPackageManager();
        mWallpaperManager = (WallpaperManager) getApplicationContext().getSystemService(Context.WALLPAPER_SERVICE);
        setImage();
        mShortcutPackageName = new String[3];
        mShortcutClassName = new String[3];
        mAppIcon = new Drawable[3];
        mAppName = new String[3];

        loadShortcutInfo();
        mDragGridView = (DragGridView) findViewById(R.id.dragGridView);
        setDragGridViewAdapter();

        mDragGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                Intent intent = new Intent(getBaseContext(), AsusSelectAPShortcutSettings.class);
                Bundle bundle = new Bundle();
                bundle.putLong("rowId", arg2);
                bundle.putBoolean("isKeyguard", false);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        mShortCutPreview = new ImageView[3];
        mShortCutPreview[0] = (ImageView) findViewById(R.id.shortcut_first_app_preview);
        mShortCutPreview[1] = (ImageView) findViewById(R.id.shortcut_second_app_preview);
        mShortCutPreview[2] = (ImageView) findViewById(R.id.shortcut_third_app_preview);

        mShortCutBackgroundPre = new ImageView[3];

        setShortcutPreview();

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mIsShortcutEnabled != isChecked) {
                    mIsShortcutEnabled = isChecked;
                    int status = (mIsShortcutEnabled ? 1 : 0);
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, status);
                }
                View shorycutsView = findViewById(R.id.shortcuts_layout);
                if (isChecked == false) {
                    shorycutsView.setVisibility(View.INVISIBLE);
                } else {
                    shorycutsView.setVisibility(View.VISIBLE);
                }
                mDragGridView.setEnabled(isChecked);
                setDragGridViewAlphaAndDrag(isChecked);
            }
        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mDragGridView.setEnabled(mIsShortcutEnabled);
                setDragGridViewAlphaAndDrag(mIsShortcutEnabled);
            }
        }, 50);
    }

    private void setDragGridViewAdapter() {
        dataSourceList.clear();
        for (int i = 0; i < 3; i++) {
            HashMap<String, Object> itemHashMap = new HashMap<String, Object>();
            itemHashMap.put("shortcut_background", R.drawable.asus_lockscreen_shortcut);
            itemHashMap.put("shortcut_app", getShortcutDrawable(i));
            itemHashMap.put("edit_app", R.drawable.asus_lockscreen_note);
            dataSourceList.add(itemHashMap);
        }
        final SimpleAdapter mSimpleAdapter = new SimpleAdapter(this, dataSourceList,
                R.layout.shortcuts_grid_item, new String[]{"shortcut_background", "shortcut_app", "edit_app"},
                new int[]{R.id.shortcut_background, R.id.shortcut_app, R.id.edit_app});

        mSimpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data,
                                        String textRepresentation) {
                if (view instanceof ImageView && data instanceof Drawable) {
                    ImageView iv = (ImageView) view;
                    BitmapDrawable shortcutBg = (BitmapDrawable) getApplicationContext()
                            .getResources().getDrawable(R.drawable.asus_lockscreen_shortcut);
                    int height = shortcutBg.getBitmap().getHeight();
                    int width = shortcutBg.getBitmap().getWidth();
                    int newHeight = (int) (height * SHORTCUT_RATIO);
                    int newWidth = (int) (width * SHORTCUT_RATIO);
                    iv.setMaxHeight(newHeight);
                    iv.setMaxWidth(newWidth);
                    Bitmap bp = getResizedBitmap((BitmapDrawable) data, newHeight, newWidth).getBitmap();
                    Drawable db = (Drawable) data;
                    iv.setImageDrawable(db);
                    return true;
                } else {
                    return false;
                }
            }
        });

        mDragGridView.setAdapter(mSimpleAdapter);
        mDragGridView.setOnChangeListener(new OnChanageListener() {

            @Override
            public void onChange(int from, int to, boolean isShow) {
                HashMap<String, Object> temp = dataSourceList.get(from);
                if (from < to) {
                    for (int i = from; i < to; i++) {
                        Collections.swap(dataSourceList, i, i + 1);
                    }
                } else if (from > to) {
                    for (int i = from; i > to; i--) {
                        Collections.swap(dataSourceList, i, i - 1);
                    }
                }
                dataSourceList.set(to, temp);
                mSimpleAdapter.notifyDataSetChanged();

                View viewFrom = mDragGridView.getChildAt(from);
                if (!isShow) {
                    ((ImageView) viewFrom.findViewById(R.id.shortcut_background)).setVisibility(View.INVISIBLE);
                } else {
                    ((ImageView) viewFrom.findViewById(R.id.shortcut_background)).setVisibility(View.VISIBLE);
                }
            }
        });

        mDragGridView.setOnActionUpListener(new OnActionUpListener() {

            @Override
            public void onActionUp(int from, int to) {
                // TODO Auto-generated method stub
                String[] packageAndClassName = new String[]{"", "", ""};
                String shortcutNames;
                for (int i = 0; i < 3; i++) {
                    packageAndClassName[i] = mShortcutPackageName[i] + "/" + mShortcutClassName[i];
                }
                if (Math.abs(from - to) <= 1) {
                    String temp = packageAndClassName[from];
                    packageAndClassName[from] = packageAndClassName[to];
                    packageAndClassName[to] = temp;
                } else {
                    String temp;
                    if (from < to) {
                        for (int i = 0; i < 2; i++) {
                            temp = packageAndClassName[i];
                            packageAndClassName[i] = packageAndClassName[i + 1];
                            packageAndClassName[i + 1] = temp;
                        }
                    } else {
                        for (int i = 2; i > 0; i--) {
                            temp = packageAndClassName[i];
                            packageAndClassName[i] = packageAndClassName[i - 1];
                            packageAndClassName[i - 1] = temp;
                        }
                    }
                }

                shortcutNames = packageAndClassName[0];
                for (int i = 1; i < 3; i++) {
                    shortcutNames += ", " + packageAndClassName[i];
                }
                Settings.System.putString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME, shortcutNames);
                loadShortcutInfo();
                setShortcutPreview();
                setShorCutBgShowOrHide();
            }
        });
    }

    private void setShorCutBgShowOrHide() {
        for (int i = 0; i < 3; i++) {
            int defaultIndex = getEssentialAppIndex(i);
            View view = mDragGridView.getChildAt(i);
            if (view != null) {
                if (defaultIndex != -1) {
                    ImageView shorCutBg = (ImageView) view.findViewById(R.id.shortcut_background);
                    ((ImageView) view.findViewById(R.id.shortcut_background)).setVisibility(View.INVISIBLE);
                } else {
                    ((ImageView) view.findViewById(R.id.shortcut_background)).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void setDragGridViewAlphaAndDrag(boolean val) {
        mDragGridView.setDragable(val);
        TextView textView = (TextView) findViewById(R.id.change_short_cut_message);
        if (val) {
            for (int i = 0; i < 3; i++) {
                View view = mDragGridView.getChildAt(i);
                if (view != null) {
                    ((ImageView) view.findViewById(R.id.shortcut_background)).setImageAlpha(255);
                    ((ImageView) view.findViewById(R.id.shortcut_app)).setImageAlpha(255);
                    ((ImageView) view.findViewById(R.id.edit_app)).setImageAlpha(255);
                }
            }
            if (!isPadMode() && textView != null) {
                textView.setTextColor(Color.argb(255, 255, 255, 255));
            }
        } else {
            for (int i = 0; i < 3; i++) {
                View view = mDragGridView.getChildAt(i);
                if (view != null) {
                    ((ImageView) view.findViewById(R.id.shortcut_background)).setImageAlpha(80);
                    ((ImageView) view.findViewById(R.id.shortcut_app)).setImageAlpha(80);
                    ((ImageView) view.findViewById(R.id.edit_app)).setImageAlpha(80);
                }
            }
            if (!isPadMode() && textView != null) {
                textView.setTextColor(Color.argb(80, 255, 255, 255));
            }
        }
    }

    @Override
    public void onResume() {
        if (!getResources().getBoolean(R.bool.quick_access_allow_rotation))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onResume();
        loadShortcutInfo();
        setDragGridViewAdapter();
        //setShortcut();
        setShortcutPreview();
        setDragGridViewStatus();
    }

    private void setDragGridViewStatus() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mDragGridView.setEnabled(mIsShortcutEnabled);
                setDragGridViewAlphaAndDrag(mIsShortcutEnabled);
                setShorCutBgShowOrHide();
            }
        }, 50);
    }

    private Drawable getShortcutDrawable(int index) {
        int defaultIndex = getEssentialAppIndex(index);
        if (defaultIndex != -1) {
            return getResources().getDrawable(mEssentialAppNormalDrawables
                    .getResourceId(defaultIndex, -1));
        } else {
            BitmapDrawable shortcutBg = (BitmapDrawable) getApplicationContext()
                    .getResources().getDrawable(R.drawable.asus_lockscreen_shortcut);
            int height = shortcutBg.getBitmap().getHeight();
            int width = shortcutBg.getBitmap().getWidth();
            int newHeight = (int) (height * SHORTCUT_RATIO);
            int newWidth = (int) (width * SHORTCUT_RATIO);
            //add by sunxiaolong@wind-mobi.com for #118573 20160628 begin
            if(mAppIcon[index] instanceof LayerDrawable){
                mAppIcon[index] = layerDrawableToBitMap((LayerDrawable) mAppIcon[index]);
            }
            //add by sunxiaolong@wind-mobi.com for #118573 20160628 end
            return getResizedBitmap((BitmapDrawable) mAppIcon[index], newHeight, newWidth);
        }
    }

    private void setShortcutPreview() {
        for (int i = 0; i < 3; i++) {
            mShortCutBackgroundPre[i] = (ImageView) findViewById(mShortcutBackgroundResourcesPre[i]);
            if (mShortCutPreview != null) {
                int defaultIndex = getEssentialAppIndex(i);
                if (defaultIndex != -1) {
                    mShortCutBackgroundPre[i].setVisibility(View.INVISIBLE);
                    mShortCutPreview[i]
                            .setImageResource(mEssentialAppNormalDrawablesPre
                                    .getResourceId(
                                            defaultIndex, -1));
                } else {
                    mShortCutBackgroundPre[i].setVisibility(View.VISIBLE);
                    BitmapDrawable shortcutBg = (BitmapDrawable) getApplicationContext()
                            .getResources().getDrawable(R.drawable.asus_lockscreen_shortcut_pre);
                    int height = shortcutBg.getBitmap().getHeight();
                    int width = shortcutBg.getBitmap().getWidth();
                    int newHeight = (int) (height * SHORTCUT_RATIO);
                    int newWidth = (int) (width * SHORTCUT_RATIO);
                    mShortCutPreview[i].setImageDrawable(getResizedBitmap(
                            (BitmapDrawable) mAppIcon[i], newHeight, newWidth));
                }
            }
        }
    }

    private void splitPackageNameAndClassName() {
        try {
            String settingShortcutString = Settings.System.getString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME);
            if (settingShortcutString == null) {
                Resources res = getBaseContext().getResources();
                int configStringRes = IS_WIFI_ONLY ? R.string.keyguard_default_shortcuts_wifi_only
                        : R.string.keyguard_default_shortcuts_telephony;
                settingShortcutString = res.getString(configStringRes);
                Settings.System.putString(getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME,
                        settingShortcutString);
            }
            Log.i(TAG, "settingShortcutString = " + settingShortcutString);
            String[] packageAndClassName = settingShortcutString.split(", ");

            for (int i = 0; i < 3; ++i) {
                try {
                    String[] tmp = packageAndClassName[i].split("/");
                    if (isAppInstalled(tmp[0]) || isAppOnSdcard(tmp[0])) {
                        mShortcutPackageName[i] = tmp[0];
                        mShortcutClassName[i] = tmp[1];
                    } else {
                        packageAndClassName[i] = handleShortcutIfAPPIsUninstalled(i);
                        Log.i(TAG, "packageAndClassName = " + packageAndClassName[i]);
                        String[] tmpPackageAndClassName = packageAndClassName[i].split("/");
                        mShortcutPackageName[i] = tmpPackageAndClassName[0];
                        mShortcutClassName[i] = tmpPackageAndClassName[1];
                    }
                } catch (Exception err) {
                    Log.e(TAG, "splitPackageNameAndClassName", err);
                }
            }
        } catch (Exception err) {
            Log.e(TAG, "splitPackageNameAndClassName", err);
        }
    }

    public boolean isAppInstalled(String packageName) {
        PackageManager pm = getBaseContext().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean enableAPP2SD() {
        //TODO
        return false;//Environment.isSecondaryExternalStorageRemovable();
    }

    public boolean isAppOnSdcard(String packageName) {
        if (!enableAPP2SD()) {
            return false;
        }
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_SIGNATURES);
            if (info == null) {
                return false;
            }
            if ((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0
                /*&& (info.flags & ApplicationInfo.FLAG_INSTALLED) != 0*/) {
                return true;
            }
            return false;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private String handleShortcutIfAPPIsUninstalled(int index) {
        String settingShortcut = Settings.System.getString(getBaseContext().getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME);
        String appInfoString = null;
        Resources res = getBaseContext().getResources();
        int defaultShortcutList = IS_WIFI_ONLY ? R.string.keyguard_default_shortcuts_wifi_only
                : R.string.keyguard_default_shortcuts_telephony;
        if (isVZWSku())
            defaultShortcutList = R.string.keyguard_default_shortcuts_vzw;
        String configShortcutString = res
                .getString(defaultShortcutList);
        String[] configPackageAndClassName = configShortcutString.split(", ");
        String[] settingPackageAndClassName = settingShortcut.split(", ");

        for (int i = 0; i < 3; ++i) {
            boolean add = true;
            for (int j = 0; j < 3; ++j) {
                if (configPackageAndClassName[i]
                        .equals(settingPackageAndClassName[j])) {
                    add = false;
                    break;
                }
            }
            if (add) {
                appInfoString = configPackageAndClassName[i];
                String newSettingShortcut = "";
                for (int k = 0; k < 3; ++k) {
                    if (k != 0) {
                        newSettingShortcut = newSettingShortcut + ", ";
                    }

                    if (k == index) {
                        newSettingShortcut = newSettingShortcut + appInfoString;
                    } else {
                        newSettingShortcut = newSettingShortcut
                                + settingPackageAndClassName[k];
                    }
                }

                Settings.System.putString(getBaseContext().getContentResolver(), Settings.System.ASUS_LOCKSCREEN_SHORTCUT_NAME, newSettingShortcut);
                return appInfoString;
            }
        }
        return appInfoString;
    }

    private void loadShortcutInfo() {
        splitPackageNameAndClassName();
        for (int i = 0; i < 3; ++i) {
            try {
                ComponentName name = new ComponentName(mShortcutPackageName[i], mShortcutClassName[i]);
                ActivityInfo activityInfo = mPm.getActivityInfo(name, PackageManager.GET_UNINSTALLED_PACKAGES
                        | PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_SIGNATURES);
                mAppIcon[i] = activityInfo.loadIcon(mPm);
                mAppName[i] = (String) activityInfo.loadLabel(mPm);
                //ComponentName name = new ComponentName(mShortcutPackageName[i],mShortcutClassName[i]);
                /*ApplicationInfo appInfo = mPm.getApplicationInfo(mShortcutPackageName[i], 0);
                Log.i(TAG,"mShortcutPackageName ="+mShortcutPackageName[i]);
                //ActivityInfo activityInfo = mPm.getActivityInfo(name, 0);
                mAppIcon[i] = mPm.getApplicationIcon(appInfo);//activityInfo.loadIcon(mPm);
                mAppName[i] = (String) mPm.getApplicationLabel(appInfo);//(String) activityInfo.loadLabel(mPm);*/
            } catch (final NameNotFoundException e) {
                if (isAppOnSdcard(mShortcutPackageName[i])) {
                    mAppIcon[i] = getResources().getDrawable(ResCustomizeConfig.getIdentifier(getBaseContext(),
                            "drawable", "sym_app_on_sd_unavailable_icon"));
                    mAppName[i] = mShortcutPackageName[i];
                } else {
                    mAppIcon[i] = null;
                    mAppName[i] = mShortcutPackageName[i];
                }
            } catch (Exception e) {
                Log.e(TAG, "loadShortcutInfo", e);
            }
        }
    }

    private void setSwitchChecked(boolean checked) {
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        View shorycutsView = findViewById(R.id.shortcuts_layout);
        if (checked == false) {
            shorycutsView.setVisibility(View.INVISIBLE);
        } else {
            shorycutsView.setVisibility(View.VISIBLE);
        }
    }

    private void setImage() {
        int width = -1;
        int height = -1;
        Drawable wallpaper = null;
        boolean isPadMode = isPadMode();
        if (isPadMode) {
            try {
                wallpaper = mWallpaperManager.getLockscreenWallpaper();
                if (wallpaper == null) {
                    wallpaper = mWallpaperManager.getDrawable();
                }
            } catch (NoSuchMethodError err) {
                Log.d(TAG, "NoSuchMethodError:" + err.getMessage());
            } catch (Exception err) {
                Log.d(TAG, "Exception:" + err.getMessage());
            }
        } else {
            // for solving lockscreen dds phone size error
            WindowManager wm = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Point realSize = new Point();
                wm.getDefaultDisplay().getRealSize(realSize);
                Log.v(TAG, "computeWallpaperBounds() realSize.x= "
                        + realSize.x
                        + ", computeWallpaperBounds() realSize.y= "
                        + realSize.y);
                width = Math.min(realSize.x, realSize.y);
                height = Math.max(realSize.x, realSize.y);
            } else {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(dm);
                width = Math.min(dm.widthPixels, dm.heightPixels);
                height = Math.max(dm.widthPixels, dm.heightPixels);
            }
            height = height - getActionBarHeight();
            try {
                wallpaper = mWallpaperManager.getLockscreenWallpaper(width,
                        height);
                if (wallpaper == null) {
                    wallpaper = mWallpaperManager.getDrawable();
                }
            } catch (NoSuchMethodError err) {
            } catch (Exception err) {
            }
        }

        if (wallpaper != null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.quick_access_tutorial);
            layout.setBackground(wallpaper);
        }
    }

    private boolean isPadMode() {
        if ((getApplicationContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            return true;
        }
        return false;
    }

    private int getActionBarHeight() {
        int height = (int) getResources().getDimension(R.dimen.actionbar_size);
        return height;
    }

    //add by sunxiaolong@wind-mobi.com for #118573 20160628 begin
    private BitmapDrawable layerDrawableToBitMap(LayerDrawable ld) {
        int width = ld.getIntrinsicWidth();
        int height = ld.getIntrinsicHeight();
        Bitmap.Config config = ld.getOpacity() != PixelFormat.OPAQUE ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        ld.setBounds(0, 0, width, height);
        ld.draw(canvas);
        BitmapDrawable newBitmap = new BitmapDrawable(bitmap);
        return newBitmap;
    }
    //add by sunxiaolong@wind-mobi.com for #118573 20160628 end

    private BitmapDrawable getResizedBitmap(BitmapDrawable bm, int newHeight,
                                            int newWidth) {
        try {
            int width = bm.getBitmap().getWidth();
            int height = bm.getBitmap().getHeight();

            Bitmap tempBitmap = bm.getBitmap();
            if (tempBitmap.getDensity() == Bitmap.DENSITY_NONE) {
                bm.setTargetDensity(getApplicationContext().getResources().getDisplayMetrics());
            }

            float ratio = (float) bm.getBitmap().getDensity()
                    / DisplayMetrics.DENSITY_MEDIUM;

            float scaleWidth = (newWidth * ratio) / width;
            float scaleHeight = (newHeight * ratio) / height;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);

            Bitmap bitmap = Bitmap.createBitmap(bm.getBitmap(), 0, 0, width,
                    height, matrix, true);
            BitmapDrawable resizeBitmap = new BitmapDrawable(bitmap);
            return resizeBitmap;
        } catch (Exception e) {
            Log.e(TAG, "getResizedBitmap", e);
            return null;
        }
    }

    public int getEssentialAppIndex(int shortcutId) {
        int index = -1;
        try {
            if (mEssentialAppPackageNames != null) {
                for (int i = 0; i < mEssentialAppPackageNames.length; i++) {
                    if (mEssentialAppPackageNames[i]
                            .equals(mShortcutPackageName[shortcutId])
                            && (mEssentialAppClassNames[i]
                            .equals(mShortcutClassName[shortcutId]))) {
                        return i;
                    }
                }
            }
        } catch (Exception err) {
            Log.d(TAG, "getEssentialAppIndex err = ", err);
        }
        return index;
    }

    public boolean isSupportPhone() {
        /*try {
            PackageManager pm = mContext.getPackageManager();
            pm.getPackageInfo(PHONE_PACKAGE, 0);
            Log.d(TAG, "isSupportPhone is true");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "isSupportPhone e = " + e);
            return false;
        }*/
        boolean voiceCapable = false;
        int vid = Resources.getSystem().getIdentifier("config_voice_capable", "bool", "android");
        try {
            voiceCapable = this.getResources().getBoolean(vid);
            Log.d(TAG, "isSupportPhone = " + voiceCapable);
        } catch (Resources.NotFoundException e) {
            Log.d(TAG, "isSupportPhone. e = " + e);
        }
        return voiceCapable;
    }

    public boolean isVZWSku() {
        String device = android.os.Build.DEVICE.toUpperCase(Locale.US);
        String sku = android.os.SystemProperties.get("ro.build.asus.sku").toUpperCase(Locale.US);
        return (device.equals("P008") && sku.startsWith("VZW"));
    }
}
