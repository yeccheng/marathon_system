package com.ape.marathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类（手写封装，不使用 @Cacheable）
 * 提供：get / set（带 TTL）/ delete / deleteByPrefix（SCAN 删除前缀）
 */
@Service
public class RedisService {

    /** 正常业务数据缓存时间：10 分钟 */
    public static final long TTL_MINUTES = 10L;

    /** 空结果缓存时间：1 分钟（防缓存穿透） */
    public static final long TTL_EMPTY_MINUTES = 1L;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存（自定义过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        if (key == null) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 写入缓存：有数据 TTL=10 分钟；空列表/空分页 TTL=1 分钟（防穿透）
     *
     * @param empty 是否为空结果
     */
    public void setWithSmartTtl(String key, Object value, boolean empty) {
        long ttl = empty ? TTL_EMPTY_MINUTES : TTL_MINUTES;
        set(key, value, ttl, TimeUnit.MINUTES);
    }

    /**
     * 读取缓存；不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null) {
            return null;
        }
        Object val = redisTemplate.opsForValue().get(key);
        return (T) val;
    }

    /**
     * 删除单个 key
     */
    public void delete(String key) {
        if (key != null) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 按前缀批量删除 key
     * <p>
     * 实现方式：使用 SCAN 游标迭代，而不是 KEYS。
     * <ul>
     *   <li>KEYS pattern：一次性返回全部匹配 key，会阻塞 Redis 单线程，生产环境禁用</li>
     *   <li>SCAN：分批游标扫描，不阻塞业务，适合前缀清理（如 event:page:*）</li>
     * </ul>
     *
     * @param prefix 前缀，可带或不带尾部 *（内部会补 *）
     * @return 实际删除的 key 数量
     */
    public long deleteByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return 0L;
        }
        final String pattern = prefix.endsWith("*") ? prefix : prefix + "*";

        // 通过 connection.scan 分批收集匹配 key（兼容 Spring Data Redis 2.x）
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
                    // ignore
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
