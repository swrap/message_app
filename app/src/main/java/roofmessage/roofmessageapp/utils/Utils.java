package roofmessage.roofmessageapp.utils;

import android.util.Log;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class Utils {

    public static String convertMMStoSMSDate(String date) {
        String s = date + "000";
        return s;
    }

    public static String convertSMStoMMSDate(String date) {
        return date.substring(0,date.length()-3);
    }
}
