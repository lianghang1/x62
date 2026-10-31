package com.scaffold.common;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * common 自身架构规约：门面模块也不得绕过 StringUtils/CollUtils 直连底座。
 *
 * <p>仅扫描本模块主代码（target/classes）——测试类不受约束。</p>
 *
 * @author lianghang
 */
class ArchConformanceTest {

    @Test
    void commonFollowsUtilFacadeRules() {
        com.tngtech.archunit.core.domain.JavaClasses classes =
                new ClassFileImporter().importPath(Paths.get("target", "classes"));

        // 门面自身（继承 lang3）不受此规则约束
        com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> notFacade =
                new com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>("not the StringUtils facade") {
                    @Override
                    public boolean apply(com.tngtech.archunit.core.domain.JavaClass input) {
                        return !"com.scaffold.common.util.StringUtils".equals(input.getFullName());
                    }
                };
        ArchRule noLang3 = noClasses().that(notFacade).should().dependOnClassesThat()
                .haveFullyQualifiedName("org.apache.commons.lang3.StringUtils")
                .because("统一走 com.scaffold.common.util.StringUtils 门面（门面自身除外）");
        ArchRule noSpringStr = noClasses().should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.util.StringUtils")
                .because("字符串判空统一走门面");
        ArchRule noHutoolCore = noClasses().should().dependOnClassesThat()
                .haveFullyQualifiedName("cn.hutool.core.util.StrUtil")
                .orShould().dependOnClassesThat()
                .haveFullyQualifiedName("cn.hutool.core.collection.CollUtil")
                .because("hutool 仅允许领域模块（captcha），通用工具走门面");

        noLang3.check(classes);
        noSpringStr.check(classes);
        noHutoolCore.check(classes);
    }
}
