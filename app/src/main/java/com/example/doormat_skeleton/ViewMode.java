package com.example.doormat_skeleton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.os.*;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.doormat_skeleton.Helpers.CameraPermissionHelper;
import com.example.doormat_skeleton.Helpers.SnackbarHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.core.Config.CloudAnchorMode;


import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.HashMap;

public class ViewMode extends AppCompatActivity {

    private ArFragment arFragment;
    private ModelRenderable sphereRenderable;
    private Anchor cloudAnchor;
    private Button clear;
    private Button finish;
    private FloatingActionButton back_btn;
    SessionManager sessionManager;
    String mName;
    private boolean isPlaced;
    SharedPreferences markerPreferences;
    LatLng latLng;
    double lat;
    double lon;
    String colorChoice;
    String shapeChoice;

    Spinner shapeSpinner;
    Spinner colorSpinner;


    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }

    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private SnackbarHelper snackbarHelper = new SnackbarHelper();
    private final StoreManager storeManager = new StoreManager();



    ArrayList<Integer> shapeList = new ArrayList<>();
    ArrayList<Integer> colorList = new ArrayList<>();
    String[] shapeArray = {"Sphere", "Cube", "Cylinder"};
    String[] colorArray = {"Blue", "Green", "Yellow", "Red"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode);
        markerPreferences = getSharedPreferences("MarkerValue", Context.MODE_PRIVATE);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        mName = user.get(sessionManager.USERNAME);



        Button clear = findViewById(R.id.clear);
        Button finish = findViewById(R.id.finish);
        finish.setVisibility(View.GONE);
        FloatingActionButton back_btn = findViewById(R.id.back_btn);
        latLng = getIntent().getExtras().getParcelable("LatLng");
        lat = latLng.latitude;
        lon = latLng.longitude;


        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloudAnchor(null);
                isPlaced = false;
            }
        });

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isPlaced == true) {
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
            }
        });



        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> {
                    sphereRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f,0.15f, 0.0f ),material);
                });

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);


        assert arFragment != null;
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING ||
                            appAnchorState != AppAnchorState.NONE) {
                        return;
                    }
                    Anchor anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());

                    setCloudAnchor(anchor);

                    appAnchorState = AppAnchorState.HOSTING;
                    Toast.makeText(this,"Now Hosting...", Toast.LENGTH_LONG).show();
                    snackbarHelper.showMessage(this, "Now hosting...");

                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    TransformableNode sphere = new TransformableNode(arFragment.getTransformationSystem());
                    sphere.setParent(anchorNode);
                    sphere.setRenderable(sphereRenderable);
                    isPlaced = true;
                    sphere.select();

                }
        );

    }

    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }



    private synchronized void checkUpdatedAnchor(){
        if(appAnchorState != AppAnchorState.HOSTING){
            return;
        }
        Anchor.CloudAnchorState cloudAnchorState = cloudAnchor.getCloudAnchorState();
        if(cloudAnchorState.isError()){
            snackbarHelper.showMessageWithDismiss(this, "Error hosting... " +
                    cloudAnchorState);
            Toast.makeText(this,"Error Hosting...", Toast.LENGTH_LONG).show();
            appAnchorState = AppAnchorState.NONE;
        }
        else if(cloudAnchorState == Anchor.CloudAnchorState.SUCCESS){
            int shortCode = storeManager.nextShortCode(this);
            storeManager.storeUsingShortCode(this,
                    shortCode,
                    cloudAnchor.getCloudAnchorId(),
                    isPlaced,
                    lat,
                    lon,
                    mName);

            snackbarHelper.showMessageWithDismiss(this, "Anchor hosted. Cloud ID: " +
                    shortCode);
            Toast.makeText(this,"Anchor hosted. Cloud ID: " + shortCode, Toast.LENGTH_LONG).show();
            appAnchorState = AppAnchorState.HOSTED;

            //i think the code to add the anchor's lat, long, and id to the database should go here
        }
    }


    private void setCloudAnchor(Anchor newAnchor){
        if(cloudAnchor != null){
            cloudAnchor.detach();
        }

        cloudAnchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);

    }


    private void placeObject(ArFragment fragment, Anchor anchor, Uri model){
        ModelRenderable.builder()
                .setSource(fragment.getContext(), model)
                .build()
                .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));

    }
    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable){
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    private void setObject(String color, String shape){
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> {
                    sphereRenderable = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f,0.15f, 0.0f ),material);
                });
    }

}


