package com.jordaine.connector.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Persistence contract for connector documents.
 *
 * <p>The crawler calls {@link #beginRun(Path, int)} once at the start of each crawl so the sink can
 * align its backing storage with the current checkpoint state. Stateless sinks can ignore this
 * hook, while file-backed sinks can use it to enforce snapshot and resume safety rules.
 */
public interface DocumentSink<D> {
    /**
     * Prepares the sink for a crawl that will start reading from the provided checkpoint offset.
     *
     * <p>For example, a sink may treat {@code skip == 0} as a full rebuild and verify that resumed
     * crawls still have access to the previously written output.
     */
    default void beginRun(Path outputFile, int skip) throws IOException {
    }

    /**
     * Persists a batch of documents produced by the crawler.
     */
    void writeDocuments(Path outputFile, List<D> documents) throws IOException;
}
