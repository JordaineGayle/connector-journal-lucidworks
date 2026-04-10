package com.jordaine.connector;

import com.jordaine.connector.client.DummyJsonClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.crawl.CrawlCheckpointStore;
import com.jordaine.connector.crawl.FileCheckpointStore;
import com.jordaine.connector.crawl.PostCrawler;
import com.jordaine.connector.crawl.PostsPageValidator;
import com.jordaine.connector.output.JsonlDocumentWriter;
import com.jordaine.connector.transform.PostDocumentMapper;
import com.jordaine.connector.util.RateLimiter;
import com.jordaine.connector.util.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            ConnectorConfig config = ConnectorConfig.fromEnvironment();

            RateLimiter rateLimiter = new RateLimiter(config.getRequestsPerMinute());

            DummyJsonClient client = new DummyJsonClient(
                    config.getBaseUrl(),
                    config.getPostsEndpoint(),
                    rateLimiter
            );

            PostDocumentMapper mapper = new PostDocumentMapper(
                    config.getBaseUrl(),
                    config.getPostsEndpoint()
            );
            JsonlDocumentWriter writer = new JsonlDocumentWriter();
            RetryExecutor retryExecutor = new RetryExecutor();
            CrawlCheckpointStore checkpointStore = new FileCheckpointStore(
                    config.getCheckpointFile()
            );
            PostsPageValidator pageValidator = new PostsPageValidator();

            PostCrawler crawler = new PostCrawler(
                    client,
                    mapper,
                    writer,
                    retryExecutor,
                    config,
                    checkpointStore,
                    pageValidator
            );

            crawler.crawl();
        } catch (Exception ex) {
            LOGGER.error("Connector execution failed", ex);
            System.exit(1);
        }
    }
}