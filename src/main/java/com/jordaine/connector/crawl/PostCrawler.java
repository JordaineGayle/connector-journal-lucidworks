package com.jordaine.connector.crawl;

import com.jordaine.connector.client.PostsClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.output.DocumentSink;
import com.jordaine.connector.transform.DocumentMapper;
import com.jordaine.connector.util.RetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PostCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostCrawler.class);

    private final PostsClient client;
    private final DocumentMapper<DummyJsonPost, ConnectorDocument> mapper;
    private final DocumentSink<ConnectorDocument> writer;
    private final RetryExecutor retryExecutor;
    private final ConnectorConfig config;
    private final CrawlCheckpointStore checkpointStore;
    private final PostsPageValidator pageValidator;

    public PostCrawler(
            PostsClient client,
            DocumentMapper<DummyJsonPost, ConnectorDocument> mapper,
            DocumentSink<ConnectorDocument> writer,
            RetryExecutor retryExecutor,
            ConnectorConfig config,
            CrawlCheckpointStore checkpointStore,
            PostsPageValidator pageValidator
    ) {
        this.client = client;
        this.mapper = mapper;
        this.writer = writer;
        this.retryExecutor = retryExecutor;
        this.config = config;
        this.checkpointStore = checkpointStore;
        this.pageValidator = pageValidator;
    }

    public void crawl() throws Exception {
        int skip = checkpointStore.loadSkip();
        int limit = config.getPageSize();
        int processedCount = 0;

        LOGGER.info("Starting crawl from checkpoint skip={}", skip);

        while (true) {
            int currentSkip = skip;

            PostsResponse response = retryExecutor.execute(
                    () -> {
                        PostsResponse fetched = client.fetchPosts(limit, currentSkip);
                        pageValidator.validate(currentSkip, limit, fetched);
                        return fetched;
                    },
                    config.getMaxRetries(),
                    config.getInitialBackoff()
            );

            List<DummyJsonPost> posts = response.getPosts();
            if (posts.isEmpty()) {
                break;
            }

            List<ConnectorDocument> documents = new ArrayList<>();
            for (DummyJsonPost post : posts) {
                documents.add(mapper.map(post));
            }

            writer.writeDocuments(config.getOutputFile(), documents);

            processedCount += documents.size();

            int nextSkip = skip + posts.size();
            checkpointStore.saveSkip(nextSkip);
            skip = nextSkip;

            LOGGER.info("Processed {} posts so far. Current skip={}", processedCount, skip);

            if (skip >= response.getTotal()) {
                break;
            }
        }

        LOGGER.info("Crawl completed. Total posts written in this run: {}", processedCount);
    }
}