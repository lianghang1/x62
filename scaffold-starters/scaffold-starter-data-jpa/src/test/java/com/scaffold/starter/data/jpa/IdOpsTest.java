package com.scaffold.starter.data.jpa;

import com.scaffold.common.util.SpringContextHolder;
import com.scaffold.starter.data.jpa.id.IdOps;
import com.scaffold.starter.data.jpa.id.Snowflake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdOps 测试：静态门面与容器内 Snowflake 单例协同、取号唯一趋势递增。
 *
 * @author lianghang
 */
class IdOpsTest {

    private AnnotationConfigApplicationContext context;

    @Configuration
    static class Config {

        @Bean
        SpringContextHolder springContextHolder() {
            return new SpringContextHolder();
        }

        @Bean
        Snowflake snowflake() {
            return new Snowflake(1, 1);
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void nextIdUniqueAndIncreasing() {
        long first = IdOps.nextId();
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            ids.add(IdOps.nextId());
        }
        assertEquals(200, ids.size());
        // 同毫秒内序列递增，200 连号必然不小于首号
        assertTrue(IdOps.nextId() > first);
    }
}
