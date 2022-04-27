package com.example.doormat_skeleton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {
    //Initializing variables
    private EditText textUsername, textPassword, textPasswordConf, textEmail;
    private Button buttonInsert;
    private FloatingActionButton backBtn;
    private ProgressBar progressBar;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        textUsername = findViewById(R.id.username);
        textPassword = findViewById(R.id.password);
        textPasswordConf = findViewById(R.id.password2);
        textEmail = findViewById(R.id.email);
        buttonInsert = findViewById(R.id.sign_in);
        progressBar = findViewById(R.id.progress_bar);

        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String username, password, email, passwordConf;
                username = String.valueOf(textUsername.getText()).trim();
                password = String.valueOf(textPassword.getText()).trim();
                passwordConf = String.valueOf(textPasswordConf.getText()).trim();
                email = String.valueOf(textEmail.getText()).trim();

                if(username.equals("") || password.equals("") || email.equals("")){
                    Toast.makeText(SignUp.this, "All fields must be entered", Toast.LENGTH_SHORT).show();
                }else if(!password.equals(passwordConf)){
                    Toast.makeText(SignUp.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                }else if(!email.matches(emailPattern)){
                    Toast.makeText(SignUp.this, "Email must be a valid address", Toast.LENGTH_SHORT).show();
                }else{
                    progressBar.setVisibility(View.VISIBLE);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //Starting Write and Read data with URL
                            //Creating array for parameters
                            String[] field = new String[3];
                            field[0] = "username";
                            field[1] = "password";
                            field[2] = "email";
                            //Creating array for data
                            String[] data = new String[3];
                            data[0] = username;
                            data[1] = password;
                            data[2] = email;
                            PutData putData = new PutData("http://34.203.214.232/mysite/signup.php", "POST", field, data);
                            if (putData.startPut()) {
                                if (putData.onComplete()) {
                                    progressBar.setVisibility(View.GONE);
                                    String result = putData.getResult();
                                    if(result.equals("Sign Up Success")){
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }else{
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                    }
                                    Log.i("PutData", result);
                                }
                            }
                            //End Write and Read data with URL
                        }
                    });
                }
            }
        });

    }
}