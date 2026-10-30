package com.scaffold.starter.test;

import com.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 数据切片自证测试的演示实体（顶层类，模拟业务方真实形态）。
 *
 * @author lianghang
 */
@Entity
@Table(name = "t_demo_task")
public class DemoTask extends BaseEntity {

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
