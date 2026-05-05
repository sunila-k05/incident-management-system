# Incident Management System (IMS)
> Zeotap Infrastructure / SRE Intern Assignment - Sunil K

A production-grade Incident Management System inspired by PagerDuty and Datadog.
Built with Java 17 + Spring Boot 3.5, React, PostgreSQL, MongoDB, and Redis.

GitHub: https://github.com/sunila-k05/incident-management-system

---

## Quick Start

git clone https://github.com/sunila-k05/incident-management-system.git
cd incident-management-system
docker-compose up --build

Wait 60 seconds then open:
- Dashboard: http://localhost:3000
- Health: http://localhost:8080/health

Run simulation:
bash scripts/simulate_failure.sh

---

## Architecture

INGESTION: POST /api/signals -> Rate Limiter (10k/sec) -> ArrayBlockingQueue
PROCESSING: Background worker -> Debounce Engine -> Strategy Pattern + State Machine
STORAGE: MongoDB (signals) + PostgreSQL (work items + RCA) + Redis (cache)
API: Spring Boot REST :8080
UI: React + Nginx :3000

---

## Tech Stack

Backend: Java 17 + Spring Boot 3.5
Database (Source of Truth): PostgreSQL 15 - ACID transactions
Database (Audit Log): MongoDB 7 - schema-free high throughput
Cache (Hot Path): Redis 7 - sub-ms dashboard reads
Rate Limiting: Bucket4j - token bucket algorithm
Async: Spring @Async + ArrayBlockingQueue
Retry: Spring Retry - exponential backoff
Frontend: React + Vite + Tailwind CSS
Proxy: Nginx - routes /api to backend container
Infra: Docker Compose

---

## Design Patterns

Strategy Pattern - Alerting:
RDBMS, API_GATEWAY -> P0 (critical)
MCP_HOST, ASYNC_QUEUE, NOSQL -> P1 (significant)
CACHE -> P2 (degraded)

State Pattern - Lifecycle:
OPEN -> INVESTIGATING -> RESOLVED -> CLOSED
Invalid transitions throw IllegalStateException.
CLOSED requires complete RCA.

---

## Backpressure Handling

Problem: 10,000 signals/sec, DB writes take 10-50ms.

Solution (3 layers):
1. Rate Limiter: Bucket4j returns HTTP 429 if exceeded
2. ArrayBlockingQueue: offer() is non-blocking - API returns 202 immediately
3. Background Worker: drains 500 signals every 100ms at DB's own pace

Spring Retry: @Retryable(maxAttempts=3, backoff 500ms->1000ms->2000ms)

---

## API Reference

POST /api/signals - ingest signal
POST /api/signals/batch - batch ingest
GET  /api/incidents - live feed sorted by priority
GET  /api/incidents/:id - detail
GET  /api/incidents/:id/signals - raw signals from MongoDB
PUT  /api/incidents/:id/investigate - OPEN -> INVESTIGATING
PUT  /api/incidents/:id/resolve - INVESTIGATING -> RESOLVED
PUT  /api/incidents/:id/close - RESOLVED -> CLOSED (requires RCA)
POST /api/rca/:id - submit RCA
GET  /health - system health + buffer stats

Component Types: RDBMS | API_GATEWAY | MCP_HOST | ASYNC_QUEUE | NOSQL | CACHE
Signal Types: LATENCY_SPIKE | CONNECTION_FAILURE | MEMORY_PRESSURE | CPU_SPIKE | ERROR_RATE_HIGH | HEALTH_CHECK_FAIL | CONNECTION_POOL_EXHAUSTED | QUEUE_DEPTH_HIGH
RCA Categories: INFRASTRUCTURE_FAILURE | DEPLOYMENT_REGRESSION | TRAFFIC_SPIKE | DEPENDENCY_FAILURE | CONFIGURATION_ERROR | HARDWARE_FAILURE | NETWORK_ISSUE | UNKNOWN

---

## Observability

/health returns per-service status + buffer metrics.

Console prints every 5 seconds:
======= IMS THROUGHPUT METRICS =======
Signals/sec: 847.0
Buffer size: 142/10000
Total received: 42350
======================================

---

## Testing

cd backend && mvn test

5 unit tests:
1. Close fails when RCA missing
2. Close fails when RCA incomplete
3. Close succeeds with complete RCA
4. Invalid state transition (OPEN->CLOSED) rejected
5. MTTR calculated correctly

---

## Simulation

bash scripts/simulate_failure.sh

t=0s  RDBMS latency spike (4500ms vs 500ms) -> P0
t=2s  RDBMS connection pool exhausted -> P0
t=5s  API Gateway error rate 85% -> P0
t=8s  MCP Host health check fails -> P1
t=10s Cache memory pressure 92% -> P2
t=12s Async Queue depth 45k -> P1

bash scripts/stress_test.sh (1000 signals throughput test)

---

## Project Structure

backend/src/main/java/com/zeotap/ims/
  controller/   REST endpoints + rate limiter
  engine/       SignalBuffer + DebounceEngine + SignalProcessor
  strategy/     P0AlertStrategy, P1AlertStrategy, P2AlertStrategy
  statemachine/ OpenState, InvestigatingState, ResolvedState, ClosedState
  model/        Signal (MongoDB), WorkItem (PG), Rca (PG)
  service/      Business logic, RCA validation, MTTR calculation
  repository/   MongoRepository + JpaRepository interfaces
  config/       AsyncConfig, RedisConfig, WebSocketConfig, RetryConfig

frontend/src/
  api/api.js          Axios client with relative URLs
  pages/Dashboard.jsx Live incident feed
  pages/IncidentDetail.jsx Signals + state buttons
  pages/RcaForm.jsx   RCA submission form
  nginx.conf          Proxies /api -> backend container

scripts/
  simulate_failure.sh Cascade failure simulation
  stress_test.sh      Throughput test

docs/
  PLANNING.md  Architecture decisions
  PROMPTS.md   LLM usage log

---

## Evaluation Rubric

Concurrency & Scaling  10%  ArrayBlockingQueue + ConcurrentHashMap + @Async
Data Handling          20%  MongoDB + PostgreSQL + Redis correct separation
LLD                    20%  Strategy Pattern + State Pattern
UI/UX & Integration    20%  Dark dashboard, live feed, signals, RCA form
Resilience & Testing   10%  Spring Retry + 5 unit tests + rate limiting
Documentation          10%  README + PLANNING.md + PROMPTS.md
Tech Stack Choices     10%  Every choice documented with reason

---

## Bonus Features

- Batch signal ingestion endpoint
- Cascade failure simulation script
- Stress test script
- MTTR auto-calculated on closure
- /health with per-service status and buffer metrics
- Nginx reverse proxy for Docker networking
- Multi-stage Docker builds
- All 6 component types and 8 signal types implemented
- Structured RCA categories

---

## Troubleshooting

ContainerConfig error (docker-compose 1.29.2 bug):
docker rm -f $(docker ps -aq)
docker-compose up --build

Port in use:
sudo fuser -k 8080/tcp
docker-compose up --build

---

Built by Sunil K - Zeotap Infrastructure / SRE Intern Assignment
GitHub: https://github.com/sunila-k05/incident-management-system
