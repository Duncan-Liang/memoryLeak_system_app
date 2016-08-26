package com.android.settings.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Color;

public class ColorTint {
    private static HashMap<String, Integer> sColorMap = new HashMap<String, Integer>();
    private static ArrayList<Integer> sColorList = new ArrayList<Integer>();
    public static final int DEFAULT_COLOR = Color.parseColor("#eda00b");
    public static int sDefaultColorValue = 0;

    public ColorTint(XmlPullParser parser) {
        parseColorValueXml(parser);
    }

    public void parseColorValueXml(XmlPullParser parser) {
        try {
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("colormask")) {
                        String colorName = parser.getAttributeValue(null, "name");
                        String colorString = parser.nextText();
                        sColorMap.put(colorName, Color.parseColor(colorString));
                        sColorList.add(Color.parseColor(colorString));
                    }
                }
                parser.next();
            }
        } catch (XmlPullParserException e) {
        } catch (IOException e) {
        }
    }

    public static ArrayList<Integer> getColorList() {
        return sColorList;
    }

    public static int getDefaultColor(String name) {
        if (sDefaultColorValue != 0) {
            return sDefaultColorValue;
        }
        return (sColorMap.get(name) != null) ? sColorMap.get(name) : DEFAULT_COLOR;
    }

    public static void updateColor(int color, int index) {
        sColorList.set(index, color);
    }
}
