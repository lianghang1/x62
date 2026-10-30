package com.scaffold.starter.protection.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Redis 分布式锁工具类。
 *
 * <p>加锁用 setIfAbsent 写入唯一持有值；解锁经 Lua 校验持有值后删除。
 * 不做看门狗自动续期——长任务请显式调大 leaseSeconds，简单透明优先于复杂机制。</p>
 *
 * @author lianghang
 */
public class RedisLockUtil {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试加锁；成功返回持有值（解锁时传入），失败返回 null。
     */
    public String tryLock(String key, long waitMillis, int leaseSeconds) {
        String fullKey = "scaffold:lock:" + key;
        String holder = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMillis;
        do {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey, holder, Duration.ofSeconds(leaseSeconds));
            if (ok != null && ok) {
                return holder;
            }
            if (waitMillis > 0) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        } while (System.currentTimeMillis() < deadline);
        return null;
    }

    /** 释放锁（仅持有者本人可释放） */
    public void unlock(String key, String holder) {
        if (holder == null) {
            return;
        }
        List<String> keys = Arrays.asList("scaffold:lock:" + key);
        redisTemplate.execute(UNLOCK_SCRIPT, keys, holder);
    }
}
