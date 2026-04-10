package com.jordaine.connector.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jordaine.connector.model.ConnectorDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests idempotent JSONL upserts and validation rules in {@link JsonlDocumentWriter}.
 */
class JsonlDocumentWriterTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @TempDir
    Path tempDir;

    @Test
    void shouldUpsertDocumentsByIdAndRewriteAtomically() throws Exception {
        Path outputFile = tempDir.resolve("posts.jsonl");
        JsonlDocumentWriter writer = new JsonlDocumentWriter();

        writer.beginRun(outputFile, 0);
        writer.writeDocuments(
                outputFile,
                List.of(
                        createDocument("post-1", "Original title", Instant.parse("2026-04-10T00:00:00Z")),
                        createDocument("post-2", "Second title", Instant.parse("2026-04-10T00:01:00Z"))
                )
        );

        writer.beginRun(outputFile, 2);
        writer.writeDocuments(
                outputFile,
                List.of(
                        createDocument("post-1", "Updated title", Instant.parse("2026-04-10T00:02:00Z")),
                        createDocument("post-3", "Third title", Instant.parse("2026-04-10T00:03:00Z"))
                )
        );

        Map<String, ConnectorDocument> documentsById = readDocumentsById(outputFile);
        assertEquals(3, documentsById.size());
        assertEquals("Updated title", documentsById.get("post-1").getTitle());
        assertEquals(Instant.parse("2026-04-10T00:02:00Z"), documentsById.get("post-1").getFetchedAt());
        assertEquals("Second title", documentsById.get("post-2").getTitle());
        assertEquals("Third title", documentsById.get("post-3").getTitle());

        Path tempFile = outputFile.resolveSibling(outputFile.getFileName() + ".tmp");
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void shouldRejectDocumentsWithoutId() throws Exception {
        Path outputFile = tempDir.resolve("posts.jsonl");
        JsonlDocumentWriter writer = new JsonlDocumentWriter();

        writer.beginRun(outputFile, 0);

        ConnectorDocument document = createDocument("post-1", "Title", Instant.parse("2026-04-10T00:00:00Z"));
        document.setId(" ");

        assertThrows(IOException.class, () -> writer.writeDocuments(outputFile, List.of(document)));
        assertFalse(Files.exists(outputFile));
    }

    private Map<String, ConnectorDocument> readDocumentsById(Path outputFile) throws IOException {
        Map<String, ConnectorDocument> documentsById = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(outputFile);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            ConnectorDocument document = objectMapper.readValue(line, ConnectorDocument.class);
            documentsById.put(document.getId(), document);
        }
        return documentsById;
    }

    private ConnectorDocument createDocument(String id, String title, Instant fetchedAt) {
        ConnectorDocument document = new ConnectorDocument();
        document.setId(id);
        document.setSourceType("post");
        document.setSourceId(1);
        document.setTitle(title);
        document.setBody("Body for " + title);
        document.setTags(new ArrayList<>());
        document.setAuthorId(7);
        document.setReactionCount(3);
        document.setViewCount(11);
        document.setSearchText(title);
        document.setSourceUrl("https://dummyjson.com/posts/1");
        document.setFetchedAt(fetchedAt);
        return document;
    }
}
