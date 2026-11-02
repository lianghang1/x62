package com.scaffold.module.cemlink.park;

/**
 * 默认墓区园区策略：顶层虚拟墓区沿用原采集程序硬编码（zqqy 基线，后续园区配置化）
 *
 * @author lianghang
 */
public class DefaultBlockParkStrategy implements BlockParkStrategy {

    @Override
    public SuperBlock superBlock() {
        return new SuperBlock("20967", "卫家角", "卫家角", "0", 0, 0);
    }
}
