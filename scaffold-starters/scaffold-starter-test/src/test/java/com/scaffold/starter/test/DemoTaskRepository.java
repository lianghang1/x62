package com.scaffold.starter.test;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 数据切片自证测试的演示 Repository。
 *
 * @author lianghang
 */
public interface DemoTaskRepository extends JpaRepository<DemoTask, Long> {
}
