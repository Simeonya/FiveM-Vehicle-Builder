package dev.simeonya.ui;

import dev.simeonya.model.ExportConfig;
import dev.simeonya.model.ImportItem;
import dev.simeonya.model.ProgressState;
import dev.simeonya.service.ExportService;
import dev.simeonya.service.RpfExtractorService;
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
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

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

    private LogManager logManager;
    private ProgressManager progressManager;
    private final ExportService exportService;

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

        Scene scene = new Scene(root, 1100, 760);
        ThemeManager.applyDarkTheme(scene);

        stage.setTitle("Made with ♥️ by Simeonya | FiveM Vehicle Builder");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label title = new Label("FiveM Vehicle Builder\nMade with ♥ by Simeonya");
        title.getStyleClass().add("h1");

        Label subtitle = new Label("Drag .rpf files or extracted folders. Edit names, then export to FiveM-ready resource structure.");
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

        HBox settingsRow1 = new HBox(12, singleResourceCheck, UIComponentFactory.createLabeledControl("Single resource name", singleResourceNameField), exportAsZipCheck, cleanupTempCheck);
        settingsRow1.setAlignment(Pos.CENTER_LEFT);

        HBox settingsRow2 = new HBox(12, UIComponentFactory.createLabeledControl("RPF extractor", extractorPathField), browseExtractorBtn);
        settingsRow2.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(10, title, subtitle, dropZone, settingsRow1, settingsRow2);
        header.setPadding(new Insets(0, 0, 14, 0));
        return header;
    }

    private StackPane createDropZone() {
        StackPane dropZone = new StackPane();
        dropZone.getStyleClass().add("drop-zone");
        dropZone.setMinHeight(120);

        Label dzTitle = new Label("Drop files here");
        dzTitle.getStyleClass().add("drop-title");

        Label dzHint = new Label("Folders work immediately. For .rpf you need an extractor CLI.");
        dzHint.getStyleClass().add("muted");

        VBox dzBox = new VBox(6, dzTitle, dzHint);
        dzBox.setAlignment(Pos.CENTER);
        dropZone.getChildren().add(dzBox);

        dropZone.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (File f : db.getFiles()) {
                    items.add(new ImportItem(f.toPath()));
                    logManager.log("Added: " + f.toPath());
                }
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        return dropZone;
    }

    private Node buildCenter() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        TableColumn<ImportItem, String> cFile = new TableColumn<>("Input");
        cFile.setCellValueFactory(v -> v.getValue().inputPathProperty());
        cFile.setPrefWidth(520);

        TableColumn<ImportItem, String> cVehicle = new TableColumn<>("Vehicle name");
        cVehicle.setCellValueFactory(v -> v.getValue().vehicleNameProperty());
        cVehicle.setCellFactory(TextFieldTableCell.forTableColumn());
        cVehicle.setOnEditCommit(e -> e.getRowValue().setVehicleName(StringUtil.sanitize(e.getNewValue())));
        cVehicle.setPrefWidth(200);

        TableColumn<ImportItem, String> cResource = new TableColumn<>("Resource name (if multi)");
        cResource.setCellValueFactory(v -> v.getValue().resourceNameProperty());
        cResource.setCellFactory(TextFieldTableCell.forTableColumn());
        cResource.setOnEditCommit(e -> e.getRowValue().setResourceName(StringUtil.sanitize(e.getNewValue())));
        cResource.setPrefWidth(230);

        TableColumn<ImportItem, String> cType = new TableColumn<>("Type");
        cType.setCellValueFactory(v -> v.getValue().typeProperty());
        cType.setPrefWidth(100);

        table.getColumns().addAll(cFile, cVehicle, cResource, cType);
        table.setItems(items);

        Button exportBtn = new Button("Export");
        exportBtn.getStyleClass().add("primary");
        exportBtn.setOnAction(e -> export());

        Button removeBtn = new Button("Remove selected");
        removeBtn.setOnAction(e -> removeSelected());

        Button clearBtn = new Button("Clear all");
        clearBtn.setOnAction(e -> items.clear());

        HBox buttons = new HBox(10, exportBtn, removeBtn, clearBtn, UIComponentFactory.createSpacer());
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

    private VBox buildFooter() {
        progressBar.setPrefWidth(420);
        statusLabel.getStyleClass().add("muted");

        HBox row = new HBox(12, progressBar, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(10, row);
        footer.setPadding(new Insets(14, 0, 0, 0));
        return footer;
    }

    private void browseExtractor() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select extractor CLI");
        File f = fc.showOpenDialog(stage);

        if (f != null) {
            extractorPathField.setText(f.toPath().toAbsolutePath().toString());
        }
    }

    private void removeSelected() {
        List<ImportItem> selected = List.copyOf(table.getSelectionModel().getSelectedItems());
        items.removeAll(selected);
    }

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

        if (outRoot == null) {
            return;
        }

        ExportConfig config = new ExportConfig(singleResourceCheck.isSelected(), StringUtil.sanitize(singleResourceNameField.getText()), exportAsZipCheck.isSelected(), extractorPathField.getText() == null ? "" : extractorPathField.getText().trim(), cleanupTempCheck.isSelected());

        runAsync(() -> {
            progressManager.setState(ProgressState.running("Exporting..."));

            try {
                exportService.export(outRoot.toPath(), List.copyOf(items), config, logManager::log, progressManager::setFraction);
                progressManager.setState(ProgressState.done("Export finished."));
            } catch (Exception e) {
                progressManager.setState(ProgressState.failed("Export failed: " + StringUtil.safeMessage(e)));
                logManager.log("ERROR: " + StringUtil.safeMessage(e));
            }
        });
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