# 本地运行与排障记录

这份文档记录 XXL-JOB Boost 之前是怎么在本机跑起来的，以及这次开发中已经确认过的运行方式、验证方法和常见故障。

目标不是泛泛写“理论上怎么启动”，而是保留一份可直接复用的实操记录。

## 结论先说

之前能稳定跑起来，核心不是手工一条条命令拼，而是依赖仓库里已经准备好的脚本：

- `scripts/dev-start.sh`
- `scripts/dev-status.sh`
- `scripts/dev-stop.sh`
- `scripts/run-admin-ui.sh`

推荐方式：

1. 用 `scripts/dev-start.sh` 拉起 MySQL、admin 和 sample executor
2. 用 `scripts/run-admin-ui.sh` 单独拉起前端开发服务
3. 用 `scripts/dev-status.sh` 看状态
4. 用 `scripts/dev-stop.sh` 停服务

## 之前是怎么跑起来的

### 1. 后端和执行器

实际启动入口：

```bash
bash scripts/dev-start.sh
```

这个脚本已经把本地联调所需动作封装好了：

- 自动检查并启动 Docker MySQL 容器 `xxljob-mysql`
- 仅为空数据库执行 `docs/db/install-xxl-job-boost.sql`；已有官方库需人工选择对应迁移 SQL
- 首次缺少 jar 时自动执行 Maven 打包
- 自动寻找 JDK 17
- 自动启动调度中心 admin
- 自动启动样例执行器 executor
- 自动等待端口就绪

脚本默认依赖这些路径和端口：

- MySQL 容器：`xxljob-mysql`
- MySQL 端口：`3306`
- admin：`8080`
- executor web：`8081`
- executor rpc：`9999`

脚本启动成功后，实际给出的访问入口就是：

- 旧控制台：`http://127.0.0.1:8080/xxl-job-admin/`
- 新控制台：`http://127.0.0.1:8080/xxl-job-admin/admin-next/`
- 样例执行器：`http://127.0.0.1:8081/`

### 2. 前端开发模式

前端不是靠系统默认 `node` 跑起来的，仓库脚本已经固定了更稳的 Node 入口：

```bash
bash scripts/run-admin-ui.sh
```

这个脚本会优先用：

```text
/Users/song/.nvm/versions/node/v20.19.0/bin/node
```

然后直接启动：

```text
xxl-job-admin-ui/node_modules/vite/bin/vite.js
```

默认端口：

- `5173`

也就是说，之前前端能跑起来，靠的不是“当前 shell 正好有个能用的 node”，而是脚本绕开了坏掉的系统 node，直接指定了可用版本。

## 推荐运行步骤

### 启动后端

```bash
bash scripts/dev-start.sh
```

### 查看状态

```bash
bash scripts/dev-status.sh
```

会检查：

- admin pid
- executor pid
- mysql 容器状态
- 8080 / 8081 / 9999 / 3306 端口监听状态

### 启动前端开发服务

```bash
bash scripts/run-admin-ui.sh
```

### 停止全部服务

```bash
bash scripts/dev-stop.sh
```

## 环境前提

这套本地运行方式默认依赖：

- JDK 17+
- Maven
- Docker
- Node.js 20.19+
- pnpm 10.5+

其中最关键的是：

- Java 必须是 17 或更高
- 不要依赖系统里那个旧的 Node 15

## 这次已经遇到并确认过的问题

### 1. Maven 编译失败：`无效的目标发行版: 17`

本次实际报错：

```text
Failed to execute goal ... Fatal error compiling: 无效的目标发行版: 17
```

这说明当前直接执行 `mvn` 时使用的 Java 版本不对，不是 JDK 17。

为什么之前能跑：

- `scripts/dev-start.sh`
- `scripts/run-admin.sh`
- `scripts/run-executor.sh`

这几个脚本都会优先解析 `JAVA_HOME`，如果不满足条件，就回退到：

```text
/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

所以之前“能跑起来”的关键，是脚本帮忙兜底选了 JDK 17。

建议：

- 平时优先用脚本启动
- 如果要手工跑 `mvn`，先确认 `java -version`
- 必要时显式设置：

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

### 2. `pnpm typecheck` / `pnpm dev` 可能被旧 Node 15 卡死

本次实际报错是：

```text
dyld: Library not loaded: /usr/local/opt/icu4c/lib/libicui18n.67.dylib
Referenced from: /usr/local/Cellar/node/15.2.0/bin/node
```

这说明当前 shell 命中的 `node` 是一个已经坏掉的旧 Node 15。

为什么之前前端能跑：

- `scripts/run-admin-ui.sh` 不依赖当前 shell 的默认 node
- 它优先使用固定的 NVM Node 20.19.0

建议：

- 前端开发直接跑：

```bash
bash scripts/run-admin-ui.sh
```

- 不要直接相信当前 shell 的 `node`
- 如果要手工执行 `pnpm`，先确认：

```bash
which node
node -v
```

必要时显式指定：

```bash
export PATH="/Users/song/.nvm/versions/node/v20.19.0/bin:$PATH"
```

### 3. 端口占用

`scripts/dev-start.sh` 会检查并清理以下端口上的 xxl-job 相关进程：

- `8080`
- `8081`
- `9999`

但如果这些端口被非 xxl-job 进程占用，脚本会直接失败，而不是强杀别的服务。

这也是合理的，避免误杀本机其他程序。

### 4. 首次启动缺少产物

如果缺少以下 jar：

- `xxl-job-admin/target/xxl-job-admin-1.0.0.jar`
- `xxl-job-executor-samples/xxl-job-executor-sample-springboot/target/xxl-job-executor-sample-springboot-1.0.0.jar`

`scripts/dev-start.sh` 会自动执行：

```bash
mvn -P 'apps,!release' -pl xxl-job-admin,xxl-job-executor-samples/xxl-job-executor-sample-springboot -am -DskipTests package
```

所以之前“直接跑起来”的一个原因，也是脚本把首次打包一起兜住了。

## 建议的稳定用法

### 最稳的本地联调方式

后端：

```bash
bash scripts/dev-start.sh
```

前端：

```bash
bash scripts/run-admin-ui.sh
```

状态：

```bash
bash scripts/dev-status.sh
```

停止：

```bash
bash scripts/dev-stop.sh
```

### 不推荐的方式

不推荐直接裸跑下面这些命令，除非你先确认环境已经对齐：

```bash
mvn compile
pnpm typecheck
pnpm dev
```

原因不是命令本身不对，而是它们会直接吃当前 shell 的 Java/Node，容易被本机旧环境污染。

## 本次阶段性记录

截至 2026-06-11，这次第二阶段告警能力开发时，代码层面已经走到：

- 告警渠道模型与表结构补齐
- 任务绑定告警渠道
- 失败告警通道化发送
- 告警记录落库
- `alerts` 管理页与记录页接入

但完整编译验证还受本机工具链影响：

- Java 未切到 17 时，Maven 会失败
- 默认 Node 指向旧 15 时，pnpm / Vite 会失败

因此后续任何人接手时，先按本 runbook 统一启动方式，不要先从裸命令开始试。

### 这次接口 404 的实际原因

这次告警渠道列表接口最开始表现为：

```text
/proxy-default/alarmchannel/pageList -> 404
```

表面看像是前端 Vite 代理没配对，但实际不是。

最终确认链路如下：

1. 先测前端代理地址，出现 404
2. 再直接测后端地址 `http://127.0.0.1:8080/xxl-job-admin/alarmchannel/pageList`
3. 发现后端直连也 404
4. 说明不是前端代理问题，而是本机正在运行的 admin 还是旧 jar，新加的 controller 根本没启动进去

修复过程：

1. 用 JDK 17 重新执行 Maven 打包
2. 重启 admin
3. 再测接口时，404 消失，变成了数据库报错

这一步非常关键，因为它说明：

- Vite 代理本身是通的
- 前端请求路径本身没写错
- 真正的问题是运行中的后端版本过旧

### 404 消失后为什么又变成 500

重新启动新后端后，接口返回不再是 404，而是：

```text
Table 'xxl_job.xxl_job_alarm_channel' doesn't exist
```

这说明新代码已经生效，但数据库还是旧结构。

根因是：

- `scripts/dev-start.sh` 只会在数据库首次初始化时导入整份 SQL
- 如果库已经存在，它不会自动补告警相关新增表和字段

因此这次修复除了代码编译，还需要做增量迁移：

- `xxl_job_info.alarm_channel_ids`
- `xxl_job_alarm_channel`
- `xxl_job_alarm_record`

启动脚本只负责空库初始化，不再自动升级已有数据库。官方旧库必须先阅读迁移指南，再从 `docs/db/` 选择与来源版本匹配的唯一迁移 SQL，避免把互斥脚本全部执行。

## 相关文件

- [README](../README.md)
- [scripts/dev-start.sh](../scripts/dev-start.sh)
- [scripts/dev-status.sh](../scripts/dev-status.sh)
- [scripts/dev-stop.sh](../scripts/dev-stop.sh)
- [scripts/run-admin-ui.sh](../scripts/run-admin-ui.sh)
