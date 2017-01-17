package rooftext.rooftextapp.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import rooftext.rooftextapp.Flush;
import rooftext.rooftextapp.R;
import rooftext.rooftextapp.activity.LoginActivity;
import rooftext.rooftextapp.background.request.MessageObservers.SMSObserver;
import rooftext.rooftextapp.background.request.RequestManager;
import rooftext.rooftextapp.background.io.NetworkManager;
import rooftext.rooftextapp.background.io.SessionManager;
import rooftext.rooftextapp.background.io.SharedPreferenceManager;
import rooftext.rooftextapp.background.io.WebSocketManager;
import rooftext.rooftextapp.io.BindListener;
import rooftext.rooftextapp.io.JSONBuilder;
import rooftext.rooftextapp.utils.PermissionsManager;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/17/2016.
 */
public class BackgroundManager extends Service implements Flush {

    private static BackgroundManager backgroundManager;

    public static final String LOGIN_START = "LOGIN START";
    private int sleep_time = 5000;
    private boolean allowLogin = false;

    private RequestManager requestManager = null;
    private NetworkManager networkManager = null;
    private WebSocketManager webSocketManager = null;
    private SMSObserver smsObserver = null;
    private SharedPreferenceManager sharedPreferenceManager = null;
    private SessionManager sessionManager = null;
    private UserLoginTask mAuthTask;
    private boolean foreground = false;

    private ConnectionReciever connectionReciever;

    public BackgroundManager() {}

    public static BackgroundManager getInstance() {
        if (backgroundManager == null) {
            backgroundManager = new BackgroundManager();
        }
        return backgroundManager;
    }

    @Override
    public void onCreate() {
        Log.e(Tag.BACKGROUND_MANAGER,"STARTING BACKMAN");
        //update version
        PackageInfo pInfo = null;
        try {
            pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            Tag.VERSION = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Tag.BACKGROUND_MANAGER, "Could not update version information.");
        }


        //check if has permissions, shutdown if not
        if (!PermissionsManager.hasAllPermissions(this)) {
            //TODO add notification and kill app if loses permission while running in background
        }
        smsObserver = SMSObserver.getInstance(this);
        if (smsObserver != null) {
            smsObserver.start();
        }
        requestManager = RequestManager.getInstance(this);
        if(!requestManager.isAlive() || (requestManager.getState() == Thread.State.NEW)){
            requestManager.start();
        }

        //network manager to receive intents
        networkManager = NetworkManager.getInstance(BackgroundManager.this);
        IntentFilter filters = new IntentFilter();
        filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        super.registerReceiver(networkManager, filters);

        //websocket intent setup
        webSocketManager = WebSocketManager.getInstance(this);
        IntentFilter webSocketManagerIntentFilter = new IntentFilter();
        webSocketManagerIntentFilter.addAction(Tag.ACTION_LOCAL_SEND_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(webSocketManager, webSocketManagerIntentFilter);

        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        sessionManager = SessionManager.getInstance(this);

        //connection receiver for intent
        connectionReciever = new ConnectionReciever();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Tag.ACTION_LOCAL_WEBSOC_CHANGE);
        intentFilter.addAction(Tag.ACTION_LOCAL_NETWORK_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionReciever,intentFilter);

        Log.d(Tag.BACKGROUND_MANAGER, "Background service has been created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;// START_STICKY; //TODO TURN OFF FOR MATT TESTING
    }

    @Override
    public boolean flush() {
        Log.d(Tag.BACKGROUND_MANAGER, "Flushing now.");
        boolean retval = false;
        if (webSocketManager != null) {
            retval = webSocketManager.flush();
        } else {
            Log.d(Tag.BACKGROUND_MANAGER, "Websocket manager is null");
        }
        if (networkManager != null) {
            super.unregisterReceiver(networkManager);
        } else {
            Log.d(Tag.BACKGROUND_MANAGER, "Network manager is null");
        }
        if (smsObserver != null) {
            smsObserver.flush();
        } else {
            Log.d(Tag.BACKGROUND_MANAGER, "SMSObserver manager is null");
        }
        Log.d(Tag.BACKGROUND_MANAGER, "Flush done.");
        return retval;
    }

    public void attemptThreadLogin(Messenger replyTo) {
        Log.d(Tag.BACKGROUND_MANAGER, "Attempting thread login, replyTo [" + replyTo + "]");
        String username = sharedPreferenceManager.getSessionUsername();
        String password = sharedPreferenceManager.getSessionPassword();
        if(webSocketManager != null && webSocketManager.getState().equals(WebSocketState.CLOSED)) {
            if (!username.isEmpty() && !password.isEmpty()) {
                if (mAuthTask == null || mAuthTask.getState().equals(Thread.State.TERMINATED)) {
                    if(mAuthTask != null) {
                        Log.d(Tag.BACKGROUND_MANAGER, "Login thread state [" + mAuthTask.getState() + "]");
                    }
                    mAuthTask = new UserLoginTask(username,password,replyTo);
                }
                if (mAuthTask.getState().equals(Thread.State.TIMED_WAITING)) {
                    Log.d(Tag.BACKGROUND_MANAGER, "Thread state is hung in timed waiting and we interrupt!");
                    mAuthTask.interrupt();
                    mAuthTask = new UserLoginTask(username,password,replyTo);
                }
                if (mAuthTask.getState().equals(Thread.State.NEW)) {
                    mAuthTask.start();
                }
            } else {
                Log.d(Tag.BACKGROUND_MANAGER, "user pass values are empty.");
            }
        } else {
            Log.d(Tag.BACKGROUND_MANAGER, "Background manager is logged in, cancelling attempt to connect.");
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends Thread {

        private final String mUsername;
        private final String mPassword;
        private final Messenger replyTo;
        private int error = BindListener.ERROR_NO_WIFI_ERROR;

        /**
         * NOTE WILL NOT when replyto is not null with only attempt to connect once
         */
        UserLoginTask(String username, String password, Messenger replyTo) {
            this.mUsername = username;
            this.mPassword = password;
            this.replyTo = replyTo;
        }

        public void run() {
            boolean retval = false;
            Log.d(Tag.BACKGROUND_MANAGER, "Attempting login.");
            while(!retval) {
                Log.d(Tag.BACKGROUND_MANAGER, "Updated base url to [" + Tag.BASE_URL + "]");
                if (sharedPreferenceManager.getSessionUsername().equals("") && sharedPreferenceManager.getSessionPassword().equals("")) {
                    Log.d(Tag.BACKGROUND_MANAGER, "PROBLEM PROBLEM PROBLEM Got into connection somehow without session values. exiting.");
                    break;
                } else {
                    retval = attemptLogin();
                    Log.d(Tag.BACKGROUND_MANAGER, "attempted login retval [" + retval + "], replyTo [" + replyTo + "]");

                    if(replyTo != null) {
                        //if reply to has something
                        Log.d(Tag.BACKGROUND_MANAGER, "Reply to is not null, quitting attempts to connect.");
                        //if could not login then save session user/pass as empty
                        sharedPreferenceManager.saveSessionUsernamePass("", "");
                        Log.d(Tag.BACKGROUND_MANAGER, "Setting up replyTo");
                        Message message = new Message();
                        message.what = MSG_ATTEMPT_LOGIN;
                        message.arg1 = retval ? 1 : 0;
                        if (!retval) {
                            message.arg2 = error;
                            if (sessionManager.getVersion() != null &&
                                    !sessionManager.getVersion().isEmpty() &&
                                    (error == BindListener.ERROR_INVALID_VERSION_PATCH ||
                                            error == BindListener.ERROR_INVALID_VERSION_MINOR ||
                                            error == BindListener.ERROR_INVALID_VERSION_MAJOR)) {
                                //error with version :(
                                Log.d(Tag.BACKGROUND_MANAGER, "PROBLEMS WITH WRONG VERSION");
                                switch (error) {
                                    case BindListener.ERROR_INVALID_VERSION_PATCH:
                                        showNotificationAboutVersion(String.format(BackgroundManager.this.getString(R.string.invalid_version_patch)
                                                .replaceAll("XX","%"), Tag.VERSION));
                                        break;
                                    case BindListener.ERROR_INVALID_VERSION_MINOR:
                                        showNotificationAboutVersion(String.format(BackgroundManager.this.getString(R.string.invalid_version_minor)
                                                .replaceAll("XX","%"), Tag.VERSION));
                                        break;
                                    case BindListener.ERROR_INVALID_VERSION_MAJOR:
                                        showNotificationAboutVersion(String.format(BackgroundManager.this.getString(R.string.invalid_version_major)
                                            .replaceAll("XX","%"), Tag.VERSION));
                                        BackgroundManager.this.stopSelf();
                                        break;
                                }
                            }
                        }
                        try {
                            Log.d(Tag.BACKGROUND_MANAGER, "Sending response in message about login [" + retval + "]");
                            replyTo.send(message);
                        } catch (RemoteException e) {
                            Log.d(Tag.BACKGROUND_MANAGER, "Could not send response back. Serious issue.");
                        }
                        break;
                    }
                    if (retval) {
                        //retval is true break
                        break;
                    } else {
                        //return val is false try again
                        Log.d(Tag.BACKGROUND_MANAGER, "Could not connect, sleeping for [" + sleep_time + "] millis.");
                        try {
                            sleep(sleep_time);
                        } catch (InterruptedException e) {
                            Log.e(Tag.BACKGROUND_MANAGER, "Interrupted sleep. BAD! Breaking", e);
                            break;
                        }
                    }
                }
            }

            if (retval && !foreground) {
                //really only need this in testing when restarting service but may cause bugs, but not known at the moment
                //if retval is true than must be logging in from main activity
                //start foreground service
                Intent notificationIntent = new Intent(BackgroundManager.this, LoginActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pIntentOpenApp = PendingIntent.getActivity(BackgroundManager.this,
                        0,
                        notificationIntent,
                        0);
                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(BackgroundManager.this)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle("RoofText")
                                .setContentText("We are running right! :)")
                                .setContentIntent(pIntentOpenApp);
                Notification notification = notificationBuilder.build();
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                startForeground(1500, notification);
                foreground = true;
                allowLogin = true;
            }

            Log.d(Tag.BACKGROUND_MANAGER, "RETVAL FUCK [" + retval + "]");
            //send change up state update
            Log.d(Tag.BACKGROUND_MANAGER, "Logged in successfully, sending update to ui.");
            Intent intent = new Intent(Tag.ACTION_WEBSOC_CHANGE);
            //TODO make state static variable
            intent.putExtra("state", webSocketManager.getLocalizeState());
            BackgroundManager.this.sendBroadcast(intent);
        }

        private void showNotificationAboutVersion(String message) {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(BackgroundManager.this)
                            //TODO ADD IMAGE FOR NOTIFICATION
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("RoofText: Invalid Version")
                            .setContentText("Update Version of App")
                            .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle()
                                    .bigText(message));
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(100, mBuilder.build());
        }

        private boolean attemptLogin() {
            boolean retval = false;
            Log.d(Tag.BACKGROUND_MANAGER, "attemptLogin, network manager[" + networkManager.canConnectBackgroundService() + "]");
            if (networkManager.canConnectBackgroundService()) {
                //we have internet
                if (sessionManager.correctVersion()) {
                    retval = sessionManager.login(mUsername, mPassword);
                    if (retval) {
                        //we can login to webserver
                        retval = webSocketManager.createConnection();
                        if (!retval) {
                            //we can login but not create a connection
                            Log.e(Tag.BACKGROUND_MANAGER, "Can login, but cannot connect socket.");
                            sessionManager.logout(mUsername, mPassword);
                        } else {
                            error = BindListener.ERROR_INVALID_WEBSOCKET_CONNECT;
                        }
                    } else {
                        //login error
                        error = BindListener.ERROR_INVALID_USER_PASS;
                    }
                } else {
                    //change error message based off of response
                    Log.d(Tag.BACKGROUND_MANAGER, "VERSIONS [" + Tag.VERSION + "] [" + sessionManager.getVersion() + "]");
                    String [] myVer = Tag.VERSION.split("\\.");
                    String [] serVer = sessionManager.getVersion().split("\\.");
                    Log.d(Tag.BACKGROUND_MANAGER, "myver [" + myVer.length + "] serVer [" + serVer.length + "]");
                    if (!myVer[0].equals(serVer[0])) {
                        //major version
                        error = BindListener.ERROR_INVALID_VERSION_MAJOR;
                    } else if (!myVer[1].equals(serVer[1])) {
                        //minor version
                        error = BindListener.ERROR_INVALID_VERSION_MINOR;
                    } else if (!myVer[2].equals(serVer[2])) {
                        //patch/bug version
                        error = BindListener.ERROR_INVALID_VERSION_PATCH;
                    } else {
                        //default if they do not match assume major error and update
                        error = BindListener.ERROR_INVALID_VERSION_MAJOR;
                    }
                }
            } else {
                error = BindListener.ERROR_NO_WIFI_ERROR;
            }
            Log.d(Tag.BACKGROUND_MANAGER, "response on login [" + retval + "]");
            return retval;
        }
    }

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_VALUE = 3;
    public static final int MSG_SAVE_USERNAME_PASS = 5;
    public static final int MSG_SAVE_SESSION_USERNAME_PASS = 6;
    public static final int MSG_SAVE_REMEMBERME = 7;
    public static final int MSG_ATTEMPT_LOGIN = 9;
    public static final int MSG_POST_WEBSOCKET_UPDATE = 11;
    public static final int MSG_LOGOUT_STOP_FOREGROUND = 12;

    public static final String KEY_USERNAME_PASS = "KEY_USERNAME_PASS";

    public static final String KEY_DISPLAY_MESSAGE = "KEY_DISPLAY_MESSAGE";

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String [] user_pass;
            Bundle bundle;
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SAVE_USERNAME_PASS:
                    bundle = msg.getData();
                    user_pass = bundle.getStringArray(KEY_USERNAME_PASS);
                    if (user_pass != null && user_pass.length > 1) {
                        sharedPreferenceManager.saveUserPass(user_pass[0], user_pass[1]);
                    }
                    break;
                case MSG_SAVE_SESSION_USERNAME_PASS:
                    bundle = msg.getData();
                    user_pass = bundle.getStringArray(KEY_USERNAME_PASS);
                    Log.d(Tag.BACKGROUND_MANAGER,"Saving session username/pass.");
                    if (user_pass != null && user_pass.length > 1) {
                        sharedPreferenceManager.saveSessionUsernamePass(user_pass[0], user_pass[1]);
                    }
                    break;
                case MSG_SAVE_REMEMBERME:
                    sharedPreferenceManager.saveRememberMe(msg.arg1 == 0 ? false : true);
                    break;
                case MSG_ATTEMPT_LOGIN:
                    Log.d(Tag.BACKGROUND_MANAGER,"MSG_ATTEMPT_LOGIN");
                    attemptThreadLogin(msg.replyTo);
                    break;
                case MSG_POST_WEBSOCKET_UPDATE:
                    if (webSocketManager.getState() == WebSocketState.CLOSED && allowLogin) {
                        //if the websocket is closed then try to reconnect
                        attemptThreadLogin(null);
                    }
                    Intent intent = new Intent(Tag.ACTION_WEBSOC_CHANGE);
                    intent.putExtra("state", webSocketManager.getLocalizeState());
                    Log.d(Tag.BACKGROUND_MANAGER, "POSTING STATE OF WEBSOCKET CHANGE. [" +
                            webSocketManager.getLocalizeState() + "]");
                    BackgroundManager.this.sendBroadcast(intent);
                    break;
                case MSG_LOGOUT_STOP_FOREGROUND:
                    Log.d(Tag.BACKGROUND_MANAGER,"Logging out of backMan.");
                    sessionManager.logout(sharedPreferenceManager.getSessionUsername(),
                            sharedPreferenceManager.getSessionPassword());
                    sharedPreferenceManager.saveSessionUsernamePass("", "");
                    webSocketManager.disconnect();
                    BackgroundManager.this.stopForeground(true);
                    foreground = false;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
        Log.d(Tag.BACKGROUND_MANAGER, "Destroying now.");
        this.unregisterReceiver(networkManager);
//        this.unregisterReceiver(webSocketManager);
//        this.unregisterReceiver(connectionReciever);
        Log.d(Tag.BACKGROUND_MANAGER, "Destroying done.");
        super.onDestroy();
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private class ConnectionReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Tag.ACTION_LOCAL_WEBSOC_CHANGE)) {
                final String state = intent.getStringExtra("state");
                Log.d(Tag.BACKGROUND_MANAGER, "State changed receiving broadcast. State [" + state + "], " +
                        "webSocketState [" + webSocketManager.localizeState(WebSocketState.CLOSED) + "]");
                if (state.equals(webSocketManager.localizeState(WebSocketState.CLOSED)) && allowLogin) {
                    Log.d(Tag.BACKGROUND_MANAGER, "Received websocket change state is [" + webSocketManager.getLocalizeState() + "]");
                    attemptThreadLogin(null);
                }
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase(),
                            JSONBuilder.Action.DISCONNECTED.name().toLowerCase());
                } catch (JSONException e) {
                    Log.e(Tag.BACKGROUND_MANAGER, "Could not create cancelled action to send.");
                }
                webSocketManager.sendString(jsonObject.toString());
            } else if (action.equals(Tag.ACTION_LOCAL_NETWORK_CHANGE) && allowLogin) {
                Log.d(Tag.BACKGROUND_MANAGER, "Local network change detected. Asking for new attemptThreadLogin");
                attemptThreadLogin(null);
            }
        }
    }
}