package com.example.doormat_skeleton.Helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.example.doormat_skeleton.ChildResult;
import com.example.doormat_skeleton.ChildRetrieval;
import com.example.doormat_skeleton.LocationApplication;
import com.example.doormat_skeleton.AnchorResult;
import com.example.doormat_skeleton.ViewMode;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver implements ChildRetrieval.VolleyCallback {

    private static final String TAG = "GeofenceBroadcastReceiv";
    private static String lastId;

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationHelper notificationHelper = new NotificationHelper(context);
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Log.d(TAG, "onReceive: Error receiving geofence event");
            return;
        }

        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
        for (Geofence geofence: geofenceList) {
            Log.d(TAG, "onReceive: " + geofence.getRequestId());
        }
        Log.d(TAG, "");
        Location userLocation = geofencingEvent.getTriggeringLocation();
        int transitionType = geofencingEvent.getGeofenceTransition();

        String notifString;
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "geofenceList: " + geofenceList);

                //update entered geofences list
                LocationApplication.addEnteredGeofences(new ArrayList<Geofence>(geofenceList));
                ViewMode.newIDsToResolve(new ArrayList<Geofence>(geofenceList));

                //retrieve child nodes for each ID
                Log.d(TAG, "geofenceList: " + geofenceList);
                for (Geofence geofence: new ArrayList<Geofence>(geofenceList)) {
                    Log.d(TAG, "onReceive: " + geofence.getRequestId());
                    getChildNodes(geofence.getRequestId());
                }

                //send notification
                notifString = getNotifString(userLocation);
                if (!notifString.equals("No nearby undiscovered anchors.")) {
                    notificationHelper.sendHighPriorityNotification("Nearby undiscovered anchors:", notifString, LocationApplication.class);
                }


                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
//                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();

                LocationApplication.removeEnteredGeofences((ArrayList<Geofence>) geofenceList);
                LocationApplication.removeNearbyChildNodes((ArrayList<Geofence>) geofenceList);
                ViewMode.newIDsToRemove((ArrayList<Geofence>) geofenceList);


                notifString = getNotifString(userLocation);
                if (!notifString.equals("No nearby undiscovered anchors.")) {
                    notificationHelper.sendHighPriorityNotification("Nearby undiscovered anchors:", notifString, LocationApplication.class);
                }
                break;
        }
    }

    private static String getNotifString(Location userLocation) {
        ArrayList<AnchorResult.DatabaseAnchor> enteredDatabaseAnchors = new ArrayList<AnchorResult.DatabaseAnchor>();
        ConcurrentHashMap<String, AnchorResult.DatabaseAnchor> databaseAnchorMap = LocationApplication.getDatabaseAnchorMap();
        for (Geofence g : LocationApplication.getEnteredGeofences().values()) {
            AnchorResult.DatabaseAnchor da = databaseAnchorMap.get(g.getRequestId());
            if (da != null && !da.isFound()) {
                da.setProximity(LocationApplication.distance(
                        userLocation.getLatitude(), da.getLatitude(),
                        userLocation.getLongitude(), da.getLongitude()));
                enteredDatabaseAnchors.add(da);
            }
        }

        if (enteredDatabaseAnchors.size() > 0) {
            //uses compareTo method of Doormat class to sort by proximity
            Collections.sort(enteredDatabaseAnchors);

            StringBuilder notifString = new StringBuilder("");

            List<AnchorResult.DatabaseAnchor> enteredDoormatsList = enteredDatabaseAnchors.stream().limit(3).collect(Collectors.toList());

            for (AnchorResult.DatabaseAnchor d : enteredDoormatsList) {
                notifString.append(d.getAnchor_id());
                notifString.append(", ");
                notifString.append(d.getColor());
                notifString.append(", ");
                notifString.append(d.getShape());
                notifString.append(", about ");
                notifString.append(String.format(Locale.US, "%.2f", d.getProximity()));
                notifString.append(" meters\n");
            }

            return notifString.toString();
        }
        else {
            return "No nearby undiscovered anchors.";
        }
    }

    public void getChildNodes(String anchor_id) {
        Log.i(TAG, "getChildNodes");
        ChildRetrieval childRetrieval = new ChildRetrieval();
        childRetrieval.setVolleyCallback(this);
        childRetrieval.getChildNodes(anchor_id);
    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        Log.i(TAG, "onSuccessResponse");
//        Toast.makeText(this, result, Toast.LENGTH_LONG).show();

        JSONObject obj = new JSONObject(result);
        ChildResult childResult = (ChildResult) new Gson().fromJson(obj.toString(), ChildResult.class);
        Log.i(TAG, "childResult.getData() = " + childResult.getData().toString());
        LocationApplication.addNearbyChildNodes(childResult.getData());
    }
}