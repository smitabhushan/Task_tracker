# Team Task Tracker API

Spring Boot + MySQL + Redis implementation of the SDE II assignment brief.

## Run

```bash
docker compose up --build
```

API: `http://localhost:8080`

Swagger/OpenAPI: `http://localhost:8080/swagger-ui.html`

React frontend: `http://localhost:5173`

## Render Deployment

Create/provide a reachable MySQL database before deploying the API. Render's container cannot connect to the local Docker MySQL service name (`mysql`) or to `localhost`, so set these environment variables on the Render web service:

```bash
DB_URL=jdbc:mysql://<mysql-host>:3306/task_tracker?useSSL=true&allowPublicKeyRetrieval=true
DB_USERNAME=<mysql-user>
DB_PASSWORD=<mysql-password>
JWT_SECRET=<long-production-secret>
```

Render provides `PORT` automatically; the API reads it at startup. If you are not attaching Redis on Render, disable Redis-backed caching with:

```bash
CACHE_TYPE=simple
```

If you do attach Redis, keep `CACHE_TYPE=redis` and set:

```bash
REDIS_HOST=<redis-host>
REDIS_PORT=6379
```

## Auth Flow

1. `POST /api/auth/register` creates an organization and its first `ADMIN`.
2. `POST /api/auth/login` returns a JWT access token and refresh token.
3. `POST /api/auth/refresh` rotates the refresh token. Refresh tokens are stored only as SHA-256 hashes and the previous token is revoked.

Use `Authorization: Bearer <accessToken>` for protected endpoints.

## Main Endpoints

- `POST /api/users`, `GET /api/users`, `PATCH /api/users/{id}`: `ADMIN` only.
- `POST /api/projects`, `PUT /api/projects/{id}`, `DELETE /api/projects/{id}`: `ADMIN`, `MANAGER`.
- `GET /api/tasks`: all roles; `MEMBER` is automatically limited to assigned tasks.
- `POST /api/tasks`, `PUT /api/tasks/{id}`, `DELETE /api/tasks/{id}`: `ADMIN`, `MANAGER`.
- `PATCH /api/tasks/{id}/status`: assignee, `MANAGER`, or `ADMIN`.
- `GET /api/analytics/summary`: bonus analytics for overdue task count per user and average completion time.

Task status transitions are enforced server side:

`TODO -> IN_PROGRESS -> IN_REVIEW -> DONE`, with `BLOCKED` reachable from active states and reopenable to `IN_PROGRESS`.

## Database Design Query

The database is created automatically by Flyway from [V1__init_schema.sql](src/main/resources/db/migration/V1__init_schema.sql). To run manually, execute that file against MySQL after creating the `task_tracker` database.

Core schema:

- `organizations`: tenant boundary.
- `users`: belongs to one organization, has `ADMIN`, `MANAGER`, or `MEMBER` role.
- `projects`: belongs to one organization.
- `tasks`: belongs to one organization and project; assigned to one user.
- `refresh_tokens`: hashed rotating refresh tokens.

Indexes:

- `idx_tasks_status(status)`
- `idx_tasks_assignee(assignee_id)`
- `idx_tasks_due_date(due_date)`
- `idx_tasks_org_filters(organization_id, status, priority, assignee_id, due_date)`

Design decision: every operational table carries `organization_id` directly. Even though task organization could be inferred through project, storing it on `tasks` keeps authorization and filtered list queries simple, indexable, and harder to accidentally query across tenants.

## Caching Strategy

Redis caches task-list responses per assignee using the cache name `tasksByAssignee`. The cache key includes organization, effective assignee, status, priority, page, and limit.

Invalidation strategy: any task create, update, delete, reassignment, or status transition evicts `tasksByAssignee`. This is intentionally broad and correct for the assignment size; a production version could evict only affected assignees and pages.

## Example Requests

Register:

```bash
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"organizationName\":\"NxtWave\",\"name\":\"Admin\",\"email\":\"admin@nxtwave.com\",\"password\":\"password123\"}"
```

Create project:

```json
{
  "name": "Platform",
  "description": "Core APIs"
}
```

Create task:

```json
{
  "projectId": 1,
  "assigneeId": 2,
  "title": "Implement task filters",
  "description": "Support status, priority and assignee filters",
  "priority": "HIGH",
  "dueDate": "2026-06-15"
}
```

## Error Format

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "dueDate due_date must be a future or present date",
  "timestamp": "2026-05-30T00:00:00Z"
}
```

## Improvements With More Time

- Add Testcontainers integration tests for login, refresh rotation, RBAC, and task transitions.
- Add precise Redis eviction for only affected assignee cache keys.
- Add audit logs for task assignment and status changes.
- Add SSE/WebSocket notifications for assigned task status changes.
