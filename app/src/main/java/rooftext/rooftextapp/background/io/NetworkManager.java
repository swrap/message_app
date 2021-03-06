package rooftext.rooftextapp.background.io;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import rooftext.rooftextapp.background.BackgroundManager;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 6/22/2016.
 */
public class NetworkManager extends BroadcastReceiver{
    private static NetworkManager networkManager;

    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private BackgroundManager backgroundManager;
    private Context context;

    //wifi
    private int WIFI_MINIMUM_MBPS = 1;
    private boolean wifiMimimumSpeedEnable = false;
    //mobile
    private int MOBLIE_MIMIMUM_KBPS = 100;
    private boolean mobileMimimumSpeedEnable = false;

    private NetworkManager(BackgroundManager backgroundManager) {
        super();
        connectivityManager = (ConnectivityManager) backgroundManager.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) backgroundManager.getSystemService(Context.WIFI_SERVICE);
        this.backgroundManager = backgroundManager;
        this.context = backgroundManager.getApplicationContext();
    }

    public static NetworkManager getInstance(BackgroundManager backgroundManager) {
        if (networkManager == null) {
            networkManager = new NetworkManager(backgroundManager);
        }
        return networkManager;
    }

    public static NetworkManager getInstance() {
        return networkManager;
    }

    public boolean isConnectedTypeWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network [] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
//            Log.d(Tag.NETWORK_MANAGER, "Type [" + networkInfo.getType() + "] " + ConnectivityManager.TYPE_WIFI );
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.d(Tag.NETWORK_MANAGER, "networkInfo [" + networkInfo + "], isconnected [" + networkInfo.isConnectedOrConnecting() + "] getType [" + networkInfo.getType() + "], wifi type [" + ConnectivityManager.TYPE_WIFI + "], all ["
                        + (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) + "]");
                if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isConnectedTypeMobile() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    public boolean isConnected() {
        if (isConnectedTypeWifi()) {
            return true;
        }
        return false;
    }

    public boolean canConnectBackgroundService() {
        //TODO: Implement this damn code to check for conditions
        return isConnected();
    }

    public boolean canSendMedia(int type, int subType){
        if(type == ConnectivityManager.TYPE_WIFI){
            WifiInfo  wifiInfo = wifiManager.getConnectionInfo();
            if(wifiMimimumSpeedEnable && wifiInfo.getLinkSpeed() < WIFI_MINIMUM_MBPS) {
                return false;
            }
            return true;
        }else if(type == ConnectivityManager.TYPE_MOBILE){
            switch(subType){
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d(Tag.NETWORK_MANAGER, "Have Wifi Connection");
            Intent webIntent = new Intent(Tag.ACTION_LOCAL_NETWORK_CHANGE);
            //add empty state
            LocalBroadcastManager.getInstance(context).sendBroadcast(webIntent);
        } else {
            Log.d(Tag.NETWORK_MANAGER, "Don't have Wifi Connection");
        }
    }
}
