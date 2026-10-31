package com.scaffold.starter.security.filter;

import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * internal 认证过滤器测试：白名单跳过、无效 token 401、有效 token 装配上下文且请求后清理。
 *
 * @author lianghang
 */
class InternalAuthenticationFilterTest {

    private TokenService tokenService;
    private SecurityProperties properties;
    private InternalAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        properties = new SecurityProperties();
        filter = new InternalAuthenticationFilter(tokenService, properties);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        AuthenticationSupport.clear();
    }

    private SessionInfo session() {
        SessionInfo s = new SessionInfo();
        s.setAccountId(7L);
        s.setAccountNo("alice");
        s.setRoles(Collections.singleton("admin"));
        return s;
    }

    @Test
    void whitelistedUriShouldSkipAuthentication() throws Exception {
        properties.getInternalWhitelist().add("/internal/auth/login");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        // 白名单直接放行，未触碰 token 服务
        verify(tokenService, never()).resolveWithRenew(anyString());
        verify(chain).doFilter(request, response);
        assertNull(LoginContext.currentOrNull());
    }

    @Test
    void missingOrInvalidTokenShouldReturn401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/system/account");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolveWithRenew(null)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":1001"),
                "业务码 1001（未认证），HTTP 状态另断言");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void validTokenShouldApplyContextAndClearAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/system/account");
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolveWithRenew("good-token")).thenReturn(session());

        final boolean[] contextVisibleInChain = {false};
        chain = new FilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res)
                    throws IOException, ServletException {
                contextVisibleInChain[0] = "alice".equals(
                        LoginContext.currentOrNull() == null ? null : LoginContext.current().getAccountNo());
            }
        };

        filter.doFilter(request, response, chain);

        assertTrue(contextVisibleInChain[0], "链内应能取到当前登录人");
        assertNull(LoginContext.currentOrNull(), "请求结束后上下文必须清理");
    }

    @Test
    void xTokenHeaderShouldAlsoBeAccepted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/x");
        request.addHeader("X-Token", "xtok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolveWithRenew("xtok")).thenReturn(session());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
