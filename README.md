# Connector Journal

## Overview

This project implements a Java-based connector that discovers, fetches, and transforms content from a legacy CMS API (simulated using [DummyJSON](https://dummyjson.com)) into structured documents suitable for indexing in a search platform.

The connector focuses on the **posts** endpoint and is designed with production-oriented ingestion principles, including reliability, fault tolerance, and extensibility.

---

## Submission checklist

This README explicitly covers the requested areas:

- How to run the connector
- Design decisions, including schema, structure, and tradeoffs
- Testing approach
- What you'd add with more time

The focus is working code with clear design, and the sections below document the key decisions and tradeoffs made.

---

## How to run the connector

### Prerequisites

- **Java** 17+
- **Gradle** (or use the included wrapper)

### Run the connector

Run the connector with the default configuration:

```bash
./gradlew run
```

With the default DummyJSON source, a fresh run starts at `skip=0`, crawls all available posts, and writes the transformed documents to `output/posts.jsonl`. Progress is checkpointed in `data/checkpoint.json`.

Expected behavior on a fresh run:

- The crawler logs progress in batches of 20 posts
- The final run completes after processing 251 posts
- The output is written as JSON Lines documents to `output/posts.jsonl`
- The checkpoint file is updated so a later run can resume safely

If the checkpoint already exists, the connector resumes from the saved `skip` value instead of reprocessing from the beginning. For a clean end-to-end run with the defaults, remove `data/checkpoint.json` and `output/posts.jsonl` before running again.

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

## Design decisions

This connector favors clear, working ingestion behavior over unnecessary feature complexity. The design emphasizes explicit contracts, restart safety, and tradeoffs that are easy to reason about.

### Structure

The connector follows a layered and modular structure:

```
config      → Configuration management
client      → HTTP API communication
crawl       → Orchestration + checkpointing
transform   → Mapping source → document
model       → Data models / DTOs
output      → Document persistence
util        → Retry + rate limiting utilities
error       → Typed retryable/non-retryable exceptions
```

Key structure decisions:

- Separation of concerns so crawling, transformation, persistence, and retry logic can evolve independently
- Composability and testability through small abstractions
- Extensibility for additional endpoints such as comments or users
- Minimal dependencies with explicit behavior instead of relying on framework magic
- SOLID-aligned abstractions:
  - `PostsClient` for source access
  - `DocumentMapper<S, D>` for source-to-document mapping
  - `DocumentSink<D>` for output persistence
  - `CrawlCheckpointStore` for checkpoint persistence

### Schema

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

Schema decisions:

- **`id`** — Stable for idempotent upserts
- **`searchText`** — Aggregates searchable content into a single field
- **`reactionCount` / `viewCount`** — Preserved as potential ranking signals
- **`sourceUrl`** — Supports traceability back to the source system
- **`fetchedAt`** — Records ingestion time for observability and reprocessing analysis

### Reliability and error handling

The connector is designed to ensure no data is missed, even under unstable API conditions.

Key guarantees:

1. **Deterministic pagination with response validation** — Uses `limit` and `skip`, and validates API metadata (`skip`, `limit`, `total`) before processing
2. **Checkpointing after successful write** — Checkpoint advances only after documents are persisted
3. **At-least-once processing** — Data may be reprocessed; data is never intentionally skipped
4. **Retry strategy** — Retries on typed retryable failures (`429`, `5xx`, network/deserialization); exponential backoff with jitter
5. **Retry-After handling** — Honors server-provided `Retry-After` when present
6. **Rate limiting** — Enforces requests per minute, configured below API threshold for safety
7. **Atomic checkpoint updates** — Uses temp file + move to avoid partial checkpoint writes
8. **Fail-closed behavior** — Corrupt checkpoint or unsafe pages fail the run instead of silently continuing

### Tradeoffs

Chosen tradeoffs:

- File-based checkpointing for simplicity and reliability
- Sequential processing to prioritize correctness over throughput
- Explicit retry logic for transparency and control
- Minimal dependencies to keep the operational model easy to understand

Deferred tradeoffs:

- Parallel processing, which would require coordinated rate limiting and more complex failure handling
- Distributed queues such as Kafka or Service Bus
- A circuit breaker framework for upstream instability
- Database-backed checkpoint persistence
- A full plugin system for multiple content sources

---

## Testing approach

Tests focus on validating core behavior rather than superficial coverage.

### Included tests

| Test | Purpose |
| --- | --- |
| **Mapper** | Validates transformation from source model to document |
| **Retry executor** | Validates retry behavior, retry limits, argument validation, and `Retry-After` handling |
| **Crawler** | Ensures checkpoint updates only after successful processing; verifies pagination behavior and resume safety |
| **Page validator** | Validates response metadata contract and rejects unsafe page states |
| **HTTP client** | Verifies classification of `429`/`5xx`/`4xx` and malformed payload behavior |
| **Rate limiter** | Verifies request spacing and configuration validation |
| **Checkpoint store** | Verifies checkpoint persistence, fail-closed loading, and negative-skip rejection |

### Strategy

- Use deterministic inputs
- Mock external dependencies
- Validate behavior, not just structure
- Prioritize production failure modes (rate limits, unstable upstreams, crash/restart recovery, and state corruption)

---

## What I'd add with more time

With additional time, the connector could include:

- Additional integration tests for client HTTP behavior (`429`, `5xx`, malformed payloads, retry-after timing)
- End-to-end crash/restart test proving full replay + resume behavior
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
