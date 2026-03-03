# job-application-tracker

`job-application-tracker` 是一个基于我个人求职状态下的痛点所开发的可直接交付的求职管理小产品：帮助求职者集中管理投递流程、状态与每个岗位针对性简历与cover letter等附件。

核心用户旅程：
`register -> login -> dashboard -> CRUD applications -> upload attachments`

## Tech Stack

- Spring Boot
- Thymeleaf
- Apache Shiro
- MyBatis-Plus
- PostgreSQL
- MinIO
- Docker Compose

## Windows Quick Start (Recommended)

1. 复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

2. 编辑 `.env`，把占位符（`change_me_*`）替换成你自己的值。

3. 一键启动（Docker + Spring Boot）：

```powershell
.\run.ps1
```

4. 访问：
- Login: `http://localhost:8080/login`
- Dashboard: `http://localhost:8080/dashboard`
- Applications: `http://localhost:8080/applications`
- Attachments: `http://localhost:8080/documents`
- MinIO Console: `http://localhost:9001`

## Manual Start (Alternative)

```powershell
Copy-Item .env.example .env
docker compose up -d
mvn -DskipTests spring-boot:run
```

## Environment Variables

复制 `.env.example` 为 `.env` 后，按需填写：

| 字段 | 用途 |
| --- | --- |
| `APP_PORT` | Spring Boot HTTP 端口（默认 8080） |
| `DB_HOST` | PostgreSQL 主机地址 |
| `DB_PORT` | PostgreSQL 端口（Compose 默认映射 55432） |
| `DB_NAME` | 应用数据库名 |
| `DB_USER` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码（必须自定义，不能是占位符） |
| `MINIO_ENABLED` | 是否启用 MinIO（`true/false`） |
| `MINIO_ENDPOINT` | MinIO 服务地址 |
| `MINIO_ACCESS_KEY` | MinIO 访问账号（必须自定义） |
| `MINIO_SECRET_KEY` | MinIO 访问密码（必须自定义） |
| `MINIO_BUCKET` | 附件桶名称 |

安全约束：
- 不要提交 `.env`
- 只提交 `.env.example`
- 所有敏感值必须通过环境变量注入，仓库内不硬编码真实凭证

## Acceptance Checklist (Manual)

1. 打开 `/register`，注册一个新账号
2. 打开 `/login`，使用新账号登录
3. 打开 `/dashboard`，看到概览（summary + recent）
4. 打开 `/applications`，完成应用记录 CRUD
5. 在创建/编辑应用时上传附件
6. 打开 `/documents`，确认附件总览可查看/下载/删除
7. 登出后访问 `/dashboard` 或 `/applications`，应重定向到 `/login`

## Automated Test

```powershell
mvn "-Djava.awt.headless=true" clean test
```

## Troubleshooting

### 8080 端口被占用

- 检查占用：

```powershell
netstat -ano | Select-String ':8080'
```

- 改端口：在 `.env` 中设置 `APP_PORT=8081`（或其他可用端口），重新执行 `.\run.ps1`。

### Docker 未启动

- 快速判断：

```powershell
docker info
```

- 如果命令报错或无法连接 daemon，请先启动 Docker Desktop 再重试。

### MinIO 控制台地址

- 控制台：`http://localhost:9001`
- 账号密码：使用 `.env` 中的 `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`

## Naming Consistency

公开发布前，建议仓库目录名统一为 `job-application-tracker`。

如果本地目录名仍不是 `job-application-tracker`（例如 `JobApplication Tracker` 或历史旧名称），可在 Windows PowerShell 安全改名：

```powershell
Set-Location ..
$oldName = "JobApplication Tracker"   # 改成你的当前目录名
Rename-Item $oldName "job-application-tracker"
Set-Location .\job-application-tracker
```

说明：
- 改名前先关闭占用该目录的终端/IDE
- 改名只影响本地目录名，不影响 Git 历史

## Security Audit

完整安全审计与可复现扫描命令见：
- `docs/SECURITY_AUDIT.md`
- `docs/CODE_QUALITY_AUDIT.md`
