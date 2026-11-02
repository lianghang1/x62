package com.scaffold.module.cemlink.contract;

/**
 * 微商订单类型（原采集程序 OrderType 平移）
 *
 * @author lianghang
 */
public enum OrderType {

    商城购物("1"),
    在线报修("2"),
    维护费("3"),
    预约看墓("4"),
    预约落葬("5"),
    代客祭扫("6"),
    盆花租摆("7"),
    寄存续费("9"),
    在线售墓("17"),
    描碑("26"),
    深度保洁("27"),
    寄存预约("30"),
    寄存祭扫("32"),
    提交照片("80");

    private final String code;

    OrderType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
