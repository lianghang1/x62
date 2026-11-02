package com.scaffold.module.cemlink.park;

/**
 * 默认园区策略：路径/编号统一去除「卫家角」前缀（沿用原采集程序规则）
 *
 * @author lianghang
 */
public class DefaultTombParkStrategy implements TombParkStrategy {

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
