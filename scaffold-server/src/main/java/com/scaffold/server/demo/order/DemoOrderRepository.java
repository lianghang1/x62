package com.scaffold.server.demo.order;

import com.scaffold.server.demo.order.entity.DemoOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 示例订单仓储。
 *
 * <p>继承 {@code JpaRepository} 获得通用 CRUD；派生查询统一过滤逻辑删除并按 id 倒序，
 * 分别供 online（按归属人）与 internal（全量）两个视角复用。</p>
 *
 * @author lianghang
 */
public interface DemoOrderRepository extends JpaRepository<DemoOrder, Long> {

    /** 查询某归属人的未删除订单，按 id 倒序。 */
    Page<DemoOrder> findByOwnerAccountIdAndDeletedFalseOrderByIdDesc(Long ownerAccountId, Pageable pageable);

    /** 查询全部未删除订单，按 id 倒序。 */
    Page<DemoOrder> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);
}
