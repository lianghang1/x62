package com.scaffold.cemlink.park;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 卫家角园区策略：路径/编号统一去除「卫家角」前缀（沿用原采集程序规则）
 *
 * <p>激活条件写在类上：{@code cemlink.park-code=weijiaojiao} 时由 {@code CemlinkConfig} 导入，
 * 未命中则走 {@code DefaultTombParkStrategy} 兜底</p>
 *
 * @author lianghang
 */
@Component
@ConditionalOnProperty(name = "cemlink.park-code", havingValue = "weijiaojiao")
public class WeijiajiaoTombParkStrategy implements TombParkStrategy {

    private static final String PREFIX = "卫家角";

    @Override
    public String cleanTombPath(String tombPath) {
        return tombPath == null ? null : tombPath.replace(PREFIX, "");
    }

    @Override
    public String cleanLayerCode(String layerCode) {
        return layerCode == null ? null : layerCode.replace(PREFIX, "");
    }
}
