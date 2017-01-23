package rooftext.rooftextapp.utils;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class Tag {

    public static String VERSION = "2.2.2";

    public static final boolean LOCAL_HOST = false;

    public static final String BASE_TAG = "RM:";

    //Dataquery Package
    public static final String CONTACT_MANAGER = BASE_TAG + "ContactMan";
    public static final String MESSAGE_MANAGER = BASE_TAG + "MessageMan";
    public static final String NETWORK_MANAGER = BASE_TAG + "NetworkMan";
    public static final String REQUEST_MANAGER = BASE_TAG + "RequestMan";
        //MessageObservers
        public static final String MESSAGE_WAITING = BASE_TAG + "MessageWaiting";
        public static final String SMSMMSDELIVERY = BASE_TAG + "SMSMMSDelivery";
        public static final String SMS_OBSERVER = BASE_TAG + "SmsObserver";
        public static final String MMS_OBSERVER = BASE_TAG + "MmsObserver";

    //Activity Package
    public static final String MAIN_ACTIVITY = BASE_TAG + "MainAct";
    public static final String LOGIN_ACTIVITY = BASE_TAG + "LoginAct";

    //IO Package
    public static final String WEB_SOC_MANAGER = BASE_TAG + "WebSocMan";
    public static final String SESSION_MANAGER = BASE_TAG + "SessionMan";
    public static final String BASE_URL = LOCAL_HOST ? "192.168.1.153:8000" : "rooftext.com";

    //LocalBroadCastReceivers and Broadcast receivers
    //TODO GET THESE LOCAL AND NON_LOCAL ACTIONS FIGURED THE FUCK OUT
    public static final String ACTION_WEBSOC_CHANGE = "roofmessage.roofmessageapp.WEBSOCEKT_STATE_CHANGE";
    public static final String ACTION_RECEIVED_MESSAGE = "roofmessage.roofmessageapp.WEBSOCKET_RECEIVED_MESSAGE";
    public static final String ACTION_LOCAL_RECEIVED_MESSAGE = "LOCAL_WEBSOCKET_RECEIVED_MESSAGE";
    public static final String ACTION_LOCAL_SEND_MESSAGE = "ACTION_LOCAL_SEND_MESSAGE";
    public static final String ACTION_FAILED_LOGIN = "ACTION_FAILED_LOGIN"; // used to post login updates to main for error messages
    public static final String ACTION_LOCAL_WEBSOC_CHANGE = "ACTION_LOCAL_WEBSOC_CHANGE";
    public static final String ACTION_LOCAL_NETWORK_CHANGE = "ACTION_LOCAL_NETWORK_CHANGE";
        //used by websocket manager to send a jso
        public static final String KEY_SEND_JSON_STRING = "KEY_SEND_JSON_STRING";
    public static final String ACTION_LOCAL_MMS_SEND_RECEIVER = "LOCAL_MMS_SEND_RECEIVER";

    //used for the key to find the value of the message.
    public static final String KEY_MESSAGE = "KEY_MESSAGE";

    //BackgroundTask
    public static final String BACKGROUND_MANAGER = BASE_TAG + "BackMan";
    public static final String BIND_LISTENER = BASE_TAG + "BindListener";

    //JSONBuilder
    public static final String JSON_BUILDER = BASE_TAG + "JSONBuilder";

    //Main Activity / Login Activity Background manager intents
    public static final String KEY_INTENT_LOGIN_ACTIVITY = "KEY_INTENT_LOGIN_ACTIVITY";

    //Utils Tag
    public static final String UTILS = "Utils:";
}