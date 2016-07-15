package roofmessage.roofmessageapp.io;

import android.content.Context;
import android.content.SharedPreferences;

import roofmessage.roofmessageapp.R;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class SharedPreferenceManager {
    private static SharedPreferenceManager sharedPreferenceManager;
    private static Context context;

    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";

    private SharedPreferenceManager(){}

    public static SharedPreferenceManager getInstance(Context context){
        if(context == null){
            context = context;
        }
        if(sharedPreferenceManager == null){
            sharedPreferenceManager = new SharedPreferenceManager();
        }

        return sharedPreferenceManager;
    }

    public void saveUserPass(String username, String password){
        SharedPreferences.Editor editor = context.getSharedPreferences(
                context.getString(R.string.shared_preferences), Context.MODE_PRIVATE).edit();
        editor.putString(USERNAME, username);
        editor.putString(PASSWORD, password);
    }

    public String getUserName() {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        return preferences.getString(USERNAME, null);
    }

    public String getPassword() {
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        return preferences.getString(PASSWORD, null);
    }
}
