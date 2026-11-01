package com.scaffold.starter.security.permission;

import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限校验拦截器三态测试：无注解直通 / 未登录 401 / 已登录无权 403 / 有权放行，
 * 并覆盖段级通配（{@code system:account:*}）、多值 any-of 与超管全量 {@code *}。
 *
 * <p>基于 MockMvc 独立环境、零外部依赖；身份经 {@code LoginContext} 手工注入，
 * 与生产链路（认证过滤器写入）同一取值口。</p>
 *
 * @author lianghang
 */
class PermissionInterceptorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .addInterceptors(new PermissionInterceptor())
                .build();
    }

    @AfterEach
    void tearDown() {
        LoginContext.clear();
    }

    @Test
    void endpointWithoutAnnotationShouldPass() throws Exception {
        mockMvc.perform(get("/demo/open")).andExpect(status().isOk());
    }

    @Test
    void annotatedEndpointShouldReturn401WhenAnonymous() {
        // security starter 不依赖 web 的全局异常处理器：standalone 环境下 BizException
        // 直接抛出，断言错误码语义与生产响应（HTTP 401 / 业务码 1001）一致
        NestedServletException ex = catchThrowableOfType(
                () -> mockMvc.perform(get("/demo/manage")), NestedServletException.class);
        assertThat(ex.getRootCause()).isInstanceOf(com.scaffold.common.exception.BizException.class);
        assertThat(((com.scaffold.common.exception.BizException) ex.getRootCause())
                .getErrorCode().getCode()).isEqualTo(1001);
    }

    @Test
    void annotatedEndpointShouldReturn403WithoutPermission() {
        login("system:dict:manage");
        NestedServletException ex = catchThrowableOfType(
                () -> mockMvc.perform(get("/demo/manage")), NestedServletException.class);
        assertThat(ex.getRootCause()).isInstanceOf(com.scaffold.common.exception.BizException.class);
        assertThat(((com.scaffold.common.exception.BizException) ex.getRootCause())
                .getErrorCode().getCode()).isEqualTo(1003);
        assertThat(ex.getRootCause()).hasMessageContaining("system:account:manage");
    }

    @Test
    void annotatedEndpointShouldPassWithExactPermission() throws Exception {
        login("system:account:manage");
        mockMvc.perform(get("/demo/manage")).andExpect(status().isOk());
    }

    @Test
    void wildcardPermissionShouldMatchSubPath() throws Exception {
        login("system:account:*");
        mockMvc.perform(get("/demo/manage")).andExpect(status().isOk());
    }

    @Test
    void anyOfValuesShouldPassOnFirstMatch() throws Exception {
        login("system:audit:query");
        mockMvc.perform(get("/demo/either")).andExpect(status().isOk());
    }

    @Test
    void superAdminWildcardShouldPassEverything() throws Exception {
        login("*");
        mockMvc.perform(get("/demo/manage")).andExpect(status().isOk());
    }

    private void login(String... permissions) {
        LoginUser user = new LoginUser();
        user.setAccountId(1L);
        user.setAccountNo("admin");
        user.setAccountType(AccountType.OPERATOR);
        user.setDisplayName("管理员");
        user.setPermissions(new java.util.HashSet<>(java.util.Arrays.asList(permissions)));
        LoginContext.set(user);
    }

    @RestController
    static class DemoController {

        @GetMapping("/demo/open")
        public String open() {
            return "open";
        }

        @GetMapping("/demo/manage")
        @RequirePermission("system:account:manage")
        public String manage() {
            return "manage";
        }

        @GetMapping("/demo/either")
        @RequirePermission({"system:account:manage", "system:audit:query"})
        public String either() {
            return "either";
        }
    }
}
