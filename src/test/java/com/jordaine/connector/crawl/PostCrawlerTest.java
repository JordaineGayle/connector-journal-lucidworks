package com.jordaine.connector.crawl;

import com.jordaine.connector.client.DummyJsonClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.model.DummyJsonPost;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.output.JsonlDocumentWriter;
import com.jordaine.connector.transform.PostDocumentMapper;
import com.jordaine.connector.util.RateLimiter;
import com.jordaine.connector.util.RetryExecutor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostCrawlerTest {

    @Test
    void shouldProcessAndAdvanceCheckpoint() throws Exception {

        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                1,
                3,
                Duration.ofMillis(10),
                100,
                Path.of("data/test-checkpoint.json"),
                Path.of("output/test.jsonl")
        );

        CrawlCheckpointStore checkpointStore = new CrawlCheckpointStore() {
            int skip = 0;

            @Override
            public int loadSkip() {
                return skip;
            }

            @Override
            public void saveSkip(int newSkip) {
                skip = newSkip;
            }
        };

        DummyJsonClient mockClient = new DummyJsonClient(
                "https://dummyjson.com",
                "/posts",
                new RateLimiter(100)
        ) {
            int callCount = 0;

            @Override
            public PostsResponse fetchPosts(int limit, int skip) {
                if (callCount++ > 0) {
                    PostsResponse empty = new PostsResponse();
                    empty.setPosts(List.of());
                    empty.setTotal(1);
                    return empty;
                }

                DummyJsonPost post = new DummyJsonPost();
                post.setId(1);
                post.setTitle("Test");
                post.setBody("Body");
                post.setViews(10);
                post.setUserId(1);

                PostsResponse response = new PostsResponse();
                response.setPosts(List.of(post));
                response.setTotal(1);
                return response;
            }
        };

        PostCrawler crawler = new PostCrawler(
                mockClient,
                new PostDocumentMapper(),
                new JsonlDocumentWriter(),
                new RetryExecutor(),
                config,
                checkpointStore
        );

        crawler.crawl();

        assertEquals(1, checkpointStore.loadSkip());
    }
}