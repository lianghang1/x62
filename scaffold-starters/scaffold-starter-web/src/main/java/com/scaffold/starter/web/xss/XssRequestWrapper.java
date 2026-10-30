package com.scaffold.starter.web.xss;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 防护请求包装器，对参数与请求体做尖括号转义。
 *
 * <p>转义范围刻意收窄为 {@code <} 与 {@code >}：JSON 语法不含尖括号，转义只影响
 * 字符串值而不破坏结构；{@code &} 与引号不转义，避免二次编码问题。query/form
 * 参数在取值时逐个转义，body 在构造时一次性读入转义后缓存，供后续过滤器与
 * Controller 重复读取。</p>
 *
 * @author lianghang
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] escapedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        // 先触发容器解析 query/form 参数（form 解析会消费原始流），再缓存 body
        super.getParameterMap();
        this.escapedBody = readAndEscape(request);
    }

    private byte[] readAndEscape(HttpServletRequest request) throws IOException {
        ServletInputStream raw = request.getInputStream();
        if (raw == null) {
            return new byte[0];
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int len;
        while ((len = raw.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        String body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        return escape(body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream source = new ByteArrayInputStream(escapedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return source.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return source.read();
            }
        };
    }

    @Override
    public java.io.BufferedReader getReader() {
        return new java.io.BufferedReader(
                new java.io.InputStreamReader(new ByteArrayInputStream(escapedBody), StandardCharsets.UTF_8));
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> escaped = new HashMap<>();
        for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            String[] copy = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                copy[i] = escape(values[i]);
            }
            escaped.put(entry.getKey(), copy);
        }
        return escaped;
    }

    @Override
    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    @Override
    public String getParameter(String name) {
        String[] values = getParameterMap().get(name);
        return values == null ? null : values[0];
    }

    /** 只转义尖括号（JSON 结构安全），&, " 引号不转义避免二次编码问题。 */
    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }
}
