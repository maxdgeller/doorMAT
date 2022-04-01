package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.example.doormat_skeleton.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import org.json.JSONException;
import org.json.JSONObject;


import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DashboardActivity extends DrawerBaseActivity {


    DoormatManager doormatManager;
    DoormatManager.VolleyCallback callback;
    private TextView username;
    SessionManager sessionManager;
    ActivityDashboardBinding activityDashboardBinding;
    public List<UserData.Doormat> mDoormats;


    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";

    String response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        activityDashboardBinding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(activityDashboardBinding.getRoot());

        username = findViewById(R.id.get_username);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        String mName = user.get(sessionManager.USERNAME);

        username.setText(mName);

        Button mapBtn = findViewById(R.id.mapBtn);

        mapBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        });

    }

}