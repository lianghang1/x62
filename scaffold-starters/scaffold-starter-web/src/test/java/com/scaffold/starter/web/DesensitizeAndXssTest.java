package com.scaffold.starter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.starter.web.desensitize.Desensitize;
import com.scaffold.starter.web.desensitize.DesensitizeType;
import com.scaffold.starter.web.xss.XssRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 脱敏注解 + XSS 转义单元测试。
 */
class DesensitizeAndXssTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class UserDto {
        @Desensitize(DesensitizeType.PHONE)
        public String phone = "13800001234";

        @Desensitize(DesensitizeType.EMAIL)
        public String email = "zhangsan@example.com";

        @Desensitize(DesensitizeType.ID_CARD)
        public String idCard = "110101199001011234";

        @Desensitize(DesensitizeType.BANK_CARD)
        public String bankCard = "6222021234567890567";

        @Desensitize(DesensitizeType.NAME)
        public String name = "张三丰";

        @Desensitize(value = DesensitizeType.CUSTOM, prefixKeep = 2, suffixKeep = 2)
        public String custom = "ABCD1234EFGH";
    }

    @Test
    void desensitizeAllTypes() throws Exception {
        String json = mapper.writeValueAsString(new UserDto());
        assertTrue(json.contains("138****1234"), json);
        assertTrue(json.contains("z****@example.com"), json);
        assertTrue(json.contains("110101****1234"), json);
        assertTrue(json.contains("6222****0567"), json);
        assertTrue(json.contains("张****"), json);
        assertTrue(json.contains("AB****GH"), json);
        assertFalse(json.contains("13800001234"));
    }

    @Test
    void xssEscapeParams() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("name", "<script>alert(1)</script>");
        request.addParameter("normal", "abc");
        XssRequestWrapper wrapper = new XssRequestWrapper(request);
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", wrapper.getParameter("name"));
        assertEquals("abc", wrapper.getParameter("normal"));
    }

    @Test
    void xssEscapeJsonBodyKeepsStructure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String body = "{\"title\":\"<img src=x onerror=alert(1)>\",\"amount\":12.5}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        String escaped;
        try {
            XssRequestWrapper wrapper = new XssRequestWrapper(request);
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            java.io.InputStream in = wrapper.getInputStream();
            byte[] chunk = new byte[4096];
            int len;
            while ((len = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, len);
            }
            escaped = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        // 结构仍可被 Jackson 解析，值内标签已被转义
        Map<String, Object> parsed = new ObjectMapper().readValue(escaped, Map.class);
        assertEquals("<img src=x onerror=alert(1)>".replace("<", "&lt;").replace(">", "&gt;"),
                parsed.get("title"));
        assertEquals(12.5, parsed.get("amount"));
    }
}
