package com.scaffold.cemlink.contract;

/**
 * 寄存业务状态（原采集程序 AshesBusinessStatus 平移）
 *
 * @author lianghang
 */
public enum AshesBusinessStatus {

    登记("1"),
    存入("2"),
    部分存入("3"),
    退款登记("4"),
    保留("5"),
    转运中("6"),
    已领出("9");

    private final String code;

    AshesBusinessStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
