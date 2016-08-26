package com.android.settings.cta;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.asus.cta.CtaChecker;

import java.util.ArrayList;
import java.util.List;

public class CtaItemListActivity extends Activity {
    PackageManager mPackageManager = null;
    CtaProviderUtils mProviderUtils;

    public CtaItemListActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        CtaChecker ctaChecker = new CtaChecker(this);
        if (!ctaChecker.isSupportCta()) {
            Toast.makeText(this, "Don't support asus CTA test.", Toast.LENGTH_SHORT).show();
            finish();
        }
        int type = bundle.getInt("CTA_TYPE");
        mPackageManager = getPackageManager();

        mProviderUtils = new CtaProviderUtils(this);
        List<CtaItem> ctaItemList = new ArrayList<CtaItem>();

        setContentView(R.layout.cta_activity_item);
        ListView ctaTypeListView = (ListView)findViewById(R.id.list_item);
        TextView textDesc = (TextView)findViewById(R.id.cta_list_item_desc);
        textDesc.setText(ctaChecker.getMessage(type,
                getResources().getText(R.string.descrption_applications).toString()));

        ArrayList<CtaProviderUtils.Permission>permissions = mProviderUtils.getPermissionsByAction(type);
        for (CtaProviderUtils.Permission permission : permissions) {
            CtaItem item = new CtaItem(permission.mAction, permission.mCaller, permission.mAccept);
            ctaItemList.add(item);
        }

        CtaItemListAdapter adapter = new CtaItemListAdapter(this, ctaItemList);
        ctaTypeListView.setAdapter(adapter);
     }

    class CtaItem {
        int mAction;
        String mCaller;
        int mAccept;
        Drawable mLogo;
        String mLabel;

        public CtaItem(int action, String caller, int accept) {
            mAction = action;
            mCaller = caller;
            mAccept = accept;

            ApplicationInfo appInfo = null;
            try {
                appInfo = mPackageManager.getApplicationInfo(caller, 0);
                mLabel = (String)appInfo.loadLabel(mPackageManager);
                mLogo = appInfo.loadIcon(mPackageManager);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
