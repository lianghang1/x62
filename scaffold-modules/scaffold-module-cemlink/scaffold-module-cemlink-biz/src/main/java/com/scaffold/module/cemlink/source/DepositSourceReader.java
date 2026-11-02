package com.scaffold.module.cemlink.source;

import com.scaffold.module.cemlink.client.model.DepositData;
import com.scaffold.module.cemlink.contract.AshesBusinessStatus;
import com.scaffold.module.cemlink.contract.DepositBillType;
import com.scaffold.module.cemlink.contract.DepositCharge;
import com.scaffold.module.cemlink.contract.DepositStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 寄存业务采集源：SQL 沿用原采集程序，含业务状态映射与收费项目计算
 *
 * @author lianghang
 */
public class DepositSourceReader {

    private static final String DEPOSIT_SQL = "SELECT t.id, " +
            "t.business_no biz_no, " +
            "t.grid_paths grid, " +
            "t1.`name` director_name, " +
            "t1.mobile_no director_phone, " +
            "t.user_names user_name, " +
            "t.`status` state, " +
            "t2.deposit_year deposit_times, " +
            "IFNULL( t2.deposit_fee, 0.00 ) deposit_fee, " +
            "DATE_FORMAT( t2.start_date, '%Y-%m-%d') start_date, " +
            "DATE_FORMAT( t2.end_date, '%Y-%m-%d' ) end_date, " +
            "t5.qr_code, " +
            "t5.box_size user_num, " +
            "t4.deposit_fee grid_fee, " +
            "t4.grid_area, " +
            "t4.layer grid_layer, " +
            "t.is_validate, " +
            "t6.is_on_line, " +
            "IFNULL( t.modify_date, t.create_date ) AS modify_date " +
            "FROM t_sfi_ashes_business t " +
            "LEFT JOIN t_sfi_ashes_bus_linkman t1 ON t.id = t1.business_id AND t1.is_validate = '1' " +
            "LEFT JOIN t_sfi_ashes_bus_user t5 ON t.id = t5.business_id AND t1.is_validate = '1' " +
            "LEFT JOIN t_sfi_ashes_bus_deposit t2 ON t.id = t2.business_id AND t2.last_flag = '1' AND t2.is_validate = '1' " +
            "LEFT JOIN t_sfi_ashes_bus_grid t3 ON t.id = t3.business_id   AND t3.is_validate = '1' AND IFNULL(t3.is_transfer_flag,'0') = '0' " +
            "LEFT JOIN t_sfi_ashes_grid t4 ON t4.id = t3.grid_id " +
            "LEFT JOIN t_sfi_ashes_block t6 ON t6.id = t4.block_id " +
            "WHERE t.is_validate = '1' and t.pay_status in ('1','2') ";

    private static final String DEPOSIT_ORDER_BY = "ORDER BY t.id DESC";

    /** 原程序 AshesBusItemDao.findBusItemByBillType 平移（仅取 sum 参与管理费计算） */
    private static final String BUS_ITEM_SQL =
            "SELECT `sum` FROM t_sfi_ashes_bus_item WHERE business_id = ? AND bill_type = ? AND is_validate = '1' ORDER BY id DESC LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    public DepositSourceReader(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 原程序 t_sfi_ashes_bus_collection_detail 水位改由本地水位线承担；floorTime 非空时过滤该时间之前的存量数据 */
    public List<DepositRow> readDeposits(Date floorTime) {
        String sql = DEPOSIT_SQL;
        Object[] params = new Object[0];
        if (floorTime != null) {
            sql += " AND IFNULL( t.modify_date, t.create_date ) > ?";
            params = new Object[]{floorTime};
        }
        sql += DEPOSIT_ORDER_BY;
        List<DepositQueryBean> rows = NativeQueryExecutor.query(jdbcTemplate, sql, DepositQueryBean.class, params);
        List<DepositRow> result = new ArrayList<>();
        for (DepositQueryBean bean : rows) {
            if (bean.getDirectorName() != null && !bean.getDirectorName().isEmpty()) {
                bean.setDirectorName(bean.getDirectorName().replaceAll(" ", ""));
            }
            if (bean.getUserName() != null && !bean.getUserName().isEmpty()) {
                bean.setUserName(bean.getUserName().replaceAll(" ", ""));
            }
            bean.setRenewable(true);
            bean.setDepositCharge(DepositCharge.按年收费.getCode());
            if ("1".equals(bean.getIsValidate())) {
                if (AshesBusinessStatus.登记.getCode().equals(bean.getState())) {
                    bean.setState(DepositStatus.预订.getCode());

                } else if (AshesBusinessStatus.存入.getCode().equals(bean.getState())
                        || AshesBusinessStatus.部分存入.getCode().equals(bean.getState())) {
                    bean.setState(DepositStatus.已存.getCode());

                } else if (AshesBusinessStatus.已领出.getCode().equals(bean.getState())) {
                    bean.setState(DepositStatus.已退.getCode());

                } else {
                    bean.setState(DepositStatus.无效.getCode());
                }
                // 设置收费项目
                setDepositFee(bean);
            } else {
                bean.setState(DepositStatus.无效.getCode());
            }

            if ("0".equals(bean.getIsOnLine())) {
                bean.setRenewable(false);
            }
            result.add(new DepositRow(assemble(bean), bean.getId(), bean.getModifyDate()));
        }
        return result;
    }

    /** 原程序 setDepositFee 平移 */
    private void setDepositFee(DepositQueryBean data) {
        BigDecimal boxSize = new BigDecimal(1);
        if (data.getUserNum() != null && !data.getUserNum().isEmpty()) {
            boxSize = new BigDecimal(data.getUserNum());
        }

        if (data.getGridFee() != null) {
            BigDecimal mf = new BigDecimal(Double.toString(data.getGridFee()));
            BigDecimal ff = new BigDecimal(100);
            BigDecimal amountPerYear = mf.multiply(boxSize).multiply(ff);
            data.setPrice(amountPerYear.longValue());
        } else if (data.getDepositTimes() != null && data.getDepositFee() != null
                && data.getDepositTimes() > 0 && data.getDepositFee() > 0) {
            Double busItemSum = findBusItemSum(data.getId(), DepositBillType.管理费.getCode());
            Double depositeFee = data.getDepositFee();
            if (busItemSum != null) {
                depositeFee += busItemSum;
                BigDecimal mf = new BigDecimal(Double.toString(busItemSum));
                BigDecimal ff = new BigDecimal(100);
                data.setManagePrice(mf.multiply(ff).longValue());
                data.setDepositCharge(DepositCharge.按年收费.getCode());
            }
            BigDecimal mf = new BigDecimal(Double.toString(depositeFee));
            BigDecimal yf = new BigDecimal(data.getDepositTimes());
            BigDecimal ff = new BigDecimal(100);
            BigDecimal amountPerYear = mf.divide(yf, 2, BigDecimal.ROUND_HALF_UP).multiply(boxSize).multiply(ff);
            data.setPrice(amountPerYear.longValue());
        }
    }

    private Double findBusItemSum(String businessId, String billType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(BUS_ITEM_SQL, businessId, billType);
        if (rows.isEmpty() || rows.get(0).get("sum") == null) {
            return null;
        }
        Object sum = rows.get(0).get("sum");
        return sum instanceof Number ? ((Number) sum).doubleValue() : Double.valueOf(sum.toString());
    }

    private DepositData assemble(DepositQueryBean bean) {
        DepositData data = new DepositData();
        data.setBizNo(bean.getBizNo());
        data.setGrid(bean.getGrid());
        data.setState(bean.getState());
        data.setUserName(bean.getUserName());
        data.setUserNum(bean.getUserNum());
        data.setDepositTimes(bean.getDepositTimes());
        data.setDepositCharge(bean.getDepositCharge());
        data.setStartDate(bean.getStartDate());
        data.setEndDate(bean.getEndDate());
        data.setGridArea(bean.getGridArea());
        data.setGridLayer(bean.getGridLayer());
        data.setDirectorPhone(bean.getDirectorPhone());
        data.setDirectorName(bean.getDirectorName());
        data.setPrice(bean.getPrice());
        data.setManagePrice(bean.getManagePrice());
        data.setRenewable(bean.getRenewable());
        data.setQrCode(bean.getQrCode());
        return data;
    }

    /** 单条寄存业务：payload + 增量比对键 */
    public static class DepositRow {

        private final DepositData data;
        private final String businessId;
        private final Date modifyDate;

        DepositRow(DepositData data, String businessId, Date modifyDate) {
            this.data = data;
            this.businessId = businessId;
            this.modifyDate = modifyDate;
        }

        public DepositData getData() {
            return data;
        }

        public String getBusinessId() {
            return businessId;
        }

        public Date getModifyDate() {
            return modifyDate;
        }
    }
}
