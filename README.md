# XXL-JOB Boost

<p align="center">
  <strong>XXL-JOB enhanced for modern Java teams.</strong>
</p>

<p align="center">
  <a href="https://github.com/xuxueli/xxl-job"><img src="https://img.shields.io/badge/upstream-XXL--JOB-blue" alt="Upstream XXL-JOB"></a>
  <img src="https://img.shields.io/badge/version-3.4.1--SNAPSHOT-orange" alt="Version">
  <img src="https://img.shields.io/badge/JDK-17%2B-brightgreen" alt="JDK 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F" alt="Spring Boot 4">
  <img src="https://img.shields.io/badge/Vue-3-42b883" alt="Vue 3">
  <img src="https://img.shields.io/badge/TypeScript-6-3178c6" alt="TypeScript">
  <img src="https://img.shields.io/badge/license-GPL--3.0-lightgrey" alt="License GPL-3.0">
</p>

XXL-JOB Boost 是基于 [XXL-JOB](https://github.com/xuxueli/xxl-job) 的增强发行版。它不重写调度内核，而是在兼容原有任务模型、数据库结构和执行器接入方式的前提下，把 XXL-JOB 补成更适合现代团队使用的调度平台：代码声明任务、启动自动同步、可选去 Netty 化传输、现代 Web UI、告警编排、审计留痕和治理视图。

如果你已经在用 XXL-JOB，Boost 的目标不是让你迁移到另一套调度系统，而是让原来的系统少一点手工配置、多一点工程化和治理能力。

## 为什么值得看

### 1. 任务可以写在代码里，不再靠控制台手工补配置

官方 XXL-JOB 主要通过 `@XxlJob` 声明本地 handler，但任务描述、Cron、路由、超时、重试、告警等配置通常还要去控制台录入。

Boost 增加了 `@XxlJobBoost`：

```java
@XxlJobBoost(
        value = "demoJobHandler",
        desc = "示例任务01",
        author = "XXL",
        jobTag = "sample,demo",
        alarmEmail = "demo@example.com",
        alarmEventTypes = "EXECUTOR_FAIL,TRIGGER_FAIL",
        scheduleType = "CRON",
        scheduleConf = "0 0 0 * * ? *",
        routeStrategy = "FIRST",
        misfireStrategy = "DO_NOTHING",
        blockStrategy = "SERIAL_EXECUTION",
        autoStart = false
)
public void demoJobHandler() {
    XxlJobHelper.log("XXL-JOB Boost, Hello World.");
}
```

执行器启动后，Boost 会扫描 `@XxlJobBoost`，注册本地 handler，并通过 `syncJobs` 主动同步到调度中心：

- 自动创建 / 更新执行器分组
- 自动创建任务
- 可按策略控制是否覆盖已有任务
- 同步变更会记录审计日志和字段 Diff
- 旧的 `@XxlJob` 和 `@XxlJob + @XxlJobBoost` 写法继续兼容

### 2. 可选 `SPRING_HTTP`，降低 Netty 强绑定

官方执行器通信以嵌入式 Netty 为主。Boost 已经拆出传输抽象：

- `NETTY_EMBED`：保留原兼容模式
- `SPRING_HTTP`：复用 Spring Web 容器暴露 `/beat`、`/idleBeat`、`/run`、`/kill`、`/log`

Spring Boot 执行器可以这样切换：

```properties
xxl.job.executor.transport=SPRING_HTTP
xxl.job.executor.sync-mode=CREATE_UPDATE
xxl.job.executor.group-title=通用执行器Sample
```

这让执行器不必额外再起一套 Netty 端口，在网关、容器、平台治理和安全策略上更容易统一。

### 3. Spring 变成适配层，而不是核心硬前提

Boost 保留 frameless sample，也把 Spring MVC / Spring Boot 接入拆到 adapter：

- `xxl-job-core`：核心执行器与 handler 能力
- `xxl-job-transport-api`：传输抽象与执行器客户端
- `xxl-job-transport-netty`：Netty 传输实现
- `xxl-job-adapter-spring-mvc`：Spring HTTP 传输适配
- `xxl-job-adapter-spring-boot-starter`：Spring Boot 自动配置

这不是“完全去 Spring”，而是把 Spring 从核心前提降到可选适配路径。

### 4. 新一代管理后台 `admin-next`

Boost 新增 `xxl-job-admin-ui`，基于 Vue 3、TypeScript、Vite、Pinia、Naive UI、ECharts。它不是空壳，当前已经覆盖主要日常链路：

- 登录与 Dashboard
- 执行器管理
- 任务树、任务管理、单任务聚焦
- GLUE 代码查看、保存与历史版本
- 日志树、日志列表、滚动日志详情
- 用户与执行器权限管理
- 告警渠道、告警规则、告警记录
- 失败聚合、慢任务分析、治理总览
- 审计日志

旧控制台仍然保留，新控制台挂在：

```text
/xxl-job-admin/admin-next/
```

### 5. 告警、审计、治理一起落库

Boost 不只把页面换漂亮了，还补了后台治理链路：

- 告警通道：`EMAIL / WEBHOOK / FEISHU / WECOM / DINGTALK`
- 告警事件：`EXECUTOR_FAIL / EXECUTOR_TIMEOUT / TRIGGER_FAIL`
- 执行器级 / 任务级告警规则
- 告警投递记录：目标、响应码、响应体、错误信息
- 审计日志：任务、执行器、用户、告警、GLUE 代码、自动同步
- 失败聚合与慢任务分析：按负责人、标签、执行器和任务定位问题热点

## 与官方 XXL-JOB 的关键差异

| 维度 | 官方 XXL-JOB | XXL-JOB Boost |
| --- | --- | --- |
| 调度核心 | 成熟稳定 | 兼容沿用，不重写 |
| 任务声明 | `@XxlJob` 声明 handler | `@XxlJobBoost` 单注解声明 handler 和完整任务元数据 |
| 任务创建 | 控制台手工建任务为主 | 执行器启动后自动同步任务 |
| 执行器分组 | 通常先在控制台建档 | 可按 `appname` 自动创建 / 更新 |
| 同步策略 | 无专门策略层 | `DISABLED / CREATE_ONLY / CREATE_UPDATE` |
| 执行器传输 | Netty 嵌入式通信为主 | `NETTY_EMBED / SPRING_HTTP` 可选 |
| Spring 接入 | 与核心实现耦合更重 | core + transport + adapter/starter 分层 |
| 管理后台 | 传统后端模板页面 | Vue 3 + TypeScript + Naive UI 新控制台 |
| 新旧控制台 | 单入口为主 | legacy + `admin-next` 双入口并存 |
| 告警 | 默认邮件告警为主 | 通道、规则、记录完整链路 |
| 治理 | 依赖日志列表人工排查 | 失败聚合、慢任务、审计、治理总览 |

更完整的能力说明见 [Boost Features](docs/boost-features.md)。

## 快速开始

### 环境要求

- JDK 17+
- Maven
- Docker
- Node.js 20.19+
- pnpm 10.5+

### 启动后端、MySQL 和样例执行器

```bash
chmod +x scripts/dev-start.sh scripts/dev-stop.sh scripts/dev-status.sh
bash scripts/dev-start.sh
```

脚本会：

- 拉起或复用 Docker MySQL 容器 `xxljob-mysql`
- 首次缺少 jar 时自动执行 Maven 打包
- 启动调度中心 `xxl-job-admin`
- 启动 Spring Boot 样例执行器
- 将运行日志写到 `/tmp/xxl-job-runtime-logs` 和 `/tmp/xxl-job-boost-run`

常用命令：

```bash
bash scripts/dev-status.sh
bash scripts/dev-stop.sh
```

### 启动前端开发服务

```bash
bash scripts/run-admin-ui.sh
```

也可以手工启动：

```bash
cd xxl-job-admin-ui
pnpm install
pnpm dev
```

### 本地访问

- 旧控制台：`http://127.0.0.1:8080/xxl-job-admin/`
- 新控制台：`http://127.0.0.1:8080/xxl-job-admin/admin-next/`
- 前端开发服务：`http://127.0.0.1:5173/`
- 样例执行器：`http://127.0.0.1:8081/`

默认管理员账号：

- 用户名：`admin`
- 密码：`123456`

## 执行器接入示例

Spring Boot 执行器只需要引入 starter：

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-adapter-spring-boot-starter</artifactId>
    <version>3.4.1-SNAPSHOT</version>
</dependency>
```

配置：

```properties
server.port=8081

xxl.job.admin.addresses=http://127.0.0.1:8080/xxl-job-admin
xxl.job.admin.accessToken=default_token
xxl.job.admin.timeout=3

xxl.job.executor.enabled=true
xxl.job.executor.appname=xxl-job-executor-sample
xxl.job.executor.group-title=通用执行器Sample
xxl.job.executor.sync-mode=CREATE_UPDATE
xxl.job.executor.transport=SPRING_HTTP
xxl.job.executor.address=
xxl.job.executor.ip=
xxl.job.executor.logpath=/data/applogs/xxl-job/jobhandler
xxl.job.executor.logretentiondays=30
```

同步策略：

| 策略 | 行为 |
| --- | --- |
| `DISABLED` | 不进行 Boost 任务同步 |
| `CREATE_ONLY` | 自动创建缺失任务，不覆盖已有任务 |
| `CREATE_UPDATE` | 自动创建并更新已有任务，更新时记录 Diff 审计 |

## 模块结构

```text
xxl-job-boost
├── xxl-job-admin                         # 调度中心后端、旧控制台、admin-next 静态资源与 JSON API
├── xxl-job-admin-ui                      # 新控制台源码，Vue 3 + TypeScript + Vite + Naive UI
├── xxl-job-admin-ui-legacy               # 保留的旧前端工程
├── xxl-job-core                          # 核心执行器、handler、日志、回调、任务同步扫描
├── xxl-job-transport-api                 # 执行器传输抽象、HTTP 客户端、endpoint 解析
├── xxl-job-transport-netty               # 兼容 Netty 嵌入式执行器传输
├── xxl-job-adapter-spring-mvc            # Spring MVC HTTP 执行器传输适配
├── xxl-job-adapter-spring-boot-starter   # Spring Boot 自动配置
├── xxl-job-executor-samples              # frameless、Spring Boot、Spring AI 样例执行器
├── doc                                   # 上游文档、初始化 SQL、数据库迁移脚本
├── docs                                  # Boost 文档、路线图、迁移指南、运行手册
├── scripts                               # 本地启动、停止、状态检查脚本
├── docker                                # Docker Compose 等容器配置
└── launchd                               # macOS launchd 常驻配置
```

## 数据库升级

全新初始化使用：

- [doc/db/tables_xxl_job.sql](doc/db/tables_xxl_job.sql)

从旧库升级时，除对齐初始化 SQL 外，还需要关注增量脚本：

- [2026-06-13-add-operator-user-id-to-audit-log.sql](doc/db/migrations/2026-06-13-add-operator-user-id-to-audit-log.sql)
- [2026-06-13-add-alarm-rule-table.sql](doc/db/migrations/2026-06-13-add-alarm-rule-table.sql)

迁移前请阅读 [从 XXL-JOB 迁移到 Boost](docs/migration-from-xxl-job.md)。

## 文档

- [Boost Features](docs/boost-features.md)：当前代码已落地能力的完整说明
- [本地运行与排障记录](docs/local-dev-runbook.md)：本机启动方式、端口、常见故障
- [从 XXL-JOB 迁移到 Boost](docs/migration-from-xxl-job.md)：迁移路径、版本边界、数据库升级
- [功能路线图 / Roadmap](docs/feature-roadmap.md)：后续演进方向，不等同于已交付清单
- [管理后台迁移计划](docs/admin-ui-migration-plan.md)：admin-next 的迁移思路
- [源码增强策略](docs/upstream-extension-strategy.md)：为什么采用源码内渐进增强
- [项目版本说明](docs/release-notes-2026-06-10.md)：早期可用基线说明
- [官方中文文档镜像](doc/XXL-JOB官方文档.md)
- [Official English Documentation mirror](doc/XXL-JOB-English-Documentation.md)

## 兼容性原则

- 不重写 XXL-JOB 调度核心。
- 不强制替换旧控制台。
- 默认保留官方执行器接入方式和 Netty 兼容模式。
- 新能力优先通过新增模块、接口、字段和页面承载。
- roadmap 是演进方向，README 和代码才是当前已落地能力依据。

## License

本仓库沿用上游 XXL-JOB 的 GPL-3.0 许可证。详见父 POM 中的 license 声明，以及各子项目保留的上游版权信息。
