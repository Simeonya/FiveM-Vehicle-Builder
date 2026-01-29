package dev.simeonya.ui;

import dev.simeonya.util.DateTimeUtil;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class LogManager {

    private final TextArea logArea;

    public LogManager(TextArea logArea) {
        this.logArea = logArea;
    }

    public void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(formatLog(msg) + "\n");
            logArea.positionCaret(logArea.getText().length());
        });
    }

    private String formatLog(String msg) {
        String level = "INFO";

        if (msg.startsWith("ERROR")) {
            level = "ERROR";
        } else if (msg.startsWith("WARN")) {
            level = "WARN";
        } else if (msg.startsWith("OK")) {
            level = "OK";
        }

        return "[" + DateTimeUtil.currentTime() + "] " +
                String.format("%-5s", level) + " " +
                msg.replaceFirst("^(ERROR|WARN|OK):?\\s*", "");
    }
}