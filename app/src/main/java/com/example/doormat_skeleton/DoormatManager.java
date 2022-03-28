package com.example.doormat_skeleton;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class DoormatManager {

    JSONObject jsonObject;
    List<UserData.Doormat> doormats;

    //pulls in doormats within a 1000 radius of the lat long doubles provided
    public void getDoormat(Activity activity ,double lat, double lon){
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Starting Write and Read data with URL
                //Creating array for parameters
                String[] field = new String[2];
                field[0] = "latitude";
                field[1] = "longitude";
                //Creating array for data
                String[] data = new String[2];
                data[0] = String.valueOf(lat);
                data[1] = String.valueOf(lon);
                PutData putData = new PutData("http://34.203.214.232/mysite/fetchdoormats.php", "POST", field, data);
                if (putData.startPut()) {
                    if (putData.onComplete()) {
                        String result = putData.getResult();
                        try {
                            jsonObject = new JSONObject(result);
                            extractDoormat(result);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.i("PutData", result);
                    }
                }
                //End Write and Read data with URL
            }
        });
    }

    //ports the response string from the database into a list of doormat objects
    public void extractDoormat(String result) throws JSONException {
        JSONObject obj = new JSONObject(result);
        UserData user_data = (UserData) new Gson().fromJson(obj.toString(), UserData.class);
        doormats = user_data.getData();
    }


}
