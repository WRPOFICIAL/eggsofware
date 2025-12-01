package com.egg.launcher.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    private final List<EggPlugin> loadedPlugins = new ArrayList<>();

    public void loadPlugins() {
        logger.info("Cargando EGG Plugins desde el directorio '/egg_plugins'...");
        File pluginDir = new File("egg_plugins");

        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            logger.warn("El directorio '/egg_plugins' no existe.");
            return;
        }

        File[] pluginJars = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (pluginJars == null) return;

        for (File jarFile : pluginJars) {
            loadPlugin(jarFile);
        }

        logger.info("Se cargaron {} EGG Plugins.", loadedPlugins.size());
        enablePlugins();
    }

    private void loadPlugin(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            // 1. Leer egg-plugin.yml
            InputStream ymlStream = jar.getInputStream(jar.getEntry("egg-plugin.yml"));
            if (ymlStream == null) {
                logger.error("El archivo '{}' no contiene 'egg-plugin.yml'.", jarFile.getName());
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(ymlStream);
            String mainClass = (String) config.get("main");
            String name = (String) config.get("name");

            logger.info("Cargando plugin: {} (main: {})", name, mainClass);

            // 2. Cargar la clase principal
            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());
            Class<?> pluginClass = Class.forName(mainClass, true, classLoader);

            // 3. Instanciar y guardar el plugin
            EggPlugin plugin = (EggPlugin) pluginClass.getDeclaredConstructor().newInstance();
            loadedPlugins.add(plugin);

        } catch (Exception e) {
            logger.error("Error al cargar el plugin '{}'.", jarFile.getName(), e);
        }
    }

    private void enablePlugins() {
        logger.info("Activando {} EGG Plugins...", loadedPlugins.size());
        for (EggPlugin plugin : loadedPlugins) {
            try {
                plugin.onEnable();
            } catch (Exception e) {
                logger.error("Error al activar el plugin '{}'.", plugin.getClass().getSimpleName(), e);
            }
        }
    }

    public void disablePlugins() {
        logger.info("Desactivando {} EGG Plugins...", loadedPlugins.size());
        for (EggPlugin plugin : loadedPlugins) {
            try {
                plugin.onDisable();
            } catch (Exception e) {
                logger.error("Error al desactivar el plugin '{}'.", plugin.getClass().getSimpleName(), e);
            }
        }
    }
}
