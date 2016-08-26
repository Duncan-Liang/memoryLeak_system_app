package com.android.settings.cta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;

public class CtaProviderUtils {
    // BEGIN COPY FROM CtaProvider
    private static final String AUTHORITY = "com.asus.CTA_PROVIDER";
    private static final String TABLE_PERMISSION = "permissions";
    public static final String COL_ACTION = "action";
    public static final String COL_CALLER = "caller";
    public static final String COL_ACCEPT = "accept";

    public static final Uri URI_PERMISSION = Uri.parse("content://" + AUTHORITY + "/" + TABLE_PERMISSION);
    // END COPY FROM CtaProvider

    Context mContext;

    public CtaProviderUtils(Context context) {
        mContext = context;
    }

    public boolean addPermission(int action, String caller, int accept) {
        ContentValues values = new ContentValues();
        values.put(COL_ACTION, action);
        values.put(COL_CALLER, caller);
        values.put(COL_ACCEPT, accept);

        try {
            mContext.getContentResolver().insert(URI_PERMISSION, values);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArrayList<Permission> getAllPermissions() {
        Cursor c = null;
        ArrayList<Permission> permissions = new ArrayList<Permission>();

        try {
            c = mContext.getContentResolver().query(URI_PERMISSION, null, null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    int action = c.getInt(c.getColumnIndex(COL_ACTION));
                    String caller = c.getString(c.getColumnIndex(COL_CALLER));
                    int accept = c.getInt(c.getColumnIndex(COL_ACCEPT));
                    permissions.add(new Permission(action, caller, accept));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (c != null) {
                c.close();
            }
        }

        return permissions;
    }

    public ArrayList<Permission> getPermissionsByAction(int action) {
        Cursor c = null;
        ArrayList<Permission> permissions = new ArrayList<Permission>();

        try {
            c = mContext.getContentResolver().query(
                    URI_PERMISSION,
                    null,
                    COL_ACTION + "=?",
                    new String[]{Integer.toString(action)},
                    null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    String caller = c.getString(c.getColumnIndex(COL_CALLER));
                    int accept = c.getInt(c.getColumnIndex(COL_ACCEPT));
                    permissions.add(new Permission(action, caller, accept));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (c != null) {
                c.close();
            }
        }

        return permissions;
    }

    public int removeAllPermissions() {
        int deleted = -1;

        try {
            deleted = mContext.getContentResolver().delete(URI_PERMISSION, null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return deleted;
    }

    public int removePermission(int action, String caller) {
        int deleted = -1;

        try {
            deleted = mContext.getContentResolver().delete(
                    URI_PERMISSION,
                    COL_ACTION + "=? AND " + COL_CALLER + "=?",
                    new String[]{Integer.toString(action), caller});
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return deleted;
    }

    public int updatePermission(int action, String caller, int accept) {
        int updated = -1;
        ContentValues values = new ContentValues();
        values.put(COL_ACCEPT, accept);

        try {
            updated = mContext.getContentResolver().update(
                    URI_PERMISSION,
                    values,
                    COL_ACTION + "=? AND " + COL_CALLER + "=?",
                    new String[]{Integer.toString(action), caller});
         }
        catch (Exception e) {
            e.printStackTrace();
        }

        return updated;
    }

    class Permission {
        int mAction;
        String mCaller;
        int mAccept;

        public Permission(int action, String caller, int accept) {
            mAction = action;
            mCaller = caller;
            mAccept = accept;
        }
    }
}
