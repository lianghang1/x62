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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * online 链认证过滤器测试：匿名放行、无效 token 照常放行、有效 token 归一化身份。
 *
 * @author lianghang
 */
class OnlineTokenFilterTest {

    private TokenService tokenService;
    private OnlineTokenFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        filter = new OnlineTokenFilter(tokenService, new SecurityProperties());
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        AuthenticationSupport.clear();
    }

    private SessionInfo session() {
        SessionInfo s = new SessionInfo();
        s.setAccountId(9L);
        s.setAccountNo("member1");
        s.setRoles(Collections.singleton("member"));
        return s;
    }

    @Test
    void anonymousRequestShouldPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/online/goods");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(tokenService, never()).resolveWithRenew(anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenShouldStillPassThroughAsAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/online/goods");
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolveWithRenew("bad")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(LoginContext.currentOrNull());
    }

    @Test
    void validTokenShouldApplyIdentityInsideChainOnly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/online/orders");
        request.addHeader("X-Token", "tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolveWithRenew("tok")).thenReturn(session());

        final Long[] accountIdInChain = {null};
        chain = new FilterChain() {
            @Override
            public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse res) {
                accountIdInChain[0] = LoginContext.currentOrNull() == null
                        ? null : LoginContext.current().getAccountId();
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals(9L, accountIdInChain[0].longValue());
        assertNull(LoginContext.currentOrNull(), "请求结束后上下文必须清理");
    }
}
