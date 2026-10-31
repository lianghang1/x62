package com.scaffold.starter.test.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构规约：字符串/集合工具 import 唯一性（机器强制，违规构建失败）。
 *
 * <p>背景：字符串/集合判空有四个潜在 import 来源（JDK/Spring/lang3/hutool），
 * IDE 自动导入会随机挑选造成风格漂移。本规约把「统一入口」从口头约定变成构建约束：</p>
 *
 * <ul>
 *   <li>字符串判空/转换 → 只允许 {@code com.scaffold.common.util.StringUtils}（继承 lang3）；</li>
 *   <li>集合判空/运算 → 只允许 {@code com.scaffold.common.util.CollUtils}（组合门面）；</li>
 *   <li>guava 高级结构（BiMap/Multimap 等）不受限，直接 import 原生类；</li>
 *   <li>hutool 领域模块（如 captcha 图像渲染）不受限——本规约只收口「通用工具」。</li>
 * </ul>
 *
 * <p>业务模块接入（一行测试即可）：</p>
 * <pre>{@code
 * class ArchConformanceTest {
 *     @Test
 *     void utilImportRules() {
 *         ScaffoldArchRules.checkMainClasses();
 *     }
 * }</pre>
 *
 * @author lianghang
 */
public final class ScaffoldArchRules {

    /** 门面自身（继承 lang3）不受此规则约束 */
    private static final com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> NOT_STRING_FACADE =
            new com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>("not the StringUtils facade") {
                @Override
                public boolean apply(com.tngtech.archunit.core.domain.JavaClass input) {
                    return !"com.scaffold.common.util.StringUtils".equals(input.getFullName());
                }
            };

    /** 禁止直接 import lang3 StringUtils——必须走 scaffold 门面（自有方法也在门面上） */
    public static final ArchRule NO_DIRECT_LANG3_STRING_UTILS =
            noClasses().that(NOT_STRING_FACADE).should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.apache.commons.lang3.StringUtils")
                    .because("字符串工具统一走 com.scaffold.common.util.StringUtils（继承 lang3 全量方法）");

    /** 禁止 Spring 的 StringUtils——与门面同名异义，是风格漂移的主要来源 */
    public static final ArchRule NO_SPRING_STRING_UTILS =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.util.StringUtils")
                    .because("字符串工具统一走 com.scaffold.common.util.StringUtils");

    /** 禁止 hutool StrUtil——通用字符串工具已由门面覆盖 */
    public static final ArchRule NO_HUTOOL_STR_UTIL =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("cn.hutool.core.util.StrUtil")
                    .because("字符串工具统一走 com.scaffold.common.util.StringUtils；hutool 仅允许领域模块（captcha）");

    /** 禁止 hutool CollUtil/CollectionUtil——集合运算走 CollUtils 或 guava 原生类 */
    public static final ArchRule NO_HUTOOL_COLL_UTIL =
            noClasses().should().dependOnClassesThat()
                    .haveFullyQualifiedName("cn.hutool.core.util.StrUtil")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("cn.hutool.core.collection.CollUtil")
                    .orShould().dependOnClassesThat()
                    .haveFullyQualifiedName("cn.hutool.core.collection.CollectionUtil")
                    .because("集合工具统一走 com.scaffold.common.util.CollUtils（组合 guava/Spring）");

    private ScaffoldArchRules() {
    }

    /**
     * 对本模块主代码（target/classes）执行全部规约。
     * 仅扫描主代码——测试类允许直接 import 底座库（写测试用例本身不受门面约束）。
     */
    public static void checkMainClasses() {
        JavaClasses classes = new ClassFileImporter().importPath(
                Paths.get("target", "classes"));
        check(classes);
    }

    /**
     * 对指定包（通常 {@code com.scaffold}）的类路径上全部主类执行规约——
     * 供 scaffold-server 的汇总检查使用：一次扫描覆盖全部模块的 main jar。
     */
    public static void checkPackages(String... packages) {
        JavaClasses classes = new ClassFileImporter().importPackages(packages);
        check(classes);
    }

    private static void check(JavaClasses classes) {
        NO_DIRECT_LANG3_STRING_UTILS.check(classes);
        NO_SPRING_STRING_UTILS.check(classes);
        NO_HUTOOL_STR_UTIL.check(classes);
        NO_HUTOOL_COLL_UTIL.check(classes);
    }
}
