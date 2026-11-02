package com.scaffold.cemlink.contract;

/**
 * 寄存收费项目（原采集程序 BillType 平移）
 *
 * @author lianghang
 */
public enum DepositBillType {

    寄存费("0001"),
    管理费("0002"),
    续存费("0010");

    private final String code;

    DepositBillType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
