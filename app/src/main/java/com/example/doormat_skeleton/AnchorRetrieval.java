package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doormat_skeleton.Helpers.DebugHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnchorRetrieval {

    VolleyCallback callback;

    public interface VolleyCallback {
        void onSuccessResponse(String result) throws JSONException;
    }

    public void setVolleyCallback(VolleyCallback callback){
        this.callback = callback;
    }

    public void getAnchors(Context context, double lat, double lon, double radius) {
        // url to post our data
        String url = "http://34.203.214.232/mysite/fetchanchors.php";

        // creating a new variable for our request queue
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        StringRequest request = new StringRequest(Request.Method.POST, url, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if(response.contains("Data not found")){
                    DebugHelper.showShortMessage(context.getApplicationContext(), "Data not found");
                    return;
                }
                try {
                    callback.onSuccessResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "onResponse: " + response);

                DebugHelper.showShortMessage(context.getApplicationContext(), "Nearby anchors retrieved.");
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
                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("radius", String.valueOf(radius));
                return params;
            }
        };

        queue.add(request);
    }


}
