package com.apiGateway.RateLimiter.model;

public class TokenBucket {
    private final double maxTokens;
    private double availableTokens;
    private final double refillRatePerNano;
    private long lastRefillTimestamp;
    private volatile long lastAccessTimestamp;

    public TokenBucket(double maxTokens, double refillTokensPerSecond) {
        this.maxTokens = maxTokens;
        this.availableTokens = maxTokens; // Start full
        this.refillRatePerNano = refillTokensPerSecond / 1_000_000_000.0;
        this.lastRefillTimestamp = System.nanoTime();
        this.lastAccessTimestamp = System.nanoTime();
    }

    /**
     * Thread-safe method to consume a token.
     */
    public synchronized boolean tryConsume() {
        this.lastAccessTimestamp = System.nanoTime();
        refill();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    /**
     * Thread-safe method to get currently available tokens (for headers).
     */
    public synchronized long getAvailableTokens() {
        refill();
        return (long) Math.floor(availableTokens);
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    private void refill() {
        long now = System.nanoTime();
        double tokensToAdd = (now - lastRefillTimestamp) * refillRatePerNano;
        if (tokensToAdd > 0) {
            availableTokens = Math.min(maxTokens, availableTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }
}