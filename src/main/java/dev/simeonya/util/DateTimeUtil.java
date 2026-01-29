package dev.simeonya.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private DateTimeUtil() {
    }

    public static String currentTime() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    public static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
}