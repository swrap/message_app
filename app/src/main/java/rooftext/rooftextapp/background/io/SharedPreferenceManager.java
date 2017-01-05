package rooftext.rooftextapp.background.io;

import android.content.Context;
import android.content.SharedPreferences;

import rooftext.rooftextapp.R;

/**
 * Created by Jesse Saran on 7/9/2016.
 */
public class SharedPreferenceManager {
    private static SharedPreferenceManager sharedPreferenceManager;
    private SharedPreferences preferences;

    public enum PreferenceKey {
        USERNAME,
        PASSWORD,
        SESSION_USERNAME,
        SESSION_PASSWORD,
        REMEMBER_ME,
        BACKGROUND_STATE,
    }


    private SharedPreferenceManager(Context context){
        //TODO Might night to switch to ContentProvider
        preferences = context.getSharedPreferences(
                context.getString(R.string.shared_preferences), Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE);
    }

    public static SharedPreferenceManager getInstance(Context context){
        if(sharedPreferenceManager == null){
            sharedPreferenceManager = new SharedPreferenceManager(context);
        }

        return sharedPreferenceManager;
    }

    public boolean saveUserPass(String username, String password){
        SharedPreferences.Editor editor = preferences.edit();
        return editor.putString(PreferenceKey.USERNAME.name(), username).commit() &&
        editor.putString(PreferenceKey.PASSWORD.name(), password).commit();
    }

    public boolean saveRememberMe(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        return editor.putBoolean(PreferenceKey.REMEMBER_ME.name(), value).commit();
    }

    public boolean saveBackgroundState(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        return editor.putBoolean(PreferenceKey.BACKGROUND_STATE.name(), value).commit();
    }

    public boolean saveSessionUsernamePass(String username, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        return editor.putString(PreferenceKey.SESSION_USERNAME.name(), username).commit() &&
                editor.putString(PreferenceKey.SESSION_PASSWORD.name(), password).commit();
    }

    public String getUsername() {
        return preferences.getString(PreferenceKey.USERNAME.name(), "");
    }

    public String getPassword() {
        return preferences.getString(PreferenceKey.PASSWORD.name(), "");
    }

    public boolean getRememberMe() {
        return preferences.getBoolean(PreferenceKey.REMEMBER_ME.name(), false);
    }

    public boolean getBackgroundState () {
        return preferences.getBoolean(PreferenceKey.BACKGROUND_STATE.name(), false);
    }

    public String getSessionUsername() {
        return preferences.getString(PreferenceKey.SESSION_USERNAME.name(), "");
    }

    public String getSessionPassword() {
        return preferences.getString(PreferenceKey.SESSION_PASSWORD.name(), "");
    }
}
