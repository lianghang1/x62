package com.scaffold.common.util;

import com.google.common.base.CaseFormat;

/**
 * 字符串工具唯一入口：继承 commons-lang3 的 {@code org.apache.commons.lang3.StringUtils}
 * （全量 250+ 静态方法穿透可用），仅追加 lang3/guava 都没有的脚手架自有语义。
 *
 * <p>为什么继承而不是直接用 lang3：统一 import 入口——团队判空/取默认值/驼峰转换
 * 只 import 本类一个来源，杜绝「同一件事四个 import」（JDK/Spring/lang3/hutool 各来一个）
 * 的风格漂移。import 唯一性由 ArchUnit 规则强制（见 scaffold-starter-test 的
 * {@code ScaffoldArchRules}），违规构建失败。</p>
 *
 * <p>已知代价（接受）：IDE 点进静态方法跳转的是 lang3 源码，类名显示为本类；
 * 本类与 lang3 同名，跨项目 review 时注意 import 来源——但 ArchUnit 保证脚手架内
 * 只存在本类这一个来源，歧义已被消灭。</p>
 *
 * <p>自有方法准则：只放「lang3/guava 都没有且脚手架内高频」的方法；
 * 纯转发 lang3 已有能力的一律不加。</p>
 *
 * @author lianghang
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    private StringUtils() {
        // 静态工具类：lang3 的设计允许子类化以统一入口，但本类不可实例化
    }

    /** 驼峰 → 下划线（userName → user_name），委托 guava CaseFormat */
    public static String camelToSnake(String text) {
        return text == null ? null
                : CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, text);
    }

    /** 下划线 → 驼峰（user_name → userName），委托 guava CaseFormat */
    public static String snakeToCamel(String text) {
        return text == null ? null
                : CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, text);
    }
}
