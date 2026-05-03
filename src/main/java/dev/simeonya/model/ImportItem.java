package dev.simeonya.model;

import dev.simeonya.util.StringUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ImportItem {

    private final StringProperty inputPath = new SimpleStringProperty();
    private final StringProperty vehicleName = new SimpleStringProperty();
    private final StringProperty resourceName = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final Path path;

    /**
     * True when this item was restored from a .sfvb project file.
     * Its files are already extracted into a temp directory, so the RPF
     * extractor must never be invoked again for this item.
     */
    private final boolean fromProject;

    public ImportItem(Path path) {
        this.path = path.toAbsolutePath();
        this.fromProject = false;
        inputPath.set(this.path.toString());

        String guessed = guessNameFromPath(this.path);
        vehicleName.set(guessed);
        resourceName.set(guessed);

        if (Files.isDirectory(this.path)) {
            type.set("Folder");
        } else {
            type.set(this.path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf") ? "RPF" : "File");
        }
    }

    /**
     * Deserialisation constructor — files are already extracted into a temp
     * directory. isRpf() always returns false for these items so the extractor
     * is never called again.
     */
    public ImportItem(Path extractedTempDir, String vehicleName, String resourceName) {
        this.path = extractedTempDir.toAbsolutePath();
        this.fromProject = true;
        inputPath.set(this.path.toString());

        this.vehicleName.set(StringUtil.sanitize(vehicleName));
        this.resourceName.set(StringUtil.sanitize(resourceName));
        type.set("Project");
    }

    private static String guessNameFromPath(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return StringUtil.sanitize(name);
    }

    public Path path() {
        return path;
    }

    public boolean isDirectory() {
        return fromProject || Files.isDirectory(path);
    }

    public boolean isRpf() {
        if (fromProject) return false;
        return !Files.isDirectory(path) &&
                path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf");
    }

    public boolean isFromProject() {
        return fromProject;
    }

    public String getVehicleName() {
        return vehicleName.get();
    }

    public void setVehicleName(String v) {
        vehicleName.set(StringUtil.sanitize(v));
    }

    public String getResourceName() {
        return resourceName.get();
    }

    public void setResourceName(String r) {
        resourceName.set(StringUtil.sanitize(r));
    }

    public String inputPath() {
        return inputPath.get();
    }

    public StringProperty inputPathProperty() {
        return inputPath;
    }

    public StringProperty vehicleNameProperty() {
        return vehicleName;
    }

    public StringProperty resourceNameProperty() {
        return resourceName;
    }

    public StringProperty typeProperty() {
        return type;
    }
}
