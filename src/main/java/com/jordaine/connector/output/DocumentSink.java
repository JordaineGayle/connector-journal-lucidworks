package com.jordaine.connector.output;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DocumentSink<D> {
    void writeDocuments(Path outputFile, List<D> documents) throws IOException;
}
