/* DeviceConf.java
 * To read the device configuration file located at /system/etc
 *
 * First version by ray2_lin@asus.com */

package com.android.settings.utils;

import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @hide Utility class for load device config file
 */

public class DeviceConf extends ArrayList<Pair<String, String>> {
    private static final long serialVersionUID = 4154379133222419302L;
    private static final String LOG_TAG = "DeviceConf";
    private final String DEFAULT_PATH = "/system/etc/devconf.json";

    public DeviceConf(String topic) {
        init(DEFAULT_PATH, topic);
    }

    public DeviceConf(String path, String topic) {
        init(path, topic);
    }

    private void init(String path, String topic) {
        JsonReader r;

        try {
            r = new JsonReader(new FileReader(path));
        } catch (FileNotFoundException e) {
            Log.d(LOG_TAG, "Configuration file not found.");
            return;
        }

        try {
            for (r.beginArray(); r.hasNext(); skipToNextObject(r)) {
                r.beginObject();

                if (!r.hasNext())
                    continue;

                String key = r.nextName();
                String value = r.nextString();
                if (!"topic".equalsIgnoreCase(key) || !topic.equalsIgnoreCase(value))
                    continue;

                while (r.hasNext()) {
                    key = r.nextName();
                    value = r.nextString();
                    add(new Pair<String, String>(key, value));
                }
            }

            r.endArray();

        } catch (IOException e) {
            Log.d(LOG_TAG, "Syntax error in configuration file.");
        }

        try {
            r.close();
        } catch (IOException e) {
        }
    }


    private void skipToNextObject(JsonReader r) throws IOException {
        while (r.hasNext()) {
            r.nextName();
            r.skipValue();
        }

        r.endObject();
    }

    public String get(String key) {
        for (Pair<String, String> p : this) {
            if (key.equalsIgnoreCase(p.first))
                return p.second;
        }

        return null;
    }

    public String get(String key, String defaultval) {
        for (Pair<String, String> p : this) {
            if (key.equalsIgnoreCase(p.first))
                return p.second != null ? p.second : defaultval;
        }

        return defaultval;
    }

    public int getInt(String key, int defaultval) throws NumberFormatException {
        String val = get(key);
        if (val == null)
            return defaultval;

        return Integer.parseInt(val.trim());
    }

    public float getFloat(String key, float defaultval) throws NumberFormatException {
        String val = get(key);
        if (val == null)
            return defaultval;

        return Float.parseFloat(val.trim());
    }

    public boolean getBoolean(String key, boolean defaultval) {
        String val = get(key);
        if (val == null)
            return defaultval;

        return (val.equals("1") || val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("y"));
    }

    public String[] getStringArray(String key, String[] defaultval) {
        String val = get(key);
        if (val == null)
            return defaultval;

        String s[] = val.split(",");
        for (int i = 0; i < s.length; i++)
            s[i] = s[i].trim();

        return s;
    }

    public int[] getIntArray(String key, int[] defaultval) throws NumberFormatException {
        String s[] = getStringArray(key, null);
        if (s == null)
            return defaultval;

        int[] v = new int[s.length];
        for (int i = 0; i < s.length; i++)
            v[i] = Integer.parseInt(s[i]);

        return v;
    }

    public float[] getFloatArray(String key, float[] defaultval) throws NumberFormatException {
        String s[] = getStringArray(key, null);
        if (s == null)
            return defaultval;

        float[] v = new float[s.length];
        for (int i = 0; i < s.length; i++)
            v[i] = Float.parseFloat(s[i]);

        return v;
    }
}




