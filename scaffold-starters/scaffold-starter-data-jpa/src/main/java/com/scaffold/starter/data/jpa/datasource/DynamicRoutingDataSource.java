package com.scaffold.starter.data.jpa.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态路由数据源：按 {@link DataSourceContextHolder#peek()} 的 key 把
 * 获取连接的调用分发到对应的目标数据源。
 *
 * <p>栈空（无 {@link TargetDataSource} 上下文）时回落默认数据源（primary），
 * 行为与不启用动态数据源时完全一致。目标数据源集合与默认库由装配层校验后传入。</p>
 *
 * @author lianghang
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private final String primary;

    public DynamicRoutingDataSource(String primary) {
        this.primary = primary;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = DataSourceContextHolder.peek();
        return key != null ? key : primary;
    }
}
