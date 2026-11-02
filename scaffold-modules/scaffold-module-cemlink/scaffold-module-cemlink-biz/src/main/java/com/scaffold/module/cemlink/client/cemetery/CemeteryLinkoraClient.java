package com.scaffold.module.cemlink.client.cemetery;

import com.scaffold.module.cemlink.client.CemlinkTopics;
import com.scaffold.module.cemlink.client.MallLinkoraClient;
import com.scaffold.module.cemlink.client.model.TombBlockData;
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
}
