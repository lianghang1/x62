package com.scaffold.starter.web;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import com.scaffold.starter.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * starter-web 基线测试，覆盖 traceId 过滤器与全局异常处理的默认行为。
 *
 * <p>基于 MockMvc 独立环境、零外部依赖：验证 traceId 的生成与上游透传复用，
 * 以及业务异常、未知异常的 HTTP 状态码与业务码双轨映射、内部细节隐藏。</p>
 *
 * @author lianghang
 */
public class WebStarterTest {

    private MockMvc mockMvc;

    @RestController
    static class DemoController {
        @GetMapping("/ok")
        public String ok() {
            return "pong";
        }

        @GetMapping("/biz-error")
        public String bizError() {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "演示业务错误");
        }

        @GetMapping("/system-error")
        public String systemError() {
            throw new RuntimeException("boom");
        }
    }

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new com.scaffold.starter.web.trace.TraceIdFilter())
                .build();
    }

    @Test
    public void traceId_shouldBeGeneratedAndReturned() throws Exception {
        mockMvc.perform(get("/ok"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("X-Trace-Id"));
    }

    @Test
    public void traceId_shouldReuseUpstreamHeader() throws Exception {
        mockMvc.perform(get("/ok").header("X-Trace-Id", "trace-abc-123"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Trace-Id", "trace-abc-123"));
    }

    @Test
    public void bizException_shouldMapToBusinessCode() throws Exception {
        // FORBIDDEN 带 httpStatus=403：HTTP 状态码与业务码双轨一致
        mockMvc.perform(get("/biz-error"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("演示业务错误"));
    }

    @Test
    public void unknownException_shouldHideDetails() throws Exception {
        mockMvc.perform(get("/system-error"))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.code").value(1500))
                .andExpect(jsonPath("$.message").value(GlobalErrorCode.SYSTEM_ERROR.getMessage()));
    }

    @Test
    public void unauthorized_shouldBeHttp401() {
        MDC.put("traceId", "t1");
        try {
            GlobalExceptionHandler handler = new GlobalExceptionHandler();
            ResponseEntity<?> response = handler.handleBiz(BizException.of(GlobalErrorCode.UNAUTHORIZED));
            assertEquals(401, response.getStatusCodeValue());
        } finally {
            MDC.remove("traceId");
        }
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
