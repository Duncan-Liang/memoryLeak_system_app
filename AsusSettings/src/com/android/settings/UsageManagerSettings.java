package com.android.settings;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.applications.ManageApplications;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.fuelgauge.PowerUsageSummary;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.ActionBar.OnNavigationListener;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceActivity;
import android.telephony.TelephonyManager;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class UsageManagerSettings extends SubSettings {

    private static final String CURRENT_TAB = "current_tab";
    private int mCurrentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar bar = getActionBar();
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayShowTitleEnabled(false);
        //Ken1_Yu, change Navigationmode to spinner
        SpinnerAdapter mSpinnerAdapter = new MyArrayAdapter(this, R.array.usage_title, android.R.layout.simple_spinner_dropdown_item);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        bar.setListNavigationCallbacks(mSpinnerAdapter, new DropDownListenser());
        if(savedInstanceState != null){
            int tab = savedInstanceState.getInt(CURRENT_TAB);
           bar.setSelectedNavigationItem(tab);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title) {
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (addToBackStack) {
            transaction.addToBackStack(SettingsActivity.BACK_STACK_PREFS);
        }
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        //super.onSaveInstanceState(outState);
            outState.putInt(CURRENT_TAB, mCurrentTab);
    }

    class DropDownListenser implements OnNavigationListener
    {
        String[] listNames = getResources().getStringArray(R.array.usage_title);

        public boolean onNavigationItemSelected(int itemPosition, long itemId)
        {
            switch(itemPosition){
            case 0:
                mCurrentTab =0;
                switchToFragment(
                        PowerUsageSummary.class.getName(), null, false, true,
                        R.string.usage_power_settings_title, null);
                break;
            case 1:
                mCurrentTab =1;
                switchToFragment(
                        DataUsageSummary.class.getName(), null, false, true,
                        R.string.usage_data_usage_summary_title, null);
                break;
            case 2:
                mCurrentTab =2;
                Bundle arg = new Bundle();
                arg.putString("classname", "com.android.settings.Settings$RunningServicesActivity");
                switchToFragment(
                        ManageApplications.class.getName(), arg, false, true,
                        R.string.usage_applications_settings, null);
                break;
            }

            return true;
        }
    }

    private class MyArrayAdapter extends ArrayAdapter<CharSequence> implements SpinnerAdapter {
        public MyArrayAdapter(Context context, int textArrayResId, int textViewResId) {
            super(context, textViewResId, context.getResources().getTextArray(textArrayResId));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text = (TextView) view;
            text.setTextAppearance(getApplicationContext(), R.style.UsageManagerTitleTextStyle);
            return view;
        }
    }
}
