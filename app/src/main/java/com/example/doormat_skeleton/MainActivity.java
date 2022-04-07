package com.example.doormat_skeleton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //Initializing variables
    private EditText textUsername, textPassword;
    private Button loginBtn;
    private ProgressBar progressBar;
    SessionManager sessionManager;

    Location mLastLocation;

    private final static int HIGH_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    public final static LocationRequest FOREGROUND_LOCATIONREQUEST = LocationRequest.create()
            .setInterval(5500)
            .setFastestInterval(3500)
            .setPriority(HIGH_PRIORITY);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Main Activity", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Creates a session manager to track user information for display
        sessionManager = new SessionManager(this);

        //Setting variables to text edit lines
        textUsername = findViewById(R.id.loginID);
        textPassword = findViewById(R.id.editTextTextPassword);
        loginBtn = findViewById(R.id.loginBtn);
        progressBar = findViewById(R.id.progress_bar);

        //On click listener for login button, attempts to log in user
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Main Activity", "login onClick");

                //Getting strings from user input
                String username, password, email;
                username = String.valueOf(textUsername.getText());
                password = String.valueOf(textPassword.getText());


                //Check if user has entered values, if not display Toast message
                if(!username.equals("") && !password.equals("")) {
                    progressBar.setVisibility(View.VISIBLE);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run");

                            //Starting Write and Read data with URL
                            //Creating array for parameters
                            String[] field = new String[2];
                            field[0] = "username";
                            field[1] = "password";
                            //Creating array for data
                            String[] data = new String[2];
                            data[0] = username;
                            data[1] = password;
                            PutData putData = new PutData("http://34.203.214.232/mysite/login.php", "POST", field, data);
                            if (putData.startPut()) {
                                if (putData.onComplete()) {
                                    progressBar.setVisibility(View.GONE);
                                    String result = putData.getResult();
                                    if(result.equals("Login Success")){
                                        sessionManager.createSession(username);

//                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }else{
//                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                    }
                                    Log.i("PutData", result);
                                }
                            }
                            //End Write and Read data with URL
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {

            mLastLocation = locationResult.getLastLocation();
            Log.i(TAG, "onLocationResult: " + mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());
//
//            manageLocationUpdates(true);
//
//            boolean isNewLocation;
//            if (locationOfSearch == null) {
//                isNewLocation = true;
//            } else {
//                isNewLocation = distance(locationOfSearch, mLastLocation, false) >= SEARCH_RADIUS - ON_MAP_RADIUS;
//            }
//
//            if (isNewLocation) {
//                // search from database if far enough away or no search performed yet
//                searchForDoormats();
//            }
//            else {
//                GoogleMap map = MapActivity.getMap();
//                if (map != null) {
//                    updateCirclesVisibility(map);
//                }
//            }
        }
    };

    public void launchMap(){
        Intent i = new Intent(this, DashboardActivity.class);
        startActivity(i);
    }
    public void launchSignUp(View V){
        Intent i = new Intent(this, SignUp.class);
        startActivity(i);
    }

}