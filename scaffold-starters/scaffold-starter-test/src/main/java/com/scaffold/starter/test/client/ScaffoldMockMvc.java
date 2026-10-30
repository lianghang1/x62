package com.scaffold.starter.test.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.result.R;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MockMvc 的 JSON 助手：以统一响应体 {@link R} 为中心收口断言语义。
 *
 * <p>替代「perform + andExpect + jsonPath + readValue」的样板串：一次调用
 * 拿到 {@link JsonCall}，链式断言 HTTP 状态、业务码、traceId，并把 data
 * 直接反序列化为目标类型。请求体自动按 UTF-8 JSON 发送，响应体自动按
 * UTF-8 解析——规避 MockMvc 默认 ISO-8859-1 中文乱码这一高频坑。</p>
 *
 * <p>用法：{@code scaffoldMvc.post("/api/user", body).expectOk().expectCode(0);} </p>
 *
 * @author lianghang
 */
public class ScaffoldMockMvc {

    private static final String SUCCESS_FIELD = "code";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public ScaffoldMockMvc(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /** GET 请求。 */
    public JsonCall get(String urlTemplate, Object... uriVars) {
        return perform(MockMvcRequestBuilders.get(urlTemplate, uriVars));
    }

    /** POST 请求，body 序列化为 JSON；body 传 null 时不带请求体。 */
    public JsonCall post(String urlTemplate, Object body) {
        return perform(MockMvcRequestBuilders.post(urlTemplate), body);
    }

    /** PUT 请求，body 序列化为 JSON。 */
    public JsonCall put(String urlTemplate, Object body) {
        return perform(MockMvcRequestBuilders.put(urlTemplate), body);
    }

    /** DELETE 请求。 */
    public JsonCall delete(String urlTemplate, Object... uriVars) {
        return perform(MockMvcRequestBuilders.delete(urlTemplate, uriVars));
    }

    private JsonCall perform(MockHttpServletRequestBuilder builder) {
        return perform(builder, null);
    }

    private JsonCall perform(MockHttpServletRequestBuilder builder, Object body) {
        try {
            builder.accept(MediaType.APPLICATION_JSON);
            builder.characterEncoding("UTF-8");
            if (body != null) {
                builder.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body));
            }
            MvcResult result = mockMvc.perform(builder).andReturn();
            return new JsonCall(result);
        } catch (Exception e) {
            throw new IllegalStateException("MockMvc 请求执行失败: " + builder, e);
        }
    }

    /**
     * 单次调用的响应视图：HTTP 状态、业务码、traceId 与 data 反序列化的统一断言入口。
     */
    public class JsonCall {

        private final MvcResult result;
        private final JsonNode body;

        private JsonCall(MvcResult result) {
            this.result = result;
            JsonNode parsed = null;
            try {
                String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                if (content != null && !content.isEmpty() && content.trim().startsWith("{")) {
                    parsed = objectMapper.readTree(content);
                }
            } catch (Exception e) {
                throw new IllegalStateException("响应体 JSON 解析失败", e);
            }
            this.body = parsed;
        }

        /** 断言 HTTP 200 且业务码为 0（成功）。 */
        public JsonCall expectOk() {
            return expectStatus(200).expectCode(0);
        }

        /** 断言 HTTP 状态码。 */
        public JsonCall expectStatus(int status) {
            assertEquals(status, result.getResponse().getStatus(), "HTTP 状态不符，响应体: " + raw());
            return this;
        }

        /** 断言统一响应体业务码。 */
        public JsonCall expectCode(int code) {
            assertNotNull(body, "响应体不是 JSON 对象，无法断言业务码，原文: " + raw());
            assertEquals(code, body.path("code").asInt(),
                    "业务码不符，message=" + body.path("message").asText());
            return this;
        }

        /** 断言 traceId 双轨齐备：X-Trace-Id 响应头与响应体 traceId 字段均非空。 */
        public JsonCall expectTraceId() {
            String header = result.getResponse().getHeader("X-Trace-Id");
            assertTrue(header != null && !header.isEmpty(), "缺少 X-Trace-Id 响应头");
            if (body != null && body.hasNonNull(R.TRACE_ID_KEY)) {
                String traceId = body.path(R.TRACE_ID_KEY).asText();
                assertTrue(traceId != null && !traceId.isEmpty(), "响应体缺少 traceId");
                assertEquals(header, traceId, "响应头与响应体的 traceId 不一致");
            }
            return this;
        }

        /** 原始 JsonNode 响应体。 */
        public JsonNode body() {
            return body;
        }

        /** 把 data 字段反序列化为目标类型；data 为 null 时返回 null。 */
        public <T> T data(Class<T> type) {
            JsonNode data = body == null ? null : body.get("data");
            if (data == null || data.isNull()) {
                return null;
            }
            try {
                return objectMapper.treeToValue(data, type);
            } catch (Exception e) {
                throw new IllegalStateException("data 反序列化失败: " + type.getName(), e);
            }
        }

        /** 原始响应字符串，断言失败时用于输出上下文。 */
        public String raw() {
            try {
                return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "<unreadable>";
            }
        }

        /** 底层 MvcResult，需要 ResultMatcher 级别细粒度断言时兜底使用。 */
        public MvcResult mvcResult() {
            return result;
        }
    }
}
