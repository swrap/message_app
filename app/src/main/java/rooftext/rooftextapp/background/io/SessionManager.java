package rooftext.rooftextapp.background.io;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.ssl.HttpsURLConnection;

import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class SessionManager {
    private static SessionManager sessionManager;
    private Context context;

    private static final CookieManager cookieManager = new CookieManager();

    private String VERSION_URL = (Tag.LOCAL_HOST ? "http://" : "https://") + Tag.BASE_URL + "/android_version/";
    private String LOGOUT_URL = (Tag.LOCAL_HOST ? "http://" : "https://") + Tag.BASE_URL + "/android_logout/";
    private String LOGIN_URL = (Tag.LOCAL_HOST ? "http://" : "https://") + Tag.BASE_URL + "/android_login/";
    private final String CSRF_MID_TOKEN = "csrfmiddlewaretoken";
    private final String CSRF_TOKEN = "csrftoken";
    private final int RESPONSE_OKAY = 200;
    private final int connection_timeout = 4000;
    private String version;
    private int loginResponseCode = -1;

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

    public boolean correctVersion() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(VERSION_URL);
            if (Tag.LOCAL_HOST) {
                connection = (HttpURLConnection) url.openConnection();
            } else {
                connection = (HttpsURLConnection) url.openConnection();
            }
            connection.setConnectTimeout(connection_timeout);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String content = in.readLine();
            Log.d(Tag.SESSION_MANAGER, "content [" + content + "] Tag.Version [" + Tag.VERSION + "]");
            if(content != null && content.equals(Tag.VERSION)) {
                return true;
            }
            version = content;
            Log.d(Tag.SESSION_MANAGER, "INCORRECT VERSION [" + version + "]");
        } catch (MalformedURLException e) {
            Log.d(Tag.SESSION_MANAGER, "Unable to check version URL Exception", e);
        } catch (IOException e) {
            Log.d(Tag.SESSION_MANAGER, "Unable to check version IO EXCEPTION", e);
            //important used in callback of background manager
            version = null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return false;
    }

    public String getVersion() {
        return version;
    }

    public int getLoginResponseCode() {
        return loginResponseCode;
    }

    public boolean login(String username, String password) {
        loginResponseCode = -1;
        try {
            // get content from URLConnection;
            // cookies are set by web site //TODO Fix loading in dynamic url
            Log.d(Tag.SESSION_MANAGER, "Before attempting to login. [" + LOGIN_URL + "]");
            URL url = new URL(LOGIN_URL);
            HttpURLConnection connection;
            if (Tag.LOCAL_HOST) {
                connection = (HttpURLConnection) url.openConnection();
            } else {
                connection = (HttpsURLConnection) url.openConnection();
            }
            connection.setConnectTimeout(connection_timeout);
            connection.getContent();
            String COOKIES_HEADER = "Set-Cookie";
            java.net.CookieManager msCookieManager = new java.net.CookieManager();

            Map<String, List<String>> headerFields = connection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

            if (cookiesHeader != null) {
                for (String cookie : cookiesHeader) {
                    msCookieManager.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
                }
            }
            int responseCode = connection.getResponseCode();
            Log.d(Tag.SESSION_MANAGER, "Response code on get login [" + responseCode + "]");
            if (responseCode == 200) {
                String csrf = getCSRFToken();

                url = new URL(LOGIN_URL);
                Log.d(Tag.SESSION_MANAGER, "Attempting login to [" + LOGIN_URL + "]");
                if (Tag.LOCAL_HOST) {
                    connection = (HttpURLConnection) url.openConnection();
                } else {
                    connection = (HttpsURLConnection) url.openConnection();
                }
//                if (msCookieManager.getCookieStore().getCookies().size() > 0) {
                    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
//                    Log.e(Tag.SESSION_MANAGER, TextUtils.join(";",  msCookieManager.getCookieStore().getCookies()));
//                    connection.setRequestProperty("Cookie",
//                            TextUtils.join(";",  msCookieManager.getCookieStore().getCookies()));
//                    connection.setRequestProperty("X-CSRFToken",
//                            TextUtils.join(";",  msCookieManager.getCookieStore().getCookies()));
//                }
//                connection.setRequestProperty("X-CSRFToken","csrfmiddlewaretoken:"+csrf);
//                connection.setRequestProperty("Cookie", "csrfmiddlewaretoken:"+csrf);
//                connection.setRequestProperty("HTTP_X_CSRFTOKEN", "csrfmiddlewaretoken:"+csrf);
                connection.setRequestProperty("Referer", "https://rooftext.com");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                String formParameters = CSRF_MID_TOKEN + "=" + csrf
                        + "&" + CSRF_TOKEN + "=" + csrf
                        + "&username=" + username
                        + "&password=" + password;
                System.out.println("OUTPUT [" + formParameters + "]");
                out.write(formParameters);
                out.close();
                Log.d(Tag.SESSION_MANAGER, "Response on post: " + connection.getResponseCode());
                if ((loginResponseCode = connection.getResponseCode()) == RESPONSE_OKAY) {
                    return true;
                } else {
                    Log.d(Tag.SESSION_MANAGER, "Response reason for failing: " + connection.getResponseMessage());
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

    public void logout(final String username, final String password) {
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // get content from URLConnection;
                    // cookies are set by web site
                    URL url = new URL(LOGOUT_URL);
                    HttpURLConnection connection;
                    if (Tag.LOCAL_HOST) {
                        connection = (HttpURLConnection) url.openConnection();
                    } else {
                        connection = (HttpsURLConnection) url.openConnection();
                    }
                    connection.setConnectTimeout(connection_timeout);
                    connection.getContent();
                    int responseCode = connection.getResponseCode();
                    Log.d(Tag.SESSION_MANAGER, "Response code on get logout [" + responseCode + "]");
                    if (responseCode == 200) {
                        String csrf = getCSRFToken();

                        url = new URL(LOGOUT_URL);
                        if (Tag.LOCAL_HOST) {
                            connection = (HttpURLConnection) url.openConnection();
                        } else {
                            connection = (HttpsURLConnection) url.openConnection();
                        }
                        connection.setRequestProperty("Referer", "https://rooftext.com");
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
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
