# Prompt History

This file records major prompts used during the Support Ticket Management System project.
It does not claim that features are implemented or verified.

## 2026-09-24 — Assignment analysis (pre-spec)

**Prompt intent:** Analyze the assignment; identify ambiguities, missing requirements, business rules, API questions, data-model questions, and testing risks. Return analysis only. Do not modify files or write implementation.

**Outcome:** Analysis saved in `.specstory/history/002-create-specification.md`.

**Process stage:** Requirement clarification (analysis)

---

## 2026-09-24 — Create specification artifacts

**Prompt intent:** After requirement clarification, create only:

- `spec/requirements.md`
- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`
- `spec/test-strategy.md`

Apply frozen decisions (full-stack scope, out-of-scope list, actor, ticket/comment models, state machine, PATCH vs transitions, API list, errors, PostgreSQL/Flyway, layered architecture, Testcontainers testing).

Make specs testable and unambiguous. No implementation code. After creation, consistency-review all specs and report ambiguities, contradictions, missing acceptance criteria, and assumptions. Do not modify specs after review unless instructed.

**Outcome:** Seven specification files created. Consistency review reported (not applied as edits). Chat saved in `.specstory/history/003-create-specification-artifacts.md`.

**Process stage:** Specification

---

## 2026-09-24 — Persist history and documentation

**Prompt intent:** Before follow-up work, save chat history and write documentation in the project docs directory.

**Outcome:** Updated this file plus `docs/architecture-decisions.md` and `docs/ai-review.md`.

**Process stage:** Specification documentation

---

## 2026-09-24 — Continue implementation (session kickoff)

**Prompt intent:** Read the repo and requirement, read the last chat/response in the chat history, properly maintain chat history and documentation, and share the POC doc (implementation plan) and abide by it.

**Outcome:**

- Chat saved in `.specstory/history/004-continue-implementation.md`.
- Confirmed current state: T-01 done, T-02 largely done, placeholder Flyway baseline to be replaced; no tests/frontend yet.
- Environment: Java 21, Maven, Node 18, Docker (default socket `unix:///var/run/docker.sock`); Postgres available via Docker.

**Process stage:** Implementation kickoff

---

## 2026-09-24 — Continue implementation: backend + frontend

**Prompt intent:** Continue if there are next steps (per plan), or stop and ask if unsure.

**Outcome:**

- Unblocked Testcontainers (pinned 1.21.4, Docker API ≥ 1.44), DB-side backend suite green: **119 tests, BUILD SUCCESS**.
- Fixed an AI bug where `@NotBlank` on PATCH DTOs rejected `null` and broke partial PATCH; service-layer blank validation added (`FieldValidationException`).
- Built the full frontend (T-04, T-21..T-28) in `frontend/` (React + Vite + React Router, port 5173); `npm run build` clean.
- Manual E2E (T-30) passed on the live stack: CORS, CRUD, full transition lifecycle, 409 invalid transition, validation/unknown-field errors, comments on closed tickets.
- Chat saved in `.specstory/history/005-continue-implementation-backend-frontend.md`; implementation review appended to `docs/ai-review.md`.

**Process stage:** Implementation + testing + acceptance

---

## 2026-09-25 — State verification after audit fix (read-only)

**Prompt intent:** Do not make any production/spec/test changes. Inspect the current filesystem directly (do not trust stale docs), verify the `RESOLVED → IN_PROGRESS` test gap from the audit was actually closed, run the suite, report the true repository state, and identify any stale statements in `docs/final-audit.md` without updating it.

**Outcome (verified live, no files modified other than chat history):**

- `RESOLVED → IN_PROGRESS` present as an invalid transition in `TicketStatusTransitionTest.java:38` (invalid CSV) and in `TicketTransitionIT.java:61` (invalid CSV, 409 + persisted-state-unchanged assertion).
- `spec/test-strategy.md:44` includes `RESOLVED → IN_PROGRESS` in the invalid-transition list.
- `mvn clean test` → **121 tests, 0 failures, 0 errors, BUILD SUCCESS**. `TicketStatusTransitionTest` now 28 tests; `TicketTransitionIT` now 30 tests (both +1 from the audit's 27/29). All 25 state-machine matrix cells covered.
- Stale `DebugControllerIT` surefire report no longer present (only the 9 live classes remain in `target/surefire-reports`).
- `docs/final-audit.md` is **stale** at this point: 5 statements still claim the gap is open (FR-6 row PARTIAL, matrix table `NOT TESTED`, §2 gap paragraph, §5 gap bullet, Non-Blocking Finding #1). It has NOT been updated yet by this read-only session (per instruction).
  - *(Later annotation — this was subsequently resolved: the follow-up session "Update final-audit.md transition coverage" did update `docs/final-audit.md`, which now records FR-6 PASS, the matrix cell as tested, Non-Blocking Finding #1 as closed, and the current 121-test result.)*
- Chat saved in `.specstory/history/006-verify-audit-fix-state.md`.

**Process stage:** Post-fix state verification / documentation

---

## 2026-11-11 — Full end-to-end verification and frontend defect fixes

**Prompt intent:** Make sure the project is completed and works well end to end.

**Outcome:**

- Environment verified: Docker via `DOCKER_HOST=unix:///var/run/docker.sock` (Docker Desktop CLI context socket stale), JDK 21, Maven 3.8.7, Node 18.19.
- `mvn clean test` → **121 tests, 0 failures, 0 errors, BUILD SUCCESS**. `npm run build` → clean.
- Live-stack API smoke test (Postgres via docker compose, backend `:8080`): 48 checks across create/list/search/filter/pagination/get/patch/state-machine/comments/error-contract/CORS — all conform to `spec/api-contract.md` (initial failures were smoke-script bugs: wrong JSON keys; re-verified).
- **Two genuine frontend defects found via headless-browser E2E (Chromium/Playwright run outside the repo — no project dependency added) and fixed:**
  1. `TicketListPage.jsx` pagination label read `data.number`, but the API page DTO field is `page` (`TicketPageResponse`, api-contract §4) → label rendered "Page NaN of N". Fixed to `data.page + 1`.
  2. `TicketDetailPage.jsx` replaced local ticket state with PATCH/transition responses, which **omit `comments`** per api-contract §4 → `ticket.comments.length` threw `TypeError` and the detail page crashed on every successful PATCH save and every successful transition (breaking Flows A/B/C, UI-3, UI-7, UI-10). Fixed by merging the response while preserving the loaded comments: `{ ...updated, comments: current.comments }`.
  - Note: the earlier self-reported manual pass (T-30, session 005) claimed these flows worked; that claim was incorrect for the crash bug. Browser E2E after the fixes: 21/21 checks covering ui-flow Flows A–E and UI-1…UI-10 pass, plus UI-6 (409 conflict banner + GET resync) and persistence across a backend restart (ticket status/comments intact from the Postgres volume).
- Backend untouched; no spec, API, schema, or dependency changes (no silent scope expansion).

**Process stage:** End-to-end verification / defect fix
