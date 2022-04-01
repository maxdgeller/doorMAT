package com.example.doormat_skeleton;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;

public class Friends extends DrawerBaseActivity  {

    ActivityDashboardBinding activityDashboardBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_friends);
    }


}