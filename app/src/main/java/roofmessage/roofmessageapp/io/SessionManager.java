package roofmessage.roofmessageapp.io;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import roofmessage.roofmessageapp.activity.LoginActivity;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class SessionManager {
    private static SessionManager sessionManager;

    private static final CookieManager cookieManager = new CookieManager();

    private final String LOGIN_URL = "http://" + Tag.BASE_URL + "/android_login/";
    private final String LOGOUT_URL = "http://" + Tag.BASE_URL + "/android_logout/";
    private final String CSRF_MID_TOKEN = "csrfmiddlewaretoken";
    private final String CSRF_TOKEN = "csrftoken";
    private final int RESPONSE_OKAY = 200;

    private SessionManager() {}

    public static SessionManager getInstance() {
        Log.e(Tag.SESSION_MANAGER, "IN");
        if(sessionManager == null) {
            sessionManager = new SessionManager();
            // Instantiate CookieManager;
            // make sure to set CookiePolicy
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(cookieManager);
        }

        Log.e(Tag.SESSION_MANAGER, "OUT");

        return sessionManager;
    }

    public boolean login(String username, String password) {
        try {
            // get content from URLConnection;
            // cookies are set by web site
            URL url = new URL(LOGIN_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getContent();

            // get cookies from underlying
            // CookieStore
            CookieStore cookieStore = cookieManager.getCookieStore();
            List<HttpCookie> cookies = cookieStore.getCookies();
            String csrf = "";
            for ( HttpCookie cookie : cookies ) {
                if (cookie.getName().equals(CSRF_TOKEN)) {
                    csrf = cookie.getValue();
                }
            }

            url = new URL(LOGIN_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStreamWriter out = new OutputStreamWriter( connection.getOutputStream() );
            String formParameters = CSRF_MID_TOKEN + "=" + csrf
                    + "&username=" +  username
                    + "&password=" + password;
            out.write(formParameters);
            out.close();
            Log.d(Tag.SESSION_MANAGER, "Response: " + connection.getResponseCode());
            if(connection.getResponseCode() == RESPONSE_OKAY) {
                WebSocketManager webSocketController = WebSocketManager.getInstance();
                return webSocketController.createConnection();
            }
        } catch (Exception e) {
            System.out.println("Unable to get cookie using CookieHandler");
            e.printStackTrace();
        }
        return false;
    }

    public CookieManager getCookieManager(){
        return cookieManager;
    }
}
