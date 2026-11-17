# AI Review — Specification Consistency

Date: 2026-09-24
Scope: Review of all files under `spec/` after initial creation
Method: Cross-file consistency check per `commands/review-spec.md`
Note: Specs were **not** modified after this review. This document does **not** claim any feature is implemented or verified.

## Artifacts reviewed

- `spec/requirements.md`
- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`
- `spec/test-strategy.md`

## Verdict

Specifications are aligned with the frozen decisions and are sufficiently detailed to plan implementation. Remaining issues are mostly clarification gaps and missing acceptance-criteria bullets, not blockers that invalidate the state machine or API shape.

## Ambiguities

1. Create/PATCH/transition responses: omit `comments` vs allow empty array (omit preferred).
2. PATCH including `status`: `BAD_REQUEST` vs `VALIDATION_ERROR` both allowed.
3. Malformed path UUID: 400 vs 404 depending on framework binding.
4. Unknown JSON fields: ignore or reject.
5. UUID generation: application vs database.
6. Enum DB checks vs application-only enforcement.
7. UI list route: `/` or `/tickets`.
8. Search trigger: debounce vs explicit submit.
9. After PATCH/transition: use response body vs refetch (responses omit comments).
10. Hibernate `ddl-auto`: `none` vs `validate`.
11. Physical table naming: suggested names vs equivalents.
12. Frontend structure/component library: unspecified.

## Contradictions

- No hard contradictions with frozen decisions.
- Soft wording: `data-model.md` “status must not be accepted on create” vs `state-machine.md` SM-17 normative **400** if `status` present — treat as reject (400), not ignore.
- `TicketResponse` example shows `comments: []` while normative rule omits `comments` on non-GET-by-id responses — easy to misread.

## Missing acceptance criteria

1. Assignee max length 100 (FR-1/FR-4).
2. Author max 100 / content max 2000 (FR-5).
3. Whitespace-only title/description as blank (FR-1); trim rules only in data model.
4. Empty PATCH body → 400 (API only).
5. Invalid `status` query → 400 (API only).
6. `size=0` / negative `page` → 400 (API only; FR-2 covers size 101).
7. List sort order (API/tests only).
8. Self-transition → 409 (matrix/tests; not FR-6 bullet).
9. Comment on CANCELLED explicitly (CLOSED covered; CANCELLED implied).
10. Optional `Location` on 201 — recommended, not required.
11. CORS — required for local full-stack, no AC.
12. Exact `fieldErrors` message strings — structure only.
13. Concurrency / optimistic locking — unspecified.
14. Automated frontend tests — not required; UI-1…UI-8 manual.

## Assumptions encoded in specs (for implementers)

| Assumption | Location |
|------------|----------|
| Comments only via GET ticket by id | api-contract, requirements FR-3 |
| Comments allowed on terminal tickets | requirements, api-contract, ui-flow |
| Transition field name `toStatus` | api-contract, state-machine, ui-flow |
| Empty PATCH → 400; status on PATCH → 400 | api-contract |
| Status on create → 400 | state-machine SM-17, test-strategy |
| Trim then validate; whitespace assignee → null | data-model |
| Page zero-based; default 0; size 1..100 | api-contract |
| Sort createdAt DESC, id DESC | api-contract |
| Empty search = no filter; search AND status | api-contract |
| Comment updates parent `updatedAt` | data-model, api-contract |
| Error code catalog listed in api-contract | api-contract |
| Page DTO: content, page, size, totalElements, totalPages | api-contract |
| Self-transition 409 | api-contract, state-machine |
| UI routes for list/create/detail | ui-flow |
| No mandatory automated frontend tests in v1 | test-strategy |

## Recommended follow-ups (when explicitly instructed)

1. Tighten acceptance criteria for length limits, empty PATCH, invalid query params, and sort.
2. Pick one error code for illegal PATCH `status`.
3. Normatively require omit vs empty `comments` on non-detail responses.
4. Decide create-with-`status` is always 400 (already SM-17) and mirror that wording in data-model.

## Related

- Chat: `.specstory/history/003-create-specification-artifacts.md`
- Decisions: `docs/architecture-decisions.md`
- Prompts: `docs/prompt-history.md`

---

## T-01 human review — bootstrap dependencies and auto-config exclusions

Date: 2026-09-24
Scope: Backend bootstrap (`T-01`) only

Observations:

- AI preloaded dependencies required by later planned tasks during bootstrap.
- Human review determined that these dependencies are explicitly required by the approved architecture and do not represent scope creep.
- AI temporarily excluded database-related Spring Boot auto-configuration so T-01 could start without database configuration.
- This is accepted for bootstrap, but T-02 must configure PostgreSQL and Flyway and remove the temporary exclusions.

Package freeze (human follow-up): Java base package and Maven `groupId` set to `com.c2assignment`.

---

## T-31 implementation review — backend + frontend (AI self-review)

Date: 2026-09-24
Scope: Backend implementation (T-03, T-05..T-20, T-29) and frontend (T-04, T-21..T-28)
Method: Cross-check against `spec/*`, `rules/*`, `docs/implementation-plan.md`; findings surfaced by the Testcontainers/MockMvc IT suite and live E2E.

### AI mistakes found (meaningful)

1. **`@NotBlank` on PATCH DTO rejected `null` — broke partial PATCH entirely.**
   - `jakarta.validation.constraints.NotBlank` is invalid for `null`, which is exactly the "field not changed" case for PATCH. Every valid partial PATCH returned 400 `VALIDATION_ERROR`.
   - Fix applied: PATCH DTO now uses only `@Size`; present-but-blank values are rejected in the service via `FieldValidationException` → 400 `VALIDATION_ERROR` with per-field messages (matches api-contract).
   - Lesson: PATCH (partial) semantics require "null = unchanged, present = validate", which bean validation cannot express with `@NotBlank`.

2. **`POSTGRES_PORT` default left at 5432 in the original compose default** — resolved earlier by defaulting to 5433 (native Postgres owns 5432). Recheck: `.env.example` documents 5433. No further action.

3. **Test isolation was initially wrong: IT classes shared one Postgres container and committed data**, causing FK clash in `deleteAll()` and count-dependent assertions to fail depending on test order. Fix: `@Transactional` per IT class so every test rolls back; MockMvc runs on the same thread and joins the test transaction.

4. **Test helper misused content as a timestamp** (`Instant.parse("OLD 2026-09-01")`). Fix: pass explicit `createdAt`.

5. **Two `V1` migrations after deletion of placeholder** (stale `target/`) — required `mvn clean`. Fix applied; documented in session 005.

### Behavior verified correct against the spec (no change needed)

- Unknown JSON fields → 400 `BAD_REQUEST`; invalid enums → 400 `VALIDATION_ERROR` w/ `fieldErrors`.
- Malformed UUID path → 400 `BAD_REQUEST`; missing UUID → 404 `NOT_FOUND`.
- Status only via transitions; invalid/self transition → 409 `INVALID_TRANSITION`; terminal statuses reject all.
- Partial PATCH keeps `status` and updates `updatedAt`; empty PATCH `{}` → 400 `BAD_REQUEST`.
- Comment parent `updatedAt` bumps; comments allowed on CLOSED/CANCELLED.
- Search: case-insensitive substring, `%`/`_`/`\` escaped, `search` + `status` AND; sort `createdAt DESC, id DESC`; `page`/`size` bounds.
- create/patch/transition responses omit `comments`; detail includes them ordered `createdAt ASC, id ASC`.
- CORS allows only `http://localhost:5173` (disallowed origin → 403, configurable via `spring.mvc.cors.allowed-origins`).
- Frontend: `/` → `/tickets`; search submitted explicitly; status not editable in form; transitions restricted to valid targets (UX-only; backend validates).

### Remaining caveats

- No automated frontend tests per test-strategy (UI-1..UI-10 manual); E2E covered the API contract from the browser origin.
- Concurrency control (`@Version`) intentionally out of scope (architecture §9).
- Testcontainers 1.21.4 pinned in `pom.xml` because the Spring Boot-managed 1.20.x can't talk to Docker Engine 29.

## Related

- Chats: `.specstory/history/004-continue-implementation.md`, `.specstory/history/005-continue-implementation-backend-frontend.md`
- Decisions: `docs/architecture-decisions.md`
- Prompts: `docs/prompt-history.md`
