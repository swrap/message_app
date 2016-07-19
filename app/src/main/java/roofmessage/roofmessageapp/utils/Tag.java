package roofmessage.roofmessageapp.utils;

import android.content.IntentFilter;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class Tag {
    public static final String BASE_TAG = "RM:";

    //Dataquery Package
    public static final String CONTACT_MANAGER = BASE_TAG + "ContactMan";
    public static final String MESSAGE_MANAGER = BASE_TAG + "MessageMan";
    public static final String QUERY_MANAGER = BASE_TAG + "QueryMan";

    //Activity Package
    public static final String MAIN_ACTIVITY = BASE_TAG + "MainAct";
    public static final String LOGIN_ACTIVITY = BASE_TAG + "LoginAct";

    //IO Package
    public static final String WEB_SOC_MANAGER = BASE_TAG + "WebSocMan";
    public static final String SESSION_MANAGER = BASE_TAG + "SessionMan";
    public static final String BASE_URL = "192.168.1.145:8000";

    //LocalBroadCastReceivers
    public static final String ACTION_WEBSOC_CHANGE = "WEBSOCEKT_STATE_CHANGE";
    public static final String ACTION_RECEIVED_MESSAGE = "WEBSOCKET_RECEIVED_MESSAGE";

    //BackgroundTask
    public static final String BACKGROUND_MANAGER = BASE_TAG + "BackMan";
}