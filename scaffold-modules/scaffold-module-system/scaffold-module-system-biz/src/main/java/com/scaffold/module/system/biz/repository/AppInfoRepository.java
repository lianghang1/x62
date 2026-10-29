package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {

    Optional<AppInfo> findByAppKeyAndDeletedFalse(String appKey);

    Optional<AppInfo> findByAccountIdAndDeletedFalse(Long accountId);

    Optional<AppInfo> findByIdAndDeletedFalse(Long id);
}
