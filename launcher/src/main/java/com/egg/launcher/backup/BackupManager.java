package com.egg.launcher.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupManager {

    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    private static final String BACKUP_DIR = "backups/auto";
    private static final int RETENTION_POLICY_COUNT = 10; // Mantener los últimos 10 backups

    public static void createBackup(String reason) {
        logger.info("Iniciando creación de backup por motivo: {}", reason);

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupPath = Paths.get(BACKUP_DIR, timestamp + "_" + reason);
            Files.createDirectories(backupPath);

            // 1. Copiar server.properties
            copyFile(Paths.get("server", "server.properties"), backupPath.resolve("server.properties"));

            // 2. Copiar logs recientes (ej: último log)
            copyLatestLog(Paths.get("logs"), backupPath.resolve("logs"));

            // 3. (Placeholder) Guardar hashes de mods/plugins
            // Esta información ya se genera en el reporte de startup, se podría unificar.
            Files.write(backupPath.resolve("mods_plugins_info.txt"), "Placeholder for hashes".getBytes());

            logger.info("Backup creado exitosamente en: {}", backupPath);

            applyRetentionPolicy();

        } catch (IOException e) {
            logger.error("Ocurrió un error al crear el backup.", e);
        }
    }

    private static void copyFile(Path source, Path destination) {
        if (Files.exists(source)) {
            try {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Copiado: {} -> {}", source, destination);
            } catch (IOException e) {
                logger.error("No se pudo copiar el archivo: {}", source, e);
            }
        } else {
            logger.warn("El archivo a respaldar no existe: {}", source);
        }
    }

    private static void copyLatestLog(Path logDir, Path backupLogDir) throws IOException {
        if (!Files.exists(logDir)) return;

        Files.createDirectories(backupLogDir);

        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(f -> f.toString().endsWith(".log"))
                 .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                 .ifPresent(latestLog -> copyFile(latestLog, backupLogDir.resolve(latestLog.getFileName())));
        }
    }

    private static void applyRetentionPolicy() {
        logger.info("Aplicando política de retención (mantener {} backups)...", RETENTION_POLICY_COUNT);
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) return;

        File[] backups = backupDir.listFiles(File::isDirectory);
        if (backups == null || backups.length <= RETENTION_POLICY_COUNT) {
            logger.info("No se requiere limpieza.");
            return;
        }

        List<File> sortedBackups = Arrays.stream(backups)
                                         .sorted(Comparator.comparing(File::getName).reversed())
                                         .collect(Collectors.toList());

        for (int i = RETENTION_POLICY_COUNT; i < sortedBackups.size(); i++) {
            deleteDirectory(sortedBackups.get(i));
        }
    }

    private static void deleteDirectory(File directory) {
        logger.info("Eliminando backup antiguo: {}", directory.getName());
        try {
            Files.walk(directory.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        } catch (IOException e) {
            logger.error("No se pudo eliminar el directorio de backup: {}", directory.getPath(), e);
        }
    }
}
