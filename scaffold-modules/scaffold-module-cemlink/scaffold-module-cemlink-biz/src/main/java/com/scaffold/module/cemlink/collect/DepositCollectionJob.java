package com.scaffold.module.cemlink.collect;

import com.scaffold.module.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.module.cemlink.client.model.DepositData;
import com.scaffold.module.cemlink.increment.WatermarkStore;
import com.scaffold.module.cemlink.source.DepositSourceReader;
import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.Job;
import com.scaffold.starter.job.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 寄存业务数据采集任务：本地时间水位比对 → linkora 上行（至少一次送达）
 *
 * @author lianghang
 */
@Job(name = "寄存业务数据采集", cron = "${cemlink.collect.deposit-cron:0 12/20 * * * ?}",
        enabled = "${cemlink.collect.deposit-enabled:true}", lockLeaseSeconds = 0)
public class DepositCollectionJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(DepositCollectionJob.class);

    private static final String DOMAIN = "tombDeposit";

    private final DepositSourceReader reader;
    private final CemeteryLinkoraClient client;
    private final WatermarkStore watermark;

    public DepositCollectionJob(DepositSourceReader reader,
                                CemeteryLinkoraClient client,
                                WatermarkStore watermark) {
        this.reader = reader;
        this.client = client;
        this.watermark = watermark;
    }

    @Override
    protected void doExecute(JobContext context) {
        List<DepositSourceReader.DepositRow> rows = reader.readDeposits();
        if (rows.isEmpty()) {
            log.info("[cemlink] 寄存业务数据无增量");
            return;
        }
        Date operateTime = new Date();
        List<DepositData> batch = new ArrayList<>();
        for (DepositSourceReader.DepositRow row : rows) {
            if (watermark.needPush(DOMAIN, row.getBusinessId(), row.getModifyDate())) {
                batch.add(row.getData());
                watermark.markPushed(DOMAIN, row.getBusinessId(), operateTime);
            }
        }
        if (!batch.isEmpty()) {
            client.uploadDeposits(batch);
        }
        log.info("[cemlink] 寄存业务数据采集完成，本轮推送 {} 条", batch.size());
    }
}
