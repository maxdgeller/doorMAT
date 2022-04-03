package com.example.doormat_skeleton;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.doormat_skeleton.Helpers.GeofenceHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapActivity extends DrawerBaseActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, DoormatManager.VolleyCallback {

    private static final String TAG = "MapActivity";

    //Initialize variables
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    FusedLocationProviderClient mFusedLocationClient;
    boolean isPlaced = false;
    float DEFAULT_LATITUDE;
    float DEFAULT_LONGITUDE;
    float DEFAULT_ZOOM;
    SharedPreferences userPos;
    DoormatManager doormatManager;

    List<UserData.Doormat> mDoormats;

    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS"; //used for storing doormats from database into sharedpreferences

    private int VIEW_MODE_REQUEST_CODE = 1;
    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;

    //Initialize geofencing variables
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private final String GEOFENCE_ID = "SOME_GEOFENCE_ID"; //placeholder

    private final float GEOFENCE_RADIUS = 50; //radius around anchor within which a notification is triggered
    private final float SEARCH_RADIUS = 1000; //radius around user from which to get nearby cloud anchors' coordinates from database
    private final float ON_MAP_RADIUS = 250; //radius around user in which nearby cloud anchors and their geofences will be marked on the map

    private Set<LatLng> nearbyAnchorLatLngs = new HashSet<LatLng>(); //set of LatLngs of anchors within SEARCH_RADIUS of the user
    private Set<String> nearbyAnchorLatLngStrings = new HashSet<String>();
    Set<String> DEFAULT_SET = new HashSet<String>();
    private Location locationOfSearch; //location of previous search

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        userPos = getSharedPreferences("UserPosition", Context.MODE_PRIVATE);
        doormatManager = new DoormatManager();

        //Set FusedLocation
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Initialize map fragment
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        //getGeofencingClient returns a GeofencingClient instance
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);


    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();

        nearbyAnchorLatLngStrings.clear();
        for (LatLng latLng : nearbyAnchorLatLngs) {
            nearbyAnchorLatLngStrings.add(latLng.toString());
        }
        editor.putStringSet("nearby", nearbyAnchorLatLngStrings);

        editor.apply();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        if(checkLocationPermission()){
            mGoogleMap.setMyLocationEnabled(true);
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5500);
        mLocationRequest.setFastestInterval(3500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        double longitude = sharedPref.getFloat("longitude", DEFAULT_LONGITUDE);
        double latitude = sharedPref.getFloat("latitude", DEFAULT_LATITUDE);
        float zoom = sharedPref.getFloat("zoom", DEFAULT_ZOOM);
        LatLng startPosition = new LatLng(latitude, longitude);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(startPosition)
                .zoom(zoom)
                .build();
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        mGoogleMap.setOnMapLongClickListener(this);

    }

    // executes inside of onLocationResult,
    // gets new anchors from the database if the user has traveled SEARCH_RADIUS - ON_MAP_RADIUS away from previous search,
    // or if nearbyAnchorLatLngs is empty
    private void searchIfNewLocation(Location lastLocation, Location newLocation) {

        if (nearbyAnchorLatLngs.isEmpty()) {
            locationOfSearch = newLocation;
            //execute a function that gets anchors within SEARCH_RADIUS from database
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            double longitude = sharedPref.getFloat("longitude", DEFAULT_LONGITUDE);
            double latitude = sharedPref.getFloat("latitude", DEFAULT_LATITUDE);
            doormatManager = new DoormatManager();
            doormatManager.setVolleyCallback(this);
            doormatManager.getDoormats(this,latitude, longitude);

            String connectionsJSONString = getPreferences(MODE_PRIVATE).getString(KEY_CONNECTIONS, null);
            Type type = new TypeToken< List < UserData.Doormat >>() {}.getType();
            mDoormats = new Gson().fromJson(connectionsJSONString, type);

            return;
        }

        double lat1 = lastLocation.getLatitude();
        double lat2 = newLocation.getLatitude();
        double lon1 = lastLocation.getLongitude();
        double lon2 = newLocation.getLongitude();
        double el1 = lastLocation.getAltitude();
        double el2 = newLocation.getAltitude();

        boolean isNewLocation = distance(lat1, lat2, lon1, lon2, el1, el2) >= SEARCH_RADIUS - ON_MAP_RADIUS;

        if (isNewLocation) {
            locationOfSearch = newLocation;
            //execute a function that gets anchors within SEARCH_RADIUS from database
            doormatManager = new DoormatManager(); // call manager
            doormatManager.setVolleyCallback(this); // set this context to listen for provide response from callback
            doormatManager.getDoormats(this, lat2, lon2); // pulls in response from database to retrieve doormats within 1000 radius of lat/lng
            //sets the retrieved doormats within 1000 radius of supplied latlong to
            //a list of doormat objects declared globally
            String connectionsJSONString = getPreferences(MODE_PRIVATE).getString(KEY_CONNECTIONS, null);
            Type type = new TypeToken< List < UserData.Doormat >>() {}.getType();
            mDoormats = new Gson().fromJson(connectionsJSONString, type); // sets global variable to list of doormat objects. see UserData class for getters
            nearByAnchorLatLngSetter(); //method to set list of nearby anchors to lat/lngs of retrieved doormat objects
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
                mLastLocation = location;

                //move map camera
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

                // search from database if far enough away
                searchIfNewLocation(mLastLocation, location);

            }
        }
    };

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//            mGoogleMap.setMyLocationEnabled(true);
            return true;
        }
        else {
            if (mFusedLocationClient != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
            mGoogleMap.setMyLocationEnabled(false);
            requestLocationPermission();
            return false;
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Please enable location permissions")
                    .setMessage("This app needs the Location permission, please accept to use location functionality")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                        }
                    })
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        }
    }

    private boolean checkBackgroundLocationPermission() {
        if ((Build.VERSION.SDK_INT >= 29) & ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else if ((Build.VERSION.SDK_INT >= 29) & ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestBackgroundLocationPermission();
            return false;
        }
        else {
            return true;
        }
    }

    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= 29) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Please enable background location permissions")
                            .setMessage("SDK versions 29 and above require background location permission")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Prompt the user once explanation has been shown
                                    ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                                }
                            })
                            .create()
                            .show();
                } else {
                    ActivityCompat.requestPermissions(MapActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FINE_LOCATION_ACCESS_REQUEST_CODE: {

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
            case BACKGROUND_LOCATION_ACCESS_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Geofencing enabled", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Background location access is necessary for geofences to trigger", Toast.LENGTH_LONG).show();
                }
            }

        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        CameraPosition mMyCam = mGoogleMap.getCameraPosition();
        double longitude = mMyCam.target.longitude;
        double latitude = mMyCam.target.latitude;
        float zoom = mMyCam.zoom;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("longitude", (float) longitude);
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("zoom", zoom);

        nearbyAnchorLatLngStrings.clear();
        for (LatLng latLng : nearbyAnchorLatLngs) {
            nearbyAnchorLatLngStrings.add(latLng.toString());
        }
        editor.putStringSet("nearby", nearbyAnchorLatLngStrings);

        editor.apply();

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        double longitude = sharedPref.getFloat("longitude", DEFAULT_LONGITUDE);
        double latitude = sharedPref.getFloat("latitude", DEFAULT_LATITUDE);
        LatLng latLng = new LatLng(latitude, longitude);

        nearbyAnchorLatLngStrings = sharedPref.getStringSet("nearby", DEFAULT_SET);


        nearbyAnchorLatLngs.clear();
        for (String latLngStr : nearbyAnchorLatLngStrings) {
            nearbyAnchorLatLngs.add(stringToLatLng(latLngStr));
        }
        if (!nearbyAnchorLatLngs.isEmpty()) {
            addGeofences((HashSet<LatLng>) nearbyAnchorLatLngs);
        }



        //i think we won't need this code anymore
//        if (isPlaced) {
//            addMarker(latLng);
//            addCircle(latLng, GEOFENCE_RADIUS, "blue");
//            addGeofence(latLng, GEOFENCE_RADIUS);
//        }

    }

    // temporary way to easily add LatLngs to the set and add new geofences for testing purposes
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        HashSet<LatLng> before = new HashSet<LatLng>(nearbyAnchorLatLngs);
        nearbyAnchorLatLngs.add(latLng);
        HashSet<LatLng> after = new HashSet<LatLng>(nearbyAnchorLatLngs);
        after.removeAll(before);

        addGeofences(after);
    }

    //takes a string that resulted from LatLng.toString() and turns it back into LatLng
    private LatLng stringToLatLng(String latLongStr) {
        latLongStr = latLongStr.substring(11, latLongStr.length() - 1);

        String[] latLng =  latLongStr.split(",");
        double latitude = Double.parseDouble(latLng[0]);
        double longitude = Double.parseDouble(latLng[1]);

        return new LatLng(latitude, longitude);
    }

    //returns distance in meters. shamelessly stolen from stackoverflow, not sure if it will work
    public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    //need a method for getting the latlngs of anchors from the database that are within SEARCH_RADIUS of the user


    // in the future, this method will be called when the app searches for anchors in the database that are within SEARCH_RADIUS of the user,
    // and its HashSet<LatLng> newLatLngs parameter contains the LatLngs of geofences which were not already added to the geofencing client or placed on the map.
    private void addGeofences(HashSet<LatLng> newLatLngs) {

        if (!checkLocationPermission() | !checkBackgroundLocationPermission()) {
            checkLocationPermission();
            checkBackgroundLocationPermission();
        }

        List<Geofence> geofenceList = new ArrayList<Geofence>();
        for (LatLng latLng : newLatLngs) {
            geofenceList.add(geofenceHelper.getGeofence(GEOFENCE_ID, latLng, GEOFENCE_RADIUS,
                    Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT));
        }

        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofenceList);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Geofence(s) added");
                        //mGoogleMap.clear();
                        for (LatLng latLng : newLatLngs) {
                            addCircle(latLng, GEOFENCE_RADIUS, "blue");
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    //need to make another method for removing the geofences whose centers are no longer within SEARCH_RADIUS of the user
    private void nearByAnchorLatLngSetter(){
        if(nearbyAnchorLatLngs != null){
            if(mDoormats!= null){
                for(UserData.Doormat d: mDoormats) {
                    double lat = d.getLatitude();
                    double lon = d.getLongitude();
                    LatLng latLng = new LatLng(lat, lon);
                    nearbyAnchorLatLngs.add(latLng);
                }
            }
        }
    }


    private void addMarker(LatLng latLng){
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        mGoogleMap.addMarker(markerOptions);

    }

    private void addCircle(LatLng latLng, float radius){
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255,0,0, 255));
        circleOptions.fillColor(Color.argb(65,0,0, 255));
        circleOptions.strokeWidth(4);
        mGoogleMap.addCircle(circleOptions);
    }

    private void addCircle(LatLng latLng, float radius, String colorString) {
        //per https://developer.android.com/reference/android/graphics/Color.html, color strings accepted by Color.parseColor are:
        //red, blue, green, black, white, gray, cyan, magenta, yellow, lightgray, darkgray, aqua, fuchsia, lime, maroon, navy, olive, purple, silver, teal
        //or hexadecimal strings of the format #RRGGBB or #AARRGGBB

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);

        int color = Color.parseColor(colorString);
        circleOptions.strokeColor(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
        circleOptions.fillColor(Color.argb(55, Color.red(color), Color.green(color), Color.blue(color)));
        circleOptions.strokeWidth(5);

        mGoogleMap.addCircle(circleOptions);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VIEW_MODE_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                isPlaced = true;

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                isPlaced = false;
            }
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("MYLABEL", result).apply();
        JSONObject obj = new JSONObject(result);

        UserData user_data = (UserData) new Gson().fromJson(obj.toString(), UserData.class);
        List<UserData.Doormat> mDoormats = user_data.getData();
        String connectionsJSONString = new Gson().toJson(mDoormats);
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(KEY_CONNECTIONS, connectionsJSONString);
        editor.commit();

    }
}