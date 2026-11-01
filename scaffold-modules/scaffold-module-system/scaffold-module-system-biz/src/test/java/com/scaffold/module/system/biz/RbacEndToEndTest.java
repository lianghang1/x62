package com.scaffold.module.system.biz;

import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 端到端三态测试：认证过滤器 → 权限拦截器 → 管理接口全链路。
 *
 * <p>覆盖 internal 链的三种真实形态：无 token 401、有 token 无权 403、
 * 有权放行 200；权限集经 {@code TokenService} mock 注入（不依赖 Redis），
 * 其余走生产装配（H2 内存库承载实体表）。</p>
 *
 * @author lianghang
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "scaffold.config.refresh-listener-enabled=false")
@AutoConfigureMockMvc
class RbacEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @AfterEach
    void tearDown() {
        com.scaffold.starter.security.identity.LoginContext.clear();
    }

    @Test
    void internalEndpointShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/internal/accounts"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void internalEndpointShouldReturn403WithoutPermission() throws Exception {
        withSession("system:dict:manage");
        mockMvc.perform(get("/internal/accounts").with(bearer("limited-token")))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void internalEndpointShouldPassWithWildcardPermission() throws Exception {
        withSession("*");
        mockMvc.perform(get("/internal/accounts").with(bearer("admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void roleSearchPageShouldWorkViaStarterSearch() throws Exception {
        withSession("system:role:manage");
        mockMvc.perform(get("/internal/roles").with(bearer("role-token"))
                        .param("code", "SUPER").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/internal/roles/permissions").with(bearer("role-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(9));
    }

    @Test
    void dictSaveShouldRequireDictManagePermission() throws Exception {
        withSession("system:account:manage");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/internal/system/dict").with(bearer("account-token"))
                        .contentType("application/json")
                        .content("{\"type\":\"TEST\",\"code\":\"k\",\"label\":\"v\"}"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(1003));
    }

    /** 让 {@code TokenService.resolveWithRenew} 对任意 token 返回指定权限集的会话。 */
    private void withSession(String... permissions) {
        SessionInfo session = new SessionInfo();
        session.setAccountId(1L);
        session.setAccountNo("admin");
        session.setAccountType("OPERATOR");
        session.setDisplayName("管理员");
        session.setRoles(Collections.emptySet());
        session.setPermissions(new java.util.HashSet<>(Arrays.asList(permissions)));
        when(tokenService.resolveWithRenew(anyString())).thenReturn(session);
    }

    private RequestPostProcessor bearer(String token) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    @SpringBootApplication
    static class TestApp {
    }
}
