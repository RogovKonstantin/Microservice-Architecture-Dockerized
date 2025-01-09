package com.example.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventCacheService {

    private static final Logger log = LoggerFactory.getLogger(EventCacheService.class);
    private static final String REDIS_PREFIX = "EVENT_CACHE";

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Map<String, Object>> hashOperations;

    public EventCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    private boolean isRedisAvailable() {
        try {
            redisTemplate.hasKey("healthcheck");
            return true;
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis is unavailable: {}", ex.getMessage());
            return false;
        }
    }

    public void cacheEvent(Long id, Map<String, Object> event) {
        if (!isRedisAvailable()) return;

        try {
            hashOperations.put(REDIS_PREFIX, String.valueOf(id), event);
            log.debug("Event id={} cached in Redis", id);
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Failed to cache event id={} due to Redis being unavailable: {}", id, ex.getMessage());
        }
    }

    public Map<String, Object> getCachedEvent(Long id) {
        if (!isRedisAvailable()) return null;

        try {
            return hashOperations.get(REDIS_PREFIX, String.valueOf(id));
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Failed to retrieve event id={} from Redis due to Redis being unavailable: {}", id, ex.getMessage());
            return null;
        }
    }

    public void updateCachedEvent(Long id, Map<String, Object> updatedEvent) {
        if (!isRedisAvailable()) return;

        try {
            if (hashOperations.hasKey(REDIS_PREFIX, String.valueOf(id))) {
                hashOperations.put(REDIS_PREFIX, String.valueOf(id), updatedEvent);
                log.debug("Updated cached event id={} in Redis", id);
            }
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Failed to update event id={} in Redis cache due to Redis being unavailable: {}", id, ex.getMessage());
        }
    }

    public void deleteCachedEvent(Long id) {
        if (!isRedisAvailable()) return;

        try {
            hashOperations.delete(REDIS_PREFIX, String.valueOf(id));
            log.debug("Deleted cached event id={} from Redis", id);
        } catch (RedisConnectionFailureException | QueryTimeoutException ex) {
            log.warn("Failed to delete event id={} from Redis cache due to Redis being unavailable: {}", id, ex.getMessage());
        }
    }
}
