package com.jordaine.connector.transform;

public interface DocumentMapper<S, D> {
    D map(S source);
}
