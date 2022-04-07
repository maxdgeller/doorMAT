package com.example.doormat_skeleton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;

public class MapActivity extends DrawerBaseActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    //Initialize variables
    private static GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    float DEFAULT_LATITUDE = (float) 0;
    float DEFAULT_LONGITUDE = (float) 0;
    float DEFAULT_ZOOM = 14;

    LocationApplication locationApplication;

    private final int VIEW_MODE_REQUEST_CODE = 1;
//    boolean isPlaced;

    //we may want this constants again if we re-implement in-app location permission requests
    //i temporarily removed location requests to make debugging simpler, but we can add them again if we want
//    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
//    private final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Map Activity", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Initialize map fragment
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFrag != null;
        mapFrag.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        Log.i("Map Activity", "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i("Map Activity", "onResume");
        super.onResume();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.i("Map Activity", "onMapReady");
        mGoogleMap = googleMap;

        locationApplication = (LocationApplication) getApplication();
        loadCameraLocation();
        locationApplication.updateCircles(googleMap, new HashSet<UserData.Doormat>());

        if ((!googleMap.isMyLocationEnabled()) && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            googleMap.setMyLocationEnabled(true);
        }
        else if (googleMap.isMyLocationEnabled()) {
            googleMap.setMyLocationEnabled(false);
        }
    }

    @Override
    public void onPause() {
        Log.i("Map Activity", "onPause");

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i("Map Activity", "onStop");

        super.onStop();
    }

//    //save location
//    private void saveLocations() {
//
//        if (mLastLocation == null) {
//            mLastLocation = new Location("");
//        }
//
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor editor = sharedPref.edit();
//
//        editor.putFloat("user longitude", (float) mLastLocation.getLongitude());
//        editor.putFloat("user latitude", (float) mLastLocation.getLatitude());
//
//        if (mGoogleMap != null) {
//            CameraPosition mMyCam = mGoogleMap.getCameraPosition();
//            editor.putFloat("camera longitude", (float) mMyCam.target.longitude);
//            editor.putFloat("camera latitude", (float) mMyCam.target.latitude);
//            editor.putFloat("camera zoom", mMyCam.zoom);
//        }
//        editor.apply();
//    }
    public static GoogleMap getMap() {
        return mGoogleMap;
    }

    private void loadCameraLocation() {
        if (mGoogleMap != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            double longitude;
            double latitude;
            float zoom = sharedPref.getFloat("camera zoom", DEFAULT_ZOOM);

            Location location = locationApplication.getLastLocation();
            if (location != null) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }
            else {
                longitude = sharedPref.getFloat("camera longitude", DEFAULT_LONGITUDE);
                latitude = sharedPref.getFloat("camera latitude", DEFAULT_LATITUDE);
            }

            LatLng startPosition = new LatLng(latitude, longitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(startPosition)
                    .zoom(zoom)
                    .build();
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public void launchViewMode(View view) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        double longitude = sharedPref.getFloat("longitude", DEFAULT_LONGITUDE);
        double latitude = sharedPref.getFloat("latitude", DEFAULT_LATITUDE);

        LatLng latLng = new LatLng(latitude, longitude);
        Intent i = new Intent(MapActivity.this, ViewMode.class);
        i.putExtra("LatLng", latLng);
        startActivityForResult(i, VIEW_MODE_REQUEST_CODE);
    }

    public void centerOnUserOnClick(View view) {
        centerOnUser();
    }

    public void centerOnUser() {
        Location location = locationApplication.getLastLocation();
        if ((location != null) && (mGoogleMap != null)) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            LatLng user = new LatLng(latitude, longitude);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(user));
        }
    }

    //do we still need this variable?
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == VIEW_MODE_REQUEST_CODE) {
//            if(resultCode == Activity.RESULT_OK){
//                isPlaced = true;
//            }
//            if (resultCode == Activity.RESULT_CANCELED) {
//                isPlaced = false;
//            }
//        }
//    }

      //we could probably rewrite this method to make it useful for debugging again
//    @Override
//    public void onMapLongClick(@NonNull LatLng latLng) {
//
//    }
}