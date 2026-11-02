package com.scaffold.module.cemlink.collect;

import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.module.cemlink.client.model.TombServiceItemData;
import com.scaffold.module.cemlink.increment.WatermarkStore;
import com.scaffold.module.cemlink.source.ServiceItemSourceReader;
import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.Job;
import com.scaffold.starter.job.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务项目数据采集任务：本地时间水位比对 → linkora 上行（至少一次送达）
 *
 * @author lianghang
 */
@Job(name = "服务项目数据采集", cron = "${cemlink.collect.service-item-cron:0 7/20 * * * ?}",
        enabled = "${cemlink.collect.service-item-enabled:true}", lockLeaseSeconds = 0)
public class ServiceItemCollectionJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(ServiceItemCollectionJob.class);

    private static final String DOMAIN = "tombServiceItem";

    private final ServiceItemSourceReader reader;
    private final CemeteryLinkoraClient client;
    private final WatermarkStore watermark;
    /** 初始水位：该时间之前的存量数据不采集，null 则首轮全量 */
    private final Date initialWatermark;

    public ServiceItemCollectionJob(ServiceItemSourceReader reader,
                                    CemeteryLinkoraClient client,
                                    WatermarkStore watermark,
                                    Date initialWatermark) {
        this.reader = reader;
        this.client = client;
        this.watermark = watermark;
        this.initialWatermark = initialWatermark;
    }

    @Override
    protected void doExecute(JobContext context) {
        List<ServiceItemSourceReader.ServiceItemRow> rows = reader.readServiceItems(initialWatermark);
        if (rows.isEmpty()) {
            log.info("[cemlink] 服务项目数据无增量");
            return;
        }
        Date operateTime = new Date();
        List<TombServiceItemData> batch = new ArrayList<>();
        for (ServiceItemSourceReader.ServiceItemRow row : rows) {
            if (watermark.needPush(DOMAIN, row.getItemId(), row.getModifyDate())) {
                batch.add(row.getData());
                watermark.markPushed(DOMAIN, row.getItemId(), operateTime);
            }
        }
        if (!batch.isEmpty()) {
            client.uploadServiceItems(batch);
        }
        log.info("[cemlink] 服务项目数据采集完成，本轮推送 {} 个服务项目", batch.size());
    }
}
