package com.scaffold.starter.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可观测性配置项。
 *
 * <p>endpoint 暴露走 EnvironmentPostProcessor 注入的默认值（health/info/prometheus），
 * 业务方在 application.yml 里写同名 key 即可整体覆盖；本类只管框架自身的
 * 指标行为开关与公共标签。</p>
 *
 * @author lianghang
 */
@ConfigurationProperties(prefix = "scaffold.observability")
public class ObservabilityProperties {

    /** 总开关：false 时框架内建埋点全部静默（端点暴露不受此开关控制） */
    private boolean enabled = true;

    /**
     * 公共标签 application 的取值；空则取 spring.application.name，再兜底 scaffold。
     * Prometheus 多实例聚合时靠该标签区分应用。
     */
    private String applicationName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
