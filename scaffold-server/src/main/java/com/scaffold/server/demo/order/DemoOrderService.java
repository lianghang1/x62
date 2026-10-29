package com.scaffold.server.demo.order;

import com.scaffold.core.exception.BizException;
import com.scaffold.server.demo.order.entity.DemoOrder;
import com.scaffold.core.exception.GlobalErrorCode;
import com.scaffold.module.system.api.api.DictApi;
import com.scaffold.starter.protection.idempotent.Idempotent;
import com.scaffold.starter.protection.lock.DistributedLock;
import com.scaffold.starter.security.identity.AccountType;
import com.scaffold.starter.security.identity.LoginContext;
import com.scaffold.starter.security.identity.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 订单服务：同一个服务被三个入口共用——身份归属由各入口装配（见三入口 Controller），
 * 服务层只认 LoginContext 与业务参数，零感知调用方是谁。
 */
@Service
public class DemoOrderService {

    private final DemoOrderRepository orderRepository;
    private final DictApi dictApi;

    public DemoOrderService(DemoOrderRepository orderRepository, DictApi dictApi) {
        this.orderRepository = orderRepository;
        this.dictApi = dictApi;
    }

    @Transactional
    @Idempotent(windowSeconds = 10)
    public DemoOrder placeOrder(String title, java.math.BigDecimal amount, Long ownerAccountId,
                                String externalUserNo) {
        DemoOrder order = new DemoOrder();
        order.setOrderNo("D" + System.currentTimeMillis() + UUID.randomUUID().toString()
                .substring(0, 4).toUpperCase());
        order.setTitle(title);
        order.setAmount(amount);
        order.setOwnerAccountId(ownerAccountId);
        order.setExternalUserNo(externalUserNo);
        order.setStatus("CREATED");
        return orderRepository.save(order);
    }

    /** 状态流转（乐观锁兜底并发；合法状态走字典校验） */
    @Transactional
    @DistributedLock(key = "'demo-order-' + #orderId")
    public DemoOrder changeStatus(Long orderId, String targetStatus) {
        dictApi.validate("order_status", targetStatus);
        DemoOrder order = orderRepository.findById(orderId)
                .filter(o -> !Boolean.TRUE.equals(o.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "订单不存在"));
        order.setStatus(targetStatus);
        return orderRepository.save(order);
    }

    public DemoOrder getById(Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> !Boolean.TRUE.equals(o.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "订单不存在"));
    }

    /** 归属校验：只能看自己的单（MEMBER/APP）；OPERATOR 管理端可看全部 */
    public DemoOrder getByIdWithOwnerCheck(Long orderId) {
        DemoOrder order = getById(orderId);
        LoginUser user = LoginContext.current();
        if (user.getAccountType() == AccountType.OPERATOR) {
            return order;
        }
        if (!order.getOwnerAccountId().equals(user.getAccountId())) {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "无权访问该订单");
        }
        return order;
    }

    public Page<DemoOrder> myOrders(Pageable pageable) {
        return orderRepository.findByOwnerAccountIdAndDeletedFalseOrderByIdDesc(
                LoginContext.currentAccountId(), pageable);
    }

    public Page<DemoOrder> allOrders(Pageable pageable) {
        return orderRepository.findAllByDeletedFalseOrderByIdDesc(pageable);
    }
}
