package com.example.doormat_skeleton;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
                             boolean isFound,
                             double lat,
                             double lon,
                             String username,
                             String color,
                             String shape) {
        int foundVal;

        if(isFound= true){foundVal = 1;} else {foundVal=0;}

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[6];
                field[0] = "doormat_id";
                field[1] = "latitude";
                field[2] = "longitude";
                field[3] = "created_by";
                field[4] = "shape";
                field[5] = "color";
                //Creating array for data
                String[] data = new String[6];
                data[0] = cloudAnchorId;
                data[1] = String.valueOf(lat);
                data[2] = String.valueOf(lon);
                data[3] = username;
                data[4] = shape;
                data[5] = color;
                PutData putData = new PutData("http://34.203.214.232/mysite/addmarker2.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        if(result.equals("Doormat saved successfully")){
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
