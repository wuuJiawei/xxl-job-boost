# XXL-JOB Boost 功能路线图

这份文档用于说明 XXL-JOB Boost 的产品方向、阶段规划和中长期增强范围。

它回答的不是“当前已经交付了什么”，而是“这个仓库接下来要往哪里演进”。

## 项目定位

XXL-JOB Boost 是一个基于 XXL-JOB 的非官方增强发行版。

目标不是重写调度器，而是在保留 XXL-JOB 轻量、稳定、易接入这些核心优势的前提下，逐步补齐现代团队更关心的能力：

- 更现代的管理后台体验
- 更清晰的任务治理能力
- 更完整的告警与通知机制
- 更强的执行器注册与元数据管理
- 更可演进的通信与扩展边界
- 更实用的可观测性与运维视角

英文定位：

`An unofficial enhanced distribution of XXL-JOB for modern Java teams.`

## 总体原则

- 保留 XXL-JOB 的调度核心，不从零重写
- 优先做渐进式增强，不做一次性大改
- 优先保证兼容性，避免破坏现有接入方式
- 新能力优先通过新增页面、接口和模块承载
- 必要时接受直接修改现有源码，而不是为了“零侵入”过度设计

## 当前阶段目标

当前第一阶段的核心目标，是先把管理后台现代化，并保证原控制台继续可用。

具体原则：

- 原 `xxl-job-admin` 页面保留
- 新增 `xxl-job-admin-ui` 前端工程
- 新 UI 先完成主链路迁移，再逐步做增强
- 先保证控制台核心页面完整可用，再继续扩展治理能力

## 分阶段路线图

## Phase 1: Admin UI Modernization

第一阶段聚焦控制台现代化，不先动调度核心和执行器协议。

目标：

- 新建 `xxl-job-admin-ui`
- 使用 Vue 3 + TypeScript + Vite + Naive UI
- 打包输出到 `xxl-job-admin/src/main/resources/static/admin-next`
- 与旧页面并存
- 覆盖旧控制台核心业务页面

第一阶段范围：

- 登录页
- Dashboard
- 执行器管理
- 任务管理
- 任务代码查看与编辑
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

## Phase 2: Alerting

第二阶段补齐告警能力，使调度平台从“能跑”提升到“能及时发现问题”。

规划范围：

- 告警通道管理
- 告警规则管理
- 告警记录查询
- Webhook 接入
- 飞书 / 企业微信 / 钉钉等通知适配

目标：

- 将告警配置从硬编码或单一渠道扩展为可配置能力
- 让任务失败、超时、异常等事件具备统一通知出口

## Phase 3: Executor Registration

第三阶段增强执行器注册能力，从“地址注册”提升到“元数据注册”。

规划范围：

- 自动注册
- 元数据注册
- 健康状态上报
- 环境标识
- 标签、负责人信息

目标：

- 让执行器不只是一个地址
- 让平台能理解执行器属于哪个环境、哪个团队、是否健康

## Phase 4: Pluggable Transport

第四阶段逐步抽象执行器通信边界，为后续适配更多运行模式做准备。

规划范围：

- 抽象执行器通信层
- 保留 Netty 兼容模式
- 新增 Spring MVC / Spring Boot Adapter
- 为后续更多运行环境适配留下接口

目标：

- 不破坏现有执行器通信方式
- 在兼容基础上提升可扩展性

## Phase 5: Governance And Observability

第五阶段进入治理和可观测性增强。

规划范围：

- 任务负责人
- 任务标签
- 慢任务分析
- 失败聚合
- 审计日志
- 运维与治理 Dashboard

目标：

- 从“任务调度平台”进一步演进为“任务治理平台”
- 降低排障成本，提升任务资产管理能力

## 规划中的仓库形态

当前已存在的核心目录：

```text
xxl-job-boost
├── xxl-job-admin
├── xxl-job-admin-ui
├── xxl-job-core
├── xxl-job-executor-samples
└── docs
```

中长期规划中的增强目录可能包括：

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

说明：

- 这部分是路线图，不代表这些模块已经全部落地
- 具体是否拆模块，会以实现收益和维护成本为准

## 兼容性策略

- 旧控制台保留，避免一次性替换
- 旧接口协议保留，避免影响现有执行器
- 旧数据库表优先保持兼容，增强能力优先通过加表或新增接口承载
- 旧 Netty 模式先保留，不在前期移除

## 命名策略

- Repository: `xxl-job-boost`
- Product Name: `XXL-JOB Boost`

现阶段为了降低改造成本，仓库仍保留原有 Java 包名、模块名和大部分 artifact 命名。等 UI 和增强能力稳定后，再决定是否需要逐步调整对外发布坐标和模块命名。

## 当前文档关系

- `README.md`：当前状态、快速启动、已交付能力
- `docs/feature-roadmap.md`：产品路线图和未来规划
- `docs/admin-ui-migration-plan.md`：第一阶段后台迁移执行方案
- `docs/upstream-extension-strategy.md`：源码增强和改造边界策略
