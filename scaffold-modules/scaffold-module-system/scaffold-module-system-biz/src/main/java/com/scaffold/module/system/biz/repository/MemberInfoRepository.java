package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.MemberInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberInfoRepository extends JpaRepository<MemberInfo, Long> {

    Optional<MemberInfo> findByOpenIdAndDeletedFalse(String openId);

    Optional<MemberInfo> findByAccountIdAndDeletedFalse(Long accountId);
}
