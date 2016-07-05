package roofmessage.roofmessageapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import java.security.Permission;
import java.util.ArrayList;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.INTERNET;

/**
 * Created by Jesse Saran on 6/21/2016.
 */
public class RoofPermissionsManager {

    public enum RoofPermissions {

        /***NOTE IF ADD NEW ENUM, MUST ADD TO valueStringARRAY***/
        ROOF_READ_SMS(READ_SMS, 1),
        ROOF_READ_CONTACTS(READ_CONTACTS, 2),
        ROOF_SEND_SMS(SEND_SMS, 3),
        ROOF_INTERNET(INTERNET,4);

        private final String androidCode;
        private final int requestCode;

        RoofPermissions(String androidCode, int requestCode) {
            this.androidCode = androidCode;
            this.requestCode = requestCode;
        }

        public String getAndroidCode() {
            return androidCode;
        }

        public int getRequestCode() {
            return requestCode;
        }
    }

    public RoofPermissionsManager(){}

    public static boolean hasAllPermissions(Context context) {
        for (RoofPermissions roofPermission : RoofPermissions.values()) {
            if (ContextCompat.checkSelfPermission(context, roofPermission.getAndroidCode()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static ArrayList<RoofPermissions> getMissingPermissions(Context context) {
        ArrayList<RoofPermissions> roofPermissionsManagerArrayList = new ArrayList<RoofPermissions>();
        for (RoofPermissions roofPermission : RoofPermissions.values()) {
            if (ContextCompat.checkSelfPermission(context, roofPermission.getAndroidCode()) != PackageManager.PERMISSION_GRANTED) {
                roofPermissionsManagerArrayList.add(roofPermission);
            }
        }
        return roofPermissionsManagerArrayList;
    }

    public static String [] valueStringArray(Context context, ArrayList<RoofPermissions> roofPermissionsArrayList){
        String [] roofPermissionsArray = new String[roofPermissionsArrayList.size()];
        for(int i = 0; i < roofPermissionsArrayList.size(); ++i) {
            switch(roofPermissionsArrayList.get(i)){
                case ROOF_READ_CONTACTS:
                    roofPermissionsArray[i] = context.getString(R.string.roof_permissions_read_contacts);
                    break;
                case ROOF_READ_SMS:
                    roofPermissionsArray[i] = context.getString(R.string.roof_permissions_read_sms);
                    break;
                case ROOF_SEND_SMS:
                    roofPermissionsArray[i] = context.getString(R.string.roof_permissions_send_sms);
                    break;
                case ROOF_INTERNET:
                    roofPermissionsArray[i] = context.getString(R.string.roof_permissions_internet);
                    break;
                default:
                    roofPermissionsArray[i] = context.getString(R.string.roof_permissions_unknown);
                    break;
            }
        }
        return roofPermissionsArray;
    }

    public static String [] androidCodeStringArray(Context context, ArrayList<RoofPermissions> roofPermissionsArrayList){
        String [] roofPermissionsArray = new String[roofPermissionsArrayList.size()];
        for(int i = 0; i < roofPermissionsArrayList.size(); ++i) {
            roofPermissionsArray[i] = roofPermissionsArrayList.get(i).getAndroidCode();
        }
        return roofPermissionsArray;
    }

    public static String valuesToString(Context context, ArrayList<RoofPermissions> roofPermissionsArrayList){
        CharSequence value = "";
        for(int i = 0; i < roofPermissionsArrayList.size(); ++i) {
            switch(roofPermissionsArrayList.get(i)){
                case ROOF_READ_CONTACTS:
                    value = TextUtils.concat(value, context.getString(R.string.roof_permissions_read_contacts));
                    break;
                case ROOF_READ_SMS:
                    value = TextUtils.concat(value, context.getString(R.string.roof_permissions_read_sms));
                    break;
                case ROOF_SEND_SMS:
                    value = TextUtils.concat(value, context.getString(R.string.roof_permissions_send_sms));
                    break;
                case ROOF_INTERNET:
                    value = TextUtils.concat(value, context.getString(R.string.roof_permissions_internet));
                    break;
                default:
                    value = TextUtils.concat(value, context.getString(R.string.roof_permissions_unknown));
                    break;
            }
            if (i == roofPermissionsArrayList.size()) {
                value = TextUtils.concat(value, ".");
            }
            value = TextUtils.concat(", ");
        }
        return value.toString();
    }

}
