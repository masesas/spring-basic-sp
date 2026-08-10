package com.masesas.exercises.demo1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.key-prefix:}")
    private String redisKeyPrefix;

    private String normalizePrefix(String prefix) {
        if (prefix == null) return "";
        String p = prefix.trim();
        if (p.isEmpty()) return "";
        return p.endsWith(":") ? p : (p + ":");
    }

    private String applyPrefix(String key) {
        if (key == null) return null;
        String prefix = normalizePrefix(redisKeyPrefix);
        if (prefix.isEmpty()) return key;
        if (key.startsWith(prefix)) return key;
        return prefix + key;
    }

    private String applyPrefixToPattern(String pattern) {
        if (pattern == null) return null;
        String prefix = normalizePrefix(redisKeyPrefix);
        if (prefix.isEmpty()) return pattern;
        if (pattern.startsWith(prefix)) return pattern;
        return prefix + pattern;
    }

    public <T> T get(String key, Class<T> clazz) {
        Object data = redisTemplate.opsForValue().get(applyPrefix(key));
        if (clazz.isInstance(data)) {
            return clazz.cast(data);
        }

        return null;
    }

    public void set(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(applyPrefix(key), value, Duration.ofSeconds(ttlSeconds));
    }

    public void delete(String key) {
        redisTemplate.delete(applyPrefix(key));
    }

    public void invalidateKeysByPattern(String pattern) {
        var k = applyPrefixToPattern(pattern);
        Set<String> keys = redisTemplate.keys(k);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public <T> void setList(String key, List<T> value, long ttlSeconds) {
        redisTemplate.opsForValue().set(applyPrefix(key), value, Duration.ofSeconds(ttlSeconds));
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        Object raw = redisTemplate.opsForValue().get(applyPrefix(key));
        if (raw == null) return List.of();

        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(clazz::isInstance)
                    .map(clazz::cast)
                    .toList();
        }

        return List.of();
    }

}
