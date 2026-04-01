# TaskFlow

An asynchronous job processing system built from scratch. TaskFlow demonstrates real system design concepts — reliable job execution, retry logic, delayed scheduling, and worker coordination — without relying on any external message broker.

---

## What It Does

TaskFlow lets you submit background jobs via a REST API. Workers continuously poll a database-backed queue, claim jobs atomically, execute them, and update their status. Failed jobs are automatically retried up to a configurable limit, then moved to a dead letter state for inspection. Jobs can be delayed by setting a future `scheduledAt` timestamp — workers ignore them until that time passes.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| Build | Maven (Maven Wrapper) |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Infrastructure | Docker / Docker Compose |
| Testing | JUnit, Mockito, Testcontainers |

---

## System Architecture

```
Client
  │
  ▼
REST API  ──────────────────────────────────────────────────
  │  POST /jobs          Submit a job (status: QUEUED)      │
  │  GET  /jobs/{id}     Get job status                     │
  │  DELETE /jobs/{id}   Cancel a job                       │
  │  GET  /jobs          List jobs (filter by status/type)  │
  │  GET  /metrics       Aggregate counts                   │
  │                                                         │
  ▼                                                         │
JobService                                                  │
  │                                                         │
  ▼                                                         │
MySQL ◄──────────────────────────────────────────────────────
  │   jobs / job_attempts / workers tables
  │
  ▼  (polled every 1s)
WorkerLoop  ──► JobQueueService.claimNextJob()
                  │  SELECT FOR UPDATE SKIP LOCKED
                  │  status: QUEUED → RUNNING
                  ▼
             ExecutorService (3 threads)
                  │
                  ▼
             JobExecutionService.execute()
                  │
                  ├── handler.handle(job)  ──► success → COMPLETED
                  │                                 └── records JobAttempt
                  │
                  └── exception → retryCount < maxRetries?
                                    ├── yes → retryCount++, status: QUEUED
                                    └── no  → status: FAILED (DLQ)
                                    └── records JobAttempt either way
```