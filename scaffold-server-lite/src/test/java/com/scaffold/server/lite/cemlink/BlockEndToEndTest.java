package com.scaffold.server.lite.cemlink;

import com.linkora.client.LinkoraClient;
import com.linkora.client.UplinkPublisher;
import com.linkora.client.storage.sqlite.SqliteClientStore;
import com.linkora.testkit.EmbeddedLinkoraServer;
import com.scaffold.server.lite.cemlink.increment.WatermarkStore;
import com.scaffold.server.lite.cemlink.client.MallLinkoraClient;
import com.scaffold.server.lite.cemlink.client.cemetery.CemeteryLinkoraClient;
import com.scaffold.server.lite.cemlink.client.model.TombBlockData;
import com.scaffold.server.lite.cemlink.client.model.TombBlockGridData;
import com.scaffold.server.lite.cemlink.collect.BlockDataCollectionJob;
import com.scaffold.server.lite.cemlink.park.WeijiajiaoParkStrategy;
import com.scaffold.server.lite.cemlink.source.BlockSourceReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 墓区采集端到端：H2 模拟墓园库 → 采集 Job → linkora 内嵌服务端收发
 *
 * @author lianghang
 */
public class BlockEndToEndTest {

    private static final String CLIENT_ID = "cemlink-block-test";

    private EmbeddedLinkoraServer testServer;
    private SqliteClientStore clientStore;
    private LinkoraClient linkoraClient;
    private WatermarkStore watermark;
    private JdbcTemplate parkJdbc;
    private RecordingCemeteryClient cemeteryClient;
    private BlockDataCollectionJob job;

    static class RecordingCemeteryClient extends CemeteryLinkoraClient {

        final List<List<TombBlockData>> batches = new CopyOnWriteArrayList<>();

        RecordingCemeteryClient(MallLinkoraClient mallLinkoraClient) {
            super(mallLinkoraClient);
        }

        @Override
        public String uploadBlocks(List<TombBlockData> data) {
            batches.add(data);
            return super.uploadBlocks(data);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        parkJdbc = new JdbcTemplate(h2DataSource());
        createSchema();
        seedData();

        testServer = EmbeddedLinkoraServer.builder()
                .registerClient(CLIENT_ID)
                .build();
        testServer.start();

        Path clientDb = Files.createTempDirectory("cemlink-client").resolve("client.db");
        clientStore = new SqliteClientStore(clientDb);
        linkoraClient = LinkoraClient.builder()
                .serverUrl(testServer.baseUrl())
                .clientId(CLIENT_ID)
                .clientStore(clientStore)
                .build();
        linkoraClient.start();

        MallLinkoraClient mallClient = new MallLinkoraClient(new UplinkPublisher(linkoraClient, clientStore));
        cemeteryClient = new RecordingCemeteryClient(mallClient);

        watermark = TestWatermarkStores.inMemory();

        BlockSourceReader reader = new BlockSourceReader(parkJdbc, new WeijiajiaoParkStrategy());
        job = new BlockDataCollectionJob(reader, cemeteryClient, watermark);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (linkoraClient != null) {
            linkoraClient.close();
        }
        if (clientStore != null) {
            clientStore.close();
        }
        if (testServer != null) {
            testServer.close();
        }
    }

    @Test
    public void firstRunAssemblesSuperBlockGridsAndDelivers() {
        job.run();

        assertEquals(1, cemeteryClient.batches.size(), "首轮应推送一个批次");
        List<TombBlockData> batch = cemeteryClient.batches.get(0);
        assertEquals(2, batch.size(), "顶层虚拟墓区 + 变更墓区");

        TombBlockData superBlock = batch.get(0);
        assertEquals("20967", superBlock.getId());
        assertEquals("卫家角", superBlock.getBlockName());
        assertNull(superBlock.getGridDatas(), "顶层虚拟墓区不带格位");

        TombBlockData block = batch.get(1);
        assertEquals("BLK_A", block.getId());
        assertEquals("A区", block.getBlockName());
        assertEquals(Boolean.TRUE, block.getCleanGrid());
        assertNotNull(block.getGridDatas());
        assertEquals(2, block.getGridDatas().size(), "BLK_A 子树应有 2 个格位");

        TombBlockGridData grid = findByTombId(block.getGridDatas(), "G1");
        assertNotNull(grid);
        assertEquals("BLK_A", grid.getTombBlockId());
        assertEquals("0", grid.getStatus());
        assertEquals("1", grid.getIsTomb());
        assertEquals("1", grid.getEnabled());
        assertEquals("B-01", grid.getLayerCode());
        assertEquals(Long.valueOf(12800L * 100), grid.getPrice(), "基础价应转分");
        assertEquals(Long.valueOf(360L * 100), grid.getMaintenancePrice(), "维护费应转分");
        assertEquals("1.2*0.8*0.6", grid.getBoreSize(), "内径应为 长*宽*高 去尾零");

        TombBlockGridData other = findByTombId(block.getGridDatas(), "G2");
        assertNotNull(other, "BLK_C 下格位应挂到 BLK_A");
    }

    @Test
    public void watermarkSkipsUnchangedBlocksOnSecondRun() {
        job.run();
        assertEquals(1, cemeteryClient.batches.size());

        job.run();
        assertEquals(1, cemeteryClient.batches.size(), "无修改不应重复推送");
    }

    @Test
    public void modifyDateBumpRepublishesOnlyChangedBlock() throws Exception {
        job.run();
        assertEquals(1, cemeteryClient.batches.size());

        parkJdbc.update("UPDATE t_sfi_tomb_block SET modify_date = ? WHERE id = 'BLK_A'",
                new Timestamp(System.currentTimeMillis() + 60_000));
        job.run();

        assertEquals(2, cemeteryClient.batches.size(), "修改时间推进的墓区应再次推送");
        List<TombBlockData> secondBatch = cemeteryClient.batches.get(1);
        assertEquals(2, secondBatch.size(), "顶层虚拟墓区每轮都在结果集首位");
        assertEquals("BLK_A", secondBatch.get(1).getId());
    }

    private TombBlockGridData findByTombId(List<TombBlockGridData> grids, String tombId) {
        for (TombBlockGridData grid : grids) {
            if (tombId.equals(grid.getTombId())) {
                return grid;
            }
        }
        return null;
    }

    private static DriverManagerDataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:h2:mem:park_block_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }

    private void createSchema() {
        parkJdbc.execute("CREATE ALIAS IF NOT EXISTS getTombBlockChildList FOR \""
                + H2Functions.class.getName() + ".tombBlockChildList\"");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb_block (id VARCHAR(32) PRIMARY KEY, "
                + "super_id VARCHAR(32), block_code VARCHAR(64), block_name VARCHAR(64), "
                + "is_bottom VARCHAR(2), length_grid INT, width_grid INT, layer_id VARCHAR(2), "
                + "is_validate VARCHAR(2), modify_date TIMESTAMP, create_date TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_bus_collection_block (block_id VARCHAR(32), "
                + "collection_time TIMESTAMP)");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb (id VARCHAR(32) PRIMARY KEY, "
                + "layer_code VARCHAR(64), display_no VARCHAR(16), tomb_path VARCHAR(128), "
                + "block_id VARCHAR(32), grave_no INT, sell_area DECIMAL(10,2), "
                + "green_area DECIMAL(10,2), fact_area DECIMAL(10,2), build_area DECIMAL(10,2), "
                + "tomb_material VARCHAR(32), basic_price DECIMAL(12,2), base_sum DECIMAL(12,2), "
                + "maintenance_fee DECIMAL(12,2), inner_length DECIMAL(8,2), "
                + "inner_height DECIMAL(8,2), inner_width DECIMAL(8,2), is_save VARCHAR(2), "
                + "tomb_style VARCHAR(32), bury_style_id VARCHAR(32), "
                + "is_validate VARCHAR(2), is_on_line VARCHAR(2), status_id VARCHAR(2))");
        parkJdbc.execute("CREATE TABLE t_sfi_tomb_style (id VARCHAR(32) PRIMARY KEY, name VARCHAR(64))");
        parkJdbc.execute("CREATE TABLE t_sfi_code_bury_style (id VARCHAR(32) PRIMARY KEY, name VARCHAR(64))");
    }

    private void seedData() {
        long base = System.currentTimeMillis() - 3_600_000L;
        parkJdbc.update("INSERT INTO t_sfi_tomb_block (id, super_id, block_code, block_name, is_bottom, "
                        + "length_grid, width_grid, layer_id, is_validate, modify_date, create_date) VALUES "
                        + "('BLK_A', NULL, 'QA', 'A区', '0', 10, 10, '1', '1', ?, ?)",
                new Timestamp(base), new Timestamp(base));
        parkJdbc.update("INSERT INTO t_sfi_tomb_block (id, super_id, block_code, block_name, is_bottom, "
                        + "layer_id, is_validate, create_date) VALUES "
                        + "('BLK_B', 'BLK_A', 'QB', 'B排', '1', '2', '1', ?)", new Timestamp(base));
        parkJdbc.update("INSERT INTO t_sfi_tomb_block (id, super_id, block_code, block_name, is_bottom, "
                        + "layer_id, is_validate, create_date) VALUES "
                        + "('BLK_C', 'BLK_A', 'QC', 'C排', '1', '2', '1', ?)", new Timestamp(base));

        parkJdbc.update("INSERT INTO t_sfi_tomb_style (id, name) VALUES ('S1', '标准墓')");
        parkJdbc.update("INSERT INTO t_sfi_code_bury_style (id, name) VALUES ('B1', '入土')");
        seedTomb("G1", "BLK_B", "B-01", "1.20", "0.80", "0.60");
        seedTomb("G2", "BLK_C", "C-01", null, null, null);
        seedTomb("G3", "BLK_X", "X-01", null, null, null);
    }

    private void seedTomb(String id, String blockId, String layerCode,
                          String innerLength, String innerWidth, String innerHeight) {
        parkJdbc.update("INSERT INTO t_sfi_tomb (id, layer_code, display_no, tomb_path, block_id, grave_no, "
                + "sell_area, green_area, fact_area, build_area, tomb_material, basic_price, base_sum, "
                + "maintenance_fee, inner_length, inner_height, inner_width, is_save, tomb_style, "
                + "bury_style_id, is_validate, is_on_line, status_id) VALUES "
                + "(?, ?, '01', ?, ?, 1, 3.00, 0.50, 1.50, 2.00, '花岗岩', 12800.00, 0.00, 360.00, "
                + "?, ?, ?, '0', 'S1', 'B1', '1', '1', '2')",
                id, layerCode, "A区->" + layerCode, blockId, innerLength, innerHeight, innerWidth);
    }
}
