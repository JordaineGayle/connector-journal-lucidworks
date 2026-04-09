package com.jordaine.connector.crawl;

import com.jordaine.connector.client.DummyJsonClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.output.JsonlDocumentWriter;
import com.jordaine.connector.transform.PostDocumentMapper;
import com.jordaine.connector.util.RetryExecutor;

import java.util.ArrayList;
import java.util.List;

public class PostCrawler {
    private final DummyJsonClient client;
    private final PostDocumentMapper mapper;
    private final JsonlDocumentWriter writer;
    private final RetryExecutor retryExecutor;
    private final ConnectorConfig config;
    private final CrawlCheckpointStore checkpointStore;

    public PostCrawler(
            DummyJsonClient client,
            PostDocumentMapper mapper,
            JsonlDocumentWriter writer,
            RetryExecutor retryExecutor,
            ConnectorConfig config,
            CrawlCheckpointStore checkpointStore
    ) {
        this.client = client;
        this.mapper = mapper;
        this.writer = writer;
        this.retryExecutor = retryExecutor;
        this.config = config;
        this.checkpointStore = checkpointStore;
    }

    public void crawl() throws Exception {
        int skip = checkpointStore.loadSkip();
        int limit = config.getPageSize();
        int processedCount = 0;

        System.out.println("Starting crawl from checkpoint skip=" + skip);

        while (true) {
            int currentSkip = skip;

            PostsResponse response = retryExecutor.execute(
                    () -> client.fetchPosts(limit, currentSkip),
                    config.getMaxRetries(),
                    config.getInitialBackoff()
            );

            List<DummyJsonPost> posts = response.getPosts();
            if (posts == null || posts.isEmpty()) {
                break;
            }

            List<ConnectorDocument> documents = new ArrayList<>();
            for (DummyJsonPost post : posts) {
                documents.add(mapper.map(post, config.getBaseUrl()));
            }

            writer.writeDocuments(config.getOutputFile(), documents);

            processedCount += documents.size();

            int nextSkip = skip + posts.size();
            checkpointStore.saveSkip(nextSkip);
            skip = nextSkip;

            System.out.println("Processed " + processedCount + " posts so far. Current skip=" + skip);

            if (skip >= response.getTotal()) {
                break;
            }
        }

        System.out.println("Crawl completed. Total posts written in this run: " + processedCount);
    }
}