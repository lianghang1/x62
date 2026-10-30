# scaffold —— JDK8 Spring Boot 多模块脚手架

> 基线：JDK 1.8 · Spring Boot 2.7.18 · Spring Data JPA · Spring Security + 自研 Redis 会话（无 JWT）· Redis · Caffeine · knife4j
>
> 定位：内部管理端（/internal）+ 三方开放 API（/openapi）+ 线上 C 端（/online）三入口统一账户体系。

## 模块结构

```
scaffold/
├── scaffold-dependencies/    BOM：三方依赖版本唯一出口
├── scaffold-core/            统一响应 R / PageResult / 错误码分段 / BizException / 工具类（hutool 底座 + JsonUtils/MaskUtils/BizAssert/IpUtils）
├── scaffold-starters/        技术组件（全部可插拔，自动装配）
│   ├── starter-web           traceId 全链路 / 全局异常 / 访问日志 / Jackson / CORS / knife4j
│   ├── starter-data-jpa      雪花 ID / 审计基类 BaseEntity / 乐观锁 VersionedEntity / EncryptedStringConverter
│   ├── starter-security      三入口安全链 + token 会话(Redis) + TTL 上下文 + openapi HMAC 签名
│   ├── starter-cache         Caffeine L1 + Redis L2 两级缓存
│   ├── starter-protection    Lua 原子限流 / @Idempotent 幂等 / @DistributedLock
│   ├── starter-job           AbstractJob 继承体系 + 执行日志 + 分布式锁
│   ├── starter-crypto        敏感字段加密：AES-256-GCM（默认）/ SM4 国密 + SM3 摘要
│   ├── starter-powerjob      PowerJob 执行器适配（AbstractJob 零改动接入）
│   ├── starter-excel         EasyExcel 导入导出
│   ├── starter-storage       x-file-storage 封装（30+ 存储平台配置化切换）
│   ├── starter-message       短信(sms4j 可选) + 邮件，业务只面向 NotificationService
│   ├── starter-http          出站 HTTP 客户端（okhttp）：超时/幂等重试/traceId 透传/签名 SPI
│   ├── starter-push          SSE 实时推送：连接注册表 + 心跳 + Redis 多实例转发，认证复用统一账户
│   └── starter-ws            WebSocket 双向通道（默认关闭）：握手复用 token 会话，与 SSE 同一推送体系
├── scaffold-modules/
│   └── module-system         统一账户(sys_account: MEMBER/OPERATOR/APP) + RBAC + 字典 + 三方应用管理
│                             （api/biz 分离：其他业务模块只依赖 system-api）
└── scaffold-server           启动模块 + 三入口订单 demo
```

## 快速启动（dev 零依赖：H2 内存库）

```bash
# Redis 仍需（安全/缓存/限流），本地起一个即可；完整依赖见 deploy/docker-compose-deps.yml
mvn clean package -DskipTests
java -jar scaffold-server/target/scaffold-server.jar
# 或开发模式：mvn -pl scaffold-server spring-boot:run
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

## 安全与审计

| 能力 | 用法 | 出处 |
|---|---|---|
| 登录日志 | 每次登录（成功/失败）自动落 `sys_login_log`（IP/UA/失败原因） | module-system，AuthService |
| 失败锁定 | 同账号连续失败 5 次锁 15 分钟（Redis 计数），`scaffold.security.password-policy` 可调 | starter-security |
| 密码强度 | 创建账号/改密时调 `PasswordPolicyValidator.validate(raw)` | starter-security |
| 操作日志 | controller 方法加 `@OperLog(module="xx", action="xx")`，自动落 `sys_oper_log`（操作人/入参/耗时/成败） | module-system |
| JSON 脱敏 | DTO 字段加 `@Desensitize(DesensitizeType.PHONE)`，序列化时自动打码（手机/邮箱/身份证/银行卡/姓名/自定义） | starter-web |
| XSS 过滤 | 默认开启：参数与 body 尖括号转义；富文本接口用 `scaffold.web.xss.exclude-urls` 排除 | starter-web |
| CI | GitHub Actions：push/PR 自动 JDK8 全量构建 + 测试（.github/workflows/ci.yml） | 工程化 |

## 国产化（信创）适配点

- 数据库：JPA 方言层换库（达梦/金仓/openGauss），业务零改动；`scaffold.data.worker-id` 多节点区分；
- 达梦大小写敏感：连接串 `CASE_SENSITIVE=0` 或建表统一小写；建表统一走 Liquibase changeset；
- 调度：PowerJob（Server 为 JPA 架构，与脚手架同栈适配达梦）双引擎之一，见上文接入三步；
- 国密：SM4 存储加密 / SM3 摘要已实装（starter-crypto，BouncyCastle），`scaffold.crypto.type=SM4` 一键切换。
