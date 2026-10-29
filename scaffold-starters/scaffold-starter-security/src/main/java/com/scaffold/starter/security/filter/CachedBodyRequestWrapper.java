package com.scaffold.starter.security.filter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求体缓存包装：openapi 过滤器验签要先读 body，包装后 Controller 仍可再次读取。
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[4096];
        int len;
        javax.servlet.ServletInputStream in = request.getInputStream();
        while ((len = in.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        this.cachedBody = bos.toByteArray();
    }

    public String getBodyString() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
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
                return bais.read();
            }
        };
    }

    @Override
    public java.io.BufferedReader getReader() {
        return new java.io.BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
