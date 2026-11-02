package com.scaffold.cemlink.park;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 卫家角墓区园区策略：顶层虚拟墓区沿用原采集程序硬编码
 *
 * <p>激活条件写在类上：{@code cemlink.park-code=weijiaojiao} 时由 {@code CemlinkConfig} 导入，
 * 未命中则走 {@code DefaultBlockParkStrategy} 兜底</p>
 *
 * @author lianghang
 */
@Component
@ConditionalOnProperty(name = "cemlink.park-code", havingValue = "weijiaojiao")
public class WeijiajiaoBlockParkStrategy implements BlockParkStrategy {

    @Override
    public SuperBlock superBlock() {
        return new SuperBlock("20967", "卫家角", "卫家角", "0", 0, 0);
    }
}
