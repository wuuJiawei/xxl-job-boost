# XXL-JOB Boost 1.0.0 发布说明

`1.0.0` 是 XXL-JOB Boost 首个自有坐标发布版本，面向生产部署和 Maven Central 发布准备。

## 发布信息

- 版本：`1.0.0`
- Maven groupId：`pub.lighting`
- 父工程坐标：`pub.lighting:xxl-job-boost:1.0.0`
- JDK：`17+`
- License：`GPL-3.0`
- SCM：`https://github.com/wuuJiawei/xxl-job-boost`

## Maven Central 发布模块

计划发布以下库模块：

- `pub.lighting:xxl-job-core:1.0.0`
- `pub.lighting:xxl-job-executor-transport:1.0.0`
- `pub.lighting:xxl-job-transport-api:1.0.0`
- `pub.lighting:xxl-job-transport-netty:1.0.0`
- `pub.lighting:xxl-job-transport-spring-mvc:1.0.0`
- `pub.lighting:xxl-job-boost-spring-boot-starter:1.0.0`

`xxl-job-admin` 和 `xxl-job-executor-samples` 不作为 Maven Central 库模块发布。

推荐执行器依赖：

```xml
<dependency>
    <groupId>pub.lighting</groupId>
    <artifactId>xxl-job-boost-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 生产镜像

生产镜像发布方案见 [生产部署方案](./production-deployment.md)。

建议镜像：

- `pub.lighting/xxl-job-boost-admin:1.0.0`
- `pub.lighting/xxl-job-boost-executor-sample-springboot:1.0.0`

样例执行器镜像不是生产必需项。

## 数据库迁移

全新数据库继续使用：

- `doc/db/tables_xxl_job.sql`

从旧库升级按顺序执行：

- `doc/db/migrations/2026-06-13-add-alarm-rule-table.sql`
- `doc/db/migrations/2026-06-13-add-operator-user-id-to-audit-log.sql`
- `doc/db/migrations/2026-07-04-upgrade-to-xxl-job-boost-1.0.0.sql`

本地启动脚本 `scripts/dev-start.sh` 会在 MySQL 可用后自动按文件名顺序执行 `doc/db/migrations/*.sql`。

## 发布前检查

发布前至少完成：

1. `pnpm build`
2. Maven 普通打包
3. Maven release profile 验证
4. Docker 本地镜像构建
5. 迁移 SQL 在旧库副本上的演练
6. Git tag `v1.0.0`
7. Maven Central 发布
8. 生产镜像推送

Maven Central 发布验证命令：

```bash
JAVA_HOME=/path/to/jdk17 mvn -P release -DskipTests -Dgpg.skip=true verify
```

正式发布时去掉 `-Dgpg.skip=true`，并确保 Maven `central` server、GPG key 和 passphrase 环境已经配置好。

截至本文档提交时，Maven Central 和生产镜像推送尚未执行。
