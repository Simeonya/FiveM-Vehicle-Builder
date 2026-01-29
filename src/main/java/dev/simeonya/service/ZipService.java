package dev.simeonya.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipService {

    public void zipFolder(Path folder, Path outZip) throws IOException {
        Files.createDirectories(outZip.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outZip))) {
            Path base = folder.toAbsolutePath();

            Files.walk(folder)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String rel = base.relativize(p.toAbsolutePath())
                                .toString()
                                .replace("\\", "/");
                        try {
                            zos.putNextEntry(new ZipEntry(rel));
                            zos.write(Files.readAllBytes(p));
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}