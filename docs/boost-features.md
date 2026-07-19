# XXL-JOB Boost Features

这份文档按当前代码实现整理 XXL-JOB Boost 已落地的主要增强能力。README 负责开源仓库首页表达，本文件负责更完整的功能索引。

## 当前定位

XXL-JOB Boost 是 XXL-JOB 的增强发行版，不是重写版。

核心取舍：

- 保留官方 XXL-JOB 调度核心和任务模型。
- 保留旧控制台，新增 `admin-next` 控制台。
- 保留 Netty 执行器传输，新增可选 Spring HTTP 传输。
- 保留原有手工配置能力，新增声明式任务同步。
- 把告警、审计、失败聚合、慢任务分析纳入治理视角。

## 声明式任务同步

相关代码：

- [`XxlJobBoost.java`](../xxl-job-core/src/main/java/com/xxl/job/core/handler/annotation/XxlJobBoost.java)
- [`JobSyncHelper.java`](../xxl-job-core/src/main/java/com/xxl/job/core/executor/JobSyncHelper.java)
- [`XxlJobSpringExecutor.java`](../xxl-job-core/src/main/java/com/xxl/job/core/executor/impl/XxlJobSpringExecutor.java)
- [`JobSyncRequest.java`](../xxl-job-core/src/main/java/com/xxl/job/core/openapi/model/JobSyncRequest.java)
- [`JobSyncItem.java`](../xxl-job-core/src/main/java/com/xxl/job/core/openapi/model/JobSyncItem.java)
- [`JobSyncServiceImpl.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/service/impl/JobSyncServiceImpl.java)

`@XxlJobBoost` 当前支持声明：

- Handler 名称：`value`
- 初始化方法：`init`
- 销毁方法：`destroy`
- 任务描述：`desc`
- 负责人：`author`
- 任务标签：`jobTag`
- 邮件告警：`alarmEmail`
- 告警渠道：`alarmChannelIds`
- 告警事件：`alarmEventTypes`
- 调度类型：`scheduleType`
- 调度配置：`scheduleConf`
- 默认参数：`executorParam`
- 路由策略：`routeStrategy`
- 过期策略：`misfireStrategy`
- 阻塞策略：`blockStrategy`
- 超时时间：`timeout`
- 失败重试：`retryCount`
- 启动策略：`startPolicy`

执行器启动时，`XxlJobSpringExecutor` 会扫描带 `@XxlJob` 或 `@XxlJobBoost` 的方法。

当前兼容规则：

| 注解组合 | handler 名称 | 注册行为 | 同步行为 |
| --- | --- | --- | --- |
| 只有 `@XxlJob` | `@XxlJob.value()` | 注册 handler | 不同步 Boost 元数据 |
| `@XxlJob + @XxlJobBoost` | 优先 `@XxlJob.value()` | 注册 handler | 同步 Boost 元数据 |
| 只有 `@XxlJobBoost` | `@XxlJobBoost.value()` | 注册 handler | 同步 Boost 元数据 |

如果只有 `@XxlJobBoost` 且 `value` 为空，会在启动扫描时失败，避免生成没有 handler 名称的任务。

调度中心同步行为：

- `DISABLED`：关闭同步。
- `CREATE_ONLY`：只创建缺失任务，不覆盖已有配置。
- `CREATE_UPDATE`：创建并更新任务，更新时计算字段 Diff。

启动策略：

- `MANUAL`：新任务创建后保持停止，已有任务保留调度中心中的运行状态。
- `ON_CREATE`：新任务创建后自动启动，已有任务保留调度中心中的运行状态。
- `ENSURE_RUNNING`：新任务创建后自动启动，并在后续同步时确保已有任务处于运行状态。

自动同步还会：

- 根据 `appname` 创建或更新执行器分组。
- 将任务写入 `xxl_job_info`。
- 可按 `startPolicy` 在首次创建后启动任务，或显式确保任务持续处于运行状态。
- 记录系统审计日志，来源为 `executor-sync`。

## 执行器传输抽象

相关代码：

- [`xxl-job-executor-transport`](../xxl-job-executor-transport)
- [`xxl-job-transport-api`](../xxl-job-executor-transport/xxl-job-transport-api)
- [`xxl-job-transport-netty`](../xxl-job-executor-transport/xxl-job-transport-netty)
- [`xxl-job-transport-spring-mvc`](../xxl-job-executor-transport/xxl-job-transport-spring-mvc)
- [`xxl-job-boost-spring-boot-starter`](../xxl-job-boost-spring-boot-starter)

当前服务端传输：

| 类型 | 模块 | 说明 |
| --- | --- | --- |
| `NETTY_EMBED` | `xxl-job-transport-netty` | 兼容官方嵌入式 Netty 执行器通信 |
| `SPRING_HTTP` | `xxl-job-transport-spring-mvc` | 复用 Spring Web 容器承接执行器 API |

`SPRING_HTTP` 暴露的执行器接口：

- `POST /beat`
- `POST /idleBeat`
- `POST /run`
- `POST /kill`
- `POST /log`

客户端侧通过 `ExecutorBizClientTransportFactory` 解析执行器地址。两种服务端模式都暴露兼容的 HTTP 端点，显式客户端前缀统一为 `HTTP::`：

```text
HTTP::http://127.0.0.1:8081/
http://127.0.0.1:9999/
```

`SPRING_HTTP` / `NETTY_EMBED` 是执行器服务端启动模式，不是 admin 客户端地址前缀。如果没有显式 `HTTP::`，客户端会按 HTTP 地址规则匹配。

## Spring Boot Starter

相关代码：

- [`XxlJobAutoConfiguration.java`](../xxl-job-boost-spring-boot-starter/src/main/java/com/xxl/job/core/spring/boot/XxlJobAutoConfiguration.java)
- [`XxlJobProperties.java`](../xxl-job-boost-spring-boot-starter/src/main/java/com/xxl/job/core/spring/boot/XxlJobProperties.java)

自动配置能力：

- 根据 `xxl.job.executor.appname` 创建 `XxlJobSpringExecutor`。
- 注入 admin 地址、accessToken、timeout、appname、groupTitle、syncMode、transport、日志配置。
- 使用 `xxl-job-boost-spring-boot-starter` 时默认启用 `SPRING_HTTP`；显式配置 `xxl.job.executor.transport=SPRING_HTTP` 时自动注册：
  - `ExecutorBiz`
  - `ExecutorTransportDispatcher`
  - `SpringHttpExecutorTransportController`
- 在 `SPRING_HTTP` 模式下优先使用 `server.port` 作为执行器端口。

## 调度中心 Server 增强

相关代码：

- [`AdminNextApiController.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/controller/base/AdminNextApiController.java)
- [`AdminNextController.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/controller/base/AdminNextController.java)
- [`AdminNextStaticController.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/controller/base/AdminNextStaticController.java)
- [`JobSyncServiceImpl.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/service/impl/JobSyncServiceImpl.java)
- [`AuditLogServiceImpl.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/service/impl/AuditLogServiceImpl.java)

新增能力：

- `/api/admin-next/**` JSON API，服务新控制台。
- `admin-next` SPA 静态资源托管。
- 执行器分组与任务自动同步。
- 任务同步 Diff 与系统审计。
- GLUE 代码详情、保存与历史版本查询。
- 日志详情元信息查询。
- 失败聚合、慢任务、治理总览 API。
- 执行器默认告警策略、告警记录、告警渠道元数据 API。

旧控制台仍然保留，旧接口也继续被新控制台复用了一部分，例如：

- `/jobgroup/**`
- `/jobinfo/**`
- `/joblog/**`
- `/user/**`
- `/alarmchannel/**`
- `/alarmrule/**`

## 新 Web UI

相关目录：

- [`xxl-job-admin-ui`](../xxl-job-admin-ui)
- [`xxl-job-admin-ui/src/views`](../xxl-job-admin-ui/src/views)
- [`xxl-job-admin-ui/src/api`](../xxl-job-admin-ui/src/api)

技术栈：

- Vue 3
- TypeScript
- Vite
- Pinia
- Naive UI
- ECharts
- UnoCSS
- elegant-router

当前页面：

- `home`：Dashboard。
- `executors`：执行器管理与在线节点查看。
- `jobs`：任务树、任务管理、任务新增/编辑/复制/启动/停止/触发/日志跳转。
- `job-code`：GLUE 代码查看、保存和版本切换。
- `logs`：日志树、日志列表、终止执行。
- `log-detail`：滚动日志详情。
- `users`：用户与执行器权限管理。
- `alerts`：告警渠道、执行器默认策略总览、告警记录。
- `failure-aggregates`：失败热点任务聚合。
- `slow-tasks`：慢任务分析。
- `governance`：治理总览，聚合失败、慢任务和最近审计。
- `audits`：审计日志。
- `system-settings`：系统设置，当前提供 SMTP 邮箱配置。
- `help`：帮助入口。

生产构建配置：

- `VITE_BASE_URL=/xxl-job-admin/admin-next/`
- `VITE_SERVICE_BASE_URL=/xxl-job-admin`

开发模式默认代理后端：

- `http://127.0.0.1:8080/xxl-job-admin`

## 告警增强

相关代码：

- [`core/alarm`](../xxl-job-admin/src/main/java/com/xxl/job/admin/core/alarm)
- [`scheduler/alarm`](../xxl-job-admin/src/main/java/com/xxl/job/admin/scheduler/alarm)
- [`JobAlarmChannelController.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/controller/biz/JobAlarmChannelController.java)
- [`JobAlarmRuleController.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/controller/biz/JobAlarmRuleController.java)
- [`XxlJobAlarmChannelMapper.xml`](../xxl-job-admin/src/main/resources/mapper/XxlJobAlarmChannelMapper.xml)
- [`XxlJobAlarmRuleMapper.xml`](../xxl-job-admin/src/main/resources/mapper/XxlJobAlarmRuleMapper.xml)
- [`XxlJobAlarmRecordMapper.xml`](../xxl-job-admin/src/main/resources/mapper/XxlJobAlarmRecordMapper.xml)

通道类型：

- `EMAIL`
- `WEBHOOK`
- `FEISHU`
- `WECOM`
- `DINGTALK`

事件类型：

- `EXECUTOR_FAIL`
- `EXECUTOR_TIMEOUT`
- `TRIGGER_FAIL`

告警配置：

- 任务字段直接绑定的 `alarm_channel_ids`，配置后覆盖执行器默认策略。
- 执行器默认告警策略，在任务未配置渠道时继承生效；旧版邮件功能关闭时，遗留邮箱字段不会阻断继承。
- 原有 `alarm_email` 邮件告警。

每次投递会写入 `xxl_job_alarm_record`，方便查询：

- 渠道名称与类型
- 告警事件
- 目标
- 响应码
- 响应体
- 错误信息

## 审计与治理

相关代码：

- [`AuditLogService.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/service/AuditLogService.java)
- [`AuditLogServiceImpl.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/service/impl/AuditLogServiceImpl.java)
- [`XxlJobAuditLog.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/model/XxlJobAuditLog.java)
- [`GovernanceOverview.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/model/GovernanceOverview.java)
- [`JobFailureAggregate.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/model/JobFailureAggregate.java)
- [`JobSlowAggregate.java`](../xxl-job-admin/src/main/java/com/xxl/job/admin/model/JobSlowAggregate.java)

审计覆盖：

- 任务新增、更新、删除、启动、停止、手动触发
- GLUE 代码更新
- 执行器新增、更新、删除
- 用户新增、更新、删除
- 告警渠道新增、更新、删除
- 在执行器管理中维护默认告警策略
- 自动任务同步创建与更新

治理视图：

- 总任务数
- 有负责人任务数
- 有标签任务数
- 审计事件数
- 失败 Top 任务
- 慢任务 Top
- 最近审计日志

## 数据库增强

数据库入口 SQL：

- [全新部署](./db/install-xxl-job-boost.sql)
- [从官方 3.4.2 迁移](./db/migrate-from-official-3.4.2.sql)
- [从官方 2.4.x / 2.5.x 迁移](./db/migrate-from-official-2.4.x-2.5.x.sql)

新增或增强的关键字段 / 表：

- `xxl_job_info.job_tag`
- `xxl_job_info.alarm_channel_ids`
- `xxl_job_info.alarm_event_types`
- `xxl_job_alarm_channel`
- `xxl_job_alarm_rule`
- `xxl_job_alarm_record`
- `xxl_job_audit_log`
- `xxl_job_audit_log.operator_user_id`

三份 SQL 是按来源版本选择的独立入口，不是连续执行的 migration 链。具体步骤见[完整迁移指南](./migration-from-xxl-job.md)。

## 样例执行器

相关目录：

- [`xxl-job-executor-sample-frameless`](../xxl-job-executor-samples/xxl-job-executor-sample-frameless)
- [`xxl-job-executor-sample-springboot`](../xxl-job-executor-samples/xxl-job-executor-sample-springboot)
- [`xxl-job-executor-sample-springboot-ai`](../xxl-job-executor-samples/xxl-job-executor-sample-springboot-ai)

Spring Boot sample 已展示：

- `@XxlJobBoost`
- `sync-mode=CREATE_UPDATE`
- `group-title`
- `transport=SPRING_HTTP`
- 可选 `log-capture` 配置示例

Frameless sample 保留：

- `xxl-job-core`
- `xxl-job-transport-netty`

这说明 Boost 当前同时支持传统嵌入式执行器和 Spring Boot HTTP 执行器。

## 业务日志采集

相关代码：

- [`XxlJobLogbackAppender.java`](../xxl-job-boost-spring-boot-starter/src/main/java/com/xxl/job/core/spring/boot/logcapture/XxlJobLogbackAppender.java)
- [`XxlJobLogCaptureRegistrar.java`](../xxl-job-boost-spring-boot-starter/src/main/java/com/xxl/job/core/spring/boot/logcapture/XxlJobLogCaptureRegistrar.java)
- [`XxlJobContext.java`](../xxl-job-core/src/main/java/com/xxl/job/core/context/XxlJobContext.java)

Spring Boot starter 提供可选 Logback appender。开启后，任务执行线程存在 `XxlJobContext` 时，普通 SLF4J/Logback 日志会追加到当前任务执行日志文件。

配置默认关闭：

```properties
xxl.job.executor.log-capture.enabled=true
xxl.job.executor.log-capture.level=INFO
xxl.job.executor.log-capture.max-event-length=4096
xxl.job.executor.log-capture.max-events-per-job=2000
xxl.job.executor.log-capture.include-packages=com.yourcompany.
xxl.job.executor.log-capture.exclude-packages=org.springframework.,spring.,com.zaxxer.hikari.
xxl.job.executor.log-capture.include-mdc=true
```

当前边界：

- 只在 classpath 存在 Logback 时注册。
- 只采集当前任务线程和 `InheritableThreadLocal` 可传递到的新建子线程。
- 不保证自动覆盖业务线程池复用场景。
- 通过级别、包前缀、单条长度和单任务条数限制控制日志膨胀。

## 已知边界

- 调度核心仍沿用 XXL-JOB，不做全新调度器。
- `SPRING_HTTP` 是可选传输模式，不代表默认移除 Netty。
- 业务日志采集第一阶段只覆盖 Logback，不做通用 SLF4J binding 改写。
- `xxl-job-core` 中 Spring 相关依赖为 `provided`，Spring 场景通过 adapter/starter 承接；这不是彻底无 Spring 项目。
- 新控制台已覆盖主链路，但 GLUE 类任务部分复杂操作仍保留旧控制台兜底。
- `docs/feature-roadmap.md` 是路线图，不等同于当前已交付清单。
