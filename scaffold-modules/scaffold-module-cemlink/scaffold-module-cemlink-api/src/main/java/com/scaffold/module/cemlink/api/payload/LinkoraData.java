package com.scaffold.module.cemlink.api.payload;

import java.io.Serializable;

/**
 * linkora 上行数据包裹：与 ERP 侧 {@code MallLinkoraData} 结构对齐（单字段 payload），
 * JSON 字段名一致即可两端互通；ERP 契约稳定后由其 contract-base 替换本类。
 *
 * @author lianghang
 */
public class LinkoraData<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private T payload;

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
