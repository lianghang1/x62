package com.scaffold.module.cemlink.source;

import com.scaffold.module.cemlink.park.TombParkStrategy;
import com.sfi.mall.contract.base.model.cemetery.TransportTomb;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 墓穴业务数据读取：SQL 与组装规则沿用原采集程序，行映射走 NativeQueryExecutor
 *
 * @author lianghang
 */
public class TombSourceReader {

    private static final String TOMB_SQL = "" +
            "SELECT t.id, " +
            "t4.layer_code, " +
            "t4.tomb_path, " +
            "t4.block_id, " +
            "t4.grave_no, " +
            "t5.NAME tomb_style, " +
            "t7.NAME bury_style, " +
            "t8.outdoor_flag, " +
            "IFNULL( t4.fact_area, 0.00 ) fact_area, " +
            "IFNULL( t4.sell_area, 0.00 ) sell_area, " +
            "IFNULL( t4.basic_price, 0.00 ) basic_price, " +
            "DATE_FORMAT( t3.apply_date, '%Y-%m-%d') app_date, " +
            "t.pay_status, " +
            "t2.`name`, " +
            "t2.mobile_no, " +
            "t.tomb_id, " +
            "t4.maintenance_year, " +
            "t4.maintenance_fee, " +
            "t.collect_version " +
            "FROM t_sfi_business t " +
            "JOIN t_sfi_bus_linkman t2 ON t.id = t2.business_id AND t2.is_validate = '1' " +
            "JOIN t_sfi_bus_detail t3 ON t.id = t3.business_id AND t3.is_validate = '1' " +
            "JOIN t_sfi_tomb t4 ON ( t.tomb_id = t4.id OR t.return_tomb_id = t4.id) " +
            "LEFT JOIN t_sfi_tomb_style t5 ON t5.id = t4.tomb_style " +
            "LEFT JOIN t_sfi_code_bury_style t7 ON t7.id = t4.bury_style_id " +
            "LEFT JOIN t_sfi_code_bury_style_type t8 ON t8.id = t7.style_type_id " +
            "LEFT JOIN(SELECT m.bus_id, max( m.collection_version) collection_version " +
            "FROM t_sfi_bus_collection_detail m GROUP BY m.bus_id ) t6 ON t.id = t6.bus_id " +
            "WHERE t.is_validate = '1' AND t.pay_status = '3' " +
            "AND ( t6.collection_version IS NULL OR t.collect_version > t6.collection_version ) " +
            "ORDER BY t.id ASC";

    private static final String LAST_MAINT_SQL =
            "select * from t_sfi_bus_maintenance where business_id=? and is_validate='1'"
                    + " order by maint_index desc limit 1";

    private final JdbcTemplate jdbcTemplate;
    private final TombParkStrategy parkStrategy;

    public TombSourceReader(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate,
                            TombParkStrategy parkStrategy) {
        this.jdbcTemplate = jdbcTemplate;
        this.parkStrategy = parkStrategy;
    }

    /** 单行业务数据：payload + 增量比对键 */
    public static class TombRow {

        private final TransportTomb tomb;
        private final String bizId;
        private final int collectVersion;

        TombRow(TransportTomb tomb, String bizId, int collectVersion) {
            this.tomb = tomb;
            this.bizId = bizId;
            this.collectVersion = collectVersion;
        }

        public TransportTomb getTomb() {
            return tomb;
        }

        public String getBizId() {
            return bizId;
        }

        public int getCollectVersion() {
            return collectVersion;
        }
    }

    public List<TombRow> readPage(int page, int pageSize) {
        String sql = TOMB_SQL + " LIMIT ? OFFSET ?";
        List<TombQueryBean> beans = NativeQueryExecutor.query(
                jdbcTemplate, sql, TombQueryBean.class, pageSize, page * pageSize);
        List<TombRow> result = new ArrayList<>(beans.size());
        for (TombQueryBean bean : beans) {
            result.add(assemble(bean));
        }
        return result;
    }

    private TombRow assemble(TombQueryBean bean) {
        TransportTomb tomb = new TransportTomb();
        tomb.setTombId(bean.getTombId());
        tomb.setBlockId(bean.getBlockId());
        tomb.setTombNo(parkStrategy.cleanLayerCode(bean.getLayerCode()));
        tomb.setTombPath(parkStrategy.cleanTombPath(bean.getTombPath()));
        tomb.setTombs(bean.getGraveNo() == null ? 0 : bean.getGraveNo());
        tomb.setTypeCode(bean.getTombStyle());
        tomb.setBuryStyle(bean.getBuryStyle());
        tomb.setNetArea(str(bean.getFactArea()));
        tomb.setFloorArea(str(bean.getSellArea()));
        tomb.setBuyTime(parseDate(bean.getAppDate()));
        // 已购墓默认可租摆、已付清
        tomb.setRentingFlag(true);
        tomb.setPayState("2");
        tomb.setDirectorName(bean.getName());
        tomb.setDirectorPhone(bean.getMobileNo());
        tomb.setState(hasLength(bean.getTombId()) ? "1" : "2");
        tomb.setAmountPerYear(0L);

        MaintenanceRow maint = lastMaintenance(bean.getId());
        if (maint != null) {
            tomb.setMaintenanceEndTime(maint.getEndDate());
            if (maint.getMaintYear() != null && maint.getMaintFee() != null
                    && maint.getMaintYear() > 0 && maint.getMaintFee().signum() > 0) {
                tomb.setAmountPerYear(maint.getMaintFee()
                        .divide(new BigDecimal(maint.getMaintYear()), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100)).longValue());
            }
        }
        return new TombRow(tomb, bean.getId(), bean.getCollectVersion() == null ? 0 : bean.getCollectVersion());
    }

    private MaintenanceRow lastMaintenance(String businessId) {
        List<MaintenanceRow> rows = NativeQueryExecutor.query(
                jdbcTemplate, LAST_MAINT_SQL, MaintenanceRow.class, businessId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static String str(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static boolean hasLength(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Date parseDate(String value) {
        if (!hasLength(value)) {
            return null;
        }
        java.time.LocalDate localDate = com.scaffold.common.util.DateUtils.parseDate(value);
        return localDate == null ? null
                : Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }

    /** 最近一次维护费记录，字段对齐 t_sfi_bus_maintenance 列别名 */
    public static class MaintenanceRow {

        private Date endDate;
        private Integer maintYear;
        private BigDecimal maintFee;

        public Date getEndDate() {
            return endDate;
        }

        public Integer getMaintYear() {
            return maintYear;
        }

        public BigDecimal getMaintFee() {
            return maintFee;
        }
    }
}
