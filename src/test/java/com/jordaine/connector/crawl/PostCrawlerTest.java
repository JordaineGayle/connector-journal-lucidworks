package com.jordaine.connector.crawl;

import com.jordaine.connector.client.PostsClient;
import com.jordaine.connector.config.ConnectorConfig;
import com.jordaine.connector.error.RetryableConnectorException;
import com.jordaine.connector.model.ConnectorDocument;
import com.jordaine.connector.model.DummyJsonPost;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.output.DocumentSink;
import com.jordaine.connector.output.JsonlDocumentWriter;
import com.jordaine.connector.transform.PostDocumentMapper;
import com.jordaine.connector.util.RetryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests checkpoint, resume, sink, and terminal-page behavior in {@link PostCrawler}.
 */
class PostCrawlerTest {
    @TempDir
    Path tempDir;

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

        PostsClient mockClient = new PostsClient() {
            int callCount = 0;

            @Override
            public PostsResponse fetchPosts(int limit, int skip) {
                if (callCount++ > 0) {
                    PostsResponse empty = new PostsResponse();
                    empty.setPosts(List.of());
                    empty.setTotal(1);
                    empty.setSkip(skip);
                    empty.setLimit(limit);
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
                response.setSkip(skip);
                response.setLimit(limit);
                return response;
            }
        };

        List<ConnectorDocument> writtenDocuments = new ArrayList<>();
        DocumentSink<ConnectorDocument> sink = (outputFile, documents) -> writtenDocuments.addAll(documents);

        PostCrawler crawler = new PostCrawler(
                mockClient,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                sink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        crawler.crawl();

        assertEquals(1, checkpointStore.loadSkip());
        assertEquals(1, writtenDocuments.size());
        assertEquals("post-1", writtenDocuments.get(0).getId());
    }

    @Test
    void shouldResetExistingOutputWhenStartingFreshCrawl() throws Exception {
        Path checkpointFile = tempDir.resolve("checkpoint.json");
        Path outputFile = tempDir.resolve("posts.jsonl");
        Files.writeString(outputFile, "stale-data\n");

        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
                Duration.ofMillis(10),
                100,
                checkpointFile,
                outputFile
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

        PostsClient client = (limit, skip) -> createPagedResponse(limit, skip, 1);

        PostCrawler crawler = new PostCrawler(
                client,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                new JsonlDocumentWriter(),
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        crawler.crawl();

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"id\":\"post-1\""));
        assertEquals(1, checkpointStore.loadSkip());
    }

    @Test
    void shouldCompleteCleanlyWhenCheckpointAlreadyMatchesTotal() {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
                Duration.ofMillis(10),
                100,
                Path.of("data/test-checkpoint.json"),
                Path.of("output/test.jsonl")
        );

        final int[] skipState = {251};
        final int[] saveCallCount = {0};
        final int[] fetchCallCount = {0};

        CrawlCheckpointStore checkpointStore = new CrawlCheckpointStore() {
            @Override
            public int loadSkip() {
                return skipState[0];
            }

            @Override
            public void saveSkip(int newSkip) {
                saveCallCount[0]++;
                skipState[0] = newSkip;
            }
        };

        PostsClient client = (limit, skip) -> {
            fetchCallCount[0]++;

            PostsResponse response = new PostsResponse();
            response.setPosts(List.of());
            response.setTotal(251);
            response.setSkip(skip);
            response.setLimit(0);
            return response;
        };

        List<ConnectorDocument> writtenDocuments = new ArrayList<>();
        DocumentSink<ConnectorDocument> sink = (outputFile, documents) -> writtenDocuments.addAll(documents);

        PostCrawler crawler = new PostCrawler(
                client,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                sink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        assertDoesNotThrow(crawler::crawl);
        assertEquals(251, checkpointStore.loadSkip());
        assertEquals(0, saveCallCount[0]);
        assertEquals(1, fetchCallCount[0]);
        assertTrue(writtenDocuments.isEmpty());
    }

    @Test
    void shouldFailWhenCheckpointExistsButOutputFileIsMissing() {
        Path checkpointFile = tempDir.resolve("checkpoint.json");
        Path outputFile = tempDir.resolve("posts.jsonl");

        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
                Duration.ofMillis(10),
                100,
                checkpointFile,
                outputFile
        );

        CrawlCheckpointStore checkpointStore = new CrawlCheckpointStore() {
            @Override
            public int loadSkip() {
                return 10;
            }

            @Override
            public void saveSkip(int newSkip) {
            }
        };

        PostCrawler crawler = new PostCrawler(
                (limit, skip) -> createPagedResponse(limit, skip, 20),
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                new JsonlDocumentWriter(),
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, crawler::crawl);
        assertTrue(exception.getMessage().contains("output file does not exist"));
    }

    @Test
    void shouldFailOnUnexpectedEmptyPageBeforeTotal() {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
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

        PostsClient mockClient = (limit, skip) -> {
            PostsResponse response = new PostsResponse();
            response.setPosts(List.of());
            response.setTotal(10);
            response.setSkip(skip);
            response.setLimit(limit);
            return response;
        };

        DocumentSink<ConnectorDocument> sink = (outputFile, documents) -> {
        };

        PostCrawler crawler = new PostCrawler(
                mockClient,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                sink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        assertThrows(RetryableConnectorException.class, crawler::crawl);
        assertEquals(0, checkpointStore.loadSkip());
    }

    @Test
    void shouldResumeAfterFailureWithoutMissingDocuments() throws Exception {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                2,
                0,
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

        Set<String> indexedDocumentIds = new HashSet<>();
        DocumentSink<ConnectorDocument> sink = (outputFile, documents) -> {
            for (ConnectorDocument document : documents) {
                indexedDocumentIds.add(document.getId());
            }
        };

        PostsClient failingClient = (limit, skip) -> {
            if (skip == 2) {
                throw new RetryableConnectorException("Simulated transient failure on second page");
            }
            return createPagedResponse(limit, skip, 5);
        };

        PostCrawler firstRunCrawler = new PostCrawler(
                failingClient,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                sink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        assertThrows(RetryableConnectorException.class, firstRunCrawler::crawl);
        assertEquals(2, checkpointStore.loadSkip());
        assertEquals(Set.of("post-1", "post-2"), indexedDocumentIds);

        PostsClient stableClient = (limit, skip) -> createPagedResponse(limit, skip, 5);
        PostCrawler secondRunCrawler = new PostCrawler(
                stableClient,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                sink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        secondRunCrawler.crawl();

        assertEquals(5, checkpointStore.loadSkip());
        assertEquals(Set.of("post-1", "post-2", "post-3", "post-4", "post-5"), indexedDocumentIds);
    }

    @Test
    void shouldNotAdvanceCheckpointWhenSinkFails() {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                2,
                0,
                Duration.ofMillis(10),
                100,
                Path.of("data/test-checkpoint.json"),
                Path.of("output/test.jsonl")
        );

        final int[] skipState = {0};
        final int[] saveCallCount = {0};

        CrawlCheckpointStore checkpointStore = new CrawlCheckpointStore() {
            @Override
            public int loadSkip() {
                return skipState[0];
            }

            @Override
            public void saveSkip(int skip) {
                saveCallCount[0]++;
                skipState[0] = skip;
            }
        };

        PostsClient client = (limit, skip) -> createPagedResponse(limit, skip, 2);
        DocumentSink<ConnectorDocument> failingSink = (outputFile, documents) -> {
            throw new IOException("Simulated sink failure");
        };

        PostCrawler crawler = new PostCrawler(
                client,
                new PostDocumentMapper("https://dummyjson.com", "/posts"),
                failingSink,
                new RetryExecutor(),
                config,
                checkpointStore,
                new PostsPageValidator()
        );

        assertThrows(IOException.class, crawler::crawl);
        assertEquals(0, checkpointStore.loadSkip());
        assertEquals(0, saveCallCount[0]);
    }

    private PostsResponse createPagedResponse(int limit, int skip, int total) {
        PostsResponse response = new PostsResponse();
        response.setSkip(skip);
        response.setLimit(limit);
        response.setTotal(total);

        if (skip >= total) {
            response.setPosts(List.of());
            return response;
        }

        int endExclusive = Math.min(skip + limit, total);
        List<DummyJsonPost> posts = new ArrayList<>();
        for (int id = skip + 1; id <= endExclusive; id++) {
            DummyJsonPost post = new DummyJsonPost();
            post.setId(id);
            post.setTitle("Title " + id);
            post.setBody("Body " + id);
            post.setViews(id * 10);
            post.setUserId(id);
            posts.add(post);
        }

        response.setPosts(posts);
        return response;
    }
}