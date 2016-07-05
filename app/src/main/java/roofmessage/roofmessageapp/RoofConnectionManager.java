package roofmessage.roofmessageapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Jesse Saran on 6/22/2016.
 */
public class RoofConnectionManager extends AsyncTask<String, Void, String> {

    private NetworkInfo networkInfo;
    private static final String ROOF_URL = "";
    private ConnectivityManager connectivityManager;

    public RoofConnectionManager(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();
    }

    @Override
    protected void onPreExecute() {
        if (networkInfo != null && networkInfo.isConnected()) {

        }
    }

    @Override
    protected String doInBackground(String... strings) {
        return null;
    }
}
