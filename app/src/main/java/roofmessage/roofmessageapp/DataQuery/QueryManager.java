package roofmessage.roofmessageapp.DataQuery;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import java.util.concurrent.ConcurrentLinkedQueue;

import roofmessage.roofmessageapp.JSONBuilder;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class QueryManager implements Runnable{
    private ConcurrentLinkedQueue<JSONBuilder> requests = new ConcurrentLinkedQueue<JSONBuilder>();
    private static ContactManager contactManager = null;
    private static QueryManager queryManager = null;
    private static MessageManager messageManager = null;
    private long waitTime = 1000;
    private final static String TAG = "ROOFMESSAGE:QManager: ";

    protected QueryManager() {}

    public static QueryManager getInstance(Context context) {
        if(queryManager == null) {
            contactManager = new ContactManager(context);
            queryManager = new QueryManager();
            messageManager = new MessageManager(context);
        }

        return queryManager;
    }

    public void addRequest(JSONBuilder jsonBuilder){
        requests.add(jsonBuilder);
    }

    @Override
    public void run() {

        while(true) {
            try {
                if (!requests.isEmpty()) {
                    JSONBuilder jsonRequest = requests.poll();
                    String action = jsonRequest.getString(JSONBuilder.JSON_KEY_MAIN.ACTION.name().toLowerCase());
                    if(action.equals(JSONBuilder.JSON_KEY_MAIN.GET_CONTACTS.name().toLowerCase())) {
                        JSONBuilder send = contactManager.getContacts();
                    } else if(action.equals(JSONBuilder.JSON_KEY_MAIN.GET_MESSAGE.name().toLowerCase())) {

                    } else {
                        Log.e(TAG, "Could not find action: " + action);
                    }
                }

                this.wait(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
