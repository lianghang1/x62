package com.scaffold.common.validation;

import com.scaffold.common.enumeration.IntValueEnum;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 枚举值校验：被标注的参数/字段值必须是实现 {@link IntValueEnum} 的指定枚举内的合法值。
 *
 * <p>接受 int、数字串与枚举名三种入参形态（前端字典传值/调试传值都友好）；
 * null 交由 {@code @NotNull} 按需组合，本注解对 null 放行。</p>
 *
 * <pre>
 * public class CreateForm {
 *     &#64;ValidateEnum(value = OrderStatus.class, message = "订单状态非法")
 *     private Integer status;
 * }
 * </pre>
 *
 * @author lianghang
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidateEnum.List.class)
@Constraint(validatedBy = ValidateEnum.Validator.class)
public @interface ValidateEnum {

    /** 允许的枚举类型（须实现 IntValueEnum） */
    Class<? extends IntValueEnum<?>> value();

    String message() default "取值不在合法枚举范围内";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 同一字段可叠加多个枚举校验（多态取值场景） */
    @Documented
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ValidateEnum[] value();
    }

    /**
     * 校验器：匹配 {@link IntValueEnum#getValue()} 或枚举 name()。
     */
    class Validator implements ConstraintValidator<ValidateEnum, Object> {

        private final Set<Integer> values = new HashSet<>();
        private final Set<String> names = new HashSet<>();

        @Override
        public void initialize(ValidateEnum annotation) {
            Arrays.stream(annotation.value().getEnumConstants()).forEach(e -> {
                values.add(e.getValue());
                names.add(((Enum<?>) e).name());
            });
        }

        @Override
        public boolean isValid(Object input, ConstraintValidatorContext context) {
            if (input == null) {
                return true;
            }
            if (input instanceof Integer) {
                return values.contains(input);
            }
            if (input instanceof Number) {
                return values.contains(((Number) input).intValue());
            }
            String text = input.toString().trim();
            // 数字串按 int 值匹配，其余按枚举名匹配
            try {
                return values.contains(Integer.valueOf(text));
            } catch (NumberFormatException notNumeric) {
                return names.contains(text);
            }
        }
    }
}
