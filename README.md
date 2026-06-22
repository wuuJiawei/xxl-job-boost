# XXL-JOB Boost

XXL-JOB Boost 是基于 XXL-JOB 的增强发行版，目标不是重写调度核心，而是在保持原有调度模型、数据库结构和接入方式基本兼容的前提下，持续补齐现代团队更在意的后台体验和治理能力。

当前版本已经完成第一阶段的核心落地：新一代管理后台 `admin-next` 已可用，旧版控制台仍然保留，前后端可以并行运行和逐步切换。

## 当前状态

- 保留原版 `xxl-job-admin` 后端与旧控制台能力
- 新增 Vue 3 + TypeScript + Vite 的 `admin-next` 管理后台
- 新旧控制台并存，避免一次性替换
- 新增 `/api/admin-next/**` 后端接口，供新前端独立消费
- 保留样例执行器，方便本地联调
- 已移除 GitHub Actions workflow，避免仓库默认依赖额外的 CI 权限配置

## 已交付能力

当前 `admin-next` 已覆盖以下主要页面和链路：

- 登录
- Dashboard
- 执行器管理
- 任务管理
- GLUE 代码查看与保存
- 调度日志列表
- 调度日志详情
- 用户管理
- 帮助页

这意味着新控制台已经不是纯壳工程，而是具备日常试运行能力的可用版本。

## 功能对比

> 对比基线说明：
>
> - 左侧“官方 XXL-JOB”指官方 `XXL-JOB 3.x` 开源版的通用能力边界
> - 右侧“XXL-JOB Boost”指当前仓库已经落地并可运行的增强能力
> - Boost 的重点不是重写调度核心，而是在兼容原有模型的前提下，把后台体验、治理视角、告警编排和工程化接入做得更完整
> - 下表第一部分是“已落地能力”；表后第二部分是“核心增强方向”，用于明确这个项目的主改造线
> - `docs/feature-roadmap.md` 是路线图，不是交付清单；是否已完成，以 README 里的状态说明和仓库内实际实现为准

| 能力项 | 官方 XXL-JOB | XXL-JOB Boost | Boost 增强点 |
| --- | --- | --- | --- |
| 调度核心、任务模型、执行器通信 | 原生支持 | 兼容并沿用 | 不改调度核心，降低迁移和接入成本 |
| 注解驱动任务声明 | `@XxlJob` 为主，任务配置仍偏控制台录入 | `@XxlJob + @XxlJobBoost` 双注解协作 | 可在代码里声明任务描述、负责人、标签、调度、告警、路由、超时、重试等元数据 |
| 执行器到调度中心的任务同步 | 以执行器注册和控制台手工建任务为主 | 已支持执行器启动后主动 `syncJobs` | 把“写代码 -> 登录控制台建任务”收敛为“写代码 -> 启动服务 -> 自动同步” |
| 执行器分组自动建档 | 通常需要先在控制台手工建执行器分组 | 已支持按 `appname` 自动创建 / 更新执行器分组 | 补齐执行器侧声明到调度中心分组建档这一步 |
| 任务同步策略 | 无专门同步策略层 | `DISABLED / CREATE_ONLY / CREATE_UPDATE` | 支持只创建、不覆盖，或创建并更新，避免线上任务被无脑覆盖 |
| 执行器传输层 | 以内嵌 Netty 通信为主 | 已支持 `NETTY_EMBED / SPRING_HTTP` 传输抽象 | 已落地可选去 netty 化接入，样例执行器可直接切到 `SPRING_HTTP`，同时保留 Netty 兼容模式 |
| 调度中心交付形态 | 单后端工程内置控制台 | 后端 + 独立前端工程 | 支持独立演进 `xxl-job-admin-ui` |
| 新旧控制台并存 | 通常按单套控制台使用 | `legacy + admin-next` 双入口并存 | 可灰度切换、可回退、可对照迁移 |
| 现代化前端工程 | 基础控制台交付 | Vue 3 + TypeScript + Vite + Naive UI | 更适合二开、主题定制和前端协作开发 |
| 面向新控制台的专用 JSON API | 通用接口为主 | `/api/admin-next/**` 专用接口层 | 前后端解耦更清晰，避免页面直接绑历史接口细节 |
| 任务管理视图 | 标准列表操作 | 任务树 + 单任务聚焦 + 详情联动 | 更适合大任务量场景下快速定位任务 |
| 日志排查体验 | 基础日志列表与详情 | 任务到日志的联动排查路径更完整 | 从任务、执行器、日志详情切换更顺手 |
| 告警通道管理 | 默认邮件告警，其他方式偏扩展接口 | 通道化管理：EMAIL / WEBHOOK / FEISHU / WECOM / DINGTALK | 告警从“单通道”提升为“可配置通道中心” |
| 告警规则管理 | 基础失败告警能力 | 执行器级 / 任务级规则编排 | 可按任务、事件、渠道组合管理 |
| 告警记录落库与查询 | 基础能力有限 | 独立告警记录模型与查询页 | 便于回溯“发没发、发给谁、结果如何” |
| 任务字段扩展 | 以原始调度字段为主 | 任务标签、告警渠道、告警事件类型等字段已落库 | 为治理、筛选和通知编排提供基础数据 |
| 失败聚合视图 | 无独立聚合页 | 已提供 Failure Aggregates 页面 | 快速看到失败热点任务 |
| 慢任务分析 | 无独立专题页 | 已提供 Slow Tasks 页面 | 快速识别高耗时任务和性能热点 |
| 治理总览 | 无统一治理入口 | 已提供 Governance Overview 页面 | 把归属、标签、失败热点、慢任务热点聚到一个入口 |
| 审计与治理联动 | 基础审计能力 | 审计日志 + 治理专题页联动 | 更适合做持续治理和变更留痕 |
| 本地开发体验 | 官方工程形态为主 | 一键脚本、独立前端开发模式、launchd 常驻配置 | 更适合团队日常开发、调试和演示 |
| 数据库升级辅助 | 官方初始化 SQL 与版本说明 | 在仓库内补齐 Boost 增量 SQL 和迁移文档 | 更方便从官方版本平滑迁到 Boost |

### Boost 的核心增强方向

下面这些也是项目的核心改造项，但需要区分“已落地”和“持续演进中”：

| 核心方向 | 当前状态 | 说明 |
| --- | --- | --- |
| `@XxlJobBoost` 注解声明任务元数据 | 已落地 | 已支持在代码里声明描述、负责人、标签、告警、调度、路由、超时、重试、自动启动等 |
| 执行器启动后自动同步任务 | 已落地 | 已支持通过 `syncJobs` 将代码声明推送到调度中心 |
| 执行器分组自动创建 / 更新 | 已落地 | 已支持按 `appname` 建档，减少控制台手工初始化工作 |
| 任务同步策略控制 | 已落地 | 已支持 `DISABLED / CREATE_ONLY / CREATE_UPDATE` |
| 执行器传输层抽象 / 可选去 netty 化 | 已落地 | 已支持 `SPRING_HTTP` 运行模式，但当前默认仍保留 `NETTY_EMBED`，属于“可选替换传输层”而不是“彻底移除 Netty” |
| 新控制台现代化改造 | 已落地 | `admin-next` 已覆盖核心主链路页面 |
| 告警通道、规则、记录体系 | 已落地 | 已形成从渠道配置到规则编排、发送记录回溯的一整套链路 |
| 失败聚合、慢任务分析、治理总览 | 已落地 | 已不只是“能调度”，而是开始具备“能治理”的平台视角 |
| 多环境隔离与更细粒度注册策略 | 持续演进中 | 路线图里已明确，会继续增强执行器接入的工程化能力 |
| 更完整的声明式注册与元数据治理 | 持续演进中 | 后续会继续围绕自动建组、自动建任务、同步保护策略细化 |
| 更系统的治理与可观测性专题 | 持续演进中 | 会继续补齐告警、审计、慢任务、失败热点等治理面板 |

### 路线图状态说明

为了避免把 roadmap 误读成“都已经做完”，这里把路线项按当前仓库状态拆成三类：

| 状态 | 路线项 | 当前结论 |
| --- | --- | --- |
| 已落地 | 声明式任务注册主链路 | `@XxlJobBoost`、自动建组、自动建任务、`syncJobs`、`DISABLED / CREATE_ONLY / CREATE_UPDATE` 均已落地，可直接运行 |
| 已落地 | 任务变更 Diff 与同步审计 | 任务同步更新时已经会计算字段差异并记录审计详情，不是纯规划项 |
| 已落地 | 告警增强主链路 | 告警通道、告警规则、告警记录、查询页面和后端接口已具备 |
| 已落地 | 治理与观测第一阶段 | 失败聚合、慢任务分析、治理总览、审计日志已经存在，不再只是 roadmap 愿景 |
| 已落地 | 可选去 Netty 化 | 已支持 `SPRING_HTTP` 传输模式，但默认仍保留 `NETTY_EMBED` 兼容模式 |
| 部分落地 | 可插拔传输层 | 传输抽象已经做进现有核心模块，但还没有按路线图拆成独立 transport / adapter 模块 |
| 部分落地 | 治理平台化演进 | 当前已经有治理页面和数据模型，但离“更系统的治理平台”还有继续深化空间 |
| 未完成 | 多环境隔离策略 | 路线图里提到的 `env`、`app-name-pattern` 等能力目前还没有形成独立配置和完整落地方案 |
| 未完成 | 更细粒度同步保护开关 | 类似 `allow-update=false` 这类更细保护策略仍属于路线方向，当前未形成明确配置入口 |
| 未完成 | 独立模块拆分 | `xxl-job-alert`、`xxl-job-registry`、`xxl-job-transport-api`、`xxl-job-adapter-*`、`xxl-job-governance` 等规划模块目前还未拆出 |
| 未完成 | 更多运行时适配 | `Solon / Quarkus / Micronaut` 等适配仍停留在路线图层面，仓库里暂无对应实现 |
| 未完成 | 任务导入导出等后续治理能力 | 属于后续增强项，当前版本不应按“已交付”理解 |

一句话概括当前阶段：

- 已经完成的，是“现代化控制台 + 声明式任务接入 + 告警治理第一阶段”。
- 仍在继续的，是“多环境隔离、模块化拆分、更多运行时适配、更深一层的治理平台化”。

### Boost 更适合什么场景

- 你已经在用 XXL-JOB，但希望保留现有任务和执行器，不想重写调度核心。
- 你真正关注的改造重点是：注解声明、自动同步、自动建组、治理视角，而不只是换一个前端皮肤。
- 你需要一个更现代、可二开的管理后台，而不是只把调度中心当成一个黑盒。
- 你希望把失败聚合、慢任务分析、告警通道、审计留痕放到同一个治理视角下持续演进。
- 你希望团队可以按前后端分工继续增强调度平台，而不是每次都在单体控制台模板里硬改。

## 仓库结构

```text
xxl-job-boost
├── xxl-job-admin                # 调度中心后端 + 旧控制台 + admin-next 静态资源
├── xxl-job-admin-ui             # 新控制台前端源码（Soybean + Vue 3）
├── xxl-job-admin-ui-legacy      # 保留的旧前端工程
├── xxl-job-core                 # XXL-JOB 核心模块
├── xxl-job-executor-samples     # 样例执行器
├── scripts                      # 本地启动/停止/状态脚本
├── launchd                      # 本机常驻启动配置
├── docs                         # 项目文档、迁移方案、版本说明
└── doc                          # 上游 XXL-JOB 原始文档与 SQL
```

## 访问入口

默认本地启动后可访问：

- 旧控制台：`http://127.0.0.1:8080/xxl-job-admin/`
- 新控制台：`http://127.0.0.1:8080/xxl-job-admin/admin-next/`
- 样例执行器：`http://127.0.0.1:8081/`

默认数据库初始化 SQL 会写入管理员账号：

- 用户名：`admin`
- 密码：`123456`

## 数据库升级

如果你是从较早版本升级，而不是全量重建数据库，除了初始化 SQL 之外还需要执行增量升级脚本。

当前已补充的升级脚本：

- [2026-06-13-add-operator-user-id-to-audit-log.sql](./doc/db/migrations/2026-06-13-add-operator-user-id-to-audit-log.sql)
- [2026-06-13-add-alarm-rule-table.sql](./doc/db/migrations/2026-06-13-add-alarm-rule-table.sql)

## 快速启动

### 环境要求

- JDK 17+
- Maven
- Docker
- Node.js 20.19+
- pnpm 10.5+

### 一键启动后端与样例执行器

```bash
chmod +x scripts/dev-start.sh scripts/dev-stop.sh scripts/dev-status.sh
bash scripts/dev-start.sh
```

脚本会自动处理以下事情：

- 拉起或复用 `xxljob-mysql` 容器
- 首次缺少产物时自动执行 Maven 打包
- 启动调度中心和样例执行器
- 将运行日志写到 `/tmp/xxl-job-runtime-logs`

常用命令：

```bash
bash scripts/dev-status.sh
bash scripts/dev-stop.sh
```

### 启动前端开发模式

后端启动后，可单独运行新前端开发服务器：

```bash
cd xxl-job-admin-ui
pnpm install
pnpm dev
```

默认开发地址：

- 前端开发服务：`http://127.0.0.1:5173/`

开发模式下会通过 Vite 代理请求后端 `http://127.0.0.1:8080/xxl-job-admin`。

如果本机已经装好依赖，也可以直接使用仓库脚本：

```bash
bash scripts/run-admin-ui.sh
```

## 前后端关系

### 后端

`xxl-job-admin` 仍然是调度中心主应用，负责：

- 调度核心管理
- 登录态与权限
- 旧控制台页面
- `admin-next` 静态资源托管
- `/api/admin-next/**` JSON 接口

### 前端

`xxl-job-admin-ui` 是新的管理后台工程，技术栈为：

- Vue 3
- TypeScript
- Vite
- Pinia
- Naive UI
- ECharts

开发态由 Vite 独立运行，生产态静态资源挂载在 `xxl-job-admin/src/main/resources/static/admin-next`。

## 兼容性原则

- 不重写 XXL-JOB 调度核心
- 不强制替换旧控制台
- 优先新增接口和页面，降低对旧链路的破坏
- 继续沿用现有数据库结构与执行器通信方式

这套策略的目标很明确：先把新控制台做成可落地的替代入口，再逐步扩展告警、注册、治理和可观测性能力。

## 文档索引

- [功能路线图 / Roadmap](docs/feature-roadmap.md)
- [项目版本说明](docs/release-notes-2026-06-10.md)
- [从 XXL-JOB 迁移到 Boost](docs/migration-from-xxl-job.md)
- [本地运行与排障记录](docs/local-dev-runbook.md)
- [管理后台迁移计划](docs/admin-ui-migration-plan.md)
- [源码增强策略](docs/upstream-extension-strategy.md)

## 本机常驻运行

如果需要脱离当前终端常驻运行，可以使用：

- `launchd/com.xxljob.boost.admin.plist`
- `launchd/com.xxljob.boost.executor.plist`
- `launchd/com.xxljob.boost.admin-ui.plist`

对应脚本：

- `scripts/run-admin.sh`
- `scripts/run-executor.sh`
- `scripts/run-admin-ui.sh`

## License

本项目基于 XXL-JOB 派生开发，继续遵循 GPL-3.0。

使用、分发或二次发布时，请同时评估上游 XXL-JOB 与 GPL-3.0 的相关要求。
