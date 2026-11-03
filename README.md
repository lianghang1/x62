# boot-scaffold

内部 Spring Boot 脚手架（JDK 1.8 / Spring Boot 2.7.18）。框架仓库只产 jar，不含启动类；宿主由各项目仓库自行组装。

## 模块

```
boot-scaffold/
├── scaffold-parent           统一父 POM：版本 + 插件基线，外部项目继承它复用
├── scaffold-dependencies     BOM：全部依赖版本唯一出口
├── scaffold-common           公共工具（异常/JSON/日期/枚举等）
└── scaffold-starters/        能力积木，按需依赖，自动装配
    ├── starter-web           Web 基础：traceId / 全局异常 / 访问日志 / Jackson / knife4j
    ├── starter-log           日志基线
    ├── starter-ip            IP 归属地解析
    ├── starter-data-jpa      数据访问：BaseEntity / 雪花 ID / 乐观锁 / 多数据源 @TargetDataSource
    ├── starter-search        动态检索（Bean Searcher 封装）
    ├── starter-security      认证鉴权：token 会话(Redis) / 三入口安全链 / openapi 签名
    ├── starter-cache         Caffeine L1 + Redis L2 两级缓存
    ├── starter-redis         Redis 客户端封装
    ├── starter-protection    限流 / 幂等 / 分布式锁
    ├── starter-job           AbstractJob 定时任务体系（执行日志 / 分布式锁）
    ├── starter-powerjob      PowerJob 执行器适配
    ├── starter-crypto        敏感字段加密（AES-256-GCM / SM4）
    ├── starter-http          出站 HTTP（okhttp 封装：重试 / traceId 透传）
    ├── starter-storage       文件存储（x-file-storage 封装）
    ├── starter-message       短信 + 邮件
    ├── starter-push          SSE 实时推送
    ├── starter-ws            WebSocket 双向通道
    ├── starter-observability Micrometer + Prometheus 埋点
    ├── starter-excel         Excel 导入导出
    └── starter-test          统一测试底座
```

## 项目怎么用

项目仓库 pom 继承 parent，依赖零版本号：

```xml
<parent>
    <groupId>com.sfi.scaffold</groupId>
    <artifactId>scaffold-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
```

```xml
<dependency>
    <groupId>com.sfi.scaffold</groupId>
    <artifactId>scaffold-starter-web</artifactId>
</dependency>
```

业务代码包名用 `com.sfi.<项目名>.*`，与框架包隔离；启动类放包根，框架走自动装配不进扫描范围。

## 构建 / 发布

```bash
mvn clean test      # 全量测试
mvn deploy \
    -Dnexus.releases=http://<nexus>/repository/maven-releases/ \
    -Dnexus.snapshots=http://<nexus>/repository/maven-snapshots/
```

发布凭据配 `~/.m2/settings.xml`（server id：sfi-releases / sfi-snapshots）。发 release 前把三处 `1.0.0-SNAPSHOT` 改为正式版本（scaffold-parent / 根 pom / scaffold-dependencies）。

## 分支

- `main`：稳定线
- `data-link`：墓园数据采集端（cemlink，宿主形态开发中，后续迁入项目仓库）
- `feature/scaffold-parent`：本次重构线（parent 抽取 + com.sfi.scaffold 命名空间迁移）
