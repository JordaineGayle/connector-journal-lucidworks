package com.jordaine.connector.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jordaine.connector.error.NonRetryableConnectorException;
import com.jordaine.connector.error.RetryableConnectorException;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link PostsClient} implementation backed by the DummyJSON posts API.
 *
 * <p>The client applies request rate limiting before each call, classifies upstream failures into
 * retryable and non-retryable categories, and deserializes the JSON response into the connector's
 * page model.
 */
public class DummyJsonClient implements PostsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyJsonClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String postsEndpoint;
    private final RateLimiter rateLimiter;

    /**
     * Creates a client for the configured posts endpoint.
     */
    public DummyJsonClient(String baseUrl, String postsEndpoint, RateLimiter rateLimiter) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.baseUrl = stripTrailingSlash(baseUrl);
        this.postsEndpoint = normalizeEndpoint(postsEndpoint);
        this.rateLimiter = rateLimiter;
    }

    @Override
    public PostsResponse fetchPosts(int limit, int skip) throws Exception {
        rateLimiter.acquire();

        String url = baseUrl + postsEndpoint + "?limit=" + limit + "&skip=" + skip;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            // Network failures are transient in nature and should be retried by the caller.
            throw new RetryableConnectorException("Network failure while fetching posts", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }

        int statusCode = response.statusCode();

        if (statusCode == 429) {
            String retryAfterHeader = response.headers().firstValue("Retry-After").orElse(null);
            Duration retryAfter = parseRetryAfter(retryAfterHeader);

            if (retryAfter != null) {
                LOGGER.warn("429 received. Retry-After header {} ms.", retryAfter.toMillis());
            } else {
                LOGGER.warn("429 received. No parsable Retry-After header.");
            }

            throw new RetryableConnectorException("HTTP 429 Too Many Requests", retryAfter);
        }

        if (statusCode >= 500 && statusCode <= 599) {
            // Server-side failures may succeed on a later attempt.
            throw new RetryableConnectorException(
                    "Failed to fetch posts. HTTP " + statusCode + ". Body: " + response.body()
            );
        }

        if (statusCode < 200 || statusCode >= 300) {
            // Client-side validation and authorization errors should fail the run immediately.
            throw new NonRetryableConnectorException(
                    "Failed to fetch posts. HTTP " + statusCode + ". Body: " + response.body()
            );
        }

        try {
            return objectMapper.readValue(response.body(), PostsResponse.class);
        } catch (IOException ex) {
            throw new RetryableConnectorException("Failed to deserialize posts response", ex);
        }
    }

    /**
     * Normalizes the configured base URL so request construction can safely concatenate paths.
     */
    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    /**
     * Ensures the configured endpoint begins with a slash.
     */
    private String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("postsEndpoint must not be blank");
        }

        return value.startsWith("/") ? value : "/" + value;
    }

    /**
     * Parses a numeric Retry-After value in seconds, returning {@code null} when the header is
     * absent or unusable.
     */
    private Duration parseRetryAfter(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return null;
        }

        try {
            long retryAfterSeconds = Long.parseLong(retryAfterHeader.trim());
            if (retryAfterSeconds < 0) {
                return null;
            }
            return Duration.ofSeconds(retryAfterSeconds);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}