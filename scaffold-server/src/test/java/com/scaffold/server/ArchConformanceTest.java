package com.scaffold.server;

import com.scaffold.starter.test.arch.ScaffoldArchRules;
import org.junit.jupiter.api.Test;

/**
 * 全模块架构规约汇总检查。
 *
 * <p>scaffold-server 的 test classpath 覆盖全部模块的 main jar——
 * 在这里扫一次 {@code com.scaffold} 包，等价于管住全部 26 个模块的主代码：
 * 字符串/集合工具 import 唯一性（StringUtils/CollUtils 门面）违规即构建失败。
 * 业务模块无需各自接入。</p>
 *
 * @author lianghang
 */
class ArchConformanceTest {

    @Test
    void allModulesFollowUtilFacadeRules() {
        ScaffoldArchRules.checkPackages("com.scaffold");
    }
}
