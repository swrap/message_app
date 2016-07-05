package roofmessage.roofmessageapp;

import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roofmessage.roofmessageapp.DataQuery.ContactManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ROOFMESSAGE:MainAct";

    private boolean HAS_ALL_PERMISSIONS = false;

    private final static String START_STATUS = "Status: ";
    private final static String NOT_CONNECTED = "Not Connected";
    private final static String CONNECTED = "Connected";
    private final static String DISCONNECT = "Disconnect";

    // UI references.
    private Button mConnectionButton;
    private TextView mStatus;

    private ContactManager contactManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactManager = ContactManager.getInstance(MainActivity.this.getApplicationContext());

        mConnectionButton = (Button) findViewById(R.id.connection_button);
        mStatus = (TextView) findViewById(R.id.status);
        mStatus.setText(TextUtils.concat());

        Button mPostContact = (Button) findViewById(R.id.post_contact);
        mPostContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contactManager.getContacts();
            }
        });

        HAS_ALL_PERMISSIONS = RoofPermissionsManager.hasAllPermissions(this.getApplicationContext());
        if (!HAS_ALL_PERMISSIONS) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
    }
}