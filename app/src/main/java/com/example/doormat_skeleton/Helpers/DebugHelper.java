package com.example.doormat_skeleton.Helpers;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.doormat_skeleton.LocationApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class DebugHelper {
    private static final String TAG = "DebugHelper";

    public DebugHelper() {
        Log.d(TAG, "DEBUG_MODE_ON");
    }

    //make sure anything passed here has a readable, useful toString() method
    public void logObjects(String sentTag, ArrayList<Object> objects) {
        Log.d(TAG, sentTag);
        for (Object o : objects) {
            Log.d(TAG, o.toString());
        }
    }
}
