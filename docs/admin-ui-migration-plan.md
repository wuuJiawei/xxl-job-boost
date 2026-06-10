# Admin UI Migration Plan

## Goal

把当前 `xxl-job-admin` 里的控制台页面逐步迁移到新的 `xxl-job-admin-ui`，先保证功能等价，再逐步做体验升级。

核心约束：

- 不一次性替换旧页面
- 不同时改完前后端所有接口
- 不先做大规模模型重构
- 每一阶段都要有可运行结果

## Existing Console Scope

当前旧控制台页面和能力边界如下：

页面模板：

- `base/login.ftl`
- `base/index.ftl`
- `base/dashboard.ftl`
- `base/help.ftl`
- `biz/group.list.ftl`
- `biz/job.list.ftl`
- `biz/job.code.ftl`
- `biz/log.list.ftl`
- `biz/log.detail.ftl`
- `biz/user.list.ftl`

对应控制器：

- `LoginController`
- `IndexController`
- `JobGroupController`
- `JobInfoController`
- `JobCodeController`
- `JobLogController`
- `JobUserController`

这意味着第一阶段不是从零定义后台，而是先把这些页面一一迁移出来。

## Technical Direction

前端建议：

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Naive UI
- ECharts

输出目录：

`xxl-job-admin/src/main/resources/static/admin-next`

访问策略：

- 旧页面继续走 `xxl-job-admin/`
- 新页面走 `xxl-job-admin/admin-next/`

## Migration Principle

### 1. API First, Not Page Clone First

不要把 FTL 页面机械翻译成 Vue 页面。

正确做法：

- 先梳理每个旧页面依赖的接口
- 确认哪些接口可直接复用
- 确认哪些接口需要补一个更适合前端调用的 JSON 版本
- 前端只消费清晰、稳定的接口

### 2. Keep Old Behavior Stable

旧页面和旧接口优先不动。

如果新 UI 需要调整：

- 优先新增接口
- 避免直接改旧页面依赖的返回结构
- 避免为了新 UI 破坏旧 jQuery 页面

### 3. Migrate By Page Cluster

不要按组件库或样式系统推进，要按业务页面簇推进。这样每个阶段都能交付用户可见结果。

## Suggested Phases

### Phase 0: Bootstrap

目标：把新前端壳搭起来，但不急着接全量业务。

交付：

- 新建 `xxl-job-admin-ui`
- 基础 Vite 工程
- Router / Store / Request 封装
- 布局框架
- 登录态管理
- `admin-next` 静态资源打包接入

验收：

- 访问 `admin-next` 有页面
- 能完成路由切换
- 能接后端登录接口

### Phase 1: Login + Layout + Dashboard

目标：先把最基础的进入链路打通。

迁移页面：

- 登录页
- 首页布局
- Dashboard

交付：

- 登录页
- 左侧导航
- 顶部栏
- Dashboard 指标卡片
- Dashboard 趋势图

验收：

- 登录成功后进入新控制台首页
- 指标数据可展示
- 旧控制台不受影响

### Phase 2: Executor Management

目标：先迁移执行器管理，因为它模型相对简单、风险较低。

迁移页面：

- `group.list.ftl`

交付：

- 执行器列表
- 新增执行器
- 编辑执行器
- 删除执行器

验收：

- 执行器 CRUD 在新 UI 可用
- 与旧页面结果一致

### Phase 3: Job Management

目标：迁移任务主页面。这是第一阶段里最重要的业务页。

迁移页面：

- `job.list.ftl`
- `job.code.ftl`

交付：

- 任务列表
- 条件筛选
- 新增任务
- 编辑任务
- 启动 / 停止任务
- 手动触发
- 查看任务代码

验收：

- 任务完整操作链路在新 UI 可用
- 常见字段展示完整
- 行为与旧页面保持一致

### Phase 4: Log Management

目标：迁移日志能力，打通调度排查主链路。

迁移页面：

- `log.list.ftl`
- `log.detail.ftl`

交付：

- 日志列表
- 多条件筛选
- 日志详情
- 执行结果展示
- 基础异常高亮

验收：

- 可以完成从任务到日志的排查路径
- 可以查看详情和运行结果

### Phase 5: User + Help + Polish

目标：补齐剩余页面并做第一轮收口。

迁移页面：

- `user.list.ftl`
- `help.ftl`

交付：

- 用户管理
- 帮助页
- 菜单权限收口
- 路由和页面命名收口
- 基础 UI 统一

验收：

- 旧控制台主页面全部在新 UI 有对应页面
- 新 UI 可作为日常主要入口试运行

## Recommended Delivery Order

建议按下面顺序做，不要跳：

1. 前端壳和静态资源接入
2. 登录和 Dashboard
3. 执行器管理
4. 任务管理
5. 日志管理
6. 用户管理和帮助页

原因：

- 登录和首页先打通，最早形成可演示入口
- 执行器页最简单，适合作为第一张业务页
- 任务页最复杂，放在基础设施稳定后做
- 日志页依赖任务上下文，放在任务页后面更稳

## API Strategy

建议为新 UI 建一层明确的 `/api/admin-next/...` 风格接口，避免直接让 Vue 页面绑定历史页面接口细节。

第一阶段可以采用双轨：

- 旧页面继续使用原接口
- 新页面优先复用可直接使用的 JSON 接口
- 不够用的地方补新的 JSON 接口

这样可以把风险控制在新 UI 范围内。

## Non-goals For Phase 1

这些事情第一阶段不要碰：

- 不重构调度核心
- 不移除 Netty
- 不做执行器自动注册重构
- 不做告警系统重构
- 不做数据库大迁移
- 不改 Java 包名
- 不改现有模块名

## Definition Of Done For Phase 1

第一阶段完成的标准不是“更炫”，而是：

- 新 UI 能覆盖旧控制台主流程
- 日常使用不必再依赖旧页面
- 旧页面仍可作为回退入口
- 后续告警、注册、治理功能有明确承载位置

## Next Step

当前最实际的下一步是：

1. 创建 `xxl-job-admin-ui` 工程骨架
2. 接入 `admin-next` 静态资源输出
3. 先实现登录页、布局和 Dashboard
