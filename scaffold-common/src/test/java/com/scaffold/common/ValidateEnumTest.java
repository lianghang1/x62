package com.scaffold.common;

import com.scaffold.common.enumeration.IntValueEnum;
import com.scaffold.common.validation.ValidateEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ValidateEnum 校验器测试：int/数字串/枚举名/null 四形态。
 *
 * @author lianghang
 */
class ValidateEnumTest {

    /** 测试用值枚举 */
    public enum Status implements IntValueEnum<Status> {
        ENABLED(1, "启用"),
        DISABLED(0, "停用");

        private final int value;
        private final String label;

        Status(int value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public int getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    /** 承载注解的测试表单：从真实字段反射取注解（注解不可匿名实例化） */
    static class Form {
        @ValidateEnum(value = Status.class, message = "非法")
        public Integer status;
    }

    private final ValidateEnum.Validator validator = new ValidateEnum.Validator();
    private final ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class);

    private ValidateEnum annotation() throws Exception {
        return Form.class.getDeclaredField("status").getAnnotation(ValidateEnum.class);
    }

    @Test
    void acceptsValidForms() throws Exception {
        validator.initialize(annotation());
        assertTrue(validator.isValid(null, context));          // null 放行
        assertTrue(validator.isValid(1, context));             // int 值
        assertTrue(validator.isValid(0L, context));            // 其他 Number
        assertTrue(validator.isValid("1", context));           // 数字串
        assertTrue(validator.isValid("ENABLED", context));     // 枚举名
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        validator.initialize(annotation());
        assertFalse(validator.isValid(99, context));
        assertFalse(validator.isValid("abc", context));
        assertFalse(validator.isValid("enabled", context));    // 大小写敏感
    }
}
