package com.android.settings.fingerprint;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.SystemProperties;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiongshigui on 2016/8/23.
 */
public class RemoveFingerprintsService extends IntentService {
    private static final String TAG = "RemoveFingerprintsService";

    private FingerprintManager mFpm;

    private List<Fingerprint> mPendingRmFps;

    private FingerprintManager.RemovalCallback mFpRemoveCallback = new FingerprintManager.RemovalCallback(){
        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
            Log.d(TAG, "onRemovalError, fp id = " + fp.getFingerId() + ", error = " + errString);
        }

        @Override
        public void onRemovalSucceeded(Fingerprint fingerprint) {
            Log.d(TAG, "onRemovalSucceeded, fp id = " + fingerprint.getFingerId());
            for (int i = 0; i < mPendingRmFps.size(); i++) {
                if (mPendingRmFps.get(i).getFingerId() == fingerprint.getFingerId()) {
                    mPendingRmFps.remove(i);
                    break;
                }
            }
            Log.d(TAG, "onRemovalSucceeded --- mPendingRmFps --- = " + mPendingRmFps);
            Log.d(TAG, "onRemovalSucceeded --- mFpm.getEnrolledFingerprints() --- = " + mFpm.getEnrolledFingerprints().size());
//            if (mPendingRmFps.size() == 0) {
//                Log.d(TAG, "onRemovalSucceeded, all fps has been removed.");
//                SystemProperties.set("persist.fingerprints.removed", "1");
//            }
            if (mFpm.getEnrolledFingerprints().size() == 0) {
                Log.d(TAG, "onRemovalSucceeded, all fps has been removed.");
                SystemProperties.set("persist.fingerprints.removed", "1");
            }
        }
    };

    public RemoveFingerprintsService() {
        super("RemoveFingerprintsService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mFpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: " + intent.getAction());
        if (!"1".equals(SystemProperties.get("persist.fingerprints.removed"))) {
            mPendingRmFps = mFpm.getEnrolledFingerprints();
            if (mPendingRmFps.size() > 0) {
                for (Fingerprint fp: mPendingRmFps) {
                    Log.d(TAG, "removing fp, id = " + fp.getFingerId());
                    mFpm.remove(fp, mFpRemoveCallback);
                }
            }
        }
    }
}
