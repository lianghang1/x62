package com.scaffold.server.lite.cemlink.client;

import com.linkora.client.UplinkPublisher;
import com.sfi.mall.contract.base.model.MallLinkoraData;

/**
 * linkora 上行窄门面：统一包 MallLinkoraData 信封
 *
 * @author lianghang
 */
public class MallLinkoraClient {

    private final UplinkPublisher publisher;

    public MallLinkoraClient(UplinkPublisher publisher) {
        this.publisher = publisher;
    }

    public String publish(String topic, Object payload, String sequenceKey) {
        MallLinkoraData<Object> data = new MallLinkoraData<>();
        data.setPayload(payload);
        return publisher.publish(topic, data, sequenceKey);
    }

    public String publish(String topic, Object payload) {
        return publish(topic, payload, null);
    }
}
