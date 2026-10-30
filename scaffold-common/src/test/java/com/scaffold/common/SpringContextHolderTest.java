package com.scaffold.common;

import com.scaffold.common.util.SpringContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SpringContextHolder 测试：容器就绪后静态可用，未就绪抛明确异常。
 *
 * @author lianghang
 */
class SpringContextHolderTest {

    @Configuration
    static class Config {
        @Bean
        public SpringContextHolder holder() {
            return new SpringContextHolder();
        }

        @Bean
        public String demoBean() {
            return "demo";
        }
    }

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(Config.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void staticAccessAfterRefresh() {
        assertSame("demo", SpringContextHolder.getBean("demoBean"));
        assertEquals("demo", SpringContextHolder.getBean(String.class));
        assertEquals("v", SpringContextHolder.getProperty("missing.key", "v"));
    }

    @Test
    void throwsExplicitlyWhenContextClosed() {
        context.close();
        assertThrows(IllegalStateException.class, () -> SpringContextHolder.getBean("demoBean"));
    }
}
