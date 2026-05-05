# Incident Management System (IMS)
> Zeotap Infrastructure / SRE Intern Assignment

A production-grade, mission-critical Incident Management System inspired by how **PagerDuty** and **Datadog** handle real-world incidents at scale. Built with Java 17 + Spring Boot 3.5, React, PostgreSQL, MongoDB, and Redis.

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Tech Stack and Decisions](#tech-stack-and-decisions)
4. [Design Patterns](#design-patterns)
5. [How Backpressure is Handled](#how-backpressure-is-handled)
6. [Setup Instructions](#setup-instructions)
7. [API Reference](#api-reference)
8. [Observability](#observability)
9. [Testing](#testing)
10. [Simulation Scripts](#simulation-scripts)
11. [Project Structure](#project-structure)
12. [Evaluation Rubric Coverage](#evaluation-rubric-coverage)
13. [Bonus Features](#bonus-features)

---

## Overview

In production environments, thousands of signals (errors, latency spikes) arrive every second from distributed infrastructure — databases, caches, API gateways, message queues. Without intelligent grouping and workflow management, engineers get overwhelmed.

This system solves that by:
- **Ingesting** up to 10,000 signals/sec without crashing
- **Grouping** related signals intelligently (debounce logic)
- **Prioritizing** incidents automatically (P0/P1/P2)
- **Tracking** each incident through a strict lifecycle
- **Enforcing** Root Cause Analysis before closure
- **Calculating** MTTR automatically

---

## Architecture

```
+------------------------------------------------------------------+
|                      INGESTION LAYER                             |
|                                                                  |
|   POST /api/signals           POST /api/signals/batch            |
|         |                              |                         |
|         +--------------+--------------+                         |
|                        |                                         |
|           +------------v------------+                           |
|           |     Rate Limiter        |  Bucket4j Token Bucket    |
|           |     10,000 req/sec      |  Returns HTTP 429 if exceeded |
|           +------------+------------+                           |
+------------------------|-----------------------------------------+
                         |
+------------------------v-----------------------------------------+
|                  IN-MEMORY BUFFER                                |
|                                                                  |
|          ArrayBlockingQueue (capacity: 10,000)                   |
|                                                                  |
|   offer() is non-blocking — returns false if full               |
|   API thread NEVER waits — system never freezes                  |
|                                                                  |
|   Background worker drains 500 signals every 100ms              |
+------------------------+-----------------------------------------+
                         |
+------------------------v-----------------------------------------+
|                  DEBOUNCE ENGINE                                 |
|                                                                  |
|   ConcurrentHashMap<componentId, DebounceWindow>                |
|                                                                  |
|   If window active (under 10s):                                  |
|     Link signal to existing Work Item                            |
|   If window expired or new:                                      |
|     Create new Work Item via Strategy Pattern                    |
+----------+----------------------+--------------------------------+
           |                      |
    +------v------+    +----------v-----------------------------+
    |  Strategy   |    |         State Machine                  |
    |  Pattern    |    |                                        |
    |             |    |  OPEN -> INVESTIGATING                 |
    |  RDBMS -> P0|    |       -> RESOLVED -> CLOSED           |
    |  API_GW -> P0    |                                        |
    |  MCP -> P1  |    |  Invalid transitions throw             |
    |  QUEUE -> P1|    |  IllegalStateException                 |
    |  NOSQL -> P1|    |                                        |
    |  CACHE -> P2|    |  CLOSED requires complete RCA          |
    +------+------+    +----------------------------------------+
           |
+----------v-----------------------------------------------------------+
|                        STORAGE LAYER                                 |
|                                                                      |
|  +------------+  +--------------+  +----------+  +--------------+   |
|  |  MongoDB   |  |  PostgreSQL  |  |  Redis   |  |  PG Table   |   |
|  |            |  |              |  |          |  |             |   |
|  | Raw Signals|  |  Work Items  |  |Dashboard |  | Timeseries  |   |
|  | Audit Log  |  |  RCA Records |  |  Cache   |  |  MTTR       |   |
|  |            |  |              |  |          |  |  Metrics    |   |
|  | schema-free|  | ACID txns    |  | sub-ms   |  |             |   |
|  | high volume|  | transactional|  | reads    |  |             |   |
|  +------------+  +--------------+  +----------+  +--------------+   |
+----------------------------------------------------------------------+
                            |
+---------------------------v------------------------------------------+
|                  SPRING BOOT REST API (port 8080)                    |
|                                                                      |
|  /api/signals      Signal ingestion (single + batch)                 |
|  /api/incidents    Work item CRUD + state transitions                |
|  /api/rca          RCA submission and retrieval                      |
|  /health           System health + buffer metrics                    |
+---------------------------+------------------------------------------+
                            |
+---------------------------v------------------------------------------+
|                  REACT DASHBOARD (port 3000)                         |
|                                                                      |
|  Live Feed         Active incidents sorted by P0 to P2              |
|  Incident Detail   Raw signals from MongoDB + state buttons          |
|  RCA Form          Structured form with all required fields          |
|                                                                      |
|  Auto-polls every 5 seconds for live updates                        |
+----------------------------------------------------------------------+
```

---

## Tech Stack and Decisions

| Layer | Technology | Why This Choice |
|---|---|---|
| Backend | Java 17 + Spring Boot 3.5 | Enterprise-grade, native async support, widely used in SRE teams |
| Source of Truth | PostgreSQL 15 | ACID transactions — state transitions must be atomic, no partial updates |
| Audit Log | MongoDB 7 | Schema-free, high write throughput — perfect for variable signal payloads |
| Hot Path Cache | Redis 7 | Sub-millisecond reads — dashboard polls every 5s, cannot hit Postgres each time |
| Rate Limiting | Bucket4j | Token bucket algorithm, zero external dependencies, battle-tested |
| Async Processing | Spring @Async + ArrayBlockingQueue | Decouples ingestion from DB writes — core of backpressure handling |
| Retry Logic | Spring Retry | Exponential backoff on DB write failures — resilience requirement |
| Frontend | React + Vite + Tailwind CSS | Fast builds, responsive dark UI, component-based |
| Containerization | Docker Compose | One-command full stack setup |
| Build Tool | Maven 3.9 | Industry standard for Java, dependency management |

---

## Design Patterns

### 1. Strategy Pattern — Alerting Priority

Different component types need different alert priorities. Instead of if-else chains, each component type maps to an AlertStrategy:

```
RDBMS        ->  P0AlertStrategy   (database down = everything down)
API_GATEWAY  ->  P0AlertStrategy   (all traffic affected)
MCP_HOST     ->  P1AlertStrategy   (significant degradation)
ASYNC_QUEUE  ->  P1AlertStrategy   (message processing affected)
NOSQL        ->  P1AlertStrategy   (document store affected)
CACHE        ->  P2AlertStrategy   (performance degradation only)
```

Adding a new component type means adding one new Strategy class. Zero changes to existing code.

### 2. State Pattern — Incident Lifecycle

Each incident state is a class that enforces its own valid transitions:

```
OPEN          can only go to INVESTIGATING
INVESTIGATING can only go to RESOLVED
RESOLVED      can only go to CLOSED (requires complete RCA)
CLOSED        no further transitions allowed
```

Invalid transitions throw `IllegalStateException` with a clear message. No switch statements in business logic.

---

## How Backpressure is Handled

This is the most critical resilience feature of the system.

### The Problem
Signals arrive at 10,000/sec. Database writes take 10-50ms each. Without protection, the system would crash under load — the ingestion thread would block waiting for DB writes.

### The Solution — Three Layer Decoupling

**Layer 1 — Rate Limiter (Bucket4j)**
```
Client -> Rate Limiter -> Buffer
          10,000/sec
          Returns HTTP 429 if exceeded
```

**Layer 2 — ArrayBlockingQueue Buffer (capacity 10,000)**
```java
// offer() is NON-BLOCKING
// If buffer full -> returns false immediately -> signal dropped gracefully
// put() would BLOCK the thread -> system freezes -> NEVER use put()
boolean accepted = buffer.offer(signal);
```

The API thread calls `offer()` and returns `202 Accepted` immediately. It never waits for the database.

**Layer 3 — Background Worker**
```
Buffer -> Worker (every 100ms) -> Drain 500 signals -> Debounce Engine -> DB
```

A scheduled thread drains the buffer in batches of 500 every 100ms. DB writes happen at their own pace without affecting ingestion speed.

**Result:** The ingestion API never crashes regardless of how slow the persistence layer is. If the buffer fills up, signals are dropped gracefully with a warning log — the system keeps running.

### Spring Retry — DB Write Resilience
```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
private WorkItem createWorkItem(Signal signal) { ... }
```
Failed DB writes retry up to 3 times with exponential backoff (500ms -> 1000ms -> 2000ms).

---

## Setup Instructions

### Prerequisites
- Docker + Docker Compose
- Git
- jq (for testing scripts)

### One-Command Setup

```bash
git clone https://github.com/sunila-k05/incident-management-system.git
cd incident-management-system
docker-compose up --build
```

Wait for all services to be healthy (about 60-90 seconds), then open:

| Service | URL |
|---|---|
| React Dashboard | http://localhost:3000 |
| Spring Boot API | http://localhost:8080 |
| Health Check | http://localhost:8080/health |

### Run Without Docker (Development)

**Start databases:**
```bash
docker-compose up -d postgres mongodb redis
```

**Start backend:**
```bash
cd backend
mvn spring-boot:run
```

**Start frontend:**
```bash
cd frontend
npm install
npm run dev
# Open http://localhost:5173
```

---

## API Reference

### Signal Ingestion

```bash
# Single signal
POST /api/signals
Content-Type: application/json

{
  "componentId": "RDBMS_PRIMARY_01",
  "componentType": "RDBMS",
  "signalType": "LATENCY_SPIKE",
  "value": 4500,
  "threshold": 500,
  "unit": "ms",
  "region": "us-east-1",
  "metadata": {"host": "db-01.internal"}
}

# Batch ingestion
POST /api/signals/batch
Content-Type: application/json
[{...}, {...}, {...}]
```

### Component Types Supported
```
RDBMS | API_GATEWAY | MCP_HOST | ASYNC_QUEUE | NOSQL | CACHE
```

### Signal Types Supported
```
LATENCY_SPIKE | CONNECTION_FAILURE | MEMORY_PRESSURE | CPU_SPIKE
ERROR_RATE_HIGH | HEALTH_CHECK_FAIL | CONNECTION_POOL_EXHAUSTED | QUEUE_DEPTH_HIGH
```

### Incident Management

```bash
GET    /api/incidents                   # List active incidents (sorted by priority)
GET    /api/incidents/:id               # Incident detail
GET    /api/incidents/:id/signals       # Raw signals from MongoDB
PUT    /api/incidents/:id/investigate   # OPEN -> INVESTIGATING
PUT    /api/incidents/:id/resolve       # INVESTIGATING -> RESOLVED
PUT    /api/incidents/:id/close         # RESOLVED -> CLOSED (requires RCA)
```

### RCA

```bash
POST /api/rca/:workItemId   # Submit or update RCA
GET  /api/rca/:workItemId   # Get RCA for a work item
```

### RCA Root Cause Categories
```
INFRASTRUCTURE_FAILURE | DEPLOYMENT_REGRESSION | TRAFFIC_SPIKE
DEPENDENCY_FAILURE | CONFIGURATION_ERROR | HARDWARE_FAILURE
NETWORK_ISSUE | UNKNOWN
```

### Health

```bash
GET /health

# Response:
{
  "status": "UP",
  "timestamp": "2026-05-03T09:10:54Z",
  "postgres": "UP",
  "mongodb": "UP",
  "redis": "UP",
  "bufferSize": 142,
  "bufferCapacity": 10000,
  "totalSignalsReceived": 8472
}
```

---

## Observability

The `/health` endpoint returns real-time status of all 3 databases plus buffer stats.

Throughput metrics are logged to the console every 5 seconds:

```
======= IMS THROUGHPUT METRICS =======
Signals/sec     : 847.0
Buffer size     : 142/10000
Total received  : 42350
======================================
```

---

## Testing

### Unit Tests
```bash
cd backend
mvn test
```

**6 tests covering:**
1. Close fails when RCA is missing
2. Close fails when RCA is incomplete (empty fields)
3. Close succeeds with complete RCA
4. Invalid state transition (OPEN -> CLOSED) is rejected
5. MTTR is calculated correctly on close
6. Application context test

### Manual API Tests

```bash
# Install jq
sudo apt install jq -y

# Send a signal
curl -s -X POST http://localhost:8080/api/signals \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "RDBMS_PRIMARY_01",
    "componentType": "RDBMS",
    "signalType": "LATENCY_SPIKE",
    "value": 4500,
    "threshold": 500,
    "unit": "ms",
    "region": "us-east-1"
  }' | jq .

# Check incidents
curl -s http://localhost:8080/api/incidents | jq '[.[] | {priority, componentId, state, signalCount}]'

# Test RCA rejection (should return error)
curl -s -X PUT http://localhost:8080/api/incidents/{id}/close | jq .

# Check health
curl -s http://localhost:8080/health | jq .
```

---

## Simulation Scripts

### Full Cascade Failure Simulation
```bash
sudo apt install jq -y
./scripts/simulate_failure.sh
```

Simulates a realistic production cascade:

| Time | Event | Component | Priority |
|---|---|---|---|
| t=0s | RDBMS latency spike (4500ms vs 500ms threshold) | RDBMS_PRIMARY_01 | P0 |
| t=2s | Connection pool exhausted (100% vs 80%) | RDBMS_PRIMARY_01 | P0 |
| t=5s | API Gateway error rate spikes (85% vs 5%) | API_GATEWAY_01 | P0 |
| t=8s | MCP Host health check fails | MCP_HOST_01 | P1 |
| t=10s | Cache memory pressure (92% vs 80%) | CACHE_CLUSTER_01 | P2 |
| t=12s | Async Queue depth growing (45,000 vs 10,000) | ASYNC_QUEUE_01 | P1 |

Expected result — 5 incidents created with correct priorities, RDBMS signals debounced into 1 Work Item.

### Stress Test (Throughput)
```bash
./scripts/stress_test.sh
```

Sends 1,000 signals in batches of 5. Watch the backend console for throughput metrics.

---

## Project Structure

```
incident-management-system/
├── backend/
│   ├── src/main/java/com/zeotap/ims/
│   │   ├── controller/
│   │   │   ├── SignalController.java       (rate limiter here)
│   │   │   ├── WorkItemController.java
│   │   │   ├── RcaController.java
│   │   │   └── HealthController.java
│   │   ├── engine/
│   │   │   ├── SignalBuffer.java           (ArrayBlockingQueue)
│   │   │   ├── DebounceEngine.java         (ConcurrentHashMap)
│   │   │   └── SignalProcessor.java        (background worker + metrics)
│   │   ├── strategy/
│   │   │   ├── AlertStrategy.java          (interface)
│   │   │   ├── P0AlertStrategy.java
│   │   │   ├── P1AlertStrategy.java
│   │   │   ├── P2AlertStrategy.java
│   │   │   └── AlertStrategyResolver.java
│   │   ├── statemachine/
│   │   │   ├── IncidentState.java          (interface)
│   │   │   ├── OpenState.java
│   │   │   ├── InvestigatingState.java
│   │   │   ├── ResolvedState.java
│   │   │   ├── ClosedState.java
│   │   │   └── IncidentStateMachine.java
│   │   ├── model/
│   │   │   ├── Signal.java                 (MongoDB document)
│   │   │   ├── WorkItem.java               (PostgreSQL entity)
│   │   │   └── Rca.java                    (PostgreSQL entity)
│   │   ├── service/
│   │   │   ├── SignalIngestionService.java
│   │   │   ├── WorkItemService.java         (RCA validation + MTTR)
│   │   │   └── RcaService.java
│   │   ├── repository/
│   │   │   ├── SignalRepository.java        (MongoRepository)
│   │   │   ├── WorkItemRepository.java      (JpaRepository)
│   │   │   └── RcaRepository.java           (JpaRepository)
│   │   └── config/
│   │       ├── AsyncConfig.java             (thread pool config)
│   │       ├── RedisConfig.java
│   │       ├── WebSocketConfig.java
│   │       └── RetryConfig.java
│   ├── src/test/                            (unit tests)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/api.js                       (Axios API client)
│       ├── pages/
│       │   ├── Dashboard.jsx                (live incident feed)
│       │   ├── IncidentDetail.jsx           (signals + state buttons)
│       │   └── RcaForm.jsx                  (RCA submission form)
│       └── components/
│           ├── PriorityBadge.jsx
│           └── StatusBadge.jsx
├── scripts/
│   ├── simulate_failure.sh                  (cascade failure simulation)
│   └── stress_test.sh                       (throughput stress test)
├── docs/
│   ├── PLANNING.md                          (architecture decisions)
│   └── PROMPTS.md                           (LLM usage log)
├── docker-compose.yml
└── README.md
```

---

## Evaluation Rubric Coverage

| Category | Weight | How We Address It |
|---|---|---|
| Concurrency & Scaling | 10% | ArrayBlockingQueue buffer, ConcurrentHashMap debounce, @Async processing, no race conditions on state updates |
| Data Handling | 20% | MongoDB (raw signals/audit log), PostgreSQL (work items + RCA with ACID), Redis (dashboard cache hot path), separate concerns per store |
| LLD | 20% | Strategy Pattern (alerting), State Pattern (lifecycle), clean package structure, no code smells |
| UI/UX & Integration | 20% | Dark themed responsive React dashboard, live feed sorted by severity, incident detail with raw signals, RCA form with all required fields |
| Resilience & Testing | 10% | Spring Retry with exponential backoff, 5 unit tests for RCA validation, rate limiting with HTTP 429, buffer overflow protection |
| Documentation | 10% | This README, PLANNING.md, PROMPTS.md, inline code comments, API reference |
| Tech Stack Choices | 10% | Each technology chosen for a specific reason documented above |

---

## Docker Networking — How Frontend Reaches Backend

When running inside Docker Compose, containers communicate by **service name**, not `localhost`.

The frontend uses Nginx as a reverse proxy:
Browser → localhost:3000/api/incidents
→ Nginx (frontend container)
→ http://backend:8080/api/incidents
→ Spring Boot (backend container)
→ Response returned to browser
This is configured in `frontend/nginx.conf`:
```nginx
location /api {
    proxy_pass http://backend:8080/api;
}
```
## Features

- Batch signal ingestion endpoint (`POST /api/signals/batch`)
- Stress test script for load testing
- Realistic cascade failure simulation (RDBMS -> API -> MCP)
- Dark themed responsive UI inspired by real SRE dashboards
- MTTR displayed in human readable format
- Auto-polling dashboard (every 5 seconds)
- Detailed `/health` endpoint with buffer metrics
- All 6 component types from assignment implemented
- All 8 signal types implemented
- Structured RCA categories (not free text)

All API calls use relative URLs (`/api`) — no hardcoded `localhost`. This means the system works identically in Docker and in local development.

## GitHub Repository

**https://github.com/sunila-k05/incident-management-system**

*Built by Sunila K for Zeotap Infrastructure / SRE Intern Assignment*
