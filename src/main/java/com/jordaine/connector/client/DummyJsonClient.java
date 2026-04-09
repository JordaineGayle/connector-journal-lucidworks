package com.jordaine.connector.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jordaine.connector.model.PostsResponse;
import com.jordaine.connector.util.RateLimiter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DummyJsonClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String postsEndpoint;
    private final RateLimiter rateLimiter;

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

    public PostsResponse fetchPosts(int limit, int skip) throws IOException, InterruptedException {
        rateLimiter.acquire();

        String url = baseUrl + postsEndpoint + "?limit=" + limit + "&skip=" + skip;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        if (statusCode == 429) {
            String retryAfterHeader = response.headers().firstValue("Retry-After").orElse(null);

            if (retryAfterHeader != null && !retryAfterHeader.isBlank()) {
                try {
                    long retryAfterSeconds = Long.parseLong(retryAfterHeader.trim());
                    long delayMillis = retryAfterSeconds * 1000L;
                    System.out.println("429 received. Respecting Retry-After for " + delayMillis + " ms.");
                    Thread.sleep(delayMillis);
                } catch (NumberFormatException ex) {
                    System.out.println("429 received with non-numeric Retry-After header: " + retryAfterHeader);
                }
            } else {
                System.out.println("429 received. No Retry-After header returned.");
            }

            throw new IOException("HTTP 429 Too Many Requests");
        }

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Failed to fetch posts. HTTP " + statusCode + ". Body: " + response.body());
        }

        return objectMapper.readValue(response.body(), PostsResponse.class);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("postsEndpoint must not be blank");
        }

        return value.startsWith("/") ? value : "/" + value;
    }
}