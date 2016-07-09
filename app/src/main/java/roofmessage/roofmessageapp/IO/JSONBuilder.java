package roofmessage.roofmessageapp.io;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jesse Saran on 6/30/2016.
 */
public class JSONBuilder extends JSONObject{

    private final static String LOG_TAG = "ROOFMESSAGE:JSONBuilder";

    public enum JSON_KEY_MAIN {
        //main message keys
        ACTION,
    }

    public enum JSON_KEY_CONTACT {
        //specific contact keys
        ANDROID_ID,
        FULL_NAME,
        PHONE_NUMBER,
        CONTACT,
    }

    public enum JSON_KEY_CONVERSATION {
        CONVERSATIONS,
        MESSAGES,

        //specific conversation keys
        DATE,
        THREAD_ID,
        MESSAGE_COUNT,
        READ,
        RECIPIENTS,

        RECIPIENT_ID,
        PHONE_NUMBER,

        MESSAGE_TYPE,
        CONVO_TYPE,

        BODY,
        DATE_RECIEVED,
        DATE_SENT,
        LOCKED,
        SEEN,
        SUBJECT,

        //TYPES
        MMS,
        SMS,
    }

    public enum Message_Type{
        MMS,
        SMS,

        TYPE,
    }

    public enum Action {
        //CONTACTS
        GET_CONTACTS,
        POST_CONTACTS,

        //CONVERSATIONS
        GET_CONVERSATIONS,
        POST_CONVERSATIONS,

        //MESSAGES
        GET_MESSAGES,
        POST_MESSAGES,

        //tets
        GET_MMS_CONVO,
        GET_SMS_CONVO,
    }

    public JSONBuilder( Action action ) {
        try {
            this.put( "action", action.name().toLowerCase() );
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unable to add action to JSON");
            e.printStackTrace();
        }
    }
}
