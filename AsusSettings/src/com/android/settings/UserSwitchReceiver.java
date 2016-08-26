
package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

public class UserSwitchReceiver extends BroadcastReceiver {

    private static final String TAG = "UserSwitchReceiver";
    private static final String USER_ID = "new_user_id";

    @Override
    public void onReceive(Context context, Intent intent) {

        int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.myUserId());
        Log.i(TAG, "UserSwitchReceiver onReceive. User Id = " + userId);
        if (UserHandle.USER_OWNER != userId) {
            context.startService(new Intent(context, UserSwitchService.class).putExtra(USER_ID,
                    userId));
        }
    }
}
