package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.SysDict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 字典项仓储。
 *
 * <p>查询结果按 sort 升序排列以固化展示顺序；「启用项」供业务翻译，
 * 「全量项」供管理端维护，两者通过方法名显式区分。</p>
 *
 * @author lianghang
 */
public interface SysDictRepository extends JpaRepository<SysDict, Long> {

    /** 查询某类型下全部启用的未删除字典项，按 sort 升序。 */
    List<SysDict> findByTypeAndEnabledTrueAndDeletedFalseOrderBySortAsc(String type);

    /** 查询某类型下全部未删除字典项（含停用），按 sort 升序。 */
    List<SysDict> findByTypeAndDeletedFalseOrderBySortAsc(String type);

    /** 精确匹配类型与值的未删除字典项，用于合法性校验。 */
    java.util.Optional<SysDict> findByTypeAndValueAndDeletedFalse(String type, String value);
}
