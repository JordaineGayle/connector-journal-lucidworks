package com.jordaine.connector;

import com.jordaine.connector.client.DummyJsonClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.crawl.CrawlCheckpointStore;
import com.jordaine.connector.crawl.FileCheckpointStore;
import com.jordaine.connector.crawl.PostCrawler;
import com.jordaine.connector.output.JsonlDocumentWriter;
import com.jordaine.connector.transform.PostDocumentMapper;
import com.jordaine.connector.util.RateLimiter;
import com.jordaine.connector.util.RetryExecutor;

public class Main {
    public static void main(String[] args) {
        try {
            ConnectorConfig config = ConnectorConfig.fromEnvironment();

            RateLimiter rateLimiter = new RateLimiter(config.getRequestsPerMinute());

            DummyJsonClient client = new DummyJsonClient(
                    config.getBaseUrl(),
                    config.getPostsEndpoint(),
                    rateLimiter
            );

            PostDocumentMapper mapper = new PostDocumentMapper();
            JsonlDocumentWriter writer = new JsonlDocumentWriter();
            RetryExecutor retryExecutor = new RetryExecutor();
            CrawlCheckpointStore checkpointStore = new FileCheckpointStore(
                    config.getCheckpointFile()
            );

            PostCrawler crawler = new PostCrawler(
                    client,
                    mapper,
                    writer,
                    retryExecutor,
                    config,
                    checkpointStore
            );

            crawler.crawl();
        } catch (Exception ex) {
            System.err.println("Connector execution failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}