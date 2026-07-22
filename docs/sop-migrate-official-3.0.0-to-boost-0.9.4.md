# 官方 XXL-JOB 3.0.0 迁移到 Boost 0.9.4 SOP

本文用于把正在运行的官方 Docker 镜像 `xuxueli/xxl-job-admin:3.0.0` 迁移到：

```text
wujiawei0926/xxl-job-boost-admin:0.9.4
```

适用前提：保留现有 `xxl_job` 数据库、任务、日志和执行器，执行器暂不升级依赖、不修改配置、不重启。

## 1. 先明确停机边界

| 组件 | 本次是否停止 | 迁移期间的行为 |
| --- | --- | --- |
| 官方 Admin 3.0.0 | 必须停止所有实例 | 停止后不再产生新调度 |
| 现有执行器 | 不停止 | 已经开始的任务继续执行；注册和回调可能暂时失败，Admin 恢复后自动重试 |
| MySQL | 不停止 | 需要执行备份、DDL 和密码重置 |
| Boost Admin 0.9.4 | SQL 完成后启动 | 先启动一个实例，验证后再扩容 |

本次不能做到 Admin 完全零停机。必须保证旧 Admin 和 Boost Admin 不同时连接同一个生产库，避免混合版本调度和重复触发。Admin 停止期间到期的任务按原任务的 misfire 策略处理。

执行器可以保持运行的前提是：

- 所有旧 Admin 实例都已停止，迁移期间没有组件继续写 `xxl_job_registry`。
- 执行器的 Admin 地址、context path 和 access token 在切换后保持不变。
- 执行器日志目录可写且持久化，失败回调能够落盘等待重试。

已有数据库只能使用 admin-only 镜像。不要使用 `xxl-job-boost-all-in-one`，它面向全新单机安装，不负责升级官方旧库。

Boost `0.9.4` 使用 MySQL Connector/J `9.6.0`，[官方支持范围](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-6-0.html)是 MySQL Server `8.0+`。如果现有数据库是 MySQL 5.7、MariaDB 或其他兼容实现，停止本 SOP，先完成独立的数据库升级或专项兼容验证，不要把数据库大版本升级合并到本次 Admin 切换窗口。

## 2. 变更前填写

正式操作前填写并由第二人复核：

```text
变更时间：
操作人：
复核人：
旧 Admin 实例/容器清单：
旧 Admin 对外地址：
旧 Admin context path：/xxl-job-admin
旧 Admin access token 已核对：是 / 否
MySQL 地址和端口：
数据库名：xxl_job
数据库版本：
数据库备份位置：
数据库恢复命令已验证：是 / 否
执行器数量：
预计 SQL 耗时：
允许的 Admin 停机窗口：
回滚决策人：
```

以下命令假设服务器使用 Docker 和外部 MySQL。容器名、端口、网络、日志目录必须按实际环境替换。

```bash
export OLD_ADMIN=xxl-job-admin
export BOOST_ADMIN=xxl-job-boost-admin
export BOOST_IMAGE=wujiawei0926/xxl-job-boost-admin:0.9.4
export WORKDIR=/opt/xxl-job-boost-migration-0.9.4

sudo install -d -m 700 -o "$(id -un)" -g "$(id -gn)" "$WORKDIR"
cd "$WORKDIR"
```

## 3. 至少提前一天完成预演

必须在生产数据库副本上完整执行一次本 SOP，记录以下耗时：

- `mysqldump` 和恢复耗时。
- 迁移 SQL 总耗时。
- `xxl_job_log` 的字段调整、索引创建和删除耗时。
- Boost Admin 启动到健康检查通过的耗时。
- 执行器全部重新注册的耗时。

迁移 SQL 包含直接 `ALTER TABLE`。大日志表可能产生元数据锁、锁表或额外磁盘占用，不能跳过预演直接在生产试跑。

建议维护窗口至少为：

```text
预演 SQL 耗时 + Admin 启动耗时 + 业务验证耗时 + 数据库恢复缓冲
```

## 4. 维护窗口前准备

### 4.1 保存旧环境和回滚材料

以下文件可能包含数据库密码和 access token，目录权限必须保持为 `700`：

```bash
docker inspect "$OLD_ADMIN" > "$WORKDIR/old-admin-inspect.json"
docker inspect "$OLD_ADMIN" \
  --format 'image={{.Config.Image}} image_id={{.Image}} restart={{.HostConfig.RestartPolicy.Name}}' \
  | tee "$WORKDIR/old-admin-image.txt"

docker ps --no-trunc --format '{{.ID}}\t{{.Image}}\t{{.Names}}\t{{.Ports}}' \
  | tee "$WORKDIR/running-containers-before.txt"
```

必须保留：

- 旧 Admin 容器或不可变镜像 ID。
- 旧 Admin 的完整环境变量、端口、网络、挂载和 JVM 参数。
- 反向代理、Docker Compose、systemd 或部署平台配置。
- 数据库恢复命令。

如果生产有多个 Admin 实例，逐一列出。只停止一个实例不等于进入维护状态。

### 4.2 准备受保护的 MySQL 客户端配置

不要把数据库密码直接写进 shell 历史：

```bash
install -m 600 /dev/null "$WORKDIR/mysql-client.cnf"
vi "$WORKDIR/mysql-client.cnf"
```

文件内容：

```ini
[client]
host=<mysql-host>
port=3306
user=<具有备份和DDL权限的账号>
password=<数据库密码>
default-character-set=utf8mb4
```

```bash
export MYSQL_CNF="$WORKDIR/mysql-client.cnf"
mysql --defaults-extra-file="$MYSQL_CNF" -Nse 'SELECT VERSION();'
```

输出必须确认是 MySQL Server `8.0+`。版本不满足时不得继续下载和执行迁移 SQL。

### 4.3 下载并校验固定版本 SQL

只使用 `v0.9.4` tag 对应脚本，不使用 `main` 或其他来源版本脚本：

```bash
curl -fL \
  https://raw.githubusercontent.com/wuuJiawei/xxl-job-boost/v0.9.4/docs/db/migrate-from-official-3.0.0.sql \
  -o "$WORKDIR/migrate-from-official-3.0.0.sql"

(cd "$WORKDIR" && \
  echo '51570a193f5f84ccedb71f54d49309e8b3fccaed163f786b4b0d78389ae85625  migrate-from-official-3.0.0.sql' \
  | sha256sum -c -)
```

预期输出：

```text
migrate-from-official-3.0.0.sql: OK
```

该 tag 内 SQL 的注释采用“Admin 和执行器全部停止”的最保守流程。本 SOP 允许执行器保持运行，但所有旧 Admin 仍必须先停止，再清空注册表和执行 SQL。

### 4.4 拉取并记录目标镜像

```bash
docker pull "$BOOST_IMAGE"
docker image inspect "$BOOST_IMAGE" --format '{{json .RepoDigests}}' \
  | tee "$WORKDIR/boost-image-digest.txt"
```

不要使用 `latest`。目标镜像必须显示为 `0.9.4`，并保存拉取到的 digest。

### 4.5 记录迁移前数据库基线

```bash
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job <<'SQL' | tee "$WORKDIR/database-baseline-before.txt"
SELECT VERSION() AS mysql_version;
SELECT COUNT(*) AS job_count FROM xxl_job_info;
SELECT COUNT(*) AS enabled_job_count FROM xxl_job_info WHERE trigger_status = 1;
SELECT COUNT(*) AS group_count FROM xxl_job_group;
SELECT COUNT(*) AS user_count FROM xxl_job_user;
SELECT COUNT(*) AS log_count FROM xxl_job_log;
SELECT COUNT(*) AS registry_count FROM xxl_job_registry;
SELECT misfire_strategy, COUNT(*) AS job_count
FROM xxl_job_info
GROUP BY misfire_strategy;
SELECT TABLE_NAME,
       TABLE_ROWS,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS total_mb
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'xxl_job'
ORDER BY DATA_LENGTH + INDEX_LENGTH DESC;
SQL
```

检查当前是否有已经触发、尚未完成回调的任务：

```bash
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job -e \
  "SELECT id, job_id, trigger_time, executor_address
   FROM xxl_job_log
   WHERE trigger_code = 200 AND handle_code = 0
   ORDER BY id DESC
   LIMIT 100;"
```

重要任务仍在运行时，优先等待完成；不能等待的，要记录日志 ID，并在迁移后确认回调补齐。

### 4.6 完成并验证预备份

```bash
mysqldump --defaults-extra-file="$MYSQL_CNF" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  --no-tablespaces \
  --databases xxl_job > "$WORKDIR/xxl_job-before-boost.sql"

sha256sum "$WORKDIR/xxl_job-before-boost.sql" \
  | tee "$WORKDIR/xxl_job-before-boost.sql.sha256"
test -s "$WORKDIR/xxl_job-before-boost.sql"
```

仅有备份文件不算完成准备。至少应在隔离 MySQL 中恢复一次，并记录恢复结果和耗时。

### 4.7 准备 Boost Admin 配置

从旧 Admin 配置中复制真实值，不要使用下面的占位符。迁移期保持端口、context path 和 access token 不变：

```bash
install -m 600 /dev/null "$WORKDIR/application-prod.properties"
vi "$WORKDIR/application-prod.properties"
```

```properties
server.port=8080
server.servlet.context-path=/xxl-job-admin
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

spring.datasource.url=jdbc:mysql://<mysql-host>:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>

xxl.job.accessToken=<与现有执行器完全一致的token>
xxl.job.timeout=3
xxl.job.i18n=zh_CN
xxl.job.triggerpool.fast.max=300
xxl.job.triggerpool.slow.max=200
xxl.job.schedule.batchsize=100
xxl.job.logretentiondays=30

management.health.mail.enabled=false
```

如旧环境调整过线程池、批量调度、日志保留或 JVM 参数，以旧环境为准。迁移窗口内不要同时修改这些运行参数。

## 5. 正式切换

### 5.1 冻结控制台变更

通知相关人员停止以下操作：

- 新增、编辑、启停和手动触发任务。
- 修改执行器地址。
- 修改用户、权限和告警配置。

记录正式切换开始时间。确认回滚负责人在线。

### 5.2 停止所有旧 Admin

```bash
docker stop --time 30 "$OLD_ADMIN"
docker inspect "$OLD_ADMIN" --format '{{.State.Status}}'
```

预期状态为：

```text
exited
```

如果有多个 Admin 实例，必须全部停止。再从容器平台、进程列表和健康检查三个位置复核，不能只看当前主机：

```bash
docker ps --format '{{.Image}}\t{{.Names}}\t{{.Status}}' | grep -E 'xxl-job-admin|xxl-job-boost' || true
curl --max-time 3 -fsS http://<admin-address>/xxl-job-admin/actuator/health
```

第二条命令此时应连接失败或由网关返回不可用。执行器继续运行，不要重启。

### 5.3 创建最终回滚点

维护窗口前的备份用于验证备份链路和估算耗时，不能替代最终回滚点。所有旧 Admin 停止后、执行任何 DDL 前，必须创建一次最终备份或存储快照。

数据库较小时执行最终逻辑备份：

```bash
mysqldump --defaults-extra-file="$MYSQL_CNF" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  --no-tablespaces \
  --databases xxl_job > "$WORKDIR/xxl_job-before-boost-final.sql"

sha256sum "$WORKDIR/xxl_job-before-boost-final.sql" \
  | tee "$WORKDIR/xxl_job-before-boost-final.sql.sha256"
test -s "$WORKDIR/xxl_job-before-boost-final.sql"
```

数据库较大时优先使用已经在预演中验证过的云盘、MySQL 实例或存储快照，并记录快照 ID。不要临时采用未经恢复验证的快照方案。最终逻辑备份或快照的耗时必须计入 Admin 停机窗口。

### 5.4 清空临时注册数据

必须在所有旧 Admin 已停止后执行：

```bash
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job -e \
  'TRUNCATE TABLE xxl_job_registry; SELECT COUNT(*) AS registry_count FROM xxl_job_registry;'
```

预期 `registry_count=0`。该表只保存在线注册状态，不会删除任务和执行器分组配置。

### 5.5 执行唯一的 3.0.0 迁移脚本

```bash
set -o pipefail
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job \
  < "$WORKDIR/migrate-from-official-3.0.0.sql" \
  2>&1 | tee "$WORKDIR/migration-output.txt"
```

必须同时满足：

```bash
test "${PIPESTATUS[0]}" -eq 0
grep -F 'XXL-JOB official 3.0.0 -> XXL-JOB Boost database migration completed' \
  "$WORKDIR/migration-output.txt"
```

如果 SQL 报错，保持所有 Admin 停止，先保存完整输出。脚本是幂等设计，但 DDL 会自动提交；只有定位并排除失败原因后才能重跑，不能改跑其他版本的迁移脚本。

### 5.6 重置管理员密码

官方 3.0.0 使用 MD5，Boost 使用 SHA-256，原密码不能直接沿用。先设置一个 4 至 20 位的临时强密码，登录后再按组织规范处理：

```bash
read -r -s -p 'New XXL-JOB admin password: ' NEW_ADMIN_PASSWORD
printf '\n'
ADMIN_PASSWORD_HASH="$(printf '%s' "$NEW_ADMIN_PASSWORD" | sha256sum | awk '{print $1}')"
unset NEW_ADMIN_PASSWORD

mysql --defaults-extra-file="$MYSQL_CNF" xxl_job -e \
  "UPDATE xxl_job_user
   SET password='${ADMIN_PASSWORD_HASH}', token=NULL
   WHERE username='admin';
   SELECT username, CHAR_LENGTH(password) AS password_hash_length
   FROM xxl_job_user
   WHERE username='admin';"

unset ADMIN_PASSWORD_HASH
```

预期 `password_hash_length=64`。其他 32 位 MD5 用户也无法直接登录，迁移后应由管理员逐个重置。

### 5.7 核对数据库结果

```bash
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job <<'SQL' | tee "$WORKDIR/database-check-after.txt"
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
    'xxl_job_system_config',
    'xxl_job_audit_log'
  )
ORDER BY TABLE_NAME;
SELECT COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'xxl_job'
  AND TABLE_NAME = 'xxl_job_info'
  AND COLUMN_NAME IN ('job_tag', 'alarm_channel_ids', 'alarm_event_types')
ORDER BY COLUMN_NAME;
SQL
```

任务、执行器分组、用户和日志数量必须与迁移前一致。应返回 5 张 Boost 表和 3 个 Boost 任务字段。

### 5.8 启动一个 Boost Admin

下面是 Docker CLI 模板。按旧容器的 inspect 结果补齐网络、端口、日志挂载、内存限制和 JVM 参数：

```bash
docker run -d \
  --name "$BOOST_ADMIN" \
  --restart unless-stopped \
  --network <与旧Admin相同的Docker网络> \
  -p 8080:8080 \
  -e TZ=Asia/Shanghai \
  -e JAVA_OPTS='<沿用旧Admin的JVM参数>' \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/application-prod.properties \
  -e LOG_HOME=/data/applogs \
  -v "$WORKDIR/application-prod.properties:/config/application-prod.properties:ro" \
  -v <宿主机Boost日志目录>:/data/applogs \
  "$BOOST_IMAGE"
```

注意：

- 如果反向代理或执行器依赖旧容器 DNS 名，必须让新容器复用原名称或网络别名。
- 不要让新旧容器同时启动。旧容器可以保留为 stopped 状态，便于回滚。
- 使用 Docker Compose 时，将 admin service 的 image 改为固定的 `0.9.4`，保留原网络、端口、挂载和配置，然后只启动 admin service；不要启动仓库示例 Compose 中的 MySQL 或样例执行器。
- 多 Admin 环境先启动一个实例。单实例验收完成后，其他实例必须全部使用相同的 Boost 版本和配置。

跟踪启动日志：

```bash
docker logs --tail 300 -f "$BOOST_ADMIN"
```

日志确认完成后按 `Ctrl-C` 退出，不要停止容器。

## 6. 上线验证

### 6.1 五分钟内完成的基础检查

```bash
curl -fsS http://127.0.0.1:8080/xxl-job-admin/actuator/health
docker ps --filter "name=$BOOST_ADMIN"
docker logs --since 10m "$BOOST_ADMIN" 2>&1 \
  | grep -E 'ERROR|Exception|AccessToken|Communications link failure' || true
```

健康检查预期包含：

```json
{"status":"UP"}
```

浏览器检查：

- 旧控制台：`http://<admin-address>/xxl-job-admin/`
- 新控制台：`http://<admin-address>/xxl-job-admin/admin-next/`
- 使用刚重置的管理员密码登录。
- 核对任务、执行器、用户和历史日志数量。

### 6.2 等待执行器自动重新注册

执行器通常会在后续心跳周期重新注册。等待 60 至 90 秒后检查：

```bash
mysql --defaults-extra-file="$MYSQL_CNF" xxl_job -e \
  "SELECT registry_key, registry_value, update_time
   FROM xxl_job_registry
   ORDER BY registry_key, registry_value;"
```

同时在控制台核对所有自动注册执行器地址。手工录入地址保存在执行器分组配置中，不依赖注册表。

如果执行器没有重新注册，依次检查：

1. 执行器配置的 Admin URL 是否仍为原地址和 `/xxl-job-admin` context path。
2. 新 Admin 的 access token 是否与执行器完全一致。
3. 执行器到 Admin 的网络和反向代理是否可达。
4. 执行器日志中是否出现 registry 或 callback 鉴权错误。

不要为了排查注册问题直接重启全部执行器。

### 6.3 业务验证顺序

1. 选择一个幂等、低风险任务手动触发一次。
2. 确认触发成功、执行器实际执行、回调成功、rolling log 可查看。
3. 核对迁移前仍在运行的日志 ID，确认回调已经补齐。
4. 观察至少一个 Cron 周期，确认没有重复调度。
5. 检查停机窗口内到期任务是否符合各自的 misfire 策略。
6. 验证登录、任务列表、执行器列表和历史日志。
7. 验证通过后再恢复控制台变更，并按需启动其他 Boost Admin 实例。

迁移当天不要升级执行器依赖、切换传输模式或启用自动任务同步。执行器升级作为后续独立变更处理。

## 7. 验收标准

以下条件全部满足才算迁移完成：

- [ ] 只有 Boost Admin 在运行，没有官方 3.0.0 Admin 实例。
- [ ] Admin 健康检查为 `UP`，日志无持续数据库或调度异常。
- [ ] 迁移前后的任务、分组、用户和日志数量一致。
- [ ] 所有预期执行器已重新注册。
- [ ] 低风险任务手动触发、执行、回调和日志查看完整通过。
- [ ] 至少观察一个 Cron 周期，没有重复调度。
- [ ] 停机窗口内任务已按 misfire 策略核对。
- [ ] 管理员可以登录，其他用户密码重置计划已建立。
- [ ] 旧 Admin 容器、旧配置和最终回滚点仍保留。

建议至少保留旧容器和迁移前备份 7 天。备份文件、配置文件和 inspect 输出包含敏感信息，不得上传代码仓库或发送到公共聊天。

## 8. 回滚

出现以下任一情况应进入回滚评估：

- Boost Admin 无法在预定时间内健康启动。
- 大量执行器超过两个心跳周期仍未注册。
- 任务持续触发失败、回调失败或出现重复调度。
- 核心数据数量与迁移前不一致。
- 数据库迁移结果无法确认完整。

迁移 SQL 含非事务 DDL，并且管理员密码已经改成 SHA-256。不要直接用迁移后的数据库启动旧 Admin。标准回滚必须恢复迁移前数据库。

### 8.1 回滚步骤

1. 立即冻结控制台操作和任务变更。
2. 停止所有 Boost Admin，执行器继续保持运行。
3. 确认没有任何 Admin 连接并写入数据库。
4. 经回滚决策人确认后，恢复迁移前数据库备份。
5. 使用保存的原配置启动官方 `xuxueli/xxl-job-admin:3.0.0`。
6. 等待执行器重新注册，核对任务、日志和回调。
7. 记录迁移窗口内可能丢失或需要补录的数据。

同库恢复示例会删除当前库，只能在明确批准后执行：

```bash
sha256sum -c "$WORKDIR/xxl_job-before-boost-final.sql.sha256"

mysql --defaults-extra-file="$MYSQL_CNF" -e 'DROP DATABASE xxl_job;'

mysql --defaults-extra-file="$MYSQL_CNF" \
  < "$WORKDIR/xxl_job-before-boost-final.sql"

docker start "$OLD_ADMIN"
```

如果最终回滚点是存储快照，应使用预演验证过的快照恢复流程，不执行上述逻辑备份恢复命令。如果旧容器在切换时被改名或由 Compose 替换，应按保存的 `old-admin-inspect.json` 和部署文件恢复原名称、网络、端口、挂载、环境变量及 restart policy。

## 9. 操作记录

```text
旧 Admin 全部停止时间：
最终备份完成时间 / 快照 ID：
数据库迁移开始时间：
数据库迁移结束时间：
Boost Admin 启动时间：
健康检查通过时间：
执行器全部注册时间：
手动任务验证结果：
Cron 验证结果：
遗留问题：
最终结论：成功 / 回滚
操作人签字：
复核人签字：
```

完整能力、其他来源版本和执行器后续升级方式参见[通用迁移指南](./migration-from-xxl-job.md)。
