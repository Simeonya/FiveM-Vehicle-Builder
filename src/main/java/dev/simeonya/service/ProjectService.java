package dev.simeonya.service;

import dev.simeonya.model.ImportItem;
import dev.simeonya.util.Constants;
import dev.simeonya.util.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.Base64;

/**
 * Saves and loads .sfvb project files (V2).
 *
 * On SAVE:
 *   - Folder items  → files embedded directly as Base64
 *   - RPF items     → extracted to temp, embedded, temp deleted
 *   - gameName read from vehicles.meta AFTER extraction → stored in ITEM_BEGIN
 *
 * On LOAD:
 *   - Files are written to  <projectfile_stem>_files/<vehicleName>/
 *     right next to the .sfvb — persistent, no temp folder.
 *   - That folder is reused on subsequent loads (files only written if missing).
 *
 * Format:
 *   SFVB_PROJECT_V2
 *   single_resource=...
 *   resource_name=...
 *   export_as_zip=...
 *   cleanup_temp=...
 *   extractor_path=...
 *   ITEM_BEGIN|<vehicleName>|<resourceName>|<originalPath>
 *   FILE|<rel-path>|<base64>
 *   ITEM_END
 */
public class ProjectService {

    private static final String HEADER = "SFVB_PROJECT_V2";

    private final RpfExtractorService    rpfExtractor = new RpfExtractorService();
    private final SpawnNameDetectorService spawnDetector = new SpawnNameDetectorService();

    // ── Save ─────────────────────────────────────────────────────────────────

    public void save(Path projectFile,
                     List<ImportItem> items,
                     boolean singleResource,
                     String resourceName,
                     boolean exportAsZip,
                     boolean cleanupTemp,
                     String extractorPath,
                     Consumer<String> logger) throws IOException {

        try (BufferedWriter w = Files.newBufferedWriter(projectFile, StandardCharsets.UTF_8)) {
            w.write(HEADER); w.newLine();
            w.write("single_resource=" + singleResource); w.newLine();
            w.write("resource_name="   + escape(resourceName)); w.newLine();
            w.write("export_as_zip="   + exportAsZip); w.newLine();
            w.write("cleanup_temp="    + cleanupTemp); w.newLine();
            w.write("extractor_path="  + escape(extractorPath == null ? "" : extractorPath)); w.newLine();

            for (ImportItem item : items) {

                Path   embedRoot    = null;
                boolean needsDelete = false;
                String  detectedName = item.getVehicleName();

                if (item.isFromProject()) {
                    // Already extracted — use stored path directly
                    embedRoot = item.path();
                } else if (item.isDirectory()) {
                    embedRoot = item.path();
                    // Try to detect gameName from vehicles.meta in the folder
                    List<String> names = spawnDetector.detectSpawnNames(embedRoot);
                    if (!names.isEmpty()) detectedName = names.get(0);
                } else if (item.isRpf()) {
                    if (extractorPath != null && !extractorPath.isBlank()) {
                        try {
                            logger.accept("Extracting " + item.path().getFileName() + " for project save...");
                            embedRoot = rpfExtractor.extractRpf(item.path(), extractorPath);
                            rpfExtractor.extractNestedRpfs(embedRoot, extractorPath);
                            needsDelete = true;
                            // Detect gameName NOW — vehicles.meta is readable after extraction
                            List<String> names = spawnDetector.detectSpawnNames(embedRoot);
                            if (!names.isEmpty()) {
                                detectedName = names.get(0);
                                logger.accept("Detected spawn name: " + detectedName);
                            }
                        } catch (Exception ex) {
                            logger.accept("WARN: Could not extract " + item.path().getFileName()
                                    + " — " + ex.getMessage());
                        }
                    } else {
                        logger.accept("WARN: No extractor path — "
                                + item.path().getFileName() + " not embedded.");
                    }
                }

                w.write("ITEM_BEGIN|" + escape(detectedName)
                        + "|" + escape(item.getResourceName())
                        + "|" + escape(item.path().toString()));
                w.newLine();

                if (embedRoot != null) {
                    embedDirectory(w, embedRoot, embedRoot, logger);
                    if (needsDelete) FileUtil.deleteIfExists(embedRoot);
                }

                w.write("ITEM_END"); w.newLine();
            }
        }
    }

    private void embedDirectory(BufferedWriter w, Path root, Path base,
                                Consumer<String> logger) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            List<Path> files = s.filter(Files::isRegularFile).sorted().toList();
            int count = 0;
            for (Path f : files) {
                String lower  = f.getFileName().toString().toLowerCase(Locale.ROOT);
                String ext    = FileUtil.getExtension(lower);
                boolean stream = ext != null && Constants.STREAM_EXTENSIONS.contains(ext);
                boolean meta   = Constants.META_NAMES.contains(lower);
                if (!stream && !meta) continue;

                String rel  = base.relativize(f).toString().replace('\\', '/');
                byte[] data = Files.readAllBytes(f);
                w.write("FILE|" + escape(rel) + "|" + Base64.getEncoder().encodeToString(data));
                w.newLine();
                count++;
            }
            if (count > 0) logger.accept("Embedded " + count + " file(s) for " + base.getFileName());
            else            logger.accept("WARN: No stream/meta files found in " + base.getFileName());
        }
    }

    // ── Load ─────────────────────────────────────────────────────────────────

    /**
     * Loads a project. Embedded files are written to a persistent folder
     * next to the .sfvb file:  <stem>_files/<vehicleName>/
     */
    public ProjectData load(Path projectFile) throws IOException {
        List<String> lines = Files.readAllLines(projectFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IOException("Empty file.");

        String first = lines.get(0).trim();
        if ("SFVB_PROJECT_V1".equals(first)) return loadV1(lines);
        if (!HEADER.equals(first))           throw new IOException("Not a valid .sfvb project file.");

        // Persistent extract folder: MyProject_files/ next to MyProject.sfvb
        String stem      = projectFile.getFileName().toString().replaceAll("(?i)\\.sfvb$", "");
        Path   filesRoot = projectFile.resolveSibling(stem + "_files");
        Files.createDirectories(filesRoot);

        boolean singleResource = true;
        String  resourceName   = "vehicles_pack";
        boolean exportAsZip    = false;
        boolean cleanupTemp    = true;
        String  extractorPath  = "";
        List<ImportItem> items = new ArrayList<>();

        String curVehicle  = null;
        String curResource = null;
        Path   curItemDir  = null;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;

            if      (line.startsWith("single_resource=")) singleResource = Boolean.parseBoolean(value(line));
            else if (line.startsWith("resource_name="))   resourceName   = unescape(value(line));
            else if (line.startsWith("export_as_zip="))   exportAsZip    = Boolean.parseBoolean(value(line));
            else if (line.startsWith("cleanup_temp="))    cleanupTemp    = Boolean.parseBoolean(value(line));
            else if (line.startsWith("extractor_path="))  extractorPath  = unescape(value(line));

            else if (line.startsWith("ITEM_BEGIN|")) {
                String[] p  = line.split("\\|", -1);
                curVehicle  = unescape(p.length > 1 ? p[1] : "vehicle");
                curResource = unescape(p.length > 2 ? p[2] : curVehicle);
                curItemDir  = filesRoot.resolve(curVehicle);
                Files.createDirectories(curItemDir);

            } else if (line.startsWith("FILE|") && curItemDir != null) {
                String[] p = line.split("\\|", 3);
                if (p.length < 3) continue;
                String rel   = unescape(p[1]);
                byte[] bytes = Base64.getDecoder().decode(p[2].trim());
                Path   dest  = curItemDir.resolve(rel.replace('/', File.separatorChar));
                Files.createDirectories(dest.getParent());
                // Only write if file doesn't exist yet (avoid re-writing large YTDs)
                if (!Files.exists(dest)) {
                    Files.write(dest, bytes, StandardOpenOption.CREATE);
                }

            } else if (line.startsWith("ITEM_END") && curItemDir != null) {
                items.add(new ImportItem(curItemDir, curVehicle, curResource));
                curVehicle = null; curResource = null; curItemDir = null;
            }
        }

        return new ProjectData(items, singleResource, resourceName,
                exportAsZip, cleanupTemp, extractorPath);
    }

    // ── V1 backwards compat ──────────────────────────────────────────────────

    private ProjectData loadV1(List<String> lines) {
        boolean singleResource = true;
        String  resourceName   = "vehicles_pack";
        boolean exportAsZip    = false;
        boolean cleanupTemp    = true;
        String  extractorPath  = "";
        List<ImportItem> items = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isBlank()) continue;
            if      (line.startsWith("single_resource=")) singleResource = Boolean.parseBoolean(value(line));
            else if (line.startsWith("resource_name="))   resourceName   = unescape(value(line));
            else if (line.startsWith("export_as_zip="))   exportAsZip    = Boolean.parseBoolean(value(line));
            else if (line.startsWith("cleanup_temp="))    cleanupTemp    = Boolean.parseBoolean(value(line));
            else if (line.startsWith("extractor_path="))  extractorPath  = unescape(value(line));
            else if (line.startsWith("ITEM|")) {
                String[] p = line.split("\\|", -1);
                if (p.length >= 4)
                    items.add(new ImportItem(Paths.get(unescape(p[1])), unescape(p[2]), unescape(p[3])));
            }
        }
        return new ProjectData(items, singleResource, resourceName, exportAsZip, cleanupTemp, extractorPath);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String value(String line) {
        int eq = line.indexOf('=');
        return eq < 0 ? "" : line.substring(eq + 1);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n").replace("\r", "");
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        boolean bs = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (bs) {
                switch (c) {
                    case '|'  -> out.append('|');
                    case 'n'  -> out.append('\n');
                    case '\\' -> out.append('\\');
                    default   -> { out.append('\\'); out.append(c); }
                }
                bs = false;
            } else if (c == '\\') {
                bs = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // ── Data container ────────────────────────────────────────────────────────

    public record ProjectData(
            List<ImportItem> items,
            boolean singleResource,
            String  resourceName,
            boolean exportAsZip,
            boolean cleanupTemp,
            String  extractorPath
    ) {}
}
