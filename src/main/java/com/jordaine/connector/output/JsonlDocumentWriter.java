package com.jordaine.connector.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jordaine.connector.model.ConnectorDocument;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class JsonlDocumentWriter {
    private final ObjectMapper objectMapper;

    public JsonlDocumentWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void writeDocuments(Path outputFile, List<ConnectorDocument> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            for (ConnectorDocument document : documents) {
                writer.write(objectMapper.writeValueAsString(document));
                writer.newLine();
            }
        }
    }
}