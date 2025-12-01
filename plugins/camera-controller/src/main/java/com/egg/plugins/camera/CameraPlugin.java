package com.egg.plugins.camera;

import com.egg.launcher.plugin.EggPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraPlugin implements EggPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CameraPlugin.class);

    @Override
    public void onEnable() {
        logger.info("¡Plugin 'camera-controller' activado!");
        // Aquí se registrarían los listeners de eventos, etc.
    }

    @Override
    public void onDisable() {
        logger.info("Plugin 'camera-controller' desactivado.");
    }
}
