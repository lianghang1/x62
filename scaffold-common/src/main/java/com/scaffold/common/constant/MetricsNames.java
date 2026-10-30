package com.scaffold.common.constant;

/**
 * 框架内建指标名契约（Micrometer 点分命名，Prometheus 抓取后自动转下划线）。
 *
 * <p>埋点在各自 starter（optional micrometer，缺席静默），但指标名是跨模块
 * 契约——Grafana 面板、告警规则都按名取数，统一在此收口防止拼写漂移。</p>
 *
 * @author lianghang
 */
public final class MetricsNames {

    /** 任务执行次数（tags: job, outcome=success/fail）—— starter-job */
    public static final String JOB_EXECUTIONS = "scaffold.job.executions";

    /** 任务执行耗时（tags: job）—— starter-job */
    public static final String JOB_DURATION = "scaffold.job.duration";

    /** 防刷拦截次数（tags: scene, rule=interval/window/daily）—— starter-protection */
    public static final String ABUSE_REJECTED = "scaffold.protection.abuse.rejected";

    /** 登录失败次数（tags: dimension=account/ip）—— starter-security */
    public static final String LOGIN_FAILURES = "scaffold.security.login.failures";

    private MetricsNames() {
    }
}
