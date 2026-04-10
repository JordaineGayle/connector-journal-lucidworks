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

import java.time.Duration;

/**
 * Application entrypoint that wires the connector components together.
 *
 * <p>By default the process performs a single crawl and exits. When
 * {@code POLL_INTERVAL_SECONDS > 0}, the same crawler is executed repeatedly with a fixed sleep
 * between runs so the connector can check for newly appended data.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @FunctionalInterface
    interface CrawlOperation {
        /**
         * Performs one crawl pass.
         */
        void run() throws Exception;
    }

    @FunctionalInterface
    interface Sleeper {
        /**
         * Pauses between polling passes.
         */
        void sleep(Duration duration) throws InterruptedException;
    }

    /**
     * Builds the connector from environment configuration and runs it until completion or
     * interruption.
     */
    public static void main(String[] args) {
        try {
            ConnectorConfig config = ConnectorConfig.fromEnvironment();
            PostCrawler crawler = createCrawler(config);
            runCrawlerLoop(config, crawler::crawl, duration -> Thread.sleep(duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.info("Connector polling interrupted. Shutting down.");
        } catch (Exception ex) {
            LOGGER.error("Connector execution failed", ex);
            System.exit(1);
        }
    }

    /**
     * Creates the concrete crawler used by the production entrypoint.
     */
    private static PostCrawler createCrawler(ConnectorConfig config) {
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

        return new PostCrawler(
                client,
                mapper,
                writer,
                retryExecutor,
                config,
                checkpointStore,
                pageValidator
        );
    }

    /**
     * Executes the crawl once or repeatedly, depending on the configured polling interval.
     *
     * <p>The {@link Sleeper} indirection keeps polling behavior easy to test without real delays.
     */
    static void runCrawlerLoop(ConnectorConfig config, CrawlOperation crawlOperation, Sleeper sleeper) throws Exception {
        do {
            crawlOperation.run();

            if (config.getPollIntervalSeconds() <= 0) {
                return;
            }

            LOGGER.info("Polling enabled. Sleeping {} seconds before next crawl.", config.getPollIntervalSeconds());
            sleeper.sleep(Duration.ofSeconds(config.getPollIntervalSeconds()));
        } while (true);
    }
}