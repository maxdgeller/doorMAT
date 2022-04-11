package com.example.doormat_skeleton;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.doormat_skeleton.Helpers.GeofenceHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

//should only lose its state if the application ends unexpectedly
public class LocationApplication extends Application implements Application.ActivityLifecycleCallbacks, DoormatManager.VolleyCallback {

    private static final String TAG = "LocationApplication";

    /*************** Default values for sharedPref keys ***************/

    //

    /********************** Location constants ************************/

    private final static int HIGH_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private final static int MID_PRIORITY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private final static int LOW_PRIORITY = LocationRequest.PRIORITY_LOW_POWER;
    private final static int GRANTED = PackageManager.PERMISSION_GRANTED;

    private final float SEARCH_RADIUS = 1000; //radius around user from which to get nearby cloud anchors' coordinates from database
    private final float ON_MAP_RADIUS = 750; //radius around user in which nearby cloud anchors and their geofences will be marked on the map

    private final static LocationRequest FOREGROUND_LOCATIONREQUEST = LocationRequest.create()
            .setInterval(5500)
            .setFastestInterval(3500)
            .setPriority(HIGH_PRIORITY);
    private final static LocationRequest BACKGROUND_LOCATIONREQUEST = LocationRequest.create()
            .setInterval(15000) //just fast enough that an average cyclist probably wouldn't miss a geofence
            .setFastestInterval(10000)
            .setPriority(MID_PRIORITY);

    /********************** Location variables ************************/

    public FusedLocationProviderClient locationClient;
    public HashSet<Circle> circles = new HashSet<Circle>();

    private Location mLastLocation = null;
    private Location locationOfSearch = null;
    private static boolean locationUpdatesActive = false;
    private static LocationRequest activeLocationRequest = null;
    private static LocationRequest desiredLocationRequest = BACKGROUND_LOCATIONREQUEST;

    /********************************* geofence constants **********************************/

    private final float GEOFENCE_RADIUS = 200; //radius around anchor within which a notification is triggered

    /********************************* geofence variables **********************************/

    private GeofencingClient geoClient;
    private GeofenceHelper geoHelper;

    /*************** Database stuff, otherwise known as 'merciless torment' ****************/

    DoormatManager doormatManager = new DoormatManager();
    HashSet<UserData.Doormat> doormats = new HashSet<UserData.Doormat>();

    /************************************ App lifecycle ************************************/

    private static final ArrayList<String> foregroundActivities = new ArrayList<String>();
    private static final AtomicBoolean appIsInBackground = new AtomicBoolean(true);

    //adjust WAIT_INTERVAL if the app ever seems confused about whether it's running in the background
    private static final long WAIT_INTERVAL = 600L;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        registerActivityLifecycleCallbacks(this);

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        geoClient = LocationServices.getGeofencingClient(this);
        geoHelper = new GeofenceHelper(this);

        manageLocationUpdates();
    }

    //the following methods are called when lifecycle methods of other activities are called
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        foregroundActivities.add(activity.getClass().getSimpleName());
        determineForegroundStatus();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        foregroundActivities.remove(activity.getClass().getSimpleName());
        determineBackgroundStatus();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.i(TAG, String.valueOf("onActivityDestroyed"));
    }

    private void determineBackgroundStatus() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "foregroundActivities.size() = " + String.valueOf(foregroundActivities.size()));
//                if(!appIsInBackground.get() && currentActivityReference == null) {
                if(!appIsInBackground.get() && foregroundActivities.size() == 0) {
                    Log.i(TAG, "IN BACKGROUND");
                    appIsInBackground.set(true);
                    desiredLocationRequest = BACKGROUND_LOCATIONREQUEST;
                    manageLocationUpdates();
                }
            }
        }, WAIT_INTERVAL);
    }
    private void determineForegroundStatus() {
        if (appIsInBackground.get()) {
            Log.i(TAG, "IN FOREGROUND (" + foregroundActivities.get(foregroundActivities.size() - 1) + ")");
            appIsInBackground.set(false);
            desiredLocationRequest = FOREGROUND_LOCATIONREQUEST;
            manageLocationUpdates();
        }
    }

    public static AtomicBoolean getAppIsInBackground() {
        return appIsInBackground;
    }

    private void manageLocationUpdates() {
        Log.i(TAG, "manageLocationUpdates");
        Task<Void> task;

        GoogleMap map = MapActivity.getMap();
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == GRANTED;
        boolean background = true;
        if (Build.VERSION.SDK_INT >= 29) {
            background = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == GRANTED;
        }

        if ((fine || coarse) && !(appIsInBackground.get() && !background)) {
            if ((locationUpdatesActive) && (activeLocationRequest != desiredLocationRequest)) {
                task = locationClient.removeLocationUpdates(mLocationCallback);
                locationUpdatesActive = !task.isSuccessful();
            }
            task = locationClient.requestLocationUpdates(desiredLocationRequest, mLocationCallback, Looper.myLooper());
            locationUpdatesActive = task.isSuccessful();
            activeLocationRequest = desiredLocationRequest;
            if ((map != null) && !map.isMyLocationEnabled()) {
                map.setMyLocationEnabled(true);
            }
        }
        else {
            task = locationClient.removeLocationUpdates(mLocationCallback);
            locationUpdatesActive = !task.isSuccessful();
            if ((map != null) && map.isMyLocationEnabled()) {
                map.setMyLocationEnabled(false);
            }
        }
    }

    //execute this instead of in-app permission dialogs
    private void navigateToAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //new activity instead of currently running
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    //for getting accurate location in a pinch, without waiting for the Looper
    //only update mLastLocation if the current value is null or some milliseconds old or older,
    //and if location permissions are granted.
    public Location getCurrentLocation(long milliseconds) {
        Log.i(TAG, "getCurrentLocation");
        boolean outdated = false;
        if (mLastLocation != null) {

            long now = SystemClock.elapsedRealtimeNanos();
            long before = mLastLocation.getElapsedRealtimeNanos();

            if (now - before >= milliseconds * 1000000) {
                outdated = true;
            }
        }
        if (outdated || mLastLocation == null) {

            boolean coarse = false;
            boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!fine) {
                coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            }

            if (fine || coarse) {
                Task<Location> locationTask = locationClient.getCurrentLocation(HIGH_PRIORITY, new CancellationToken() {
                    @NonNull
                    @Override
                    public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                        Log.i(TAG, "onCanceledRequest");
                        return null;
                    }

                    @Override
                    public boolean isCancellationRequested() {
                        return false;
                    }
                });
                if (locationTask.isSuccessful()) {
                    Location location = locationTask.getResult();
                    //if it results in a null value, it returns null and does not update mLastLocation.
                    if (location != null) {
                        mLastLocation = location;
                        Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                        return location;
                    }
                }
                else {
                    Log.i(TAG, "locationTask.isSuccessful() returned false");
                }
            }
            else {
                Log.i(TAG, "Insufficient location permissions.");
            }
        }
        Log.i(TAG, "Previous location not null or outdated.");
        return null;
    }

    //updates mLastLocation even if the result is null;
    //use this when we would rather have a brand new null than an out of date location.
    //null-related problems are quicker to cause exceptions; outdated locations would be harder to fix.
    public Location forceNewLocation() {
        mLastLocation = getCurrentLocation(0);
        return mLastLocation;
    }

    public Location getLastLocation() {
        return mLastLocation;
    }

    //an estimation that should be precise enough for the small distances we're using
    public static double distance(double lat1, double lat2, double lng1, double lng2) {
        return Math.sqrt(Math.pow(latDistance(lat1, lat2), 2) +
                Math.pow(lngDistance(lng1, lng2, lat1), 2));
    }
    //distance in meters between lat1 and lat2
    public static double latDistance(double lat1, double lat2) {
        return Math.abs((lat1 - lat2) * 111320);
    }
    //distance in meters between lng1 and lng2
    public static double lngDistance(double lng1, double lng2, double lat1) {
        return Math.abs(lng1 - lng2) * 111320 * Math.cos(lat1); //distance in meters between lng1 and lng2
    }

    public LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {

            mLastLocation = locationResult.getLastLocation();
            double lat1 = mLastLocation.getLatitude();
            double lng1 =  mLastLocation.getLongitude();
            Log.i(TAG, "onLocationResult: " + lat1 + " " + lng1);

            if ((locationOfSearch == null) || (distance(lat1, locationOfSearch.getLatitude(), lng1, locationOfSearch.getLongitude()) >= SEARCH_RADIUS - (ON_MAP_RADIUS * 1.3))) {
                // search from database if far enough away, or no search performed yet
                searchForDoormats();
            }
            else if ((MapActivity.getMap() != null) && foregroundActivities.contains("MapActivity")) {
                    updateCirclesVisibility(MapActivity.getMap());
            }
            manageLocationUpdates();
        }
    };

    public static boolean isCoarseLocationGranted(Context context) {
        String coarse = Manifest.permission.ACCESS_COARSE_LOCATION;
        return ContextCompat.checkSelfPermission(context, coarse) == GRANTED;
    }

    public static boolean isFineLocationGranted(Context context) {
        String fine = Manifest.permission.ACCESS_FINE_LOCATION;
        return ContextCompat.checkSelfPermission(context, fine) == GRANTED;
    }

    public static boolean isBackLocationGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 29) {
            String background = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
            return ContextCompat.checkSelfPermission(context, background) == GRANTED;
        }
        return true;
    }

    //should execute in onCreate or onStart of DashboardActivity
    public void permissionsNeededAlert(Context context) {
        Log.i(TAG, "permissionsNeededAlert");
        if (context == null) {
            context = this;
        }
        new AlertDialog.Builder(context)
                .setTitle("Location permissions required for main features.")
                .setMessage("Tap OK to go to the app's settings. To be notified of nearby anchors while the app is running in the background, go to location permissions and choose \"Allow all the time\". For just foreground location tracking, choose \"Allow only while using the app.\".")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        navigateToAppSettings();
                    }
                })
                .create()
                .show();
    }

    //should execute when the user tries to click the map button on the doashboard, as well is if they click on the button in the drawer.
    public void foregroundPermissionNeededAlert(Context context) {
        Log.i(TAG, "foregroundPermissionNeededAlert");
        if (context == null) {
            context = this;
        }
        new AlertDialog.Builder(context)
                .setTitle("A minimum of foreground location permission required for basic map functionality.")
                .setMessage("Tap OK to go to the app's settings.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        navigateToAppSettings();
                    }
                })
                .create()
                .show();
    }

    // executes inside of onLocationResult,
    // gets anchors if user's traveled SEARCH_RADIUS - ON_MAP_RADIUS from previous search, or if this is the first search
    public void searchForDoormats() {
        Log.i(TAG, "searchForDoormats");

        double lat = mLastLocation.getLatitude();
        double lng = mLastLocation.getLongitude();

        //execute a function that gets anchors within SEARCH_RADIUS from database
        doormatManager.setVolleyCallback(this);
        doormatManager.getDoormats(this, lat, lng);

    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        Log.i(TAG, "onSuccessResponse");
//        Toast.makeText(this, result, Toast.LENGTH_LONG).show();

        locationOfSearch = mLastLocation;

        JSONObject obj = new JSONObject(result);
        UserData user_data = (UserData) new Gson().fromJson(obj.toString(), UserData.class);

        //set prevSearchDoormats as copy of doormats before doormats updated with getData()
        HashSet<UserData.Doormat> prevSearchDoormats = new HashSet<UserData.Doormat>(doormats);

        doormats = user_data.getData();

        HashSet<UserData.Doormat> newDoormats = getNewDoormats(prevSearchDoormats);
        HashSet<UserData.Doormat> oldDoormats = getOldDoormats(prevSearchDoormats);

        addNewGeofences(newDoormats);
        removeOldGeofences(oldDoormats);

        GoogleMap map = MapActivity.getMap();
        if (map != null) {
            updateCircles(map, prevSearchDoormats);
        }

    }

    private HashSet<UserData.Doormat> getNewDoormats(HashSet<UserData.Doormat> prevSearchDoormats) {
        HashSet<UserData.Doormat> newDoormats = new HashSet<UserData.Doormat>(doormats);
        newDoormats.removeAll(prevSearchDoormats);
        return newDoormats;
    }
    private HashSet<UserData.Doormat> getOldDoormats(HashSet<UserData.Doormat> prevSearchDoormats) {
        HashSet<UserData.Doormat> oldDoormats = new HashSet<UserData.Doormat>(prevSearchDoormats);
        oldDoormats.removeAll(doormats);
        return oldDoormats;
    }

    private void addNewGeofences(HashSet<UserData.Doormat> newDoormats) {
        Log.i(TAG, "addNewGeofences");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //maybe request permissions, create alert dialog, do a Log.i or Toast, or just return.
            return;
        }

        List<Geofence> geofenceList = new ArrayList<Geofence>();

        if (!newDoormats.isEmpty()) {
            for (UserData.Doormat d : newDoormats) {
                geofenceList.add(geoHelper.getGeofence(String.valueOf(d.getDoormat_id()), new LatLng(d.getLatitude(), d.getLongitude()), GEOFENCE_RADIUS,
                        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT));
            }
        }
        GeofencingRequest geofencingRequest = geoHelper.getGeofencingRequest(geofenceList);
        PendingIntent pendingIntent = geoHelper.getPendingIntent();

        geoClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Geofence(s) added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geoHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                    }
                });
    }

    private void removeOldGeofences(HashSet<UserData.Doormat> oldDoormats) {
        Log.i(TAG, "removeOldGeofences");
        List<String> requestIds = new ArrayList<String>();

        if (!oldDoormats.isEmpty()) {
            for (UserData.Doormat d : oldDoormats) {
                requestIds.add(String.valueOf(d.getDoormat_id()));
            }
        }
        geoClient.removeGeofences(requestIds);

    }

    //when updating circles immediately upon map creation,
    // send an empty HashSet as prevSearchDoormats to get all current doormats
    public void updateCircles(GoogleMap map, HashSet<UserData.Doormat> prevSearchDoormats) {
        Log.i(TAG, "updateCircles");
        boolean isInDoormats;

        int color = Color.parseColor("red");
        int alphaStroke = 255;
        int alphaFill = 120;

        HashSet<UserData.Doormat> newDoormats = getNewDoormats(prevSearchDoormats);

        //remove circles that are not in the most recent 'batch' of doormats and add the new ones,
        //unless newDoormats is empty.
        if (!newDoormats.isEmpty()) {
            for (Circle c : circles) {
                isInDoormats = false;
                for (UserData.Doormat d: doormats) {
                    if ((d.getLatitude() == c.getCenter().latitude) && (d.getLongitude() == c.getCenter().longitude)) {
                        isInDoormats = true;
                        break;
                    }
                }
                if (!isInDoormats) {
                    c.remove();
                    circles.remove(c);
                }
            }
            for (UserData.Doormat d : newDoormats) {
                CircleOptions circleOptions = new CircleOptions();
                circleOptions.center(new LatLng(d.getLatitude(), d.getLongitude()));
                circleOptions.radius(GEOFENCE_RADIUS);
                circleOptions.strokeColor(Color.argb(alphaStroke, Color.red(color), Color.green(color), Color.blue(color)));
                circleOptions.fillColor(Color.argb(alphaFill, Color.red(color), Color.green(color), Color.blue(color)));
                circleOptions.strokeWidth(3);
                circles.add(map.addCircle(circleOptions));
            }
        }
        updateCirclesVisibility(map);
    }
    public void updateCirclesVisibility(GoogleMap map) {
        Log.i(TAG, "updateCirclesVisibility");
        int color = Color.parseColor("red");
        int alphaStroke = 255;
        int alphaFill = 120;

        //set alpha of circles lower if they're farther away,
        //and make them invisible if they're more than ON_MAP_RADIUS away from user.
        for (Circle c : circles) {
            double dist = distance(mLastLocation.getLatitude(), c.getCenter().latitude, mLastLocation.getLongitude(), c.getCenter().longitude);
            if (dist >= ON_MAP_RADIUS && c.isVisible()) {
                c.setVisible(false);
            }
            else {
                double ratio = (ON_MAP_RADIUS - dist) / ON_MAP_RADIUS;
                c.setStrokeColor(Color.argb((int) (alphaStroke * ratio), Color.red(color), Color.green(color), Color.blue(color)));
                c.setFillColor(Color.argb((int) (alphaFill * ratio), Color.red(color), Color.green(color), Color.blue(color)));
                if (!c.isVisible()) {
                    c.setVisible(true);
                }
            }
        }
    }

    //unnecessarily complex except for huge distances; avoid using until we have a use for it
    public static double distanceOnASphere(Location lastLocation, Location newLocation, boolean useElevation) {

        double lat1 = lastLocation.getLatitude();
        double lat2 = newLocation.getLatitude();
        double lon1 = lastLocation.getLongitude();
        double lon2 = newLocation.getLongitude();

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(lonDistance / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        if (useElevation) {
            double el1 = lastLocation.getAltitude();
            double el2 = newLocation.getAltitude();
            distance = Math.pow(distance, 2) + Math.pow(el1 - el2, 2);
            return Math.sqrt(distance);
        }
        return distance;
    }
}
