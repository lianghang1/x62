package com.scaffold.server.lite.cemlink.collect;

import com.scaffold.server.lite.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.server.lite.cemlink.config.CemlinkProperties;
import com.scaffold.server.lite.cemlink.increment.WatermarkStore;
import com.scaffold.server.lite.cemlink.source.TombSourceReader;
import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.Job;
import com.scaffold.starter.job.JobContext;
import com.sfi.mall.contract.base.model.cemetery.TransportTomb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 墓穴数据采集任务：分页读取 → 水位比对 → linkora 上行（至少一次送达）
 *
 * @author lianghang
 */
@Job(name = "墓穴数据采集", cron = "${cemlink.collect.tomb-cron:0 0/10 * * * ?}",
        enabled = "${cemlink.collect.tomb-enabled:true}", lockLeaseSeconds = 0)
public class TombDataCollectionJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(TombDataCollectionJob.class);

    private static final String DOMAIN = "tomb";

    private final TombSourceReader reader;
    private final CemeteryLinkoraClient client;
    private final WatermarkStore watermark;
    private final CemlinkProperties properties;

    public TombDataCollectionJob(TombSourceReader reader,
                                 CemeteryLinkoraClient client,
                                 WatermarkStore watermark,
                                 CemlinkProperties properties) {
        this.reader = reader;
        this.client = client;
        this.watermark = watermark;
        this.properties = properties;
    }

    @Override
    protected void doExecute(JobContext context) {
        int pageSize = properties.getCollect().getPageSize();
        int page = 0;
        int pushed = 0;
        while (true) {
            List<TombSourceReader.TombRow> rows = reader.readPage(page++, pageSize);
            if (rows.isEmpty()) {
                break;
            }
            List<TombSourceReader.TombRow> pending = new ArrayList<>();
            for (TombSourceReader.TombRow row : rows) {
                if (watermark.needPush(DOMAIN, row.getBizId(), row.getCollectVersion())) {
                    pending.add(row);
                }
            }
            if (!pending.isEmpty()) {
                List<TransportTomb> batch = new ArrayList<>(pending.size());
                for (TombSourceReader.TombRow row : pending) {
                    batch.add(row.getTomb());
                }
                client.uploadTombs(batch);
                for (TombSourceReader.TombRow row : pending) {
                    watermark.markPushed(DOMAIN, row.getBizId(), row.getCollectVersion());
                }
                pushed += pending.size();
            }
        }
        log.info("[cemlink] 墓穴数据采集完成，本轮推送 {} 条", pushed);
    }
}
