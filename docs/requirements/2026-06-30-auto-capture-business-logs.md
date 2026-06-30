# 自动采集业务日志到 XXL-JOB 执行日志需求

状态：`Draft`

创建日期：`2026-06-30`

## 原始需求记录

用户原始口径：

> 现在日志必须通过 `XxlJobHelper.log("XXL-JOB Boost, Hello World.");` 才能被 xxljob 记录。需要加个全局的配置，支持自动拦截 logback、slf4j 的 log，也能够自动被 xxljob 里面记录，方便直接在调度中心里面进行日志查询，省的再去看业务系统的日志。当然这个作为可选项的配置。同时要考虑日志膨胀的问题，因为 xxljob 的日志好像是存在数据库的。

## 现状澄清

当前执行日志写入路径：

- 任务执行时，`JobThread` 会创建 `XxlJobContext`，其中包含 `logFileName`。
- `XxlJobHelper.log(...)` 会通过 `XxlJobFileAppender.appendLog(...)` 追加到执行器本地日志文件。
- 调度中心的滚动日志接口会调用执行器 `ExecutorBiz.log(...)`，再由执行器读取本地日志文件返回。

相关代码：

- `xxl-job-core/src/main/java/com/xxl/job/core/thread/JobThread.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/context/XxlJobContext.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/context/XxlJobHelper.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/log/XxlJobFileAppender.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/openapi/impl/ExecutorBizImpl.java`
- `xxl-job-admin/src/main/java/com/xxl/job/admin/controller/biz/JobLogController.java`

注意：XXL-JOB 的 `xxl_job_log` 表主要保存调度日志元数据、触发状态、执行状态、消息摘要等；执行日志正文不是直接整段写入数据库，而是主要保存在执行器日志文件里，由调度中心查询时滚动拉取。

所以“日志膨胀”仍然需要重视，但重点不只是数据库膨胀，还包括：

- 执行器磁盘日志文件膨胀。
- 调度中心滚动拉取日志时响应过大。
- 高频业务日志影响任务执行性能。
- 敏感日志被展示到调度中心。

## 目标

提供一个可选配置，使业务代码里的 SLF4J / Logback 日志能自动进入当前 XXL-JOB 执行日志。

目标效果：

```java
private static final Logger log = LoggerFactory.getLogger(DemoJob.class);

@XxlJobBoost(value = "demoJobHandler", desc = "示例任务", author = "XXL")
public void demoJobHandler() {
    log.info("开始处理订单: {}", orderId);
}
```

开启配置后，在调度中心滚动日志里能看到这条业务日志，不再必须写：

```java
XxlJobHelper.log("开始处理订单: {}", orderId);
```

## 建议配置

建议放在执行器侧配置中，默认关闭：

```properties
xxl.job.executor.log-capture.enabled=false
xxl.job.executor.log-capture.level=INFO
xxl.job.executor.log-capture.max-event-length=4096
xxl.job.executor.log-capture.max-events-per-job=2000
xxl.job.executor.log-capture.include-packages=
xxl.job.executor.log-capture.exclude-packages=org.springframework.,spring.,com.zaxxer.hikari.
xxl.job.executor.log-capture.include-mdc=true
```

配置含义：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 是否启用业务日志自动采集 |
| `level` | `INFO` | 最低采集级别 |
| `max-event-length` | `4096` | 单条日志最大字符数，超出截断 |
| `max-events-per-job` | `2000` | 单次任务最多采集条数，避免无限膨胀 |
| `include-packages` | 空 | 非空时只采集这些包前缀 |
| `exclude-packages` | 常见框架包 | 排除框架噪音日志 |
| `include-mdc` | `true` | 是否输出 MDC |

## 实现方向

### 方案 A：Logback Appender

为 Logback 提供一个 `Appender`，在 append 时判断当前线程是否存在 `XxlJobContext`。

优点：

- 对 Spring Boot + Logback 用户最自然。
- 不需要改业务 logger 调用。
- 可以使用 Logback 原始事件信息，包括 level、loggerName、thread、MDC、throwable。

风险：

- 只覆盖 Logback，不覆盖所有 SLF4J 实现。
- Appender 注册方式要谨慎，避免重复注册和循环写入。

### 方案 B：SLF4J 层通用拦截

尝试在 SLF4J 层做通用 hook。

优点：

- 理论上更通用。

风险：

- SLF4J API 本身不是为全局拦截设计的。
- 不同 binding 行为不同，容易引入兼容性和维护成本。

建议优先做方案 A，先覆盖 Spring Boot 默认主流场景。后续如果有 Log4j2 需求，再补独立 appender。

## 推荐实现切片

### Slice 1：核心写入器

新增一个内部工具类，例如：

```text
xxl-job-core/src/main/java/com/xxl/job/core/log/XxlJobLogCaptureAppender.java
```

职责：

- 从当前线程读取 `XxlJobContext`。
- 格式化业务日志事件。
- 调用 `XxlJobFileAppender.appendLog(...)`。
- 执行长度截断、条数限制、包过滤、级别过滤。

### Slice 2：Spring Boot 自动配置

在 `xxl-job-adapter-spring-boot-starter` 中按配置自动注册 Logback appender。

约束：

- 只有 `log-capture.enabled=true` 才注册。
- 注册前检查是否已经注册，避免重复 appender。
- 如果当前日志系统不是 Logback，启动时只打 debug / warn，不应让应用启动失败。

### Slice 3：任务上下文与异步线程

当前 `XxlJobContext` 使用 `InheritableThreadLocal`，可以覆盖部分子线程场景，但线程池复用场景可能拿不到或拿到旧上下文。

第一阶段建议：

- 明确只保证任务执行线程及其新建子线程。
- 不承诺自动覆盖业务线程池。
- 后续如需线程池透传，再设计 task decorator / wrapper。

### Slice 4：样例与文档

在 Spring Boot sample 中增加一个使用普通 SLF4J logger 的任务示例，并在配置中展示如何开启。

## 膨胀与安全控制

必须考虑：

- 默认关闭。
- 单条截断。
- 单任务条数上限。
- 级别过滤。
- 包名前缀过滤。
- 排除框架包。
- 异常堆栈长度限制。
- 避免把 access token、password、secret 等敏感内容无控制地写进调度中心。

后续可以考虑：

- 增加敏感字段脱敏规则。
- 增加每秒采集速率限制。
- 增加任务级覆盖配置。

## 验收标准

- 默认关闭时，业务 SLF4J / Logback 日志不会进入 XXL-JOB 执行日志。
- 开启配置后，任务执行线程内的 `log.info/error(...)` 可以在调度中心滚动日志里看到。
- `XxlJobHelper.log(...)` 旧方式仍正常。
- 日志级别过滤生效。
- 单条长度截断生效。
- 单任务最大条数限制生效。
- 不会因为当前日志系统不是 Logback 导致执行器启动失败。
- 不会出现 appender 递归写入导致死循环。

## 需要重点验证

- Spring Boot sample + Logback。
- 同一 JVM 多个任务并发执行时，日志不能串到其他任务。
- 任务超时、失败、异常堆栈下仍能正常写入。
- 调度中心滚动日志分页读取正常。
- 大量日志场景下执行器磁盘增长可控。

## 文档更新点

实现后需要更新：

- `README.md`
- `docs/boost-features.md`
- sample `application.properties`
- sample job handler 注释
- 本需求文档状态和实现入口

