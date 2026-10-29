package com.scaffold.server.demo.order;

import com.scaffold.server.demo.order.entity.DemoOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoOrderRepository extends JpaRepository<DemoOrder, Long> {

    Page<DemoOrder> findByOwnerAccountIdAndDeletedFalseOrderByIdDesc(Long ownerAccountId, Pageable pageable);

    Page<DemoOrder> findAllByDeletedFalseOrderByIdDesc(Pageable pageable);
}
