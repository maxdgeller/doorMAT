package com.example.doormat_skeleton;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.example.doormat_skeleton.SessionManager;

import java.util.HashMap;


public class DashboardActivity extends DrawerBaseActivity {

    private TextView username;
    SessionManager sessionManager;
    ActivityDashboardBinding activityDashboardBinding;



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

    }
}