package dev.simeonya.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class UIComponentFactory {

    private UIComponentFactory() {
    }

    public static Region createSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Node createLabeledControl(String label, Control control) {
        Label l = new Label(label);
        l.getStyleClass().add("muted");

        VBox box = new VBox(4, l, control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}