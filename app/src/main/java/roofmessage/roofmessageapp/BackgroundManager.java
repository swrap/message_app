package roofmessage.roofmessageapp;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocketState;

import roofmessage.roofmessageapp.activity.MainActivity;
import roofmessage.roofmessageapp.dataquery.QueryManager;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.io.NetworkManager;
import roofmessage.roofmessageapp.io.SessionManager;
import roofmessage.roofmessageapp.io.SharedPreferenceManager;
import roofmessage.roofmessageapp.io.WebSocketManager;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/17/2016.
 */
public class BackgroundManager extends Service{

    private static BackgroundManager backgroundManager;

    public static final String LOGIN_START = "LOGIN START";
    private int sleep_time = 5000;

    private QueryManager queryManager = null;
    private NetworkManager networkManager = null;
    private WebSocketManager webSocketManager = null;
    private static SharedPreferenceManager sharedPreferenceManager = null;
    private static SessionManager sessionManager = null;

    private static boolean running = false;

    public BackgroundManager() {}

    public static BackgroundManager getInstance() {
        if (backgroundManager == null) {
            backgroundManager = new BackgroundManager();
        }
        return backgroundManager;
    }

    @Override
    public void onCreate() {
        queryManager = QueryManager.getInstance(this);
        if(!queryManager.isAlive()){
            queryManager.start();
        }
        networkManager = NetworkManager.getInstance(this);
        webSocketManager = WebSocketManager.getInstance(this);
        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        sessionManager = SessionManager.getInstance(this);
        running = true;
        Log.d(Tag.BACKGROUND_MANAGER, "Background service has been created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Tag.BACKGROUND_MANAGER, "Background onStartCommand. Received start id " + startId + ": " + intent);
        if (intent == null && sharedPreferenceManager.getBackgroundState()) {
            Log.d(Tag.BACKGROUND_MANAGER, "Starting login worker threads");
            UserLoginTask mAuthTask = new UserLoginTask(sharedPreferenceManager.getUsername(),
                    sharedPreferenceManager.getPassword());
            mAuthTask.execute();
        } else if (intent == null && !sharedPreferenceManager.getBackgroundState()) {
            Log.d(Tag.BACKGROUND_MANAGER, "SharedPreferences background is false. And started after app closed. Stopping service.");
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public void onDestroy() {
        running = false;
        sharedPreferenceManager.saveBackgroundState(false);
        Log.d(Tag.BACKGROUND_MANAGER, "KILLED SERVICE");
        super.onDestroy();
    }

    public boolean isRunning() {
        return running;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void checkWebsocket() {
        if (sharedPreferenceManager.getBackgroundState()) {
            final String username = sharedPreferenceManager.getSessionUsername();
            final String password = sharedPreferenceManager.getSessionUsername();
            if (sessionManager.login(username, password)) {
                if (webSocketManager.getState() != WebSocketState.OPEN ||
                        webSocketManager.getState() == WebSocketState.CONNECTING) {
                    webSocketManager.createConnection();
                }
            } else {
                UserLoginTask mAuthTask = new UserLoginTask(username, password);
                mAuthTask.execute();
            }
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            sessionManager = SessionManager.getInstance(BackgroundManager.this);
            boolean retval = false;
            if (networkManager.canConnectBackgroundService()) {
                //we have internet
                retval = sessionManager.login(mUsername, mPassword);
                if (retval) {
                    //we can login to django
                    WebSocketManager webSocketController = WebSocketManager.getInstance(BackgroundManager.this);
                    retval = webSocketController.createConnection();
                }
                if (!retval) {
                    //we can login but not create a connection
                    Log.e(Tag.LOGIN_ACTIVITY, "Should not reach here. Can login, cannot create channel.");
                    sessionManager.logout(mUsername, mPassword);
                }
            } else {
                Log.d(Tag.BACKGROUND_MANAGER, "Background service refused by network manager.");
            }
            return retval;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Log.d(Tag.BACKGROUND_MANAGER, "Connected! Stopped trying to connect.");
            } else {
                try {
                    Log.d(Tag.BACKGROUND_MANAGER, "Could not connect, starting new worker thread.");
                    Thread.sleep(sleep_time);
                    UserLoginTask mAuthTask = new UserLoginTask(sharedPreferenceManager.getUsername(),
                            sharedPreferenceManager.getPassword());
                    mAuthTask.execute();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onCancelled() {
            Log.d(Tag.BACKGROUND_MANAGER, "UserLogin Cancelled? We don't know why.");
        }
    }
}