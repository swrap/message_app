package roofmessage.roofmessageapp.activity;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import roofmessage.roofmessageapp.dataquery.ContactManager;
import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.dataquery.MessageManager;
import roofmessage.roofmessageapp.dataquery.QueryManager;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.io.PermissionsManager;
import roofmessage.roofmessageapp.utils.Tag;

public class MainActivity extends AppCompatActivity {

    private boolean HAS_ALL_PERMISSIONS = false;

    private final static String START_STATUS = "Status: ";
    private final static String NOT_CONNECTED = "Not Connected";
    private final static String CONNECTED = "Connected";
    private final static String DISCONNECT = "Disconnect";

    // UI references.
    private Button mConnectionButton;
    private TextView mStatus;

    QueryManager queryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queryManager = QueryManager.getInstance(MainActivity.this.getApplicationContext());
        Log.d(Tag.MAIN_ACTIVITY, "IS ALIVE: " + queryManager.isAlive() + " STATE: " + queryManager.getState().name());
        if(!queryManager.isAlive()){
            queryManager.start();
        }
        Log.d(Tag.MAIN_ACTIVITY, "IS ALIVE: " + queryManager.isAlive() + " STATE: " + queryManager.getState().name());

        mConnectionButton = (Button) findViewById(R.id.connection_button);
        mStatus = (TextView) findViewById(R.id.status);
        mStatus.setText(TextUtils.concat());

        Button mContact = (Button) findViewById(R.id.post_contact);
        mContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_CONTACTS);
                queryManager.addRequest( jsonBuilder );
            }
        });

        Button mConvo = (Button) findViewById(R.id.convo);
        mConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_CONVERSATIONS);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button mSMSConvo = (Button) findViewById(R.id.sms_convo);
        mSMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_SMS_CONVO);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button mMMSConvo = (Button) findViewById(R.id.mms_convo);
        mMMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_MMS_CONVO);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button getMessages = (Button) findViewById(R.id.messages);
        getMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_MESSAGES);
                queryManager.addRequest(jsonBuilder);
            }
        });

        HAS_ALL_PERMISSIONS = PermissionsManager.hasAllPermissions(this.getApplicationContext());
        if (!HAS_ALL_PERMISSIONS) {
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        while(queryManager.isAlive()){
            queryManager.kill();
        }
        Log.i(Tag.MAIN_ACTIVITY, "Killed");
    }
}