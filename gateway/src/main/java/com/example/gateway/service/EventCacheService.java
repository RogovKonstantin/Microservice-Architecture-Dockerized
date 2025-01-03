package com.example.gateway.service;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventCacheService {

    private static final String REDIS_PREFIX = "EVENT_CACHE";

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Map<String, Object>> hashOperations;

    public EventCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = this.redisTemplate.opsForHash();
    }

    public void cacheEvent(Long id, Map<String, Object> event) {
        hashOperations.put(REDIS_PREFIX, String.valueOf(id), event);
    }

    public Map<String, Object> getCachedEvent(Long id) {
        return hashOperations.get(REDIS_PREFIX, String.valueOf(id));
    }

    public void updateCachedEvent(Long id, Map<String, Object> updatedEvent) {
        if (hashOperations.hasKey(REDIS_PREFIX, String.valueOf(id))) {
            hashOperations.put(REDIS_PREFIX, String.valueOf(id), updatedEvent);
        }
    }

    public void deleteCachedEvent(Long id) {
        hashOperations.delete(REDIS_PREFIX, String.valueOf(id));
    }
}
