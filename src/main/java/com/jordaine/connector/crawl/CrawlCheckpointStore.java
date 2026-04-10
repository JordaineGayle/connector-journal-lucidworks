package com.jordaine.connector.crawl;

/**
 * Persists crawl progress between connector runs.
 */
public interface CrawlCheckpointStore {
    /**
     * Loads the next skip offset that should be requested from the source API.
     */
    int loadSkip();

    /**
     * Persists the next skip offset after a page has been written successfully.
     */
    void saveSkip(int skip);
}