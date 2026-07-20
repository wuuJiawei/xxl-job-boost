# 数据库脚本

本目录只保留四份可直接选择的数据库入口 SQL。迁移脚本互斥，不按文件名顺序连续执行。

| 文件 | 用途 | 是否保留原数据 |
| --- | --- | --- |
| [`install-xxl-job-boost.sql`](./install-xxl-job-boost.sql) | 空库全新部署 XXL-JOB Boost | 不适用于已有库 |
| [`migrate-from-official-3.4.2.sql`](./migrate-from-official-3.4.2.sql) | 官方 XXL-JOB `3.4.2` 迁移到 Boost | 是 |
| [`migrate-from-official-3.0.0.sql`](./migrate-from-official-3.0.0.sql) | 官方 XXL-JOB `3.0.0` 迁移到 Boost | 是 |
| [`migrate-from-official-2.4.x-2.5.x.sql`](./migrate-from-official-2.4.x-2.5.x.sql) | 官方 XXL-JOB `2.4.x / 2.5.x` 迁移到 Boost | 是 |

执行前必须：

1. 阅读[完整迁移指南](../migration-from-xxl-job.md)。
2. 停止对应 admin 实例。
3. 备份数据库并验证备份可以恢复。
4. 根据准确来源版本只选择一份 SQL。
5. 先在预发环境完整演练。

官方 `2.3.x` 及更早版本不支持直接运行本目录迁移 SQL。先按官方升级路径到 `2.4.2` 或 `2.5.0`。
