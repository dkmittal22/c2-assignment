# Architecture Decisions

Status: Implemented (v1). These decisions were recorded during the specification phase, before implementation began, and they describe the design that the current implementation follows; runtime behavior is verified by the automated test suite (`mvn clean test` passes 121 tests).

## ADR-001 — Full-stack modular monolith

**Decision:** One Spring Boot backend and one React (Vite) SPA. No microservices.

**Why:** Matches v1 scope; keeps deployment and testing simple.

**Consequences:** Single API surface (`/api/v1`); CORS required for local SPA ↔ API.

## ADR-002 — Layered backend

**Decision:** Controller → Service → Repository → Database.

**Why:** Aligns with project Java/Spring rules; keeps HTTP concerns out of business rules.

**Consequences:** Controllers use DTOs only; state machine lives in service/domain; repositories do not own transition rules.

## ADR-003 — PostgreSQL + Flyway only

**Decision:** PostgreSQL is the system of record. Schema changes go through Flyway. Hibernate must not auto-create/update schema for the application.

**Why:** Version-controlled schema; avoids silent drift.

**Consequences:** Integration tests must run Flyway against PostgreSQL (Testcontainers).

## ADR-004 — Status changes only via transitions API

**Decision:** `PATCH /api/v1/tickets/{id}` must not change status. Status changes use `POST /api/v1/tickets/{id}/transitions` with body `{ "toStatus": "..." }`.

**Why:** Single enforcement point for the lifecycle; prevents PATCH bypass.

**Consequences:** UI edit form shows status read-only; transition controls are separate.

## ADR-005 — Explicit state machine with terminal states

**Decision:** Valid transitions only:

- OPEN → IN_PROGRESS | CANCELLED
- IN_PROGRESS → RESOLVED | CANCELLED
- RESOLVED → CLOSED

CLOSED and CANCELLED are terminal. All other transitions (including self-transitions) → HTTP 409 `INVALID_TRANSITION`.

**Why:** Frozen product rule; testable matrix.

**Consequences:** No reopen in v1; integration tests must cover valid and invalid paths.

## ADR-006 — No authentication in v1

**Decision:** APIs are unauthenticated. Actor is a generic Support User. Assignee and comment author are free-text strings.

**Why:** Explicitly out of scope.

**Consequences:** No user entity; security limited to input validation, secrets via env, and backend rule enforcement.

## ADR-007 — Comments on all statuses

**Decision:** Comments may be added to tickets in any status, including CLOSED and CANCELLED.

**Why:** Allows follow-up notes without reopening.

**Consequences:** UI keeps comment form on terminal tickets; no status gate on comment API.

## ADR-008 — Comments embedded on ticket detail GET

**Decision:** No standalone list-comments endpoint. `GET /api/v1/tickets/{id}` returns comments ordered by `createdAt` ascending. List/create/patch/transition responses omit `comments`.

**Why:** Frozen API list has no comment collection GET; UI needs comments on detail.

**Consequences:** After PATCH/transition, UI should refetch detail if it needs comments.

## ADR-009 — List search, filter, pagination

**Decision:** `GET /api/v1/tickets` supports `search` (case-insensitive substring on title OR description), `status` (exact enum), `page` (default 0), `size` (default 20, max 100). Combined filters use AND. Sort: `createdAt` DESC, then `id` DESC.

**Why:** Frozen API requirements plus testable defaults.

## ADR-010 — Consistent API errors

**Decision:** Error body includes `timestamp`, `status`, `error`, `message`, `path`, and optional `fieldErrors`. Codes include `VALIDATION_ERROR`, `BAD_REQUEST`, `NOT_FOUND`, `INVALID_TRANSITION`, `INTERNAL_ERROR`.

**Why:** Matches API standards in repository rules.

## ADR-011 — Testing with Testcontainers PostgreSQL

**Decision:** Unit tests for domain/business logic; integration tests for REST, persistence, validation, and state machine against PostgreSQL via Testcontainers. Automated frontend tests not required for v1 (manual UI checklist).

**Why:** Frozen testing decisions; avoids H2 dialect drift.

## Related specs

- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/test-strategy.md`
