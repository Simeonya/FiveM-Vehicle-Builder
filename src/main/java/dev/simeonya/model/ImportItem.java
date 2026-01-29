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

    public ImportItem(Path path) {
        this.path = path.toAbsolutePath();
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
        return Files.isDirectory(path);
    }

    public boolean isRpf() {
        return !Files.isDirectory(path) &&
                path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf");
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