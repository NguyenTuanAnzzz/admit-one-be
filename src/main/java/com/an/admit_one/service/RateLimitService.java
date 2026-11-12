package com.an.admit_one.service;

import com.an.admit_one.exception.RateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RateLimitService {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private RedisScript<Long> rateLimitScript;

    public void check(String key, int limit, int windowSeconds) {
        Long waitSeconds;
        try {
            waitSeconds = redis.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(windowSeconds),
                    String.valueOf(limit),
                    // Mã riêng của request này, làm member trong Sorted Set (ARGV[3])
                    UUID.randomUUID().toString()
            );
        } catch (RedisConnectionFailureException e) {
            // Redis chết thì vẫn cho mua, không để rate limit làm hỏng việc bán vé
            return;
        }

        if (waitSeconds != null && waitSeconds > 0) {
            throw new RateLimitException(waitSeconds);
        }
    }
}