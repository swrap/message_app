package roofmessage.roofmessageapp.background.io;

import android.content.Context;
import android.util.Log;

import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class SessionManager {
    private static SessionManager sessionManager;
    private Context context;

    private static final CookieManager cookieManager = new CookieManager();

    private final String LOGIN_URL = "http://" + Tag.BASE_URL + "/android_login/";
    private final String LOGOUT_URL = "http://" + Tag.BASE_URL + "/android_logout/";
    private final String CSRF_MID_TOKEN = "csrfmiddlewaretoken";
    private final String CSRF_TOKEN = "csrftoken";
    private final int RESPONSE_OKAY = 200;
    private final int connection_timeout = 4000;

    private SessionManager(Context context) {
        this.context = context;
    }

    public static SessionManager getInstance(Context context) {
        if(sessionManager == null) {
            sessionManager = new SessionManager(context);
            // Instantiate CookieManager;
            // make sure to set CookiePolicy
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(cookieManager);
        }

        return sessionManager;
    }

    public boolean login(String username, String password) {
        try {
            // get content from URLConnection;
            // cookies are set by web site
            Log.d(Tag.SESSION_MANAGER, "Before attempting to login.");
            URL url = new URL(LOGIN_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(connection_timeout);
            connection.getContent();
            int responseCode = connection.getResponseCode();
            Log.d(Tag.SESSION_MANAGER, "Response code on get login [" + responseCode + "]");
            if (responseCode == 200) {
                String csrf = getCSRFToken();

                url = new URL(LOGIN_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                String formParameters = CSRF_MID_TOKEN + "=" + csrf
                        + "&username=" + username
                        + "&password=" + password;
                out.write(formParameters);
                out.close();
                Log.d(Tag.SESSION_MANAGER, "Response: " + connection.getResponseCode());
                if (connection.getResponseCode() == RESPONSE_OKAY) {
                    return true;
                }
            }
        } catch (SocketTimeoutException e) {
            Log.d(Tag.SESSION_MANAGER, "Login waited [" + connection_timeout + "] to connect but failed.");
        } catch (ConnectException e) {
            Log.d(Tag.SESSION_MANAGER, "Unable to connect to host.", e);
        } catch (Exception e) {
            Log.e(Tag.SESSION_MANAGER, "Failed for some reason.", e);
            e.printStackTrace();
        }
        return false;
    }

    public CookieManager getCookieManager(){
        return cookieManager;
    }

    public boolean logout(String username, String password) {
        try {
            // get content from URLConnection;
            // cookies are set by web site
            URL url = new URL(LOGOUT_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(connection_timeout);
            connection.getContent();
            int responseCode = connection.getResponseCode();
            Log.d(Tag.SESSION_MANAGER, "Response code on get logout [" + responseCode + "]");
            if (responseCode == 200) {
                String csrf = getCSRFToken();

                url = new URL(LOGOUT_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                String formParameters = CSRF_MID_TOKEN + "=" + csrf
                        + "&username=" + username
                        + "&password=" + password;
                out.write(formParameters);
                out.close();
                Log.d(Tag.SESSION_MANAGER, "Response: " + connection.getResponseCode());
                if (connection.getResponseCode() == RESPONSE_OKAY) {
                    return true;
                }
            }
        } catch (SocketTimeoutException e) {
            Log.d(Tag.SESSION_MANAGER, "Logout waited [" + connection_timeout + "] to connect but failed.");
        } catch (ConnectException e) {
            Log.d(Tag.SESSION_MANAGER, "Unable to connect to host.", e);
        } catch (Exception e) {
            Log.d(Tag.SESSION_MANAGER, "Failed for some reason.", e);
            e.printStackTrace();
        }
        return false;
    }

    private String getCSRFToken() {
        CookieStore cookieStore = cookieManager.getCookieStore();
        List<HttpCookie> cookies = cookieStore.getCookies();
        String csrf = "";
        for ( HttpCookie cookie : cookies ) {
            if (cookie.getName().equals(CSRF_TOKEN)) {
                csrf = cookie.getValue();
            }
        }
        return csrf;
    }
}
