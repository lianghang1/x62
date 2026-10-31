package com.scaffold.starter.security;

import com.scaffold.common.exception.BizException;
import com.scaffold.starter.security.config.SecurityProperties;
import com.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 密码强度校验器测试：长度阈值、字母数字双要求、通过路径。
 *
 * @author lianghang
 */
class PasswordPolicyValidatorTest {

    private final SecurityProperties.PasswordPolicy policy = new SecurityProperties.PasswordPolicy();
    private final PasswordPolicyValidator validator = new PasswordPolicyValidator(policy);

    @Test
    void nullOrTooShortShouldFail() {
        assertThrows(BizException.class, () -> validator.validate(null));
        assertThrows(BizException.class, () -> validator.validate("a1"));
    }

    @Test
    void letterOnlyOrDigitOnlyShouldFail() {
        assertThrows(BizException.class, () -> validator.validate("abcdefgh"));
        assertThrows(BizException.class, () -> validator.validate("12345678"));
    }

    @Test
    void letterPlusDigitShouldPass() {
        assertDoesNotThrow(() -> validator.validate("abcd1234"));
    }

    @Test
    void customMinLengthShouldTakeEffect() {
        policy.setMinLength(4);
        assertDoesNotThrow(() -> validator.validate("a1b2"));
        assertThrows(BizException.class, () -> validator.validate("a1"));
    }
}
