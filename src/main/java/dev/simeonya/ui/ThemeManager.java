package dev.simeonya.ui;

import javafx.scene.Scene;

public final class ThemeManager {

    private ThemeManager() {
    }

    public static void applyDarkTheme(Scene scene) {
        scene.getStylesheets().add("data:text/css," + encodeCss(DARK_CSS));
        scene.getRoot().getStyleClass().add("root");
    }

    private static String encodeCss(String css) {
        return css.replace("\n", "%0A")
                .replace(" ", "%20")
                .replace("#", "%23")
                .replace("{", "%7B")
                .replace("}", "%7D")
                .replace(":", "%3A")
                .replace(";", "%3B")
                .replace(",", "%2C")
                .replace("\"", "%22")
                .replace("'", "%27")
                .replace("(", "%28")
                .replace(")", "%29");
    }

    private static final String DARK_CSS = """
.root {
    -fx-background-color: #0b0f17;
    -fx-font-family: "Inter","Segoe UI","Arial";
    -fx-font-size: 13px;
}

.h1 {
    -fx-font-size: 20px;
    -fx-font-weight: 800;
    -fx-text-fill: #e6edf7;
}

.muted {
    -fx-text-fill: #9aa6b2;
}

.drop-zone {
    -fx-background-color: #0f1624;
    -fx-border-color: #27344a;
    -fx-border-width: 2;
    -fx-border-radius: 14;
    -fx-background-radius: 14;
}

.drop-title {
    -fx-text-fill: #e6edf7;
    -fx-font-size: 16px;
    -fx-font-weight: 700;
}

.button {
    -fx-background-color: #182235;
    -fx-text-fill: #e6edf7;
    -fx-background-radius: 12;
    -fx-padding: 10 14 10 14;
    -fx-cursor: hand;
    -fx-border-color: #27344a;
    -fx-border-radius: 12;
}

.button:hover {
    -fx-background-color: #1e2b43;
}

.button.primary {
    -fx-background-color: #2b4cff;
    -fx-border-color: #2b4cff;
    -fx-font-weight: 700;
}

.button.primary:hover {
    -fx-background-color: #3a5cff;
}

.text-field, .text-area {
    -fx-background-color: #0f1624;
    -fx-text-fill: #e6edf7;
    -fx-prompt-text-fill: #6f7c8a;
    -fx-background-radius: 12;
    -fx-border-radius: 12;
    -fx-border-color: #27344a;
    -fx-padding: 10;
}

.check-box {
    -fx-text-fill: #e6edf7;
}

.table-view {
    -fx-background-color: #0f1624;
    -fx-border-color: #27344a;
    -fx-border-radius: 12;
    -fx-background-radius: 12;
}

.table-view .column-header-background {
    -fx-background-color: #0f1624;
    -fx-background-radius: 12 12 0 0;
}

.table-view .column-header, .table-view .filler {
    -fx-background-color: transparent;
    -fx-border-color: #27344a;
    -fx-border-width: 0 0 1 0;
}

.table-view .column-header .label {
    -fx-text-fill: #b9c4d0;
    -fx-font-weight: 700;
}

.table-row-cell {
    -fx-background-color: transparent;
    -fx-text-fill: #e6edf7;
}

.table-row-cell:filled:selected {
    -fx-background-color: rgba(43, 76, 255, 0.25);
}

.table-cell {
    -fx-border-color: transparent;
    -fx-text-fill: #e6edf7;
}

.split-pane {
    -fx-background-color: transparent;
}

.split-pane-divider {
    -fx-background-color: #27344a;
    -fx-padding: 0.5;
}

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

.progress-bar {
    -fx-accent: #2b4cff;
}
""";
}