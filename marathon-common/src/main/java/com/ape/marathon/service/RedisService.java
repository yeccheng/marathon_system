package com.ape.marathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnClass(RedisTemplate.class)
public class RedisService {

    public static final long TTL_MINUTES = 10L;
    public static final long TTL_EMPTY_MINUTES = 1L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (key == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void setWithSmartTtl(String key, Object value, boolean empty) {
        long ttl = empty ? TTL_EMPTY_MINUTES : TTL_MINUTES;
        set(key, value, ttl, TimeUnit.MINUTES);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        Object val = redisTemplate.opsForValue().get(key);
        return (T) val;
    }

    public void delete(String key) {
        if (key != null) {
            redisTemplate.delete(key);
        }
    }

    public long deleteByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return 0L;
        }
        final String pattern = prefix.endsWith("*") ? prefix : prefix + "*";
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<String>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            Cursor<byte[]> cursor = connection.scan(options);
            try {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } finally {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
            return result;
        });
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0L : deleted;
    }
}
