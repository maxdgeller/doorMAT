package com.example.doormat_skeleton;


import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;

import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;

import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewMode extends AppCompatActivity  {

    private ArFragment arFragment;

    private ModelRenderable sphereRenderable;
    private ModelRenderable blueSphere;
    private ModelRenderable greenSphere;
    private ModelRenderable redSphere;
    private ModelRenderable blueCube;
    private ModelRenderable redCube;
    private ModelRenderable greenCube;
    private ModelRenderable blueCylinder;
    private ModelRenderable redCylinder;
    private ModelRenderable greenCylinder;

    private Anchor cloudAnchor;
    SessionManager sessionManager;
    String mName;
    private boolean isPlaced;
    SharedPreferences markerPreferences;
    LatLng latLng;
    double lat;
    double lon;
    double latitude;
    double longitude;
    String colorChoice = "blue";
    String shapeChoice = "sphere";
    String resolvedAnchorID;

    Anchor anchor;

    HashSet<UserData.Doormat> mDoormats;
    HashSet<UserData.Doormat> currentMats;
    HashSet<Geofence> geofences;

    boolean isEntered;

    Spinner doormatSpinner;

    List<String> spinnerList;
    List<String> anchorList;

    LocationApplication locationApplication;



    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

    private enum ResolveState{
        INSUFFICIENT,
        SUFFICIENT,
        GOOD
    }


    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private ResolveState resolveState = ResolveState.INSUFFICIENT;
    private final SnackbarHelper snackbarHelper = new SnackbarHelper();
    private final StoreManager storeManager = new StoreManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode);
        markerPreferences = getSharedPreferences("MarkerValue", Context.MODE_PRIVATE);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        mName = user.get(SessionManager.USERNAME);

        Button resolveBtn = findViewById(R.id.resolveBtn);
        resolveBtn.setVisibility(View.GONE);

        Button clear = findViewById(R.id.clear);
        Button finish = findViewById(R.id.finish);
        Button resolveFinish = findViewById(R.id.resolveFinish);
        resolveFinish.setVisibility(View.GONE);

        ImageButton sphereBtn = findViewById(R.id.sphereBtn);
        ImageButton cubeBtn = findViewById(R.id.cubeBtn);
        ImageButton cylinderBtn = findViewById(R.id.cylinderBtn);
        ImageButton redBtn = findViewById(R.id.redBtn);
        ImageButton blueBtn = findViewById(R.id.blueBtn);
        ImageButton greenBtn = findViewById(R.id.greenBtn);

        doormatSpinner = findViewById(R.id.doormat_spinner);
        doormatSpinner.setVisibility(View.GONE);

        finish.setVisibility(View.GONE);
        FloatingActionButton back_btn = findViewById(R.id.back_btn);
//        latLng = getIntent().getExtras().getParcelable("LatLng");
//        lat = latLng.latitude;
//        lon = latLng.longitude;


        locationApplication = (LocationApplication) getApplication();
        Location location = locationApplication.getLastLocation();

        latitude = location.getLatitude();
        longitude = location.getLongitude();


        mDoormats = LocationApplication.getCurrentDoormats();
        geofences = LocationApplication.getEnteredGeofences();
        currentMats = new HashSet<UserData.Doormat>();


        checkEntered();
        if(isEntered){
            resolveBtn.setVisibility(View.VISIBLE);
            addDoormats();
        }

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> sphereRenderable = ShapeFactory.makeSphere(
                        0.1f, new Vector3(0.0f,0.15f, 0.0f ),material));


        //Creating sphere models
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> blueSphere = ShapeFactory.makeSphere(
                        0.1f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "sphere";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.RED))
                .thenAccept(material -> redSphere = ShapeFactory.makeSphere(
                        0.1f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "sphere";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.GREEN))
                .thenAccept(material -> greenSphere = ShapeFactory.makeSphere(
                        0.1f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "sphere";

        //Creating cube models
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> blueCube = ShapeFactory.makeCube(
                        new Vector3(0.25f, 0.25f, 0.25f), new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cube";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.RED))
                .thenAccept(material -> redCube = ShapeFactory.makeCube(
                        new Vector3(0.25f, 0.25f, 0.25f),
                        new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cube";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.GREEN))
                .thenAccept(material -> greenCube = ShapeFactory.makeCube(
                        new Vector3(0.25f, 0.25f, 0.25f),
                        new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cube";

        //Creating cylinder models
        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> blueCylinder = ShapeFactory.makeCylinder(
                        0.1f, 0.3f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cylinder";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.RED))
                .thenAccept(material -> redCylinder = ShapeFactory.makeCylinder(
                        0.1f, 0.3f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cylinder";

        MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(android.graphics.Color.GREEN))
                .thenAccept(material -> greenCylinder = ShapeFactory.makeCylinder(
                        0.1f, 0.3f, new Vector3(0.0f, 0.15f, 0.0f), material));
        shapeChoice = "cylinder";


        clear.setOnClickListener(view -> {
            setCloudAnchor(null);
            if(cloudAnchor!=null) {
                cloudAnchor.detach();
                isPlaced = false;
            }
            if(anchor!=null) {
                anchor.detach();
                anchor = null;
            }
            finish.setVisibility(View.GONE);
        });

        resolveBtn.setOnClickListener(view -> {
            if(cloudAnchor != null){
                return;
            }

            if(spinnerList.size() == 1){
                resolvedAnchorID = anchorList.get(0);
                String item = spinnerList.get(0);
                String[] parts = item.split(",");
                colorChoice = parts[0].trim();
                shapeChoice = parts[1].trim();

                Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(resolvedAnchorID);
                setCloudAnchor(resolvedAnchor);

                AnchorNode anchorNode = new AnchorNode(resolvedAnchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                setSphereRenderable();

                TransformableNode sphere = new TransformableNode(arFragment.getTransformationSystem());
                sphere.setParent(anchorNode);
                sphere.setRenderable(sphereRenderable);
                sphere.select();
                Toast.makeText(ViewMode.this, "Resolving doormat...", Toast.LENGTH_SHORT).show();
                appAnchorState = AppAnchorState.RESOLVING;

                return;
            }


            resolveStart();

//            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerList);
//
//            doormatSpinner.setAdapter(dataAdapter);
//            doormatSpinner.setVisibility(View.VISIBLE);
//            doormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                    String item = doormatSpinner.getSelectedItem().toString();
//                    String[] parts = item.split(",");
//                    Log.d(TAG, "onItemSelected: " + parts[0]);
//                    resolvedAnchorID = parts[0];
//                    colorChoice = parts[1].trim();
//                    shapeChoice = parts[2].trim();
//
//                    Log.d(TAG, "onItemSelected: color: " + colorChoice + " shape: " + shapeChoice );
//                    resolveFinish.setVisibility(View.VISIBLE);
//
//                }
//
//                @Override
//                public void onNothingSelected(AdapterView<?> adapterView) {
//
//                }
//            });



//            ResolveDialogFragment dialog = new ResolveDialogFragment();
//            dialog.setOkListener(ViewMode.this::onResolveOkPressed);
//            dialog.show(getSupportFragmentManager(), "Resolve");
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
            setSphereRenderable();
        });

        cubeBtn.setOnClickListener(view -> {
            shapeChoice = "cube";
            setSphereRenderable();
        });

        cylinderBtn.setOnClickListener(view -> {
            shapeChoice = "cylinder";
            setSphereRenderable();
        });

        redBtn.setOnClickListener(view ->{
            colorChoice = "red";
            setSphereRenderable();
        });

        blueBtn.setOnClickListener(view -> {
            colorChoice = "blue";
            setSphereRenderable();
        });

        greenBtn.setOnClickListener(view -> {
            colorChoice = "green";
            setSphereRenderable();
        });


        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        assert arFragment != null;
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);


        assert arFragment != null;
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING ||
                            appAnchorState != AppAnchorState.NONE) {
                        return;
                    }


                    placeItem(hitResult);

                    if(isPlaced){
                        finish.setVisibility(View.VISIBLE);
                    }

                }
        );

        resolveFinish.setOnClickListener(view -> {

            if(cloudAnchor != null){
                Toast.makeText(this, "Please clear doormat first.", Toast.LENGTH_SHORT).show();
                resolveFinish.setVisibility(View.GONE);
                return;
            }

            Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(resolvedAnchorID);
            setCloudAnchor(resolvedAnchor);

//            AnchorNode anchorNode = new AnchorNode(resolvedAnchor);
//            anchorNode.setParent(arFragment.getArSceneView().getScene());
//
//            setSphereRenderable();
//
//            TransformableNode sphere = new TransformableNode(arFragment.getTransformationSystem());
//            sphere.setParent(anchorNode);
//            sphere.setRenderable(sphereRenderable);
//            sphere.select();
            Toast.makeText(ViewMode.this, "Resolving doormat...", Toast.LENGTH_SHORT).show();
            appAnchorState = AppAnchorState.RESOLVING;
            resolveFinish.setVisibility(View.GONE);

        });

        finish.setOnClickListener(view -> {
            Session session = arFragment.getArSceneView().getSession();
            cloudAnchor = session.hostCloudAnchor(anchor);
            setCloudAnchor(cloudAnchor);
            appAnchorState = AppAnchorState.HOSTING;
            Toast.makeText(getApplicationContext(),"Now Hosting...", Toast.LENGTH_LONG).show();
        });
    }




    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }


    private synchronized void checkUpdatedAnchor(){
        if(appAnchorState != AppAnchorState.HOSTING && appAnchorState != AppAnchorState.RESOLVING){
            return;
        }
        Anchor.CloudAnchorState cloudAnchorState = cloudAnchor.getCloudAnchorState();
        if(appAnchorState == AppAnchorState.HOSTING) {
            if (cloudAnchorState.isError()) {

                Toast.makeText(this, "Error Hosting...", Toast.LENGTH_LONG).show();
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
//                int shortCode = storeManager.nextShortCode(this);
                Log.d(TAG, "checkUpdatedAnchor: " + cloudAnchor.getCloudAnchorId());
                storeManager.storeDoormat(this,
                        cloudAnchor.getCloudAnchorId(),
                        isPlaced,
                        latitude,
                        longitude,
                        mName,
                        colorChoice,
                        shapeChoice);

                Toast.makeText(this, "Anchor hosted. Cloud ID: " + cloudAnchor.getCloudAnchorId(), Toast.LENGTH_LONG).show();
                appAnchorState = AppAnchorState.HOSTED;
                addToFoundAnchors(cloudAnchor.getCloudAnchorId());
            }
        }
        else if(appAnchorState == AppAnchorState.RESOLVING){
            if (cloudAnchorState.isError()) {
                Toast.makeText(this, "Error resolving...", Toast.LENGTH_LONG).show();
                appAnchorState = AppAnchorState.NONE;
            } else if(cloudAnchorState == Anchor.CloudAnchorState.SUCCESS){
                Toast.makeText(this, "Doormat resolved.", Toast.LENGTH_LONG).show();

                AnchorNode anchorNode = new AnchorNode(cloudAnchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                setSphereRenderable();

                TransformableNode sphere = new TransformableNode(arFragment.getTransformationSystem());
                sphere.setParent(anchorNode);
                sphere.setRenderable(sphereRenderable);
                isPlaced = true;
                sphere.select();

                appAnchorState = AppAnchorState.RESOLVED;
                addToFoundAnchors(resolvedAnchorID);
            }
        }
    }

    private void resolveStart(){
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerList);

        doormatSpinner.setAdapter(dataAdapter);
        doormatSpinner.setVisibility(View.VISIBLE);
        doormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = doormatSpinner.getSelectedItem().toString();
                int pos = doormatSpinner.getSelectedItemPosition();
                String[] parts = item.split(",");
//                Log.d(TAG, "onItemSelected: " + parts[0]);
                resolvedAnchorID = anchorList.get(pos);
                Log.d(TAG, "onItemSelected: " + resolvedAnchorID);
                colorChoice = parts[0].trim();
                shapeChoice = parts[1].trim();

                Log.d(TAG, "onItemSelected: color: " + colorChoice + " shape: " + shapeChoice );
                Button resolveFinish = findViewById(R.id.resolveFinish);
                resolveFinish.setVisibility(View.VISIBLE);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    private void setCloudAnchor(Anchor newAnchor){
        if(cloudAnchor != null){
            cloudAnchor.detach();
        }
        cloudAnchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);
    }


    private void setSphereRenderable(){
        if(colorChoice.equals("blue") && shapeChoice.equals("sphere")){
            sphereRenderable = blueSphere;
        }else if(colorChoice.equals("red") && shapeChoice.equals("sphere")){
            sphereRenderable = redSphere;
        }else if(colorChoice.equals("green") && shapeChoice.equals("sphere")){
            sphereRenderable = greenSphere;
        }else if(colorChoice.equals("blue") && shapeChoice.equals("cube")){
            sphereRenderable = blueCube;
        }else if(colorChoice.equals("red") && shapeChoice.equals("cube")){
            sphereRenderable = redCube;
        }else if(colorChoice.equals("green") && shapeChoice.equals("cube")){
            sphereRenderable = greenCube;
        }else if(colorChoice.equals("blue") && shapeChoice.equals("cylinder")){
            sphereRenderable = blueCylinder;
        }else if(colorChoice.equals("red") && shapeChoice.equals("cylinder")){
            sphereRenderable = redCylinder;
        }else if(colorChoice.equals("green") && shapeChoice.equals("cylinder")){
            sphereRenderable = greenCylinder;
        }
    }

    private void placeItem(HitResult hitResult){
        anchor = hitResult.createAnchor();
        setCloudAnchor(anchor);
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        TransformableNode sphere = new TransformableNode(arFragment.getTransformationSystem());
        sphere.setParent(anchorNode);
        sphere.setRenderable(sphereRenderable);
        isPlaced = true;
        sphere.select();
    }

    private void checkEntered(){
        if(geofences != null){
            isEntered = true;
        }else{
            isEntered = false;
        }
    }

    //adds doormat objects in geofence to spinner list
    private void addDoormats(){
        if(mDoormats != null){
            for(Geofence g: geofences){
                for(UserData.Doormat d: mDoormats){
                    if(String.valueOf(d.getDoormat_id()).equals(g.getRequestId())){
                        currentMats.add(d);
                    }
                }
            }
            spinnerList = new ArrayList<String>();
            anchorList = new ArrayList<String>();
            for(UserData.Doormat d: currentMats){
                spinnerList.add(d.getColor() + ", " + d.getShape());
                anchorList.add(d.getDoormat_id());
                Log.d(TAG, "addDoormats: " + spinnerList.toString());
            }
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

}



