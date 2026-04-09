package com.jordaine.connector.crawl;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileCheckpointStore implements CrawlCheckpointStore {
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

            if (skipValue instanceof Number number) {
                return number.intValue();
            }

            return 0;
        } catch (Exception ex) {
            System.err.println("Failed to load checkpoint, defaulting to 0: " + ex.getMessage());
            return 0;
        }
    }

    @Override
    public void saveSkip(int skip) {
        try {
            Path parent = checkpointFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            objectMapper.writeValue(checkpointFile.toFile(), Map.of("skip", skip));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save checkpoint", ex);
        }
    }
}