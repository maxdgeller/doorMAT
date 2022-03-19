package com.example.doormat_skeleton;

import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.core.Config;
import com.google.ar.core.Session;

public class CustomArFragment extends ArFragment {

    @Override
    protected Config onCreateSessionConfig(Session session){
        Config config = super.onCreateSessionConfig(session);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        return config;
    }
}
