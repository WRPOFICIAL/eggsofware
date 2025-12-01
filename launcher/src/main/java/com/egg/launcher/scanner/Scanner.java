package com.egg.launcher.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Scanner {

    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);

    public static ScanResult runScans() {
        logger.info("Iniciando escaneo de mods y plugins...");
        List<FileScanResult> results = new ArrayList<>();
        boolean hasCriticalErrors = false;

        // Escanear mods
        results.addAll(scanDirectory(new File("mods")));
        // Escanear plugins
        results.addAll(scanDirectory(new File("plugins")));

        for (FileScanResult result : results) {
            if (result.getStatus() == ScanStatus.CRITICAL) {
                hasCriticalErrors = true;
            }
        }

        generateReport(results);

        logger.info("Escaneo completado.");
        return new ScanResult(results, hasCriticalErrors);
    }

    private static List<FileScanResult> scanDirectory(File directory) {
        List<FileScanResult> results = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("El directorio '{}' no existe.", directory.getName());
            return results;
        }

        try (Stream<Path> paths = Files.walk(Paths.get(directory.getPath()))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".jar"))
                 .forEach(path -> {
                     File file = path.toFile();
                     String hash = calculateHash(file);
                     // Placeholder para la lógica de compatibilidad
                     ScanStatus status = checkCompatibility(file);
                     results.add(new FileScanResult(file.getName(), hash, status, ""));
                 });
        } catch (IOException e) {
            logger.error("Error al escanear el directorio '{}'.", directory.getName(), e);
        }

        return results;
    }

    private static String calculateHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("No se pudo calcular el hash para el archivo '{}'.", file.getName(), e);
            return "Error";
        }
    }

    private static ScanStatus checkCompatibility(File file) {
        // Lógica de placeholder - aquí iría la validación de versiones, blacklist, etc.
        return ScanStatus.OK;
    }

    private static void generateReport(List<FileScanResult> results) {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        // Generar reporte en formato de texto
        File txtReportFile = new File(reportsDir, "startup_" + timestamp + ".txt");
        try {
            Files.write(txtReportFile.toPath(), formatTxtReport(results).getBytes());
            logger.info("Reporte de escaneo (TXT) guardado en '{}'.", txtReportFile.getPath());
        } catch (IOException e) {
            logger.error("Error al guardar el reporte de escaneo (TXT).", e);
        }

        // Generar reporte en formato JSON
        File jsonReportFile = new File(reportsDir, "compatibility_" + timestamp + ".json");
        try {
            Files.write(jsonReportFile.toPath(), formatJsonReport(results).getBytes());
            logger.info("Reporte de compatibilidad (JSON) guardado en '{}'.", jsonReportFile.getPath());
        } catch (IOException e) {
            logger.error("Error al guardar el reporte de compatibilidad (JSON).", e);
        }
    }

    private static String formatTxtReport(List<FileScanResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("--- Reporte de Escaneo de Inicio ---\n");
        report.append("Timestamp: ").append(new java.util.Date()).append("\n\n");

        for (FileScanResult result : results) {
            report.append("Archivo: ").append(result.getFileName()).append("\n");
            report.append("  Hash (SHA-256): ").append(result.getHash()).append("\n");
            report.append("  Estado: ").append(result.getStatus()).append("\n");
            if (result.getNotes() != null && !result.getNotes().isEmpty()) {
                report.append("  Notas: ").append(result.getNotes()).append("\n");
            }
            report.append("\n");
        }

        return report.toString();
    }

    private static String formatJsonReport(List<FileScanResult> results) {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < results.size(); i++) {
            FileScanResult result = results.get(i);
            json.append("  {\n");
            json.append("    \"mod\": \"").append(result.getFileName()).append("\",\n");
            json.append("    \"hash\": \"").append(result.getHash()).append("\",\n");
            json.append("    \"status\": \"").append(result.getStatus()).append("\",\n");
            json.append("    \"notes\": \"").append(result.getNotes()).append("\"\n");
            json.append("  }");
            if (i < results.size() - 1) {
                json.append(",\n");
            }
        }
        json.append("\n]");
        return json.toString();
    }
}
