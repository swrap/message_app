package rooftext.rooftextapp.background.request;

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

import rooftext.rooftextapp.background.request.MessageObservers.MMSObserver;
import rooftext.rooftextapp.background.request.MessageObservers.MessageWaiting;
import rooftext.rooftextapp.io.JSONBuilder;
import rooftext.rooftextapp.background.request.MessageObservers.SMSObserver;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class RequestManager extends Thread {
    private ConcurrentLinkedQueue<JSONBuilder> requests = new ConcurrentLinkedQueue<JSONBuilder>();;
    private static ContactQuery contactQuery = null;
    private static RequestManager requestManager;
    private static MessageQuery messageQuery = null;
    private static SMSObserver smsObserver = null;
    private static MMSObserver mmsObserver = null;
    //Todo make these not static
    private boolean kill = false;
    private static Context context;
    private ActionReceiver actionReceiver = new ActionReceiver();

    public static ConcurrentLinkedQueue<MessageWaiting> messagesWaiting = new ConcurrentLinkedQueue<MessageWaiting>();

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
            mmsObserver = MMSObserver.getInstance(context);
        }

        return requestManager;
    }

    public void addRequest(JSONBuilder jsonBuilder){
        //check if Async, if async, create request here do not notify
        try {
            String action = jsonBuilder.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase());
            if (action.equals(JSONBuilder.Action.GET_DATA.name().toLowerCase())) {
                    //SPECIAL CASE RUN WITH NON BLOCKING FOR QUERYING DATA
                    Intent intent = new Intent(Tag.ACTION_LOCAL_SEND_MESSAGE);
                    messageQuery.queryDataAndSendAsync(jsonBuilder.getString(
                            JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.PART_ID.name().toLowerCase()),
                            jsonBuilder.getString(
                                    JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.CONTENT_TYPE.name().toLowerCase()),
                            jsonBuilder.getString(
                                    JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase()),
                            context,
                            intent);
            } else if(action.equals(JSONBuilder.Action.GET_CONVERSATIONS.name().toLowerCase())) {
                Log.d(Tag.REQUEST_MANAGER, "Querying conversations async");
                messageQuery.sendAllConversationsAsync(context);
            } else {
                //Synchronous request
                requests.add(jsonBuilder);
                synchronized (this) {
                    this.notify();
                }
                try {
                    Log.d(Tag.REQUEST_MANAGER, "Added request [" + jsonBuilder.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase()) + "] and notified");
                } catch (JSONException e) {
                    Log.d(Tag.REQUEST_MANAGER, "Could not find action key-value pair.");
                }
            }
        } catch (JSONException e) {
            Log.e(Tag.REQUEST_MANAGER, "Could not find action :(.");
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

                if (requests.isEmpty()) {
                    synchronized (this) {
                        this.wait();
                        if (kill) {
                            break;
                        }
                    }
                }
                Log.d(Tag.REQUEST_MANAGER, "Woken with request length [" + requests.size() + "]");

                if (!requests.isEmpty()) {
                    JSONBuilder jsonRequest = requests.poll();
                    Log.d(Tag.REQUEST_MANAGER, "Request to string [" + jsonRequest + "]");
                    String action = jsonRequest.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase());
                    JSONBuilder send = null;

                    Log.d(Tag.REQUEST_MANAGER, "Processing request action [" + action + "]");
                    if(action.equals(JSONBuilder.Action.GET_CONTACTS.name().toLowerCase())) {
                        Log.d(Tag.REQUEST_MANAGER, "Querying contacts");
                        send = contactQuery.getContacts();
//                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_SMS_CONVO.name().toLowerCase())) {
                        send = messageQuery.getSMS("596", 5, "0", 1);
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MMS_CONVO.name().toLowerCase())) {
                        send = messageQuery.getMMS("596", 5, "0", "0");
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MESSAGES.name().toLowerCase())) {
                        send = messageQuery.getConversationMessages(
                                jsonRequest.getString(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase()),
                                jsonRequest.getInt(JSONBuilder.JSON_KEY_CONVERSATION.AMOUNT.name().toLowerCase()),
                                jsonRequest.getString(JSONBuilder.JSON_KEY_CONVERSATION.OFFSET.name().toLowerCase()),
                                jsonRequest.getInt(JSONBuilder.JSON_KEY_CONVERSATION.PERIOD.name().toLowerCase())
                        );
//                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.SEND_MESSAGES.name().toLowerCase())) {
                        Log.d(Tag.REQUEST_MANAGER, "Attempting to send message[" + jsonRequest.toString() + "]");
//                        String [] arrNumbers = {"9146108631","8143238900"};
//                        SmsMmsDelivery smsMmsDelivery = new SmsMmsDelivery(
//                                "How is it going?",
//                                "5",
//                                arrNumbers,
//                                smsObserver,
//                                mmsObserver);
                        JSONArray jsonNumbers = jsonRequest.getJSONArray(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.NUMBERS.name().toLowerCase());
                        String [] arrNumbers = new String[jsonNumbers.length()];
                        for (int i = 0; i < jsonNumbers.length(); i++) {
                            arrNumbers[i] = jsonNumbers.getJSONObject(i).getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.NUMBER.name().toLowerCase());
                        }
                        SmsMmsDelivery smsMmsDelivery = new SmsMmsDelivery(
                                jsonRequest.getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.BODY.name().toLowerCase()),
                                jsonRequest.getString(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase()),
                                arrNumbers,
                                smsObserver,
                                mmsObserver);
                        ArrayList<MessageWaiting> messageWaitings = smsMmsDelivery.prepareMessage();
                        Log.d(Tag.REQUEST_MANAGER, "Preparing message: " + action + " " + messageWaitings.size());
                        for (MessageWaiting message : messageWaitings) {
                            this.messagesWaiting.add(message);
                        }
                        Log.d(Tag.REQUEST_MANAGER, "Added message: " + action);
                        smsMmsDelivery.sendMessage(context);
                    } else if(action.equals(JSONBuilder.Action.CONNECTED.name().toLowerCase())) {
                        send = new JSONBuilder(JSONBuilder.Action.CONNECTED);
                        Log.d(Tag.REQUEST_MANAGER, send.toString(4));
                    } else {
                        Log.d(Tag.REQUEST_MANAGER, "Could not find action: " + action);
                    }

                    if (send != null) {
                        Log.d(Tag.REQUEST_MANAGER, "Sending 'send' from action [" + action + "]");
                        Intent intent = new Intent(Tag.ACTION_LOCAL_SEND_MESSAGE);
                        intent.putExtra(Tag.KEY_SEND_JSON_STRING, send.toString());
                        Log.d(Tag.REQUEST_MANAGER, "Broadcasting 'send' from action [" + action + "]");
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
                if (!action.equals("") && action.equals(Tag.ACTION_LOCAL_RECEIVED_MESSAGE)) {
                    Log.d(Tag.REQUEST_MANAGER, "Recieved from local [" + action + "] " + intent.getStringExtra(Tag.KEY_MESSAGE));
                    try {
                        String message = intent.getStringExtra(Tag.KEY_MESSAGE);
                        if (message != null && !message.isEmpty()) {
                            addRequest(new JSONBuilder(message));
                        } else {
                            Log.e(Tag.REQUEST_MANAGER, "Received request with action [" + action + "] and message [" +
                            message + "]");
                        }
                    } catch (JSONException e) {
                        Log.d(Tag.REQUEST_MANAGER, "Could not create JSON Object.", e);
                    }
                }
            }
        }
    }
}
