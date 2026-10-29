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

/**
 * online 端订单（C 端视角：owner = 当前登录会员自己）。
 */
@Tag(name = "online-我的订单")
@RestController
@RequestMapping("/online/orders")
public class OrderOnlineController {

    private final DemoOrderService orderService;

    public OrderOnlineController(DemoOrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "下单（owner = 当前登录会员）")
    @PostMapping
    public R<DemoOrder> create(@RequestParam String title,
                               @RequestParam java.math.BigDecimal amount) {
        return R.ok(orderService.placeOrder(title, amount,
                com.scaffold.starter.security.identity.LoginContext.currentAccountId(), null));
    }

    @Operation(summary = "我的订单")
    @GetMapping
    public R<PageResult<DemoOrder>> my(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "10") int pageSize) {
        Page<DemoOrder> page = orderService.myOrders(PageRequest.of(Math.max(pageNum - 1, 0), pageSize));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    @Operation(summary = "订单详情（只能看自己的）")
    @GetMapping("/{id}")
    public R<DemoOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.getByIdWithOwnerCheck(id));
    }
}
