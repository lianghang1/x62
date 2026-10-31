package com.scaffold.starter.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.BufferedReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 请求体缓存包装器测试：body 可重复读，验签与业务互不干扰。
 *
 * @author lianghang
 */
class CachedBodyRequestWrapperTest {

    @Test
    void bodyShouldBeReadableMultipleTimes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openapi/x");
        request.setContent("{\"k\":\"值\"}".getBytes("UTF-8"));

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        // getBodyString 与流式两次读取结果一致（验签读一次、Controller 再读一次）
        assertEquals("{\"k\":\"值\"}", wrapper.getBodyString());
        byte[] first = new byte[64];
        int len1 = wrapper.getInputStream().read(first);
        assertEquals("{\"k\":\"值\"}", new String(first, 0, len1, "UTF-8"));

        BufferedReader reader = wrapper.getReader();
        assertEquals("{\"k\":\"值\"}", reader.readLine());
    }

    @Test
    void emptyBodyShouldRoundTrip() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openapi/x");
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
        assertEquals("", wrapper.getBodyString());
        assertEquals(-1, wrapper.getInputStream().read());
    }
}
