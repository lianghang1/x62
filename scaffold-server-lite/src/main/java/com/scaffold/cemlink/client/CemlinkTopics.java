package com.scaffold.cemlink.client;

/**
 * 墓园链路 topic 常量：TOMB 取自 server 契约，其余预定义待 server 契约完善后替换
 *
 * @author lianghang
 */
public final class CemlinkTopics {

    private CemlinkTopics() {
    }

    /** 上行：server 契约已定义 */
    public static final String UPLINK_TOMB_BLOCK = "cemetery-tomb-block";
    public static final String UPLINK_TOMB_ORDER = "cemetery-tomb-order";
    public static final String UPLINK_TOMB_WORK = "cemetery-tomb-work";
    public static final String UPLINK_TOMB_SERVICE_ITEM = "cemetery-tomb-service-item";
    public static final String UPLINK_TOMB_ASHES_DEPOSIT = "cemetery-tomb-ashes-deposit";

    /** 上行：处理结果回传（原 CMD .../update 语义） */
    public static final String UPLINK_WORK_UPDATE = "cemetery-tomb-work-update";
    public static final String UPLINK_SERVICE_ITEM_UPDATE = "cemetery-tomb-service-item-update";
    public static final String UPLINK_DEPOSIT_BIND_UPDATE = "cemetery-tomb-ashes-deposit-bind-update";
    public static final String UPLINK_ORDER_UPDATE = "cemetery-tomb-order-update";

    /** 下行：server 处理结果下发（原 HTTP 同步 response 内 datas 语义） */
    public static final String DOWNLINK_WORK_CHECK = "cemetery-tomb-work-check";
    public static final String DOWNLINK_SERVICE_ITEM = "cemetery-tomb-service-item-push";
    public static final String DOWNLINK_DEPOSIT_BIND = "cemetery-tomb-ashes-deposit-bind-push";
    public static final String DOWNLINK_ORDER = "cemetery-tomb-order-push";
}
