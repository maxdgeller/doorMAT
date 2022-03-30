package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoormatManager {

    JSONObject jsonObject;
    public List<UserData.Doormat> doormats;
    VolleyCallback callback;


    //pulls in doormats within a 1000 radius of the lat long doubles provided
//    public void getDoormat(Activity activity ,double lat, double lon){
//
//        final GlobalClass globalVariable = (GlobalClass) activity.getApplicationContext();
//
//        Handler handler = new Handler();
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                //Starting Write and Read data with URL
//                //Creating array for parameters
//                String[] field = new String[2];
//                field[0] = "latitude";
//                field[1] = "longitude";
//                //Creating array for data
//                String[] data = new String[2];
//                data[0] = String.valueOf(lat);
//                data[1] = String.valueOf(lon);
//                PutData putData = new PutData("http://34.203.214.232/mysite/fetchdoormats.php", "POST", field, data);
//                if (putData.startPut()) {
//                    if (putData.onComplete()) {
//                        String result = putData.getResult();
//                        globalVariable.setResponse(result);
//                        try {
//                            jsonObject = new JSONObject(result);
//                            extractDoormat(result);
//
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        Log.i("PutData", result);
//                    }
//                }
//                //End Write and Read data with URL
//            }
//        });
//    }
//
//    //ports the response string from the database into a list of doormat objects
//    public void extractDoormat(String result) throws JSONException {
//        JSONObject obj = new JSONObject(result);
//        UserData user_data = (UserData) new Gson().fromJson(obj.toString(), UserData.class);
//        doormatSetter(user_data.getData());
//    }
//
//    public void doormatSetter(List<UserData.Doormat> doormats){
//        this.doormats = doormats;
//    }
//
//    public List<UserData.Doormat> doormatGetter(){
//        return doormats;
//    }

    public interface VolleyCallback {
        void onSuccessResponse(String result) throws JSONException;
    }

    public void setVolleyCallback(VolleyCallback callback){
        this.callback = callback;
    }

    public void getDoormats(Activity activity, double lat, double lon) {
        // url to post our data
        String url = "http://34.203.214.232/mysite/fetchdoormats.php";


        // creating a new variable for our request queue
        RequestQueue queue = Volley.newRequestQueue(activity.getApplicationContext());

        // on below line we are calling a string
        // request method to post the data to our API
        // in this we are calling a post method.
        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // inside on response method we are
                // hiding our progress bar
                // and setting data to edit text as empty

                try {
                    callback.onSuccessResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "onResponse: " + response);


                // on below line we are displaying a success toast message.
                Toast.makeText(activity.getApplicationContext(), "Nearby doormats retrieved.", Toast.LENGTH_SHORT).show();
                try {
                    JSONObject respObj = new JSONObject(response);
                    Toast.makeText(activity.getApplicationContext(), respObj.toString(), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // method to handle errors.
                Toast.makeText(activity.getApplicationContext(), "Fail to get response = " + error, Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                return params;
            }
        };

        queue.add(request);
    }




}
