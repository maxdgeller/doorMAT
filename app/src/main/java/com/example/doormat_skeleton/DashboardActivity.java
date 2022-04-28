package com.example.doormat_skeleton;

import androidx.annotation.RequiresApi;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;


import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class DashboardActivity extends DrawerBaseActivity {


    AnchorRetrieval anchorRetrieval;
    AnchorRetrieval.VolleyCallback callback;
    private TextView username;
    SessionManager sessionManager;
    ActivityDashboardBinding activityDashboardBinding;
    public List<AnchorResult.DatabaseAnchor> mDatabaseAnchors;


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

//        username = findViewById(R.id.get_username);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        String mName = user.get(SessionManager.USERNAME);

//        username.setText(mName);
        TextView welcome = findViewById(R.id.welcome);
        welcome.setText(String.format("%s%s", welcome.getText(), mName));

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

        Button mapTestBtn = findViewById(R.id.maptestbtn);

        mapTestBtn.setOnClickListener(view -> {
            Random rand = new Random();
            StoreManager storeManager = new StoreManager();
            Location location = LocationApplication.getLastLocation();
            for (int i = 0; i < 50; i++) {
                double lat = location.getLatitude() + (((rand.nextDouble()*2) - 1) * MapActivity.metersToDegrees(LocationApplication.SEARCH_RADIUS));
                double lng = location.getLongitude() + (((rand.nextDouble()*2) - 1) * MapActivity.metersToDegrees(LocationApplication.SEARCH_RADIUS));
                storeManager.storeDoormat(this, String.valueOf(i), "black", "sphere", lat, lng, "test_radius");
            }
            for (int i = 50; i < 200; i++) {
                double lat = location.getLatitude() + (((rand.nextDouble()*2) - 1) * MapActivity.metersToDegrees(LocationApplication.ON_MAP_RADIUS));
                double lng = location.getLongitude() + (((rand.nextDouble()*2) - 1) * MapActivity.metersToDegrees(LocationApplication.ON_MAP_RADIUS));
                storeManager.storeDoormat(this, String.valueOf(i), "black", "sphere", lat, lng, "test_radius");
            }
            LocationApplication.setLocationOfSearch(null);
            mapTestBtn.setVisibility(View.GONE);
        });
    }
}