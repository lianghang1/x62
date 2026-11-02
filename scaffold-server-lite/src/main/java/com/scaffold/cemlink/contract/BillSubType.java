package com.scaffold.cemlink.contract;

/**
 * 业务子类型（原采集程序 BillSubType 平移）
 *
 * @author lianghang
 */
public enum BillSubType {

    盆花租摆("1"),
    绿化报修("2"),
    绿化改建("3"),
    墓穴改建("6"),
    墓穴报修("7"),
    描碑("9"),
    补办墓穴证("10"),
    鲜花服务("11"),
    代客祭扫("12"),
    照片验收("60");

    private final String code;

    BillSubType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
