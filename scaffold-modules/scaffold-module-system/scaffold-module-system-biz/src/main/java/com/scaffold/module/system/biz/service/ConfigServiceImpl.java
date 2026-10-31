package com.scaffold.module.system.biz.service;

import com.scaffold.common.constant.RedisKeys;
import com.scaffold.module.system.api.api.ConfigApi;
import com.scaffold.module.system.biz.entity.SysConfig;
import com.scaffold.module.system.biz.repository.SysConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scaffold.common.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统参数配置服务：进程内缓存 + 更新广播 + 多实例失效同步。
 *
 * <p>读取路径纯内存（{@link ConcurrentHashMap}，key 小写归一），零回源开销；
 * 管理端保存后除刷新本机缓存外，还经 Redis pub/sub 广播被改键，
 * 其他实例收到后回源该键——多实例最终一致，无需引入 MQ。</p>
 *
 * @author lianghang
 */
@Service
public class ConfigServiceImpl implements ConfigApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceImpl.class);

    private final SysConfigRepository configRepository;
    private final StringRedisTemplate redisTemplate;

    /** 配置缓存：key 小写 → 实体（含停用项，读取时统一过滤） */
    private final Map<String, SysConfig> cache = new ConcurrentHashMap<>();

    public ConfigServiceImpl(SysConfigRepository configRepository, StringRedisTemplate redisTemplate) {
        this.configRepository = configRepository;
        this.redisTemplate = redisTemplate;
    }

    /** 启动时全量预热缓存（配置表量级小，一次装载足够）。 */
    @PostConstruct
    public void loadCache() {
        cache.clear();
        List<SysConfig> all = configRepository.findAll();
        for (SysConfig config : all) {
            cache.put(normalize(config.getConfigKey()), config);
        }
        log.info("[config] 系统参数缓存预热完成: {} 项", cache.size());
    }

    @Override
    public String getValue(String key) {
        if (!StringUtils.isNotBlank(key)) {
            return null;
        }
        SysConfig config = cache.get(normalize(key));
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }
        return config.getConfigValue();
    }

    @Override
    public Integer getIntValue(String key) {
        String value = getValue(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("[config] 配置键非整数值: key={}, value={}", key, value);
            return null;
        }
    }

    @Override
    public Boolean getBoolValue(String key) {
        String value = getValue(key);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 管理端保存（新增或按 key 覆盖），刷新本机缓存并广播失效。
     */
    @Transactional
    public Long save(SysConfig form) {
        if (!StringUtils.isNotBlank(form.getConfigKey())) {
            throw com.scaffold.common.exception.BizException.of(
                    com.scaffold.common.exception.GlobalErrorCode.BAD_REQUEST, "configKey 不能为空");
        }
        String key = normalize(form.getConfigKey());
        form.setConfigKey(key);
        Optional<SysConfig> existing = configRepository.findByConfigKeyAndDeletedFalse(key);
        SysConfig entity = existing.orElseGet(SysConfig::new);
        entity.setConfigKey(key);
        entity.setConfigValue(form.getConfigValue());
        entity.setRemark(form.getRemark());
        if (form.getEnabled() != null) {
            entity.setEnabled(form.getEnabled());
        }
        SysConfig saved = configRepository.save(entity);
        cache.put(key, saved);
        broadcast(key);
        return saved.getId();
    }

    /**
     * 多实例失效同步：收到广播后回源被改键。
     *
     * @param key 被改配置键
     */
    public void onRefreshMessage(String key) {
        if (!StringUtils.isNotBlank(key)) {
            return;
        }
        configRepository.findByConfigKeyAndDeletedFalse(normalize(key))
                .ifPresentOrElse(
                        config -> cache.put(normalize(key), config),
                        () -> cache.remove(normalize(key)));
        log.info("[config] 收到失效广播，已刷新: key={}", key);
    }

    private void broadcast(String key) {
        try {
            redisTemplate.convertAndSend(RedisKeys.CONFIG_REFRESH, key);
        } catch (Exception e) {
            // 广播失败只影响其他实例的即时性（有兜底：重启或下次保存刷新），不阻塞保存
            log.warn("[config] 失效广播发送失败: key={}", key, e);
        }
    }

    private String normalize(String key) {
        return key == null ? null : key.trim().toLowerCase();
    }
}
