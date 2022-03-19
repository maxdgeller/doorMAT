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
    private EditText textUsername, textPassword, textEmail;
    private Button buttonInsert;
    private FloatingActionButton backBtn;
    private ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        textUsername = findViewById(R.id.username);
        textPassword = findViewById(R.id.password);
        textEmail = findViewById(R.id.email);
        buttonInsert = findViewById(R.id.sign_in);
        progressBar = findViewById(R.id.progress_bar);

        buttonInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String username, password, email;
                username = String.valueOf(textUsername.getText());
                password = String.valueOf(textPassword.getText());
                email = String.valueOf(textEmail.getText());


                if(!username.equals("") && !password.equals("") && !email.equals("")) {
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
                }else{
                    Toast.makeText(getApplicationContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                }

            }
        });






//        // Reference variables by ID's from XML format
//        textUsername = findViewById(R.id.username);
//        textPassword = findViewById(R.id.password);
//        textEmail = findViewById(R.id.email);
//        buttonInsert = findViewById(R.id.sign_in);
//
//        //Set an Onclick Listener
//        buttonInsert.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // Create an Insert Function
//                insertData();
//            }
//        });
//
//    }
//
//    public void back(View v){
//        Intent i = new Intent(this, MainActivity.class);
//        startActivity(i);
//    }
//
//    private void insertData() {
//        //Defining variables for getting info from user input
//        final String username = textUsername.getText().toString();
//        final String password = textPassword.getText().toString();
//        final String email = textEmail.getText().toString();
//
//        //Validate inputs
//        if (username.isEmpty()) {
//            textUsername.setError("Please enter name");
//        } else if (password.isEmpty()) {
//            textPassword.setError("Please enter password");
//        } else if (email.isEmpty()) {
//            textEmail.setError("Please enter email");
//        }else{
//            //Get the insert URL
//            String insert_url = "http://34.203.214.232/mysite/insertData.php";
//            //Communicate with backend
//            StringRequest stringRequest = new StringRequest(Request.Method.POST, insert_url,
//                    new Response.Listener<String>() {
//                        @Override
//                        public void onResponse(String response) {
//                            //Set Response
//                            try {
//                                JSONObject jsonObject = new JSONObject(response);
//                                String success = jsonObject.getString("success");
//
//                                if (success.equals("1")){
//                                    Toast.makeText(SignUp.this, "User data has been inserted", Toast.LENGTH_SHORT ).show();
//
//                                }
//                            }catch (JSONException e){
//                                e.printStackTrace();
//                            }
//                        }
//                    },
//                    new Response.ErrorListener() {
//                        @Override
//                        public void onErrorResponse(VolleyError error) {
//                            Toast.makeText(SignUp.this, "Unable to insert data" + error.toString(), Toast.LENGTH_SHORT).show();
//                        }
//                    }){
//                //Set Parameters that will also match with the API
//
//                @Override
//                protected Map<String, String> getParams() throws AuthFailureError {
//                    //Create hashmap, equivalent to an array
//                    Map<String, String> params = new HashMap<>();
//                    params.put("username", username);
//                    params.put("password", password);
//                    params.put("email", email);
//
//                    return params;
//                }
//            };
//            //Set a requestQueue, enables Volley communication to backend
//            RequestQueue requestQueue = Volley.newRequestQueue(this);
//            requestQueue.add(stringRequest);
//        }
    }
}