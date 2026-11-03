package com.scaffold.server.lite.cemlink.client.cemetery;

import com.scaffold.server.lite.cemlink.client.CemlinkTopics;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.DepositData;
import com.scaffold.server.lite.cemlink.client.model.ServiceItemData;
import com.scaffold.server.lite.cemlink.client.model.TombBlockData;
import com.scaffold.server.lite.cemlink.client.model.TombServiceItemData;
import com.scaffold.server.lite.cemlink.client.model.ServiceOrderUpdateData;
import com.sfi.mall.contract.base.model.cemetery.TransportTomb;
import com.sfi.mall.contract.base.topic.cemetery.CemeteryTopic;

import java.util.List;

/**
 * 公墓域上行客户端（tomb/block 等各域独立封装，此为 tomb 域）
 *
 * @author lianghang
 */
public class CemeteryLinkoraClient {

    private final MallLinkoraClient mallLinkoraClient;

    public CemeteryLinkoraClient(MallLinkoraClient mallLinkoraClient) {
        this.mallLinkoraClient = mallLinkoraClient;
    }

    public String uploadTombs(List<TransportTomb> data) {
        return mallLinkoraClient.publish(CemeteryTopic.Uplink.TOMB, data);
    }

    public String uploadBlocks(List<TombBlockData> data) {
        return mallLinkoraClient.publish(CemlinkTopics.UPLINK_TOMB_BLOCK, data);
    }

    public String uploadServiceItems(List<TombServiceItemData> data) {
        return mallLinkoraClient.publish(CemlinkTopics.UPLINK_TOMB_SERVICE_ITEM, data);
    }

    /** 回传微商服务项目编码（原 CMD dataCollection/tomb/service/update 语义） */
    public String uploadServiceItemCodes(List<TombServiceItemData> data) {
        return mallLinkoraClient.publish(CemlinkTopics.UPLINK_SERVICE_ITEM_UPDATE, data);
    }

    public String uploadDeposits(List<DepositData> data) {
        return mallLinkoraClient.publish(CemlinkTopics.UPLINK_TOMB_ASHES_DEPOSIT, data);
    }

    /** 回传订单同步状态（原 CMD dataCollection/tomb/order/update 语义） */
    public String uploadOrderUpdates(List<ServiceOrderUpdateData> data) {
        return mallLinkoraClient.publish(CemlinkTopics.UPLINK_ORDER_UPDATE, data);
    }
}
