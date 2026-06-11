package com.oraculum.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public final class FileCleanupUtil {

    private FileCleanupUtil() {
        // Utility class
    }

    private static boolean isValidDirectory(Path folderPath) {
        if (folderPath == null || !Files.exists(folderPath)) {
            log.warn("Folder path is null or does not exist: {}", folderPath);
            return false;
        }
        if (!Files.isDirectory(folderPath)) {
            log.warn("Path is not a directory: {}", folderPath);
            return false;
        }
        return true;
    }

    private static boolean isOlderThan(Path path, Instant cutoffInstant) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoffInstant);
        } catch (IOException e) {
            log.error("Could not read last modified time for file: {}", path, e);
            return false;
        }
    }

    private static List<Path> getFilesToDelete(Path folderPath, Instant cutoffInstant) {
        try (Stream<Path> stream = Files.list(folderPath)) {
            return stream.filter(path -> isOlderThan(path, cutoffInstant)).toList();
        } catch (IOException e) {
            log.error("Failed to list files in directory: {}", folderPath, e);
            return List.of();
        }
    }

    private static boolean deleteFile(Path file) {
        try {
            Files.delete(file);
            log.debug("Deleted file: {}", file);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", file, e);
            return false;
        }
    }

    private static int deleteFiles(List<Path> files) {
        int deleted = 0;
        for (Path file : files) {
            if (deleteFile(file)) {
                deleted++;
            }
        }
        return deleted;
    }

    /**
     * Deletes all files directly inside the specified folder that have a last modified time
     * older than the specified cutoff instant. Subfolders are ignored.
     *
     * @param folderPath    The path of the folder to clean up.
     * @param cutoffInstant The cutoff timestamp. Files modified before this instant will be deleted.
     */
    public static void deleteFilesOlderThan(Path folderPath, Instant cutoffInstant) {
        if (!isValidDirectory(folderPath)) {
            return;
        }
        log.info("Starting file cleanup in: {} (modified before {})", folderPath, cutoffInstant);
        List<Path> filesToDelete = getFilesToDelete(folderPath, cutoffInstant);
        int deleted = deleteFiles(filesToDelete);
        int failed = filesToDelete.size() - deleted;
        log.info("Finished file cleanup in: {}. Deleted: {}, Failed: {}", folderPath, deleted, failed);
    }
}
