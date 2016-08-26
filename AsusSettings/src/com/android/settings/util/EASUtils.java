package com.android.settings.util;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.util.ArraySet;

public class EASUtils {

    private static final String ASUS_EXCHANGE_ACCOUNT_MANAGER_TYPE = "com.asus.exchange";

    //+++Jason_Uang, for AT&T EAS disable SmartLock issue
    public static boolean hasEASAccountAndIsATTsku(Context context) {
        boolean isATT = Utils.isATT();
        boolean existEASAccount = hasEASAccount(context);
        return (existEASAccount && isATT);
    }

    private static boolean hasEASAccount(Context context) {
        final AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        final Account[] accounts = accountManager.getAccountsByType(ASUS_EXCHANGE_ACCOUNT_MANAGER_TYPE);
        boolean hasEASaccounts = (accounts.length > 0) ? true : false;
        return hasEASaccounts;
    }
    //---Jason_Uang, for AT&T EAS disable SmartLock issue

    //+++Jason_Uang, for AT&T EAS disable SmartLock issue
    public static void disableTrustAgentsForATTsku(Context context) {
        if (Utils.isATT()) {
            disableTrustAgents(context);
        }
    }
    //---Jason_Uang, for AT&T EAS disable SmartLock issue

    private static void disableTrustAgents(Context context) {
        final ArraySet<ComponentName> activeAgents = new ArraySet<ComponentName>();
        final LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
        lockPatternUtils.setEnabledTrustAgents(activeAgents, UserHandle.myUserId());
    }

}
