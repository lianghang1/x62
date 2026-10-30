package com.scaffold.server.config;

import com.scaffold.module.system.biz.entity.AccountRole;
import com.scaffold.module.system.biz.entity.SysAccount;
import com.scaffold.module.system.biz.entity.SysRole;
import com.scaffold.module.system.biz.repository.AccountRoleRepository;
import com.scaffold.module.system.biz.repository.SysAccountRepository;
import com.scaffold.module.system.biz.repository.SysRoleRepository;
import com.scaffold.module.system.biz.service.OperatorUserDetailsService;
import com.scaffold.starter.security.identity.AccountType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * dev 环境种子数据初始化器：内存库无 Liquibase 时自动创建开箱管理员。
 *
 * <p>dev 使用 H2 内存库且默认关闭 Liquibase，应用每次启动都是空库，
 * 新同事 clone 下来无法登录——本初始化器在 admin 不存在时自动补齐
 * {@code admin/admin123} + 超级管理员角色 + 绑定关系，仅用于本地联调。</p>
 *
 * <p>生效条件（同时满足）：dev profile + Liquibase 关闭 + admin 不存在。
 * 生产/信创环境建表走 Liquibase changeset（03-seed-data），本类不参与。</p>
 *
 * @author lianghang
 */
@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final SysAccountRepository accountRepository;
    private final SysRoleRepository roleRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final OperatorUserDetailsService userDetailsService;
    private final Environment environment;

    public DevDataInitializer(SysAccountRepository accountRepository,
                              SysRoleRepository roleRepository,
                              AccountRoleRepository accountRoleRepository,
                              OperatorUserDetailsService userDetailsService,
                              Environment environment) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.accountRoleRepository = accountRoleRepository;
        this.userDetailsService = userDetailsService;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (Boolean.parseBoolean(environment.getProperty("spring.liquibase.enabled"))) {
            // Liquibase 管种子（03-seed-data changeset），跳过
            return;
        }
        if (accountRepository.findByAccountNoAndDeletedFalse("admin").isPresent()) {
            return;
        }
        SysAccount admin = new SysAccount();
        admin.setAccountNo("admin");
        admin.setType(AccountType.OPERATOR);
        admin.setStatus("ENABLE");
        admin.setPassword(userDetailsService.encode("admin123"));
        accountRepository.save(admin);

        SysRole superAdmin = roleRepository.findByCodeAndDeletedFalse("SUPER_ADMIN").orElseGet(() -> {
            SysRole role = new SysRole();
            role.setCode("SUPER_ADMIN");
            role.setName("超级管理员");
            role.setPermissions("*");
            return roleRepository.save(role);
        });
        AccountRole binding = new AccountRole();
        binding.setAccountId(admin.getId());
        binding.setRoleId(superAdmin.getId());
        accountRoleRepository.saveAll(Collections.singletonList(binding));

        log.warn("[dev-seed] 已创建开箱管理员 admin/admin123（仅本地联调用，生产环境不存在本初始化器）");
    }
}
