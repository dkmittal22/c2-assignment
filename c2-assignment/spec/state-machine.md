# State Machine Specification

## 1. Purpose

Define all legal and illegal ticket status transitions. Enforcement is mandatory in the backend service/domain layer.

## 2. Statuses

| Status | Terminal | Description |
|--------|----------|-------------|
| `OPEN` | No | Newly created ticket |
| `IN_PROGRESS` | No | Work has started |
| `RESOLVED` | No | Work finished; awaiting close |
| `CLOSED` | Yes | Completed and closed |
| `CANCELLED` | Yes | Abandoned / cancelled |

## 3. Initial State

Every newly created ticket starts in `OPEN`.

Clients cannot set the initial status.

If `status` is present in a create request, the request is rejected with HTTP 400 `BAD_REQUEST` and no ticket is created.

## 4. Valid Transitions

| From | To | Allowed |
|------|----|---------|
| `OPEN` | `IN_PROGRESS` | Yes |
| `OPEN` | `CANCELLED` | Yes |
| `IN_PROGRESS` | `RESOLVED` | Yes |
| `IN_PROGRESS` | `CANCELLED` | Yes |
| `RESOLVED` | `CLOSED` | Yes |

No other transitions are allowed.

The only cancellation transitions are:

- `OPEN` → `CANCELLED`
- `IN_PROGRESS` → `CANCELLED`

`RESOLVED` → `CANCELLED` is not allowed.

```
                 CANCELLED
                    ▲
                    │
 OPEN ──────────────┤
  │                 │
  └──► IN_PROGRESS ─┘
            │
            ▼
        RESOLVED
            │
            ▼
         CLOSED
```

## 5. Terminal States

| Status | Rule |
|--------|------|
| `CLOSED` | No outbound transitions |
| `CANCELLED` | No outbound transitions |

Reopening is out of scope for v1.

## 6. Full Transition Matrix

Rows = current status. Columns = requested `toStatus`.

| Current \ Target | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|------------------|------|-------------|----------|--------|-----------|
| OPEN | Reject | Allow | Reject | Reject | Allow |
| IN_PROGRESS | Reject | Reject | Allow | Reject | Allow |
| RESOLVED | Reject | Reject | Reject | Allow | Reject |
| CLOSED | Reject | Reject | Reject | Reject | Reject |
| CANCELLED | Reject | Reject | Reject | Reject | Reject |

**Reject** (via transitions API) means: HTTP `409 Conflict`, `error` = `INVALID_TRANSITION`, ticket status unchanged.

Self-transitions (same status) are **Reject**.

## 7. Enforcement Rules

1. Status changes are accepted only through `POST /api/v1/tickets/{id}/transitions`.
2. `PATCH /api/v1/tickets/{id}` must not change status. If `status` is present in a PATCH body: HTTP 400, `error` = `BAD_REQUEST`, ticket remains unchanged.
3. Controllers must not implement transition rules; the service/domain layer must.
4. Frontend may hide invalid targets but must not be trusted.
5. Invalid transitions must never be persisted.
6. Optimistic locking (`@Version`) and concurrent transition handling are out of scope for v1.

## 8. Transition API Behavior

Request:

```json
{ "toStatus": "IN_PROGRESS" }
```

On success:

- Persist new status
- Update `updatedAt`
- Return HTTP 200 with ticket representation (without `comments`)

On invalid transition:

- HTTP 409
- Body uses standard error structure with `INVALID_TRANSITION`
- `message` identifies current status and requested status

## 9. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| SM-1 | `OPEN → IN_PROGRESS` succeeds |
| SM-2 | `OPEN → CANCELLED` succeeds |
| SM-3 | `IN_PROGRESS → RESOLVED` succeeds |
| SM-4 | `IN_PROGRESS → CANCELLED` succeeds |
| SM-5 | `RESOLVED → CLOSED` succeeds |
| SM-6 | `OPEN → RESOLVED` returns 409 |
| SM-7 | `OPEN → CLOSED` returns 409 |
| SM-8 | `IN_PROGRESS → CLOSED` returns 409 |
| SM-9 | `IN_PROGRESS → OPEN` returns 409 |
| SM-10 | `RESOLVED → OPEN` returns 409 |
| SM-11 | `RESOLVED → CANCELLED` returns 409 |
| SM-12 | `CLOSED → OPEN` returns 409 |
| SM-13 | `CANCELLED → OPEN` returns 409 |
| SM-14 | `CLOSED → *` (any) returns 409 |
| SM-15 | `CANCELLED → *` (any) returns 409 |
| SM-16 | If `status` is present in a PATCH body: HTTP 400, `error` = `BAD_REQUEST`, ticket remains unchanged |
| SM-17 | If `status` is present in a create request, the request is rejected with HTTP 400 `BAD_REQUEST` and no ticket is created |
