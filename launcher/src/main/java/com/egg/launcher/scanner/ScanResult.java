package com.egg.launcher.scanner;

import java.util.List;

public class ScanResult {
    private final List<FileScanResult> results;
    private final boolean hasCriticalErrors;

    public ScanResult(List<FileScanResult> results, boolean hasCriticalErrors) {
        this.results = results;
        this.hasCriticalErrors = hasCriticalErrors;
    }

    public List<FileScanResult> getResults() {
        return results;
    }

    public boolean hasCriticalErrors() {
        return hasCriticalErrors;
    }
}
