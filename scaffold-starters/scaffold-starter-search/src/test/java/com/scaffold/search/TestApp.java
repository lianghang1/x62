package com.scaffold.search;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 测试应用：只开自动配置（含 DataSource / JdbcTemplate / Bean Searcher / scaffold-search），
 * 不做组件扫描。
 */
@Configuration
@EnableAutoConfiguration
public class TestApp {
}
