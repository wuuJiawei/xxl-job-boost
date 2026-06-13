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
