package dev.simeonya.service;

import dev.simeonya.model.ExportConfig;
import dev.simeonya.model.ImportItem;
import dev.simeonya.model.MappingResult;
import dev.simeonya.util.DateTimeUtil;
import dev.simeonya.util.FileUtil;
import dev.simeonya.util.StringUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ExportService {

    private final RpfExtractorService rpfExtractorService;
    private final FiveMStructureService fiveMStructureService;
    private final ZipService zipService;

    public ExportService() {
        this.rpfExtractorService = new RpfExtractorService();
        this.fiveMStructureService = new FiveMStructureService();
        this.zipService = new ZipService();
    }

    public void export(Path outRoot, List<ImportItem> items, ExportConfig config,
                       Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater) throws Exception {

        if (config.isSingleResource()) {
            exportSingleResource(outRoot, items, config, logger, progressUpdater);
        } else {
            exportMultiResource(outRoot, items, config, logger, progressUpdater);
        }
    }

    private void exportSingleResource(Path outRoot, List<ImportItem> items, ExportConfig config,
                                      Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater)
            throws Exception {

        String resourceName = StringUtil.sanitize(config.getSingleResourceName());
        Path resourceRoot = outRoot.resolve(resourceName);

        FileUtil.deleteIfExists(resourceRoot);
        Files.createDirectories(resourceRoot);
        fiveMStructureService.writeManifest(resourceRoot);

        int total = items.size();
        AtomicInteger idx = new AtomicInteger(0);

        for (ImportItem item : items) {
            int i = idx.incrementAndGet();
            processItem(item, resourceRoot, config, logger);
            progressUpdater.accept(i, total);
        }

        if (config.isExportAsZip()) {
            String zipName = resourceName + "_" + DateTimeUtil.timestamp() + ".zip";
            Path zipPath = outRoot.resolve(zipName);
            zipService.zipFolder(resourceRoot, zipPath);
            logger.accept("ZIP created: " + zipPath);
        } else {
            logger.accept("Exported folder: " + resourceRoot);
        }
    }

    private void exportMultiResource(Path outRoot, List<ImportItem> items, ExportConfig config,
                                     Consumer<String> logger, BiConsumer<Integer, Integer> progressUpdater)
            throws Exception {

        int total = items.size();
        AtomicInteger idx = new AtomicInteger(0);

        for (ImportItem item : items) {
            int i = idx.incrementAndGet();

            String resourceName = StringUtil.sanitize(item.getResourceName());
            Path resourceRoot = outRoot.resolve(resourceName);

            FileUtil.deleteIfExists(resourceRoot);
            Files.createDirectories(resourceRoot);
            fiveMStructureService.writeManifest(resourceRoot);

            processItem(item, resourceRoot, config, logger);

            if (config.isExportAsZip()) {
                String zipName = resourceName + "_" + DateTimeUtil.timestamp() + ".zip";
                Path zipPath = outRoot.resolve(zipName);
                zipService.zipFolder(resourceRoot, zipPath);
                logger.accept("ZIP created: " + zipPath);
            } else {
                logger.accept("Exported folder: " + resourceRoot);
            }

            progressUpdater.accept(i, total);
        }
    }

    private void processItem(ImportItem item, Path resourceRoot, ExportConfig config,
                             Consumer<String> logger) throws Exception {

        String vehicleName = StringUtil.sanitize(item.getVehicleName());
        Path extractedRoot;
        boolean temp = false;

        if (item.isDirectory()) {
            extractedRoot = item.path();
        } else if (item.isRpf()) {
            extractedRoot = rpfExtractorService.extractRpf(item.path(), config.getExtractorPath());
            rpfExtractorService.extractNestedRpfs(extractedRoot, config.getExtractorPath());
            temp = true;
        } else {
            throw new IllegalStateException("Unsupported input: " + item.path());
        }

        MappingResult result = fiveMStructureService.mapToFiveMStructure(
                extractedRoot, resourceRoot, vehicleName
        );

        logger.accept("Mapped [" + vehicleName + "] stream=" + result.streamCount() +
                ", data=" + result.dataCount());

        if (temp && config.isCleanupTemp()) {
            FileUtil.deleteIfExists(extractedRoot);
        }
    }
}