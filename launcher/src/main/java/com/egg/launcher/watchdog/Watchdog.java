package com.egg.launcher.watchdog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Watchdog implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Watchdog.class);
    private static final int CHECK_INTERVAL_SECONDS = 15;
    private static final int FREEZE_THRESHOLD_SECONDS = 60;
    private static final double CPU_LOAD_THRESHOLD = 90.0;

    private final SystemInfo systemInfo = new SystemInfo();
    private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
    private final GlobalMemory memory = systemInfo.getHardware().getMemory();

    private final Process serverProcess;
    private final Runnable restartCallback;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean isServerResponding = new AtomicBoolean(true);

    public Watchdog(Process serverProcess, Runnable restartCallback) {
        this.serverProcess = serverProcess;
        this.restartCallback = restartCallback;
    }

    public void start() {
        logger.info("Iniciando Watchdog con un intervalo de {} segundos.", CHECK_INTERVAL_SECONDS);
        scheduler.scheduleAtFixedRate(this, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        logger.info("Deteniendo Watchdog...");
        scheduler.shutdownNow();
    }

    public void receiveHeartbeat() {
        lastHeartbeat.set(System.currentTimeMillis());
        if (!isServerResponding.get()) {
            logger.info("El servidor ha vuelto a responder.");
            isServerResponding.set(true);
        }
    }

    @Override
    public void run() {
        if (!serverProcess.isAlive()) {
            logger.warn("El proceso del servidor no está activo. El Watchdog no tomará ninguna acción.");
            stop();
            return;
        }

        logSystemMetrics();
        checkServerResponsiveness();
    }

    private void logSystemMetrics() {
        double cpuLoad = processor.getSystemCpuLoad(1000) * 100;
        long usedMemoryMB = (memory.getTotal() - memory.getAvailable()) / (1024 * 1024);
        long totalMemoryMB = memory.getTotal() / (1024 * 1024);

        logger.info(String.format("Métricas del sistema -> CPU: %.2f%%, RAM: %d/%d MB", cpuLoad, usedMemoryMB, totalMemoryMB));

        if (cpuLoad > CPU_LOAD_THRESHOLD) {
            logger.warn("¡ALERTA! El uso de la CPU es superior al {}%.", CPU_LOAD_THRESHOLD);
        }
    }

    private void checkServerResponsiveness() {
        long timeSinceLastHeartbeat = (System.currentTimeMillis() - lastHeartbeat.get()) / 1000;

        if (timeSinceLastHeartbeat > FREEZE_THRESHOLD_SECONDS) {
            if (isServerResponding.get()) {
                logger.error("¡El servidor ha dejado de responder por más de {} segundos!", FREEZE_THRESHOLD_SECONDS);
                isServerResponding.set(false);
                triggerRestart();
            }
        } else {
            if (!isServerResponding.get()) {
                 logger.info("El servidor ha recuperado la capacidad de respuesta.");
            }
            isServerResponding.set(true);
        }
    }

    private void triggerRestart() {
        logger.warn("El Watchdog ha detectado un servidor congelado. Se iniciará el proceso de reinicio...");

        // Llamar al callback para reiniciar el servidor de forma segura
        // (que debería incluir backups, etc.)
        if (restartCallback != null) {
            new Thread(restartCallback).start();
        }

        // Detener el watchdog actual
        stop();
    }
}
