# XXL-JOB Boost 0.9.5

`0.9.5` 在 `0.9.4` 基础上补充安全登录策略、旧平台任务迁移和上游依赖同步。

## 主要变化

- 原版 Freemarker 管理页面入口关闭，仅开放 `admin-next`。
- 新安装或迁移后的用户首次登录必须修改密码。
- 密码要求 12-64 位，同时包含大写字母、小写字母、数字和特殊字符。
- 新增“旧平台任务同步”页面，可从另一套 XXL-JOB admin 预览并导入任务。
- 导入任务默认停用，已有任务跳过，不覆盖新平台配置，降低双平台并行期间重复调度风险。
- 同步旧平台执行器和任务时，账号密码只用于当前请求，不保存。
- 基于上游 `3.5.0-SNAPSHOT` 同步基础依赖版本。

## Maven Central

已发布，deployment ID：`c698453a-ecd0-4d07-a778-b456f1ca567a`。

以下库模块发布到 Maven Central，版本均为 `0.9.5`：

- `pub.lighting:xxl-job-core:0.9.5`
- `pub.lighting:xxl-job-executor-transport:0.9.5`
- `pub.lighting:xxl-job-transport-api:0.9.5`
- `pub.lighting:xxl-job-transport-netty:0.9.5`
- `pub.lighting:xxl-job-transport-spring-mvc:0.9.5`
- `pub.lighting:xxl-job-boost-spring-boot-starter:0.9.5`

推荐执行器依赖：

```xml
<dependency>
    <groupId>pub.lighting</groupId>
    <artifactId>xxl-job-boost-spring-boot-starter</artifactId>
    <version>0.9.5</version>
</dependency>
```

## Docker Hub

- `wujiawei0926/xxl-job-boost-admin:0.9.5`
- `wujiawei0926/xxl-job-boost-all-in-one:0.9.5`

两个镜像同时更新 `latest` 标签，并发布 `linux/amd64` 和 `linux/arm64` 架构。

## 升级注意

已有 Boost 数据库先执行 `docs/db/migrate-password-security.sql`，再启动 `0.9.5`。首次登录使用原账号密码，完成强密码修改后才能进入后台。

旧平台任务迁移入口位于新控制台的“旧平台任务同步”。导入完成后，确认执行器接入、任务参数和切换窗口，再手动启动新任务。
