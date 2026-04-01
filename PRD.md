# Product Requirements Document (PRD)

## Problem Statement:

Modern backend systems require asynchronous processing for tasks such as file processing, notifications, and data pipelines.

Building such systems introduces challenges:

- Reliable job execution
- Handling failures and retries
- Coordinating multiple workers
- Ensuring durability and recovery
- Managing delayed execution

Goal:

Build a practical, from-scratch async job processing system that:

- Demonstrates real system design concepts
- Is simple enough to implement fully
- Avoids unnecessary distributed complexity
- Is extensible for future improvements

## Solution Overview:

TaskFlow is a modular backend system that enables asynchronous job execution using:

- A REST API for job submission and tracking
- A persistent job store
- A queue mechanism for dispatching jobs
- Worker processes for execution
- Retry and failure handling mechanisms
- Optional scheduling for delayed jobs

The system will operate as a modular monolith while simulating distributed behavior through multiple workers.

## Core Features & Functional Requirements:
### 3.1 Job Management API

Endpoints:
- POST /jobs → Submit a job
- GET /jobs/{id} → Get job status
- DELETE /jobs/{id} → Cancel job
- GET /jobs → List jobs (filter by status/type)

Job Model:
{
  "id": "uuid",
  "type": "string",
  "payload": "json",
  "status": "queued | running | completed | failed",
  "scheduled_at": "timestamp (optional)",
  "retry_count": 0,
  "max_retries": 3,
  "created_at": "timestamp",
  "updated_at": "timestamp"
}

### 3.2 Queue System
Responsibilities:

- Store jobs ready for execution
- Dispatch jobs to workers
- Ensure jobs are not lost
- Implementation Approach
- Queue backed by PostgreSQL (no external broker)
- Workers poll for available jobs
- Jobs transition from queued → running

### 3.3 Worker System
Responsibilities:

- Fetch jobs from queue
- Execute job logic
- Update job status

Features:
- Multiple worker processes supported
- Concurrent execution (thread pool)
- Graceful shutdown support

### 3.4 Retry Mechanism
Features:

- Retry failed jobs automatically
- Increment retry_count
- Stop retrying after max_retries

### 3.5 Dead Letter Queue (DLQ)
Features:

- Jobs exceeding retry limit are marked as failed
- Stored for inspection/debugging
- No reprocessing

### 3.6 Scheduler (Delayed Jobs)
Features:

- Jobs with scheduled_at are delayed
- Workers ignore jobs until scheduled time
- No recurring jobs (kept simple)

### 3.7 Worker Heartbeats
Features:

- Workers periodically update heartbeat timestamp
- Used to detect inactive workers (basic tracking)

### 3.8 Basic Observability
Metrics (simple implementation):

- Total jobs processed
- Failed jobs count
- Retry count
- Logging
- Job lifecycle events
- Worker execution logs

## System Behavior
Job Lifecycle:

- Client submits job via API
- Job stored in PostgreSQL with status queued
- Worker polls database for available jobs
- Worker claims job → status set to running
- Worker executes task
- On success → completed
- On failure:
  - Retry if under limit
  - Otherwise → DLQ

Delivery Semantics:

- At-least-once delivery
- Jobs may execute more than once in failure scenarios

Failure Handling:
Worker crash

- Job remains in running state → can be retried manually or via timeout logic (optional extension)

API crash

- No data loss due to PostgreSQL persistence

## System Architecture
Components:

- API Layer
- Job Service (business logic)
- Queue Logic (DB-backed)
- Worker Module
- Scheduler (time-based filtering)
- MySQL Database

Architecture Flow:

Client → API → PostgreSQL (store job)
                    ↓
            Worker polls database
                    ↓
           Job execution (worker)
                    ↓
        Update job status in DB

Deployment Model:

- Single application (modular monolith)
- Multiple worker instances can run concurrently

## Key Design Decisions

Storage:
- MySQL is the single source of truth

Queue Strategy:
- Database-backed queue (simpler, reliable)

Delivery Guarantee:
- At-least-once delivery

Concurrency:
- Multiple workers simulate distributed processing

Complexity Management:
- Avoid external systems (Redis, Kafka, etc.)
- Keep architecture simple but extensible

## Data Model
Tables:
jobs

- id
- type
- payload
- status
- scheduled_at
- retry_count
- max_retries
- created_at
- updated_at

job_attempts (optional but recommended)

- job_id
- attempt_number
- status
- error_message

workers

- worker_id
- last_heartbeat

## Tech Stack
Backend:

- Java 21
- Spring Boot
- Gradle

Database:

- MySQL

Infrastructure:

- Docker

Testing:

- JUnit 5

API Docs:
- OpenAPI / Swagger

## Success Criteria
Jobs persist reliably in database
Multiple workers process jobs concurrently
Failed jobs retry correctly
System continues functioning under worker failure
Job lifecycle transitions are correct and observable

## Future Enhancements (Not in Scope)
Redis-based queueing
Distributed broker / partitioning
Priority queues
Idempotency keys
Prometheus/Grafana monitoring
Microservices architecture

---

## Roadmap

Build sequentially — each phase is independently verifiable before proceeding to the next.

### Phase 1 — Project Scaffolding
- Initialize Spring Boot project with Gradle
- Add dependencies: Spring Web, Spring Data JPA, MySQL Driver, Lombok, OpenAPI/Swagger
- Set up Docker Compose for local MySQL
- Configure `application.properties` for DB connection
- **Verify:** app starts and connects to DB

### Phase 2 — Data Layer
- Write MySQL schema (`jobs`, `job_attempts`, `workers` tables)
- Create JPA entities: `Job`, `JobAttempt`, `Worker`
- Create Spring Data repositories
- **Verify:** schema auto-creates via Hibernate; entities persist and retrieve correctly

### Phase 3 — Job Management API
- Implement `JobService` (business logic layer)
- Implement REST controller with all 4 endpoints:
  - `POST /jobs`
  - `GET /jobs/{id}`
  - `DELETE /jobs/{id}`
  - `GET /jobs` (with `status` / `type` query filters)
- Add request/response DTOs
- Configure OpenAPI / Swagger UI
- **Verify:** all endpoints work correctly via Swagger UI

### Phase 4 — Queue System (DB-backed)
- Implement queue polling: `SELECT` jobs where `status = queued` AND `scheduled_at <= NOW()`
- Use `SELECT ... FOR UPDATE SKIP LOCKED` for safe concurrent claiming
- Transition job status `queued → running` on claim
- **Verify:** jobs are claimed without duplication under concurrent access

### Phase 5 — Worker System
- Implement `Worker` runnable with a job handler registry (`job.type → handler`)
- Add a sample job handler (e.g. `EmailJobHandler`, no-op impl)
- Run multiple worker threads via `ExecutorService` / `@Scheduled`
- Update job status to `completed` or `failed` after execution
- **Verify:** multiple workers concurrently process jobs without overlap

### Phase 6 — Retry Mechanism & Dead Letter Queue
- On failure: increment `retry_count`, re-queue if `retry_count < max_retries`
- On `max_retries` exceeded: set `status = failed` (DLQ behavior — stored for inspection)
- Record each attempt in `job_attempts` table
- **Verify:** a failing job retries N times then lands in `failed` state with full attempt history

### Phase 7 — Scheduler (Delayed Jobs)
- Queue poll filters out jobs where `scheduled_at > NOW()`
- **Verify:** a job submitted with a future `scheduled_at` is ignored until that time passes

### Phase 8 — Worker Heartbeats
- Workers periodically upsert `last_heartbeat` timestamp in `workers` table
- **Verify:** heartbeat timestamp updates at regular intervals while worker is active

### Phase 9 — Observability
- Add structured logging for all job lifecycle events: submitted, claimed, completed, failed, retried
- Add `GET /metrics` endpoint returning aggregate counts: `total_processed`, `total_failed`, `total_retried`
- **Verify:** logs trace a full job lifecycle end-to-end; metrics endpoint returns accurate counts

### Phase 10 — Testing & Cleanup
- Write JUnit 5 unit tests for `JobService`, retry logic, and queue claiming
- Write integration tests against a real MySQL instance using Testcontainers
- Final review and cleanup of OpenAPI docs
- **Verify:** all tests pass; Swagger docs accurately reflect the API