package com.scaffold.module.cemlink.park;

/**
 * 默认园区策略：无个性化处理
 *
 * @author lianghang
 */
public class DefaultTombParkStrategy implements TombParkStrategy {

    @Override
    public String cleanTombPath(String tombPath) {
        return tombPath;
    }

    @Override
    public String cleanLayerCode(String layerCode) {
        return layerCode;
    }
}
