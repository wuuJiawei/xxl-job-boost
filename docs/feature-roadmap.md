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

## Phase 3: Executor Registration And Declarative Job Sync

第三阶段不再把“自动注册”理解成单纯的执行器地址上报，而是明确拆成两层能力：

- XXL-JOB 现有能力：执行器机器地址自动注册、下线自动摘除、`@XxlJob` 本地自动扫描
- XXL-JOB Boost 目标能力：执行器分组自动建档、任务声明式注册、任务配置同步策略

这也是 Boost 最有差异化价值的一段，不重写调度内核，而是补齐工程化接入体验。

### 边界澄清

当前 XXL-JOB 已经具备：

- 执行器机器地址自动注册
- 执行器节点下线自动摘除
- `@XxlJob` 方法在本地执行器容器内自动扫描注册

但当前仍然缺少完整原生支持的能力：

- 执行器分组自动创建或更新
- 调度中心任务配置自动创建
- 注解驱动的 Cron / 路由 / 超时 / 重试 / 告警声明
- 任务配置同步策略
- 多环境隔离下的安全注册机制

因此，Boost 在这一阶段的核心不是“再做一个注册线程”，而是把下面这条链路补齐：

```text
业务服务启动
  ↓
扫描 @XxlJobBoost / @XxlJob
  ↓
生成任务元数据
  ↓
调用 xxl-job-admin OpenAPI / 内部接口
  ↓
自动创建或更新执行器分组
  ↓
自动创建或更新任务配置
  ↓
执行器继续按原生方式注册机器地址
```

### 规划范围

#### 1. 执行器分组自动创建

把“执行器 appname 已配置，但调度中心还要手工建组”这一步补齐。

建议能力：

- 启动时检查调度中心是否存在对应 `appname`
- 不存在则自动创建执行器分组
- 存在则按策略更新标题、排序、注册方式等字段

示例配置：

```yaml
xxl:
  job:
    boost:
      auto-register-group: true
      app-name: mall-order-executor
      title: 商城订单执行器
```

#### 2. 注解驱动任务声明式注册

新增增强注解，例如：

```java
@XxlJobBoost(
    value = "closeTimeoutOrderJob",
    desc = "关闭超时未支付订单",
    cron = "0 */5 * * * ?",
    author = "武佳伟",
    routeStrategy = RouteStrategyEnum.FIRST,
    misfireStrategy = MisfireStrategyEnum.DO_NOTHING,
    blockStrategy = BlockStrategyEnum.SERIAL_EXECUTION,
    timeout = 60,
    retryCount = 3,
    autoStart = false
)
public void closeTimeoutOrderJob() {
    // 任务逻辑
}
```

目标是把当前接入流程：

```text
写代码
→ 登录 XXL-JOB
→ 新增执行器
→ 新增任务
→ 填 JobHandler / Cron / 策略
→ 保存
```

改造成：

```text
写代码
→ 启动服务
→ 自动注册
```

#### 3. 任务同步策略

任务同步不能默认“每次启动无脑覆盖”，否则线上临时调整容易被冲掉。

建议至少支持三种模式：

```text
CREATE_ONLY
CREATE_UPDATE
DISABLED
```

含义：

- `CREATE_ONLY`：只创建不存在的任务，不覆盖已有任务
- `CREATE_UPDATE`：创建并更新注解声明字段
- `DISABLED`：关闭任务自动同步

默认建议：

- 默认使用 `CREATE_ONLY`
- 生产环境默认关闭更新能力，避免误覆盖

可补充保护配置：

```text
xxl.job.boost.sync-mode=CREATE_ONLY
xxl.job.boost.allow-update=false
```

#### 4. 多环境隔离

注册能力必须把 dev / test / prod 隔开，否则很容易串环境。

建议能力：

- 按环境生成执行器 appname
- 支持 `spring.profiles.active` 模板化命名
- 启动阶段显式打印当前目标环境和调度中心地址

示例：

```text
xxl.job.boost.env=dev
xxl.job.executor.appname=mall-order-executor-dev
```

或：

```text
xxl.job.boost.app-name-pattern=${spring.application.name}-${spring.profiles.active}
```

#### 5. 元数据增强

在“自动建组 + 自动建任务”之外，再为后续治理做铺垫。

建议逐步补齐：

- 负责人默认值
- 标签
- 环境标识
- 健康状态
- 变更来源
- 启动注册结果与 Diff 日志

### 模块拆分建议

这一阶段不应强依赖 Spring MVC，也不应该把能力死绑在某一种运行时里。

建议结构：

```text
xxl-job-boost-core
  元数据模型、注册协议、同步逻辑

xxl-job-boost-spring-boot-starter
  Spring Boot 自动配置、注解扫描、生命周期管理

xxl-job-boost-admin-api
  调用 xxl-job-admin 的客户端

xxl-job-boost-ui
  后台增强页面，可后置
```

这样后续如果要适配：

- Spring Boot
- Solon
- Quarkus
- Micronaut
- 普通 Java main

也不会被某一种 Web 技术栈绑死。

### 这一阶段的版本优先级

建议按下面顺序交付，不要一口气把所有治理能力都塞进来：

第一版先闭环：

- 执行器分组自动创建
- `@XxlJobBoost` 自动扫描
- 任务自动创建
- 任务变更 Diff 日志
- `CREATE_ONLY / CREATE_UPDATE / DISABLED` 策略
- 启动时打印注册结果

第二版再扩展：

- 任务分组标签
- 任务负责人默认值
- 告警渠道扩展
- 企业微信 / 钉钉 / 飞书通知
- 任务变更审计
- 任务导入导出

这条路线的目标是：

- 不重写 XXL-JOB 调度核心
- 不破坏原有执行器地址注册机制
- 重点补齐“声明式任务接入”和“调度中心配置同步”
- 让 XXL-JOB Boost 真正形成工程化差异

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
├── xxl-job-executor-transport
│   ├── xxl-job-transport-api
│   ├── xxl-job-transport-netty
│   └── xxl-job-transport-spring-mvc
├── xxl-job-boost-spring-boot-starter
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
