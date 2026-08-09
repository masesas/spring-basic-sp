package com.masesas.exercises.demo1.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;

    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "demo1:login-attempt:";

    private final StringRedisTemplate redis;

    public boolean isLocked(String username) {
        String value = redis.opsForValue().get(key(username));
        return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String username) {
        Long attempts = redis.opsForValue().increment(key(username));
        if (attempts != null && attempts == 1L) {
            redis.expire(key(username), LOCK_DURATION);
        }
    }

    public void reset(String username) {
        redis.delete(key(username));
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
