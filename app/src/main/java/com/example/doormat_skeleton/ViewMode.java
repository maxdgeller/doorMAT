package com.example.doormat_skeleton;


import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
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

import com.example.doormat_skeleton.Helpers.DebugHelper;
import com.google.android.gms.location.Geofence;
import com.google.ar.core.Anchor;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;

import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.HitTestResult;
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
import com.google.ar.schemas.lull.Quat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ViewMode extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ArFragment arFragment;

    private static final String TAG = "ViewMode";

    String mName;

    /***** State Constants (for convenience) *****/
    private static final Anchor.CloudAnchorState TASK_IN_PROGRESS = Anchor.CloudAnchorState.TASK_IN_PROGRESS;
    private static final Anchor.CloudAnchorState SUCCESS = Anchor.CloudAnchorState.SUCCESS;

    /***** Data from outside ViewMode *****/
    ConcurrentHashMap<String, AnchorResult.DatabaseAnchor> databaseAnchorMap = LocationApplication.getDatabaseAnchorMap();

    /** Queue of IDs of Anchors that need to be resolved. ******/
    private static final Queue<String> idsToResolve = new LinkedList<String>();

    /** Queue of Anchors that are currently resolving. ******/
    private final Queue<Anchor> resolving = new LinkedList<Anchor>();

    /** Queue of AnchorNodes whose Anchors have been resolved. ******/
    private final Queue<AnchorNode> resolved = new LinkedList<AnchorNode>();

    /** Queue of IDs of Anchors that should be detached. ******/
    private static final Queue<String> idsToRemove = new LinkedList<String>();

    /** Resolved child nodes waiting to be rendered. *****/
    private static final Queue<ChildResult.DatabaseChildNode> childrenWaiting = new LinkedList<ChildResult.DatabaseChildNode>();

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

        newIDsToResolve(new ArrayList<Geofence>(LocationApplication.getEnteredGeofences().values()));

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        assert arFragment != null;
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
        arFragment.setOnTapArPlaneListener(this::onTapPlane);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    private void onUpdateFrame(FrameTime frameTime) {
        checkUpdatedAnchor();
    }

    private synchronized void checkUpdatedAnchor() {
        if (!idsToResolve.isEmpty()) { resolveNext(); }
        if (!resolving.isEmpty()) { checkNextResolving(); }
        if (!resolved.isEmpty()) { renderNextChildren(); }
        if (anchorToHost != null) { checkHosting(); }
    }

    private synchronized void resolveNext() {
        Log.i(TAG, "        resolveNext");
        DebugHelper.showShortMessage(getApplicationContext(), "attempting to resolve " + idsToResolve.peek());
        //Each frame, remove a single ID from idsToResolve and add a new anchornode to resolvingAnchors
        Session session = arFragment.getArSceneView().getSession();
        assert session != null;
        ArrayList<String> idList = new ArrayList<String>();
        for (Anchor a : session.getAllAnchors()) {
            idList.add(a.getCloudAnchorId());
        }
        String id = idsToResolve.poll();
        if (!idList.contains(id)) {
            Anchor a = session.resolveCloudAnchor(id);
            resolving.add(a);
        }
    }

    //called in onCreate, and called by GeofenceBroadcastReceiver
    public synchronized void resolveAnchors(Collection<Geofence> geofenceList) {
        Log.i(TAG, "resolveAnchors");
        Session session = arFragment.getArSceneView().getSession();
        assert session != null;
        ArrayList<String> idList = new ArrayList<String>();
        for (Anchor a : session.getAllAnchors()) {
            idList.add(a.getCloudAnchorId());
        }
        for (Geofence g : geofenceList) {
            if (!idList.contains(g.getRequestId())) {
                Anchor a = session.resolveCloudAnchor(g.getRequestId());
                resolving.add(a);
            }
        }
    }

    private synchronized void checkNextResolving() {
        Log.i(TAG, "        checkNextResolving");
        Anchor a = resolving.poll();
        assert a != null;
        Anchor.CloudAnchorState state = a.getCloudAnchorState();

        if (state == TASK_IN_PROGRESS) { resolving.add(a); } //put resolving anchor at end of queue
        else if (state == SUCCESS) {
            logState(a, state);

            AnchorNode anchorNode = new AnchorNode(a);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            anchorNode.setName(a.getCloudAnchorId());
            addToFoundAnchors(a.getCloudAnchorId());

            resolved.add(anchorNode);
        }
        else if (state.isError()) { logState(a, state); a.detach(); } //show error and detach anchor
    }

    private synchronized void renderNextChildren() {
        Log.i(TAG, "        renderNextChildren");
        AnchorNode anchorNode = resolved.poll();
        assert anchorNode != null;

        Anchor a = anchorNode.getAnchor();
        assert a != null;
        Log.d(TAG, "RENDERING ANCHOR: " + a.getCloudAnchorId());
        Log.d(TAG, a.getCloudAnchorState().toString());
        Log.d(TAG, a.getPose().toString());
        Log.d(TAG, a.getTrackingState().toString());

        HashSet<ChildResult.DatabaseChildNode> childNodes = LocationApplication.getNearbyChildNodes();
        for (ChildResult.DatabaseChildNode cn : childNodes) {
            if (cn.getAnchor_id().equals(Objects.requireNonNull(anchorNode.getAnchor()).getCloudAnchorId())) {
                Node node = new Node();
                node.setParent(anchorNode);
                node.setLocalPosition(cn.getPosition());
                node.setWorldScale(cn.getScale());
                node.setWorldRotation(cn.getRotation());
                node.setRenderable(getRenderable(cn.getColor(), cn.getShape()));
            }
        }
    }

    private synchronized void checkHosting() {
        Log.i(TAG, "        checkHosting");
        Anchor.CloudAnchorState hostState = anchorToHost.getCloudAnchorState();

        if (hostState != TASK_IN_PROGRESS) {
            logState(anchorToHost, hostState);
        }

        if (hostState == TASK_IN_PROGRESS) { return; } //skip rest of method if hosting in progress
        if (hostState.isError()) { showState(anchorToHost, hostState); anchorToHost = null; } //failed hosting

        else if (hostState == Anchor.CloudAnchorState.SUCCESS) {
            showState(anchorToHost, hostState);
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
        Log.i(TAG, "                storeAnchor");
        String[] val = madeModelsReverse.get(rootTNode.getRenderable());
        assert val != null;
        Location location = LocationApplication.getLastLocation();
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        storeManager.storeDoormat(this, id, val[0], val[1], lat, lng, mName);
    }

    private synchronized void recursivelyStoreDescendants(Node parent, String id) {
        Log.i(TAG, "                recursivelyStoreDescendants");
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
        DebugHelper.showShortMessage(getApplicationContext(), "ID: " + a.getCloudAnchorId() + "\nState: " + state.name());
    }

    private void logState(Anchor a, Anchor.CloudAnchorState state) {
        Log.d(TAG,"\nID: " + a.getCloudAnchorId() + "\nState: " + state.name());
    }

    //add the ID of a just-resolved anchor to the locally-stored set and update current doormats
    private void addToFoundAnchors(String resolvedAnchorID) {
        Log.i(TAG, "                addToFoundAnchors");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        HashSet<String> foundAnchors = new HashSet<String>(sharedPref.getStringSet("found anchors", new HashSet<String>()));
        foundAnchors.add(resolvedAnchorID);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("found anchors", foundAnchors);
        editor.apply();

        LocationApplication.updateDoormatFound(resolvedAnchorID);

    }

    //methods called by GeofenceBroadcastReceiver upon events being triggered
    public static synchronized void newIDsToResolve(ArrayList<Geofence> triggeredGeofences) {
        Log.i(TAG, "newIDsToResolve");
        for (Geofence g : triggeredGeofences) {
            if (!idsToResolve.contains(g.getRequestId())) {
                Log.d(TAG, "Adding triggered Geofence to idsToResolve, ID: " + g.getRequestId());
                idsToResolve.add(g.getRequestId());
            }
        }
    }

    public static synchronized void newIDsToRemove(ArrayList<Geofence> triggeredGeofences) {
        Log.i(TAG, "newIDsToRemove");
        for (Geofence g : triggeredGeofences) {
            idsToRemove.add(g.getRequestId());
        }
    }
    public void onClickSphere(View view) { shapeChoice = "sphere"; updateSelectedNode("shape"); }
    public void onClickCube(View view) { shapeChoice = "cube"; updateSelectedNode("shape"); }
    public void onClickCylinder(View view) { shapeChoice = "cylinder"; updateSelectedNode("shape"); }

    private void updateSelectedNode(String choiceType) {
        TransformableNode tNode = (TransformableNode) arFragment.getTransformationSystem().getSelectedNode();
        if (tNode != null) {
            String[] val = madeModelsReverse.get(tNode.getRenderable());
            assert val != null;
            if (choiceType.equals("shape")) {
                tNode.setRenderable(getRenderable(val[0], shapeChoice));
            }
            if (choiceType.equals("color")) {
                tNode.setRenderable(getRenderable(colorChoice, val[1]));
            }
        }
    }

    public void onClickBack(View view) {
        this.onBackPressed();
    }

    public void onClickRemove(View view) {
        TransformableNode tNode = (TransformableNode) arFragment.getTransformationSystem().getSelectedNode();
        if (tNode != null) {
            arFragment.getTransformationSystem().selectNode(null);
            tNode.setRenderable(null);
            tNode.setParent(null);
            tNode.setEnabled(false);
        }
        view.setVisibility(View.INVISIBLE);
        if (rootNode == null || rootNode.getChildren().isEmpty()) {
            findViewById(R.id.hostBtn).setVisibility(View.INVISIBLE);
        }
    }

    public void onClickHost(View view) {
        view.setVisibility(View.INVISIBLE);
        Session session = arFragment.getArSceneView().getSession();
        assert session != null;
        anchorToHost = session.hostCloudAnchor(rootNode.getAnchor());
        Toast.makeText(getApplicationContext(),"Now Hosting...", Toast.LENGTH_SHORT).show();
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
        updateSelectedNode("color");
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
                rootTNode.select();
                findViewById(R.id.hostBtn).setVisibility(View.VISIBLE);
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

                tNode.setOnTapListener(new TransformableNode.OnTapListener() {
                    @Override
                    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                        TransformableNode t = (TransformableNode) hitTestResult.getNode();
                        assert t != null;
                        t.select();
                        findViewById(R.id.remove).setVisibility(View.VISIBLE);
                    }
                });

                findViewById(R.id.remove).setVisibility(View.VISIBLE);
            }

        }
    }
}
//2022-04-24 19:26:44.376 27793-27793/com.example.doormat_skeleton D/ViewMode: RESOLVING ANCHOR:
//2022-04-24 19:26:44.376 27793-27793/com.example.doormat_skeleton D/ViewMode: TASK_IN_PROGRESS
//2022-04-24 19:26:44.376 27793-27793/com.example.doormat_skeleton D/ViewMode: t:[x:0.000, y:0.000, z:0.000], q:[x:0.00, y:0.00, z:0.00, w:1.00]
//2022-04-24 19:26:44.376 27793-27793/com.example.doormat_skeleton D/ViewMode: PAUSED

