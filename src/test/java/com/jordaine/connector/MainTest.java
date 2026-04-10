package com.jordaine.connector;

import com.jordaine.connector.config.ConnectorConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests one-shot and polling loop behavior in {@link Main}.
 */
class MainTest {

    @Test
    void shouldRunOnceWhenPollingIsDisabled() throws Exception {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
                Duration.ofMillis(10),
                100,
                Path.of("data/test-checkpoint.json"),
                Path.of("output/test.jsonl"),
                0
        );

        AtomicInteger crawlCount = new AtomicInteger();
        AtomicInteger sleepCount = new AtomicInteger();

        Main.runCrawlerLoop(
                config,
                crawlCount::incrementAndGet,
                duration -> sleepCount.incrementAndGet()
        );

        assertEquals(1, crawlCount.get());
        assertEquals(0, sleepCount.get());
    }

    @Test
    void shouldSleepAndRunAgainWhenPollingIsEnabled() {
        ConnectorConfig config = new ConnectorConfig(
                "https://dummyjson.com",
                "/posts",
                20,
                0,
                Duration.ofMillis(10),
                100,
                Path.of("data/test-checkpoint.json"),
                Path.of("output/test.jsonl"),
                3
        );

        AtomicInteger crawlCount = new AtomicInteger();
        List<Duration> sleepDurations = new ArrayList<>();

        assertThrows(StopPollingException.class, () ->
                Main.runCrawlerLoop(
                        config,
                        () -> {
                            if (crawlCount.incrementAndGet() >= 2) {
                                throw new StopPollingException();
                            }
                        },
                        sleepDurations::add
                )
        );

        assertEquals(2, crawlCount.get());
        assertEquals(List.of(Duration.ofSeconds(3)), sleepDurations);
    }

    @Test
    void shouldRejectNegativePollInterval() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConnectorConfig(
                        "https://dummyjson.com",
                        "/posts",
                        20,
                        0,
                        Duration.ofMillis(10),
                        100,
                        Path.of("data/test-checkpoint.json"),
                        Path.of("output/test.jsonl"),
                        -1
                )
        );
    }

    private static final class StopPollingException extends Exception {
    }
}
