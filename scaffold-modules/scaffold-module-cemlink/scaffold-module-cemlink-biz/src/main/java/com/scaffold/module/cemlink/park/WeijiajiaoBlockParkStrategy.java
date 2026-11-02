package com.scaffold.module.cemlink.park;

/**
 * 卫家角墓区园区策略：顶层虚拟墓区沿用原采集程序硬编码
 *
 * @author lianghang
 */
public class WeijiajiaoBlockParkStrategy implements BlockParkStrategy {

    @Override
    public SuperBlock superBlock() {
        return new SuperBlock("20967", "卫家角", "卫家角", "0", 0, 0);
    }
}
