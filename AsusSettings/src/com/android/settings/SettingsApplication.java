package com.android.settings;
import android.app.Application;
import android.content.Context;
import com.squareup.leakcanary.LeakCanary;
import android.os.StrictMode;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD;

public class SettingsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        enabledStrictMode();
        LeakCanary.install(this);
    }

    private void enabledStrictMode() {
        if (SDK_INT >= GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder() //
                    .detectAll() //
                    .penaltyLog() //
                    .penaltyDeath() //
                    .build());
        }
    }
}