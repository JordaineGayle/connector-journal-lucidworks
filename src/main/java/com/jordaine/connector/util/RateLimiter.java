package com.jordaine.connector.util;

public class RateLimiter {
    private final long intervalMillis;
    private long lastRequestTime = 0L;

    public RateLimiter(int requestsPerMinute) {
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException("requestsPerMinute must be greater than 0");
        }

        this.intervalMillis = Math.max(1L, 60000L / requestsPerMinute);
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long waitTime = (lastRequestTime + intervalMillis) - now;

        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for rate limiter", ex);
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }
}