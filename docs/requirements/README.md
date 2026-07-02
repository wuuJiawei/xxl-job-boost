# Requirements

本目录用于保存 XXL-JOB Boost 后续开发需求的原始记录、范围拆分、设计约束和验收口径。

维护规则：

- 每个需求单独建文档，文件名使用 `YYYY-MM-DD-短标题.md`。
- 文档必须保留“原始需求记录”，不要只写最终方案，方便后续回看当时为什么要做。
- 如果需求还没实现，状态标为 `Draft` 或 `Ready`；实现后再更新为 `Done` 并补充代码入口。
- 涉及 README、路线图、发布说明时，注意区分“计划中”和“已落地”。

## 当前需求

| 状态 | 需求 | 文档 |
| --- | --- | --- |
| Done | `@XxlJobBoost` 单注解化，覆盖 `@XxlJob` handler 能力 | [2026-06-30-xxljobboost-single-annotation.md](2026-06-30-xxljobboost-single-annotation.md) |
| Done | 自动采集业务侧 SLF4J / Logback 日志到 XXL-JOB 执行日志 | [2026-06-30-auto-capture-business-logs.md](2026-06-30-auto-capture-business-logs.md) |
