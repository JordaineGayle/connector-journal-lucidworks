package com.jordaine.connector.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jordaine.connector.model.ConnectorDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * File-backed {@link DocumentSink} that stores documents as JSON Lines.
 *
 * <p>The sink keeps file-level writes idempotent by loading existing documents keyed by their
 * stable connector {@code id}, merging the new batch on top, and rewriting the file atomically.
 * This preserves the latest representation of each logical document without appending duplicates
 * across retries, resumes, or polling runs.
 */
public class JsonlDocumentWriter implements DocumentSink<ConnectorDocument> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonlDocumentWriter.class);
    private final ObjectMapper objectMapper;

    public JsonlDocumentWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void beginRun(Path outputFile, int skip) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // A run from skip=0 represents a full rebuild, so stale rows must be removed first.
        if (skip == 0) {
            Files.deleteIfExists(outputFile);
            return;
        }

        // Resuming without the existing snapshot would silently produce an incomplete file.
        if (!Files.exists(outputFile)) {
            throw new IllegalStateException(
                    "Checkpoint skip=" + skip + " but output file does not exist: " + outputFile
            );
        }
    }

    @Override
    public void writeDocuments(Path outputFile, List<ConnectorDocument> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // LinkedHashMap preserves file order for existing rows while letting later documents
        // replace earlier ones with the same logical id.
        LinkedHashMap<String, ConnectorDocument> documentsById = loadExistingDocuments(outputFile);
        Set<String> existingDocumentIds = new LinkedHashSet<>(documentsById.keySet());
        Set<String> replacedDocumentIds = new LinkedHashSet<>();
        Set<String> newDocumentIds = new LinkedHashSet<>();
        for (ConnectorDocument document : documents) {
            String documentId = requireDocumentId(document);
            if (existingDocumentIds.contains(documentId)) {
                replacedDocumentIds.add(documentId);
            } else {
                newDocumentIds.add(documentId);
            }
            documentsById.put(documentId, document);
        }

        writeAtomically(outputFile, documentsById.values());

        if (!replacedDocumentIds.isEmpty()) {
            LOGGER.info(
                    "Output {} already contained {} incoming document(s). Replaced {} existing document(s) and added {} new document(s) in this batch.",
                    outputFile,
                    replacedDocumentIds.size(),
                    replacedDocumentIds.size(),
                    newDocumentIds.size()
            );
        }
    }

    /**
     * Reads the existing JSONL snapshot into memory so incoming documents can be upserted by id.
     */
    private LinkedHashMap<String, ConnectorDocument> loadExistingDocuments(Path outputFile) throws IOException {
        LinkedHashMap<String, ConnectorDocument> documentsById = new LinkedHashMap<>();
        if (!Files.exists(outputFile)) {
            return documentsById;
        }

        try (BufferedReader reader = Files.newBufferedReader(outputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                ConnectorDocument document = objectMapper.readValue(line, ConnectorDocument.class);
                documentsById.put(requireDocumentId(document), document);
            }
        }

        return documentsById;
    }

    /**
     * Rewrites the entire JSONL file through a temporary file so readers never observe a partially
     * updated snapshot.
     */
    private void writeAtomically(Path outputFile, Collection<ConnectorDocument> documents) throws IOException {
        Path tempFile = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (ConnectorDocument document : documents) {
                writer.write(objectMapper.writeValueAsString(document));
                writer.newLine();
            }
        }

        try {
            Files.move(
                    tempFile,
                    outputFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ex) {
            LOGGER.debug("Atomic move not supported for JSONL output, falling back to non-atomic move.");
            Files.move(tempFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Ensures every stored document has the stable identifier required for idempotent upserts.
     */
    private String requireDocumentId(ConnectorDocument document) throws IOException {
        if (document == null || document.getId() == null || document.getId().isBlank()) {
            throw new IOException("Connector document id must not be blank");
        }

        return document.getId();
    }
}