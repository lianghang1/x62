package com.scaffold.module.cemlink.park;

/**
 * 默认墓区园区策略：无顶层虚拟墓区
 *
 * @author lianghang
 */
public class DefaultBlockParkStrategy implements BlockParkStrategy {

    @Override
    public SuperBlock superBlock() {
        return null;
    }
}
