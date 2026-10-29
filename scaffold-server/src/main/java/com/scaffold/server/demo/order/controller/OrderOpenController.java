package com.scaffold.server.demo.order.controller;

import com.scaffold.core.result.PageResult;
import com.scaffold.core.result.R;
import com.scaffold.server.demo.order.DemoOrderService;
import com.scaffold.server.demo.order.entity.DemoOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * openapi 端订单（三方系统视角：owner = APP 账户，scope 鉴权）。
 * 签名请求头：X-App-Key / X-Timestamp / X-Nonce / X-Sign（算法见 SignUtils 注释）。
 */
@Tag(name = "openapi-订单")
@RestController
@RequestMapping("/openapi/orders")
public class OrderOpenController {

    private final DemoOrderService orderService;

    public OrderOpenController(DemoOrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "三方下单（scope: order:create）")
    @PreAuthorize("hasAuthority('order:create')")
    @PostMapping
    public R<DemoOrder> create(@RequestBody Map<String, Object> body) {
        String title = String.valueOf(body.get("title"));
        java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(body.get("amount")));
        String externalUserNo = body.get("externalUserNo") == null
                ? null : String.valueOf(body.get("externalUserNo"));
        // owner = 当前 APP 账户；终端用户标识备查不进主模型
        Long appAccountId = com.scaffold.starter.security.identity.LoginContext.currentAccountId();
        return R.ok(orderService.placeOrder(title, amount, appAccountId, externalUserNo));
    }

    @Operation(summary = "三方订单列表（scope: order:query）")
    @PreAuthorize("hasAuthority('order:query')")
    @GetMapping
    public R<PageResult<DemoOrder>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Page<DemoOrder> page = orderService.myOrders(PageRequest.of(Math.max(pageNum - 1, 0), pageSize));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    @Operation(summary = "订单详情（只能看自己应用的）")
    @PreAuthorize("hasAuthority('order:query')")
    @GetMapping("/{id}")
    public R<DemoOrder> detail(@PathVariable Long id) {
        return R.ok(orderService.getByIdWithOwnerCheck(id));
    }
}
