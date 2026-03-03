# Architecture Overview

## Product Boundary

`job-application-tracker` is a server-rendered Spring Boot web app for personal job-search tracking.

Core capabilities:
- Account registration and login
- Application CRUD
- Dashboard summary and recent records
- Attachment upload/download/delete (stored in MinIO)

## Runtime Topology

- App: Spring Boot (`localhost:${APP_PORT}`)
- DB: PostgreSQL (`DB_HOST:DB_PORT`)
- Object storage: MinIO (`MINIO_ENDPOINT`)

All runtime credentials are environment-variable driven (`.env` local only, `.env.example` in repo).

## Layering

1. Controller: request routing + view model assembly
2. Service: business rules + ownership checks
3. Mapper: MyBatis-Plus persistence layer
4. Entity/DTO/VO: database models and page payloads
5. Thymeleaf templates: server-side rendered UI

## Security Model

- Apache Shiro session auth (`/login` + protected routes)
- Per-user data isolation:
  - application queries always filter by current user
  - document queries/download/delete always validate ownership

## Data Model

- `app_users`: login accounts (`email`, `password`, optional legacy `salt`)
- `applications`: per-user application records
- `documents`: per-user attachment metadata with optional `application_id`

Schema bootstrap is handled by `src/main/resources/schema.sql`.
