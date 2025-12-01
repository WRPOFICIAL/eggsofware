package com.egg.launcher.scanner;

public class FileScanResult {
    private final String fileName;
    private final String hash;
    private final ScanStatus status;
    private final String notes;

    public FileScanResult(String fileName, String hash, ScanStatus status, String notes) {
        this.fileName = fileName;
        this.hash = hash;
        this.status = status;
        this.notes = notes;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHash() {
        return hash;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
}
