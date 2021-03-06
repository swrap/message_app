package rooftext.rooftextapp.io;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 6/30/2016.
 */
public class JSONBuilder extends JSONObject{

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
        MESSAGE,

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
        ID,
        AMOUNT,
        OFFSET,
        CREATOR,

        FULL_NAME,
        ADDRESS,

        //TYPES
        MMS,
        SMS, TEXT_ONLY, PARTS,

        DATA, PERIOD, CONTACT_ID, HEX_COLOR,
    }

    public enum Message_Type{
        MMS,
        SMS,

        TYPE,
    }

    public enum JSON_KEY_MESSAGE_DELIVERY {
        ACTION,
        BODY,
        DATA,
        NUMBER,
        NUMBERS,
        TEMP_MESSAGE_ID,
        THREAD_ID,
        MESSAGE_ID, PART_ID, CONTENT_TYPE,
    }

    public enum Action {
        //QUERY_CONTACTS
        GET_CONTACTS,
        POST_CONTACTS,

        //QUERY_CONVERSATIONS
        GET_CONVERSATIONS,
        POST_CONVERSATIONS,
        //QUERY_SMS_MMS_CONVO
        GET_MMS_CONVO,
        GET_SMS_CONVO,

        //QUERY_MESSAGES
        GET_MESSAGES,
        POST_MESSAGES,

        //SEND_MESSAGES
        SEND_MESSAGES,
        SENT_MESSAGES,
        SENT_MESSAGES_FAILED,

        //UPDATING MESSAGES
        RECEIVED_MESSAGE,

        //CANCEL ACTIONS
        CANCELLED,

        //CONNECTION
        CONNECTED,
        DISCONNECTED,

        //data actions
        GET_DATA,
        POST_DATA, CONNECTED_REPLY,
    }

    public enum Parts {
        CONTENT_TYPE,
        TEXT,
        ID,
        MESSAGE_ID
    }

    public JSONBuilder( Action action ) {
        try {
            this.put( "action", action.name().toLowerCase() );
        } catch (JSONException e) {
            Log.e(Tag.JSON_BUILDER, "unable to add action to JSON");
            e.printStackTrace();
        }
    }

    public JSONBuilder(String stringExtra) throws JSONException {
        super(stringExtra);
    }
}
