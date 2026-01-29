package dev.simeonya.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class RpfExtractorService {

    public static boolean isValidExtractorPath(String p) {
        if (p == null) {
            return false;
        }

        String s = p.trim();
        if (s.isEmpty()) {
            return false;
        }

        Path path;
        try {
            path = Paths.get(s);
        } catch (Exception e) {
            return false;
        }

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }

        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.equals("rpf-cli.exe") || name.endsWith("rpf-cli.exe");
    }

    public Path extractRpf(Path rpfFile, String extractorPath) throws Exception {
        Path extractor = Paths.get(extractorPath).toAbsolutePath();
        Path rpfDir = rpfFile.getParent().toAbsolutePath();
        Path tempRoot = Files.createTempDirectory("rpf_cli_root_");

        List<String> cmd = List.of(
                extractor.toString(),
                "extract",
                rpfFile.getFileName().toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(rpfDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = process.waitFor();

        if (code != 0) {
            throw new IllegalStateException("rpf-cli failed:\n" + output);
        }

        Path extracted = findExtractedDir(rpfDir, rpfFile.getFileName().toString());
        if (extracted == null) {
            throw new IllegalStateException("rpf-cli finished but no extracted content found");
        }

        Files.move(extracted, tempRoot.resolve(extracted.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        return tempRoot.resolve(extracted.getFileName());
    }

    public void extractNestedRpfs(Path root, String extractorPath) throws Exception {
        boolean extracted;
        do {
            extracted = false;
            List<Path> rpfs = findNestedRpfs(root);

            for (Path rpf : rpfs) {
                Path parent = rpf.getParent();
                String name = rpf.getFileName().toString().toLowerCase(Locale.ROOT);

                if (name.equals("dlc.rpf")) {
                    continue;
                }

                List<String> cmd = List.of(
                        extractorPath,
                        "extract",
                        rpf.getFileName().toString()
                );

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(parent.toFile());
                pb.redirectErrorStream(true);

                Process p = pb.start();
                p.getInputStream().readAllBytes();
                int code = p.waitFor();

                if (code == 0) {
                    Files.deleteIfExists(rpf);
                    extracted = true;
                }
            }
        } while (extracted);
    }

    private Path findExtractedDir(Path baseDir, String rpfName) throws IOException {
        String plain = rpfName.replace(".rpf", "");

        try (var s = Files.list(baseDir)) {
            for (Path p : s.toList()) {
                if (!Files.isDirectory(p)) {
                    continue;
                }

                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.equals("dlc") || n.equals(plain) || n.equals(rpfName.toLowerCase(Locale.ROOT))) {
                    return p;
                }
            }
        }

        return null;
    }

    private List<Path> findNestedRpfs(Path root) throws IOException {
        try (var s = Files.walk(root)) {
            return s.filter(p -> Files.isRegularFile(p) &&
                            p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".rpf"))
                    .toList();
        }
    }
}