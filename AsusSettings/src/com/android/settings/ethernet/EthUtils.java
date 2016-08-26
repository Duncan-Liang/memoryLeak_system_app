package com.android.settings.ethernet;

import android.content.ContentResolver;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.provider.Settings;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

public class EthUtils {
    private static final String TAG = "EthUtils";

    public static final int ETHERNET_UNKNOWN = -1;
    public static final int ETHERNET_DISABLED = 0;
    public static final int ETHERNET_ENABLED = 1;

    public static final int ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND = 0;
    public static final int ETHERNET_POLICY_KEEP_ON_WHEN_SUSPEND = 1;

    public static boolean setDatabase(ContentResolver cr, String Key, String value) {
        int tryCount = 10;
        while (Settings.System.getString(cr, Key) == null ||
                !Settings.System.getString(cr, Key).equals(value)) {
            Log.d(TAG, "try setDataBase:" + Key + ",count:" + tryCount);
            Settings.System.putString(cr, Key, value);
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
            if (--tryCount == 0) {
                return false;
            }
        }
        return true;
    }

    public static int getDatabase(ContentResolver cr, String Key, int dafultValue) {
        return Settings.System.getInt(cr, Key, dafultValue);
    }

    public static boolean setDatabase(ContentResolver cr, String Key, int value) {
        int tryCount = 10;
        while (Settings.System.getInt(cr, Key, -1) != value) {
            Log.d(TAG, "try setDataBase:" + Key + ",count:" + tryCount);
            Settings.System.putInt(cr, Key, value);
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
            if (--tryCount == 0) {
                return false;
            }
        }

        return true;
    }

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

    public static boolean isEthernetAvailable(EthernetManager em) {
        return ((em != null) && (em.isAvailable()));
    }

    // Old checking mechanism
    // private static final String ETHERNET_IFACE_PATH =  "/sys/class/net";
    // public static boolean isEthernetAvailable() {
    //     File path = new File(ETHERNET_IFACE_PATH);
    //     String [] ifaceList = path.list();
    //     for (String iface : ifaceList) {
    //         if (iface.matches("eth\\d")) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    public static boolean isEthernetEnabled(ContentResolver cr) {
        int value = Settings.System.getInt(cr, Settings.System.ETHERNET_ENABLE, ETHERNET_UNKNOWN);
        if (value == ETHERNET_UNKNOWN) {
            Settings.System.putInt(cr, Settings.System.ETHERNET_ENABLE, ETHERNET_ENABLED);
        }
        return (Settings.System.getInt(cr, Settings.System.ETHERNET_ENABLE, ETHERNET_UNKNOWN) == ETHERNET_ENABLED);
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