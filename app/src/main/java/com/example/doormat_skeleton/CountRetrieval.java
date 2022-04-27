package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doormat_skeleton.Helpers.DebugHelper;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class CountRetrieval {
   VolleyCallback callback;

    public interface VolleyCallback {
        void onSuccessResponse(String result) throws JSONException;
    }

    public void setVolleyCallback(VolleyCallback callback){
        this.callback = callback;
    }


    public void getUserCount(String username) {
        // url to post our data
        String url = "http://34.203.214.232/mysite/fetchusercount.php";

        Context context = LocationApplication.getContext();

        // creating a new variable for our request queue
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response.contains("Data not found")) {
                    Log.d(TAG, "Data not found");
                    return;
                }
                try {
                    callback.onSuccessResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "onResponse: " + response);

                DebugHelper.showShortMessage(context.getApplicationContext(), "Count retrieved.");
            }
        }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                DebugHelper.showShortMessage(context.getApplicationContext(), "Fail to get response = " + error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("username", username);
                return params;
            }
        };
        queue.add(request);
    }

}
