package com.jordaine.connector.transform;

/**
 * Generic contract for transforming source records into connector documents.
 */
public interface DocumentMapper<S, D> {
    /**
     * Maps one source object into its output representation.
     */
    D map(S source);
}
