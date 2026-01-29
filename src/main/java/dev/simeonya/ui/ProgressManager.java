package dev.simeonya.ui;

import dev.simeonya.model.ProgressMode;
import dev.simeonya.model.ProgressState;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ProgressManager {

    private final ProgressBar progressBar;
    private final Label statusLabel;

    public ProgressManager(ProgressBar progressBar, Label statusLabel) {
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public void setState(ProgressState state) {
        Platform.runLater(() -> {
            statusLabel.setText(state.message());

            if (state.mode() == ProgressMode.RUNNING) {
                if (progressBar.getProgress() < 0) {
                    progressBar.setProgress(0);
                }
            } else if (state.mode() == ProgressMode.DONE) {
                progressBar.setProgress(1);
            }
        });
    }

    public void setFraction(int done, int total) {
        Platform.runLater(() -> {
            double progress = total <= 0 ? 0 : (double) done / (double) total;
            progressBar.setProgress(progress);
        });
    }
}