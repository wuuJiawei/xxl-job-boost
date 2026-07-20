# XXL-JOB Boost 0.9.4 发布说明

`0.9.4` 是 XXL-JOB Boost 首个自有坐标公开测试版本，用于 Maven Central 发布链路和真实项目兼容性验证，不代表 `1.0.0` 生产 GA。

## 发布信息

- 版本：`0.9.4`
- Maven groupId：`pub.lighting`
- 父工程坐标：`pub.lighting:xxl-job-boost:0.9.4`
- JDK：`17+`
- License：`GPL-3.0`
- SCM：`https://github.com/wuuJiawei/xxl-job-boost`
- Maven Central：已发布
- Central deployment：`311e83ea-1071-419c-a3fd-87e474150942`

## Maven Central 发布模块

本次发布以下库模块：

- `pub.lighting:xxl-job-core:0.9.4`
- `pub.lighting:xxl-job-executor-transport:0.9.4`
- `pub.lighting:xxl-job-transport-api:0.9.4`
- `pub.lighting:xxl-job-transport-netty:0.9.4`
- `pub.lighting:xxl-job-transport-spring-mvc:0.9.4`
- `pub.lighting:xxl-job-boost-spring-boot-starter:0.9.4`

`xxl-job-admin` 和 `xxl-job-executor-samples` 不作为 Maven Central 库模块发布。

推荐执行器依赖：

```xml
<dependency>
    <groupId>pub.lighting</groupId>
    <artifactId>xxl-job-boost-spring-boot-starter</artifactId>
    <version>0.9.4</version>
</dependency>
```

## Docker 镜像

生产镜像发布方案见 [生产部署方案](./production-deployment.md)。

面向用户提供两种 admin 镜像：

- `javeyswuu/xxl-job-boost-admin:0.9.4`：仅 admin，连接外部 MySQL。
- `javeyswuu/xxl-job-boost-all-in-one:0.9.4`：admin + MySQL 8.4，仅用于全新单机环境。

样例执行器镜像 `javeyswuu/xxl-job-boost-executor-sample-springboot:0.9.4` 不是生产必需项。

## 数据库迁移

全新数据库继续使用：

- `docs/db/install-xxl-job-boost.sql`

官方旧库按来源版本选择其中一份：

- `docs/db/migrate-from-official-3.4.2.sql`
- `docs/db/migrate-from-official-3.0.0.sql`
- `docs/db/migrate-from-official-2.4.x-2.5.x.sql`

数据库脚本现已按来源版本整理为四个明确入口。本地启动脚本 `scripts/dev-start.sh` 只在空库执行全量安装 SQL；已有官方库按迁移指南人工选择一份迁移 SQL。

## 发布前检查

完整产品发布前至少完成：

1. `pnpm build`
2. Maven 普通打包
3. Maven release profile 验证
4. Docker 本地镜像构建
5. 迁移 SQL 在旧库副本上的演练
6. Maven Central 发布
7. Git tag `v0.9.4`
8. 生产镜像推送

Maven Central 发布验证命令：

```bash
JAVA_HOME=/path/to/jdk17 mvn -P release -DskipTests -Dgpg.skip=true verify
```

正式发布时去掉 `-Dgpg.skip=true`，并确保 Maven `central` server、GPG key 和 passphrase 环境已经配置好。

本轮 Maven 库模块已发布并完成公网解析验证；GitHub Release 和生产镜像另行处理。
