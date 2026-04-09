package com.jordaine.connector.crawl;

public interface CrawlCheckpointStore {
    int loadSkip();

    void saveSkip(int skip);
}