package com.scaffold.starter.security.policy;

import com.scaffold.core.exception.BizException;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.starter.security.config.SecurityProperties;

/**
 * 密码强度校验（创建账号/修改密码时调用）：
 * 最小长度 + 必须同时包含字母和数字（scaffold.security.password-policy 可调）。
 */
public class PasswordPolicyValidator {

    private final SecurityProperties.PasswordPolicy policy;

    public PasswordPolicyValidator(SecurityProperties.PasswordPolicy policy) {
        this.policy = policy;
    }

    /** 校验不通过抛 BizException（code=400），通过则静默返回 */
    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < policy.getMinLength()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                    "密码长度至少 " + policy.getMinLength() + " 位");
        }
        if (policy.isRequireLetterAndDigit()) {
            boolean hasLetter = false;
            boolean hasDigit = false;
            for (char c : rawPassword.toCharArray()) {
                if (Character.isLetter(c)) {
                    hasLetter = true;
                }
                if (Character.isDigit(c)) {
                    hasDigit = true;
                }
            }
            if (!hasLetter || !hasDigit) {
                throw BizException.of(GlobalErrorCode.BAD_REQUEST, "密码必须同时包含字母和数字");
            }
        }
    }
}
