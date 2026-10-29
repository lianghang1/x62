package com.scaffold.module.system.biz.convert;

import com.scaffold.module.system.api.dto.AccountDTO;
import com.scaffold.module.system.biz.entity.AppInfo;
import com.scaffold.module.system.biz.entity.MemberInfo;
import com.scaffold.module.system.biz.entity.SysAccount;

/**
 * 手写显式转换（规约：不用 MapStruct/BeanUtils 反射拷贝——字段增删在编译期与评审中一目了然）。
 */
public final class AccountConverter {

    private AccountConverter() {
    }

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
