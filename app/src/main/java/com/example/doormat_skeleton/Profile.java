package com.example.doormat_skeleton;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class Profile extends DrawerBaseActivity implements DoormatManager.VolleyCallback {

    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";
    ActivityDashboardBinding activityDashboardBinding;
    SessionManager sessionManager;
    DoormatManager doormatManager;
    List<UserData.Doormat> mDoormats;
    String mName;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDashboardBinding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_profile);

        ImageButton backBtn = findViewById(R.id.goBack);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            }
        });

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        mName = user.get(sessionManager.USERNAME);

        TextView setName = findViewById(R.id.usertag);
        setName.setText(mName);

        doormatManager = new DoormatManager();

        doormatManager.getDoormats(this, 28.135974884033203, -82.50953674316406);
        doormatManager.setVolleyCallback(this);

        String connectionsJSONString = getPreferences(MODE_PRIVATE).getString(KEY_CONNECTIONS, null);
        Type type = new TypeToken< List < UserData.Doormat >>() {}.getType();
        mDoormats = new Gson().fromJson(connectionsJSONString, type);

        int placedMats = countMats();

        TextView placed = findViewById(R.id.placed);
        placed.setText(String.valueOf(placedMats));
    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("MYLABEL", result).apply();
        JSONObject obj = new JSONObject(result);

        UserData user_data = (UserData) new Gson().fromJson(obj.toString(), UserData.class);
        List<UserData.Doormat> mDoormats = user_data.getData();
        String connectionsJSONString = new Gson().toJson(mDoormats);
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(KEY_CONNECTIONS, connectionsJSONString);
        editor.commit();
    }

    public int countMats(){
        int placedMats = 0;

        for(UserData.Doormat d: mDoormats){
            if(d.getCreated_by().equals(mName)){
                placedMats++;
            }

        }
        return placedMats;
    }
}