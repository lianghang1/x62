package com.scaffold.module.cemlink.source;

import com.scaffold.module.cemlink.client.model.TombServiceItemData;
import com.scaffold.module.cemlink.contract.BillSubType;
import com.scaffold.module.cemlink.contract.OrderType;
import com.scaffold.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务项目采集源：SQL 沿用原采集程序，按单据子类型映射微商订单类型
 *
 * @author lianghang
 */
public class ServiceItemSourceReader {

    private static final String SERVICE_SQL = "SELECT t3.id, t3.`name`, t3.price * 100 AS price, 1 AS qty , t3.index_no AS orderNo, t3.is_validate AS STATUS, t.bill_type, t.bill_sub_type, " +
            "IFNULL( t3.modify_date, t3.create_date ) AS modify_date " +
            "FROM t_sfi_config_item_node t " +
            "JOIN t_sfi_code_service_type t1 ON t1.id IN (t.item_sort_id_array) AND t1.is_validate = '1' " +
            "JOIN t_sfi_code_service t3 ON t1.id = t3.service_type_id AND t3.price >= 0 " +
            "WHERE t.is_validate = '1' " +
            "AND t.item_type = '1'";

    private final JdbcTemplate jdbcTemplate;

    public ServiceItemSourceReader(@Qualifier("cemlinkParkJdbc") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 原程序 t_sfi_bus_collection_service 水位改由本地水位线承担，仅保留微商订单类型非空的有效行；floorTime 非空时过滤该时间之前的存量数据 */
    public List<ServiceItemRow> readServiceItems(Date floorTime) {
        String sql = SERVICE_SQL;
        Object[] params = new Object[0];
        if (floorTime != null) {
            sql += " AND IFNULL( t3.modify_date, t3.create_date ) > ?";
            params = new Object[]{floorTime};
        }
        List<ServiceItemQueryBean> rows = NativeQueryExecutor.query(jdbcTemplate, sql, ServiceItemQueryBean.class, params);
        List<ServiceItemRow> result = new ArrayList<>();
        for (ServiceItemQueryBean bean : rows) {
            if (BillSubType.盆花租摆.getCode().equals(bean.getBillSubType())) {
                bean.setOrderTypeNo(OrderType.盆花租摆.getCode());
            } else if (BillSubType.代客祭扫.getCode().equals(bean.getBillSubType())) {
                bean.setOrderTypeNo(OrderType.代客祭扫.getCode());
            } else if (BillSubType.描碑.getCode().equals(bean.getBillSubType())) {
                bean.setOrderTypeNo(OrderType.描碑.getCode());
            } else if (BillSubType.墓穴报修.getCode().equals(bean.getBillSubType())) {
                bean.setOrderTypeNo(OrderType.在线报修.getCode());
            }
            if (StringUtils.isNotBlank(bean.getOrderTypeNo())) {
                result.add(new ServiceItemRow(assemble(bean), bean.getId(), bean.getModifyDate()));
            }
        }
        return result;
    }

    private TombServiceItemData assemble(ServiceItemQueryBean bean) {
        TombServiceItemData data = new TombServiceItemData();
        data.setId(bean.getId());
        data.setName(bean.getName());
        data.setPrice(bean.getPrice());
        data.setQty(bean.getQty());
        data.setOrderNo(bean.getOrderNo());
        data.setStatus(bean.getStatus());
        data.setOrderType(bean.getOrderTypeNo());
        return data;
    }

    /** 单个服务项目：payload + 增量比对键 */
    public static class ServiceItemRow {

        private final TombServiceItemData data;
        private final String itemId;
        private final Date modifyDate;

        ServiceItemRow(TombServiceItemData data, String itemId, Date modifyDate) {
            this.data = data;
            this.itemId = itemId;
            this.modifyDate = modifyDate;
        }

        public TombServiceItemData getData() {
            return data;
        }

        public String getItemId() {
            return itemId;
        }

        public Date getModifyDate() {
            return modifyDate;
        }
    }
}
