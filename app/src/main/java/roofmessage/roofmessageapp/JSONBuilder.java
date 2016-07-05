package roofmessage.roofmessageapp;

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
        CONTACTS,
        GET_CONTACTS,
        GET_MESSAGE,
    }

    public enum JSON_KEY_CONTACT {
        //specific contact keys
        ANDROID_ID,
        FULL_NAME,
        PHONE_NUMBER,
        CONTACT,
    }

    public enum JSON_KEY_MESSAGE {
        //specific message keys
        _ID,
        THREAD_ID,
        ADDRESS,
        PERSON,
        DATE,
        BODY,
        TYPE;
    }

    public enum Action {
        post_contacts,
        post_update,
        post_delete,
        post_create;
    }

    public JSONBuilder( Action action ) {
        try {
            this.put( "action", action.name() );
        } catch (JSONException e) {
            Log.e(LOG_TAG, "unable to add action to JSON");
            e.printStackTrace();
        }
    }
}
