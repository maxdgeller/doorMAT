package com.example.doormat_skeleton;


import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;

import android.location.Location;
import android.os.Bundle;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.ar.core.Anchor;

import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;

import com.google.ar.core.Session;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.Renderable;
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
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ViewMode extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ArFragment arFragment;

    String mName;

    /***** State Constants (for convenience) *****/
    private static final Anchor.CloudAnchorState TASK_IN_PROGRESS = Anchor.CloudAnchorState.TASK_IN_PROGRESS;
    private static final Anchor.CloudAnchorState SUCCESS = Anchor.CloudAnchorState.SUCCESS;

    /***** Data from outside ViewMode *****/
    HashMap<String, UserData.Doormat> doormatMap = LocationApplication.getCurrentDoormatMap();

    /** Queue of IDs of Anchors that need to be resolved. ******/
    private static final Queue<String> idsToResolve = new LinkedList<String>();

    /** Queue of Anchors that are currently resolving. ******/
    private final Queue<Anchor> resolving = new LinkedList<Anchor>();

    /** Queue of AnchorNodes whose Anchors have been resolved. ******/
    private final Queue<AnchorNode> resolved = new LinkedList<AnchorNode>();

    /** Queue of IDs of Anchors that should be detached. ******/
    private static final Queue<String> idsToRemove = new LinkedList<String>();

    /** Resolved child nodes waiting to be rendered. *****/
    private static final Queue<UserData.ChildNode> childrenWaiting = new LinkedList<UserData.ChildNode>();

    /* Hosting anchors */
    private final StoreManager storeManager = new StoreManager();
    private Anchor anchorToHost;
    private AnchorNode rootNode;
    private TransformableNode rootTNode;

    /** Acceptable colors. ******/
    private static final String[] COLORS = new String[] { //acceptable color strings
            "red", "green", "blue", "cyan", "magenta", "yellow", "black",  "grey", "white",
            "aqua", "fuchsia", "lime", "maroon", "navy", "olive", "purple", "silver", "teal"};
    /** Map of {color, shape} array keys and their associated ModelRenderables. ******/
    private static final Map<String[], ModelRenderable> madeModels = new HashMap<String[], ModelRenderable>();
    /** Map of ModelRenderable keys and their associated {color, shape} arrays. ******/
    private static final Map<Renderable, String[]> madeModelsReverse = new HashMap<Renderable, String[]>();
    /** Map of color keys and their associated Material CompletableFutures. ******/
    private static final Map<String, CompletableFuture<Material>> colorMaterials = new HashMap<String, CompletableFuture<Material>>();

    private static String colorChoice = "blue";
    private static String shapeChoice = "sphere";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mode);

        SessionManager sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        mName = sessionManager.getUserDetail().get(SessionManager.USERNAME);

        Spinner colorSpinner = findViewById(R.id.color_spinner);
        colorSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, COLORS));
        colorSpinner.setOnItemSelectedListener(this);

        for (String cStr : COLORS) {
            Color color = new Color(android.graphics.Color.parseColor(cStr));
            colorMaterials.put(cStr, MaterialFactory.makeOpaqueWithColor(getApplicationContext(), color));
        }

        idsToResolve.clear();
        newIDsToResolve(new ArrayList<Geofence>(LocationApplication.getEnteredGeofences().values()));

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        assert arFragment != null;
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        arFragment.setOnTapArPlaneListener(this::onTapPlane);
    }

    //call this whenever you need a ModelRenderable
    private ModelRenderable getRenderable(String color, String shape) {
        String[] key = new String[]{color, shape};
        if (madeModels.containsKey(key)) { return madeModels.get(key); }

        ModelRenderable model;
        Material material = colorMaterials.get(color).getNow(null);

        if (shape.equals("sphere")) {model = ShapeFactory.makeSphere(0.1f, new Vector3(0.0f, 0.15f, 0.0f), material);}
        else if (shape.equals("cube")) {model = ShapeFactory.makeCube(new Vector3(0.25f, 0.25f, 0.25f), new Vector3(0.0f, 0.15f, 0.0f), material);}
        else {model = ShapeFactory.makeCylinder(0.1f, 0.3f, new Vector3(0.0f, 0.15f, 0.0f), material);}

        madeModels.put(key, model);
        madeModelsReverse.put(model, key);
        return madeModels.get(key);
    }

    private void renderResolvedChild(UserData.ChildNode childNode) {
        AnchorNode an = new AnchorNode();
        an.setParent(arFragment.getArSceneView().getScene().findByName(childNode.getAnchor_id()));
        an.setLocalPosition(childNode.getPosition());
        an.setWorldScale(childNode.getScale());
        an.setWorldRotation(childNode.getRotation());
        an.setRenderable(getRenderable(childNode.getColor(), childNode.getShape()));
    }

    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }

    private synchronized void checkUpdatedAnchor() {
        if (!idsToResolve.isEmpty()) { resolveNext(); }
        if (!resolving.isEmpty()) { checkNextResolving(); }
        if (!resolved.isEmpty() && !childrenWaiting.isEmpty()) { renderNextChild(); }
        //haven't yet written code to detach anchors that are no longer nearby
        if (!resolved.isEmpty() && !idsToRemove.isEmpty()) {  }
        if (anchorToHost != null) { checkHosting(); }
    }

    private synchronized void resolveNext() {
        Toast.makeText(getApplicationContext(),"attempting to resolve " + idsToResolve.peek(), Toast.LENGTH_LONG).show();
        //Each frame, remove a single ID from idsToResolve and add a new anchornode to resolvingAnchors
        Session session = arFragment.getArSceneView().getSession();
        assert session != null;
        Anchor a = session.resolveCloudAnchor(idsToResolve.poll());
        resolving.add(a);
    }

    private synchronized void checkNextResolving() {
        Anchor a = resolving.poll();
        assert a != null;
        Anchor.CloudAnchorState state = a.getCloudAnchorState();
        logState(a, state);
        showState(a, state);

        if (state == TASK_IN_PROGRESS) { resolving.add(a); } //put resolving anchor at end of queue

        else if (state == SUCCESS) {
            UserData.Doormat d = doormatMap.get(a.getCloudAnchorId());
            assert d != null;
            addToFoundAnchors(a.getCloudAnchorId());

            AnchorNode anchorNode = new AnchorNode(a);
            anchorNode.setName(a.getCloudAnchorId());

            resolved.add(anchorNode);
        }

        else if (state.isError()) { showState(a, state); a.detach(); } //show error and detach anchor
    }

    private synchronized void renderNextChild() {
        UserData.ChildNode childNode = childrenWaiting.poll();
        assert childNode != null;

        AnchorNode an = (AnchorNode) arFragment.getArSceneView().getScene().findByName(childNode.getAnchor_id());
        assert an != null;
        Anchor.CloudAnchorState state = Objects.requireNonNull(an.getAnchor()).getCloudAnchorState();

        if (state == SUCCESS) {
            renderResolvedChild(childNode);
        }
        else if (state.isError()) {
            logState(an.getAnchor(), state);
            showState(an.getAnchor(), state);
        }
        else {
            childrenWaiting.add(childNode);
        }
    }

    private synchronized void checkHosting() {
        Anchor.CloudAnchorState hostState = anchorToHost.getCloudAnchorState();

        if (hostState != TASK_IN_PROGRESS) {
            logState(anchorToHost, hostState);
        }

        if (hostState == TASK_IN_PROGRESS) { return; } //skip rest of method if hosting in progress
        if (hostState.isError()) { showState(anchorToHost, hostState); anchorToHost = null; } //failed hosting

        else if (hostState == Anchor.CloudAnchorState.SUCCESS) {
            logState(anchorToHost, hostState);
            String id = anchorToHost.getCloudAnchorId();

            //store data
            storeAnchor(id);
            recursivelyStoreDescendants(rootNode, id);

            Toast.makeText(this, "Anchor hosted.\nCloud ID: " + anchorToHost.getCloudAnchorId(), Toast.LENGTH_LONG).show();

            addToFoundAnchors(anchorToHost.getCloudAnchorId());
            anchorToHost.detach();
            anchorToHost = null;
            //update global doormats, including newly hosted anchor
            LocationApplication.setLocationOfSearch(null);

        }
    }

    private synchronized void storeAnchor(String id) {
        String[] val = madeModelsReverse.get(rootTNode.getRenderable());
        assert val != null;
        Location location = LocationApplication.getLastLocation();
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        storeManager.storeDoormat(this, id, val[0], val[1], lat, lng, mName);
    }

    private synchronized void recursivelyStoreDescendants(Node parent, String id) {
        for (Node child : parent.getChildren()) {
            if (child.getClass() == TransformableNode.class) {
                // e.g. child's pos (0, 3, 1) minus rootNode's pos (1, 2, -4) equals local pos (-1, 1, 5)
                Vector3 pos = Vector3.subtract(child.getWorldPosition(), rootNode.getWorldPosition());
                Vector3 scale = child.getWorldScale();
                Quaternion rot = child.getWorldRotation();
                String[] val = madeModelsReverse.get(child.getRenderable()); //array of color and shape strings
                assert val != null;
                storeManager.storeChildNode(this, id, val[0], val[1], pos, scale, rot);
            }
            if (!child.getChildren().isEmpty()) {
                recursivelyStoreDescendants(child, id);
            }
        }
    }

    private void showState(Anchor a, Anchor.CloudAnchorState state) {
        Toast.makeText(getApplicationContext(),"ID: " + a.getCloudAnchorId() + "\nState: " + state.name(), Toast.LENGTH_LONG).show();
    }

    private void logState(Anchor a, Anchor.CloudAnchorState state) {
        Log.d(TAG,"\nID: " + a.getCloudAnchorId() + "\nState: " + state.name());
    }

    //add the ID of a just-resolved anchor to the locally-stored set and update current doormats
    private void addToFoundAnchors(String resolvedAnchorID) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        HashSet<String> foundAnchors = new HashSet<String>(sharedPref.getStringSet("found anchors", new HashSet<String>()));
        foundAnchors.add(resolvedAnchorID);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("found anchors", foundAnchors);
        editor.apply();

        LocationApplication.updateDoormatFound(resolvedAnchorID);

    }

    //methods called by GeofenceBroadcastReceiver upon events being triggered
    private static synchronized void newIDsToResolve(ArrayList<Geofence> triggeredGeofences) {
        for (Geofence g : triggeredGeofences) {
            Log.d(TAG, "Adding triggered Geofence to idsToResolve, ID: " + g.getRequestId());
            idsToResolve.add(g.getRequestId());
        }
    }

    //methods called by GeofenceBroadcastReceiver upon events being triggered
    public static synchronized void newIDToResolve(ArrayList<UserData.ChildNode> newChildNodes) {
        idsToResolve.add(newChildNodes.get(0).getAnchor_id());
        childrenWaiting.addAll(newChildNodes);
    }

    public static synchronized void newIDsToRemove(ArrayList<Geofence> triggeredGeofences) {
        for (Geofence g : triggeredGeofences) {
            idsToRemove.add(g.getRequestId());
        }
    }

    public void onClickSphere(View view) { shapeChoice = "sphere"; updateSelectedNode(); }
    public void onClickCube(View view) { shapeChoice = "cube"; updateSelectedNode(); }
    public void onClickCylinder(View view) { shapeChoice = "cylinder"; updateSelectedNode(); }

    private void updateSelectedNode() {
        TransformableNode tNode = (TransformableNode) arFragment.getTransformationSystem().getSelectedNode();
        if (tNode != null) {
            tNode.setRenderable(getRenderable(colorChoice, shapeChoice));
        }
    }

    public void onClickBack(View view) { finish(); }

    public void onClickRemove(View view) {
        TransformableNode tNode = (TransformableNode) arFragment.getTransformationSystem().getSelectedNode();
        if (tNode != null) {
            arFragment.getTransformationSystem().selectNode(null);
            tNode.setRenderable(null);
            tNode.setParent(null);
            tNode.setEnabled(false);
        }
        view.setVisibility(View.INVISIBLE);
        if (rootNode != null && !rootNode.getChildren().isEmpty()) {
            findViewById(R.id.hostBtn).setVisibility(View.INVISIBLE);
        }
    }

    public void onClickHost(View view) {
        view.setVisibility(View.INVISIBLE);
        Session session = arFragment.getArSceneView().getSession();
        assert session != null;
        anchorToHost = session.hostCloudAnchor(rootNode.getAnchor());
        Toast.makeText(getApplicationContext(),"Now Hosting...", Toast.LENGTH_LONG).show();
    }

    public void onClickClear(View view) {
        if (rootNode != null) {
            rootTNode.setRenderable(null);
            rootTNode = null;
            Anchor anchor = rootNode.getAnchor();
            if (anchor != null) {
                anchor.detach();
            }
            rootNode = null;
        }
        findViewById(R.id.hostBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.remove).setVisibility(View.INVISIBLE);
        view.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        Spinner colorSpinner = findViewById(R.id.color_spinner);
        colorChoice = colorSpinner.getSelectedItem().toString();
        Log.d(TAG, "onItemSelected: color: " + colorChoice);
        updateSelectedNode();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) { }

    private void onTapPlane(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING) {
            //if no anchor exists, create one and move rootNode to its position
            if (rootNode == null) {
                rootNode = new AnchorNode();
                rootNode.setParent(arFragment.getArSceneView().getScene());
                rootNode.setAnchor(hitResult.createAnchor());

                rootTNode = new TransformableNode(arFragment.getTransformationSystem());
                rootTNode.getTranslationController().setEnabled(false);
                rootTNode.setParent(rootNode);
                rootTNode.setRenderable(getRenderable(colorChoice, shapeChoice));

                findViewById(R.id.clear).setVisibility(View.VISIBLE);
            }
            //if an anchor and rootNode exist, create a new transformable node
            else {
                AnchorNode an = new AnchorNode();
                an.setParent(rootNode);
                an.setAnchor(hitResult.createAnchor());

                TransformableNode tNode = new TransformableNode(arFragment.getTransformationSystem());
                tNode.setParent(an);
                tNode.setRenderable(getRenderable(colorChoice, shapeChoice));
                tNode.select();
                findViewById(R.id.hostBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.remove).setVisibility(View.VISIBLE);
            }

        }
    }
}



