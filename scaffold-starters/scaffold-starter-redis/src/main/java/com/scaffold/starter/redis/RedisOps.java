package com.scaffold.starter.redis;

import com.scaffold.common.util.JsonUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis 薄门面：管 80% 高频操作，把「正确的事」做成默认——
 * <ul>
 *   <li>对象自动 JSON 序列化/反序列化（统一走 {@link JsonUtils}，与 web 层行为一致）</li>
 *   <li><b>TTL 强制</b>：{@code set} 系列必须显式传 TTL，传 {@link Duration#ZERO} 才表示永久——
 *       从 API 层杜绝「忘设过期」导致的 key 永久泄漏</li>
 *   <li>key 规约：前缀一律引用 {@link com.scaffold.common.constant.RedisKeys} 常量，禁止裸字符串</li>
 * </ul>
 *
 * <p>刻意<b>不封装</b> hash 复杂操作 / list / set / zset / pipeline / Lua——
 * 这些低频结构化场景直接注入标准 {@link StringRedisTemplate} 使用，
 * 薄门面只做高频路径，不设 440 行大杂烩式的墙。</p>
 *
 * @author lianghang
 */
public class RedisOps {

    private final StringRedisTemplate redisTemplate;

    public RedisOps(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 底层模板：hash/list/zset/pipeline/Lua 等低频结构化操作从这里透出 */
    public StringRedisTemplate template() {
        return redisTemplate;
    }

    // ---------- string 读写 ----------

    /**
     * 写入对象（自动 JSON 序列化），TTL 必填。
     *
     * @param ttl 过期时间；{@link Duration#ZERO} 表示永久（显式声明，无「默认永久」）
     */
    public void set(String key, Object value, Duration ttl) {
        requireTtl(ttl);
        String json = value instanceof String ? (String) value : JsonUtils.toJson(value);
        redisTemplate.opsForValue().set(key, json, ttl);
    }

    /**
     * 不存在才写入（写入即计时），常用于一次性凭证 / 简单防重。
     *
     * @return true=写入成功（首次）；false=已存在
     */
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        requireTtl(ttl);
        String json = value instanceof String ? (String) value : JsonUtils.toJson(value);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, json, ttl));
    }

    /** 读对象（自动反序列化）；key 不存在返回 null */
    public <T> T get(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        return json == null ? null : JsonUtils.fromJson(json, type);
    }

    /** 读原始字符串；key 不存在返回 null */
    public String getString(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 读后即删（一次性消费），返回消费到的对象；常用于验证码 / 一次性凭证 */
    public <T> T getAndDelete(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        redisTemplate.delete(key);
        return JsonUtils.fromJson(json, type);
    }

    // ---------- 生命周期 ----------

    /** 续期；key 不存在返回 false */
    public boolean expire(String key, Duration ttl) {
        requireTtl(ttl);
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl));
    }

    /** 查剩余存活时间；key 不存在返回 null，永久 key 返回 -1 */
    public Duration getExpire(String key) {
        Long seconds = redisTemplate.getExpire(key);
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 删除一个或多个 key，返回删除数量 */
    public long delete(String... keys) {
        Long count = redisTemplate.delete(Arrays.asList(keys));
        return count == null ? 0 : count;
    }

    public long delete(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count == null ? 0 : count;
    }

    // ---------- 计数 ----------

    /** 原子自增并返回新值（配合 expire 可实现固定窗口计数） */
    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0 : value;
    }

    /** 原子递减并返回新值 */
    public long decrement(String key, long delta) {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value == null ? 0 : value;
    }

    // ---------- hash（仅最常用三个） ----------

    /** 写 hash 字段；{@code ttl} 为整个 hash 的过期时间，ZERO 表示永久 */
    public void hSet(String key, String field, Object value, Duration ttl) {
        requireTtl(ttl);
        String json = value instanceof String ? (String) value : JsonUtils.toJson(value);
        redisTemplate.opsForHash().put(key, field, json);
        if (!Duration.ZERO.equals(ttl)) {
            redisTemplate.expire(key, ttl);
        }
    }

    /** 读 hash 字段（自动反序列化）；不存在返回 null */
    public <T> T hGet(String key, String field, Class<T> type) {
        Object json = redisTemplate.opsForHash().get(key, field);
        return json == null ? null : JsonUtils.fromJson(json.toString(), type);
    }

    /** 删除 hash 字段，返回删除数量 */
    public long hDelete(String key, Object... fields) {
        Long count = redisTemplate.opsForHash().delete(key, fields);
        return count == null ? 0 : count;
    }

    /** 读整个 hash（值为原始 JSON 串，类型由调用方解析） */
    public Map<String, String> hGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        return entries.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    private static void requireTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("Redis 写入必须显式传非负 TTL（Duration.ZERO 表示永久），禁止默认永久");
        }
    }
}
