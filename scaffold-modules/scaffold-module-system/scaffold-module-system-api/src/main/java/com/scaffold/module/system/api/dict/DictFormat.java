package com.scaffold.module.system.api.dict;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典翻译注解：DTO 字段标注后，JSON 序列化时自动把存储值翻译为字典显示文本。
 *
 * <p>典型用法——订单实体/出参 DTO 里的状态码字段：</p>
 * <pre>{@code
 * public class OrderVO {
 *     @DictFormat("order_status")
 *     private String status;          // 序列化输出 {"status": "待支付"}
 * }
 * }</pre>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>只影响序列化出参，不影响反序列化入参——入参仍提交存储值，由 {@code DictApi#validate} 校验；</li>
 *   <li>翻译经 {@link DictFormatSerializer} 委托 {@code DictApi}，未命中返回原值、字典服务异常也回退原值——
 *       翻译是锦上添花，绝不拖垮接口；</li>
 *   <li>需要同时输出原始值时，DTO 另加一个不带本注解的字段。</li>
 * </ul>
 *
 * @author lianghang
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DictFormatSerializer.class)
public @interface DictFormat {

    /** 字典类型，如 {@code order_status} */
    String value();
}
