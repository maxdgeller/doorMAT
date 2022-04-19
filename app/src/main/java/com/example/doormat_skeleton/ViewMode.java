package com.example.doormat_skeleton;


import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;

import android.location.Location;
import android.os.Bundle;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import android.widget.Spinner;
import android.widget.Toast;

import com.example.doormat_skeleton.Helpers.SnackbarHelper;
import com.google.android.gms.location.Geofence;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;

import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;

import com.google.ar.core.Session;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ViewMode extends AppCompatActivity  {

    private ArFragment arFragment;

    /***** Data from outside ViewMode *****/
    LocationApplication locationApplication;
    HashMap<String, UserData.Doormat> doormatMap = LocationApplication.getCurrentDoormatMap();
    HashSet<UserData.Doormat> currentMats;
    ArrayList<Geofence> geofences;

    /****** Rendering anchors ******/
    //acceptable colors
    private static final String[] COLORS = new String[]{
            "red", "green", "blue",
            "cyan", "magenta", "yellow", "black",  "grey", "white",
            "aqua", "fuchsia", "lime", "maroon", "navy", "olive", "purple", "silver", "teal"
    };
    //example of a key: {"red", "cylinder"}
    private final Map<String[], ModelRenderable> madeModels = new HashMap<String[], ModelRenderable>();
    //example of a key: "blue"
    private final Map<String, CompletableFuture<Material>> colorMaterials = new HashMap<String, CompletableFuture<Material>>();

    /****** Resolving anchors ******/
    //add to Queue from Geofence enter event
    private static final Queue<String> idsToResolve = new LinkedList<String>();
    //anchors being resolved
    private final Queue<Anchor> resolvingAnchors = new LinkedList<Anchor>();
    //anchors that have finished resolving
    private final Queue<Anchor> resolvedAnchors = new LinkedList<Anchor>();
    //add to Queue from Geofence exit event
    private static final Queue<String> idsToRemove = new LinkedList<String>();

    /****** Hosting anchors ******/
    private final StoreManager storeManager = new StoreManager();
    private Anchor anchorToHost;
    private boolean isPlaced = false;
    String colorChoice = "blue";
    String shapeChoice = "sphere";
    Spinner colorSpinner;
    Button hostBtn;
    HitResult lastHitResult;

    /****** Session data ******/
    SessionManager sessionManager;
    String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode);

        /***** Data from outside ViewMode *****/
        locationApplication = (LocationApplication) getApplication();

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        mName = user.get(SessionManager.USERNAME);

        Button clear = findViewById(R.id.clear);
        hostBtn = findViewById(R.id.hostBtn);

        ImageButton sphereBtn = findViewById(R.id.sphereBtn);
        ImageButton cubeBtn = findViewById(R.id.cubeBtn);
        ImageButton cylinderBtn = findViewById(R.id.cylinderBtn);

        colorSpinner = findViewById(R.id.color_spinner);
        colorSpinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, COLORS);

        colorSpinner.setAdapter(dataAdapter);
        colorSpinner.setVisibility(View.VISIBLE);
        colorSpinner.setPrompt("Color");
        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                 Log.d(TAG, "onItemSelected: color: " + colorChoice);

                 String color = colorSpinner.getSelectedItem().toString();
                 colorChoice = color;
                 placeItem(lastHitResult);
//                 colorSpinner.setBackgroundColor(android.graphics.Color.parseColor(color));
             }

             @Override
             public void onNothingSelected(AdapterView<?> adapterView) {

             }
         });

        hostBtn.setVisibility(View.GONE);
        FloatingActionButton back_btn = findViewById(R.id.back_btn);

        for (String cStr : COLORS) {
            Color color = new Color(android.graphics.Color.parseColor(cStr));
            colorMaterials.put(cStr, MaterialFactory.makeOpaqueWithColor(getApplicationContext(), color));
        }

        geofences = new ArrayList<Geofence>(LocationApplication.getEnteredGeofences().values());
        if (idsToResolve.isEmpty()) { newIDsToResolve(geofences); }

        currentMats = new HashSet<UserData.Doormat>();

        clear.setOnClickListener(view -> {
            clearPlacedAnchor();
            hostBtn.setVisibility(View.GONE);
        });


        back_btn.setOnClickListener(view -> {
            if(isPlaced) {
                Intent intent = new Intent(ViewMode.this, MapActivity.class);
                intent.putExtra("isPlaced", isPlaced);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
            else{
                Intent intent = new Intent(ViewMode.this, MapActivity.class);
                setResult(Activity.RESULT_CANCELED, intent);
                finish();
            }
        });

        sphereBtn.setOnClickListener(view -> {
            shapeChoice = "sphere";
            placeItem(lastHitResult);
        });

        cubeBtn.setOnClickListener(view -> {
            shapeChoice = "cube";
            placeItem(lastHitResult);
        });

        cylinderBtn.setOnClickListener(view -> {
            shapeChoice = "cylinder";
            placeItem(lastHitResult);
        });

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        assert arFragment != null;
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        if (anchorToHost == null || anchorToHost.getCloudAnchorState() != Anchor.CloudAnchorState.TASK_IN_PROGRESS) {
                            placeItem(hitResult);
                            lastHitResult = hitResult;
                            hostBtn.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        hostBtn.setOnClickListener(view -> {
            Session session = arFragment.getArSceneView().getSession();
            assert session != null;
            anchorToHost = session.hostCloudAnchor(anchorToHost);
            Toast.makeText(getApplicationContext(),"Now Hosting...", Toast.LENGTH_LONG).show();
        });
    }

    private void clearPlacedAnchor() {
        if (anchorToHost != null) {
            anchorToHost.detach();
            anchorToHost = null;
            isPlaced = false;
        }
    }

    //call this whenever you need a ModelRenderable
    private ModelRenderable getRenderable(String color, String shape) {
        String[] key = new String[]{color, shape};
        if (madeModels.containsKey(key)) { Log.d(TAG, "Retrieving model: " + key[0] + " " + key[1]); return madeModels.get(key); }

        ModelRenderable model;
        Material material = colorMaterials.get(color).getNow(null);

        if (shape.equals("sphere")) {model = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.15f, 0.0f), material);}
        else if (shape.equals("cube")) {model = ShapeFactory.makeCube(new Vector3(0.25f, 0.25f, 0.25f), new Vector3(0.0f, 0.15f, 0.0f), material);}
        else {model = ShapeFactory.makeCylinder(0.1f, 0.3f, new Vector3(0.0f, 0.15f, 0.0f), material);}

        madeModels.put(key, model);
        return madeModels.get(key);
    }

    private TransformableNode renderAnchor(Anchor a, String color, String shape) {
        AnchorNode anchorNode = new AnchorNode(a);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        TransformableNode tNode = new TransformableNode(arFragment.getTransformationSystem());
        tNode.setParent(anchorNode);
        tNode.setRenderable(getRenderable(color, shape));
        return tNode;
    }


    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }


    private synchronized void checkUpdatedAnchor(){
//        Log.i(TAG, "checkUpdatedAnchor");

        if (!idsToResolve.isEmpty()) {
            //Each frame, remove a single ID from idsToResolve and add a new anchornode to resolvingAnchors
            Anchor a = arFragment.getArSceneView().getSession().resolveCloudAnchor(idsToResolve.poll());
            resolvingAnchors.add(a);
        }
        if (!resolvingAnchors.isEmpty()) {
            Anchor a = resolvingAnchors.poll();
            Anchor.CloudAnchorState state = a.getCloudAnchorState();
            Log.d(TAG, "State: " + state.name() + ", ID: " + a.getCloudAnchorId());
            if (state == Anchor.CloudAnchorState.TASK_IN_PROGRESS) {
                //move to the end of the queue if resolution in progress
                resolvingAnchors.add(a);
            }
            else if (state == Anchor.CloudAnchorState.SUCCESS) {
                UserData.Doormat d = doormatMap.get(a.getCloudAnchorId());

                addToFoundAnchors(a.getCloudAnchorId());

                assert d != null;
                renderAnchor(a, d.getColor(), d.getShape());

                resolvedAnchors.add(a);
            }
            else if (state.isError()) {
                a.detach();
                Toast.makeText(getApplicationContext(),"ERROR: " + state.name() + ", ID: " + a.getCloudAnchorId(), Toast.LENGTH_LONG).show();
            }
        }

        if (anchorToHost != null) {
            Anchor.CloudAnchorState hostState = anchorToHost.getCloudAnchorState();
            if (hostState.isError()) {
                Toast.makeText(this, "Error Hosting...", Toast.LENGTH_LONG).show();
            }
            else if (hostState == Anchor.CloudAnchorState.SUCCESS) {
                Log.d(TAG, "checkUpdatedAnchor: " + anchorToHost.getCloudAnchorId());

                Location location = locationApplication.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                storeManager.storeDoormat(this,
                        anchorToHost.getCloudAnchorId(),
                        isPlaced,
                        latitude,
                        longitude,
                        mName,
                        colorChoice,
                        shapeChoice);

                Toast.makeText(this, "Anchor hosted. Cloud ID: " + anchorToHost.getCloudAnchorId(), Toast.LENGTH_LONG).show();

                addToFoundAnchors(anchorToHost.getCloudAnchorId());
                clearPlacedAnchor();

                //update global doormats, including newly hosted anchor
                locationApplication.setLocationOfSearch(null);

            }
        }
    }

    private void placeItem(HitResult hitResult){
        if (colorChoice != null && shapeChoice != null && hitResult != null) {
            clearPlacedAnchor();
            anchorToHost = hitResult.createAnchor();
            renderAnchor(anchorToHost, colorChoice, shapeChoice).select();
            isPlaced = true;
        }
    }

    //add the ID of a just-resolved anchor to the locally-stored set and update current doormats
    private void addToFoundAnchors(String resolvedAnchorID) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        HashSet<String> foundAnchors = new HashSet<String>(sharedPref.getStringSet("found anchors", new HashSet<String>()));
        foundAnchors.add(resolvedAnchorID);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("found anchors", foundAnchors);
        editor.apply();

        locationApplication.updateDoormatFound(resolvedAnchorID);

    }

    //methods called by GeofenceBroadcastReceiver upon events being triggered
    public static void newIDsToResolve(ArrayList<Geofence> triggeredGeofences) {
        for (Geofence g : triggeredGeofences) {
            Log.d(TAG, "Adding triggered Geofence to idsToResolve, ID: " + g.getRequestId());
            idsToResolve.add(g.getRequestId());
        }
    }
    public static void newIDsToRemove(ArrayList<Geofence> triggeredGeofences) {
        for (Geofence g : triggeredGeofences) {
            idsToRemove.add(g.getRequestId());
        }
    }

}



