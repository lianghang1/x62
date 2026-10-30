package com.scaffold.module.system.biz.convert;

import com.scaffold.module.system.api.dto.AccountDTO;
import com.scaffold.module.system.biz.entity.AppInfo;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;

/**
 * 账户实体与 DTO 的转换器。
 *
 * <p>手写显式转换而非 MapStruct / BeanUtils 反射拷贝——字段增删
 * 在编译期与评审中一目了然；展示名按「昵称优先、应用名次之、
 * 账户编号兜底」的顺序推导。</p>
 *
 * @author lianghang
 */
public final class AccountConverter {

    private AccountConverter() {
    }

    /**
     * 聚合主账户与扩展信息，转换为统一账户 DTO。
     *
     * @param account 主账户，可为 {@code null}
     * @param member  会员扩展信息，可为 {@code null}
     * @param app     应用扩展信息，可为 {@code null}
     * @return 账户 DTO；account 为 {@code null} 时返回 {@code null}
     */
    public static AccountDTO toDTO(SysAccount account, MemberInfo member, AppInfo app) {
        if (account == null) {
            return null;
        }
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setAccountNo(account.getAccountNo());
        dto.setType(account.getType() == null ? null : account.getType().name());
        dto.setStatus(account.getStatus());
        String display = account.getAccountNo();
        if (member != null && member.getNickname() != null) {
            display = member.getNickname();
        } else if (app != null) {
            display = app.getAppName();
        }
        dto.setDisplayName(display);
        return dto;
    }
}
