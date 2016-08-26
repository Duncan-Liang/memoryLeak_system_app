package com.android.settings.flipfont;

import android.app.ActivityManager;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/*
 * Class to write ttf files.
 * Used also to write font location (*.loc) files
 */
public class FontWriter {

    FileOutputStream fOut = null;
    OutputStreamWriter osw = null;

    // Subdirectory name for font files.  "app_" is added when getDir()
    public static final String FONT_DIRECTORY = "fonts";

    // path to loc files
    private static final String FLIPFONT_PATH = "/flipfont";
    private static final String APP_FONTS_PATH = "/app_fonts";

    private static final String OWNER_DATA_PATH = "/data/data/";
    private static final String USER_DATA_PATH = "/data/user/";

    /*
     *  Create the loc file for given font containing absolute path to font directory
     */
    public void writeLoc(Context context, String fontpath, String fontname) {
        // TODO - write property to improve speed - either for owner only or for all users
        // Write the FlipFont path to system properties
        Settings.Global.putString(context.getContentResolver(), Settings.Global.FLIPFONT_SETTINGS_PATH, fontpath);
        Settings.Global.putString(context.getContentResolver(), Settings.Global.FLIPFONT_SETTINGS_NAME, fontname);
    }

    /*
     * Create font directory and change the access permissions
     */
    public File createFontDirectory(Context context, String fontName) {
        int currentUser = UserHandle.getCallingUserId();
        // try ActivityManager.getCurrentUser() if available - it is more accurate
        if (context != null && currentUser == 0) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                currentUser = am.getCurrentUser();
            } catch (Exception e) {
            }
        }
        File appFontDir;
        Log.v("FlipFont>FontWriter", "createFontDirectory > currentUser" + currentUser);
        if (currentUser == 0) {
            File flipFontDir = new File(OWNER_DATA_PATH + FLIPFONT_PATH);
            if (!flipFontDir.exists()) {
                flipFontDir.mkdir();
                flipFontDir.setExecutable(true, false);
            }

            appFontDir = new File(OWNER_DATA_PATH + FLIPFONT_PATH + APP_FONTS_PATH);
            if (!appFontDir.exists()) {
                appFontDir.mkdir();
                appFontDir.setExecutable(true, false);
            }
        } else {
            File userDir = new File(USER_DATA_PATH + currentUser);
            userDir.setExecutable(true, false);

            File flipFontDir = new File(USER_DATA_PATH + currentUser + FLIPFONT_PATH);
            if (!flipFontDir.exists()) {
                flipFontDir.mkdir();
                flipFontDir.setExecutable(true, false);
            }

            appFontDir = new File(USER_DATA_PATH + currentUser + FLIPFONT_PATH + APP_FONTS_PATH);
            if (!appFontDir.exists()) {
                appFontDir.mkdir();
                appFontDir.setExecutable(true, false);
            }
            Log.v("FlipFont>FontWriter", "createFontDirectory > path user = " + USER_DATA_PATH + currentUser + FLIPFONT_PATH + APP_FONTS_PATH);
        }
        File fontFile = new File(appFontDir, fontName);

        // creat font folder
        fontFile.mkdir();
        fontFile.setExecutable(true, false); // allow directory execute for all

        return fontFile;
    }


    /*
     * Deleate previous font directories 
     */
    public void deleteFontDirectory(Context context, String keepfolder) {
        ////File newFontDir = context.getDir(FONT_DIRECTORY, Context.MODE_WORLD_READABLE);
        int currentUser = UserHandle.getCallingUserId();
        // try ActivityManager.getCurrentUser() if available - it is more accurate
        if (context != null && currentUser == 0) {
            try {
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                currentUser = am.getCurrentUser();
            } catch (Exception e) {
            }
        }
        File flipFontDir;
        if (currentUser == 0)
            flipFontDir = new File(OWNER_DATA_PATH + FLIPFONT_PATH + APP_FONTS_PATH);
        else
            flipFontDir = new File(USER_DATA_PATH + currentUser + FLIPFONT_PATH + APP_FONTS_PATH);

        File fontFile = new File(flipFontDir, keepfolder);

        // delete all the existing fonts
        // get a list of subdirectories and delete them
        String tmp[] = flipFontDir.list();
        if (tmp != null) {
            for (int i = 0; i < tmp.length; i++) {
                if (0 != tmp[i].compareTo(keepfolder))
                    deleteFolder(flipFontDir, tmp[i]);
            }
        }
    }

    /*
     * Delete folder and all files in the folder
     */
    private boolean deleteFolder(File FontDir, String folderName) {
        boolean bRet;
        File newDir = new File(FontDir, folderName);
        String tmp[] = newDir.list();
        if (tmp != null) {
            for (int i = 0; i < tmp.length; i++) {
                File file = new File(newDir, tmp[i]);
                file.delete();
            }
            bRet = newDir.delete();
        } else
            bRet = false;
        return (bRet);
    }

    /*
     * Copy font ttf file to given destination and name
     * @param directory Directory where the font ttf shoud be written
     * @param is Stream containing open InputStream to the ttf file
     * @param destination Filename for the new ttf file
     */
    public boolean copyFontFile(File directory, InputStream is, String destination) {
        InputStream myInputStream = is;
        File myDirectory = directory;
        String myDestination = destination;
        boolean err_filecopy = false;
        try {
            File dest = new File(myDirectory, myDestination);
            dest.createNewFile();
            dest.setReadable(true, false); // allow read access by everyone

            fOut = new FileOutputStream(dest);
            BufferedOutputStream os = new BufferedOutputStream(fOut);

            byte[] b = new byte[1024];

            int read = 0;
            while ((read = myInputStream.read(b)) > 0) {
                os.write(b, 0, read);
            }
            os.flush();
            fOut.flush();
            os.close();
        } catch (Exception ex) {

            //Toast.makeText(ReadWebpageAsynTask.this, "file copy error: "+ e.getMessage().toString(),  Toast.LENGTH_LONG).show();

            err_filecopy = true;

            // Check file length. Zero length files can possibly brick the phone
            File file = new File(myDirectory, myDestination);
            long length = file.length();
            if (length == 0) {
                file.delete();
            }
            ex.printStackTrace();
        } finally {
            try {
                if (myInputStream != null)
                    myInputStream.close();
                if (fOut != null)
                    fOut.close();

            } catch (IOException e) {
            }
        }

        // Check file length. Zero length files can possibly brick the phone
        File file = new File(myDirectory, myDestination);
        long length = file.length();
        if (length == 0) {
            file.delete();
            err_filecopy = true;
        }

        return err_filecopy;
    }
}
