/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.ListView;

import com.android.settings.Settings.TetherSettingsActivity;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardTile;
import com.android.settingslib.TetherUtil;

import com.mediatek.settings.FeatureOption;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CreateShortcut extends LauncherActivity {

    private static final String TOP_LEVEL_HEADER = "com.android.settings.TOP_LEVEL_HEADER_ID";

    /// M: add audio profile and sound class name @{
    private static final String TAG = "CreateShortcut";
    private static final String AUDIOPROFILE =
        "com.android.settings.Settings$AudioProfileSettingsActivity";
    private static final String SOUND = "com.android.settings.Settings$SoundSettingsActivity";
    private static final String NOTIFICATION =
        "com.android.settings.Settings$NotificationSettingsActivity";
    /// @}
    /// M: add DreamSettings class name @{
    private static final String DREAM_SETTINGS =
        "com.android.settings.Settings$DreamSettingsActivity";
    /// @}

    @Override
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent(Intent.ACTION_MAIN, null);
        targetIntent.addCategory("com.android.settings.SHORTCUT");
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, itemForPosition(position).label);
        ResolveInfo resolveInfo = itemForPosition(position).resolveInfo;
        ActivityInfo activityInfo = resolveInfo.activityInfo;
        if (activityInfo.metaData != null && activityInfo.metaData.containsKey(TOP_LEVEL_HEADER)) {
            int topLevelId = activityInfo.metaData.getInt(TOP_LEVEL_HEADER);
            int resourceId = getDrawableResource(topLevelId);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, createIcon(resourceId));
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    private Bitmap createIcon(int resource) {
        Context context = new ContextThemeWrapper(this, android.R.style.Theme_Material);
        View view = LayoutInflater.from(context).inflate(R.layout.shortcut_badge, null);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageResource(resource);

        int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        view.measure(spec, spec);
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.draw(canvas);
        return bitmap;
    }

    private int getDrawableResource(int topLevelId) {
        ArrayList<DashboardCategory> categories = new ArrayList<>();
        SettingsActivity.loadCategoriesFromResource(R.xml.dashboard_categories, categories, this);
        for (DashboardCategory category : categories) {
            for (DashboardTile tile : category.tiles) {
                if (tile.id == topLevelId) {
                    return tile.iconRes;
                }
            }
        }
        return 0;
    }

    /// M: update create shortcut list items @{
    @Override
    public List<ListItem> makeListItems() {
        List<ListItem> list = super.makeListItems();
        list.removeAll(makeRemoveListItems(list));
        return list;
    }

    private List<ListItem> makeRemoveListItems(List<ListItem> list) {
        ArrayList<ListItem> result = new ArrayList<>();
        if (list != null) {
            ArrayList<String> needRemoveItemComp = new ArrayList<>();
            // Resolve audio profile and sound setting conflict
            if (FeatureOption.MTK_AUDIO_PROFILES) {
                needRemoveItemComp.add(SOUND);
                needRemoveItemComp.add(NOTIFICATION);
                Log.i(TAG, "Not support google sound, remove it");
            } else {
                needRemoveItemComp.add(AUDIOPROFILE);
                Log.i(TAG, "Not support mtk audio profle, remove it");
            }
            // Remove DreamSettings when device is low ram device
            if (FeatureOption.MTK_GMO_RAM_OPTIMIZE) {
                needRemoveItemComp.add(DREAM_SETTINGS);
                Log.i(TAG, "Not support daydream, remove it");
            }
            // Record need remove items
            Iterator<ListItem> oriListInte = list.iterator();
            while (oriListInte.hasNext()) {
                ListItem item = oriListInte.next();
                if (needRemoveItemComp.contains(item.className)) {
                    result.add(item);
                }
            }
        }
        return result;
    }
    /// @}

    @Override
    protected boolean onEvaluateShowIcons() {
        return false;
    }

    /**
     * Perform query on package manager for list items.  The default
     * implementation queries for activities.
     */
    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
        if (activities == null) return null;
        for (int i = activities.size() - 1; i >= 0; i--) {
            ResolveInfo info = activities.get(i);
            if (info.activityInfo.name.endsWith(TetherSettingsActivity.class.getSimpleName())) {
                if (!TetherUtil.isTetheringSupported(this)) {
                    activities.remove(i);
                }
            }
        }
        return activities;
    }
}
