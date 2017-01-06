package rooftext.rooftextapp.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class Utils {

    private static String KEY = "ip";

    public static String convertMMStoSMSDate(String date) {
        String s = date + "000";
        return s;
    }

    public static String convertSMStoMMSDate(String date) {
        if(date.length() > 2) {
            date = date.substring(0,date.length()-3);
        }
        return date;
    }
}
