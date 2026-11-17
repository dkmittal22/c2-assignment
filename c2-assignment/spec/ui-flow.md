# UI Flow Specification

## 1. Purpose

Define the React + Vite frontend flows for the single Support User actor.

Authentication UI is out of scope.

## 2. Application Shell

- Single-page application served locally at `http://localhost:5173`.
- Primary navigation:
  - **Tickets** → `/tickets`
  - **New Ticket** → `/tickets/new`
- Detail views use `/tickets/{id}`.
- Route `/` redirects to `/tickets`.
- Global/inline area for API error messages.

## 3. Screens

### 3.1 Ticket List

**Route:** `/tickets`

**Shows**

- Search input
- Status filter (`ALL` + each `TicketStatus`)
- Paginated table/list of tickets: title, priority, status, assignee, createdAt
- Page controls respecting API `page` / `size` (default size 20)
- Link/button to create ticket
- Row click / “View” navigates to `/tickets/{id}`

**Search interaction**

- Search is submitted explicitly by pressing Enter or clicking a search button.
- No debounce requirement.

**Behavior**

| Action | Result |
|--------|--------|
| Submit search (Enter or button) | `GET /api/v1/tickets?search=...` with current filters |
| Change status filter | Refetch with `status` when a status is selected; omit `status` when ALL |
| Change page | Refetch with new `page` |
| Open ticket | Navigate to `/tickets/{id}` |

**Empty state:** Message when `content` is empty.

**Error state:** Show API `message` on failure.

### 3.2 Create Ticket

**Route:** `/tickets/new`

**Form fields**

| Field | Input | Notes |
|-------|-------|-------|
| title | text | Required |
| description | textarea | Required |
| priority | select | Optional; default UI value `MEDIUM` |
| assignee | text | Optional |

**Actions**

| Action | Behavior |
|--------|----------|
| Submit | `POST /api/v1/tickets`; on 201 navigate to `/tickets/{id}` using the created id |
| Cancel | Navigate to `/tickets` |
| Validation errors | Display `fieldErrors` next to fields |
| `BAD_REQUEST` errors | Display API `message` |

Client-side required checks may exist; backend errors are authoritative.

### 3.3 Ticket Detail

**Route:** `/tickets/{id}`

**Shows**

- All ticket fields
- `updatedAt` / `createdAt`
- Comments chronologically
- Edit controls for mutable fields
- Transition controls
- Add-comment form

**Load:** `GET /api/v1/tickets/{id}`

**404:** Show not-found message and link back to `/tickets`.

**400 (malformed id):** Show bad-request message and link back to `/tickets`.

#### Edit ticket

Editable: title, description, priority, assignee.

| Action | Behavior |
|--------|----------|
| Save | `PATCH /api/v1/tickets/{id}` with changed fields only |
| Success | Update local ticket state from the PATCH response body |
| Error | Show field/global errors |

Status is displayed read-only on this form (not an editable PATCH field).

#### Transitions

Show only valid next statuses for current status:

| Current | Offered targets |
|---------|-----------------|
| OPEN | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | RESOLVED, CANCELLED |
| RESOLVED | CLOSED |
| CLOSED | none — show terminal notice |
| CANCELLED | none — show terminal notice |

| Action | Behavior |
|--------|----------|
| Confirm transition | `POST /api/v1/tickets/{id}/transitions` with `{ "toStatus": "..." }` |
| Success | Update local ticket state from the transition response body |
| 409 | Display conflict message; refresh ticket via GET to resync |

#### Comments

| UI | Behavior |
|----|----------|
| List | Render `comments` from initial GET detail (plus locally appended comments) |
| Form | author (text), content (textarea) |
| Submit | `POST /api/v1/tickets/{id}/comments` |
| Success | Append the returned `CommentResponse` to local comments |

Comments remain available when status is `CLOSED` or `CANCELLED`.

## 4. End-to-End User Flows

### Flow A — Create and start work

1. User opens `/tickets/new`
2. Submits valid form → ticket created `OPEN`
3. Lands on `/tickets/{id}` → chooses transition `IN_PROGRESS`
4. Local status shows `IN_PROGRESS` from transition response

### Flow B — Resolve and close

1. Ticket is `IN_PROGRESS`
2. User transitions to `RESOLVED`
3. User transitions to `CLOSED`
4. Transition controls hidden; comment form still available

### Flow C — Cancel from open

1. Ticket is `OPEN`
2. User transitions to `CANCELLED`
3. Ticket is terminal

### Flow D — Search and filter

1. User enters search text and clicks search (or presses Enter)
2. User selects a status filter
3. List shows intersection of matches
4. User opens a result detail at `/tickets/{id}`

### Flow E — Validation failure

1. User submits create/patch/comment with blank required field
2. UI shows field error from API `fieldErrors`

## 5. UI Acceptance Criteria

| ID | Criterion |
|----|-----------|
| UI-1 | User can create a ticket and land on `/tickets/{id}` |
| UI-2 | User can list, submit search via Enter/button, filter by status, and paginate on `/tickets` |
| UI-3 | User can patch title/description/priority/assignee from detail; local state updates from PATCH response |
| UI-4 | User cannot change status through the edit form |
| UI-5 | Transition control offers only valid targets |
| UI-6 | Invalid transition API errors are visible |
| UI-7 | User can add comments on open and terminal tickets; local list appends CommentResponse |
| UI-8 | Not-found detail route shows a clear error state |
| UI-9 | `/` redirects to `/tickets` |
| UI-10 | Transition success updates local ticket from response |

## 6. Non-Goals (UI)

- Login / logout
- Role-based menus
- Realtime push updates
- Attachment upload widgets
- Rich-text editor requirements (plain text is sufficient)
- Debounced search
