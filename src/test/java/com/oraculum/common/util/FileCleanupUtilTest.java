package com.oraculum.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCleanupUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void testDeleteFilesOlderThan() throws IOException {
        // Arrange
        Instant now = Instant.now();
        Instant cutoff = now.minus(1, ChronoUnit.DAYS);

        // 1. Create a file that is older than the cutoff (e.g. 2 days old)
        Path oldFile = tempDir.resolve("old_file.parquet");
        Files.createFile(oldFile);
        Files.setLastModifiedTime(oldFile, FileTime.from(now.minus(2, ChronoUnit.DAYS)));

        // 2. Create a file that is newer than the cutoff (e.g. 2 hours old)
        Path newFile = tempDir.resolve("new_file.parquet");
        Files.createFile(newFile);
        Files.setLastModifiedTime(newFile, FileTime.from(now.minus(2, ChronoUnit.HOURS)));

        // 3. Create a subdirectory with an old modification time to ensure it is ignored
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);
        Files.setLastModifiedTime(subDir, FileTime.from(now.minus(2, ChronoUnit.DAYS)));

        // 4. Create a file inside that subdirectory to ensure we do not touch it
        Path fileInSubDir = subDir.resolve("file_in_subdir.parquet");
        Files.createFile(fileInSubDir);
        Files.setLastModifiedTime(fileInSubDir, FileTime.from(now.minus(2, ChronoUnit.DAYS)));

        // Act
        FileCleanupUtil.deleteFilesOlderThan(tempDir, cutoff);

        // Assert
        assertFalse(Files.exists(oldFile), "Old file should have been deleted");
        assertTrue(Files.exists(newFile), "New file should be kept");
        assertTrue(Files.exists(subDir), "Subdirectory should be ignored and kept");
        assertTrue(Files.exists(fileInSubDir), "File inside subdirectory should be kept");
    }
}
