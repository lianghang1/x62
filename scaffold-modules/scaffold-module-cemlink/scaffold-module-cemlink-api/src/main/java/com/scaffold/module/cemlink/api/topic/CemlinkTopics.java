package com.scaffold.module.cemlink.api.topic;

/**
 * 墓园数据链路上行 topic 目录：采集端 → ERP 服务端。
 *
 * <p>{@code TOMB} 与 ERP 侧 contract-base 的 {@code CemeteryTopic.Uplink.TOMB} 取值一致
 * （服务端已实现消费）；其余 topic 为采集端预先定义，ERP 侧契约完善后切换为
 * 依赖其 contract-base 常量，取值不变即无需两端同步发版。</p>
 *
 * <p>命名规约：{@code cemetery-{业务域}}，与 ERP 侧既有命名保持同构。</p>
 *
 * @author lianghang
 */
public final class CemlinkTopics {

    /** ERP 服务端已实现：收集已售墓信息（payload = List&lt;TransportTomb&gt;） */
    public static final String TOMB = "cemetery-tomb";

    /** 预定义：墓区信息（payload = List&lt;TransportTombBlock&gt;） */
    public static final String TOMB_BLOCK = "cemetery-tomb-block";

    /** 预定义：订单信息（payload = List&lt;TransportTombOrder&gt;） */
    public static final String TOMB_ORDER = "cemetery-tomb-order";

    /** 预定义：工单信息（payload = List&lt;TransportTombWork&gt;） */
    public static final String TOMB_WORK = "cemetery-tomb-work";

    /** 预定义：服务项目信息（payload = List&lt;TransportTombServiceItem&gt;） */
    public static final String TOMB_SERVICE_ITEM = "cemetery-tomb-service-item";

    /** 预定义：骨灰寄存信息（payload = List&lt;TransportTombAshesDeposit&gt;） */
    public static final String TOMB_ASHES_DEPOSIT = "cemetery-tomb-ashes-deposit";

    private CemlinkTopics() {
    }
}
