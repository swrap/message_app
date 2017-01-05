package rooftext.rooftextapp.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import java.util.ArrayList;

import rooftext.rooftextapp.R;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.INTERNET;

/**
 * Created by Jesse Saran on 6/21/2016.
 */
public class PermissionsManager {

    private static int MULTIPLE_MISSING_CODE = 1;

    public static void checkPermissions(final AppCompatActivity activity) {
        hasAllPermissions = PermissionsManager.hasAllPermissions(activity.getApplicationContext());
        ArrayList<RoofPermissions> permissionArrayList = getMissingPermissions(activity);
        ArrayList<RoofPermissions> requestDeniedPermanetly = new ArrayList<RoofPermissions>();
        ArrayList<RoofPermissions> requestNormal = new ArrayList<RoofPermissions>();
        for (RoofPermissions roofPermission : permissionArrayList) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, roofPermission.getAndroidCode()) ) {
                requestNormal.add(roofPermission);
            } else {
                requestDeniedPermanetly.add(roofPermission);
            }
        }
        if (!requestNormal.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    PermissionsManager.androidCodeStringArray(activity, requestNormal),
                    MULTIPLE_MISSING_CODE);
        } else if (!requestDeniedPermanetly.isEmpty()){
            AlertDialog.Builder permissionsAlert = new AlertDialog.Builder(activity);
            permissionsAlert.setMessage(R.string.error_permissions)
                    .setPositiveButton(R.string.enable_permissions, new DialogInterface.OnClickListener() {

                        @TargetApi(16)
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", activity.getPackageName(), null));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //TODO: cancelled dialog
                        }
                    });
            permissionsAlert.create();
            permissionsAlert.show();
        }
    }

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

    private static boolean hasAllPermissions = false;

    public PermissionsManager(){}

    public static boolean hasAllPermissions(Context context)  {
        boolean retval = true;
        for (RoofPermissions roofPermission : RoofPermissions.values()) {
            if (ContextCompat.checkSelfPermission(context, roofPermission.getAndroidCode()) != PackageManager.PERMISSION_GRANTED) {
                retval = false;
            }
        }

        hasAllPermissions = retval;
        return retval;
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
