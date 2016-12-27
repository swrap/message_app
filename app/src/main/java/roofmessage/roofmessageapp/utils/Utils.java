package roofmessage.roofmessageapp.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
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

    public static void loadIpStringNot() { //TODO remove from release
        Properties properties = new Properties();
        File file = new File(Environment.getExternalStorageDirectory(), "/RoofText");
        boolean created = false;
        Log.e(Tag.UTILS, "Directory exists [" + file.exists() + "]");
        if (!file.exists()) {
            try {
                created = file.mkdir();
            } catch (Exception e) {
                Log.e(Tag.UTILS, "Could not create directory. ", e);
            }
        }
        if (!created) {
            Log.e(Tag.UTILS, "Could not create directory.");
        }
        file = new File(file.getPath(), "/config.properties");
        Log.d(Tag.UTILS, "Checking if file exists [" + file.exists() + "] [" + file.getPath() + "]");
        if (!file.exists()) {
            OutputStream outputStream = null;
            try {
                file.createNewFile();
                outputStream = new FileOutputStream(file);
                properties.setProperty(KEY,Tag.BASE_URL);
                properties.store(outputStream,"IP used to connect");
            } catch (IOException e) {
                Log.e(Tag.UTILS, "Could not create file.", e);
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                InputStream inputStream = new FileInputStream(file);
                properties.load(inputStream);
//                Tag.BASE_URL = properties.getProperty(KEY);
            } catch (FileNotFoundException e) {
                Log.d(Tag.UTILS, "Could not load config file.");
            } catch (IOException e) {
                Log.d(Tag.UTILS, "Could not load config file io exception.");
            }
        }
    }
}
