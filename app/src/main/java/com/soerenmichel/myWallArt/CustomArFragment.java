package com.soerenmichel.myWallArt;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class CustomArFragment extends ArFragment {
    @Override
    protected Config getSessionConfiguration(Session session) {
        Config config = super.getSessionConfiguration(session);
        config.setPlaneFindingMode(Config.PlaneFindingMode.VERTICAL);
        config.setFocusMode(Config.FocusMode.AUTO);
        return config;
    }
}
