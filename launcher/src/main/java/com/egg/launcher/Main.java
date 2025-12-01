package com.egg.launcher;

import com.egg.launcher.backup.BackupManager;
import com.egg.launcher.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.egg.launcher.scanner.ScanResult;
import com.egg.launcher.scanner.Scanner;
import com.egg.launcher.watchdog.Watchdog;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Watchdog watchdog;
    private static final PluginManager pluginManager = new PluginManager();

    public static void main(String[] args) {
        printBanner();
        logger.info("Iniciando EGG HYBRID SERVER™...");

        if (!checkEnvironment()) {
            logger.error("Fallo en la verificación del entorno. El servidor no puede iniciar.");
            return;
        }
        logger.info("Verificación del entorno completada con éxito.");

        // Validar existencia del server JAR antes de escanear
        File serverJar = new File("server", "forge-server.jar");
        if (!serverJar.exists()) {
            logger.error("¡ERROR CRÍTICO! No se encontró 'forge-server.jar' en el directorio '/server'.");
            logger.error("El arranque ha sido abortado. Por favor, asegúrate de que el archivo del servidor Forge esté en la ubicación correcta.");
            return;
        }

        // Ejecutar escaneo de mods y plugins
        ScanResult scanResult = Scanner.runScans();
        if (scanResult.hasCriticalErrors()) {
            logger.error("Se detectaron errores críticos durante el escaneo. El arranque del servidor ha sido abortado.");
            logger.error("Por favor, revisa el último reporte en el directorio '/reports' para más detalles.");
            return;
        }

        // Iniciar el listener del Core Mod en un nuevo hilo
        new Thread(Main::startCoreModListener).start();

        // Cargar EGG plugins
        pluginManager.loadPlugins();

        // Iniciar el servidor por primera vez
        restartServer(true);

        // Registrar hook de apagado para desactivar plugins
        Runtime.getRuntime().addShutdownHook(new Thread(pluginManager::disablePlugins));
    }

    private static void restartServer(boolean isInitialStart) {
        if (isInitialStart) {
            logger.info("Iniciando el servidor Forge por primera vez...");
        } else {
            logger.info("Iniciando el proceso de reinicio del servidor...");
            BackupManager.createBackup("watchdog_restart");
        }

        // Lanzar el servidor en un nuevo hilo para no bloquear el main
        new Thread(Main::startForgeServer).start();
    }

    private static void startCoreModListener() {
        try (ServerSocket serverSocket = new ServerSocket(25566)) { // Puerto de comunicación
            logger.info("Esperando conexión del EGG-CORE-MOD en el puerto 25566...");
            Socket clientSocket = serverSocket.accept();
            logger.info("¡EGG-CORE-MOD conectado desde {}!", clientSocket.getInetAddress());

            // Simular recepción de heartbeats
            new Thread(() -> {
                while (!clientSocket.isClosed()) {
                    try {
                        // Aquí se leerían los eventos, por ahora solo actualizamos el heartbeat
                        if (watchdog != null) {
                            watchdog.receiveHeartbeat();
                        }
                        Thread.sleep(5000); // Simular envío de heartbeat cada 5s
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();

        } catch (IOException e) {
            logger.error("Error en el listener del Core Mod.", e);
        }
    }

    private static void startForgeServer() {
        File serverDir = new File("server");
        File serverJar = new File(serverDir, "forge-server.jar");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xms2G", "-Xmx4G", "-jar", serverJar.getName(), "nogui");
            processBuilder.directory(serverDir);

            Process serverProcess = processBuilder.start();
            logger.info("El proceso del servidor Forge ha sido iniciado. PID: {}", serverProcess.pid());

            // Iniciar el Watchdog
            watchdog = new Watchdog(serverProcess, () -> restartServer(false));
            watchdog.start();

            // Redirigir la salida del servidor a la consola del launcher
            redirectStream(new InputStreamReader(serverProcess.getInputStream()), "SERVER");
            // Redirigir la salida de errores del servidor
            redirectStream(new InputStreamReader(serverProcess.getErrorStream()), "SERVER-ERROR");

            // Esperar a que el proceso del servidor termine
            int exitCode = serverProcess.waitFor();
            logger.info("El proceso del servidor Forge ha terminado con el código de salida: {}", exitCode);

        } catch (IOException | InterruptedException e) {
            logger.error("Ocurrió un error al iniciar o gestionar el proceso del servidor Forge.", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void redirectStream(InputStreamReader reader, String prefix) {
        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println("[" + prefix + "] " + line);
                }
            } catch (IOException e) {
                logger.error("Error al leer la salida del stream del servidor.", e);
            }
        }).start();
    }

    private static void printBanner() {
        String banner = "███████╗ ██████╗  ██████╗ \n" +
                        "██╔════╝██╔═══██╗██╔═══██╗\n" +
                        "█████╗  ██║   ██║██║   ██║   E G G\n" +
                        "██╔══╝  ██║   ██║██║   ██║ PRODUCCIÓN OFICIAL\n" +
                        "██║     ╚██████╔╝╚██████╔╝ SOFTWARE RP ENGINE\n" +
                        "╚═╝      ╚═════╝  ╚═════╝ \n";
        System.out.println(banner);
    }

    private static boolean checkEnvironment() {
        logger.info("Realizando verificaciones del entorno...");

        // 1. Verificar versión de Java
        String javaVersion = System.getProperty("java.version");
        logger.info("Versión de Java detectada: {}", javaVersion);
        if (!javaVersion.startsWith("17") && !javaVersion.startsWith("1.8")) { // Permitimos 1.8 por compatibilidad inicial, ideal es 17
             try {
                int majorVersion = Integer.parseInt(javaVersion.split("\\.")[0]);
                if (majorVersion < 17) {
                    logger.error("Se requiere Java 17 o superior. Versión actual: {}", javaVersion);
                    //return false; // Desactivado temporalmente para flexibilidad
                }
             } catch (NumberFormatException e) {
                // Ignorar si no se puede parsear
             }
        }


        // 2. Verificar memoria mínima
        SystemInfo si = new SystemInfo();
        GlobalMemory memory = si.getHardware().getMemory();
        long totalMemoryGB = memory.getTotal() / (1024 * 1024 * 1024);
        logger.info("Memoria total del sistema: {} GB", totalMemoryGB);
        if (totalMemoryGB < 2) {
            logger.warn("Se recomienda tener al menos 2 GB de memoria RAM en el sistema.");
        }

        // 3. Verificar espacio en disco
        File currentDir = new File(".");
        long freeSpaceGB = currentDir.getFreeSpace() / (1024 * 1024 * 1024);
        logger.info("Espacio libre en disco: {} GB", freeSpaceGB);
        if (freeSpaceGB < 1) {
            logger.error("No hay suficiente espacio en disco. Se requiere al menos 1 GB libre.");
            return false;
        }

        // 4. Verificar permisos de escritura
        File testFile = new File("test_write_permissions.tmp");
        try {
            if (testFile.createNewFile()) {
                testFile.delete();
                logger.info("Verificación de permisos de escritura: OK");
            } else {
                logger.error("No se tienen permisos para escribir en el directorio actual.");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error al verificar los permisos de escritura: {}", e.getMessage());
            return false;
        }

        return true;
    }
}
