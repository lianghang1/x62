package com.scaffold.cemlink.source;

import com.scaffold.cemlink.client.model.TombBlockData;
import com.scaffold.cemlink.client.model.TombBlockGridData;
import com.scaffold.cemlink.park.BlockParkStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 墓区业务数据读取：SQL 与组装规则沿用原采集程序
 *
 * @author lianghang
 */
public class BlockSourceReader {

    private static final String BLOCK_SQL = "" +
            "SELECT t.id, t.super_id parent_id, getTombBlockChildList ( t.id ) child_ids, t.block_code, t.block_name, " +
            "IFNULL( t.is_bottom, '0' ) is_bottom, t.length_grid, t.width_grid, IFNULL( t.modify_date, t.create_date ) modify_date " +
            "FROM t_sfi_tomb_block t " +
            "LEFT JOIN( SELECT m.BLOCK_ID, max( m.COLLECTION_TIME) COLLECTION_TIME FROM t_sfi_bus_collection_block m GROUP BY m.BLOCK_ID ) t2 " +
            "ON t.id = t2.BLOCK_ID " +
            "WHERE t.is_validate = '1' AND t.layer_id='1' " +
            "AND ( t2.COLLECTION_TIME IS NULL OR ( IFNULL( t.modify_date, t.create_date ) > t2.COLLECTION_TIME ) ) " +
            "ORDER BY t.block_name ASC";

    private static final String GRID_SQL = "" +
            "SELECT t.id,t.id tomb_id,t.block_id, t.block_id tomb_block_id,t.layer_code,t.display_no,t.tomb_path,t.grave_no," +
            "t.sell_area,t.green_area,t.fact_area,t.build_area,t.tomb_material, " +
            "t.basic_price,t.base_sum,t.maintenance_fee, " +
            "t.inner_length,t.inner_height,t.inner_width, t.is_save, " +
            "ts.`name` tomb_style,bs.`name` bury_style " +
            "FROM t_sfi_tomb t " +
            "LEFT JOIN t_sfi_tomb_style ts ON ts.id=t.tomb_style " +
            "LEFT JOIN t_sfi_code_bury_style bs ON bs.id=t.bury_style_id " +
            "WHERE t.is_validate='1' AND t.is_on_line='1' AND t.status_id='2' " +
            "ORDER BY t.layer_code";

    private final JdbcTemplate jdbcTemplate;
    private final BlockParkStrategy parkStrategy;

    public BlockSourceReader(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                             BlockParkStrategy parkStrategy) {
        this.jdbcTemplate = jdbcTemplate;
        this.parkStrategy = parkStrategy;
    }

    public List<BlockRow> readBlocks() {
        List<BlockQueryBean> blocks = NativeQueryExecutor.query(jdbcTemplate, BLOCK_SQL, BlockQueryBean.class);
        List<BlockRow> result = new ArrayList<>();
        if (!blocks.isEmpty()) {
            List<BlockGridQueryBean> grids = NativeQueryExecutor.query(jdbcTemplate, GRID_SQL, BlockGridQueryBean.class);
            for (BlockQueryBean block : blocks) {
                result.add(new BlockRow(assemble(block, grids), block.getId(), block.getModifyDate()));
            }
        }
        return result;
    }

    /** 顶层虚拟墓区（原程序每轮随变更批次一并上报），无则返回 null */
    public TombBlockData superBlock() {
        BlockParkStrategy.SuperBlock superBlock = parkStrategy.superBlock();
        if (superBlock == null) {
            return null;
        }
        TombBlockData top = new TombBlockData();
        top.setId(superBlock.getId());
        top.setBlockCode(superBlock.getBlockCode());
        top.setBlockName(superBlock.getBlockName());
        top.setIsBottom(superBlock.getIsBottom());
        top.setLengthGrid(superBlock.getLengthGrid());
        top.setWidthGrid(superBlock.getWidthGrid());
        return top;
    }

    /** 单个墓区：payload + 增量比对键 */
    public static class BlockRow {

        private final TombBlockData block;
        private final String blockId;
        private final Date modifyDate;

        BlockRow(TombBlockData block, String blockId, Date modifyDate) {
            this.block = block;
            this.blockId = blockId;
            this.modifyDate = modifyDate;
        }

        public TombBlockData getBlock() {
            return block;
        }

        public String getBlockId() {
            return blockId;
        }

        public Date getModifyDate() {
            return modifyDate;
        }
    }

    private TombBlockData assemble(BlockQueryBean block, List<BlockGridQueryBean> grids) {
        TombBlockData data = new TombBlockData();
        data.setId(block.getId());
        data.setParentId(block.getParentId());
        data.setBlockCode(block.getBlockCode());
        data.setBlockName(block.getBlockName());
        data.setIsBottom(block.getIsBottom());
        data.setLengthGrid(block.getLengthGrid());
        data.setWidthGrid(block.getWidthGrid());
        if (grids != null && !grids.isEmpty()) {
            List<TombBlockGridData> gridDatas = new ArrayList<>(grids.size());
            for (BlockGridQueryBean grid : grids) {
                if (block.getChildIds() != null
                        && block.getChildIds().indexOf(',' + grid.getBlockId()) != -1) {
                    gridDatas.add(assembleGrid(block, grid));
                }
            }
            data.setCleanGrid(true);
            data.setGridDatas(gridDatas);
        }
        return data;
    }

    private TombBlockGridData assembleGrid(BlockQueryBean block, BlockGridQueryBean grid) {
        TombBlockGridData data = new TombBlockGridData();
        data.setId(grid.getId());
        data.setTombId(grid.getTombId());
        data.setTombBlockId(block.getId());
        data.setLayerCode(grid.getLayerCode());
        data.setDisplayNo(grid.getDisplayNo());
        data.setTombPath(grid.getTombPath());
        data.setGraveNo(grid.getGraveNo());
        data.setSellArea(str(grid.getSellArea()));
        data.setGreenArea(str(grid.getGreenArea()));
        data.setFactArea(str(grid.getFactArea()));
        data.setBuildArea(str(grid.getBuildArea()));
        data.setTombMaterial(grid.getTombMaterial());
        data.setIsSave(grid.getIsSave());
        data.setTombStyle(grid.getTombStyle());
        data.setBuryStyle(grid.getBuryStyle());
        data.setStatus("0");
        data.setIsTomb("1");
        data.setEnabled("1");
        if (grid.getInnerHeight() != null && grid.getInnerLength() != null && grid.getInnerWidth() != null) {
            data.setBoreSize(formatPrice(grid.getInnerLength()) + "*" + formatPrice(grid.getInnerWidth())
                    + "*" + formatPrice(grid.getInnerHeight()));
        }
        if (grid.getBasicPrice() != null) {
            data.setPrice(grid.getBasicPrice().multiply(new BigDecimal(100)).longValue());
        }
        if (grid.getMaintenanceFee() != null) {
            data.setMaintenancePrice(grid.getMaintenanceFee().multiply(new BigDecimal(100)).longValue());
        }
        return data;
    }

    private static String str(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static String formatPrice(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
