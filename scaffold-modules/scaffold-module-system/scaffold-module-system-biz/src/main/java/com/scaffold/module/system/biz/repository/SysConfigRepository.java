package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 系统参数配置仓储。
 *
 * <p>按键查询用于缓存未命中回源；动态条件查询供管理端分页。</p>
 *
 * @author lianghang
 */
public interface SysConfigRepository extends JpaRepository<SysConfig, Long>,
        JpaSpecificationExecutor<SysConfig> {

    Optional<SysConfig> findByConfigKeyAndDeletedFalse(String configKey);
}
