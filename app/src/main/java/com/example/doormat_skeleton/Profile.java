package com.example.doormat_skeleton;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.doormat_skeleton.databinding.ActivityDashboardBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Profile extends DrawerBaseActivity {

    public static final String KEY_CONNECTIONS = "KEY_CONNECTIONS";
    ActivityDashboardBinding activityDashboardBinding;
    SessionManager sessionManager;
    DoormatManager doormatManager;
    StoreManager storeManager;
    HashSet<UserData.Doormat> mDoormats;
    String mName;
    String mEmail;




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

        TextView setName = findViewById(R.id.usertag);
        setName.setText(mName);

//        mDoormats = LocationApplication.getCurrentDoormats();

        storeManager = new StoreManager();

//        String connectionsJSONString = getPreferences(MODE_PRIVATE).getString(KEY_CONNECTIONS, null);
//        Type type = new TypeToken< List < UserData.Doormat >>() {}.getType();
//        mDoormats = new Gson().fromJson(connectionsJSONString, type);

        int placedMats = countMats();

        TextView placed = findViewById(R.id.placed);
        placed.setText(String.valueOf(placedMats));

        ImageButton updateEmail = findViewById(R.id.updateEmailBtn);
        ImageButton updatePassword = findViewById(R.id.updatePasswordBtn);

        updateEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText email = new EditText(Profile.this);
                email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

                AlertDialog.Builder emailBuilder = new AlertDialog.Builder(Profile.this);

                emailBuilder.setView(email);
                emailBuilder.setMessage("Enter new email: ");

                emailBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mEmail = email.getText().toString();
//                        storeManager.updateEmail(Profile.this, mName, mEmail);
                        Log.d(TAG, "onClick: " + mEmail);
                    }
                });
                emailBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                emailBuilder.show();
            }
        });

        updatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                AlertDialog.Builder passwordBuilder = new AlertDialog.Builder(Profile.this);
                LayoutInflater inflater = Profile.this.getLayoutInflater();

                View mView = getLayoutInflater().inflate(R.layout.password_update, null);
                passwordBuilder.setView(mView);
                passwordBuilder.setMessage("Enter new password, then confirm password: ");

                passwordBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        EditText setPass = (EditText) mView.findViewById(R.id.password1);
                        EditText confPass = (EditText) mView.findViewById(R.id.password_confirm);

                        String passSet = setPass.getText().toString();
                        String passConf = confPass.getText().toString();

                        if(passSet.equals(passConf)){
                            storeManager.updatePassword(Profile.this, mName, passSet);
                        }else{
                            Toast.makeText(Profile.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                passwordBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                passwordBuilder.show();

            }
        });
    }

    public int countMats(){
        int placedMats = 0;
        if(mDoormats != null) {
            for (UserData.Doormat d : mDoormats) {
                if (d.getCreated_by().equals(mName)) {
                    placedMats++;
                }
            }
        }
        return placedMats;
    }
}