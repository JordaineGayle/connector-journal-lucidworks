package com.jordaine.connector.crawl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class FileCheckpointStore implements CrawlCheckpointStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCheckpointStore.class);

    private final Path checkpointFile;
    private final ObjectMapper objectMapper;

    public FileCheckpointStore(Path checkpointFile) {
        this.checkpointFile = checkpointFile;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public int loadSkip() {
        try {
            if (!Files.exists(checkpointFile)) {
                return 0;
            }

            Map<?, ?> data = objectMapper.readValue(Files.readAllBytes(checkpointFile), Map.class);
            Object skipValue = data.get("skip");

            if (skipValue == null) {
                return 0;
            }

            if (skipValue instanceof Number number) {
                int skip = number.intValue();
                if (skip < 0) {
                    throw new IllegalStateException("Checkpoint skip must be >= 0");
                }
                return skip;
            }

            throw new IllegalStateException("Checkpoint skip is not numeric");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load checkpoint file " + checkpointFile, ex);
        }
    }

    @Override
    public void saveSkip(int skip) {
        if (skip < 0) {
            throw new IllegalArgumentException("skip must be >= 0");
        }

        try {
            Path parent = checkpointFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = checkpointFile.resolveSibling(checkpointFile.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                objectMapper.writeValue(writer, Map.of("skip", skip));
            }

            try {
                Files.move(
                        tempFile,
                        checkpointFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ex) {
                LOGGER.debug("Atomic move not supported for checkpoint file, falling back to non-atomic move.");
                Files.move(tempFile, checkpointFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save checkpoint", ex);
        }
    }
}