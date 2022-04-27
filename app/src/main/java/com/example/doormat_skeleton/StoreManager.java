package com.example.doormat_skeleton;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

public class StoreManager {
    private static final String NEXT_SHORT_CODE = "next_short_code";
    private static final String KEY_PREFIX = "anchor;";
    private static final int INITIAL_SHORT_CODE = 142;

    /** Gets a new short code that can be used to store the anchor ID. */
//    int nextShortCode(Activity activity) {
//        SharedPreferences sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE);
//        int shortCode = sharedPrefs.getInt(NEXT_SHORT_CODE, INITIAL_SHORT_CODE);
//        // Increment and update the value in sharedPrefs, so the next code retrieved will be unused.
//        sharedPrefs.edit().putInt(NEXT_SHORT_CODE, shortCode + 1)
//                .apply();
//        return shortCode;
//    }

//    /** Stores the cloud anchor ID in the activity's SharedPrefernces. */
//    void storeUsingShortCode(Activity activity, int doormat_id,
//                             String cloudAnchorId,
//                             boolean isFound,
//                             double lat,
//                             double lon,
//                             String username,
//                             String color,
//                             String shape) {
//        int foundVal;
//        SharedPreferences sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE);
//        sharedPrefs.edit().putString(KEY_PREFIX + doormat_id, cloudAnchorId).apply();
//
//        if(isFound= true){foundVal = 1;} else {foundVal=0;}
//
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                //Starting Write and Read data with URL
//                //Creating array for parameters
//                String[] field = new String[6];
//                field[0] = "doormat_id";
//                field[1] = "latitude";
//                field[2] = "longitude";
//                field[3] = "created_by";
//                field[4] = "shape";
//                field[5] = "color";
//                //Creating array for data
//                String[] data = new String[6];
//                data[0] = String.valueOf(doormat_id);
//                data[1] = String.valueOf(lat);
//                data[2] = String.valueOf(lon);
//                data[3] = username;
//                data[4] = shape;
//                data[5] = color;
//                PutData putData = new PutData("http://34.203.214.232/mysite/addmarker.php", "POST", field, data);
//                if (putData.startPut()) {
//                    if (putData.onComplete()) {
//                        String result = putData.getResult();
//                        if(result.equals("Doormat saved successfully")){
//                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
//                        }else{
//                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
//                        }
//                        Log.i("PutData", result);
//                    }
//                }
//                //End Write and Read data with URL
//            }
//        });
//
//    }


    void storeDoormat(Activity activity,
                             String cloudAnchorId,
                             String color,
                             String shape,
                             double lat,
                             double lon,
                             String username) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[6];
                field[0] = "anchor_id";
                field[1] = "color";
                field[2] = "shape";
                field[3] = "latitude";
                field[4] = "longitude";
                field[5] = "created_by";
                //Creating array for data
                String[] data = new String[6];
                data[0] = cloudAnchorId;
                data[1] = color;
                data[2] = shape;
                data[3] = String.valueOf(lat);
                data[4] = String.valueOf(lon);
                data[5] = username;

                PutData putData = new PutData("http://34.203.214.232/mysite/addanchor.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        if(result.equals("Anchor saved successfully")){
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }
                        Log.i("PutData", result);
                    }
                }
                //End Write and Read data with URL
            }
        });

    }

    void storeChildNode(Activity activity,
                        String cloudAnchorId,
                        String color,
                        String shape,
                        Vector3 position,
                        Vector3 scale,
                        Quaternion rotation) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[13];
                field[0] = "anchor_id";
                field[1] = "color";
                field[2] = "shape";
                field[3] = "position_vx";
                field[4] = "position_vy";
                field[5] = "position_vz";
                field[6] = "scale_vx";
                field[7] = "scale_vy";
                field[8] = "scale_vz";
                field[9] = "rotation_qx";
                field[10] = "rotation_qy";
                field[11] = "rotation_qz";
                field[12] = "rotation_qw";
                //Creating array for data
                String[] data = new String[13];
                data[0] = cloudAnchorId;
                data[1] = color;
                data[2] = shape;
                data[3] = String.valueOf(position.x);
                data[4] = String.valueOf(position.y);
                data[5] = String.valueOf(position.z);
                data[6] = String.valueOf(scale.x);
                data[7] = String.valueOf(scale.y);
                data[8] = String.valueOf(scale.z);
                data[9] = String.valueOf(rotation.x);
                data[10] = String.valueOf(rotation.y);
                data[11] = String.valueOf(rotation.z);
                data[12] = String.valueOf(rotation.w);

                PutData putData = new PutData("http://34.203.214.232/mysite/addchild.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        if(result.equals("Anchor saved successfully")){
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }
                        Log.i("PutData", result);
                    }
                }
                //End Write and Read data with URL
            }
        });

    }

    public void updateEmail(Activity activity, String username, String email){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[2];
                field[0] = "username";
                field[1] = "email";
                //Creating array for data
                String[] data = new String[2];
                data[0] = username;
                data[1] = email;
                PutData putData = new PutData("http://34.203.214.232/mysite/updateEmail.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        if(result.equals("Email updated successfully")){
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }
                        Log.i("PutData", result);
                    }
                }
                //End Write and Read data with URL
            }
        });
    }

    public void updatePassword(Activity activity, String username, String password){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[2];
                field[0] = "username";
                field[1] = "password";
                //Creating array for data
                String[] data = new String[2];
                data[0] = username;
                data[1] = password;
                PutData putData = new PutData("http://34.203.214.232/mysite/updatePassword.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        if(result.equals("Password updated successfully")){
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                        }
                        Log.i("PutData", result);
                    }
                }
                //End Write and Read data with URL
            }
        });
    }


    /**
     * Retrieves the cloud anchor ID using a short code. Returns an empty string if a cloud anchor ID
     * was not stored for this short code.
     */
    String getCloudAnchorID(Activity activity, int shortCode) {
        SharedPreferences sharedPrefs = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPrefs.getString(KEY_PREFIX + shortCode, "");
    }

}
