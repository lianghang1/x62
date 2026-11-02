package com.scaffold.cemlink.contract;

/**
 * 寄存收费类别（原采集程序 DepositCharge 平移）
 *
 * @author lianghang
 */
public enum DepositCharge {

    按年收费("y"),
    按月收费("m");

    private final String code;

    DepositCharge(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
