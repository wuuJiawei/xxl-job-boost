# 从官方 XXL-JOB 迁移到 XXL-JOB Boost

本文给出从官方 XXL-JOB 迁移到 XXL-JOB Boost 的完整执行顺序，覆盖数据库、admin server、执行器、`NETTY_EMBED` / `SPRING_HTTP` 传输、Docker 镜像和 Docker Compose。目标是在保留任务、执行器分组、用户和历史日志的前提下完成可回滚迁移。

> 版本基线：截至 2026-07-19，官方最新 release 为 `3.4.2`。本仓库提供官方 `3.4.2` 和官方 `2.4.x / 2.5.x` 两条迁移 SQL。官方 `2.3.x` 及更早版本不在直迁范围内，必须先升级到 `2.4.2` 或 `2.5.0`。

## 1. 先选择迁移路径

| 当前系统 | 应执行的数据库脚本 | 推荐路径 |
| --- | --- | --- |
| 全新部署，没有历史数据 | [`install-xxl-job-boost.sql`](./db/install-xxl-job-boost.sql) | 直接初始化 Boost |
| 官方 XXL-JOB `3.4.2` | [`migrate-from-official-3.4.2.sql`](./db/migrate-from-official-3.4.2.sql) | 停 admin、备份、执行脚本、部署 Boost admin |
| 官方 XXL-JOB `3.0.x` 至 `3.4.1` | 先升级或核对到官方 `3.4.2`，再执行 `migrate-from-official-3.4.2.sql` | 不要把版本差异留到 Boost 切换窗口处理 |
| 官方 XXL-JOB `2.4.x / 2.5.x` | [`migrate-from-official-2.4.x-2.5.x.sql`](./db/migrate-from-official-2.4.x-2.5.x.sql) | 停 admin 和执行器、清注册表、执行脚本、重置密码、部署 Boost |
| 官方 XXL-JOB `2.3.x` 及更早 | 无直迁脚本 | 先按官方升级到 `2.4.2` 或 `2.5.0`，稳定后再走上一行 |

不要对同一个库连续执行两份迁移 SQL。两份迁移文件代表不同的来源版本，不是按顺序执行的 migration 链。

## 2. 哪些不变，哪些会变

### 2.1 保持不变

| 项目 | 迁移后的行为 |
| --- | --- |
| 数据库名和官方核心表 | 默认继续使用 `xxl_job`，不重建官方核心表 |
| 任务 ID、执行器分组 ID | 原值保留，任务和分组不重新编号 |
| Cron、路由、阻塞、超时、重试 | 继续沿用原任务配置 |
| 历史调度日志和报表 | `xxl_job_log`、`xxl_job_log_report` 数据保留 |
| `@XxlJob` handler | 继续支持，不要求改成 `@XxlJobBoost` |
| admin OpenAPI 地址 | 保持原 context path 时，执行器的 `xxl.job.admin.addresses` 不需要修改 |
| access token | `xxl.job.accessToken` 与执行器 `xxl.job.admin.accessToken` 必须继续保持一致 |
| 官方 Netty 执行器 | 可以先不升级，Boost admin 保留 HTTP 协议兼容链路 |
| 旧控制台 | 继续保留，默认入口仍为 `/xxl-job-admin/` |

### 2.2 会发生变化

| 项目 | 变化和影响 |
| --- | --- |
| Java 基线 | Boost admin 和使用 Boost 依赖的执行器要求 JDK 17+ |
| admin 技术栈 | Boost 当前基于 Spring Boot 4，必须重新检查外置配置和 JVM 参数 |
| 登录密码 | 官方 2.x 的 MD5 密码不能直接用于 SHA-256 登录，2.x 迁移必须重置密码 |
| 登录会话 | `xxl_job_user.token` 和 SSO 配置变化后，旧登录会话应视为失效并重新登录 |
| 数据库 | 增加任务标签、告警、审计相关字段与表；2.x 路径还会对齐官方 3.4.2 字段和索引 |
| 新控制台 | 新增 `/xxl-job-admin/admin-next/`，与旧控制台并存 |
| 执行器依赖 | 只有使用 Boost starter、声明式同步或 `SPRING_HTTP` 时才需要改依赖 |
| 执行器传输 | 可继续使用 `NETTY_EMBED`，也可按执行器逐个切换到 `SPRING_HTTP` |

## 3. 迁移前准备

### 第一步：建立资产清单

记录并保存以下内容：

- 官方 XXL-JOB 的准确版本，不要只写“2.x”或“3.x”。
- admin 的 JDK、启动命令、镜像标签、context path 和实例数。
- 数据库地址、库名、字符集、MySQL 版本和数据量。
- `xxl.job.accessToken`、邮件配置、日志目录、调度线程池参数。
- 每个执行器的 `appname`、JDK、依赖版本、注册地址、端口和部署方式。
- 手工录入的执行器地址及负载均衡、反向代理、防火墙规则。
- 所有对官方源码、表结构、Mapper 或登录逻辑的自定义改动。

版本必须从正在运行的 JAR、镜像标签或构建清单确认。数据库中没有可靠的官方版本表，不能仅凭表结构猜版本。

### 第二步：准备回滚材料

至少保留：

- 旧 admin JAR 或不可变镜像 digest。
- 旧执行器 JAR 或镜像。
- admin 和执行器的完整外置配置。
- 数据库全量备份及恢复命令。
- 反向代理、Ingress、Service、Compose 文件和密钥配置。

推荐备份命令：

```bash
mysqldump \
  --host=<mysql-host> \
  --port=3306 \
  --user=<backup-user> \
  --password \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  xxl_job > xxl_job-before-boost.sql
```

备份完成后记录校验值，并在隔离 MySQL 实例做一次恢复验证：

```bash
sha256sum xxl_job-before-boost.sql
mysql --host=<restore-host> --user=<restore-user> --password xxl_job < xxl_job-before-boost.sql
```

### 第三步：在预发完整演练

使用生产库脱敏副本演练完整流程，记录：

- SQL 总耗时和每个 `ALTER TABLE` 的耗时。
- `xxl_job_log` 大表加索引或改字段造成的锁表时间。
- admin 启动时间、数据库连接数和调度恢复时间。
- 执行器重新注册所需时间。
- 回滚恢复耗时。

生产停机窗口必须大于“预发 SQL 耗时 + admin 部署耗时 + 验证耗时 + 回滚缓冲”。

## 4. 数据库迁移

### 4.1 全新部署

仅空库使用：

```bash
mysql --host=<mysql-host> --user=<db-user> --password \
  < docs/db/install-xxl-job-boost.sql
```

该文件会创建 `xxl_job` 数据库、官方核心表、Boost 增强表和默认管理员。不要对已有生产库执行全量安装脚本。

### 4.2 从官方 3.4.2 迁移

1. 停止所有官方 `xxl-job-admin` 实例，避免迁移期间仍有调度线程写库。
2. 保持执行器运行或停止均可；为减少无效注册和回调，建议一起停止。
3. 完成数据库备份并验证可恢复。
4. 执行唯一对应脚本：

```bash
mysql --host=<mysql-host> --user=<ddl-user> --password xxl_job \
  < docs/db/migrate-from-official-3.4.2.sql
```

5. 查看输出最后一行应包含：

```text
XXL-JOB official 3.4.2 -> XXL-JOB Boost database migration completed
```

该脚本只增加 Boost 所需内容：

- `xxl_job_info.job_tag`
- `xxl_job_info.alarm_channel_ids`
- `xxl_job_info.alarm_event_types`
- `xxl_job_alarm_channel`
- `xxl_job_alarm_rule`
- `xxl_job_alarm_record`
- `xxl_job_audit_log`

脚本按幂等方式编写，部分失败后排除原因可以重新执行；但数据库 DDL 不提供跨整份脚本的原子事务，不能把“可重复执行”理解为“自动回滚”。

### 4.3 从官方 2.4.x / 2.5.x 迁移

1. 停止所有 admin 和执行器。
2. 备份数据库。
3. 清空临时注册数据。`xxl_job_registry` 只保存在线注册状态，执行器重启后会自动重建：

```sql
TRUNCATE TABLE xxl_job_registry;
```

4. 执行对应脚本：

```bash
mysql --host=<mysql-host> --user=<ddl-user> --password xxl_job \
  < docs/db/migrate-from-official-2.4.x-2.5.x.sql
```

该脚本先完成官方 3.4.2 结构对齐：

- `xxl_job_group.title` 扩到 `varchar(64)`。
- `xxl_job_registry.id` 扩到 `bigint`，注册键调整为唯一索引。
- 任务和日志的 `executor_param` 调整为 `text`。
- `xxl_job_user.password` 扩到 `varchar(100)`并增加 `token`。
- 日志查询索引对齐为 `I_jobgroup` 和 `I_jobid`。

随后执行与 3.4.2 路径相同的 Boost 增量。

5. 处理密码。脚本输出 `users_requiring_password_reset`，长度为 32 的值是旧 MD5 哈希，不能转换成原密码的 SHA-256。至少先为管理员设置一个新的强密码：

```sql
UPDATE xxl_job_user
SET password = SHA2('<new-strong-password>', 256),
    token = NULL
WHERE username = 'admin';
```

不要在命令历史里直接写生产密码。推荐把上述 SQL 放到权限受控的临时文件，执行后立即安全删除，并安排其他用户逐个重置密码。

### 4.4 数据库迁移后核对

```sql
SELECT COUNT(*) AS job_count FROM xxl_job_info;
SELECT COUNT(*) AS group_count FROM xxl_job_group;
SELECT COUNT(*) AS user_count FROM xxl_job_user;
SELECT COUNT(*) AS log_count FROM xxl_job_log;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'xxl_job'
  AND TABLE_NAME IN (
      'xxl_job_alarm_channel',
      'xxl_job_alarm_rule',
      'xxl_job_alarm_record',
      'xxl_job_audit_log'
  )
ORDER BY TABLE_NAME;

SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'xxl_job'
  AND TABLE_NAME = 'xxl_job_info'
  AND COLUMN_NAME IN ('job_tag', 'alarm_channel_ids', 'alarm_event_types')
ORDER BY COLUMN_NAME;
```

任务、执行器、用户、日志数量要与迁移前清单一致。新表默认没有业务数据是正常现象。

## 5. admin server 迁移

### 第一步：保留兼容参数

优先沿用原配置：

```properties
server.port=8080
server.servlet.context-path=/xxl-job-admin

spring.datasource.url=jdbc:mysql://<mysql-host>:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>

xxl.job.accessToken=<existing-token>
xxl.job.timeout=3
xxl.job.i18n=zh_CN
xxl.job.triggerpool.fast.max=300
xxl.job.triggerpool.slow.max=200
xxl.job.schedule.batchsize=100
xxl.job.logretentiondays=30
```

当前前端生产包按 `/xxl-job-admin/admin-next/` 构建。迁移期不要同时修改 context path；否则必须重新构建前端并完整验证静态资源、登录回跳和执行器 admin 地址。

Boost 额外支持：

```properties
# 不使用邮件时明确关闭，避免邮件健康检查和发送链路干扰迁移验证
xxl.job.mail.enabled=false

# 保持新旧控制台和登录端点的 SSO 配置
xxl-sso.token.key=xxl_job_login_token
xxl-sso.token.timeout=604800000
xxl-sso.client.excluded.paths=/admin-next,/admin-next/**,/auth/login,/auth/doLogin,/auth/logout
xxl-sso.client.login.path=/auth/login
```

### 第二步：构建或准备 admin 产物

源码构建：

```bash
cd xxl-job-admin-ui
pnpm install --frozen-lockfile
pnpm build

cd ..
mvn -P 'apps,!release' -pl xxl-job-admin -am -DskipTests package
```

`xxl-job-admin/pom.xml` 会把 `xxl-job-admin-ui/dist` 打进 `static/admin-next`。构建 JAR 前必须先构建前端，否则新控制台可能不是当前源码版本。

### 第三步：单实例启动验证

先只启动一个 Boost admin：

```bash
java -jar xxl-job-admin/target/xxl-job-admin-1.0.0.jar \
  --server.port=8080 \
  --server.servlet.context-path=/xxl-job-admin \
  --spring.datasource.url='<jdbc-url>' \
  --spring.datasource.username='<db-user>' \
  --spring.datasource.password='<db-password>' \
  --xxl.job.accessToken='<existing-token>'
```

确认以下入口：

- 旧控制台：`http://<admin-host>:8080/xxl-job-admin/`
- 新控制台：`http://<admin-host>:8080/xxl-job-admin/admin-next/`
- 健康检查：`http://<admin-host>:8080/xxl-job-admin/actuator/health`

单实例通过后再恢复其他 admin 实例。不要让官方 admin 与 Boost admin 在迁移 DDL 期间同时运行。

## 6. 执行器迁移

执行器不要求一次性全改。最稳的方式是先保持官方执行器不动，只切 admin；确认调度兼容后，再逐个执行器升级依赖或传输模式。

### 6.1 方案 A：暂不修改官方执行器

适合先降低 admin 迁移风险：

- 保留原 `com.xuxueli:xxl-job-core` 依赖。
- 保留 `@XxlJob` 和原 `XxlJobSpringExecutor` 配置。
- 保留 `xxl.job.executor.port` 和 Netty 端口暴露。
- 只确认 `xxl.job.admin.addresses` 指向 Boost admin，access token 一致。

官方 2.4.x 及后续协议处于兼容范围内。旧执行器仍可回调、注册、执行和读取 rolling log，但不能使用 `@XxlJobBoost`、自动任务同步、`SPRING_HTTP` 和业务 Logback 采集。

### 6.2 方案 B：升级 Boost starter，继续 NETTY_EMBED

替换依赖：

```xml
<dependency>
    <groupId>pub.lighting</groupId>
    <artifactId>xxl-job-boost-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

配置：

```properties
xxl.job.admin.addresses=http://<admin-host>:8080/xxl-job-admin
xxl.job.admin.accessToken=<existing-token>
xxl.job.executor.appname=<existing-appname>
xxl.job.executor.transport=NETTY_EMBED
xxl.job.executor.port=9999
xxl.job.executor.sync-mode=DISABLED
```

迁移初期保持 `sync-mode=DISABLED`，避免执行器启动时自动覆盖控制台中的任务配置。确认现有任务稳定后，再按需要改为 `CREATE_ONLY` 或 `CREATE_UPDATE`。

`NETTY_EMBED` 模式下：

- 业务 Web 端口和执行器端口是两个端口。
- 容器、Service、防火墙继续暴露 `xxl.job.executor.port`。
- `xxl.job.executor.address` 留空时会根据 IP 和执行器端口生成注册地址。

### 6.3 方案 C：切换到 SPRING_HTTP

适合 Spring MVC Servlet 应用，执行器 API 复用业务 Web 容器：

```properties
server.port=8081

xxl.job.admin.addresses=http://<admin-host>:8080/xxl-job-admin
xxl.job.admin.accessToken=<existing-token>
xxl.job.executor.appname=<existing-appname>
xxl.job.executor.transport=SPRING_HTTP
xxl.job.executor.address=http://<admin可访问的执行器地址>:8081/
xxl.job.executor.sync-mode=DISABLED
```

starter 默认传输就是 `SPRING_HTTP`，但迁移配置建议显式写出，避免版本升级后误判。

切换时必须同步处理：

1. 确认应用是 Spring MVC Servlet，不是纯 WebFlux 或无 Web 容器应用。
2. 确认 admin 能直连执行器 `server.port`。
3. 将 `/beat`、`/idleBeat`、`/run`、`/kill`、`/log` 放行给 admin。
4. 如有 Spring Security、网关或鉴权过滤器，为这些端点配置 XXL-JOB access token 兼容策略。
5. 停止暴露不再使用的 Netty 端口，更新安全组、Service 和健康检查。
6. 手动触发一条任务，并验证 kill、rolling log 和回调。

服务端模式名是 `NETTY_EMBED` / `SPRING_HTTP`；admin 访问两类执行器时都使用 HTTP 客户端。自动注册地址会规范化为 `HTTP::http://host:port/`。手工地址使用普通 `http://host:port/` 或 `HTTP::http://host:port/`，不要填写 `SPRING_HTTP::` 或 `NETTY_EMBED::`。

### 6.4 自动同步任务的启用顺序

| 配置 | 行为 | 迁移建议 |
| --- | --- | --- |
| `DISABLED` | 不同步 Boost 任务元数据 | 迁移初期默认 |
| `CREATE_ONLY` | 只创建不存在的任务 | 第二阶段小范围试用 |
| `CREATE_UPDATE` | 创建并更新任务，记录 Diff 审计 | 完成字段归属确认后启用 |

不要直接把所有执行器改为 `CREATE_UPDATE`。先明确 Cron、负责人、告警、路由等字段究竟由代码还是控制台维护，否则重启执行器可能覆盖人工配置。

## 7. Docker 镜像迁移

### 7.1 从官方 admin 镜像切换

官方常见镜像：

```text
xuxueli/xxl-job-admin:<version>
```

Boost 镜像使用：

```text
pub.lighting/xxl-job-boost-admin:1.0.0
```

迁移时保持以下挂载和参数语义不变：

- 数据库地址、账号、密码。
- `server.servlet.context-path=/xxl-job-admin`。
- `xxl.job.accessToken`。
- 时区和日志持久化目录。
- JVM 内存、GC、容器时区和优雅停机设置。

先拉取并记录镜像 digest，不要仅依赖可移动的 `latest` 标签：

```bash
docker pull pub.lighting/xxl-job-boost-admin:1.0.0
docker image inspect pub.lighting/xxl-job-boost-admin:1.0.0 \
  --format '{{index .RepoDigests 0}}'
```

数据库 SQL 必须在启动 Boost 容器前执行。镜像启动不会自动升级已有数据库。

### 7.2 执行器镜像切换模式

`NETTY_EMBED`：继续暴露独立执行器端口，例如 `9999`。

```yaml
ports:
  - "9999:9999"
environment:
  PARAMS: >-
    --xxl.job.executor.transport=NETTY_EMBED
    --xxl.job.executor.port=9999
```

`SPRING_HTTP`：暴露业务 Web 端口，例如 `8081`，不再依赖独立 Netty 端口。

```yaml
ports:
  - "8081:8081"
environment:
  PARAMS: >-
    --server.port=8081
    --xxl.job.executor.transport=SPRING_HTTP
    --xxl.job.executor.address=http://executor-service:8081/
```

容器注册地址不能写 `127.0.0.1`，因为 admin 容器中的 `127.0.0.1` 指向 admin 自己。使用 Compose service 名、Kubernetes Service DNS 或 admin 可达的固定域名。

## 8. Docker Compose 迁移

### 场景 A：继续使用原 MySQL 数据卷

这是生产迁移的常见方式：

1. `docker compose stop xxl-job-admin`，并停止执行器。
2. 对当前 MySQL 数据卷做逻辑备份和存储快照。
3. 通过 `docker compose exec -T mysql mysql ...` 执行匹配来源版本的迁移 SQL。
4. 把 admin image 改为 Boost 固定版本标签。
5. 保留原数据库连接、context path、access token 和网络。
6. 先只启动 `xxl-job-admin`，验证后再启动执行器。

示例：

```bash
docker compose stop xxl-job-admin
docker compose stop <executor-service>

docker compose exec -T mysql mysql -uroot -p xxl_job \
  < docs/db/migrate-from-official-3.4.2.sql

docker compose up -d xxl-job-admin
docker compose logs -f --tail=200 xxl-job-admin
```

`-p` 后不直接带密码会交互读取；自动化环境应使用 secret 或受保护的客户端配置文件。

### 场景 B：新建 MySQL 数据卷

只有不保留历史数据时才挂载全量初始化 SQL：

```yaml
volumes:
  - ../docs/db/install-xxl-job-boost.sql:/docker-entrypoint-initdb.d/install-xxl-job-boost.sql:ro
```

MySQL 的 `/docker-entrypoint-initdb.d` 只在数据目录为空时执行。已有数据卷即使修改挂载文件也不会自动执行迁移，必须手工导入对应迁移 SQL。

### Compose 配置检查清单

- admin 和 MySQL 在同一网络，JDBC host 使用 MySQL service 名。
- context path 保持 `/xxl-job-admin`。
- admin 与所有执行器 access token 一致。
- `SPRING_HTTP` 暴露 `server.port`；`NETTY_EMBED` 暴露 executor port。
- 数据、日志和配置使用持久卷，不写到容器临时层。
- `depends_on` 只表示启动顺序；仍需数据库 healthcheck 和 admin 就绪检查。
- 生产不启动样例执行器。

## 9. 上线验证顺序

### admin 基础验证

1. 登录旧控制台和新控制台。
2. 对照迁移前清单核对任务、执行器、用户和历史日志数量。
3. 查看 Dashboard、任务列表、日志列表、用户权限。
4. 新建一条不启用调度的测试任务，保存后删除，确认审计日志写入。

### 执行器验证

1. 启动一个低风险执行器。
2. 确认注册表和执行器页面出现在线地址。
3. 手动触发测试任务。
4. 查看调度结果、回调和 rolling log。
5. 验证 kill 和超时任务。
6. 验证 Cron 至少实际触发一次。
7. 再按批次恢复剩余执行器。

### 告警与权限验证

1. 验证 legacy 邮件配置是否按预期启用或关闭。
2. 新建测试告警通道和规则，触发一次失败告警。
3. 核对普通用户只能访问授权执行器。
4. 核对旧登录会话失效后能重新登录。

## 10. 回滚

### 尚未写入迁移后业务数据

1. 停止 Boost admin 和已升级执行器。
2. 恢复迁移前数据库备份。
3. 恢复官方 admin JAR/镜像和原配置。
4. 恢复原执行器镜像和端口规则。
5. 按迁移前清单验证。

### 已产生迁移后任务、告警或审计数据

不要只删除 Boost 新表然后直接启动旧 admin。应先评估迁移窗口内新增或修改的任务是否需要回放，再决定：

- 整库恢复并人工补录窗口内变更；或
- 保留数据库、停止 Boost 新功能写入，经结构核对后回退应用。

生产默认推荐整库恢复，因为它的状态边界最清楚。

## 11. 常见错误

- 对已有库执行 `install-xxl-job-boost.sql`。
- 不确认来源版本，同时执行两份迁移 SQL。
- 2.x 迁移后没有重置 MD5 密码，导致所有用户无法登录。
- admin context path 改了，但前端 base URL、执行器地址和反向代理没有一起改。
- `SPRING_HTTP` 仍只暴露 Netty 的 `9999`，实际 `server.port` 不可达。
- 容器注册地址使用 `127.0.0.1`。
- 一上来启用 `CREATE_UPDATE`，覆盖控制台人工维护的任务配置。
- 只备份数据库，没有保留旧镜像、配置和恢复命令。
- 在大日志表上直接执行 DDL，没有提前测量锁表时间和磁盘空间。

## 12. 相关资料

- [数据库脚本说明](./db/README.md)
- [生产部署方案](./production-deployment.md)
- [Boost 已落地能力](./boost-features.md)
- [官方中文文档镜像](./upstream/XXL-JOB官方文档.md)
- [官方英文文档镜像](./upstream/XXL-JOB-English-Documentation.md)
- [项目 README](../README.md)
