package com.scaffold.starter.test;

import javax.validation.constraints.NotBlank;

/**
 * Web 切片自证测试的校验请求体。
 *
 * @author lianghang
 */
public class DemoCreateForm {

    @NotBlank(message = "名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
