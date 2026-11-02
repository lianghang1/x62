package com.scaffold.cemlink.collect;

import com.scaffold.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.cemlink.client.model.TombBlockData;
import com.scaffold.cemlink.config.CemlinkProperties;
import com.scaffold.cemlink.increment.WatermarkStore;
import com.scaffold.cemlink.source.BlockSourceReader;
import com.scaffold.starter.job.AbstractJob;
import com.scaffold.starter.job.Job;
import com.scaffold.starter.job.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 墓区数据采集任务：整库读取 → 本地时间水位比对 → linkora 上行（至少一次送达）
 *
 * @author lianghang
 */
@Job(name = "墓区数据采集", cron = "${cemlink.collect.block-cron:0 2/15 * * * ?}",
        enabled = "${cemlink.collect.block-enabled:true}", lockLeaseSeconds = 0)
public class BlockDataCollectionJob extends AbstractJob {

    private static final Logger log = LoggerFactory.getLogger(BlockDataCollectionJob.class);

    private static final String DOMAIN = "tombBlock";

    private final BlockSourceReader reader;
    private final CemeteryLinkoraClient client;
    private final WatermarkStore watermark;

    public BlockDataCollectionJob(BlockSourceReader reader,
                                  CemeteryLinkoraClient client,
                                  WatermarkStore watermark) {
        this.reader = reader;
        this.client = client;
        this.watermark = watermark;
    }

    @Override
    protected void doExecute(JobContext context) {
        List<BlockSourceReader.BlockRow> rows = reader.readBlocks();
        if (rows.isEmpty()) {
            log.info("[cemlink] 墓区数据无增量");
            return;
        }
        Date operateTime = new Date();
        List<BlockSourceReader.BlockRow> pending = new ArrayList<>();
        for (BlockSourceReader.BlockRow row : rows) {
            if (watermark.needPush(DOMAIN, row.getBlockId(), row.getModifyDate())) {
                pending.add(row);
            }
        }
        if (!pending.isEmpty()) {
            List<TombBlockData> batch = new ArrayList<>(pending.size() + 1);
            TombBlockData superBlock = reader.superBlock();
            if (superBlock != null) {
                batch.add(superBlock);
            }
            for (BlockSourceReader.BlockRow row : pending) {
                batch.add(row.getBlock());
                watermark.markPushed(DOMAIN, row.getBlockId(), operateTime);
            }
            client.uploadBlocks(batch);
        }
        log.info("[cemlink] 墓区数据采集完成，本轮推送 {} 个墓区", pending.size());
    }
}
