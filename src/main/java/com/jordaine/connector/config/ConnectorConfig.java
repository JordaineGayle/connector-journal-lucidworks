package com.jordaine.connector.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class ConnectorConfig {
    private final String baseUrl;
    private final String postsEndpoint;
    private final int pageSize;
    private final int maxRetries;
    private final Duration initialBackoff;
    private final int requestsPerMinute;
    private final Path checkpointFile;
    private final Path outputFile;

    public ConnectorConfig(
            String baseUrl,
            String postsEndpoint,
            int pageSize,
            int maxRetries,
            Duration initialBackoff,
            int requestsPerMinute,
            Path checkpointFile,
            Path outputFile
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (postsEndpoint == null || postsEndpoint.isBlank()) {
            throw new IllegalArgumentException("postsEndpoint must not be blank");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be > 0");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (initialBackoff == null || initialBackoff.isNegative()) {
            throw new IllegalArgumentException("initialBackoff must be >= 0");
        }
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException("requestsPerMinute must be > 0");
        }
        if (checkpointFile == null) {
            throw new IllegalArgumentException("checkpointFile must not be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile must not be null");
        }

        this.baseUrl = baseUrl;
        this.postsEndpoint = postsEndpoint;
        this.pageSize = pageSize;
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
        this.requestsPerMinute = requestsPerMinute;
        this.checkpointFile = checkpointFile;
        this.outputFile = outputFile;
    }

    public static ConnectorConfig fromEnvironment() {
        String baseUrl = getEnv("API_BASE_URL", "https://dummyjson.com");
        String postsEndpoint = getEnv("POSTS_ENDPOINT", "/posts");
        int pageSize = getEnvInt("PAGE_SIZE", 20);
        int maxRetries = getEnvInt("MAX_RETRIES", 5);
        int requestsPerMinute = getEnvInt("REQUESTS_PER_MINUTE", 90);
        long initialBackoffMillis = getEnvLong("INITIAL_BACKOFF_MILLIS", 1000L);

        Path checkpointFile = Paths.get(getEnv("CHECKPOINT_FILE", "data/checkpoint.json"));
        Path outputFile = Paths.get(getEnv("OUTPUT_FILE", "output/posts.jsonl"));

        return new ConnectorConfig(
                baseUrl,
                postsEndpoint,
                pageSize,
                maxRetries,
                Duration.ofMillis(initialBackoffMillis),
                requestsPerMinute,
                checkpointFile,
                outputFile
        );
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Environment variable " + key + " must be an integer", ex);
        }
    }

    private static long getEnvLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Environment variable " + key + " must be a long", ex);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPostsEndpoint() {
        return postsEndpoint;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public Path getCheckpointFile() {
        return checkpointFile;
    }

    public Path getOutputFile() {
        return outputFile;
    }
}