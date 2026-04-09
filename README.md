# Connector Journal

## Overview

This project implements a Java-based connector that discovers, fetches, and transforms content from a legacy CMS API (simulated using [DummyJSON](https://dummyjson.com)) into structured documents suitable for indexing in a search platform.

The connector focuses on the **posts** endpoint and is designed with production-oriented ingestion principles, including reliability, fault tolerance, and extensibility.

---

## How to run

### Prerequisites

- **Java** 17+
- **Gradle** (or use the included wrapper)

### Run the connector

```bash
./gradlew run
```

### Run tests

```bash
./gradlew test
```

---

## Configuration

The connector is configurable via environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `API_BASE_URL` | `https://dummyjson.com` | Base API URL |
| `POSTS_ENDPOINT` | `/posts` | Endpoint for posts |
| `PAGE_SIZE` | `20` | Number of items per request |
| `MAX_RETRIES` | `5` | Retry attempts |
| `INITIAL_BACKOFF_MILLIS` | `1000` | Initial retry delay |
| `REQUESTS_PER_MINUTE` | `90` | Rate limit (below API max of 100) |
| `CHECKPOINT_FILE` | `data/checkpoint.json` | File for storing progress |
| `OUTPUT_FILE` | `output/posts.jsonl` | Output file |

---

## Architecture

The connector follows a layered and modular architecture:

```
config      → Configuration management
client      → HTTP API communication
crawl       → Orchestration + checkpointing
transform   → Mapping source → document
model       → Data models / DTOs
output      → Document persistence
util        → Retry + rate limiting utilities
```

### Key design principles

- Separation of concerns
- Composability and testability
- Extensibility for additional endpoints (comments, users)
- Minimal dependencies with explicit behavior

---

## Document schema

Each post is transformed into a structured document:

```json
{
  "id": "post-123",
  "sourceType": "post",
  "sourceId": 123,
  "title": "...",
  "body": "...",
  "tags": ["..."],
  "authorId": 1,
  "reactionCount": 10,
  "viewCount": 100,
  "searchText": "...",
  "sourceUrl": "...",
  "fetchedAt": "timestamp"
}
```

### Design rationale

- **`id`** — Stable for idempotent upserts
- **`searchText`** — Aggregates searchable content
- **`reactionCount` / `viewCount`** — Can be used as ranking signals
- **`sourceUrl`** — Enables traceability

---

## Reliability and error handling

The connector is designed to ensure no data is missed, even under unstable API conditions.

### Key guarantees

1. **Deterministic pagination** — Uses `limit` and `skip` to ensure full dataset coverage
2. **Checkpointing** — Progress is stored in a file (`checkpoint.json`); checkpoints are only updated after successful processing; safe resume after failure
3. **At-least-once processing** — Data may be reprocessed; data is never skipped
4. **Retry strategy** — Retries on HTTP 429, HTTP 5xx, and network failures; exponential backoff with jitter
5. **Rate limiting** — Enforces requests per minute, configured below API threshold for safety
6. **Retry-After handling** — Honors server-provided retry delays when available
7. **Fail-safe behavior** — Stops processing if a page cannot be safely fetched; prevents advancing checkpoint on failure

---

## Testing approach

Tests focus on validating core behavior rather than superficial coverage.

### Included tests

| Test | Purpose |
| --- | --- |
| **Mapper** | Validates transformation from source model to document |
| **Retry** | Ensures retry logic works under failure conditions |
| **Crawler** | Ensures checkpoint updates only after successful processing; verifies pagination behavior |

### Strategy

- Use deterministic inputs
- Mock external dependencies
- Validate behavior, not just structure

---

## Design tradeoffs

### Chosen

- File-based checkpointing (simple and reliable)
- Sequential processing (correctness over complexity)
- Explicit retry logic (transparency and control)
- Minimal dependencies (clarity over abstraction)

### Deferred

- Parallel processing (would require coordinated rate limiting)
- Distributed queues (e.g., Kafka, Service Bus)
- Circuit breaker framework
- Database persistence for checkpoints
- Full plugin system

---

## Future improvements

If extended further, the connector could include:

- Incremental sync using timestamps (if API supports it)
- Support for additional endpoints (comments, users)
- Parallel fetching with shared rate limiter
- Dead-letter handling for failed records
- Observability (metrics, tracing)
- Integration with a real indexing platform
- Circuit breaker for unstable upstream APIs

---

## Summary

This connector prioritizes correctness, reliability, and clarity over unnecessary complexity. It demonstrates how to safely ingest data from an unreliable API while ensuring completeness and recoverability.

The design intentionally favors at-least-once processing semantics and checkpoint-based recovery, aligning with the requirement that missing data is unacceptable.
