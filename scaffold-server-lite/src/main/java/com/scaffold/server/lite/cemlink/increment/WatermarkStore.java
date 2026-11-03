package com.scaffold.server.lite.cemlink.increment;

import com.scaffold.server.lite.cemlink.entity.WatermarkEntity;
import com.scaffold.server.lite.cemlink.repository.WatermarkRepository;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 水位线门面（主库 cemlink_watermark 表）：按 (域, 业务ID) 记录已推送版本
 *
 * @author lianghang
 */
@Component
public class WatermarkStore {

    private final WatermarkRepository repository;

    public WatermarkStore(WatermarkRepository repository) {
        this.repository = repository;
    }

    /** 版本大于已推送版本（或未推送过）才需要推 */
    public boolean needPush(String domain, String bizId, int version) {
        return needPush(domain, bizId, Long.valueOf(version));
    }

    /** 采集时间晚于已推送时间（或未推送过）才需要推，对应原程序 COLLECTION_TIME 水位 */
    public boolean needPush(String domain, String bizId, Date collectedAt) {
        return needPush(domain, bizId, collectedAt == null ? 0L : collectedAt.getTime());
    }

    public void markPushed(String domain, String bizId, int version) {
        markPushed(domain, bizId, Long.valueOf(version));
    }

    public void markPushed(String domain, String bizId, Date collectedAt) {
        markPushed(domain, bizId, collectedAt == null ? 0L : collectedAt.getTime());
    }

    private boolean needPush(String domain, String bizId, long value) {
        return repository.findByDomainAndBizId(domain, bizId)
                .map(WatermarkEntity::getVersion)
                .map(pushed -> value > pushed)
                .orElse(true);
    }

    private void markPushed(String domain, String bizId, long value) {
        WatermarkEntity entity = repository.findByDomainAndBizId(domain, bizId)
                .orElseGet(() -> {
                    WatermarkEntity created = new WatermarkEntity();
                    created.setDomain(domain);
                    created.setBizId(bizId);
                    return created;
                });
        entity.setVersion(value);
        repository.save(entity);
    }
}
