package dev.simeonya.service;

import dev.simeonya.model.MappingResult;
import dev.simeonya.util.Constants;
import dev.simeonya.util.FileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FiveMStructureService {

    public MappingResult mapToFiveMStructure(Path extractedRoot, Path resourceRoot, String vehicleName)
            throws IOException {

        Path streamDir = resourceRoot.resolve("stream").resolve(vehicleName);
        Path dataDir = resourceRoot.resolve("data").resolve(vehicleName);

        Files.createDirectories(streamDir);
        Files.createDirectories(dataDir);

        List<Path> allFiles = Files.walk(extractedRoot)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        int streamCount = 0;
        int dataCount = 0;

        for (Path f : allFiles) {
            String fileName = f.getFileName().toString();
            String lower = fileName.toLowerCase(Locale.ROOT);

            if (Constants.META_NAMES.contains(lower)) {
                Files.copy(f, dataDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                dataCount++;
                continue;
            }

            String ext = FileUtil.getExtension(lower);
            if (ext != null && Constants.STREAM_EXTENSIONS.contains(ext)) {
                Files.copy(f, streamDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                streamCount++;
            }
        }

        return new MappingResult(streamCount, dataCount);
    }

    public void writeManifest(Path resourceRoot) throws IOException {
        Files.writeString(
                resourceRoot.resolve("fxmanifest.lua"),
                Constants.MANIFEST_CONTENT,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}