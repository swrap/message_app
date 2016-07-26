package roofmessage.roofmessageapp.background;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocketState;

import java.util.ArrayList;

import roofmessage.roofmessageapp.Flush;
import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.background.request.MessageObservers.SMSObserver;
import roofmessage.roofmessageapp.background.request.RequestManager;
import roofmessage.roofmessageapp.background.io.NetworkManager;
import roofmessage.roofmessageapp.background.io.SessionManager;
import roofmessage.roofmessageapp.background.io.SharedPreferenceManager;
import roofmessage.roofmessageapp.background.io.WebSocketManager;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.PermissionsManager;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/17/2016.
 */
public class BackgroundManager extends Service implements Flush {

    private static BackgroundManager backgroundManager;

    public static final String LOGIN_START = "LOGIN START";
    private int sleep_time = 5000;

    private RequestManager requestManager = null;
    private NetworkManager networkManager = null;
    private WebSocketManager webSocketManager = null;
    private SMSObserver smsObserver = null;
    private static SharedPreferenceManager sharedPreferenceManager = null;
    private static SessionManager sessionManager = null;
    private UserLoginTask mAuthTask;
    private final IntentFilter filters = new IntentFilter();

    private ConnectionReciever connectionReciever;

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
        Log.e(Tag.BACKGROUND_MANAGER,"STARTING BACKMAN");
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
        filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filters.addAction("android.net.wifi.STATE_CHANGE");
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
        intentFilter.addAction(Tag.ACTION_WEBSOC_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionReciever,intentFilter);

        running = true;
        Log.d(Tag.BACKGROUND_MANAGER, "Background service has been created. Backgroundstate[" + sharedPreferenceManager.getBackgroundState() + "]");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Tag.BACKGROUND_MANAGER, "Background onStartCommand. Received start id [" + startId + "] [" + intent + "] backgroundstate[" + sharedPreferenceManager.getBackgroundState() + "]");
        if (intent == null && sharedPreferenceManager.getBackgroundState() && networkManager.canConnectBackgroundService()) {
            Log.d(Tag.BACKGROUND_MANAGER, "Starting login worker threads");
            mAuthTask = new UserLoginTask(sharedPreferenceManager.getUsername(),
                    sharedPreferenceManager.getPassword(),
                    null /*replyTo*/);
            mAuthTask.start();
        } else if (intent == null && !sharedPreferenceManager.getBackgroundState()) {
            Log.d(Tag.BACKGROUND_MANAGER, "SharedPreferences background is false. And started after app closed. Stopping service.");
            stopSelf();
        }
        return START_STICKY;
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

    public boolean isRunning() {
        return running;
    }

    //// TODO: 7/20/2016 check all thread.state.???? to do .equals(

    public void attemptThreadLogin(Messenger replyTo) {
        Log.d(Tag.BACKGROUND_MANAGER, "Attempting thread login, replyTo [" + replyTo + "]");
        String username = sharedPreferenceManager.getSessionUsername();
        String password = sharedPreferenceManager.getSessionPassword();
        if (!username.isEmpty() && !password.isEmpty()) {
            if (mAuthTask == null || mAuthTask.getState().equals(Thread.State.TERMINATED)) {
                mAuthTask = new UserLoginTask(username,password,replyTo);
            }
            if (mAuthTask.getState().equals(Thread.State.NEW)) {
                mAuthTask.start();
            }
        } else {
            //TODO REMOVE
            Log.d(Tag.BACKGROUND_MANAGER, "Session values are empty.");
        }
        Log.d(Tag.BACKGROUND_MANAGER, "Background state is [" +
                sharedPreferenceManager.getBackgroundState() + "]");
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends Thread {

        private final String mUsername;
        private final String mPassword;
        private final Messenger replyTo;

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
                retval = attemptLogin();
                Log.d(Tag.BACKGROUND_MANAGER, "attempted login retval [" + retval + "], replyTo [" + replyTo + "]");
                if (replyTo != null) {
                    Log.d(Tag.BACKGROUND_MANAGER, "Reply to is not null. Breaking.");
                    break;
                }
                if (!retval) {
                    if (!networkManager.canConnectBackgroundService()) {
                        Log.d(Tag.BACKGROUND_MANAGER, "No internet service, halting attempt to connect.");
                        break;
                    } else {
                        if (sharedPreferenceManager.getSessionUsername().equals("") && sharedPreferenceManager.getSessionPassword().equals("")) {
                            Log.d(Tag.BACKGROUND_MANAGER, "Got into connection somehow without session values. exiting.");
                            break;
                        } else {
                            Log.d(Tag.BACKGROUND_MANAGER, "Could not connect, sleeping for [" + sleep_time + "] millis.");
                            try {
                                this.sleep(sleep_time);
                            } catch (InterruptedException e) {
                                Log.e(Tag.BACKGROUND_MANAGER, "Interrupted sleep. BAD!", e);
                            }
                        }
                    }
                }
            }
            if (replyTo != null ) {
                //if could not login then save session user/pass as empty
                if (!retval) {
                    sharedPreferenceManager.saveSessionUsernamePass("", "");
                }
                Log.d(Tag.BACKGROUND_MANAGER, "Setting up replyTo");
                Message message = new Message();
                message.what = MSG_ATTEMPT_LOGIN;
                message.arg1 = retval ? 1 : 0;
                try {
                    Log.d(Tag.BACKGROUND_MANAGER, "Sending response in message about login [" + retval + "]");
                    replyTo.send(message);
                } catch (RemoteException e) {
                    Log.d(Tag.BACKGROUND_MANAGER, "Could not send response back. Serious issue.");
                }
            }
        }

        private boolean attemptLogin() {
            boolean retval = false;
            Log.d(Tag.BACKGROUND_MANAGER, "attemptLogin, network manager[" + networkManager.canConnectBackgroundService() + "]");
            if (networkManager.canConnectBackgroundService()) {
                //we have internet
                retval = sessionManager.login(mUsername, mPassword);
                if (retval) {
                    //we can login to webserver
                    retval = webSocketManager.createConnection();
                    if (!retval) {
                        //we can login but not create a connection
                        Log.d(Tag.BACKGROUND_MANAGER, "Can login, but cannot connect socket.");
                        sessionManager.logout(mUsername, mPassword);
                    }
                }
            }
            Log.d(Tag.BACKGROUND_MANAGER, "response on login [" + retval + "]");
            return retval;
        }
    }

    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_VALUE = 3;
    public static final int MSG_ACTION_BUTTON = 4;
    public static final int MSG_SAVE_USERNAME_PASS = 5;
    public static final int MSG_SAVE_SESSION_USERNAME_PASS = 6;
    public static final int MSG_SAVE_REMEMBERME = 7;
    public static final int MSG_SAVE_BACKGROUND_STATE = 8;
    public static final int MSG_ATTEMPT_LOGIN = 9;
    public static final int MSG_REQUEST = 10;
    public static final int MSG_POST_WEBSOCKET_UPDATE = 11;
    public static final int MSG_LOGOUT = 12;

    public static final String KEY_ACTION = "KEY_ACTION"; //TODO REMOVE AFTER TESTING
    public static final String KEY_USERNAME_PASS = "KEY_USERNAME_PASS";

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
                case MSG_ACTION_BUTTON:
                    JSONBuilder action = (JSONBuilder) msg.obj;
                    BackgroundManager.this.requestManager.addRequest(action);
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
                case MSG_SAVE_BACKGROUND_STATE:
                    Log.d(Tag.BACKGROUND_MANAGER,"Saving background state as[" + (msg.arg1 == 0 ? false : true) + "]");
                    sharedPreferenceManager.saveBackgroundState(msg.arg1 == 0 ? false : true);
                    break;
                case MSG_ATTEMPT_LOGIN:
                    attemptThreadLogin(msg.replyTo);
                    break;
                case MSG_REQUEST: //TODO REMOVE AFTER TESTING
                    String actionString = msg.getData().getString(KEY_ACTION);
                    JSONBuilder.Action actionRequest = JSONBuilder.Action.GET_CONTACTS;
                    for (JSONBuilder.Action act : JSONBuilder.Action.values()) {
                        if (act.name().equals(actionString)) {
                            actionRequest = act;
                            break;
                        }
                    }
                    JSONBuilder jsonBuilder = new JSONBuilder(actionRequest);
                    requestManager.addRequest(jsonBuilder);
                    break;
                case MSG_POST_WEBSOCKET_UPDATE:
                    Intent intent = new Intent(Tag.ACTION_WEBSOC_CHANGE);
                    intent.putExtra("state", webSocketManager.getLocalizeState());
                    LocalBroadcastManager.getInstance(BackgroundManager.this).sendBroadcast(intent);
                    break;
                case MSG_LOGOUT:
                    Log.d(Tag.BACKGROUND_MANAGER,"Logging out of backMan.");
                    sessionManager.logout(sharedPreferenceManager.getSessionUsername(),
                            sharedPreferenceManager.getSessionPassword());
                    sharedPreferenceManager.saveSessionUsernamePass("", "");
                    sharedPreferenceManager.saveBackgroundState(false);
                    webSocketManager.disconnect();
                    break;
                /**case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i=mClients.size()-1; i>=0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null, MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;*/
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
        running = false;
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
            if (action.equals(Tag.ACTION_WEBSOC_CHANGE)) {
                final String state = intent.getStringExtra("state");
                Log.d(Tag.BACKGROUND_MANAGER, "State changed receiving broadcast. State [" + state + "], " +
                        "webSocketState [" + webSocketManager.localizeState(WebSocketState.CLOSED) + "]");
                if (state.equals(webSocketManager.localizeState(WebSocketState.CLOSED))) {
                    attemptThreadLogin(null);
                }
            }
        }
    }
}