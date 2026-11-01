package com.scaffold.module.system.api.dict;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.util.SpringContextHolder;
import com.scaffold.module.system.api.api.DictApi;
import com.scaffold.module.system.api.dto.DictDataDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DictFormat} 序列化测试：命中翻译、未命中回退、服务异常回退、容器未就绪回退。
 *
 * <p>核心断言是「翻译失败永不拖垮接口」——三种回退路径全部走原值。</p>
 *
 * @author lianghang
 */
class DictFormatSerializerTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    /** 被序列化的样例 DTO：status 带注解，raw 无注解做对照。 */
    static class OrderVO {

        @DictFormat("order_status")
        private String status;

        private String raw;

        public String getStatus() {
            return status;
        }

        public String getRaw() {
            return raw;
        }
    }

    @Configuration
    static class Ctx {

        @Bean
        SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }

        @Bean
        DictApi dictApi() {
            return new FakeDictApi();
        }
    }

    static class FakeDictApi implements DictApi {

        final Map<String, String> dict = new HashMap<>();

        FakeDictApi() {
            dict.put("0", "待支付");
            dict.put("1", "已支付");
        }

        @Override
        public String translate(String type, String value) {
            if ("BOOM".equals(value)) {
                throw new IllegalStateException("字典服务异常");
            }
            return dict.getOrDefault(value, value);
        }

        @Override
        public List<DictDataDTO> listByType(String type) {
            return Collections.emptyList();
        }

        @Override
        public void validate(String type, String value) {
        }
    }

    private void startContext() {
        context = new AnnotationConfigApplicationContext(Ctx.class);
    }

    private String serialize(OrderVO vo) throws Exception {
        return new ObjectMapper().writeValueAsString(vo);
    }

    @Test
    void shouldTranslateWhenDictHit() throws Exception {
        startContext();
        OrderVO vo = new OrderVO();
        vo.status = "0";
        vo.raw = "0";
        String json = serialize(vo);
        assertTrue(json.contains("\"status\":\"待支付\""));
        // 无注解字段保持原值
        assertTrue(json.contains("\"raw\":\"0\""));
    }

    @Test
    void shouldFallbackWhenDictMiss() throws Exception {
        startContext();
        OrderVO vo = new OrderVO();
        vo.status = "99";
        assertTrue(serialize(vo).contains("\"status\":\"99\""));
    }

    @Test
    void shouldFallbackWhenDictServiceThrows() throws Exception {
        startContext();
        OrderVO vo = new OrderVO();
        vo.status = "BOOM";
        assertTrue(serialize(vo).contains("\"status\":\"BOOM\""));
    }

    @Test
    void shouldFallbackWhenContextNotReady() throws Exception {
        startContext();
        Object previous = null;
        Field field = SpringContextHolder.class.getDeclaredField("CONTEXT");
        field.setAccessible(true);
        try {
            previous = field.get(null);
            field.set(null, null);
            OrderVO vo = new OrderVO();
            vo.status = "0";
            assertTrue(serialize(vo).contains("\"status\":\"0\""));
        } finally {
            field.set(null, previous);
        }
    }
}
