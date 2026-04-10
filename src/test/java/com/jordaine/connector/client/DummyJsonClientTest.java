package com.jordaine.connector.client;

import com.jordaine.connector.error.NonRetryableConnectorException;
import com.jordaine.connector.error.RetryableConnectorException;
import com.jordaine.connector.util.RateLimiter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DummyJsonClientTest {
    private MockWebServer server;
    private DummyJsonClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        client = new DummyJsonClient(
                server.url("/").toString(),
                "/posts",
                new RateLimiter(60_000)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldClassify429AsRetryableAndExposeRetryAfter() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(429)
                        .setHeader("Retry-After", "3")
                        .setBody("{}")
        );

        RetryableConnectorException exception = assertThrows(
                RetryableConnectorException.class,
                () -> client.fetchPosts(30, 0)
        );

        assertTrue(exception.getRetryAfter().isPresent());
        assertEquals(Duration.ofSeconds(3), exception.getRetryAfter().get());
    }

    @Test
    void shouldClassify5xxAsRetryable() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(503)
                        .setBody("{\"error\":\"unavailable\"}")
        );

        assertThrows(RetryableConnectorException.class, () -> client.fetchPosts(30, 0));
    }

    @Test
    void shouldClassify4xxAsNonRetryable() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setBody("{\"error\":\"not found\"}")
        );

        assertThrows(NonRetryableConnectorException.class, () -> client.fetchPosts(30, 0));
    }

    @Test
    void shouldClassifyMalformedJsonAsRetryable() {
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("{this-is-not-json")
        );

        assertThrows(RetryableConnectorException.class, () -> client.fetchPosts(30, 0));
    }
}
