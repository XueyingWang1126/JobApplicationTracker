# job-application-tracker

## Project Overview

`job-application-tracker` is a productized personal project built around a real job-search pain point: keeping applications, status updates, notes, and role-specific attachments organized in one place.

Instead of a demo-only CRUD app, this repository is structured as a small deliverable product for portfolio review:
- account-based usage (register/login)
- dashboard summary for quick progress tracking
- full application management workflow
- attachment storage integration for CV/cover-letter variants

Core user journey:
`register -> login -> dashboard -> CRUD applications -> upload attachments`

## Features

- Email registration and login
- Dashboard summary (`region x status`) plus recent entries
- Application CRUD (company, role, status, region, applied date, link, notes)
- Attachment upload/download/delete linked to an application
- Company auto-fill helper (summary/website/country suggestion)
- Per-user data isolation (each user sees only their own records)

## Tech Stack

- Java 17
- Spring Boot
- Thymeleaf
- Apache Shiro
- MyBatis-Plus
- PostgreSQL
- MinIO
- Docker Compose
- Maven

## Architecture (High-Level)

- `Controller` layer: request handling + page model assembly
- `Service` layer: business rules, ownership checks, storage orchestration
- `Mapper` layer: MyBatis-Plus persistence access
- `Entity/DTO/VO`: data contracts between DB, services, and views
- `Templates`: server-rendered UI with Thymeleaf

Runtime services:
- Web app: `http://localhost:8080`
- PostgreSQL: `localhost:55432`
- MinIO API: `localhost:9000`
- MinIO Console: `http://localhost:9001`

## Getting Started

### Prerequisites

- Windows + PowerShell
- Java 17
- Maven 3.8+
- Docker Desktop (running)

### Setup

```powershell
Copy-Item .env.example .env -Force
```

Update `.env` with your own credentials before first run.

## Run Locally (Docker + run.ps1)

One-command startup:

```powershell
Copy-Item .env.example .env -Force
.\run.ps1
```

What `run.ps1` does:
- loads `.env`
- validates required environment variables
- starts Docker services (`postgres`, `minio`, `minio-init`)
- waits for dependencies to be ready
- starts Spring Boot

Default local endpoints:
- Web Login: `http://localhost:8080/login`
- Dashboard: `http://localhost:8080/dashboard`
- Applications: `http://localhost:8080/applications`
- Attachments: `http://localhost:8080/documents`
- PostgreSQL: `localhost:55432`
- MinIO API: `localhost:9000`
- MinIO Console: `http://localhost:9001`

## Environment Variables

Template file: `.env.example`  
Local file: `.env` (do not commit)

| Variable | Purpose |
| --- | --- |
| `APP_PORT` | Spring Boot HTTP port (default `8080`) |
| `DB_HOST` | PostgreSQL host |
| `DB_PORT` | PostgreSQL port (default `55432`) |
| `DB_NAME` | Database name |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `MINIO_ENABLED` | Enable/disable MinIO integration (`true`/`false`) |
| `MINIO_ENDPOINT` | MinIO endpoint (default `http://localhost:9000`) |
| `MINIO_ACCESS_KEY` | MinIO access key |
| `MINIO_SECRET_KEY` | MinIO secret key |
| `MINIO_BUCKET` | MinIO bucket name |

Important:
- Commit `.env.example`
- Never commit `.env`
- Never hardcode real credentials in source files

## Testing

Run test suite:

```powershell
mvn "-Djava.awt.headless=true" clean test
```

If you only need standard unit/integration execution:

```powershell
mvn test
```

## Troubleshooting

### 1) Port 8080 already in use

Check:

```powershell
netstat -ano | Select-String ':8080'
```

Fix:
- change `APP_PORT` in `.env` (for example `8081`)
- rerun `.\run.ps1`

### 2) Docker port conflicts (`55432`, `9000`, `9001`)

Check:

```powershell
netstat -ano | Select-String ':55432|:9000|:9001'
```

Fix options:
- stop conflicting local services
- or change published ports in `docker-compose.yml` and align `.env` / app config

### 3) Windows permission / script execution issues

Symptoms:
- `run.ps1` blocked by execution policy
- Docker command access denied

Fix:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

Then rerun:

```powershell
.\run.ps1
```

Also ensure Docker Desktop is started and healthy (`docker info`).

## Security Notes

### Why MD5 is not recommended

MD5 is considered cryptographically broken for password storage (fast hash, vulnerable to brute-force/rainbow-table style attacks).

### Current project approach

- New passwords are stored with BCrypt in `UserServiceImpl`
- A legacy MD5+salt verification branch is still present only for backward compatibility/migration safety

### Planned hardening path

- Remove MD5 fallback after legacy data migration window
- Add password policy and optional reset flow
- Add stricter upload validation (type/size scanning strategy)
- Add CI secret scanning in pull-request pipeline

## Screenshots

Add portfolio screenshots here:

- `docs/screenshots/login.png` (placeholder)
- `docs/screenshots/dashboard.png` (placeholder)
- `docs/screenshots/applications.png` (placeholder)
- `docs/screenshots/documents.png` (placeholder)
