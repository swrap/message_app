package roofmessage.roofmessageapp.background.request.MessageObservers;

import android.util.Log;

import org.json.JSONException;

import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/19/2016.
 */
public class MessageWaiting {
    public final String numbers;
    public final String body;
    public final String temp_message_id;

    public MessageWaiting(String body, String temp_message_id, String numbers) {
        this.numbers = numbers;
        this.temp_message_id = temp_message_id;
        this.body = body;
    }

    public JSONBuilder getJSON(String thread_id, String message_id) {
        JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.SEND_MESSAGES);
        try {
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.ACTION.name().toLowerCase(),
                    JSONBuilder.Action.SENT_MESSAGES.name().toLowerCase());
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
                    temp_message_id);
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.THREAD_ID.name().toLowerCase(),
                    thread_id);
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
                    message_id);
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_WAITING, "Unable to create json object.", e);
        }
        return jsonBuilder;
    }

    public JSONBuilder getFailedJSON(String message_id) {
        JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.SEND_MESSAGES);

        try {
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.ACTION.name().toLowerCase(),
                    JSONBuilder.Action.SENT_MESSAGES_FAILED.name().toLowerCase());
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
                    temp_message_id);
            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
                    message_id);
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_WAITING, "Unable to create json object. ", e);
        }
        return jsonBuilder;
    }
    //TODO go through each way to close application and test out closing service and so on.
}
