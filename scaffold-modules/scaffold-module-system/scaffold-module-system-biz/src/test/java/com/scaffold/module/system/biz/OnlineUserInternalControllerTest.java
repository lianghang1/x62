package com.scaffold.module.system.biz;

import com.scaffold.starter.security.token.SessionInfo;
import com.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 在线用户管理端到端测试：会话由 Redis 承载、这里 mock {@code TokenService}，
 * 重点验证权限三态（401/403/放行）、关键字过滤与踢人动作真实下发。
 *
 * @author lianghang
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "scaffold.config.refresh-listener-enabled=false")
@AutoConfigureMockMvc
class OnlineUserInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenService tokenService;

    @AfterEach
    void tearDown() {
        com.scaffold.starter.security.identity.LoginContext.clear();
    }

    @Test
    void listShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/internal/system/online"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void listShouldReturn403WithoutQueryPermission() throws Exception {
        withSession("system:dict:manage");
        mockMvc.perform(get("/internal/system/online").with(bearer("limited-token")))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void listShouldReturnSessionsWithKeywordFilter() throws Exception {
        Map<String, SessionInfo> sessions = new LinkedHashMap<>();
        sessions.put("token-alice", session("alice", "张三"));
        sessions.put("token-bob", session("bob", "李四"));
        when(tokenService.listAllOnlineSessions()).thenReturn(sessions);

        withSession("system:online:query");
        // 全量
        mockMvc.perform(get("/internal/system/online").with(bearer("admin-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].token").value("token-alice"))
                .andExpect(jsonPath("$.data.list[0].displayName").value("张三"));
        // 关键字过滤
        mockMvc.perform(get("/internal/system/online").with(bearer("admin-token"))
                        .param("keyword", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].accountNo").value("bob"));
    }

    @Test
    void kickShouldRequireManagePermission() throws Exception {
        withSession("system:online:query");
        mockMvc.perform(post("/internal/system/online/kick").with(bearer("readonly-token"))
                        .param("token", "token-alice"))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(1003));
        Mockito.verify(tokenService, Mockito.never()).revoke(anyString());
    }

    @Test
    void kickShouldRevokeSessionWithManagePermission() throws Exception {
        withSession("system:online:manage");
        mockMvc.perform(post("/internal/system/online/kick").with(bearer("admin-token"))
                        .param("token", "token-alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(tokenService).revoke("token-alice");
    }

    private SessionInfo session(String accountNo, String displayName) {
        SessionInfo info = new SessionInfo();
        info.setAccountId(accountNo.hashCode() & 0xffffL);
        info.setAccountNo(accountNo);
        info.setAccountType("OPERATOR");
        info.setDisplayName(displayName);
        info.setRoles(Collections.emptySet());
        info.setPermissions(Collections.emptySet());
        info.setDevice("PC");
        info.setLoginTime(System.currentTimeMillis());
        return info;
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
