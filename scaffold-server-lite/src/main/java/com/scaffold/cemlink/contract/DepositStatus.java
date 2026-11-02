package com.scaffold.cemlink.contract;

/**
 * 电商骨灰寄存状态（原采集程序 DepositStatus 平移）
 *
 * @author lianghang
 */
public enum DepositStatus {

    无效("0"),
    已存("1"),
    已退("2"),
    预订("3");

    private final String code;

    DepositStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
