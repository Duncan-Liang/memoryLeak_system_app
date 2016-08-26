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

package com.android.settings.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.switchenabler.AbstractEnabler;
import com.android.settings.switchenabler.BluetoothSwitchEnabler;
import com.android.settings.switchenabler.LocationEnabler;
import com.android.settings.switchenabler.MobileDataEnabler;
import com.android.settings.switchenabler.WifiSwitchEnabler;

import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

import java.util.HashMap;
import java.util.List;
//liangfeng@wind-mobi.com 20160808 for MTBF test OutofMemeryError crash start bug#126167
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
//liangfeng@wind-mobi.com 20160808 for MTBF test OutofMemeryError crash end bug#126167

public class DashboardSummary extends InstrumentedFragment {
    private static final String LOG_TAG = "DashboardSummary";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboard;
    //++Sunny_Yuan
    private  WifiSwitchEnabler mWifiEnabler;
    private  BluetoothSwitchEnabler mBluetoothEnabler;
    private  LocationEnabler mLocationEnabler;
    private MobileDataEnabler mMobileDataEnabler;
    private  HashMap<Long, AbstractEnabler> mEnablers;
    private int mEasyMode ;
    private static  int[] TYPE_SWITCH_TILES = {
        R.id.location_settings,
        R.id.wifi_settings,
        R.id.bluetooth_settings,
    };

    private static final int MSG_REBUILD_UI = 1;
    private ISettingsMiscExt mExt;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };
    //++Sunny_Yuan
    private static boolean isTypeSwitch(long id) {
        for (int tileId : TYPE_SWITCH_TILES) {
            if (id == (long)tileId) return true;
        }
        return false;
    }

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();
        //++Sunny_Yuan,easyMode
        mWifiEnabler = new WifiSwitchEnabler(getActivity(), new Switch(getActivity()));
        mBluetoothEnabler = new BluetoothSwitchEnabler(getActivity(), new Switch(getActivity()));
        mLocationEnabler = new LocationEnabler(getActivity(), new Switch(getActivity()));
        mMobileDataEnabler = new MobileDataEnabler(getActivity(), new Switch(getActivity()));

        mEnablers = new HashMap<Long, AbstractEnabler>();
        mEnablers.put(Long.valueOf((long) R.id.wifi_settings), mWifiEnabler);
        mEnablers.put(Long.valueOf((long) R.id.bluetooth_settings), mBluetoothEnabler);
        mEnablers.put(Long.valueOf((long) R.id.location_settings), mLocationEnabler);
        //delete by sunxiaolong@wind-mobi.com for #117974 start
        //mEnablers.put(Long.valueOf((long) R.id.mobile_data_settings), mMobileDataEnabler);
        //delete by sunxiaolong@wind-mobi.com for #117974 end

        mEasyMode = android.provider.Settings.System.getInt(getActivity().getContentResolver(),
                android.provider.Settings.System.ASUS_EASY_LAUNCHER,
                android.provider.Settings.System.ASUS_EASY_LAUNCHER_DISABLED);

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);
        //++Sunny_Yuan
        if (null != mEnablers) {
            for (AbstractEnabler enabler : mEnablers.values()) {
                enabler.resume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
        //++Sunny_Yuan
        if(null != mEnablers){
            for(AbstractEnabler enabler : mEnablers.values()) {
                enabler.pause();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mExt = UtilsExt.getMiscPlugin(this.getActivity());
        mLayoutInflater = inflater;

        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);

        return rootView;
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(LOG_TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboard.removeAllViews();
        //++Sunny_Yuan
        mEnablers = new HashMap<Long, AbstractEnabler>();

        List<DashboardCategory> categories =
                ((SettingsActivity) context).getDashboardCategories(true);

        final int count = categories.size();

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);
            //++Sunny_Yuan, easyMode
            boolean isEasyModeMore = ((SettingsActivity) context).isEasyModeShowMore();
            mEasyMode = ((SettingsActivity) context).getEasyMode();
            View categoryView = null;
            if (Settings.System.ASUS_EASY_LAUNCHER_ENABLED == mEasyMode && !isEasyModeMore) {
                //++Sunny_Yuan, easyMode: only show tile view
                categoryView = mLayoutInflater.inflate(R.layout.dashboard_category_easy_mode, mDashboard,
                        false);
            } else {
                categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                        false);
                TextView categoryLabel = (TextView) categoryView.findViewById(R.id.category_title);
                categoryLabel.setText(category.getTitle(res));
            }  
            ViewGroup categoryContent =
                    (ViewGroup) categoryView.findViewById(R.id.category_content);

            final int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);

                DashboardTileView tileView = new DashboardTileView(context);
                if(isTypeSwitch(tile.id)){ //++Sunny_Yuan, switch type
                    tileView.getSwitch().setVisibility(View.VISIBLE);
                    setEnabler(tile,tileView.getSwitch());
                }else{
                    tileView.getSwitch().setVisibility(View.GONE);
                }
                updateTileView(context, res, tile, tileView.getImageView(),
                        tileView.getTitleTextView(), tileView.getStatusTextView());

                tileView.setTile(tile);

                categoryContent.addView(tileView);
            }

            // Add the category
            mDashboard.addView(categoryView);
        }
        long delta = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "rebuildUI took: " + delta + " ms");
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile,
            ImageView tileIcon, TextView tileTextView, TextView statusTextView) {

        if (!TextUtils.isEmpty(tile.iconPkg)) {
            try {
                Drawable drawable = context.getPackageManager()
                        .getResourcesForApplication(tile.iconPkg).getDrawable(tile.iconRes, null);
                if (!tile.iconPkg.equals(context.getPackageName()) && drawable != null) {
                    // If this drawable is coming from outside Settings, tint it to match the color.
                    TypedValue tintColor = new TypedValue();
                    context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                            tintColor, true);
                    drawable.setTint(tintColor.data);
                }
                tileIcon.setImageDrawable(drawable);
            } catch (NameNotFoundException | Resources.NotFoundException e) {
                tileIcon.setImageDrawable(null);
                tileIcon.setBackground(null);
            }
        } else if (tile.iconRes > 0) {
            //liangfeng@wind-mobi.com 20160808 for MTBF test OutofMemeryError crash start bug#126167
            //tileIcon.setImageResource(tile.iconRes);
            tileIcon.setImageBitmap(readBitMap(getActivity(),tile.iconRes));
            //liangfeng@wind-mobi.com 20160808 for MTBF test OutofMemeryError crash end bug#126167
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
            mExt.customizeDashboardTile(tile, tileIcon);
        }
        ///M: feature replace sim to uim
        tileTextView.setText(mExt.customizeSimDisplayString(
                tile.getTitle(res).toString(), SubscriptionManager.INVALID_SUBSCRIPTION_ID));

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }

    //liangfeng@wind-mobi.com 20160808 for mtbf test OutofMemeryError crash start bug#126167
    /**
     * @param context
     * @param resId
     * @return
     */
    private Bitmap readBitMap(Context context, int resId){
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is,null,opt);
    }
    //liangfeng@wind-mobi.com 20160808 for mtbf test OutofMemeryError crash end bug#126167



    //++Sunny_Yuan
    private void setEnabler(DashboardTile tile, Switch tileSwitch) {
        if (R.id.wifi_settings == tile.id) {
            mWifiEnabler.setSwitch(tileSwitch);
        } else if (R.id.bluetooth_settings == tile.id) {
            mBluetoothEnabler.setSwitch(tileSwitch);
        } else if (R.id.location_settings == tile.id) {
            mLocationEnabler.setSwitch(tileSwitch);
        //delete by sunxiaolong@wind-mobi.com for #117974 start
        } /*else if (R.id.mobile_data_settings == tile.id) {
            mMobileDataEnabler.setSwitch(tileSwitch);
        }*/
        //delete by sunxiaolong@wind-mobi.com for #117974 end

    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }
}
