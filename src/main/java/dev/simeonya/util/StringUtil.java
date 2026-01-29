package dev.simeonya.util;

import java.util.Locale;

public final class StringUtil {

    private StringUtil() {
    }

    public static String sanitize(String s) {
        if (s == null) {
            return "vehicle";
        }

        String out = s.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]", "_");
        out = out.replaceAll("_+", "_");

        return out.isBlank() ? "vehicle" : out;
    }

    public static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }
}