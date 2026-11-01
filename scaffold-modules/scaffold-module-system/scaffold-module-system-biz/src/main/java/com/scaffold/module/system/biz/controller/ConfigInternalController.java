package com.scaffold.module.system.biz.controller;

import com.scaffold.module.system.api.annotation.OperLog;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.R;
import com.scaffold.module.system.biz.entity.SysConfig;
import com.scaffold.module.system.biz.repository.SysConfigRepository;
import com.scaffold.module.system.biz.service.ConfigServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.scaffold.common.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * internal 端系统参数配置管理接口。
 *
 * <p>分页查询 + 保存（新增/按 key 覆盖）；保存即刷新缓存并广播多实例失效，
 * 改参数不重启生效。写操作经 {@code @OperLog} 留痕（需 system:config:manage 权限）。</p>
 *
 * @author lianghang
 */
@Tag(name = "internal-系统配置")
@RestController
@RequestMapping("/internal/system/config")
public class ConfigInternalController {

    private final SysConfigRepository configRepository;
    private final ConfigServiceImpl configService;

    public ConfigInternalController(SysConfigRepository configRepository, ConfigServiceImpl configService) {
        this.configRepository = configRepository;
        this.configService = configService;
    }

    @Operation(summary = "配置分页查询（按 key/remark 模糊过滤）")
    @GetMapping("/page")
    public R<PageResult<SysConfig>> page(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "20") int pageSize,
                                         @RequestParam(required = false) String key,
                                         @RequestParam(required = false) Boolean enabled) {
        Specification<SysConfig> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (StringUtils.isNotBlank(key)) {
                predicates.add(cb.like(root.get("configKey"), "%" + key.trim().toLowerCase() + "%"));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
        Page<SysConfig> page = configRepository.findAll(spec,
                PageRequest.of(Math.max(pageNum, 1) - 1, Math.min(Math.max(pageSize, 1), 200),
                        Sort.by(Sort.Direction.ASC, "configKey")));
        return R.ok(PageResult.of(page.getContent(), page.getTotalElements(), pageNum, pageSize));
    }

    @Operation(summary = "保存配置（新增或按 key 覆盖，需 system:config:manage 权限）")
    @OperLog(module = "系统配置", action = "保存配置")
    @PostMapping
    public R<Long> save(@RequestBody SysConfig config) {
        return R.ok(configService.save(config));
    }
}
