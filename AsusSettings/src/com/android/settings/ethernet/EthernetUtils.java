/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.ethernet;

import android.content.ContentResolver;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.provider.Settings;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

/**
 * The class is from Asus usb dialog patch.
 * The methods here are exactly same with ${@link EthUtils}.
 */
public class EthernetUtils {
    private static final String TAG = "EthernetUtils";

    public static Inet4Address strToInet4Address(String ip) {
        if ((ip == null) || (ip.length() == 0)) {
            return null;
        }
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(ip);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    public static String inet4AddressToString(Inet4Address ipv4) {
        if (ipv4 == null) {
            return null;
        }
        return ipv4.getHostAddress();
    }

    public static boolean validateIpConfigFields(String ip) {
        if ((ip == null) || (ip.length() == 0)) {
            return false;
        }
        Inet4Address inetAddr = strToInet4Address(ip);
        if (inetAddr == null) {
            return false;
        }
        return true;
    }

    public static boolean validateNetPrefixConfigFields(String prefixLength) {
        int networkPrefixLength = -1;
        if ((prefixLength == null) || (prefixLength.length() == 0)) {
            return false;
        }
        try {
            networkPrefixLength = Integer.parseInt(prefixLength);
        } catch (NumberFormatException e) {
            // Use -1
        }
        if (networkPrefixLength < 0 || networkPrefixLength > 32) {
            return false;
        } else {
            return true;
        }
    }

    public static String getEthernetIpAddresses(ConnectivityManager cm) {
        LinkProperties prop = cm.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        if (prop == null) return null;
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        // If there are no entries, return null
        if (!iter.hasNext()) return null;
        // Concatenate all available addresses, comma separated
        String addresses = "";
        while (iter.hasNext()) {
            addresses += iter.next().getHostAddress();
            if (iter.hasNext()) addresses += "\n";
        }
        return addresses;
    }
}
