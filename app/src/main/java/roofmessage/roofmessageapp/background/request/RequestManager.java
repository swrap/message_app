package roofmessage.roofmessageapp.background.request;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import roofmessage.roofmessageapp.background.request.MessageObservers.MessageWaiting;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.background.request.MessageObservers.SMSObserver;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class RequestManager extends Thread {
    private ConcurrentLinkedQueue<JSONBuilder> requests = new ConcurrentLinkedQueue<JSONBuilder>();;
    private static ContactQuery contactQuery = null;
    private static RequestManager requestManager;
    private static MessageQuery messageQuery = null;
    private static SMSObserver smsObserver = null;
    //Todo make these not static
    private boolean kill = false;
    private static Context context;
    private ActionReceiver actionReceiver = new ActionReceiver();

    protected RequestManager(Context context) {
        this.context = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Tag.ACTION_LOCAL_RECEIVED_MESSAGE);
        LocalBroadcastManager.getInstance(this.context).registerReceiver(actionReceiver, intentFilter);
    }

    public static RequestManager getInstance(Context context) {
        if(requestManager == null) {
            contactQuery = new ContactQuery(context);
            messageQuery = new MessageQuery(context);
            smsObserver = SMSObserver.getInstance(context);
            requestManager = new RequestManager(context);
        }

        return requestManager;
    }

    public void addRequest(JSONBuilder jsonBuilder){
        requests.add(jsonBuilder);
        synchronized (this) {
            this.notify();
        }
        Log.e(Tag.REQUEST_MANAGER, "STATE: " + this.getState());
        try {
            Log.d(Tag.REQUEST_MANAGER, "Added request [" + jsonBuilder.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase()) + "] and notified");
        } catch (JSONException e) {
            Log.d(Tag.REQUEST_MANAGER, "Could not find action key-value pair.");
        }
    }

    private void kill(){
        this.kill = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void run() {

        Log.d(Tag.REQUEST_MANAGER, "Starting Thread");
        while(true) {
            try {
                synchronized (this) {
                    this.wait();
                    if(kill){
                        break;
                    }
                }
                Log.d(Tag.REQUEST_MANAGER, "Woken with request length [" + requests.size() + "]");

                if (!requests.isEmpty()) {
                    JSONBuilder jsonRequest = requests.poll();
                    String action = jsonRequest.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase());
                    JSONBuilder send = null;

                    Log.d(Tag.REQUEST_MANAGER, "Processing request action [" + action + "]");
                    if(action.equals(JSONBuilder.Action.GET_CONTACTS.name().toLowerCase())) {
                        send = contactQuery.getContacts();
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_CONVERSATIONS.name().toLowerCase())) {
                        send = messageQuery.getAllConversations();
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_SMS_CONVO.name().toLowerCase())) {
                        send = messageQuery.getSMS("596", 5, 0);
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MMS_CONVO.name().toLowerCase())) {
                        send = messageQuery.getMMS("596", 5, "0", "0");
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MESSAGES.name().toLowerCase())) {
                        send = messageQuery.getConversationMessages(
                                jsonRequest.getString(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase()),
                                jsonRequest.getInt(JSONBuilder.JSON_KEY_CONVERSATION.AMOUNT.name().toLowerCase()),
                                jsonRequest.getInt(JSONBuilder.JSON_KEY_CONVERSATION.OFFSET.name().toLowerCase())
                        );
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.SEND_MESSAGES.name().toLowerCase())) {
                        Log.d(Tag.REQUEST_MANAGER, "Attempting to send message");
//                        String [] temp = {"4848889627"};
//                        SmsMmsDelivery smsMmsDelivery = new SmsMmsDelivery("hello self!","8573468",temp);
                        JSONArray jsonNumbers = jsonRequest.getJSONArray(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.NUMBERS.name().toLowerCase());
                        String [] arrNumbers = new String[jsonNumbers.length()];
                        for (int i = 0; i < jsonNumbers.length(); i++) {
                            arrNumbers[i] = jsonNumbers.getJSONObject(i).getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.NUMBER.name().toLowerCase());
                        }
                        SmsMmsDelivery smsMmsDelivery = new SmsMmsDelivery(
                                jsonRequest.getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.BODY.name().toLowerCase()),
                                jsonRequest.getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase()),
                                arrNumbers);
                        ArrayList<MessageWaiting> messageWaitings = smsMmsDelivery.prepareMessage();
                        Log.d(Tag.REQUEST_MANAGER, "Preparing message: " + action);
                        for (MessageWaiting message : messageWaitings) {
                            smsObserver.addMessage(message);
                        }
                        Log.d(Tag.REQUEST_MANAGER, "Added message: " + action);
                        smsMmsDelivery.sendMessage(context, smsObserver);
                    } else {
                        Log.d(Tag.REQUEST_MANAGER, "Could not find action: " + action);
                    }

                    if (send != null) {
                        Intent intent = new Intent(Tag.ACTION_LOCAL_SEND_MESSAGE);
                        intent.putExtra(Tag.KEY_SEND_JSON_STRING, send.toString());
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                }
            } catch (InterruptedException e) {
                Log.e(Tag.REQUEST_MANAGER, "Interrupted request manager. Something messed up bad.", e);
            } catch (JSONException e) {
                Log.e(Tag.REQUEST_MANAGER, "JSONException in request manager. Something messed up.", e);
            }
        }
    }

    private class ActionReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                Log.d(Tag.REQUEST_MANAGER, "Recieved: " + action + " " + intent.getStringExtra(Tag.KEY_MESSAGE));
                if (!action.equals("") && action.equals(Tag.ACTION_LOCAL_RECEIVED_MESSAGE)) {
                    try {
                        addRequest(new JSONBuilder(intent.getStringExtra(Tag.KEY_MESSAGE)));
                    } catch (JSONException e) {
                        Log.d(Tag.REQUEST_MANAGER, "Could not create JSON Object.", e);
                    }
                }
            }
        }
    }
}
