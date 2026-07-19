# XXL-JOB Boost 生产部署方案

本文档记录 `1.0.0` 版本的生产 Docker 镜像发布和部署建议。当前阶段先固化方案，不执行镜像推送。

## 发布产物

建议发布两个镜像：

| 镜像 | 用途 | 是否必需 |
| --- | --- | --- |
| `pub.lighting/xxl-job-boost-admin:1.0.0` | 调度中心后端，内置最新版 `admin-next` 静态资源 | 必需 |
| `pub.lighting/xxl-job-boost-executor-sample-springboot:1.0.0` | Spring Boot 样例执行器，用于演示和联调 | 可选 |

同时保留移动标签：

- `pub.lighting/xxl-job-boost-admin:latest`
- `pub.lighting/xxl-job-boost-executor-sample-springboot:latest`

生产环境不建议默认部署样例执行器。样例镜像只用于演示、验收或开发联调。

## 本地构建

```bash
bash scripts/docker-build.sh
```

该脚本会执行：

1. `xxl-job-admin-ui` 前端生产构建。
2. Maven 打包 `xxl-job-admin` 和 Spring Boot 样例执行器。
3. 使用 `docker/docker-compose.yml` 构建本地镜像。

本地镜像名：

- `pub.lighting/xxl-job-boost-admin:local`
- `pub.lighting/xxl-job-boost-executor-sample-springboot:local`

## 镜像打标与推送

发布时在构建通过后执行：

```bash
docker tag pub.lighting/xxl-job-boost-admin:local pub.lighting/xxl-job-boost-admin:1.0.0
docker tag pub.lighting/xxl-job-boost-admin:local pub.lighting/xxl-job-boost-admin:latest
docker push pub.lighting/xxl-job-boost-admin:1.0.0
docker push pub.lighting/xxl-job-boost-admin:latest
```

如需发布样例执行器：

```bash
docker tag pub.lighting/xxl-job-boost-executor-sample-springboot:local pub.lighting/xxl-job-boost-executor-sample-springboot:1.0.0
docker tag pub.lighting/xxl-job-boost-executor-sample-springboot:local pub.lighting/xxl-job-boost-executor-sample-springboot:latest
docker push pub.lighting/xxl-job-boost-executor-sample-springboot:1.0.0
docker push pub.lighting/xxl-job-boost-executor-sample-springboot:latest
```

如果最终使用 GHCR 或 Docker Hub，需要把镜像前缀替换为对应 registry，例如：

- `ghcr.io/wuujiawei/xxl-job-boost-admin:1.0.0`
- `docker.io/javeyswuu/xxl-job-boost-admin:1.0.0`

## 生产部署要点

生产环境建议：

1. 使用外部 MySQL 或独立托管的 MySQL 容器，不把 MySQL 数据目录放在临时目录。
2. 从 `docker/.env.example` 复制生产环境变量，至少修改 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PATH`。
3. `XXL_JOB_ADMIN_CONTEXT_PATH` 默认保持 `/xxl-job-admin`，兼容旧控制台和执行器配置。
4. 只部署 `xxl-job-boost-admin`，除非需要演示，否则不启动样例执行器。
5. 首次上线前根据来源版本选择 `docs/db/` 中唯一匹配的 SQL，在预发库演练并验证新旧控制台。

## 数据库升级

全新数据库使用：

```text
docs/db/install-xxl-job-boost.sql
```

官方旧库升级按来源版本二选一：

```text
docs/db/migrate-from-official-3.4.2.sql
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

- 尚未推送生产镜像。
- 尚未创建 GitHub Release。
- 尚未发布 Maven Central。
