package com.scaffold.starter.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.test.annotation.ScaffoldWebSlice;
import com.scaffold.starter.test.client.ScaffoldMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @ScaffoldWebSlice} 切片自证测试：验证被测 Controller 天然继承
 * 生产级横切层（traceId / 统一异常响应 / 校验映射），且环境零外部依赖。
 *
 * @author lianghang
 */
@ScaffoldWebSlice(controllers = DemoController.class)
class WebSliceTest {

    /**
     * 切片需要测试类可见的 {@code @SpringBootConfiguration}：业务方即应用主类，
     * 本模块内用嵌套静态类代替。组件扫描模拟业务应用——被测 Controller 在扫描
     * 范围内，{@code controllers} 属性进一步收窄到被测对象。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = WebSliceTest.class)
    static class TestBoot {

        /** 业务方通常这样包一层：把助手注册成 Bean，测试内直接注入使用。 */
        @Bean
        ScaffoldMockMvc scaffoldMockMvc(org.springframework.test.web.servlet.MockMvc mockMvc, ObjectMapper om) {
            return new ScaffoldMockMvc(mockMvc, om);
        }
    }

    @Autowired
    private ScaffoldMockMvc mvc;

    @Test
    void rawEndpointReachable() {
        // 非 R 包装的端点（如 ping/文件流）仍可用：只断言 HTTP 状态，不做业务码断言
        mvc.get("/demo/ok").expectStatus(200);
        assertTrue(mvc.get("/demo/ok").raw().contains("pong"));
    }

    @Test
    void bizErrorUsesUnifiedResponseAndTraceId() {
        mvc.get("/demo/biz-error")
                .expectStatus(HttpStatus.FORBIDDEN.value())
                .expectCode(GlobalErrorCode.FORBIDDEN.getCode())
                .expectTraceId();
        assertEquals("无权访问该资源", mvc.get("/demo/biz-error").body().path("message").asText());
    }

    @Test
    void validationErrorMappedToBadRequest() {
        DemoCreateForm form = new DemoCreateForm();
        mvc.post("/demo/create", form)
                .expectStatus(400)
                .expectCode(GlobalErrorCode.BAD_REQUEST.getCode())
                .expectTraceId();
    }
}
