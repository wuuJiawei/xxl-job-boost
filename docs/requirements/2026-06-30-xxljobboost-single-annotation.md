# `@XxlJobBoost` 单注解化需求

状态：`Done`

创建日期：`2026-06-30`

## 原始需求记录

用户原始口径：

> XxlJobBoost 注解要同时支持 XxlJob 注解的能力，也就是不再需要 `@XxlJob + @XxlJobBoost` 的组合，只需要一个注解就能声明。

## 背景

当前代码里任务声明需要两个注解配合：

```java
@XxlJob("demoJobHandler")
@XxlJobBoost(
        desc = "示例任务01",
        author = "XXL",
        scheduleType = "CRON",
        scheduleConf = "0 0 0 * * ? *"
)
public void demoJobHandler() {
    XxlJobHelper.log("XXL-JOB Boost, Hello World.");
}
```

当前相关代码：

- `xxl-job-core/src/main/java/com/xxl/job/core/handler/annotation/XxlJob.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/handler/annotation/XxlJobBoost.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/executor/impl/XxlJobSpringExecutor.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/executor/JobSyncHelper.java`

问题是 `@XxlJobBoost` 已经表达了任务元数据，但 handler 名称、init、destroy 仍然必须放在 `@XxlJob` 上，使用方会觉得“声明式增强”没有闭环。

## 目标

支持只使用一个注解声明任务：

```java
@XxlJobBoost(
        value = "demoJobHandler",
        desc = "示例任务01",
        author = "XXL",
        scheduleType = "CRON",
        scheduleConf = "0 0 0 * * ? *",
        init = "init",
        destroy = "destroy"
)
public void demoJobHandler() {
    XxlJobHelper.log("XXL-JOB Boost, Hello World.");
}
```

目标效果：

- `@XxlJobBoost` 可直接注册本地 job handler。
- `@XxlJobBoost` 可直接参与任务同步。
- `@XxlJob + @XxlJobBoost` 旧写法继续兼容。
- 旧的纯 `@XxlJob` 写法继续兼容。

## 建议设计

### 1. 扩展 `@XxlJobBoost`

新增字段：

```java
String value() default "";
String init() default "";
String destroy() default "";
```

其中：

- `value` 等价于 `@XxlJob.value()`，表示 handler 名称。
- `init` 等价于 `@XxlJob.init()`。
- `destroy` 等价于 `@XxlJob.destroy()`。

### 2. 调整扫描逻辑

`XxlJobSpringExecutor.scanJobHandlerMethod` 当前只扫描 `@XxlJob` 方法。需要改成扫描：

- 带 `@XxlJob` 的方法
- 带 `@XxlJobBoost` 的方法

处理规则建议：

| 注解组合 | handler 名称 | 注册行为 | 同步行为 |
| --- | --- | --- | --- |
| 只有 `@XxlJob` | `@XxlJob.value()` | 注册 handler | 不同步 Boost 元数据 |
| `@XxlJob + @XxlJobBoost` | 优先 `@XxlJob.value()` | 注册 handler | 同步 Boost 元数据 |
| 只有 `@XxlJobBoost` | `@XxlJobBoost.value()` | 注册 handler | 同步 Boost 元数据 |

如果 `@XxlJobBoost.value()` 为空且没有 `@XxlJob`，应启动失败并给出明确错误，避免任务无 handler 名称。

### 3. 避免破坏旧 API

不要删除 `@XxlJob`，也不要强迫旧任务迁移。官方兼容和低迁移成本仍然是 Boost 的核心原则。

## 关键约束

- 不能影响现有 `@XxlJob` 的 handler 注册行为。
- 同一个方法同时存在两个注解时，不能重复注册 handler。
- handler 命名冲突仍应复用现有冲突检查。
- `JobSyncHelper.toItem` 当前接收 handlerName 和 `XxlJobBoost`，可以继续复用。

## 验收标准

- 只有 `@XxlJobBoost(value = "...")` 的 Spring Bean 方法可以被注册为 handler。
- 只有 `@XxlJobBoost(value = "...")` 的方法可以被同步到调度中心。
- `@XxlJob + @XxlJobBoost` 旧写法仍正常。
- 只有 `@XxlJob` 的旧写法仍正常。
- handler 命名冲突仍能启动时报错。
- sample 中至少提供一个单注解写法示例。

## 建议测试

- 增加或更新 `XxlJobSpringExecutor` 相关单元测试，覆盖三种注解组合。
- 增加 sample 验证：启动 Spring Boot sample 后，单注解任务出现在调度中心。
- 验证 `syncMode=DISABLED / CREATE_ONLY / CREATE_UPDATE` 下单注解任务行为一致。

## 文档更新点

实现后需要更新：

- `README.md`
- `docs/boost-features.md`
- `docs/release-notes-*.md` 或新增 release note
- sample 注释

## 实现记录

实现日期：`2026-06-30`

核心改动：

- `XxlJobBoost` 新增 `value/init/destroy` 字段，覆盖 `XxlJob` 的 handler 注册能力。
- `XxlJobSpringExecutor` 扫描逻辑改为同时识别 `@XxlJob` 和 `@XxlJobBoost`。
- `XxlJobExecutor` 增加基于 `name/init/destroy` 的注册重载，避免为 `@XxlJobBoost` 构造伪注解。
- Spring Boot sample 已将 `demoJobHandler` 改为单注解写法。
- 增加 `XxlJobSpringExecutorTest` 覆盖纯 `@XxlJob`、双注解、纯 `@XxlJobBoost` 和空 handler 名称失败场景。

代码入口：

- `xxl-job-core/src/main/java/com/xxl/job/core/handler/annotation/XxlJobBoost.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/executor/impl/XxlJobSpringExecutor.java`
- `xxl-job-core/src/main/java/com/xxl/job/core/executor/XxlJobExecutor.java`
- `xxl-job-core/src/test/java/com/xxl/job/core/executor/impl/XxlJobSpringExecutorTest.java`
- `xxl-job-executor-samples/xxl-job-executor-sample-springboot/src/main/java/com/xxl/job/executor/jobhandler/SampleXxlJob.java`
