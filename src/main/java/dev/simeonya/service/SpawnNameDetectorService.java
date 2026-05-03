package dev.simeonya.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans an extracted vehicle folder and reads the spawn name from
 * vehicles.meta — specifically the <gameName> tag.
 * Falls back to the folder/file name when nothing is found.
 */
public class SpawnNameDetectorService {

    // Matches  <gameName>adder</gameName>  (case-insensitive, whitespace-tolerant)
    private static final Pattern GAME_NAME_PATTERN =
            Pattern.compile("<gameName>\\s*([A-Za-z0-9_]+)\\s*</gameName>", Pattern.CASE_INSENSITIVE);

    /**
     * Tries to find a spawn name for the item at {@code root}.
     * Searches recursively for vehicles.meta and reads <gameName>.
     * Returns an empty list when nothing useful is found.
     */
    public List<String> detectSpawnNames(Path root) {
        List<String> names = new ArrayList<>();
        try {
            List<Path> metaFiles = findVehiclesMeta(root);
            for (Path meta : metaFiles) {
                String content = Files.readString(meta, StandardCharsets.UTF_8);
                Matcher m = GAME_NAME_PATTERN.matcher(content);
                while (m.find()) {
                    String name = m.group(1).trim().toLowerCase(Locale.ROOT);
                    if (!name.isBlank() && !names.contains(name)) {
                        names.add(name);
                    }
                }
                if (!names.isEmpty()) {
                    break; // first meta file with data is enough
                }
            }
        } catch (IOException ignored) {
            // Silently fall back – caller handles empty result
        }
        return names;
    }

    private List<Path> findVehiclesMeta(Path root) throws IOException {
        if (Files.isRegularFile(root)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> Files.isRegularFile(p) &&
                            p.getFileName().toString().equalsIgnoreCase("vehicles.meta"))
                    .toList();
        }
    }
}
