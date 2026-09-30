package com.apiGateway.RateLimiter.service;

import com.apiGateway.RateLimiter.model.TokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBucketRegistry {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketRegistry.class);

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Injected from application.yml, with defaults of 60 tokens max, 1 token per second
    @Value("${ratelimit.capacity:60}")
    private double capacity;

    @Value("${ratelimit.refill-rate:1}")
    private double refillRate;

    // If a user doesn't make a request for 5 minutes, delete their bucket
    private static final long IDLE_TIMEOUT_NANOS = 5L * 60 * 1_000_000_000;

    public TokenBucket getBucket(String userId) {
        return buckets.computeIfAbsent(userId, k -> new TokenBucket(capacity, refillRate));
    }

    /**
     * Runs every 60 seconds to clean up inactive buckets to prevent memory leaks.
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanupIdleBuckets() {
        long now = System.nanoTime();
        int initialSize = buckets.size();

        buckets.entrySet().removeIf(entry ->
                (now - entry.getValue().getLastAccessTimestamp()) > IDLE_TIMEOUT_NANOS
        );

        int removed = initialSize - buckets.size();
        if (removed > 0) {
            logger.debug("Rate Limiter Cleanup: Evicted {} inactive token buckets.", removed);
        }
    }
}
