package dev.simeonya.ui;

import dev.simeonya.model.ExportConfig;
import dev.simeonya.model.ImportItem;
import dev.simeonya.model.ProgressState;
import dev.simeonya.service.ExportService;
import dev.simeonya.service.ProjectService;
import dev.simeonya.service.RpfExtractorService;
import dev.simeonya.service.SpawnNameDetectorService;
import dev.simeonya.util.StringUtil;
import javafx.application.Platform;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class MainWindow {

    private final Stage stage;
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

    // Project state
    private Path currentProjectFile = null;

    private LogManager logManager;
    private ProgressManager progressManager;
    private final ExportService exportService;
    private final SpawnNameDetectorService spawnDetector = new SpawnNameDetectorService();
    private final ProjectService projectService = new ProjectService();

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.exportService = new ExportService();
    }

    public void show() {
        logManager = new LogManager(logArea);
        progressManager = new ProgressManager(progressBar, statusLabel);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 1100, 780);
        ThemeManager.applyDarkTheme(scene);

        // Keyboard shortcuts
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::saveProject);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::saveProjectAs);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openProject);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::newProject);

        updateTitle();
        stage.setScene(scene);
        stage.show();
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private VBox buildHeader() {
        Label title = new Label("FiveM Vehicle Builder\nMade with ♥ by Simeonya");
        title.getStyleClass().add("h1");

        Label subtitle = new Label("Drag .rpf files or extracted folders. Spawn names are auto-detected from vehicles.meta.");
        subtitle.getStyleClass().add("muted");

        StackPane dropZone = createDropZone();

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

        Button browseExtractorBtn = new Button("Browse");
        browseExtractorBtn.setOnAction(e -> browseExtractor());

        HBox settingsRow1 = new HBox(12,
                singleResourceCheck,
                UIComponentFactory.createLabeledControl("Single resource name", singleResourceNameField),
                exportAsZipCheck,
                cleanupTempCheck);
        settingsRow1.setAlignment(Pos.CENTER_LEFT);

        HBox settingsRow2 = new HBox(12,
                UIComponentFactory.createLabeledControl("RPF extractor", extractorPathField),
                browseExtractorBtn);
        settingsRow2.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(10, title, subtitle, buildMenuBar(), dropZone, settingsRow1, settingsRow2);
        header.setPadding(new Insets(0, 0, 14, 0));
        return header;
    }

    private HBox buildMenuBar() {
        Button newBtn  = new Button("⬜ New");
        Button openBtn = new Button("📂 Open");
        Button saveBtn = new Button("💾 Save");
        Button saveAsBtn = new Button("💾 Save As…");

        newBtn.setOnAction(e -> newProject());
        openBtn.setOnAction(e -> openProject());
        saveBtn.setOnAction(e -> saveProject());
        saveAsBtn.setOnAction(e -> saveProjectAs());

        // Tooltip hints
        newBtn.setTooltip(new Tooltip("New project  (Ctrl+N)"));
        openBtn.setTooltip(new Tooltip("Open .sfvb project  (Ctrl+O)"));
        saveBtn.setTooltip(new Tooltip("Save project  (Ctrl+S)"));
        saveAsBtn.setTooltip(new Tooltip("Save project as…  (Ctrl+Shift+S)"));

        HBox bar = new HBox(8, newBtn, openBtn, saveBtn, saveAsBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private StackPane createDropZone() {
        StackPane dropZone = new StackPane();
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setMinHeight(100);

        Label dzTitle = new Label("Drop files here");
        dzTitle.getStyleClass().add("drop-title");

        Label dzHint = new Label("Folders work immediately. Spawn name is detected automatically from vehicles.meta.");
        dzHint.getStyleClass().add("muted");

        VBox dzBox = new VBox(6, dzTitle, dzHint);
        dzBox.setAlignment(Pos.CENTER);
        dropZone.getChildren().add(dzBox);

        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    addItem(f.toPath());
                }
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        return dropZone;
    }

    // ── Center ────────────────────────────────────────────────────────────────

    private Node buildCenter() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        TableColumn<ImportItem, String> cFile = new TableColumn<>("Input");
        cFile.setCellValueFactory(v -> v.getValue().inputPathProperty());
        cFile.setPrefWidth(460);

        TableColumn<ImportItem, String> cVehicle = new TableColumn<>("Vehicle / Spawn name");
        cVehicle.setCellValueFactory(v -> v.getValue().vehicleNameProperty());
        cVehicle.setCellFactory(TextFieldTableCell.forTableColumn());
        cVehicle.setOnEditCommit(e -> {
            e.getRowValue().setVehicleName(StringUtil.sanitize(e.getNewValue()));
            resolveDuplicates();
        });
        cVehicle.setPrefWidth(200);

        TableColumn<ImportItem, String> cResource = new TableColumn<>("Resource name (if multi)");
        cResource.setCellValueFactory(v -> v.getValue().resourceNameProperty());
        cResource.setCellFactory(TextFieldTableCell.forTableColumn());
        cResource.setOnEditCommit(e -> e.getRowValue().setResourceName(StringUtil.sanitize(e.getNewValue())));
        cResource.setPrefWidth(200);

        TableColumn<ImportItem, String> cType = new TableColumn<>("Type");
        cType.setCellValueFactory(v -> v.getValue().typeProperty());
        cType.setPrefWidth(80);

        table.getColumns().addAll(cFile, cVehicle, cResource, cType);
        table.setItems(items);

        Button exportBtn = new Button("Export");
        exportBtn.getStyleClass().add("primary");
        exportBtn.setOnAction(e -> export());

        Button removeBtn = new Button("Remove selected");
        removeBtn.setOnAction(e -> removeSelected());

        Button clearBtn = new Button("Clear all");
        clearBtn.setOnAction(e -> items.clear());

        Button autoDetectBtn = new Button("🔍 Re-detect spawn names");
        autoDetectBtn.setTooltip(new Tooltip("Re-scan vehicles.meta for all items"));
        autoDetectBtn.setOnAction(e -> autoDetectAllSpawnNames());

        HBox buttons = new HBox(10, exportBtn, removeBtn, clearBtn, autoDetectBtn, UIComponentFactory.createSpacer());
        buttons.setAlignment(Pos.CENTER_LEFT);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log");

        SplitPane split = new SplitPane();
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(table, logArea);
        split.setDividerPositions(0.62);

        return new VBox(10, buttons, split);
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private VBox buildFooter() {
        progressBar.setPrefWidth(420);
        statusLabel.getStyleClass().add("muted");

        HBox row = new HBox(12, progressBar, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(10, row);
        footer.setPadding(new Insets(14, 0, 0, 0));
        return footer;
    }

    // ── Add item with auto-detect ─────────────────────────────────────────────

    private void addItem(Path path) {
        ImportItem item = new ImportItem(path);
        tryAutoDetectSpawnName(item);
        items.add(item);
        resolveDuplicates();
        logManager.log("Added: " + path + "  →  spawn: " + item.getVehicleName());
    }

    private void tryAutoDetectSpawnName(ImportItem item) {
        List<String> detected = spawnDetector.detectSpawnNames(item.path());
        if (!detected.isEmpty()) {
            item.setVehicleName(detected.get(0));
            item.setResourceName(detected.get(0));
        }
    }

    private void autoDetectAllSpawnNames() {
        for (ImportItem item : items) {
            List<String> detected = spawnDetector.detectSpawnNames(item.path());
            if (!detected.isEmpty()) {
                item.setVehicleName(detected.get(0));
                logManager.log("Detected [" + item.getVehicleName() + "] for " + item.path().getFileName());
            }
        }
        resolveDuplicates();
        table.refresh();
    }

    // ── Duplicate resolution ──────────────────────────────────────────────────

    /**
     * Ensures all vehicle names in the list are unique.
     * When a duplicate is found the second (and further) occurrences get a
     * numeric suffix: _2, _3, etc.
     */
    private void resolveDuplicates() {
        Map<String, Integer> seen = new LinkedHashMap<>();
        for (ImportItem item : items) {
            String base = item.getVehicleName();
            int count = seen.getOrDefault(base, 0) + 1;
            seen.put(base, count);
            if (count > 1) {
                String unique = base + "_" + count;
                item.setVehicleName(unique);
                item.setResourceName(unique);
                logManager.log("WARN: Duplicate spawn name – renamed to " + unique);
            }
        }
        Platform.runLater(table::refresh);
    }

    // ── Project system ────────────────────────────────────────────────────────

    private void newProject() {
        if (!items.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Discard current project and start fresh?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("New Project");
            confirm.setHeaderText("Unsaved changes will be lost.");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.YES) return;
        }
        items.clear();
        currentProjectFile = null;
        singleResourceCheck.setSelected(true);
        singleResourceNameField.setText("vehicles_pack");
        exportAsZipCheck.setSelected(false);
        cleanupTempCheck.setSelected(true);
        extractorPathField.clear();
        logArea.clear();
        updateTitle();
        logManager.log("New project created.");
    }

    private void openProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Project");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("FiveM Vehicle Builder Project (*.sfvb)", "*.sfvb"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        try {
            ProjectService.ProjectData data = projectService.load(f.toPath());
            items.setAll(data.items());
            singleResourceCheck.setSelected(data.singleResource());
            singleResourceNameField.setText(data.resourceName());
            exportAsZipCheck.setSelected(data.exportAsZip());
            cleanupTempCheck.setSelected(data.cleanupTemp());
            extractorPathField.setText(data.extractorPath());
            currentProjectFile = f.toPath();
            updateTitle();
            logManager.log("Project loaded: " + f.toPath());
        } catch (Exception ex) {
            showAlert("Open failed", ex.getMessage());
        }
    }

    private void saveProject() {
        if (currentProjectFile == null) {
            saveProjectAs();
            return;
        }
        persistProject(currentProjectFile);
    }

    private void saveProjectAs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Project As");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("FiveM Vehicle Builder Project (*.sfvb)", "*.sfvb"));
        if (currentProjectFile != null) {
            fc.setInitialFileName(currentProjectFile.getFileName().toString());
            fc.setInitialDirectory(currentProjectFile.getParent().toFile());
        } else {
            fc.setInitialFileName("my_vehicles.sfvb");
        }
        File f = fc.showSaveDialog(stage);
        if (f == null) return;

        // Ensure .sfvb extension
        Path target = f.toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sfvb")) {
            target = target.resolveSibling(target.getFileName() + ".sfvb");
        }
        currentProjectFile = target;
        persistProject(currentProjectFile);
    }

    private void persistProject(Path target) {
        List<ImportItem> snapshot   = List.copyOf(items);
        boolean          single     = singleResourceCheck.isSelected();
        String           resName    = StringUtil.sanitize(singleResourceNameField.getText());
        boolean          asZip      = exportAsZipCheck.isSelected();
        boolean          cleanup    = cleanupTempCheck.isSelected();
        String           extractor  = extractorPathField.getText() == null ? "" : extractorPathField.getText().trim();

        progressManager.setState(dev.simeonya.model.ProgressState.running("Saving project..."));

        runAsync(() -> {
            try {
                projectService.save(target, snapshot, single, resName, asZip, cleanup, extractor,
                        logManager::log);
                Platform.runLater(() -> {
                    updateTitle();
                    progressManager.setState(dev.simeonya.model.ProgressState.done("Project saved."));
                    logManager.log("Project saved: " + target);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progressManager.setState(dev.simeonya.model.ProgressState.failed("Save failed."));
                    showAlert("Save failed", ex.getMessage());
                });
            }
        });
    }

    private void updateTitle() {
        String file = currentProjectFile == null ? "Unsaved Project" : currentProjectFile.getFileName().toString();
        stage.setTitle("Made with ♥ by Simeonya | FiveM Vehicle Builder  —  " + file);
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private void export() {
        if (items.isEmpty()) {
            showAlert("Nothing to export", "Add at least one .rpf or folder first.");
            return;
        }

        boolean needsExtractor = items.stream().anyMatch(i -> !i.isDirectory() && i.isRpf());
        if (needsExtractor && !RpfExtractorService.isValidExtractorPath(extractorPathField.getText())) {
            showAlert("Extractor missing", "You dropped .rpf files. Select an extractor CLI path first.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select export folder");
        File outRoot = chooser.showDialog(stage);
        if (outRoot == null) return;

        ExportConfig config = new ExportConfig(
                singleResourceCheck.isSelected(),
                StringUtil.sanitize(singleResourceNameField.getText()),
                exportAsZipCheck.isSelected(),
                extractorPathField.getText() == null ? "" : extractorPathField.getText().trim(),
                cleanupTempCheck.isSelected());

        runAsync(() -> {
            progressManager.setState(ProgressState.running("Exporting..."));
            try {
                exportService.export(outRoot.toPath(), List.copyOf(items), config,
                        logManager::log, progressManager::setFraction);
                progressManager.setState(ProgressState.done("Export finished."));
            } catch (Exception e) {
                progressManager.setState(ProgressState.failed("Export failed: " + StringUtil.safeMessage(e)));
                logManager.log("ERROR: " + StringUtil.safeMessage(e));
            }
        });
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private void browseExtractor() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select extractor CLI");
        File f = fc.showOpenDialog(stage);
        if (f != null) extractorPathField.setText(f.toPath().toAbsolutePath().toString());
    }

    private void removeSelected() {
        List<ImportItem> selected = List.copyOf(table.getSelectionModel().getSelectedItems());
        items.removeAll(selected);
        resolveDuplicates();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(title);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    private void runAsync(Runnable r) {
        Thread t = new Thread(r, "export-worker");
        t.setDaemon(true);
        t.start();
    }
}
