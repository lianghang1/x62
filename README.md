# scaffold —— JDK8 Spring Boot 多模块脚手架

> 基线：JDK 1.8 · Spring Boot 2.7.18 · Spring Data JPA · Spring Security + 自研 Redis 会话（无 JWT）· Redis · Caffeine · knife4j
>
> 定位：内部管理端（/internal）+ 三方开放 API（/openapi）+ 线上 C 端（/online）三入口统一账户体系。

## 模块结构

```
scaffold/
├── scaffold-dependencies/    BOM：三方依赖版本唯一出口
├── scaffold-common/          公共地基（纯工具层，非 starter）：全局常量(traceId/指标名契约) / R / PageResult / 错误码分段 / BizException / 域工具类（excel/json/mask/ip）
├── scaffold-starters/        技术组件（全部可插拔，自动装配）
│   ├── starter-web           traceId 全链路 / 全局异常 / 访问日志 / Jackson / CORS / knife4j
│   ├── starter-data-jpa      雪花 ID / 审计基类 BaseEntity / 乐观锁 VersionedEntity / EncryptedStringConverter
│   ├── starter-search        动态检索：Bean Searcher 底座 + SearchFilter/QueryUtils 门面 + QueryPlan 联表计划
│   ├── starter-security      三入口安全链 + token 会话(Redis) + TTL 上下文 + openapi HMAC 签名
│   ├── starter-cache         Caffeine L1 + Redis L2 两级缓存
│   ├── starter-protection    Lua 原子限流 / @Idempotent 幂等 / @DistributedLock
│   ├── starter-job           AbstractJob 继承体系 + 执行日志 + 分布式锁
│   ├── starter-crypto        敏感字段加密：AES-256-GCM（默认）/ SM4 国密 + SM3 摘要
│   ├── starter-powerjob      PowerJob 执行器适配（AbstractJob 零改动接入）
│   ├── starter-storage       x-file-storage 封装（30+ 存储平台配置化切换）
│   ├── starter-message       短信(sms4j 可选) + 邮件，业务只面向 NotificationService
│   ├── starter-http          出站 HTTP 客户端（okhttp）：超时/幂等重试/traceId 透传/签名 SPI
│   ├── starter-push          SSE 实时推送：连接注册表 + 心跳 + Redis 多实例转发，认证复用统一账户
│   ├── starter-ws            WebSocket 双向通道（默认关闭）：握手复用 token 会话，与 SSE 同一推送体系
│   ├── starter-observability Micrometer + Prometheus：公共标签 / 端点默认暴露 / 内建埋点（job/防刷/登录失败）
│   └── starter-test          统一测试底座：@ScaffoldWebSlice / @ScaffoldDataSlice / MockMvc JSON 助手 / @WithAuditor
├── scaffold-modules/
│   └── module-system         统一账户(sys_account: MEMBER/OPERATOR/APP) + RBAC + 字典 + 三方应用管理
│                             （api/biz 分离：其他业务模块只依赖 system-api）
├── scaffold-server           完整版启动模块 + 三入口订单 demo
└── scaffold-server-lite      轻量版：单机零依赖（无 DB/无 Redis），本地跑任务与轻量接口
```

## 受支持装配（两个发行版，同一份代码）

**新项目起步建议从 lite 开始**：clone 后只面对 web + data-jpa 最小骨架，业务模块按需生长；
需要登录/管理后台时把 module-system 与对应 starter 加回依赖即可升级为完整版，代码零改动。

| | scaffold-server（完整版） | scaffold-server-lite（轻量版） |
|---|---|---|
| 场景 | 全功能业务系统（含管理后台支撑） | **新项目干净起步** / 本地跑数据 / 边缘节点 |
| 外部依赖 | H2(dev)/MySQL + Redis | **零**（H2 内存库内置，改一行接 MySQL） |
| 数据访问 | JPA + 雪花主键 + 审计字段 + 字段加密 | JPA + 雪花主键 + 审计字段（ddl-auto 建表） |
| 任务 | @Job 调度 + job_run_log 落库 + Redis 分布式锁 | @Job 调度，日志走应用日志，无锁 |
| 账户/安全/字典/配置中心/审计日志 | 全量（module-system） | 无（不在类路径，源码也不进依赖树） |
| 启动 | `java -jar scaffold-server.jar` | `java -jar scaffold-server-lite.jar`（秒级启动） |

> 架构不变量：**每个 starter 必须可脱离全家桶独立使用**——lite 就是这条不变量的验收装配。
> lite 无 security：接口匿名可访问，对外暴露请引 starter-security 接鉴权或仅绑定内网地址。
> 纯框架用法：任何 Spring Boot 项目引 `scaffold-dependencies` BOM 后按需挑选 starters，不需要 server。

## 快速启动（dev 零依赖：H2 内存库）

```bash
# Redis 仍需（安全/缓存/限流），本地起一个即可；完整依赖见 deploy/docker-compose-deps.yml
mvn clean package -DskipTests
java -jar scaffold-server/target/scaffold-server.jar
# 或开发模式：mvn -pl scaffold-server spring-boot:run
```

## 单机完整部署（full 版一键起）

```bash
# 前提：已产出可执行 jar；Redis + MySQL + server 三容器
docker compose -f deploy/docker-compose-full.yml up -d --build
curl http://localhost:8080/ping
```

## 三入口验证

| 入口 | 路径 | 认证 | 演示接口 |
|---|---|---|---|
| internal | `/internal/**` | token 会话（登录换 token） | `POST /internal/auth/login`（username/password）→ `GET /internal/orders` |
| openapi | `/openapi/**` | HMAC 签名（X-App-Key/Timestamp/Nonce/Sign） | `POST /openapi/orders`（scope: order:create） |
| online | `/online/**` | 匿名 + 可选 token | `GET /online/ping`、`POST /online/auth/login?provider=wechat-mp&code=x` |

openapi 签名算法（见 `SignUtils`）：

```
bodyHash = SHA-256(body)
signBase = appKey \n timestamp \n nonce \n bodyHash
sign     = HMAC-SHA256(signBase, appSecret)  # hex
```

先在 `POST /internal/apps` 创建三方应用拿 appKey/appSecret。

## 关键规约（详见 /workspace/脚手架最终实施计划.md）

1. 业务层拿身份唯一入口：`LoginContext.current()`（TTL 底座，线程池透传不串号）；
2. Entity↔DTO 手写 Converter，禁用 MapStruct/BeanUtils 反射拷贝；
3. 跨模块只依赖对方 api 子模块（biz 互引禁止）；厂商 SDK 只进适配子模块；
4. 生产/信创环境 `ddl-auto=none`，建表走 Liquibase；密钥全部环境变量（.env.example）；
5. 调度引擎可切换：`scaffold.job.engine=simple|none`，接 PowerJob 时业务类零改动。

## 工具类底座（统一入口，机器强制）

字符串/集合原子操作收敛到唯一入口，**import 唯一性由 ArchUnit 规则在构建期强制**（违规直接构建失败）：

```java
import com.scaffold.common.util.StringUtils;   // 字符串唯一入口：继承 lang3 全量方法 + camelToSnake/snakeToCamel
import com.scaffold.common.util.CollUtils;      // 集合唯一入口：组合门面（判空/交并差集/partition/不可变集合）

StringUtils.isBlank(name);                      // 底层 lang3，写法无感
StringUtils.defaultIfEmpty(val, "-");
CollUtils.intersection(setA, setB);             // 底层 guava Sets
CollUtils.partition(bigList, 500);              // 批量入库分批
```

- **禁用来源**（ArchUnit 规则，主代码）：`org.apache.commons.lang3.StringUtils`、`org.springframework.util.StringUtils`、hutool `StrUtil`/`CollUtil`；
- **guava 高级结构**（BiMap/Multimap/Table/Range）直接 import 原生类，门面只收 80% 高频；
- **业务模块接入自查**：引 starter-test 后写一行测试调 `ScaffoldArchRules.checkMainClasses()`（common 模块已内置 `ArchConformanceTest`）；
- **自研准则**：只有脚手架专属语义才建工具类（MaskUtils/TreeUtils/SerialOps 先例），纯转发 lang3/guava 的一律不加；
- **hutool 唯一合法存在**：`hutool-captcha`（领域库，图像渲染不在 lang3/guava 范畴），禁 hutool-all。

## 敏感字段加密（starter-crypto）

实体字段一键接入，AES-256-GCM（默认）与 SM4 国密可配：

```java
@Convert(converter = EncryptedStringConverter.class)   // starter-data-jpa 提供
@Column(name = "phone", length = 64)                   // 列长按密文放宽
private String phone;
```

```yaml
scaffold:
  crypto:
    type: SM4                              # 信创环境切国密；默认 AES
    key: ${SCAFFOLD_CRYPTO_KEY}            # 必须环境变量注入，默认值仅为本地联调
```

`Sm3DigestUtils` 提供 SM3 国密摘要（密码哈希/报文摘要备选）。密文格式 Base64(随机 IV || 密文)，同明文两次加密结果不同。

## 动态任务管理（simple 引擎）

任务默认按 `@Job` 注解的 cron 跑；`sys_job` 表作为**可选覆盖层**（表配置 > 代码默认 > 无库纯代码驱动），管理接口（internal 端）：

| 接口 | 行为 |
|------|------|
| `GET /internal/jobs` | 列表：注册表快照与表配置合并视图（含仅落表的孤儿配置） |
| `POST /internal/jobs/{jobKey}/config?cron=&enabled=&remark=` | 改 cron/启停：落表 + **动态刷新调度，不重启**（cron 先经合法性校验） |
| `POST /internal/jobs/{jobKey}/trigger` | 立即触发一次（锁/日志语义与定时一致） |
| `GET /internal/jobs/run-logs?jobName=&pageNum=` | 执行日志分页（job_run_log，最新在前） |

powerjob 引擎（`engine=none`）运行时操作不可用，任务管理走 PowerJob Server 控制台，执行日志查询两引擎通用。

## 角色管理

`POST /internal/roles`（创建，编码唯一）/ `POST /internal/roles/{id}/update`（名称/权限串）/ `POST /internal/roles/{id}/delete`（有账号绑定时拒绝）/ `GET /internal/roles`。写操作全部 `@OperLog` 留痕。

## dev 种子账号

dev 是 H2 内存库且默认关 Liquibase，每次启动空库——`DevDataInitializer` 在 admin 不存在时自动创建 `admin/admin123`（仅 dev profile + Liquibase 关闭时生效，生产走 03-seed-data changeset）。

## 接入 PowerJob（三步）

1. 配置 `scaffold.powerjob.enabled=true` + worker 连接（prod profile 已给模板）：
   ```yaml
   powerjob:
     worker:
       enabled: true
       server-address: 127.0.0.1:7700      # 调度中心，deploy/docker-compose-powerjob.yml
       app-name: scaffold
   ```
2. 停掉内置调度：`scaffold.job.engine=none`（AbstractJob 执行日志/分布式锁能力保留）；
3. PowerJob 控制台建任务：处理器全限定类名填
   `com.scaffold.starter.powerjob.PowerJobJobAdapter`，任务参数填 `{"jobName":"对账任务"}`
   （与 `@Job.name` 一致）。业务任务类零改动，同一份代码可在 simple/PowerJob 两种引擎间切换。

## 出站 HTTP 客户端（starter-http）

```java
@Autowired private ScaffoldHttpClient http;

// GET（幂等，网络类失败自动重试）、POST JSON（不重试，防重复下单）
TransportResponse resp = http.get("https://api.third.com/pay/query");
TransportResponse created = http.postJson("https://api.third.com/order", orderDto);
```

- 实现基于 okhttp；自定义 `HttpTransport` Bean 可整体替换；
- 自动携带 `X-Trace-Id` 出站（跨系统排障）；错误分类 CONNECT/TIMEOUT/RETRYABLE_IO，收敛为 TransportException；
- 对接三方签名：实现 `RequestSigner` 注册为 Bean 即全局生效（内置 HMAC 签名器参考 openapi 协议）；
- 【规约】禁止在数据库事务内发起 HTTP 外呼——事务内等待三方会长时间占用连接池。

## 实时推送（starter-push / starter-ws）

**SSE（默认启用）**：前端连 `/online/subscribe`（Authorization 头带 token 会话），服务端一行推送：

```java
@Autowired private PushService push;

push.pushToUser(accountId, "order.paid", Map.of("orderId", "D1"));  // 定向
push.broadcast("notice", "系统维护通知");                             // 广播
```

- 心跳 `hb` 事件每 30s（`scaffold.push.heartbeat-seconds`），防代理掐断空闲连接；
- 多实例部署：`scaffold.push.multi-instance: true`（Redis pub/sub 扇出，各节点本机投递）；
- 断连自动清理；推送是「实时加速」，必达消息请以落库 + 轮询/补偿为准。

**WebSocket（默认关闭，需要双向通信时开启）**：

```yaml
scaffold.ws.enabled: true   # 端点 /online/ws?token=xxx，与 SSE 共用 PushService
```

上下行协议统一为 `PushMessage` JSON；业务收上行实现 `WsMessageListener`，发下行仍走 `PushService`——两通道并列，业务代码零改动。

## Liquibase 建表（生产/信创）

- `scaffold-server/src/main/resources/db/changelog/`：master + v1.0.0 changeset（全部实体表 + 种子管理员），
  跨方言通用（MySQL / 达梦 / 金仓 / openGauss），生产 profile 已启用（dev 用 H2 + ddl-auto）；
- 新增表：新建 changeset 文件并在 master include，禁止改历史 changeset；
- 达梦需 Liquibase 扩展：优先达梦安装包自带 liquibase-dm jar（install 到内网私服），
  连接串加 `CASE_SENSITIVE=0`；种子管理员 `admin/admin123` 仅联调用，交付前必须改密。

## 多数据源动态切换（starter-data-jpa）

- **默认关闭**：`scaffold.datasource.dynamic.enabled=true` 才装配；启用后整体接管数据源
  （Boot 按 `spring.datasource.*` 建默认库的逻辑自动退避），单库场景不要开启；
- 配置主库与各库连接（每库独立 Hikari 连接池）：
  ```yaml
  scaffold:
    datasource:
      dynamic:
        enabled: true
        primary: master            # 无注解上下文时路由到这里，必须是 datasources 中的 key
        datasources:
          master:
            url: jdbc:mysql://localhost:3306/main
            username: root
            password: xxx
          slave:
            url: jdbc:mysql://localhost:3306/report
            username: root
            password: xxx
  ```
- 用法：方法或类标注 `@TargetDataSource("slave")`，方法优先于类；注解调用退出后自动恢复外层数据源；
- 切面为最高优先级——先于事务拦截器执行，事务内的连接获取已按注解路由；
  **同一事务内不可再切换**（连接在事务开始时绑定，这是 JDBC 事务模型的边界，不是组件限制）；
- fail-fast：注解 key 未在 `datasources` 中配置直接抛异常，不静默回落主库掩盖配置错误；
- 异步安全：数据源上下文走 TransmittableThreadLocal，经 `ScaffoldExecutors`/`@Async` 装饰的线程池照样取得到提交时刻的路由意图。

## 数据权限（starter-data-jpa）

- 用法：服务方法或类标注 `@DataScope(DataScopeType.SELF)`（仅本人）/ `ALL`（不过滤）；
- SELF 通过 Hibernate Filter 在 SQL 层追加 `create_by = 当前审计人`——**实体按需白名单加入**：
  实体类标注 `@Filter(name = DataScopeAspect.FILTER_SELF)` 的才参与过滤，不标注的完全不受影响；
- 当前审计人即登录账号（`ScaffoldAuditorHolder`，security 层写入），data-jpa 不依赖 security；
- fail-closed：SELF 且无登录态直接抛异常，绝不静默放全量；
- 事务自动兜底：方法在事务内则复用，否则切面自动开启（REQUIRED）——Filter 必须挂在贯穿全部查询的
  Session 上，这一步不依赖使用者的 `@Transactional` 习惯，与事务注解的先后顺序无关；
- 已知边界（Hibernate Filter 机制）：对 HQL/Criteria 查询生效，对 `em.find()` 按主键直达与原生 SQL
  不生效——按 id 越权访问须另行校验；
- 部门等组织维度待组织实体模型落地后，以同样的 FilterDef + 切面方式扩展，使用方式不变。

## 业务侧多线程规约（starter-web 预置池）

框架预置两个线程池，业务**直接注入使用，禁止自建**（自建必须经 `ScaffoldExecutors.decorate` 包装，否则登录人/审计人/traceId 全丢）：

| 池 | Bean 名 | 用途 | 配置前缀 |
|---|---|---|---|
| 业务并行池 | `scaffoldBizExecutor` | service 方法内并行任务：注入后 `submit`/`invokeAll` | `scaffold.executor.biz.*` |
| @Async 专属池 | `scaffoldAsyncExecutor` | `@Async` 发后不管型异步（经 AsyncConfigurer 显式绑定，不依赖 Boot 默认池） | `scaffold.executor.async.*` |

两者均已预置：TTL 透传（登录人/审计人/数据源路由）+ MDC traceId、**有界队列**、`CallerRunsPolicy`（打满回落提交线程，背压不丢任务）、优雅关闭。业务定义同名 Bean 即整体接管；`scaffold.executor.biz.enabled=false` 可整体关闭业务池。

```java
// 标准三步：注入 → 提交 → 汇总
@Autowired
private ThreadPoolTaskExecutor scaffoldBizExecutor;

public ReportVO buildReport(Long orderId) {
    Future<Order> order = scaffoldBizExecutor.submit(() -> orderRepository.findById(orderId).orElseThrow());
    Future<UserVO> user = scaffoldBizExecutor.submit(() -> userClient.load(orderId));
    return new ReportVO(order.get(5, TimeUnit.SECONDS), user.get(5, TimeUnit.SECONDS));
}
```

三条硬边界：

1. **事务不透传**——子线程各拿各的连接，与提交线程不在同一事务；需要事务的子任务单独标 `@Transactional`。同一事务内也不可切换数据源（见多数据源章节）。
2. **`Future.get` 必须设超时**——不设超时的并行等待是挂死高发点。
3. **异常收口**——`get` 会把子任务异常包成 `ExecutionException` 抛回；发后不管型任务体内自行 try-catch（`@Async` void 方法由 AsyncExceptionHandler 统一记录）。

> 设计说明：不依赖 Boot 默认 applicationTaskExecutor——它带 `@ConditionalOnMissingBean(Executor.class)`，classpath 里出现任意 Executor Bean（如我们的业务池）它就退避，依赖它等于依赖装配顺序。所以 `@Async` 走显式绑定的 `scaffoldAsyncExecutor`。

## 动态检索（starter-search）

底层 Bean Searcher，门面保留 db-spring QueryUtils 写法；`SpringSqlExecutor` 默认感知 Spring 事务（`@Transactional` 内自动复用事务连接），联表/分页保护/防注入由引擎承担。

**方式一：SearchFilter + QueryUtils（后台编程式）**

```java
List<SearchFilter> filters = new ArrayList<>();
filters.add(new SearchFilter("status", Operator.EQ, 1));
filters.add(new SearchFilter(UserVO::getName, Operator.CN, "张"));   // lambda 字段，重构安全
filters.add(SearchFilter.or(                                        // OR 分组
        new SearchFilter("deptId", Operator.EQ, 1L),
        new SearchFilter("deptId", Operator.EQ, 2L)));

Page<UserVO> page = QueryUtils.doQuery(filters, PageRequest.of(0, 15, Sort.by(desc("age"))), UserVO.class);
List<UserVO> list = QueryUtils.doListQuery(filters, UserVO.class);
long count       = QueryUtils.doCount(filters, UserVO.class);
```

运算符（对齐 db-spring Operator 命名）：EQ/NE/GT/GE/LT/LE、IN/NI、CN/NC（like %x%）、LL/RL（like x%/%x）、NU/NN（is null）、BT/NBT（between，值传两元素集合）、BLANK/NBLANK。

**方式二：QueryPlan 命名联表计划（注解声明，运行时合成元数据）**

```java
@QueryPlan(name = "userPlan", returnType = DeptUserSummary.class)   // returnType 缺省直接返回计划类
@Table(name = "t_user", alias = "u",
        filters = @Table.Fixed(name = "status", op = Operator.EQ, value = "1", type = Integer.class))
@JoinTable(name = "t_dept", alias = "d",
        columns = @JoinColumn(name = "id", referencedColumnName = "dept_id"))  // d.id = u.dept_id
public class UserPlan {
    @Column("u.name")       private String name;
    @Column("d.dept_name")  private String deptName;   // 未标注的字段不参与 SQL
}

QueryUtils.doQuery("userPlan", filters, pageable);                 // 按名执行（注册表）
QueryUtils.doQuery(UserPlan.class, filters, pageable);             // 按类执行
```

- 固定条件 `@Table.Fixed` 总是生效，前端无法覆盖；`@JoinColumn` 支持 `sql()` 写自定义 ON 条件
- 计划类由 `scaffold.search.plan-packages` 扫描注册，或手动 `QueryPlanRegistry.register(Class)`

**方式三：原生 Bean Searcher（前端参数驱动，可选）**

直接注入 `BeanSearcher`/`MapSearcher`，前端传 `?status=1&name_ct=张&age_gt=25&page=0&size=15`，后端一行 `beanSearcher.search(UserVO.class)`。三种方式共用同一引擎与事务。

## 安全与审计

| 能力 | 用法 | 出处 |
|---|---|---|
| 登录日志 | 每次登录（成功/失败）自动落 `sys_login_log`（IP/UA/失败原因），异步落库不拖慢主流程；查询 `GET /internal/system/audit/login`（账号/成败/时间段过滤，时间倒序分页） | module-system，AuthService→AuditLogListener |
| 失败锁定 | 同账号连续失败 5 次锁 15 分钟（Redis 计数），`scaffold.security.password-policy` 可调 | starter-security |
| IP 防撞库 | 同 IP 窗口内登录失败达上限拒绝该 IP 登录，`scaffold.security.ip-guard` 可调 | starter-security |
| 计算型验证码 | internal 登录防机器撞库：算式 GIF（如 3+5=?），Redis 存算式、校验时求值比对，OCR 无法直读答案；`scaffold.security.captcha.mode: off/always/after_fail`（默认连续失败 3 次后强制），`GET /internal/auth/captcha` 生成 | starter-security |
| 短信防刷 | 发送前置四道闸：手机号 60s 间隔 / 手机号日限 10 / IP 日限 50 / 全局日配额 1000，`scaffold.message.sms-guard` 可调 | starter-message，SmsAbuseGuard |
| 防刷闸门 | 业务动作防刷（领券/投票等）：注入 `AbuseGuard` 调 `checkInterval/checkWindow/checkDaily`，超限抛 429 | starter-protection |
| 密码强度 | 创建账号/改密时调 `PasswordPolicyValidator.validate(raw)` | starter-security |
| 操作日志 | controller 方法加 `@OperLog(module="xx", action="xx")`，事务提交后异步落 `sys_oper_log`（操作人/入参/耗时/成败，回滚不发）；查询 `GET /internal/system/audit/oper` | module-system，OperLogAspect→AuditLogListener |
| JSON 脱敏 | DTO 字段加 `@Desensitize(DesensitizeType.PHONE)`，序列化时自动打码（手机/邮箱/身份证/银行卡/姓名/自定义） | starter-web |
| XSS 过滤 | 默认开启：参数与 body 尖括号转义；富文本接口用 `scaffold.web.xss.exclude-urls` 排除 | starter-web |
| 接口加解密 | 方法/类加 `@ApiEncrypt`：请求体密文进、响应体（含 R 包装）密文出，经 `DataCipher`（AES/SM4）整包加解密，适合整包防窃听的敏感接口 | starter-security |

## 运行时配置与事件总线

- **DB 配置中心**（sys_config，改参数不重启生效）：
  ```java
  // 跨模块读取统一走 ConfigApi（进程内缓存，零回源开销）
  configApi.getValue("upload.max-size-mb");   // "20"
  configApi.getIntValue("password.policy.min-length");
  configApi.getBoolValue("feature.xxx");
  ```
  管理端：`GET /internal/system/config/page`（分页）、`POST /internal/system/config`
  （保存即刷新缓存，并经 Redis pub/sub 广播 `scaffold:config:refresh`，多实例回源被改键）；
  无 Redis 时单实例形态完整可用；
- **事件总线 EventOps**（common，进程内解耦主链路，替代 MQ 的 90% 场景）：
  ```java
  EventOps.publish(event);             // 立即发布
  EventOps.publishAfterCommit(event);  // 事务提交后才发，回滚不发
  // 消费方：@Async @EventListener（走 web starter 的 applicationTaskExecutor，含 traceId/TTL 装饰器）
  ```
  登录日志/操作日志落库即基于此——生产方请求线程取齐上下文后发事件，监听器异步写库；
- **雪花 ID IdOps**：主键之外的取号场景（对外单号/消息幂等号）`IdOps.nextId()`，
  与 JPA 主键共用同一 Snowflake 单例（workerId 由 `scaffold.data.worker-id/datacenter-id` 配置）；
- **业务单号 SerialOps**（starter-redis，与 IdOps 互补：雪花给机器看、单号给人看）：
  ```java
  serialOps.next("ORD");    // → ORD20261031-0001（前缀 + 日期段 + 4 位序列）
  serialOps.next("PAY", 6); // → PAY20261031-000001（自定义位宽，超出自然变长不截断）
  ```
  Redis 原子自增多实例不重号；序列 key 内嵌日期段跨日归零，首号自动补 48h 兜底 TTL；
- **树装配 TreeUtils**（common）：扁平 List → 树，实体零侵入（函数式取 id/parentId/挂 children）：
  `TreeUtils.build(list, Node::getId, Node::getParentId, 0L, Node::setChildren)`；
  叶子 children 恒为空 List；孤儿节点（父不存在）提升为根不丢数据；id 重复 fail-fast；
- **日期工具 DateUtils**：全局格式唯一出口，新增 `Date↔LocalDateTime` 互转、
  `dayBalanceSeconds()`（当天剩余秒，"当日有效"过期标准算法）、
  `formatCompact()`（yyyyMMddHHmmss 流水号）、`formatDateCompact()`（yyyyMMdd 日期段）、
  `chineseWeek()`（中文星期）；
- **@Async 异常兜底**：web starter 注册 `AsyncUncaughtExceptionHandler`（方法+参数+异常统一记录），
  `@EnableAsync` 引入 starter-web 即开启。

## 可观测性（starter-observability）

- **指标端点**：引依赖即得 `/actuator/prometheus`（EnvironmentPostProcessor 默认暴露
  health/info/prometheus，用户 yml 写同名 key 整体覆盖）；生产环境请用网关/安全组限制
  actuator 外网访问；
- **公共标签**：所有指标自动带 `application=spring.application.name`
  （`scaffold.observability.application-name` 可覆盖），Prometheus 多应用聚合靠它区分；
- **内建埋点**（optional micrometer，observability 不在场自动静默）：
  | 指标 | 标签 | 出处 |
  |---|---|---|
  | `scaffold.job.executions` | job, outcome(success/fail) | starter-job |
  | `scaffold.job.duration` | job | starter-job |
  | `scaffold.protection.abuse.rejected` | scene, rule(interval/window/daily) | starter-protection |
  | `scaffold.security.login.failures` | dimension(account/ip) | starter-security |
- **业务自定义**：注入 `ScaffoldMetrics` 门面，`increment("scaffold.order.created", "channel", "app")`
  一行打点，命名规约 `scaffold.<模块>.<指标>`；
- lite 轻量版不含本 starter（零外部依赖形态不需要指标端点）。

## 统一测试底座（starter-test）

业务方以 **test scope** 引入本模块，替代散装样板：

```java
// Web 切片：零外部依赖，被测接口继承生产级横切层（traceId/统一异常/Jackson 规约）
@ScaffoldWebSlice(controllers = DemoController.class)
class DemoTest {
    @Autowired ScaffoldMockMvc mvc;   // 或 @Autowired MockMvc 自行包装

    @Test
    void ok() {
        mvc.get("/demo/ok").expectStatus(200);
    }
    @Test
    void bizError() {
        mvc.get("/demo/biz-error")
           .expectStatus(403).expectCode(40301).expectTraceId();
    }
}

// 数据切片：H2 内存库 + 雪花 ID + 审计人注入，每方法事务回滚
@ScaffoldDataSlice
@WithAuditor("tester")
class RepoTest {
    @Autowired DemoTaskRepository repo;
    @Test
    void audit() {
        assertEquals("tester", repo.save(new DemoTask()).getCreateBy());
    }
}
```

### 账号管理（internal 管理端）

| 接口 | 说明 |
|---|---|
| `GET /internal/accounts?accountNo=&type=&status=&pageNum=&pageSize=` | 分页查询（组合过滤，不含密码） |
| `POST /internal/accounts?accountNo=&password=` | 创建操作员（密码强度校验 + BCrypt） |
| `POST /internal/accounts/{id}/status?enable=` | 启停用（停用即全端下线） |
| `POST /internal/accounts/{id}/reset-password` | 重置密码（随机密码明文仅此一次返回 + 强制重登） |
| `POST /internal/auth/password?oldPassword=&newPassword=` | 改自己密码（验旧密 + 强度校验 + 全端下线） |
| `POST /internal/accounts/{id}/kick` | 踢人下线 |

写操作全部挂 `@OperLog` 审计；登录/验证码接口在 `scaffold.security.internal-whitelist`。

## 配置项总表（IDE 自动补全已就绪）

以下由 spring-boot-configuration-processor 自动生成（`scaffold.data.*` 为手工补充），IDEA 中编辑 yml 即有补全与文档提示；此表为静态快照，以元数据为准。

| 配置项 | 默认值 | 说明 | 出处 |
|---|---|---|---|
| `scaffold.cache.key-prefix` | `—` | Redis key 前缀 | cache |
| `scaffold.cache.l1-max-size` | `4096` | L1 最大条目数 | cache |
| `scaffold.cache.l1-ttl-seconds` | `60` | L1 过期时间（秒） | cache |
| `scaffold.cache.l2-ttl-seconds` | `3600` | L2 过期时间（秒） | cache |
| `scaffold.cache.type` | `two-level` | 缓存形态：two-level（默认）= Caffeine + Redis；redis = 纯 Redis | cache |
| `scaffold.crypto.key` | `—` | 加密口令（派生密钥用），必须环境变量注入，禁止硬编码 | crypto |
| `scaffold.crypto.type` | `AES` | 算法类型：AES（默认）| SM4（国密/信创） | crypto |
| `scaffold.data.datacenter-id` | `1` | 雪花算法 datacenterId（0-31）；多机房/多实例部署按环境区分 | data-jpa |
| `scaffold.data.worker-id` | `1` | 雪花算法 workerId（0-31）；单机默认 1，多实例部署必须按实例区分，否则主键可能重复 | data-jpa |
| `scaffold.datasource.dynamic.datasources.{name}.driver-class-name` | `—` | 该数据源驱动类名，缺省由连接串自动推断 | data-jpa |
| `scaffold.datasource.dynamic.datasources.{name}.password` | `—` | 该数据源密码 | data-jpa |
| `scaffold.datasource.dynamic.datasources.{name}.url` | `—` | 该数据源 JDBC 连接串 | data-jpa |
| `scaffold.datasource.dynamic.datasources.{name}.username` | `—` | 该数据源用户名 | data-jpa |
| `scaffold.datasource.dynamic.enabled` | `false` | 多数据源动态切换总开关；单库场景保持 false | data-jpa |
| `scaffold.datasource.dynamic.primary` | `master` | 默认数据源名，无 `@TargetDataSource` 上下文时路由到这里 | data-jpa |
| `scaffold.executor.async.core-size` | `8` | @Async 专属池核心线程数 | web |
| `scaffold.executor.async.max-size` | `16` | @Async 专属池最大线程数 | web |
| `scaffold.executor.async.queue-capacity` | `1000` | @Async 专属池有界队列容量 | web |
| `scaffold.executor.async.name-prefix` | `scaffold-async-` | @Async 专属池线程名前缀 | web |
| `scaffold.executor.async.await-termination-seconds` | `30` | 优雅关闭等待秒数 | web |
| `scaffold.executor.biz.enabled` | `true` | 业务并行池开关；业务同名 Bean 可整体接管 | web |
| `scaffold.executor.biz.core-size` | `4` | 业务并行池核心线程数 | web |
| `scaffold.executor.biz.max-size` | `8` | 业务并行池最大线程数 | web |
| `scaffold.executor.biz.queue-capacity` | `1000` | 业务并行池有界队列容量 | web |
| `scaffold.executor.biz.name-prefix` | `scaffold-biz-` | 业务并行池线程名前缀 | web |
| `scaffold.executor.biz.await-termination-seconds` | `30` | 优雅关闭等待秒数 | web |
| `scaffold.http.connect-timeout-ms` | `5000` | 建连超时（毫秒） | http |
| `scaffold.http.enabled` | `true` | starter 总开关 | http |
| `scaffold.http.read-timeout-ms` | `10000` | 读超时（毫秒） | http |
| `scaffold.http.retry-backoff-ms` | `500` | 重试退避间隔（毫秒） | http |
| `scaffold.http.retry-max-attempts` | `2` | 幂等请求最大尝试次数（含首次） | http |
| `scaffold.job.engine` | `simple` | simple = 本注册器；none = 接入外部调度平台 | job |
| `scaffold.job.pool-size` | `4` | 调度线程池大小 | job |
| `scaffold.message.sms-guard` | `—` | 短信防刷配置 | message |
| `scaffold.observability.application-name` | `—` | 公共标签 application 的取值；空则取 spring | observability |
| `scaffold.observability.enabled` | `true` | 总开关：false 时框架内建埋点全部静默（端点暴露不受此开关控制） | observability |
| `scaffold.push.enabled` | `true` | starter 总开关 | push |
| `scaffold.push.heartbeat-seconds` | `30` | 心跳间隔（秒；0 关闭心跳） | push |
| `scaffold.push.multi-instance` | `false` | 多实例转发：部署多于一个节点时必须开启（Redis pub/sub 扇出）， 否则只投递到「收到推送请求的那个节点」上的连接 | push |
| `scaffold.push.relay-topic` | `—` | Redis 转发频道名（多实例时各节点订阅同一频道） | push |
| `scaffold.push.sse-timeout-ms` | `0` | SSE emitter 超时（毫秒；0 表示永不超时，存活靠心跳） | push |
| `scaffold.search.enabled` | `true` | 动态检索总开关（QueryUtils/QueryPlan/Bean Searcher） | search |
| `scaffold.search.plan-packages` | `—` | QueryPlan 计划类扫描包（启动时注册到注册表） | search |
| `scaffold.security.captcha.fail-threshold` | `3` | after_fail 模式的触发阈值（连续失败次数） | security |
| `scaffold.security.captcha.mode` | `after_fail` | 模式：off=不启用 | always=每次都要 | after_fail=连续失败 failThreshold 次后强制 | security |
| `scaffold.security.captcha.ttl-seconds` | `120` | 验证码有效期（秒） | security |
| `scaffold.security.exclusive-login` | `false` | 同端互斥：登录是否踢掉同设备旧会话 | security |
| `scaffold.security.internal-whitelist` | `—` | internal 链白名单（如登录接口），相对 /internal/** | security |
| `scaffold.security.ip-guard.enabled` | `true` | 总开关；关闭后仅保留账号维度失败锁定 | security |
| `scaffold.security.ip-guard.fail-limit` | `30` | 同 IP 窗口内登录失败上限，达到后拒绝该 IP 的账密登录 | security |
| `scaffold.security.ip-guard.window-seconds` | `600` | 失败计数窗口（秒） | security |
| `scaffold.security.online-whitelist` | `—` | online 链白名单（整链默认 permitAll，此配置仅用于过滤器跳过解析） | security |
| `scaffold.security.openapi-ip-whitelist-check` | `false` | openapi 是否校验来源 IP 白名单（需 AppIdentity 提供） | security |
| `scaffold.security.openapi-timestamp-window-seconds` | `300` | openapi 时间戳窗口（秒，正负） | security |
| `scaffold.security.openapi-whitelist` | `—` | openapi 链白名单 | security |
| `scaffold.security.password-policy.lock-seconds` | `900` | 锁定时长（秒） | security |
| `scaffold.security.password-policy.max-fail-count` | `5` | 连续登录失败次数上限，达到即临时锁定 | security |
| `scaffold.security.password-policy.min-length` | `8` | 密码最小长度 | security |
| `scaffold.security.password-policy.require-letter-and-digit` | `true` | 是否必须同时包含字母和数字 | security |
| `scaffold.security.renew-threshold-percent` | `20` | 剩余有效期低于该百分比时自动续期 | security |
| `scaffold.security.token-ttl-seconds` | `0` | token 会话 TTL（秒），默认 7 天 | security |
| `scaffold.web.access-log-enabled` | `true` | 访问日志切面开关 | web |
| `scaffold.web.cors.allowed-headers` | `—` |  | web |
| `scaffold.web.cors.allowed-methods` | `—` |  | web |
| `scaffold.web.cors.allowed-origins` | `—` | 允许的跨域来源；为空表示不启用 CORS 配置（默认关闭，禁止全放开） | web |
| `scaffold.web.cors.max-age` | `1800` | 预检请求缓存时间（秒） | web |
| `scaffold.web.xss.enabled` | `true` | 是否启用 XSS 过滤 | web |
| `scaffold.web.xss.exclude-urls` | `—` | 排除路径（Ant 风格，如富文本编辑接口 /internal/rich/**） | web |
| `scaffold.ws.allowed-origins` | `*` | 允许的来源（跨域；* 表示不限制，生产建议收敛） | ws |
| `scaffold.ws.enabled` | `false` | starter 开关（默认关闭） | ws |
| `scaffold.ws.path` | `/online/ws` | 握手端点路径 | ws |

## 国产化（信创）适配点

- 数据库：JPA 方言层换库（达梦/金仓/openGauss），业务零改动；`scaffold.data.worker-id` 多节点区分；
- 达梦大小写敏感：连接串 `CASE_SENSITIVE=0` 或建表统一小写；建表统一走 Liquibase changeset；
- 调度：PowerJob（Server 为 JPA 架构，与脚手架同栈适配达梦）双引擎之一，见上文接入三步；
- 国密：SM4 存储加密 / SM3 摘要已实装（starter-crypto，BouncyCastle），`scaffold.crypto.type=SM4` 一键切换。
