package com.android.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ZenUIUpdateSettings  extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        String url = "market://search?q=pub:\"ZenUI,+ASUS+Computer+Inc.\"";
        Uri uri = Uri.parse(url);
        Intent itent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(itent);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

}
