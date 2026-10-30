package com.scaffold.server.demo.order.controller;

import com.scaffold.core.result.PageResult;
import com.scaffold.core.result.R;
import com.scaffold.server.demo.order.DemoOrderService;
import com.scaffold.server.demo.order.entity.DemoOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * internal 端订单管理接口。
 *
 * <p>管理后台视角：可查看全部订单并代客操作——owner 由请求参数 {@code customerId} 显式指定，
 * 状态流转依赖服务层的字典校验与乐观锁/分布式锁兜底并发。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-订单管理")
@RestController
@RequestMapping("/internal/orders")
public class OrderInternalController {

    private final DemoOrderService orderService;

    public OrderInternalController(DemoOrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单分页")
    @GetMapping
    public R<PageResult<DemoOrder>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Page<DemoOrder> page = orderService.allOrders(PageRequest.of(Math.max(pageNum - 1, 0), pageSize));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<DemoOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
    }

    @Operation(summary = "代客下单（owner = 请求参数里的 customerId）")
    @PostMapping
    public R<DemoOrder> create(@RequestParam String title,
                               @RequestParam java.math.BigDecimal amount,
                               @RequestParam Long customerId) {
        return R.ok(orderService.placeOrder(title, amount, customerId, null));
    }

    @Operation(summary = "状态流转")
    @PostMapping("/{id}/status")
    public R<DemoOrder> changeStatus(@PathVariable Long id, @RequestParam String targetStatus) {
        return R.ok(orderService.changeStatus(id, targetStatus));
    }
}
