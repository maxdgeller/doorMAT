package com.example.doormat_skeleton;

import androidx.annotation.RequiresApi;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;


import java.util.HashMap;
import java.util.List;


public class DashboardActivity extends DrawerBaseActivity {


    DoormatManager doormatManager;
    DoormatManager.VolleyCallback callback;
    private TextView username;
    SessionManager sessionManager;
    ActivityDashboardBinding activityDashboardBinding;
    public List<UserData.Doormat> mDoormats;


    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";

    String response;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Dashboard Activity", "onCreate");

        super.onCreate(savedInstanceState);
        activityDashboardBinding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(activityDashboardBinding.getRoot());

        LocationApplication locationApplication = (LocationApplication) getApplication();
        if (!LocationApplication.isFineLocationGranted(this) || !LocationApplication.isBackLocationGranted(this)) {
            locationApplication.permissionsNeededAlert(this);
        }

        username = findViewById(R.id.get_username);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        String mName = user.get(SessionManager.USERNAME);

        username.setText(mName);

        Button mapBtn = findViewById(R.id.mapBtn);

        mapBtn.setOnClickListener(view -> {
            if (LocationApplication.isFineLocationGranted(this) || LocationApplication.isCoarseLocationGranted(this)) {
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
            }
            else {
                locationApplication.foregroundPermissionNeededAlert(this);
            }
        });
    }
}