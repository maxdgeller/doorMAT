package com.example.doormat_skeleton;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

public class Profile extends DrawerBaseActivity implements AnchorRetrieval.VolleyCallback {

    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";
    ActivityDashboardBinding activityDashboardBinding;
    SessionManager sessionManager;
    AnchorRetrieval anchorRetrieval;
    HashSet<AnchorResult.DatabaseAnchor> mDatabaseAnchors;
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

        anchorRetrieval = new AnchorRetrieval();

        anchorRetrieval.getAnchors(this, 28.135974884033203, -82.50953674316406, LocationApplication.SEARCH_RADIUS);
        anchorRetrieval.setVolleyCallback(this);

//        String connectionsJSONString = getPreferences(MODE_PRIVATE).getString(KEY_CONNECTIONS, null);
//        Type type = new TypeToken< List < UserData.Doormat >>() {}.getType();
//        mDoormats = new Gson().fromJson(connectionsJSONString, type);

        int placedMats = countMats();

        TextView placed = findViewById(R.id.placed);
        placed.setText(String.valueOf(placedMats));
    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("MYLABEL", result).apply();
        JSONObject obj = new JSONObject(result);

        AnchorResult user_data = (AnchorResult) new Gson().fromJson(obj.toString(), AnchorResult.class);
        mDatabaseAnchors = user_data.getData();

    }

    public int countMats(){
        int placedMats = 0;
        if(mDatabaseAnchors != null) {
            for (AnchorResult.DatabaseAnchor d : mDatabaseAnchors) {
                if (d.getCreated_by().equals(mName)) {
                    placedMats++;
                }

            }
        }
        return placedMats;
    }
}