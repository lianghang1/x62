package com.scaffold.starter.web.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.result.R;
import com.scaffold.starter.web.trace.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 访问日志 START/END 行测试：START 由 TraceIdFilter 在进入时记录（到达性证明），
 * END 由切面在退出时记录（处理结果）；两行靠 traceId 配对。
 *
 * <p>基于 MockMvc / AspectJProxyFactory 独立环境、零外部依赖；日志经
 * {@code ListAppender} 直接从 logback 事件流断言。</p>
 *
 * @author lianghang
 */
class AccessStartEndLogTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger traceLogger;
    private Logger aspectLogger;

    @BeforeEach
    void setUp() {
        traceLogger = (Logger) LoggerFactory.getLogger(TraceIdFilter.class);
        aspectLogger = (Logger) LoggerFactory.getLogger(AccessLogAspect.class);
        appender = new ListAppender<>();
        appender.start();
        traceLogger.addAppender(appender);
        aspectLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        traceLogger.detachAppender(appender);
        aspectLogger.detachAppender(appender);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void startLineShouldBeLoggedOnEnter() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .addFilters(new TraceIdFilter(true))
                .build();
        mockMvc.perform(get("/demo/hello"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"));

        List<String> startLines = linesWith("START");
        assertThat(startLines).hasSize(1);
        assertThat(startLines.get(0)).contains("[access] START").contains("GET").contains("/demo/hello");
        // MDC 已就绪：START 事件携带 traceId，与响应头一致
        assertThat(appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("START"))
                .findFirst().orElseThrow().getMDCPropertyMap().get("traceId"))
                .isNotBlank();
    }

    @Test
    void startLineShouldBeSuppressedWhenDisabled() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .addFilters(new TraceIdFilter(false))
                .build();
        mockMvc.perform(get("/demo/hello")).andExpect(status().isOk());

        assertThat(linesWith("START")).isEmpty();
    }

    @Test
    void endLineShouldBeLoggedOnSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/hello");
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));

        AccessLogAspect aspect = new AccessLogAspect(new ObjectMapper());
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoController());
        factory.addAspect(aspect);
        DemoController proxy = factory.getProxy();

        assertThat(proxy.hello().getCode()).isEqualTo(0);

        List<String> endLines = linesWith("END");
        assertThat(endLines).hasSize(1);
        assertThat(endLines.get(0))
                .contains("[access] END")
                .contains("GET /demo/hello")
                .contains("status=200")
                .contains("cost=")
                .contains("result=");
    }

    @Test
    void endLineShouldContainErrorOnFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo/fail");
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));

        AccessLogAspect aspect = new AccessLogAspect(new ObjectMapper());
        AspectJProxyFactory factory = new AspectJProxyFactory(new DemoController());
        factory.addAspect(aspect);
        DemoController proxy = factory.getProxy();

        assertThatThrownBy(proxy::fail).isInstanceOf(IllegalStateException.class);

        List<String> endLines = linesWith("END");
        assertThat(endLines).hasSize(1);
        assertThat(endLines.get(0)).contains("error=IllegalStateException: 故意失败");
    }

    private List<String> linesWith(String keyword) {
        return appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(msg -> msg.contains(keyword))
                .collect(Collectors.toList());
    }

    @RestController
    static class DemoController {

        @GetMapping("/demo/hello")
        public R<String> hello() {
            return R.ok("hi");
        }

        @GetMapping("/demo/fail")
        public R<String> fail() {
            throw new IllegalStateException("故意失败");
        }
    }
}
