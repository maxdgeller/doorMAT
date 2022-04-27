package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

public class Profile extends DrawerBaseActivity implements CountRetrieval.VolleyCallback {

    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";
    ActivityDashboardBinding activityDashboardBinding;
    SessionManager sessionManager;
    AnchorRetrieval anchorRetrieval;
    CountRetrieval countRetrieval;
    HashSet<AnchorResult.DatabaseAnchor> mDatabaseAnchors;
    String mName;
    StoreManager storeManager;
    String mEmail;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    int placedMats;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDashboardBinding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_profile);

        ImageButton backBtn = findViewById(R.id.goBack);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            }
        });

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();
        HashMap<String, String> user = sessionManager.getUserDetail();
        mName = user.get(sessionManager.USERNAME);

        storeManager = new StoreManager();

        TextView setName = findViewById(R.id.usertag);
        setName.setText(mName);


        countRetrieval = new CountRetrieval();
        countRetrieval.getUserCount(mName);
        countRetrieval.setVolleyCallback(this);

        ImageButton updateEmail = findViewById(R.id.updateEmailBtn);
        ImageButton updatePassword = findViewById(R.id.updatePasswordBtn);

        updateEmail.setOnClickListener(view -> showEmailChange());

        updatePassword.setOnClickListener(view -> showPasswordChange());
    }

    @Override
    public void onSuccessResponse(String result) throws JSONException {
        TextView placed = findViewById(R.id.placed);
        placed.setText(result);
        Log.d(TAG, "onSuccessResponse: " + result);
    }


    private void showPasswordChange(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.update_password);
        Button ok = dialog.findViewById(R.id.okayBtn);
        Button cancel = dialog.findViewById(R.id.cancelBtn);

        EditText setPass = dialog.findViewById(R.id.enterPassBtn);
        EditText confPass = dialog.findViewById(R.id.confPassBtn);


        ok.setOnClickListener(view -> {
            String pass = setPass.getText().toString().trim();
            String conf = confPass.getText().toString().trim();
            if(!pass.equals(conf)) {
                Toast.makeText(Profile.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }else{
                if(pass.isEmpty() || conf.isEmpty()){
                    Toast.makeText(Profile.this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                }
                else{
                    storeManager.updatePassword(Profile.this, mName, pass);
                    dialog.dismiss();
                }
            }
        });

        cancel.setOnClickListener(view -> dialog.cancel());

        dialog.show();
    }

    private void showEmailChange(){
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.update_email);
        Button ok = dialog.findViewById(R.id.emailOkayBtn);
        Button cancel = dialog.findViewById(R.id.emailCancelBtn);

        EditText setEmail = dialog.findViewById(R.id.enterEmailBtn);
        EditText confEmail = dialog.findViewById(R.id.confEmailBtn);


        ok.setOnClickListener(view -> {
            String email = setEmail.getText().toString().trim();
            String conf = confEmail.getText().toString().trim();
            if(!email.equals(conf)) {
                Toast.makeText(Profile.this, "Emails do not match", Toast.LENGTH_SHORT).show();
            }else{
                if(email.isEmpty() || conf.isEmpty()){
                    Toast.makeText(Profile.this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
                }else if(!email.matches(emailPattern)){
                    Toast.makeText(Profile.this, "Email must be a valid address", Toast.LENGTH_SHORT).show();
                }else{
                    storeManager.updateEmail(Profile.this, mName, email);
                    dialog.dismiss();
                }
            }
        });

        cancel.setOnClickListener(view -> dialog.cancel());

        dialog.show();
    }

}