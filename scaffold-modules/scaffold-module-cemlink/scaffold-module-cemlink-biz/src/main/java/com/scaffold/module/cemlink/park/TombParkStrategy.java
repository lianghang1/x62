package com.scaffold.module.cemlink.park;

/**
 * 园区定制策略：园区查询/数据差异的覆盖点，按 park-code 提供不同实现
 *
 * @author lianghang
 */
public interface TombParkStrategy {

    String cleanTombPath(String tombPath);

    String cleanLayerCode(String layerCode);
}
