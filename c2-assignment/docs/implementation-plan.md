# Implementation Plan

Status: Planning only. No application code is produced by this document.
Sources of truth: `spec/*.md`, `rules/*.md`.

## Frozen technical decisions

| Decision | Value |
|----------|-------|
| Monorepo structure | `backend/`, `frontend/`, `spec/`, `rules/`, `commands/`, `docs/`, `.specstory/` |
| Backend HTTP port | `8080` |
| Frontend port | `5173` |
| Frontend routing | React Router |
| Backend HTTP/API integration testing | MockMvc |
| PostgreSQL persistence integration testing | Testcontainers PostgreSQL |

## Constraints (must not violate)

- No authentication, authorization, Keycloak, or user accounts
- No Kafka, RabbitMQ, Redis, Elasticsearch, microservices, or other non-specified infrastructure
- No optimistic locking (`@Version`)
- No H2 for PostgreSQL integration tests — use Testcontainers PostgreSQL
- Status changes only via transitions API — never via PATCH
- Do not expose JPA entities through REST — DTOs only
- Flyway owns schema; `spring.jpa.hibernate.ddl-auto=validate`
- Follow the state machine exactly
- Backend listens on port **8080**; frontend on port **5173**
- API integration tests use **MockMvc**; persistence/DB integration tests use **Testcontainers PostgreSQL**

### Architectural constraint — frontend is not the source of truth

The frontend must never be the source of truth for business rules.
The frontend may hide invalid transition options for usability, but the backend must independently validate every requested transition.

### AI implementation constraint — no silent scope expansion

During implementation, AI must not introduce technologies, architectural patterns, infrastructure, dependencies, endpoints, entities, or workflows that are not required by the approved specifications. If an implementation detail is genuinely unspecified, AI must identify the decision and request human approval rather than silently expanding scope.

## Definition of Done (every implementation task)

For every implementation task, work is not complete until all of the following are true, in order:

1. **Implementation complete** — responsibilities for the task are done
2. **Tests added/updated** — automated and/or manual tests required by the task exist
3. **Tests pass** — verification method succeeds
4. **Spec compliance checked** — behavior matches the cited `spec/` requirements
5. **Human review before proceeding** — a human accepts the task before dependent work continues

## Independence legend

| Label | Meaning |
|-------|---------|
| Independent | Can start once its listed dependencies are done; no coupling to later phases |
| Sequential | Requires prior tasks in this plan |
| Parallel-ok | Can proceed in parallel with sibling tasks that share the same dependencies |

---

## 1. Project bootstrap

### T-01 — Backend Maven / Spring Boot skeleton

| Field | Content |
|-------|---------|
| **Task ID** | T-01 |
| **Objective** | Create the Spring Boot / Maven project under `backend/` with Java 21, ready for JPA, Flyway, Web, Validation, Testcontainers, serving on port 8080. |
| **Dependencies** | None |
| **Independence** | Independent |
| **Files/components** | `backend/pom.xml`, `backend/src/main/java/.../Application`, `backend/src/main/resources/application*.yml`, root `.gitignore` as needed |
| **Implementation responsibilities** | Bootstrap Spring Boot app in `backend/`; declare dependencies (web, data-jpa, postgresql, flyway, validation, test, testcontainers); set Java 21; `server.port=8080`; no business endpoints; no auth starters; no H2 |
| **Tests** | Compile/package verification; context-load smoke deferred until Testcontainers base exists |
| **Acceptance criteria** | `backend/` builds; main application class exists; port 8080 configured; no auth starters; no H2 runtime dependency |
| **Verification method** | `cd backend && mvn -q -DskipTests package` |
| **Specs satisfied** | architecture §3; requirements in-scope backend stack |

### T-02 — Application configuration baselines

| Field | Content |
|-------|---------|
| **Task ID** | T-02 |
| **Objective** | Configure PostgreSQL datasource via env-friendly properties, Flyway enabled, `ddl-auto=validate`, server port 8080. |
| **Dependencies** | T-01 |
| **Independence** | Sequential |
| **Files/components** | `backend/src/main/resources/application.yml`, optional `application-local.yml`, env example (no secrets committed) |
| **Implementation responsibilities** | Set `spring.jpa.hibernate.ddl-auto=validate`; enable Flyway; externalize DB URL/user/password; `server.port=8080`; do not hardcode secrets |
| **Tests** | Config review; no dedicated automated test required beyond later IT suite |
| **Acceptance criteria** | `ddl-auto=validate`; Flyway on; port 8080; DB credentials not hardcoded |
| **Verification method** | Manual review of config; confirm `ddl-auto=validate`, `server.port=8080`, no committed passwords |
| **Specs satisfied** | architecture §4.4, §8; data-model §2; security rules; NFR-3, NFR-4 |

### T-03 — Testcontainers PostgreSQL + MockMvc test base

| Field | Content |
|-------|---------|
| **Task ID** | T-03 |
| **Objective** | Establish shared integration-test support: Testcontainers PostgreSQL for persistence; MockMvc for HTTP/API tests; Flyway against the container. |
| **Dependencies** | T-01, T-02 |
| **Independence** | Sequential |
| **Files/components** | `backend/pom.xml` test deps; abstract IT base under `backend/src/test/java` |
| **Implementation responsibilities** | Wire Testcontainers PostgreSQL; configure MockMvc-based API IT style; ensure tests do not use H2; Hibernate validate against migrated schema |
| **Tests** | Smoke IT that Spring context loads with Postgres container (completes fully after T-05 migration exists) |
| **Acceptance criteria** | Persistence ITs use Testcontainers PostgreSQL; API ITs use MockMvc; no H2 for those tests |
| **Verification method** | Dependency review now; after T-05: `cd backend && mvn -q -Dtest=*IT test` (smoke) |
| **Specs satisfied** | test-strategy §2–3, §5; architecture §3; frozen testing decisions |

### T-04 — Frontend Vite + React + React Router skeleton

| Field | Content |
|-------|---------|
| **Task ID** | T-04 |
| **Objective** | Create React + Vite SPA under `frontend/` with React Router, serving on port 5173. |
| **Dependencies** | None (parallel with T-01) |
| **Independence** | Independent / Parallel-ok with backend bootstrap |
| **Files/components** | `frontend/package.json`, Vite config, `frontend/src/main`, React Router stub |
| **Implementation responsibilities** | Scaffold app in `frontend/`; install/configure React Router; placeholder routes; port 5173; no business UI yet |
| **Tests** | Manual: dev server starts on 5173 |
| **Acceptance criteria** | Dev server at `http://localhost:5173`; React Router present; project builds |
| **Verification method** | `cd frontend && npm install && npm run build` |
| **Specs satisfied** | architecture §3, §7; ui-flow §2 (shell only); frozen FE decisions |

---

## 2. Database and domain model

### T-05 — Flyway migration: `tickets` and `comments`

| Field | Content |
|-------|---------|
| **Task ID** | T-05 |
| **Objective** | Add Flyway SQL creating fixed tables `tickets` and `comments` with columns, PK, FK `ON DELETE RESTRICT`, and required indexes. |
| **Dependencies** | T-02, T-03 |
| **Independence** | Sequential |
| **Files/components** | `backend/src/main/resources/db/migration/V1__*.sql` |
| **Implementation responsibilities** | Create schema exactly per data-model physical tables; no DB UUID defaults; no version column |
| **Tests** | MockMvc not required; Testcontainers IT: Flyway migrates; tables `tickets` and `comments` exist |
| **Acceptance criteria** | Tables/indexes/FK match `spec/data-model.md`; no `@Version` column |
| **Verification method** | `cd backend && mvn -q -Dtest=FlywayMigrationIT test` |
| **Specs satisfied** | data-model §2, §7, §8; NFR-8, NFR-9 |

### T-06 — Domain enums and Ticket JPA entity

| Field | Content |
|-------|---------|
| **Task ID** | T-06 |
| **Objective** | Define `Priority`, `TicketStatus` enums and the Ticket JPA entity mapped to table `tickets`. |
| **Dependencies** | T-05 |
| **Independence** | Sequential |
| **Files/components** | Enums; `Ticket` entity under `backend/src/main/java/...` |
| **Implementation responsibilities** | String enum storage; application-assigned UUID ids; map to `tickets`; no `@Version`; do not expose entity via REST; Comment entity is owned by T-18 |
| **Tests** | Testcontainers persistence IT: ticket field round-trip; enums as strings; Hibernate validate |
| **Acceptance criteria** | Hibernate validate succeeds; Ticket maps to `tickets` |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketPersistenceIT test` |
| **Specs satisfied** | data-model §3–4; architecture §4–5; java-springboot persistence rules |

### T-07 — Ticket repository

| Field | Content |
|-------|---------|
| **Task ID** | T-07 |
| **Objective** | Add Spring Data JPA `TicketRepository` with list filter/search/pagination/sort support. |
| **Dependencies** | T-06 |
| **Independence** | Sequential |
| **Files/components** | `TicketRepository` |
| **Implementation responsibilities** | Status filter; case-insensitive `ILIKE` on title/description; paging; sort `createdAt DESC, id DESC`. Comment repository is owned by T-18 |
| **Tests** | Testcontainers repository IT for search/filter/sort |
| **Acceptance criteria** | Queries match api-contract list semantics at persistence level |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketRepositoryIT test` |
| **Specs satisfied** | api-contract §5.2; data-model §7; architecture §4.3 |

---

## 3. API foundation

### T-08 — Error response model and centralized exception handling

| Field | Content |
|-------|---------|
| **Task ID** | T-08 |
| **Objective** | Implement consistent API error body and `@ControllerAdvice` mapping for validation, bad request, not found, invalid transition, internal error. |
| **Dependencies** | T-01 |
| **Independence** | Parallel-ok with T-05–T-07 once T-01 done; needed before controllers |
| **Files/components** | Error DTO, exception types, `GlobalExceptionHandler` |
| **Implementation responsibilities** | Codes: `VALIDATION_ERROR`, `BAD_REQUEST`, `NOT_FOUND`, `INVALID_TRANSITION`, `INTERNAL_ERROR`; `fieldErrors` only for validation |
| **Tests** | Unit/MockMvc slice tests proving mapping to status + body shape |
| **Acceptance criteria** | Error JSON matches api-contract §3 |
| **Verification method** | `cd backend && mvn -q -Dtest=GlobalExceptionHandler* test` |
| **Specs satisfied** | api-contract §2–3; architecture §6; api-standards; NFR-5 |

### T-09 — Jackson unknown-property rejection and malformed UUID handling

| Field | Content |
|-------|---------|
| **Task ID** | T-09 |
| **Objective** | Fail on unknown JSON properties (`BAD_REQUEST`) and map malformed UUID path params to HTTP 400 `BAD_REQUEST`. |
| **Dependencies** | T-08 |
| **Independence** | Sequential after T-08 |
| **Files/components** | Jackson config / DTO annotations; exception handler for type mismatch / method argument errors |
| **Implementation responsibilities** | Unknown fields → 400 `BAD_REQUEST`; malformed UUID → 400 `BAD_REQUEST`; valid missing UUID → 404 from services |
| **Tests** | MockMvc tests (stub or real ticket endpoints once available) |
| **Acceptance criteria** | Behavior matches api-contract path/UUID and unknown-field rules |
| **Verification method** | `cd backend && mvn -q -Dtest=*BadRequest* test` (and later ticket ITs) |
| **Specs satisfied** | api-contract §2 path table, unknown fields; FR-3/FR-4 UUID rules |

### T-10 — CORS for local frontend

| Field | Content |
|-------|---------|
| **Task ID** | T-10 |
| **Objective** | Allow origin `http://localhost:5173` for `/api/v1/**`. |
| **Dependencies** | T-01 |
| **Independence** | Parallel-ok with T-08/T-09 |
| **Files/components** | CORS / WebMvc config in `backend/` |
| **Implementation responsibilities** | Configure allowed origin exactly as specified for API paths |
| **Tests** | MockMvc CORS IT asserting allowance for `http://localhost:5173` |
| **Acceptance criteria** | Requests/preflight from that origin to `/api/v1/**` succeed CORS checks |
| **Verification method** | `cd backend && mvn -q -Dtest=CorsIT test` |
| **Specs satisfied** | api-contract §7; architecture §8; NFR-7; test-strategy §5.6 |

---

## 4. Ticket APIs

### T-11 — Ticket DTOs and mapping (no entity exposure)

| Field | Content |
|-------|---------|
| **Task ID** | T-11 |
| **Objective** | Define request/response DTOs for create, patch, summary, detail (comments field present on detail DTO) and mappers from Ticket entity. |
| **Dependencies** | T-06 |
| **Independence** | Parallel-ok with T-07/T-08 |
| **Files/components** | Ticket DTO records, ticket mappers |
| **Implementation responsibilities** | Trim-then-validate constraints on ticket DTOs; create/patch/transition responses omit `comments`; detail DTO includes `comments` (population completed in T-18); never expose entities |
| **Tests** | Unit tests for mapping defaults (priority MEDIUM, status OPEN) |
| **Acceptance criteria** | Controllers can return DTOs only; entities never appear in API signatures |
| **Verification method** | `cd backend && mvn -q -Dtest=*TicketMapper* test` |
| **Specs satisfied** | api-contract §4; architecture §5; java-springboot DTO rule |

### T-12 — Create ticket API (`POST /api/v1/tickets`)

| Field | Content |
|-------|---------|
| **Task ID** | T-12 |
| **Objective** | Implement create ticket: service + controller; app UUID; status OPEN; default priority MEDIUM; Location header; reject status/unknown/illegal fields. |
| **Dependencies** | T-07, T-08, T-09, T-11 |
| **Independence** | Sequential |
| **Files/components** | `TicketController`, `TicketService`, create DTO validation |
| **Implementation responsibilities** | Business create rules in service; controller HTTP only; 201 + body without comments; SM-17 reject `status` on create with 400 `BAD_REQUEST` |
| **Tests** | MockMvc + Testcontainers IT: valid create; blank fields; lengths; invalid priority; status present; unknown field; Location header |
| **Acceptance criteria** | FR-1 acceptance criteria pass |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketCreateIT test` |
| **Specs satisfied** | FR-1; api-contract §5.1; SM-17; data-model create rules |

### T-13 — Get ticket by id (`GET /api/v1/tickets/{id}`) — ticket shell

| Field | Content |
|-------|---------|
| **Task ID** | T-13 |
| **Objective** | Return ticket by id with a `comments` array field; 400 malformed UUID; 404 missing. Full comment population/ordering is completed in T-18. |
| **Dependencies** | T-12 |
| **Independence** | Sequential after T-12; Parallel-ok with T-14 |
| **Files/components** | `TicketController`, `TicketService` |
| **Implementation responsibilities** | Load ticket; return detail DTO including `comments` (empty until T-18 wires comment loading) |
| **Tests** | MockMvc IT: get existing (comments may be empty); malformed UUID → 400 `BAD_REQUEST`; missing → 404 `NOT_FOUND` |
| **Acceptance criteria** | Ticket fields returned correctly; UUID error split correct; comments ordering/content verified under T-18 |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketGetIT test` |
| **Specs satisfied** | FR-3 (ticket + UUID); api-contract §5.3 (partial until T-18) |

### T-14 — List tickets (`GET /api/v1/tickets`)

| Field | Content |
|-------|---------|
| **Task ID** | T-14 |
| **Objective** | Paginated list with search, status filter, defaults page=0 size=20, max size 100, sort order, page DTO. |
| **Dependencies** | T-12 |
| **Independence** | Sequential after T-12; Parallel-ok with T-13 |
| **Files/components** | `TicketController`, `TicketService`, `TicketRepository` queries |
| **Implementation responsibilities** | AND semantics for search+status; empty search = no filter; validation for page/size/status; omit `comments` on list items |
| **Tests** | MockMvc + Testcontainers: all list cases in test-strategy §5.1 |
| **Acceptance criteria** | FR-2 acceptance criteria pass |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketListIT test` |
| **Specs satisfied** | FR-2; api-contract §5.2 |

### T-15 — Patch ticket (`PATCH /api/v1/tickets/{id}`) — no status changes

| Field | Content |
|-------|---------|
| **Task ID** | T-15 |
| **Objective** | Partial update of title, description, priority, assignee only; reject status/unknown/empty/illegal fields with `BAD_REQUEST`; update `updatedAt`. |
| **Dependencies** | T-12, T-13 |
| **Independence** | Sequential |
| **Files/components** | `TicketController`, `TicketService`, patch DTO |
| **Implementation responsibilities** | Never change status in PATCH path; response omits comments |
| **Tests** | MockMvc + Testcontainers: patch success; status present → 400 `BAD_REQUEST` and DB unchanged; unknown field; empty body; missing; malformed UUID; assignee length |
| **Acceptance criteria** | FR-4 and SM-16 pass |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketPatchIT test` |
| **Specs satisfied** | FR-4; api-contract §5.4; state-machine enforcement rule 2 |

---

## 5. State machine

### T-16 — Transition domain rules (pure unit-testable)

| Field | Content |
|-------|---------|
| **Task ID** | T-16 |
| **Objective** | Implement pure domain component encoding the full transition matrix. |
| **Dependencies** | T-06 |
| **Independence** | Parallel-ok with ticket APIs after T-06; required before T-17 |
| **Files/components** | Domain transition policy (e.g. `TicketStatusTransitionPolicy`) |
| **Implementation responsibilities** | Encode exact matrix from `spec/state-machine.md` with no HTTP/persistence: all valid transitions; all invalid transitions; terminal states (`CLOSED`, `CANCELLED`) have no outbound transitions; self-transitions rejected |
| **Tests** | Unit tests must explicitly cover: **all valid transitions**; **all invalid transitions** in the matrix; **terminal states** (no outbound); **self-transitions** for every status |
| **Acceptance criteria** | Unit suite proves allow/reject for the complete matrix including terminals and self-transitions |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketStatusTransitionTest test` |
| **Specs satisfied** | state-machine §4–6; test-strategy §4.1; java-springboot state machine rules |

### T-17 — Transition API (`POST /api/v1/tickets/{id}/transitions`)

| Field | Content |
|-------|---------|
| **Task ID** | T-17 |
| **Objective** | Expose transitions via API; persist only allowed changes; reject invalid transitions with 409 without modifying persisted state. |
| **Dependencies** | T-12, T-16, T-08, T-09 |
| **Independence** | Sequential |
| **Files/components** | `TicketController`, `TicketService`, transition request DTO |
| **Implementation responsibilities** | Service (not controller) enforces T-16; backend independently validates every requested transition (frontend must never be trusted); reject unknown fields; update `updatedAt` on success; omit `comments` in response; no `@Version` |
| **Tests** | MockMvc + Testcontainers IT must explicitly cover: **all valid transitions** (200 + persisted status); **all invalid transitions** including **terminal states** and **self-transitions** → **HTTP 409** `INVALID_TRANSITION`; assert **persisted status and fields unchanged** on every 409; missing ticket 404; malformed UUID 400; invalid enum 400; unknown field 400 |
| **Acceptance criteria** | FR-6; SM-1…SM-15; invalid transition returns 409 and does not modify persisted state; backend validation independent of any UI |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketTransitionIT test` |
| **Specs satisfied** | FR-6; api-contract §5.6; state-machine §7–9; test-strategy §5.3; architectural frontend-not-SOT constraint |

---

## 6. Comments

### T-18 — Comments feature (entity through API and GET detail)

| Field | Content |
|-------|---------|
| **Task ID** | T-18 |
| **Objective** | Deliver the full comments capability end-to-end for v1. |
| **Dependencies** | T-05, T-06, T-08, T-09, T-11, T-13 |
| **Independence** | Sequential after ticket get shell; Parallel-ok with T-17 |
| **Files/components** | `Comment` entity; `CommentRepository`; comment request/response DTOs; comment mapper; `TicketController` comment mapping and/or `CommentService`; updates to get-ticket mapping |
| **Implementation responsibilities** | Explicitly implement all of the following: (1) **Comment entity** mapped to `comments` with application-generated UUID; (2) **Comment repository**; (3) **`POST /api/v1/tickets/{id}/comments`**; (4) **comment validation** (author/content required non-blank after trim; max lengths; unknown fields → 400 `BAD_REQUEST`); (5) **CommentResponse** on 201; (6) **GET ticket detail including comments**; (7) comments ordered **`createdAt ASC`, `id ASC`**; (8) **parent ticket `updatedAt` update** on successful comment; (9) **comments allowed on `CLOSED` and `CANCELLED`** tickets |
| **Tests** | MockMvc + Testcontainers: create comment; validation failures; unknown fields; missing/malformed ticket id; comment on CLOSED; comment on CANCELLED; parent `updatedAt` changes; GET detail returns comments in `createdAt ASC, id ASC` order |
| **Acceptance criteria** | FR-5 fully pass; GET detail includes ordered comments; terminal-ticket comments succeed; parent `updatedAt` refreshed |
| **Verification method** | `cd backend && mvn -q -Dtest=TicketCommentIT,TicketGetIT test` |
| **Specs satisfied** | FR-5; api-contract §5.3, §5.5; data-model §5–6; test-strategy §5.2 |

---

## 7. Backend hardening

### T-19 — Validation edge hardening and trim rules

| Field | Content |
|-------|---------|
| **Task ID** | T-19 |
| **Objective** | Ensure trim-then-validate for text fields; whitespace-only rejected; assignee whitespace clears to null on create/patch. |
| **Dependencies** | T-12, T-15, T-18 |
| **Independence** | Sequential hardening |
| **Files/components** | DTO normalization, service trimming helpers |
| **Implementation responsibilities** | Align with data-model blankness rules end-to-end for tickets and comments |
| **Tests** | Unit/MockMvc IT: whitespace title/description/author/content; assignee blank → null |
| **Acceptance criteria** | data-model §6 blankness behavior verified |
| **Verification method** | `cd backend && mvn -q -Dtest=*Validation*IT,*Trim*Test test` |
| **Specs satisfied** | data-model §6; test-strategy §4.2 |

### T-20 — Error-contract and persistence regression suite pass

| Field | Content |
|-------|---------|
| **Task ID** | T-20 |
| **Objective** | Ensure error body shape tests and persistence tests (enum strings, FK, validate mode, CORS) are green as a suite. |
| **Dependencies** | T-10, T-12–T-18 |
| **Independence** | Sequential checkpoint |
| **Files/components** | Test suite organization (no new product features) |
| **Implementation responsibilities** | Fill any gaps vs test-strategy §5.4–5.6; confirm MockMvc + Testcontainers usage |
| **Tests** | Full backend IT suite |
| **Acceptance criteria** | Backend automated Definition of Done items in test-strategy met |
| **Verification method** | `cd backend && mvn -q test` |
| **Specs satisfied** | test-strategy §5.4–5.6, §9; NFR-6 |

---

## 8. Frontend

### T-21 — API client and shared error display

| Field | Content |
|-------|---------|
| **Task ID** | T-21 |
| **Objective** | Frontend HTTP client targeting backend `http://localhost:8080/api/v1`, parsing error `message` / `fieldErrors`. |
| **Dependencies** | T-04; full verification needs backend from T-12+ |
| **Independence** | Parallel-ok after T-04 |
| **Files/components** | `frontend/src` API client module, error display helpers |
| **Implementation responsibilities** | No auth headers; configurable base URL defaulting to port 8080; surface API errors; do not encode business-rule authority in the client |
| **Tests** | Manual |
| **Acceptance criteria** | Client can call list/create against running API on 8080 |
| **Verification method** | Manual browser/network check with backend on 8080 and frontend on 5173 |
| **Specs satisfied** | architecture §7; ui-flow error display; FR-7 |

### T-22 — React Router routes

| Field | Content |
|-------|---------|
| **Task ID** | T-22 |
| **Objective** | Implement React Router routes `/tickets`, `/tickets/new`, `/tickets/{id}` and `/` → `/tickets` redirect; nav links. |
| **Dependencies** | T-04 |
| **Independence** | Parallel-ok with T-21 |
| **Files/components** | React Router config, layout/nav |
| **Implementation responsibilities** | Exact routes from ui-flow using React Router only (no alternate router) |
| **Tests** | Manual UI-9 |
| **Acceptance criteria** | `/` redirects to `/tickets`; nav works on port 5173 |
| **Verification method** | Manual browser check on `http://localhost:5173/` |
| **Specs satisfied** | ui-flow §2; FR-7 routes; UI-9 |

### T-23 — Ticket list page

| Field | Content |
|-------|---------|
| **Task ID** | T-23 |
| **Objective** | List UI with explicit search (Enter/button), status filter, pagination, empty/error states. |
| **Dependencies** | T-14, T-21, T-22 |
| **Independence** | Sequential |
| **Files/components** | List page component under `frontend/` |
| **Implementation responsibilities** | No debounce; wire query params to API; no business-rule authority beyond UX |
| **Tests** | Manual UI-2 |
| **Acceptance criteria** | ui-flow §3.1 and FR-7 list AC |
| **Verification method** | Manual checklist UI-2 |
| **Specs satisfied** | ui-flow §3.1; FR-7; UI-2 |

### T-24 — Create ticket page

| Field | Content |
|-------|---------|
| **Task ID** | T-24 |
| **Objective** | Create form posting to API; navigate to detail on 201; show field errors. |
| **Dependencies** | T-12, T-21, T-22 |
| **Independence** | Parallel-ok with T-23 after shared deps |
| **Files/components** | Create page component |
| **Implementation responsibilities** | Fields per ui-flow; default priority MEDIUM in UI; backend remains authoritative |
| **Tests** | Manual UI-1, Flow E |
| **Acceptance criteria** | UI-1; ui-flow §3.2 |
| **Verification method** | Manual create → lands on `/tickets/{id}` |
| **Specs satisfied** | ui-flow §3.2; FR-7; UI-1 |

### T-25 — Ticket detail view (read-only shell)

| Field | Content |
|-------|---------|
| **Task ID** | T-25 |
| **Objective** | Detail page loads `GET /api/v1/tickets/{id}` and displays ticket fields, timestamps, and comments list (read-only); handle 404/400. |
| **Dependencies** | T-13, T-18 (for comments content), T-21, T-22 |
| **Independence** | Sequential; independently verifiable from edit/transition/comment forms |
| **Files/components** | Detail page view components |
| **Implementation responsibilities** | Render ticket + comments; link back on not-found/bad-request; no edit/transition/comment mutations in this task |
| **Tests** | Manual: open detail, see fields/comments, 404 state (UI-8) |
| **Acceptance criteria** | Detail view matches ui-flow load/display; UI-8 |
| **Verification method** | Manual checklist for view + not-found |
| **Specs satisfied** | ui-flow §3.3 load/show; UI-8 |

### T-26 — Ticket detail edit (PATCH)

| Field | Content |
|-------|---------|
| **Task ID** | T-26 |
| **Objective** | Editable title, description, priority, assignee on detail; PATCH; update local ticket state from PATCH response; status read-only. |
| **Dependencies** | T-15, T-25 |
| **Independence** | Sequential after T-25; independently verifiable |
| **Files/components** | Detail edit form |
| **Implementation responsibilities** | PATCH changed fields only; never send `status`; update local state from response; show field/global errors |
| **Tests** | Manual UI-3, UI-4 |
| **Acceptance criteria** | UI-3, UI-4; local state updates from PATCH response |
| **Verification method** | Manual edit save and confirm UI reflects response |
| **Specs satisfied** | ui-flow §3.3 edit; FR-7; UI-3, UI-4 |

### T-27 — Ticket detail transitions UI

| Field | Content |
|-------|---------|
| **Task ID** | T-27 |
| **Objective** | Transition controls offering only valid targets; POST transitions; update local ticket from response; show 409 and resync via GET. |
| **Dependencies** | T-17, T-25 |
| **Independence** | Sequential after T-25; Parallel-ok with T-26; independently verifiable |
| **Files/components** | Transition control component |
| **Implementation responsibilities** | Offer only valid targets for usability; **must not** be treated as source of truth — backend validates every transition; terminal states show notice and no targets; on success update local ticket from response; on 409 display message and GET resync |
| **Tests** | Manual UI-5, UI-6, UI-10; Flows A–C |
| **Acceptance criteria** | UI-5, UI-6, UI-10; architectural frontend-not-SOT constraint respected |
| **Verification method** | Manual transitions including an invalid attempt if forced (e.g. crafted request) proves backend 409 still governs |
| **Specs satisfied** | ui-flow transitions; FR-7; state-machine UI table; architectural constraint |

### T-28 — Ticket detail comments UI

| Field | Content |
|-------|---------|
| **Task ID** | T-28 |
| **Objective** | Comment form on detail; POST comment; append returned `CommentResponse` locally; available on terminal tickets. |
| **Dependencies** | T-18, T-25 |
| **Independence** | Sequential after T-25; Parallel-ok with T-26/T-27; independently verifiable |
| **Files/components** | Comment list (if not fully in T-25), comment form |
| **Implementation responsibilities** | Author/content form; append CommentResponse on 201; keep form available for CLOSED/CANCELLED; show validation errors |
| **Tests** | Manual UI-7 |
| **Acceptance criteria** | UI-7; comments work on open and terminal tickets |
| **Verification method** | Manual add comment on OPEN and CLOSED/CANCELLED |
| **Specs satisfied** | ui-flow comments; FR-7; UI-7 |

---

## 9. Integration and acceptance testing

### T-29 — Full backend automated suite

| Field | Content |
|-------|---------|
| **Task ID** | T-29 |
| **Objective** | Run complete Maven test suite; fix failures; confirm MockMvc + Testcontainers PostgreSQL; confirm no H2. |
| **Dependencies** | T-20 |
| **Independence** | Sequential gate |
| **Files/components** | Tests / minor fixes only |
| **Implementation responsibilities** | Achieve green backend tests under frozen testing decisions |
| **Tests** | Entire backend suite per test-strategy |
| **Acceptance criteria** | `mvn test` green; Testcontainers Postgres for persistence ITs; MockMvc for API ITs |
| **Verification method** | `cd backend && mvn -q test` |
| **Specs satisfied** | test-strategy §9; NFR-6 |

### T-30 — End-to-end manual acceptance (API + UI)

| Field | Content |
|-------|---------|
| **Task ID** | T-30 |
| **Objective** | Manually execute ui-flow Flows A–E and UI-1…UI-10 against backend `:8080` and frontend `:5173`. |
| **Dependencies** | T-10, T-23, T-24, T-25, T-26, T-27, T-28, T-29 |
| **Independence** | Sequential |
| **Files/components** | None (verification); bugfixes spawn from findings |
| **Implementation responsibilities** | Record pass/fail; no scope creep; confirm frontend is not relied on as business-rule authority |
| **Tests** | Manual acceptance |
| **Acceptance criteria** | All UI ACs and flows pass; CORS works from 5173 to 8080 |
| **Verification method** | Manual scripted checklist; both apps running |
| **Specs satisfied** | FR-7; ui-flow §4–5; test-strategy §6 |

---

## 10. AI review

### T-31 — AI code review against specs and rules

| Field | Content |
|-------|---------|
| **Task ID** | T-31 |
| **Objective** | Structured AI review for bugs, spec violations, security, missing tests, complexity, and silent scope expansion — report only first. |
| **Dependencies** | T-29; frontend T-25–T-28 substantially complete |
| **Independence** | Sequential review gate |
| **Files/components** | `docs/ai-review.md` (findings); no production code in this task |
| **Implementation responsibilities** | Severity-tagged issues; check AI implementation constraint; do not silently fix in the same step |
| **Tests** | None (review artifact) |
| **Acceptance criteria** | Written review covering java/spring, API, security, spec, tests, scope creep |
| **Verification method** | Review document completed |
| **Specs satisfied** | Process Review stage; rules adherence; AI implementation constraint |

---

## 11. Human review and fixes

### T-32 — Human review of AI findings and product acceptance

| Field | Content |
|-------|---------|
| **Task ID** | T-32 |
| **Objective** | Human validates AI review, prioritizes fixes, confirms out-of-scope items and unapproved technologies were not introduced. |
| **Dependencies** | T-31, T-30 |
| **Independence** | Sequential |
| **Files/components** | Review notes |
| **Implementation responsibilities** | Accept/reject findings; enforce human approval for any unspecified decisions |
| **Tests** | Re-run affected tests after decisions |
| **Acceptance criteria** | Signed-off review list for fixes |
| **Verification method** | Human checklist complete |
| **Specs satisfied** | Process Human review; AI implementation constraint |

### T-33 — Fix defects with regression tests

| Field | Content |
|-------|---------|
| **Task ID** | T-33 |
| **Objective** | Fix approved defects; for each bug: reproduce with test, fix, keep regression test. |
| **Dependencies** | T-32 |
| **Independence** | Sequential |
| **Files/components** | Production + test code as needed under `backend/` / `frontend/` |
| **Implementation responsibilities** | Follow test-strategy regression policy; no scope expansion; no new technologies without approval |
| **Tests** | New regression tests + full backend `mvn test`; selective manual UI if FE affected |
| **Acceptance criteria** | Approved issues closed; suite green; UI re-check if needed |
| **Verification method** | `cd backend && mvn -q test` + selective manual UI |
| **Specs satisfied** | test-strategy §7; Fix stage |

---

## 12. Documentation

### T-34 — Update architecture decisions and prompt/history docs

| Field | Content |
|-------|---------|
| **Task ID** | T-34 |
| **Objective** | Update `docs/architecture-decisions.md` and `docs/prompt-history.md` to reflect implemented decisions without unverified claims. |
| **Dependencies** | T-29 minimum; prefer after T-33 |
| **Independence** | Parallel-ok late |
| **Files/components** | `docs/architecture-decisions.md`, `docs/prompt-history.md` |
| **Implementation responsibilities** | Document frozen decisions and outcomes; evidence-based only |
| **Tests** | None |
| **Acceptance criteria** | Docs match shipped design; no false “works” claims without tests |
| **Verification method** | Manual doc review vs documentation skill |
| **Specs satisfied** | documentation skill; process Documentation |

### T-35 — README runbook (local)

| Field | Content |
|-------|---------|
| **Task ID** | T-35 |
| **Objective** | Fill root `README.md` with how to run Postgres, backend (`:8080`), frontend (`:5173`), and tests. |
| **Dependencies** | T-02, T-04, T-29 |
| **Independence** | Parallel-ok late |
| **Files/components** | `README.md` |
| **Implementation responsibilities** | Operator steps; monorepo paths; env vars; CORS origin; no secrets |
| **Tests** | None |
| **Acceptance criteria** | New developer can start system from README |
| **Verification method** | Dry-run instructions manually |
| **Specs satisfied** | Operational clarity for local setup |

### T-36 — Final AI review doc update after fixes

| Field | Content |
|-------|---------|
| **Task ID** | T-36 |
| **Objective** | Update `docs/ai-review.md` with post-fix status of findings (resolved/deferred). |
| **Dependencies** | T-33 |
| **Independence** | Sequential |
| **Files/components** | `docs/ai-review.md` |
| **Implementation responsibilities** | Evidence-based status only |
| **Tests** | None |
| **Acceptance criteria** | Review doc reflects final state |
| **Verification method** | Doc review |
| **Specs satisfied** | documentation skill; Review/Fix closure |

---

## Dependency overview

```
T-01 ──► T-02 ──► T-03 ──► T-05 ──► T-06 ──► T-07 ──┐
  │         │                           │           │
  │         └──────────► T-08 ──► T-09 ─┼───────────┤
  ├────────────────────► T-10          │           │
  │                                    ▼           ▼
  │                                  T-11 ──► T-12 ──► T-13 ──┐
  │                                             │  └► T-14    │
  │                                             ├────► T-15   │
  │                     T-06 ──► T-16 ──────────┴────► T-17   │
  │                                                       T-18 ◄┘
  │                                                         │
  │                              T-19 ◄── T-12/15/18        │
  │                              T-20 ◄── T-10 + APIs ──────┘
  │                                │
  │                                ▼
  │                              T-29 ──► T-30 ──► T-31 ──► T-32 ──► T-33 ──► T-36
  │                                         ▲
T-04 ──► T-21 ──► T-22 ──► T-23/T-24        │
              │                             │
              └► T-25 ──► T-26              │
                   ├────► T-27 ─────────────┘
                   └────► T-28

Docs: T-34, T-35 (late)
```

### Independently startable now

- **T-01** (`backend/` bootstrap)
- **T-04** (`frontend/` bootstrap)

### Frontend detail tasks (independently verifiable after T-25)

| Task | Concern |
|------|---------|
| T-25 | Read-only detail view |
| T-26 | PATCH edit UI |
| T-27 | Transitions UI |
| T-28 | Comments UI |

---

## Plan-vs-specification consistency check

### Coverage

| Spec item | Tasks |
|-----------|-------|
| FR-1…FR-6 | T-12…T-15, T-17, T-18 |
| FR-7 / UI | T-21…T-28, T-30 |
| State machine full matrix + 409 + no persist on reject | T-16, T-17 |
| Comments entity→API→GET order→updatedAt→terminal | T-18 |
| CORS / ports / MockMvc / Testcontainers / monorepo | Frozen decisions + T-01…T-04, T-10, T-03, T-29 |
| Frontend not SOT | Constraint + T-17, T-27 |
| AI no silent scope expansion | Constraint + T-31, T-32, T-33 |

### Remaining genuine human decisions (unspecified by specs)

1. **Exact `fieldErrors` / `message` string wording** — structure fixed; copy not prescribed.
2. **Local Postgres for manual runs** — Testcontainers covers tests; whether developers use Docker Postgres, native install, or another local server for manual API/UI is unspecified (must not add Redis/Elasticsearch/etc.).
3. **Frontend CSS approach** — plain CSS vs existing simple styling; no design system specified (must not add large UI kits without approval).
4. **Package/group name for Java base package** — e.g. `com.example.supporttickets` — unspecified.

### Out-of-scope reminders

Do **not** add: authentication, authorization, Kafka/RabbitMQ/Redis/Elasticsearch, Keycloak, microservices, `@Version`, H2-backed ITs, status via PATCH, entity exposure, Hibernate `update`/`create` DDL, audit history, or any technology not required by approved specifications without human approval.
