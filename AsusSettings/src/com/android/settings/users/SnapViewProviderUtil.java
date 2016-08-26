
package com.android.settings.users;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

public class SnapViewProviderUtil {

    private static final String TAG = "SnapViewProviderUtil";
    private static final String AUTHORITY = SnapViewProvider.AUTHORITY;
    public static final String ACCOUNT = "account";
    public static final String QUESTION = "question";
    public static final String ANSWER = "answer";

    public static final class Secure
    {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/secures");

        public static final String NAME = "name";
        public static final String VALUE = "value";

        public static boolean putAccount(ContentResolver resolver, String title, String value)
        {
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, title);
                values.put(VALUE, value);
                resolver.insert(CONTENT_URI, values);
                return true;
            } catch (SQLException e) {
                Log.w(TAG, "Can't set key ACCOUNT  in " + CONTENT_URI, e);
                return false;
            }
        }

        public static boolean updateAccount(ContentResolver resolver, String title, String value)
        {
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, title);
                values.put(VALUE, value);
                resolver.update(CONTENT_URI, values, null, null);
                return true;
            } catch (SQLException e) {
                Log.w(TAG, "Can't update key ACCOUNT in " + CONTENT_URI, e);
                return false;
            }
        }

        public static String getAccount(ContentResolver resolver, Uri content_uri, String title)
        {
            Cursor cur = null;

            cur = resolver.query(content_uri, new String[] {"name","value"}, "name=?", new String[] {title}, null);
            String value = null;
            if (cur != null) {
                try {
                    if (cur.moveToFirst()) {
                        value = cur.getString(1);
                    }
                }
                finally {
                    cur.close();
                }
            }

            return value;
        }

        public static String getAccount(ContentResolver resolver, String title)
        {
            return getAccount(resolver, CONTENT_URI, title);
        }
    }
}
