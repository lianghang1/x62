package com.scaffold.module.system.biz.repository;

import com.scaffold.module.system.biz.entity.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 账户-角色关联仓储。
 *
 * <p>授权采用「先删后插」的整体替换式写法：查旧绑定用于展示，
 * 删旧绑定用于重建，天然避免绑定漂移。</p>
 *
 * @author lianghang
 */
public interface AccountRoleRepository extends JpaRepository<AccountRole, Long> {

    /** 查询账户的全部角色绑定。 */
    List<AccountRole> findByAccountId(Long accountId);

    /** 删除账户的全部角色绑定。 */
    void deleteByAccountId(Long accountId);
}
