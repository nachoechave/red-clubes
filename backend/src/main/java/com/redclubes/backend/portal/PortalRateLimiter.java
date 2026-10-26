package com.redclubes.backend.portal;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Single-instance, bounded, fail-closed limiter. Keys contain only keyed digests. */
@Component
public class PortalRateLimiter {
    private record Bucket(Instant expires, Instant last, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    PortalRateLimiter(Clock clock) { this.clock = clock; }

    synchronized boolean allow(String key, int maximum, int cooldownSeconds) {
        Instant now = clock.instant();
        buckets.entrySet().removeIf(e -> !e.getValue().expires.isAfter(now));
        Bucket old = buckets.get(key);
        if (old != null && (old.count >= maximum || old.last.plusSeconds(cooldownSeconds).isAfter(now))) return false;
        if (old == null && buckets.size() >= 10000) return false;
        buckets.put(key, new Bucket(old == null ? now.plusSeconds(900) : old.expires, now, old == null ? 1 : old.count + 1));
        return true;
    }
}
