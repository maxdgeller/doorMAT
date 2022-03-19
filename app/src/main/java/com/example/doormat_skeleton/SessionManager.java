package com.example.doormat_skeleton;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {

    SharedPreferences sharedPreferences;
    public SharedPreferences.Editor editor;
    public Context context;
    int PRIVATE_MODE = 0;

    private static final String PREF_NAME = "LOGIN";
    private static final String LOGIN = "IS_LOGIN";
    public static final String USERNAME = "USERNAME";

    public SessionManager(Context context){
        this.context = context;
        sharedPreferences = context.getSharedPreferences("LOGIN", PRIVATE_MODE);
        editor = sharedPreferences.edit();
    }

    public void createSession(String username){
        editor.putBoolean(LOGIN, true);
        editor.putString("USERNAME", username);
        editor.apply();
    }

    public boolean isLogin(){
        return sharedPreferences.getBoolean(LOGIN, false);
    }

    public void checkLogin(){
        if (!this.isLogin()){
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
            ((DashboardActivity) context).finish();
        }
    }

    public HashMap<String, String> getUserDetail(){
        HashMap<String, String> user = new HashMap<>();
        user.put(USERNAME, sharedPreferences.getString(USERNAME, null));
        return user;
    }

    public void logout(){
        editor.clear();
        editor.commit();
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
        ((DashboardActivity) context).finish();
    }

}
