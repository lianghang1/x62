package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysDict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysDictRepository extends JpaRepository<SysDict, Long> {

    List<SysDict> findByTypeAndEnabledTrueAndDeletedFalseOrderBySortAsc(String type);

    List<SysDict> findByTypeAndDeletedFalseOrderBySortAsc(String type);

    java.util.Optional<SysDict> findByTypeAndValueAndDeletedFalse(String type, String value);
}
