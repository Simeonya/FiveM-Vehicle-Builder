package dev.simeonya;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main extends Application {

    private static final Set<String> STREAM_EXT = Set.of("yft", "ytd", "ydd", "ydr", "ybn", "ymt", "ycd", "ynv", "awc");
    private static final Set<String> META_NAMES = Set.of("vehicles.meta", "carcols.meta", "carvariations.meta", "handling.meta", "vehiclelayouts.meta");

    private final ObservableList<ImportItem> items = FXCollections.observableArrayList();
    private final TextArea logArea = new TextArea();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Ready.");

    private TableView<ImportItem> table;
    private CheckBox singleResourceCheck;
    private TextField singleResourceNameField;
    private CheckBox exportAsZipCheck;
    private TextField extractorPathField;
    private CheckBox cleanupTempCheck;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        var root = new BorderPane();
        root.setPadding(new Insets(16));

        root.setTop(buildHeader(stage));
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        var scene = new Scene(root, 1100, 760);
        applyDarkTheme(scene);

        stage.setTitle("Made with ♥️ by Simeonya | FiveM Vehicle Builder");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader(Stage stage) {
        var title = new Label("FiveM Vehicle Builder\nMade with ♥ by Simeonya");
        title.getStyleClass().add("h1");

        var subtitle = new Label("Drag .rpf files or extracted folders. Edit names, then export to FiveM-ready resource structure.");
        subtitle.getStyleClass().add("muted");

        var dropZone = new StackPane();
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setMinHeight(120);

        var dzTitle = new Label("Drop files here");
        dzTitle.getStyleClass().add("drop-title");

        var dzHint = new Label("Folders work immediately. For .rpf you need an extractor CLI.");
        dzHint.getStyleClass().add("muted");

        var dzBox = new VBox(6, dzTitle, dzHint);
        dzBox.setAlignment(Pos.CENTER);

        dropZone.getChildren().add(dzBox);

        dropZone.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (var f : db.getFiles()) {
                    var p = f.toPath();
                    items.add(new ImportItem(p));
                    log("Added: " + p);
                }
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        singleResourceCheck = new CheckBox("Export as single resource");
        singleResourceCheck.setSelected(true);

        singleResourceNameField = new TextField("vehicles_pack");
        singleResourceNameField.setPrefColumnCount(18);
        singleResourceNameField.disableProperty().bind(singleResourceCheck.selectedProperty().not());

        exportAsZipCheck = new CheckBox("Export as ZIP");
        exportAsZipCheck.setSelected(false);

        cleanupTempCheck = new CheckBox("Cleanup temp extraction");
        cleanupTempCheck.setSelected(true);

        extractorPathField = new TextField();
        extractorPathField.setPromptText("Extractor CLI path (required for .rpf)");
        extractorPathField.setPrefColumnCount(28);

        var browseExtractorBtn = new Button("Browse");
        browseExtractorBtn.setOnAction(a -> {
            var fc = new FileChooser();
            fc.setTitle("Select extractor CLI");
            var f = fc.showOpenDialog(stage);
            if (f != null) extractorPathField.setText(f.toPath().toAbsolutePath().toString());
        });

        var settingsRow1 = new HBox(12, singleResourceCheck, labeled("Single resource name", singleResourceNameField), exportAsZipCheck, cleanupTempCheck);
        settingsRow1.setAlignment(Pos.CENTER_LEFT);

        var settingsRow2 = new HBox(12, labeled("RPF extractor", extractorPathField), browseExtractorBtn);
        settingsRow2.setAlignment(Pos.CENTER_LEFT);

        var header = new VBox(10, title, subtitle, dropZone, settingsRow1, settingsRow2);
        header.setPadding(new Insets(0, 0, 14, 0));
        return header;
    }

    private Node buildCenter() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        var cFile = new TableColumn<ImportItem, String>("Input");
        cFile.setCellValueFactory(v -> v.getValue().inputPathProperty());
        cFile.setPrefWidth(520);

        var cVehicle = new TableColumn<ImportItem, String>("Vehicle name");
        cVehicle.setCellValueFactory(v -> v.getValue().vehicleNameProperty());
        cVehicle.setCellFactory(TextFieldTableCell.forTableColumn());
        cVehicle.setOnEditCommit(e -> e.getRowValue().setVehicleName(sanitize(e.getNewValue())));
        cVehicle.setPrefWidth(200);

        var cResource = new TableColumn<ImportItem, String>("Resource name (if multi)");
        cResource.setCellValueFactory(v -> v.getValue().resourceNameProperty());
        cResource.setCellFactory(TextFieldTableCell.forTableColumn());
        cResource.setOnEditCommit(e -> e.getRowValue().setResourceName(sanitize(e.getNewValue())));
        cResource.setPrefWidth(230);

        var cType = new TableColumn<ImportItem, String>("Type");
        cType.setCellValueFactory(v -> v.getValue().typeProperty());
        cType.setPrefWidth(100);

        table.getColumns().addAll(cFile, cVehicle, cResource, cType);
        table.setItems(items);

        var exportBtn = new Button("Export");
        exportBtn.getStyleClass().add("primary");
        exportBtn.setOnAction(a -> export());

        var removeBtn = new Button("Remove selected");
        removeBtn.setOnAction(a -> removeSelected());

        var clearBtn = new Button("Clear all");
        clearBtn.setOnAction(a -> items.clear());

        var buttons = new HBox(10, exportBtn, removeBtn, clearBtn, spacer());
        buttons.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log");

        var split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(table, logArea);
        split.setDividerPositions(0.62);

        return new VBox(10, buttons, split);
    }

    private VBox buildFooter() {
        progressBar.setPrefWidth(420);
        statusLabel.getStyleClass().add("muted");
        var row = new HBox(12, progressBar, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        var footer = new VBox(10, row);
        footer.setPadding(new Insets(14, 0, 0, 0));
        return footer;
    }

    private void removeSelected() {
        var selected = List.copyOf(table.getSelectionModel().getSelectedItems());
        items.removeAll(selected);
    }

    private void export() {
        if (items.isEmpty()) {
            alert("Nothing to export", "Add at least one .rpf or folder first.");
            return;
        }

        var needsExtractor = items.stream().anyMatch(i -> !i.isDirectory() && i.isRpf());
        if (needsExtractor && !isValidExtractorPath(extractorPathField.getText())) {
            alert("Extractor missing", "You dropped .rpf files. Select an extractor CLI path first.");
            return;
        }

        var chooser = new DirectoryChooser();
        chooser.setTitle("Select export folder");
        var outRoot = chooser.showDialog(table.getScene().getWindow());
        if (outRoot == null) return;

        var singleResource = singleResourceCheck.isSelected();
        var singleName = sanitize(singleResourceNameField.getText());
        var asZip = exportAsZipCheck.isSelected();
        var extractorPath = extractorPathField.getText() == null ? "" : extractorPathField.getText().trim();
        var cleanupTemp = cleanupTempCheck.isSelected();

        runAsync(() -> {
            setProgress(ProgressState.running("Exporting..."));
            try {
                if (singleResource)
                    exportSingleResource(outRoot.toPath(), singleName, asZip, extractorPath, cleanupTemp);
                else exportMultiResource(outRoot.toPath(), asZip, extractorPath, cleanupTemp);
                setProgress(ProgressState.done("Export finished."));
            } catch (Exception e) {
                setProgress(ProgressState.failed("Export failed: " + safeMsg(e)));
                log("ERROR: " + safeMsg(e));
            }
        });
    }

    private void exportSingleResource(Path outRoot, String resourceName, boolean asZip, String extractorPath, boolean cleanupTemp) throws Exception {
        var resourceRoot = outRoot.resolve(resourceName);
        deleteIfExists(resourceRoot);
        Files.createDirectories(resourceRoot);

        manifestWrite(resourceRoot);

        var total = items.size();
        var idx = new AtomicInteger(0);

        for (var item : items) {
            var i = idx.incrementAndGet();
            setProgress(ProgressState.running("Processing " + i + "/" + total + ": " + item.inputPath()));
            processOneItemIntoResource(item, resourceRoot, extractorPath, cleanupTemp);
            setProgressFraction(i, total);
        }

        if (asZip) {
            var zipName = resourceName + "_" + timestamp() + ".zip";
            var zipPath = outRoot.resolve(zipName);
            zipFolder(resourceRoot, zipPath);
            log("ZIP created: " + zipPath);
        } else {
            log("Exported folder: " + resourceRoot);
        }
    }

    private void exportMultiResource(Path outRoot, boolean asZip, String extractorPath, boolean cleanupTemp) throws Exception {
        var total = items.size();
        var idx = new AtomicInteger(0);

        for (var item : items) {
            var i = idx.incrementAndGet();
            setProgress(ProgressState.running("Processing " + i + "/" + total + ": " + item.inputPath()));

            var resourceName = sanitize(item.getResourceName());
            var resourceRoot = outRoot.resolve(resourceName);
            deleteIfExists(resourceRoot);
            Files.createDirectories(resourceRoot);

            manifestWrite(resourceRoot);
            processOneItemIntoResource(item, resourceRoot, extractorPath, cleanupTemp);

            if (asZip) {
                var zipName = resourceName + "_" + timestamp() + ".zip";
                var zipPath = outRoot.resolve(zipName);
                zipFolder(resourceRoot, zipPath);
                log("ZIP created: " + zipPath);
            } else {
                log("Exported folder: " + resourceRoot);
            }

            setProgressFraction(i, total);
        }
    }

    private void processOneItemIntoResource(ImportItem item, Path resourceRoot, String extractorPath, boolean cleanupTemp) throws Exception {
        var vehicleName = sanitize(item.getVehicleName());
        Path extractedRoot = null;
        boolean temp = false;

        if (item.isDirectory()) {
            extractedRoot = item.path();
        } else if (item.isRpf()) {
            extractedRoot = extractRpfToTemp(item.path(), extractorPath);
            extractNestedRpfs(extractedRoot, extractorPath);
            temp = true;
        } else {
            throw new IllegalStateException("Unsupported input: " + item.path());
        }

        var result = mapToFiveMStructure(extractedRoot, resourceRoot, vehicleName);
        log("Mapped [" + vehicleName + "] stream=" + result.streamCount + ", data=" + result.dataCount);

        if (temp && cleanupTemp) deleteIfExists(extractedRoot);
    }

    private static MappingResult mapToFiveMStructure(Path extractedRoot, Path resourceRoot, String vehicleName) throws IOException {
        var streamDir = resourceRoot.resolve("stream").resolve(vehicleName);
        var dataDir = resourceRoot.resolve("data").resolve(vehicleName);

        Files.createDirectories(streamDir);
        Files.createDirectories(dataDir);

        var allFiles = Files.walk(extractedRoot).filter(Files::isRegularFile).collect(Collectors.toList());

        int streamCount = 0;
        int dataCount = 0;

        for (var f : allFiles) {
            var fileName = f.getFileName().toString();
            var lower = fileName.toLowerCase(Locale.ROOT);

            if (META_NAMES.contains(lower)) {
                Files.copy(f, dataDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                dataCount++;
                continue;
            }

            var ext = extension(lower);
            if (ext != null && STREAM_EXT.contains(ext)) {
                Files.copy(f, streamDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                streamCount++;
            }
        }

        return new MappingResult(streamCount, dataCount);
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return null;
        return name.substring(i + 1);
    }

    private static void manifestWrite(Path resourceRoot) throws IOException {
        var content = """
                fx_version 'cerulean'
                game 'gta5'
                
                files {
                  'data/**/vehicles.meta',
                  'data/**/carcols.meta',
                  'data/**/carvariations.meta',
                  'data/**/handling.meta',
                  'data/**/vehiclelayouts.meta'
                }
                
                data_file 'VEHICLE_METADATA_FILE' 'data/**/vehicles.meta'
                data_file 'CARCOLS_FILE' 'data/**/carcols.meta'
                data_file 'VEHICLE_VARIATION_FILE' 'data/**/carvariations.meta'
                data_file 'HANDLING_FILE' 'data/**/handling.meta'
                data_file 'VEHICLE_LAYOUTS_FILE' 'data/**/vehiclelayouts.meta'
                """;

        Files.writeString(resourceRoot.resolve("fxmanifest.lua"), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void zipFolder(Path folder, Path outZip) throws IOException {
        Files.createDirectories(outZip.getParent());
        try (var zos = new ZipOutputStream(Files.newOutputStream(outZip))) {
            var base = folder.toAbsolutePath();
            Files.walk(folder).filter(Files::isRegularFile).forEach(p -> {
                var rel = base.relativize(p.toAbsolutePath()).toString().replace("\\", "/");
                try {
                    zos.putNextEntry(new ZipEntry(rel));
                    zos.write(Files.readAllBytes(p));
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        });
    }

    private static boolean isValidExtractorPath(String p) {
        if (p == null) return false;
        var s = p.trim();
        if (s.isEmpty()) return false;

        Path path;
        try {
            path = Paths.get(s);
        } catch (Exception e) {
            return false;
        }

        if (!Files.exists(path) || !Files.isRegularFile(path)) return false;

        var name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.equals("rpf-cli.exe") || name.endsWith("rpf-cli.exe");
    }

    private static Path extractRpfToTemp(Path rpfFile, String extractorPath) throws Exception {
        var extractor = Paths.get(extractorPath).toAbsolutePath();
        var rpfDir = rpfFile.getParent().toAbsolutePath();
        var tempRoot = Files.createTempDirectory("rpf_cli_root_");

        var cmd = List.of(extractor.toString(), "extract", rpfFile.getFileName().toString());

        var pb = new ProcessBuilder(cmd);
        pb.directory(rpfDir.toFile());
        pb.redirectErrorStream(true);

        var process = pb.start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        var code = process.waitFor();

        if (code != 0) {
            throw new IllegalStateException("rpf-cli failed:\n" + output);
        }

        Path extracted = findExtractedDir(rpfDir, rpfFile.getFileName().toString());
        if (extracted == null) {
            throw new IllegalStateException("rpf-cli finished but no extracted content found");
        }

        Files.move(extracted, tempRoot.resolve(extracted.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        return tempRoot.resolve(extracted.getFileName());
    }


    private static Path findExtractedDir(Path baseDir, String rpfName) throws IOException {
        var plain = rpfName.replace(".rpf", "");
        try (var s = Files.list(baseDir)) {
            for (Path p : s.toList()) {
                if (!Files.isDirectory(p)) continue;
                var n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.equals("dlc") || n.equals(plain) || n.equals(rpfName.toLowerCase(Locale.ROOT))) {
                    return p;
                }
            }
        }
        return null;
    }

    private static List<Path> findNestedRpfs(Path root) throws IOException {
        try (var s = Files.walk(root)) {
            return s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf")).toList();
        }
    }

    private static void extractNestedRpfs(Path root, String extractorPath) throws Exception {
        boolean extracted;

        do {
            extracted = false;
            var rpfs = findNestedRpfs(root);

            for (Path rpf : rpfs) {
                var parent = rpf.getParent();
                var name = rpf.getFileName().toString().toLowerCase(Locale.ROOT);

                if (name.equals("dlc.rpf")) continue;

                var cmd = List.of(extractorPath, "extract", rpf.getFileName().toString());

                var pb = new ProcessBuilder(cmd);
                pb.directory(parent.toFile());
                pb.redirectErrorStream(true);

                var p = pb.start();
                p.getInputStream().readAllBytes();
                var code = p.waitFor();

                if (code == 0) {
                    Files.deleteIfExists(rpf);
                    extracted = true;
                }
            }
        } while (extracted);
    }

    private void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(formatLog(msg) + "\n");
            logArea.positionCaret(logArea.getText().length());
        });
    }

    private static String formatLog(String msg) {
        String level = "INFO";

        if (msg.startsWith("ERROR")) level = "ERROR";
        else if (msg.startsWith("WARN")) level = "WARN";
        else if (msg.startsWith("OK")) level = "OK";

        return "[" + timeNow() + "] " + String.format("%-5s", level) + " " + msg.replaceFirst("^(ERROR|WARN|OK):?\\s*", "");
    }


    private static String timeNow() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private static void runAsync(Runnable r) {
        var t = new Thread(r, "export-worker");
        t.setDaemon(true);
        t.start();
    }

    private void setProgress(ProgressState state) {
        Platform.runLater(() -> {
            statusLabel.setText(state.message);
            if (state.mode == ProgressMode.RUNNING) {
                if (progressBar.getProgress() < 0) progressBar.setProgress(0);
            } else if (state.mode == ProgressMode.DONE) {
                progressBar.setProgress(1);
            }
        });
    }

    private void setProgressFraction(int done, int total) {
        Platform.runLater(() -> progressBar.setProgress(total <= 0 ? 0 : (double) done / (double) total));
    }

    private static String safeMsg(Throwable t) {
        var m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    private void alert(String title, String content) {
        Platform.runLater(() -> {
            var a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(title);
            a.setContentText(content);
            a.getDialogPane().getScene().getStylesheets().add("data:text/css," + encodeCss(DARK_CSS));
            a.showAndWait();
        });
    }

    private static void applyDarkTheme(Scene scene) {
        scene.getStylesheets().add("data:text/css," + encodeCss(DARK_CSS));
        scene.getRoot().getStyleClass().add("root");
    }

    private static String encodeCss(String css) {
        return css.replace("\n", "%0A").replace(" ", "%20").replace("#", "%23").replace("{", "%7B").replace("}", "%7D").replace(":", "%3A").replace(";", "%3B").replace(",", "%2C").replace("\"", "%22").replace("'", "%27").replace("(", "%28").replace(")", "%29");
    }

    private static final String DARK_CSS = """
                .root { -fx-background-color: #0b0f17; -fx-font-family: "Inter","Segoe UI","Arial"; -fx-font-size: 13px; }
                .h1 { -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #e6edf7; }
                .muted { -fx-text-fill: #9aa6b2; }
                .drop-zone { -fx-background-color: #0f1624; -fx-border-color: #27344a; -fx-border-width: 2; -fx-border-radius: 14; -fx-background-radius: 14; }
                .drop-title { -fx-text-fill: #e6edf7; -fx-font-size: 16px; -fx-font-weight: 700; }
                .button { -fx-background-color: #182235; -fx-text-fill: #e6edf7; -fx-background-radius: 12; -fx-padding: 10 14 10 14; -fx-cursor: hand; -fx-border-color: #27344a; -fx-border-radius: 12; }
                .button:hover { -fx-background-color: #1e2b43; }
                .button.primary { -fx-background-color: #2b4cff; -fx-border-color: #2b4cff; -fx-font-weight: 700; }
                .button.primary:hover { -fx-background-color: #3a5cff; }
                .text-field, .text-area { -fx-background-color: #0f1624; -fx-text-fill: #e6edf7; -fx-prompt-text-fill: #6f7c8a; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #27344a; -fx-padding: 10; }
                .check-box { -fx-text-fill: #e6edf7; }
                .table-view { -fx-background-color: #0f1624; -fx-border-color: #27344a; -fx-border-radius: 12; -fx-background-radius: 12; }
                .table-view .column-header-background { -fx-background-color: #0f1624; -fx-background-radius: 12 12 0 0; }
                .table-view .column-header, .table-view .filler { -fx-background-color: transparent; -fx-border-color: #27344a; -fx-border-width: 0 0 1 0; }
                .table-view .column-header .label { -fx-text-fill: #b9c4d0; -fx-font-weight: 700; }
                .table-row-cell { -fx-background-color: transparent; -fx-text-fill: #e6edf7; }
                .table-row-cell:filled:selected { -fx-background-color: rgba(43, 76, 255, 0.25); }
                .table-cell { -fx-border-color: transparent; -fx-text-fill: #e6edf7; }
                .split-pane { -fx-background-color: transparent; }
                .split-pane-divider { -fx-background-color: #27344a; -fx-padding: 0.5; }
                .log {
                    -fx-font-family: "JetBrains Mono","Consolas","Menlo",monospace;
                    -fx-font-size: 12px;
                    -fx-text-fill: #d6deeb;
                    -fx-background-color: #05080f;
                    -fx-control-inner-background: #05080f;
                    -fx-highlight-fill: #2b4cff;
                    -fx-highlight-text-fill: #ffffff;
                    -fx-padding: 12;
                    -fx-border-color: #1f2a40;
                    -fx-border-radius: 12;
                    -fx-background-radius: 12;
                }
                .progress-bar { -fx-accent: #2b4cff; }
            """;

    private static Region spacer() {
        var r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static Node labeled(String label, Control control) {
        var l = new Label(label);
        l.getStyleClass().add("muted");
        var box = new VBox(4, l, control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static String sanitize(String s) {
        if (s == null) return "vehicle";
        var out = s.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        out = out.replaceAll("_+", "_");
        return out.isBlank() ? "vehicle" : out;
    }

    public static final class ImportItem {
        private final StringProperty inputPath = new SimpleStringProperty();
        private final StringProperty vehicleName = new SimpleStringProperty();
        private final StringProperty resourceName = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final Path path;

        public ImportItem(Path path) {
            this.path = path.toAbsolutePath();
            inputPath.set(this.path.toString());
            var guessed = guessNameFromPath(this.path);
            vehicleName.set(guessed);
            resourceName.set(guessed);
            if (Files.isDirectory(this.path)) type.set("Folder");
            else
                type.set(this.path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf") ? "RPF" : "File");
        }

        private static String guessNameFromPath(Path p) {
            var name = p.getFileName().toString();
            var dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            return sanitize(name);
        }

        public Path path() {
            return path;
        }

        public boolean isDirectory() {
            return Files.isDirectory(path);
        }

        public boolean isRpf() {
            return !Files.isDirectory(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf");
        }

        public String getVehicleName() {
            return vehicleName.get();
        }

        public void setVehicleName(String v) {
            vehicleName.set(sanitize(v));
        }

        public String getResourceName() {
            return resourceName.get();
        }

        public void setResourceName(String r) {
            resourceName.set(sanitize(r));
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

    private record MappingResult(int streamCount, int dataCount) {
    }

    private enum ProgressMode {RUNNING, DONE, FAILED}

    private record ProgressState(ProgressMode mode, String message) {
        static ProgressState running(String m) {
            return new ProgressState(ProgressMode.RUNNING, m);
        }

        static ProgressState done(String m) {
            return new ProgressState(ProgressMode.DONE, m);
        }

        static ProgressState failed(String m) {
            return new ProgressState(ProgressMode.FAILED, m);
        }
    }
}
