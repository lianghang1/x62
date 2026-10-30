package com.scaffold.starter.test;

import com.scaffold.common.exception.BizException;
import com.scaffold.common.exception.GlobalErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * Web 切片自证测试的被测 Controller（顶层类，模拟业务方真实形态）。
 *
 * @author lianghang
 */
@RestController
public class DemoController {

    /** 非 R 包装的裸端点：验证切片不破坏文件流/ping 类响应。 */
    @GetMapping("/demo/ok")
    public String ok() {
        return "pong";
    }

    @GetMapping("/demo/biz-error")
    public String bizError() {
        throw BizException.of(GlobalErrorCode.FORBIDDEN, "无权访问该资源");
    }

    @PostMapping("/demo/create")
    public String create(@Valid @RequestBody DemoCreateForm form) {
        return form.getName();
    }
}
