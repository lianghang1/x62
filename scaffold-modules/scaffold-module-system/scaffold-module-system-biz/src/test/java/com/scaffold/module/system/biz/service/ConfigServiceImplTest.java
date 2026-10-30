package com.scaffold.module.system.biz;

import com.scaffold.common.exception.BizException;
import com.scaffold.module.system.biz.entity.SysConfig;
import com.scaffold.module.system.biz.repository.SysConfigRepository;
import com.scaffold.module.system.biz.service.ConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 配置服务测试：缓存读路径、保存刷新缓存并广播、多实例失效回源。
 *
 * @author lianghang
 */
class ConfigServiceImplTest {

    private final SysConfigRepository repository = mock(SysConfigRepository.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    private ConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfigServiceImpl(repository, redisTemplate);
    }

    private SysConfig entity(String key, String value, boolean enabled) {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setEnabled(enabled);
        return config;
    }

    @Test
    void readPathFromCache() {
        when(repository.findAll()).thenReturn(Collections.singletonList(
                entity("upload.max-size-mb", "20", true)));
        service.loadCache();

        assertEquals("20", service.getValue("UPLOAD.MAX-SIZE-MB"));
        assertEquals("20", service.getValue(" upload.max-size-mb "));
        assertEquals(Integer.valueOf(20), service.getIntValue("upload.max-size-mb"));
        assertNull(service.getIntValue("upload.max-size-mb2"));
    }

    @Test
    void disabledOrMissingIsNull() {
        when(repository.findAll()).thenReturn(Collections.singletonList(
                entity("feature.off", "true", false)));
        service.loadCache();

        assertNull(service.getValue("feature.off"));
        assertNull(service.getBoolValue("feature.missing"));
        assertNull(service.getValue(null));
        assertNull(service.getValue(" "));
    }

    @Test
    void saveNormalizesKeyAndBroadcasts() {
        when(repository.findByConfigKeyAndDeletedFalse("demo.flag"))
                .thenReturn(Optional.empty());
        SysConfig saved = entity("demo.flag", "true", true);
        saved.setId(9L);
        when(repository.save(any(SysConfig.class))).thenReturn(saved);

        SysConfig form = new SysConfig();
        form.setConfigKey(" DEMO.FLAG ");
        form.setConfigValue("true");

        Long id = service.save(form);

        assertEquals(9L, id);
        assertEquals("demo.flag", form.getConfigKey());
        verify(redisTemplate).convertAndSend(eq("scaffold:config:refresh"), eq("demo.flag"));
        // 保存后本机缓存立即可读
        assertEquals("true", service.getValue("demo.flag"));
    }

    @Test
    void saveRejectsBlankKey() {
        SysConfig form = new SysConfig();
        form.setConfigValue("x");
        assertThrows(BizException.class, () -> service.save(form));
    }

    @Test
    void refreshMessageResyncsFromDb() {
        // 先预热为 enabled
        when(repository.findAll()).thenReturn(Collections.singletonList(
                entity("demo.flag", "true", true)));
        service.loadCache();
        assertTrue(Boolean.TRUE.equals(service.getBoolValue("demo.flag")));

        // 广播后 DB 已改为 false，回源应刷新
        SysConfig updated = entity("demo.flag", "false", true);
        when(repository.findByConfigKeyAndDeletedFalse("demo.flag"))
                .thenReturn(Optional.of(updated));
        service.onRefreshMessage("DEMO.FLAG");
        assertEquals("false", service.getValue("demo.flag"));
    }

    @Test
    void broadcastFailureDoesNotBreakSave() {
        when(repository.findByConfigKeyAndDeletedFalse("demo.flag"))
                .thenReturn(Optional.empty());
        SysConfig saved = entity("demo.flag", "1", true);
        when(repository.save(any(SysConfig.class))).thenReturn(saved);
        Mockito.doThrow(new RuntimeException("redis down"))
                .when(redisTemplate).convertAndSend(any(String.class), any(Object.class));

        SysConfig form = new SysConfig();
        form.setConfigKey("demo.flag");
        form.setConfigValue("1");
        assertEquals(1L, service.save(form));
    }
}
