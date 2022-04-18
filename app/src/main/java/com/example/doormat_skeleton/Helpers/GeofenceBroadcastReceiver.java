package com.example.doormat_skeleton.Helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.example.doormat_skeleton.LocationApplication;
import com.example.doormat_skeleton.UserData;
import com.example.doormat_skeleton.ViewMode;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiv";
    
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

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                Toast.makeText(context, "GEOFENCE_TRANSITION_ENTER", Toast.LENGTH_SHORT).show();
                for (Geofence geofence: new ArrayList<Geofence>(geofenceList)) {
                    Log.d(TAG, "onReceive: " + geofence.getRequestId());
                }

                LocationApplication.addEnteredGeofences(new ArrayList<Geofence>(geofenceList));
                ViewMode.newIDsToResolve(new ArrayList<Geofence>(geofenceList));
                notificationHelper.sendHighPriorityNotification("Nearby undiscovered anchors:", getNotifString(userLocation), LocationApplication.class);
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
//                Toast.makeText(context, "GEOFENCE_TRANSITION_DWELL", Toast.LENGTH_SHORT).show();
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                Toast.makeText(context, "GEOFENCE_TRANSITION_EXIT", Toast.LENGTH_SHORT).show();

                LocationApplication.removeEnteredGeofences(new ArrayList<Geofence>(geofenceList));
                ViewMode.newIDsToRemove(new ArrayList<Geofence>(geofenceList));
                notificationHelper.sendHighPriorityNotification("Nearby undiscovered anchors:", getNotifString(userLocation), LocationApplication.class);
                break;
        }
    }

    private static String getNotifString(Location userLocation) {
        ArrayList<UserData.Doormat> enteredDoormats = new ArrayList<UserData.Doormat>();

        for (Geofence g : LocationApplication.getEnteredGeofences().values()) {
            for (UserData.Doormat d : LocationApplication.getCurrentDoormatSet()) {
                if (g.getRequestId().equals(String.valueOf(d.getDoormat_id()))) {

                    double lat1 = userLocation.getLatitude();
                    double lat2 = d.getLatitude();
                    double lng1 = userLocation.getLongitude();
                    double lng2 = d.getLongitude();

                    d.setProximity(LocationApplication.distance(lat1, lat2, lng1, lng2));
                    enteredDoormats.add(d);
                }
            }
        }

        //exclude from the notification any anchors that have already been found by the user
        enteredDoormats.removeIf(UserData.Doormat::isFound);

        if (enteredDoormats.size() > 0) {
            //uses compareTo method of Doormat class to sort by proximity
            Collections.sort(enteredDoormats);

            StringBuilder notifString = new StringBuilder("");

            List<UserData.Doormat> enteredDoormatsList = enteredDoormats.stream().limit(3).collect(Collectors.toList());

            for (UserData.Doormat d : enteredDoormatsList) {
                notifString.append(d.getDoormat_id());
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

    public static void sendNotif(Location userLocation, Context context) {
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.sendHighPriorityNotification("Nearby undiscovered anchors:", getNotifString(userLocation), LocationApplication.class);
    }
}