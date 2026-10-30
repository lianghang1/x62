package com.scaffold.module.system.biz.controller;

import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.R;
import com.scaffold.module.system.biz.entity.LoginLog;
import com.scaffold.module.system.biz.entity.OperLog;
import com.scaffold.module.system.biz.repository.LoginLogRepository;
import com.scaffold.module.system.biz.repository.OperLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * internal 端审计日志查询接口。
 *
 * <p>登录日志与操作日志只读出参：按账号/模块、成败、时间段动态过滤，
 * 时间倒序分页——安全审计的高频姿势是「查某人近况」「查某时段异常」。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-审计日志")
@RestController
@RequestMapping("/internal/system/audit")
public class AuditInternalController {

    private final LoginLogRepository loginLogRepository;
    private final OperLogRepository operLogRepository;

    public AuditInternalController(LoginLogRepository loginLogRepository,
                                   OperLogRepository operLogRepository) {
        this.loginLogRepository = loginLogRepository;
        this.operLogRepository = operLogRepository;
    }

    @Operation(summary = "登录日志分页（按账号/成败/时间段过滤）")
    @GetMapping("/login")
    public R<PageResult<LoginLog>> loginLogs(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "20") int pageSize,
                                             @RequestParam(required = false) String accountNo,
                                             @RequestParam(required = false) String loginType,
                                             @RequestParam(required = false) Boolean success,
                                             @RequestParam(required = false) LocalDate beginDate,
                                             @RequestParam(required = false) LocalDate endDate) {
        Specification<LoginLog> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(accountNo)) {
                predicates.add(cb.equal(root.get("accountNo"), accountNo));
            }
            if (StringUtils.hasText(loginType)) {
                predicates.add(cb.equal(root.get("loginType"), loginType));
            }
            if (success != null) {
                predicates.add(cb.equal(root.get("success"), success));
            }
            if (beginDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("loginTime"), beginDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("loginTime"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
        Page<LoginLog> page = loginLogRepository.findAll(spec, pageOf(pageNum, pageSize, "loginTime"));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    @Operation(summary = "操作日志分页（按模块/操作人/成败/时间段过滤）")
    @GetMapping("/oper")
    public R<PageResult<OperLog>> operLogs(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "20") int pageSize,
                                           @RequestParam(required = false) String module,
                                           @RequestParam(required = false) String operator,
                                           @RequestParam(required = false) Boolean success,
                                           @RequestParam(required = false) LocalDate beginDate,
                                           @RequestParam(required = false) LocalDate endDate) {
        Specification<OperLog> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (StringUtils.hasText(operator)) {
                predicates.add(cb.equal(root.get("operator"), operator));
            }
            if (success != null) {
                predicates.add(cb.equal(root.get("success"), success));
            }
            if (beginDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), beginDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThan(root.get("createTime"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
        Page<OperLog> page = operLogRepository.findAll(spec, pageOf(pageNum, pageSize, "createTime"));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    private PageRequest pageOf(int pageNum, int pageSize, String timeField) {
        int size = Math.min(Math.max(pageSize, 1), 200);
        return PageRequest.of(Math.max(pageNum, 1) - 1, size, Sort.by(Sort.Direction.DESC, timeField));
    }
}
