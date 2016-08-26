
package com.android.settings.users;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class SnapViewProvider extends ContentProvider {

    public static final String TAG = "SnapViewProvider";
    public static final String AUTHORITY = "SnapView_DB_Provider";

    public static final String SECURE_TABLE_NAME = "secure";
    private static final int SECURES = 1;
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "secures", SECURES);
    }

    private DatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {

            case SECURES:
                qb.setTables(SECURE_TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        // the default Cursor interface does not support per-user observation
        try {
            AbstractCursor c = (AbstractCursor) ret;
            c.setNotificationUri(getContext().getContentResolver(), uri);
        } catch (ClassCastException e) {
            // details of the concrete Cursor implementation have changed and this code has
            // not been updated to match -- complain and fail hard.
            Log.e(TAG, "Incompatible cursor derivation!");
            throw e;
        }

        return ret;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        long rowId = 0;
        Uri rst_uri = null;
        switch (sUriMatcher.match(uri)) {
            case SECURES:
                rowId = db.insert(SECURE_TABLE_NAME, "name", values);
                rst_uri = ContentUris.withAppendedId(SnapViewProviderUtil.Secure.CONTENT_URI,
                        rowId);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowId > 0) {
            getContext().getContentResolver().notifyChange(rst_uri, null, true);
            return rst_uri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int count;
        switch (sUriMatcher.match(uri)) {
            case SECURES:
                count = db.delete(SECURE_TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null, true);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        int count;
        switch (sUriMatcher.match(uri)) {
            case SECURES:
                count = db.update(SECURE_TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null, true);

        return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        private static final String TAG = SnapViewProvider.TAG;

        private static final String DATABASE_NAME = "snapview.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE " + SnapViewProvider.SECURE_TABLE_NAME +
                " (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE ON CONFLICT REPLACE," +
                "value TEXT" +
                ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            // bypass for version 1
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

            int version = oldVersion;
        }
    }
}
