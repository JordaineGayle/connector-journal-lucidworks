package com.jordaine.connector.crawl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests persistence, validation, and temp-file behavior in {@link FileCheckpointStore}.
 */
class FileCheckpointStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnZeroWhenCheckpointFileDoesNotExist() {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        assertEquals(0, store.loadSkip());
    }

    @Test
    void shouldPersistAndLoadCheckpointAndCleanupTempFile() {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        store.saveSkip(42);

        assertEquals(42, store.loadSkip());
        Path tempCheckpointPath = checkpointPath.resolveSibling(checkpointPath.getFileName() + ".tmp");
        assertFalse(Files.exists(tempCheckpointPath));
    }

    @Test
    void shouldThrowWhenCheckpointFileContainsInvalidJson() throws IOException {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        Files.writeString(checkpointPath, "not-valid-json");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        assertThrows(IllegalStateException.class, store::loadSkip);
    }

    @Test
    void shouldThrowWhenCheckpointSkipIsNonNumeric() throws IOException {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        Files.writeString(checkpointPath, "{\"skip\":\"abc\"}");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        assertThrows(IllegalStateException.class, store::loadSkip);
    }

    @Test
    void shouldThrowWhenCheckpointSkipIsNegative() throws IOException {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        Files.writeString(checkpointPath, "{\"skip\":-1}");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        assertThrows(IllegalStateException.class, store::loadSkip);
    }

    @Test
    void shouldRejectSavingNegativeSkip() {
        Path checkpointPath = tempDir.resolve("checkpoint.json");
        FileCheckpointStore store = new FileCheckpointStore(checkpointPath);

        assertThrows(IllegalArgumentException.class, () -> store.saveSkip(-1));
    }
}
