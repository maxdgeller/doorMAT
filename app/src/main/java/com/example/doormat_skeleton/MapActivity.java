package com.example.doormat_skeleton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.doormat_skeleton.databinding.ActivityMapBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.ArCoreApk;

import java.util.List;

public class MapActivity extends DrawerBaseActivity implements OnMapReadyCallback{

    //Initalize variables
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Set FusedLocation
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Initialize map fragment
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5500);
        mLocationRequest.setFastestInterval(3500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //Location Permission already granted
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        } else {
            //Request Location Permission
            checkLocationPermission();
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
            }
        }
    };

    public static final int PERMISSIONS_REQUEST_LOCATION = 101;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle("Please enable location permissions")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }



    // Leaving old code commented out for now, will remove later if location tracking is working

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        activityMapBinding = ActivityMapBinding.inflate(getLayoutInflater());
//        setContentView(activityMapBinding.getRoot());
//        allocateActivityTitle("Map");
//
//        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//
//        if(ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) ==
//                PackageManager.PERMISSION_GRANTED){
//
//            getCurrentLocation();
//
//        }else{
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
//        }
//
//    }
//
//    private void getCurrentLocation() {
//        if(ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) !=
//                PackageManager.PERMISSION_GRANTED){
//            return;
//        }
//
//
//        @SuppressLint("MissingPermission") Task<Location> task = fusedLocationProviderClient.getLastLocation();
//        task.addOnSuccessListener(new OnSuccessListener<Location>() {
//            @Override
//            public void onSuccess(Location location) {
//                if(location != null){
//
//                    mapFragment.getMapAsync(new OnMapReadyCallback() {
//                        @Override
//                        public void onMapReady(@NonNull GoogleMap googleMap) {
//                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You are here");
//
//                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
//
//                            googleMap.addMarker(markerOptions).showInfoWindow();
//
//                        }
//                    });
//
//                }
//            }
//        });
//    }
//
//    @SuppressLint("MissingSuperCall")
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//
//        if(requestCode == REQUEST_CODE){
//            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                getCurrentLocation();
//            }
//        }else{
//            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
//        }
//
//    }
//
//

    public void launchViewMode(View view) {

        Intent i = new Intent(MapActivity.this, ViewMode.class);
        startActivity(i);

    }
}