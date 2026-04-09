package com.jordaine.connector.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jordaine.connector.model.PostsResponse;

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

    public DummyJsonClient(String baseUrl, String postsEndpoint) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.baseUrl = stripTrailingSlash(baseUrl);
        this.postsEndpoint = normalizeEndpoint(postsEndpoint);
    }

    public PostsResponse fetchPosts(int limit, int skip) throws IOException, InterruptedException {
        String url = baseUrl + postsEndpoint + "?limit=" + limit + "&skip=" + skip;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Failed to fetch posts. HTTP " + statusCode + ". Body: " + response.body());
        }

        return objectMapper.readValue(response.body(), PostsResponse.class);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeEndpoint(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("postsEndpoint must not be blank");
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}