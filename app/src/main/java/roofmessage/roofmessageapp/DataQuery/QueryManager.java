package roofmessage.roofmessageapp.dataquery;

import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONException;

import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class QueryManager extends Thread{
    private static ConcurrentLinkedQueue<JSONBuilder> requests;
    private static ContactManager contactManager = null;
    private static QueryManager queryManager = null;
    private static MessageManager messageManager = null;
    private boolean kill = false;

    private final static Object LOCK = new Object();

    protected QueryManager() {}

    public static QueryManager getInstance(Context context) {
        if(queryManager == null) {
            contactManager = new ContactManager(context);
            queryManager = new QueryManager();
            messageManager = new MessageManager(context);
            requests = new ConcurrentLinkedQueue<JSONBuilder>();
        } else if (queryManager.getState() == State.TERMINATED){
            contactManager = new ContactManager(context);
            queryManager = new QueryManager();
            messageManager = new MessageManager(context);
        }

        return queryManager;
    }

    public void addRequest(JSONBuilder jsonBuilder){
        requests.add(jsonBuilder);
        synchronized (this) {
            this.notify();
        }
        Log.d(Tag.QUERY_MANAGER, "Added request and notified");
    }

    public void kill(){
        this.kill = true;
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void run() {

        Log.d(Tag.QUERY_MANAGER, "STRAING THREAD");
        while(true) {
            try {
                synchronized (this) {
                    this.wait();
                    if(kill){
                        break;
                    }
                }

                if (!requests.isEmpty()) {
                    JSONBuilder jsonRequest = requests.poll();
                    String action = jsonRequest.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase());

                    if(action.equals(JSONBuilder.Action.GET_CONTACTS.name().toLowerCase())) {
                        JSONBuilder send = contactManager.getContacts();
                        Log.d(Tag.QUERY_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_CONVERSATIONS.name().toLowerCase())) {
                        JSONBuilder send = messageManager.getAllConversations();
                        Log.d(Tag.QUERY_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_SMS_CONVO.name().toLowerCase())) {
                        JSONBuilder send = messageManager.getSMS("596", 5, 0);
                        Log.d(Tag.QUERY_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MMS_CONVO.name().toLowerCase())) {
                        JSONBuilder send = messageManager.getMMS("596", 5, "0", "0");
                        Log.d(Tag.QUERY_MANAGER, send.toString(4));
                    } else if(action.equals(JSONBuilder.Action.GET_MESSAGES.name().toLowerCase())) {
                        JSONBuilder send = messageManager.getConversationMessages("596", 3, 13);
                        Log.d(Tag.QUERY_MANAGER, send.toString(4));
                    } else {
                        Log.e(Tag.QUERY_MANAGER, "Could not find action: " + action);
                    }

                    Log.d(Tag.QUERY_MANAGER, "Querying");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
