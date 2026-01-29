package dev.simeonya.model;

public record ProgressState(ProgressMode mode, String message) {

    public static ProgressState running(String m) {
        return new ProgressState(ProgressMode.RUNNING, m);
    }

    public static ProgressState done(String m) {
        return new ProgressState(ProgressMode.DONE, m);
    }

    public static ProgressState failed(String m) {
        return new ProgressState(ProgressMode.FAILED, m);
    }
}