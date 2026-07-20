# XXL-JOB Boost 生产部署方案

本文档记录 `0.9.4` 测试版本的 Docker 镜像构建、发布和部署方式。

## 发布产物

面向用户提供两种 admin 镜像：

| 镜像 | 用途 | 是否必需 |
| --- | --- | --- |
| `wujiawei0926/xxl-job-boost-admin:0.9.4` | 仅调度中心，连接外部 MySQL；适合生产和已有数据库迁移 | 二选一 |
| `wujiawei0926/xxl-job-boost-all-in-one:0.9.4` | 调度中心 + MySQL 8.4；适合全新单机试用和验收 | 二选一 |

可选样例执行器镜像：`wujiawei0926/xxl-job-boost-executor-sample-springboot:0.9.4`。

同时保留移动标签：

- `wujiawei0926/xxl-job-boost-admin:latest`
- `wujiawei0926/xxl-job-boost-all-in-one:latest`
- `wujiawei0926/xxl-job-boost-executor-sample-springboot:latest`

all-in-one 和样例执行器都不作为生产默认选择。正式环境建议使用 admin-only 镜像连接独立、高可用、已备份的外部 MySQL。

## 本地构建

```bash
bash scripts/docker-build.sh
```

该脚本会执行：

1. `xxl-job-admin-ui` 前端生产构建。
2. Maven 打包 `xxl-job-admin` 和 Spring Boot 样例执行器。
3. 构建 admin-only、all-in-one 和样例执行器本地镜像。

本地镜像名：

- `wujiawei0926/xxl-job-boost-admin:local`
- `wujiawei0926/xxl-job-boost-all-in-one:local`
- `wujiawei0926/xxl-job-boost-executor-sample-springboot:local`

## 运行方式

### admin-only：连接外部 MySQL

已有数据库先执行唯一匹配的迁移 SQL，再启动容器：

```bash
docker run -d --name xxl-job-boost-admin \
  -p 8080:8080 \
  -e PARAMS="--spring.datasource.url=jdbc:mysql://mysql.example.com:3306/xxl_job --spring.datasource.username=xxl_job --spring.datasource.password=change_me" \
  -v xxl-job-boost-logs:/data/applogs \
  wujiawei0926/xxl-job-boost-admin:0.9.4
```

镜像内不包含 MySQL，但 admin 本身仍然必须连接 MySQL。

### all-in-one：内置 MySQL 8.4

```bash
docker run -d --name xxl-job-boost-all-in-one \
  -p 8080:8080 \
  -e MYSQL_ROOT_PASSWORD=change_this_root_password \
  -e MYSQL_PASSWORD=change_this_app_password \
  -v xxl-job-boost-mysql:/var/lib/mysql \
  -v xxl-job-boost-logs:/data/applogs \
  wujiawei0926/xxl-job-boost-all-in-one:0.9.4
```

首次启动空数据卷时，镜像自动导入 `install-xxl-job-boost.sql`。账号密码只在首次初始化时生效；更换环境变量不会修改已有数据卷中的 MySQL 账号。镜像默认不向宿主机暴露 3306。

Compose 方式：

```bash
cp -n docker/.env.example docker/.env
docker compose -f docker/docker-compose-all-in-one.yml up -d
```

all-in-one 不会升级已有官方 XXL-JOB 数据库，也不应直接挂载官方 3.0.0 或其他旧版数据卷。

## 镜像打标与推送

发布时在构建通过后执行：

```bash
docker tag wujiawei0926/xxl-job-boost-admin:local wujiawei0926/xxl-job-boost-admin:0.9.4
docker tag wujiawei0926/xxl-job-boost-admin:local wujiawei0926/xxl-job-boost-admin:latest
docker push wujiawei0926/xxl-job-boost-admin:0.9.4
docker push wujiawei0926/xxl-job-boost-admin:latest

docker tag wujiawei0926/xxl-job-boost-all-in-one:local wujiawei0926/xxl-job-boost-all-in-one:0.9.4
docker tag wujiawei0926/xxl-job-boost-all-in-one:local wujiawei0926/xxl-job-boost-all-in-one:latest
docker push wujiawei0926/xxl-job-boost-all-in-one:0.9.4
docker push wujiawei0926/xxl-job-boost-all-in-one:latest
```

如需发布样例执行器：

```bash
docker tag wujiawei0926/xxl-job-boost-executor-sample-springboot:local wujiawei0926/xxl-job-boost-executor-sample-springboot:0.9.4
docker tag wujiawei0926/xxl-job-boost-executor-sample-springboot:local wujiawei0926/xxl-job-boost-executor-sample-springboot:latest
docker push wujiawei0926/xxl-job-boost-executor-sample-springboot:0.9.4
docker push wujiawei0926/xxl-job-boost-executor-sample-springboot:latest
```

## 生产部署要点

生产环境建议：

1. 使用外部 MySQL 或独立托管的 MySQL 容器，不把 MySQL 数据目录放在临时目录。
2. 从 `docker/.env.example` 复制环境变量，至少修改 MySQL root 和应用账号密码以及数据路径。
3. `XXL_JOB_ADMIN_CONTEXT_PATH` 默认保持 `/xxl-job-admin`，兼容旧控制台和执行器配置。
4. 生产只部署 admin-only 镜像，除非需要单机试用，否则不部署 all-in-one 和样例执行器。
5. 首次上线前根据来源版本选择 `docs/db/` 中唯一匹配的 SQL，在预发库演练并验证新旧控制台。

## 数据库升级

全新数据库使用：

```text
docs/db/install-xxl-job-boost.sql
```

官方旧库升级按来源版本三选一：

```text
docs/db/migrate-from-official-3.4.2.sql
docs/db/migrate-from-official-3.0.0.sql
docs/db/migrate-from-official-2.4.x-2.5.x.sql
```

迁移脚本按幂等方式编写，可在排除部分失败原因后重复执行，但不能交叉执行。生产仍然需要先备份数据库，再按[迁移指南](./migration-from-xxl-job.md)在预发环境演练。

## 上线验证

至少验证：

- 旧控制台：`/xxl-job-admin/`
- 新控制台：`/xxl-job-admin/admin-next/`
- 登录、任务列表、执行器列表、日志滚动。
- 手动触发任务、Cron 触发、执行器回调。
- 告警渠道、告警规则、告警记录。
- 审计日志写入。

## 当前未执行事项

- 两种 Docker 镜像的构建与运行定义已完成，镜像推送状态以 Docker Hub 为准。
- 尚未创建 GitHub Release。
- Maven Central `0.9.4` 已发布；本文件中的 Docker 镜像仍未发布。
