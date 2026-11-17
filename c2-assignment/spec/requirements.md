# Requirements Specification

## 1. Problem

Support teams need a simple system to create, track, update, comment on, and close support tickets through a defined lifecycle.

## 2. Product Scope

Build a full-stack Support Ticket Management System.

### In scope

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot, Spring Data JPA, REST API, PostgreSQL, Flyway, Maven |
| Frontend | React, Vite |
| Testing | JUnit 5, Mockito (where appropriate), Spring Boot Test, Testcontainers with PostgreSQL |

### Functional capabilities (v1)

1. Create a support ticket
2. List tickets with search, status filter, and pagination
3. View a single ticket and its comments
4. Update mutable ticket fields (not status)
5. Add a comment to a ticket
6. Transition ticket status according to the state machine
7. Use a web UI to perform all of the above

### Out of scope (v1)

- Authentication
- Authorization
- User registration / login
- Roles
- Notifications
- Email
- Attachments
- Audit history / transition history persistence
- Realtime updates
- Multi-tenancy
- Microservices
- Ticket deletion (hard or soft)
- Reopening terminal tickets
- Assignee as a managed user entity
- Optimistic locking and concurrent transition handling (`@Version` must not be introduced)
- Full-text search engines (including Elasticsearch)

## 3. Actor

| Actor | Description |
|-------|-------------|
| Support User | A single generic user of the system. No identity, login, or role model exists in v1. |

All API endpoints are publicly callable in v1. The backend still enforces validation and business rules on every request.

## 4. Domain Summary

### Ticket

A ticket represents a support request.

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Application-generated |
| title | String | Required |
| description | String | Required |
| priority | Enum | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; default `MEDIUM` |
| status | Enum | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`; initial `OPEN` |
| assignee | String | Optional free-text label |
| createdAt | Instant | System-set on create |
| updatedAt | Instant | System-set on create and on every successful mutation |

### Comment

A comment is a message attached to a ticket.

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | Application-generated |
| ticketId | UUID | Parent ticket |
| author | String | Required free-text label (no user account) |
| content | String | Required |
| createdAt | Instant | System-set on create |

## 5. Functional Requirements

### FR-1 Create ticket

- The system shall create a ticket with status `OPEN`.
- If priority is omitted, the system shall set priority to `MEDIUM`.
- The system shall reject invalid create payloads with HTTP 400.
- If `status` is present in a create request, the request is rejected with HTTP 400 `BAD_REQUEST` and no ticket is created.
- Unknown JSON fields on create are rejected with HTTP 400 `BAD_REQUEST` and no ticket is created.

**Acceptance criteria**

- Given a valid create request without priority, when created, then status is `OPEN` and priority is `MEDIUM`.
- Given a blank title or blank description, when created, then HTTP 400 with `VALIDATION_ERROR` and field errors.
- Given title longer than 200 characters or description longer than 5000 characters, when created, then HTTP 400 `VALIDATION_ERROR`.
- Given assignee longer than 100 characters, when created, then HTTP 400 `VALIDATION_ERROR`.
- Given a create body containing `status`, when submitted, then HTTP 400 `BAD_REQUEST` and no ticket exists.
- Given a create body containing an unknown field, when submitted, then HTTP 400 `BAD_REQUEST` and no ticket exists.
- Given a successful create, when completed, then response omits `comments` and includes `Location: /api/v1/tickets/{id}`.

### FR-2 List tickets

- The system shall return a paginated list of tickets.
- The system shall support optional `status` filter (exact enum match).
- The system shall support optional `search` as case-insensitive substring matching against title and description (no full-text search infrastructure).
- Default page size is 20; maximum page size is 100.
- Default page index is 0.
- Sort order is `createdAt` descending, then `id` descending.

**Acceptance criteria**

- Given tickets exist, when listed without params, then page size defaults to 20 and page defaults to 0.
- Given `size=101`, when listed, then HTTP 400 `BAD_REQUEST`.
- Given `size=0`, when listed, then HTTP 400 `BAD_REQUEST`.
- Given negative `page`, when listed, then HTTP 400 `BAD_REQUEST`.
- Given `search=Database` and a ticket titled `database issue`, when listed, then that ticket is included.
- Given `search` matching only description text, when listed, then that ticket is included.
- Given `status=OPEN`, when listed, then only `OPEN` tickets are returned.
- Given invalid `status` query value, when listed, then HTTP 400 `VALIDATION_ERROR`.
- Given both `search` and `status`, when listed, then results satisfy both (logical AND).
- Given multiple tickets, when listed, then order is newest `createdAt` first.

### FR-3 Get ticket

- The system shall return a ticket by id, including its comments ordered by `createdAt` ascending, then `id` ascending.
- Malformed UUID path parameter → HTTP 400 `BAD_REQUEST`.
- Valid UUID with no matching ticket → HTTP 404 `NOT_FOUND`.

**Acceptance criteria**

- Given an existing ticket with comments, when fetched, then comments are present in chronological order.
- Given a malformed UUID path, when fetched, then HTTP 400 `BAD_REQUEST`.
- Given a valid UUID with no ticket, when fetched, then HTTP 404 `NOT_FOUND`.

### FR-4 Update ticket (PATCH)

- The system shall allow partial update of: `title`, `description`, `priority`, `assignee`.
- If `status` is present in a PATCH body: HTTP 400 `BAD_REQUEST`; ticket remains unchanged.
- Unknown JSON fields on PATCH: HTTP 400 `BAD_REQUEST`; ticket remains unchanged.
- Empty PATCH body: HTTP 400 `BAD_REQUEST`; ticket remains unchanged.
- The system shall not allow changing `id`, `createdAt`, or client-supplied `updatedAt`.
- Malformed UUID → HTTP 400 `BAD_REQUEST`.
- Valid UUID with no ticket → HTTP 404 `NOT_FOUND`.

**Acceptance criteria**

- Given a PATCH body containing `status`, when applied, then HTTP 400 `BAD_REQUEST` and ticket unchanged.
- Given a PATCH body containing an unknown field, when applied, then HTTP 400 `BAD_REQUEST` and ticket unchanged.
- Given an empty PATCH body, when applied, then HTTP 400 `BAD_REQUEST` and ticket unchanged.
- Given a valid PATCH of title, when applied, then title updates, `updatedAt` changes, and response omits `comments`.
- Given assignee longer than 100 characters on PATCH, when applied, then HTTP 400 `VALIDATION_ERROR`.
- Given a non-existent ticket id (valid UUID), when patched, then HTTP 404 `NOT_FOUND`.

### FR-5 Add comment

- The system shall add a comment to an existing ticket.
- Comments are allowed on tickets in any status, including terminal states.
- Author max 100 characters; content max 2000 characters; both required non-blank after trim.
- Unknown JSON fields → HTTP 400 `BAD_REQUEST`; no comment created.
- Malformed UUID → HTTP 400 `BAD_REQUEST`.
- Valid UUID with no ticket → HTTP 404 `NOT_FOUND`.

**Acceptance criteria**

- Given a valid comment on an `OPEN` ticket, when posted, then HTTP 201 and comment appears on subsequent GET.
- Given blank content, when posted, then HTTP 400 `VALIDATION_ERROR`.
- Given content longer than 2000 characters, when posted, then HTTP 400 `VALIDATION_ERROR`.
- Given author longer than 100 characters, when posted, then HTTP 400 `VALIDATION_ERROR`.
- Given a `CLOSED` ticket, when a valid comment is posted, then HTTP 201.
- Given a `CANCELLED` ticket, when a valid comment is posted, then HTTP 201.
- Given an unknown field in the comment body, when posted, then HTTP 400 `BAD_REQUEST`.

### FR-6 Transition status

- The system shall change ticket status only via `POST /api/v1/tickets/{id}/transitions`.
- The system shall allow only the valid transitions defined in `spec/state-machine.md`.
- Invalid transitions shall return HTTP 409 `INVALID_TRANSITION`.
- Malformed UUID → HTTP 400 `BAD_REQUEST`.
- Valid UUID with no ticket → HTTP 404 `NOT_FOUND`.

**Acceptance criteria**

- Given status `OPEN`, when transitioning to `IN_PROGRESS`, then status becomes `IN_PROGRESS` and HTTP 200.
- Given status `OPEN`, when transitioning to `RESOLVED`, then HTTP 409 and status remains `OPEN`.
- Given status `RESOLVED`, when transitioning to `CANCELLED`, then HTTP 409.
- Given status `CLOSED`, when any transition is requested, then HTTP 409.
- Given status `CANCELLED`, when any transition is requested, then HTTP 409.
- Given a self-transition (same status), when requested, then HTTP 409.

### FR-7 Web UI

- The UI shall allow the Support User to create, list, filter/search, view, update, comment on, and transition tickets.
- Routes: `/tickets`, `/tickets/new`, `/tickets/{id}`; `/` redirects to `/tickets`.
- Search is submitted explicitly by Enter or button (no debounce requirement).
- PATCH success updates local ticket state from the response body.
- Transition success updates local ticket state from the response body.
- Comment success appends the returned `CommentResponse` to local comments.
- The UI shall display API validation and business errors to the user.
- The UI shall only offer valid transition targets for the current status.
- Backend enforcement remains authoritative; UI restrictions are convenience only.

**Acceptance criteria**

- Given navigation to `/`, when loaded, then the user is redirected to `/tickets`.
- Given the list page, when the user submits search via Enter or button and filters by status, then results match the API behavior.
- Given a ticket detail page in `OPEN`, when the user opens transitions, then only `IN_PROGRESS` and `CANCELLED` are offered.
- Given a successful PATCH, when completed, then the UI shows fields from the PATCH response without requiring a full refetch for those fields.
- Given a successful comment POST, when completed, then the new comment appears from the response without requiring a full refetch.
- Given an invalid API response (400/409/404), when shown in the UI, then an error message is visible.

## 6. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-1 | Backend must validate all externally supplied input. |
| NFR-2 | Business rules must be enforced in the service/domain layer, never only in the UI. |
| NFR-3 | Flyway is solely responsible for schema creation/evolution; `spring.jpa.hibernate.ddl-auto=validate`. |
| NFR-4 | Secrets (DB credentials) must come from environment variables / external config, not hardcoded source. |
| NFR-5 | API errors must use the consistent error structure defined in `spec/api-contract.md`. |
| NFR-6 | Every business rule must have automated tests as defined in `spec/test-strategy.md`. |
| NFR-7 | API must allow CORS origin `http://localhost:5173` for `/api/v1/**`. |
| NFR-8 | Physical tables are named `tickets` and `comments`. |
| NFR-9 | UUIDs are generated by the application; PostgreSQL does not generate IDs. |

## 7. Dependencies Between Specs

| Spec | Purpose |
|------|---------|
| `architecture.md` | Component boundaries and tech decisions |
| `data-model.md` | Persistence model and constraints |
| `api-contract.md` | HTTP API request/response contracts |
| `state-machine.md` | Status transition rules |
| `ui-flow.md` | Frontend screens and flows |
| `test-strategy.md` | Required automated tests |
