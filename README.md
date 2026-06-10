# XXL-JOB Boost

XXL-JOB Boost 是一个基于 XXL-JOB 的非官方增强发行版。

它保留 XXL-JOB 轻量、稳定、易接入的调度核心，在兼容原有使用习惯的前提下，逐步补齐现代团队更关心的管理后台体验、告警能力、执行器注册、任务治理和可观测性能力。

## Project Positioning

- 保留 XXL-JOB 的调度核心，不从零重写调度器。
- 优先做渐进式增强，不做一次性大改。
- 优先保证兼容性，避免破坏现有接入方式。
- 新能力优先以新增代码和独立前端承载，必要时直接修改现有源码实现。

英文定位：

`An unofficial enhanced distribution of XXL-JOB for modern Java teams.`

## Current Goal

当前第一阶段目标只有一个：先把管理后台现代化，并且保证原控制台继续可用。

具体原则：

- 原 `xxl-job-admin` 页面保留。
- 新增 `xxl-job-admin-ui` 前端工程。
- 新 UI 先做“完整迁移”，后做“增强改造”。
- 先保证现有控制台页面都能迁移过来，不追求一步到位。

## Stage Plan

### Phase 1: Admin UI Migration

第一阶段先做控制台迁移，不先动底层通信层和任务模型。

目标：

- 新建 `xxl-job-admin-ui`
- 使用 `Vue 3 + TypeScript + Vite + Naive UI`
- 打包输出到 `xxl-job-admin/src/main/resources/static/admin-next`
- 与旧页面并存
- 完成旧控制台核心页面迁移

第一阶段迁移范围：

- 登录页
- Dashboard
- 执行器管理
- 任务管理
- 任务代码查看
- 调度日志列表
- 调度日志详情
- 用户管理
- 帮助页

验收标准：

- 新 UI 可以登录
- 核心列表页和详情页可用
- 任务新增、编辑、启动、停止链路可用
- 日志查询链路可用
- 不影响旧页面访问和使用

### Phase 2: Alerting

- 告警通道
- 告警规则
- 告警记录
- Webhook / 飞书 / 企业微信 / 钉钉适配

### Phase 3: Executor Registration

- 自动注册
- 元数据注册
- 健康状态
- 环境、标签、负责人信息

### Phase 4: Pluggable Transport

- 抽象执行器通信层
- 保留 Netty 兼容模式
- 新增 Spring MVC / Spring Boot Adapter

### Phase 5: Governance And Observability

- 任务负责人
- 任务标签
- 慢任务分析
- 失败聚合
- 审计日志
- 运营与治理 Dashboard

## Repository Layout

当前代码基线：

```text
xxl-job-boost
├── xxl-job-admin
├── xxl-job-core
├── xxl-job-executor-samples
└── docs
```

规划中的增强目录：

```text
xxl-job-boost
├── xxl-job-admin
├── xxl-job-admin-ui
├── xxl-job-core
├── xxl-job-alert
├── xxl-job-registry
├── xxl-job-transport-api
├── xxl-job-transport-netty
├── xxl-job-adapter-spring-mvc
├── xxl-job-adapter-spring-boot-starter
├── xxl-job-adapter-solon
└── xxl-job-governance
```

## Compatibility Strategy

- 旧控制台保留，避免一次性替换。
- 旧接口协议保留，避免影响现有执行器。
- 旧数据库表优先保持兼容，增强能力优先加表。
- 旧 Netty 模式先保留，不在第一阶段移除。

## Naming Strategy

项目名称：

- Repository: `xxl-job-boost`
- Product Name: `XXL-JOB Boost`

当前仓库是 Boost 的开发基线，但为了降低改造成本，现阶段仍保留原有 Java 包名、模块名和大部分 artifact 命名。等第一阶段 UI 落稳后，再决定是否需要逐步调整发布坐标与模块命名。

## License

本项目基于 XXL-JOB 派生开发。

- XXL-JOB 使用 GPL-3.0
- 本项目中的派生代码继续遵循 GPL-3.0

如果后续对外分发修改后的程序、镜像或安装包，需要同时考虑 GPL-3.0 对源码提供和继续授权的要求。

## Development Notes

当前开发策略：

- 先完成新旧 UI 共存
- 先迁移、再增强
- 先界面、再能力
- 先可运行、再重构

更详细的控制台迁移拆解见：

- `docs/admin-ui-migration-plan.md`

## Local Run

本地快速启动：

```bash
chmod +x scripts/dev-start.sh scripts/dev-stop.sh scripts/dev-status.sh
bash scripts/dev-start.sh
```

说明：

- 依赖 JDK 17、Maven、Docker
- 会自动拉起或复用 `xxljob-mysql`
- 首次缺少 jar 时会自动执行 Maven 打包
- 会自动把日志目录切到 `/tmp/xxl-job-runtime-logs`，避免默认 `/data` 路径在本机不可写

常用命令：

```bash
bash scripts/dev-status.sh
bash scripts/dev-stop.sh
```

如果需要在本机长期常驻，而不是依赖当前终端会话，可以用仓库内的 `launchd` 配置：

- `launchd/com.xxljob.boost.admin.plist`
- `launchd/com.xxljob.boost.executor.plist`

它们会调用：

- `scripts/run-admin.sh`
- `scripts/run-executor.sh`
