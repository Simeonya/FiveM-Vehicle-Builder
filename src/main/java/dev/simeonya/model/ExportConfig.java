package dev.simeonya.model;

public class ExportConfig {

    private final boolean singleResource;
    private final String singleResourceName;
    private final boolean exportAsZip;
    private final String extractorPath;
    private final boolean cleanupTemp;

    public ExportConfig(boolean singleResource, String singleResourceName, boolean exportAsZip,
                        String extractorPath, boolean cleanupTemp) {
        this.singleResource = singleResource;
        this.singleResourceName = singleResourceName;
        this.exportAsZip = exportAsZip;
        this.extractorPath = extractorPath;
        this.cleanupTemp = cleanupTemp;
    }

    public boolean isSingleResource() {
        return singleResource;
    }

    public String getSingleResourceName() {
        return singleResourceName;
    }

    public boolean isExportAsZip() {
        return exportAsZip;
    }

    public String getExtractorPath() {
        return extractorPath;
    }

    public boolean isCleanupTemp() {
        return cleanupTemp;
    }
}