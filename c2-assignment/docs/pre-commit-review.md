# Pre-Commit Review

Support Ticket Management System — pre-commit review of exactly what would be submitted to the evaluator.
This review is documentation-only; no application, configuration, or specification files were modified. Nothing was staged or committed.

## Current Git State

- **Branch:** `master`
- **Commit history:** exactly **1 commit** — `8e25429 chore: initialize repository structure` (2026-09-24 10:50:54 +0530, Saurabh).
- **Tracked files (4):** `README.md`, `docs/ai-review.md`, `docs/architecture-decisions.md`, `docs/prompt-history.md`.
  - All were **0-byte empty placeholders** in the committed tree (`git ls-tree -r -l 8e25429` → size 0, blob `e69de29…` for all 4).
- **Modified tracked files (4):** all four above (` M` in worktree) — i.e. all real content is uncommitted.
- **Untracked (non-ignored) files:** **82**.
- **Ignored files on disk:** **2380** paths.
- **Other refs:** no tags, no stashes, no remote-tracked branches besides `master`.
- **Generated / build artefacts (ignored):**
  - `backend/target/` — 63 paths: compiled classes, `test-classes`, `generated-sources`, `generated-test-sources`, `maven-status`, `surefire-reports` (9 XML report files), Flyway-migrated copy of the schema SQL.
  - `frontend/node_modules/` — 2313 paths (npm dependencies).
  - `frontend/dist/` — 3 paths (Vite build output: `assets/index-ByoJlHQM.js`, `assets/index-D7wqj-FS.css`, `index.html`).
  - `.env` — 1 path (local dev environment file with DB credentials).

**Bottom line:** the committed repository tree contains zero deliverables; everything that constitutes the assignment is currently uncommitted (82 untracked + 4 modified tracked files).

## Proposed Submission Manifest

All 86 existing candidate files, grouped by category (counts verified by `git status --porcelain --untracked-files=all`). The table below lists every file; the explanation applies to the group, with per-file notes where useful.

### Application (57 files)

**backend/ — Spring Boot 3.4 REST API, Java 21 (42 files)**
| Path | Why it belongs |
|---|---|
| `backend/pom.xml` | Maven build: Spring Boot parent, dependencies, surefire `**/*Test`/`**/*IT` config, pinned `testcontainers.version=1.21.4` |
| `backend/src/main/resources/application.yml` | Runtime config: port 8080, datasource via `${SPRING_DATASOURCE_*}`, `ddl-auto=validate`, Flyway enabled — credentials externalized |
| `backend/src/main/resources/db/migration/V1__create_tickets_and_comments.sql` | Schema — the single source of truth for the data model (nullable `assignee`, `ticket_status_check` constraint, CASC properties) |
| `backend/src/main/java/com/c2assignment/C2AssignmentApplication.java` (1) | Application entry point |
| `…/common/` (6) | `ApiError`, `GlobalExceptionHandler` (+ `BadRequestException`, `FieldValidationException`, `InvalidTransitionException`, `TicketNotFoundException`) — uniform error contract with `fieldErrors` |
| `…/config/WebConfig.java` (1) | CORS configuration (origin allow-list from environment) |
| `…/comment/` + `…/comment/dto/` (7) | Comment entity/controller/service/mapper/repository + DTOs |
| `…/ticket/` + `…/ticket/dto/` (14) | `Ticket`, `TicketStatus`, `Priority`, `TicketStatusTransitionPolicy` (state machine), controller/service/mapper/repository + 6 request/response DTOs |
| `backend/src/test/java/` (10) | `AbstractIntegrationTest` (shared Testcontainers base), 8 MockMvc/Testcontainers IT classes, `ticket/TicketStatusTransitionTest` (unit) — the 121-test suite and state-machine coverage |

**frontend/ — React 18 + Vite 5 SPA (14 files)**
| Path | Why it belongs |
|---|---|
| `frontend/index.html`, `frontend/package.json`, `frontend/package-lock.json`, `frontend/vite.config.js` | App shell; manifest; reproducible dependency lock; dev/preview server config (port 5173) |
| `frontend/src/main.jsx`, `…/App.jsx`, `…/index.css`, `…/constants.js` | Bootstrap, routing, styles, shared constants |
| `frontend/src/api/client.js` | API client — base URL from `VITE_API_BASE_URL`, defaults to `http://localhost:8080/api/v1` |
| `frontend/src/components/ErrorDisplay.jsx`, `…/TicketForm.jsx` | Shared error + ticket form components |
| `frontend/src/pages/TicketCreatePage.jsx`, `…/TicketDetailPage.jsx`, `…/TicketListPage.jsx` | The three UI screens (create, detail+comments, list+search) |

**Docker / docker-compose (1 file)**
| Path | Why it belongs |
|---|---|
| `docker-compose.yml` | Postgres 16 service, port `5433→5432`, credentials interpolated from `POSTGRES_*`, named volume, healthcheck. Backend/frontend run natively (README Quick start); only Postgres is containerized |

### Specifications — spec/ (7 files)

| Path | Why it belongs |
|---|---|
| `spec/requirements.md` | Functional + non-functional requirements (FR-1…, NFR-4 secrets via env), the approved baseline |
| `spec/architecture.md` | Layered architecture, technology stack, out-of-scope decisions |
| `spec/data-model.md` | Entities, columns, constraints, relationships |
| `spec/api-contract.md` | Full REST contract (paths, verbs, request/response bodies, error codes) |
| `spec/state-machine.md` | Ticket status transition matrix |
| `spec/ui-flow.md` | Frontend screens and navigation |
| `spec/test-strategy.md` | Test plan (unit/IT split, Testcontainers, manual UI scope, transition coverage) |

### AI development instructions (8 files)

| Path | Why it belongs |
|---|---|
| `rules/api-standards.md`, `rules/java-springboot.md`, `rules/security.md`, `rules/testing.md` (4) | Enforced development rules (e.g. `security.md`: never commit secrets; credentials only via env) |
| `skills/documentation/skill.md` (1) | Session-documentation skill that governed the `.specstory/` + docs updates |
| `commands/generate-tests.md`, `commands/review-code.md`, `commands/review-spec.md` (3) | Repeatable slash-command workflows used to drive the process |

### AI process evidence (11 files)

| Path | Why it belongs |
|---|---|
| `.specstory/history/002-create-specification.md`, `003-create-specification-artifacts.md`, `004-continue-implementation.md`, `005-continue-implementation-backend-frontend.md`, `006-verify-audit-fix-state.md` (5) | Per-session development records — summaries of prompts and outcomes (not verbatim transcripts) |
| `docs/prompt-history.md` | Narrative of every major prompt → outcome, incl. the issue/correction trail |
| `docs/ai-review.md` | Spec consistency review + human/AI implementation reviews (issues raised → reasons → corrections) |
| `docs/implementation-plan.md` | T-01…T-36 task plan the implementation followed |
| `docs/architecture-decisions.md` | ADR-001…ADR-006 decisions (recorded during spec phase) |
| `docs/final-audit.md` | Final read-only audit incl. verified 121-test result, FR traceability, assignee API limitation finding |
| `docs/submission-readiness.md` | Readiness assessment — Verified / Evidence gaps / Assessment risks / Recommended actions |

### Project documentation (1 file)

| Path | Why it belongs |
|---|---|
| `README.md` | Entry point: overview, prerequisites, Quick start (DB/backend/frontend), test commands, API table, project layout |

### Submission scaffold (2 files)

| Path | Why it belongs |
|---|---|
| `.gitignore` | Ignores `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `.env`, `!.env.example`, IDE/OS files — the exclusions in this report |
| `.env.example` | Template for env vars consumed by compose + Spring; safe placeholder values only |

**Manifest totals:** backend 42 + frontend 14 + docker-compose 1 + spec 7 + rules/skills/commands 8 + AI evidence 11 + README 1 + scaffold 2 = **86 files**. `docs/pre-commit-review.md` (this document) is a further proposed submission file, bringing the total to **87**.

## Files To Exclude

Excluded paths are already covered by `.gitignore`; the exact breakdown (from `git status --porcelain --ignored`):
| Path | Count | Type |
|---|---|---|
| `backend/target/` | 63 | Build artefacts: `.class` files, surefire reports, maven-status, Flyway output copies |
| `frontend/node_modules/` | 2313 | Installed npm dependencies (regenerable via `npm install`) |
| `frontend/dist/` | 3 | Vite production build output (regenerable via `npm run build`) |
| `.env` | 1 | Local DB credentials (regenerable from `.env.example`) |
| **Total** | **2380** | |

`.gitignore` also omits other categories that were checked AND found absent (see Accidental Files): IDE metadata (`.idea/`, `.vscode/`, `*.iml`), OS files (`.DS_Store`, `Thumbs.db`), editor swap/backup (`*.swp`, `*~`). No `docker volume` data, local database dumps, or caches exist outside git exclusion rules (Postgres data lives in the Docker volume `support_tickets_pgdata`, not in the repo tree).

## Secret Review

Scan of every submission candidate (86 files) for passwords, API keys, tokens, private keys, credentials, JWT/cloud key material, and `AKIA*` patterns.

- **Genuine secrets found:** **none** in any file proposed for submission.
- **`.env` (excluded, 1 file)** — contains **local database credentials**: `POSTGRES_DB/USER/PASSWORD` and `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`. These are throwaway local values for the Dockerized dev Postgres, but they are real credentials to the local instance and must remain excluded.
- **Placeholders:**
  - `.env.example` — `POSTGRES_PASSWORD=local_dev_only_change_me`, `SPRING_DATASOURCE_PASSWORD=local_dev_only_change_me` (explicitly labelled "local dev only, change me"); `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/support_tickets`. **Safe to submit.**
- **Test-only values:**
  - `backend/src/test/java/com/c2assignment/AbstractIntegrationTest.java:26` — `.withPassword("test_only")` for the Testcontainers Postgres container (throwaway, non-real). **Safe to submit.**
- **Configuration placeholders (not secrets):**
  - `backend/src/main/resources/application.yml:10-14` — `url/username/password` = `${SPRING_DATASOURCE_URL/USERNAME/PASSWORD}` env placeholders.
  - `docker-compose.yml` — `${POSTGRES_DB/USER/PASSWORD}` interpolation from the environment.
- **False positives ruled out:** `js-tokens` in `frontend/package-lock.json` (npm dependency name, not a token); "secrets via env" wording in rules/spec/docs (policy statements); `TicketRepositoryIT.java` "Password reset" (test fixture text).
- **No** API keys, JWT secrets, private keys (`BEGIN RSA/OPENSSH/PRIVATE`), AWS/cloud credentials, or npm package tokens present anywhere in the submission set.

## Accidental Files

Checked across the entire worktree (excluding ignored dirs) for screenshots, terminal captures, dumps, archives, PDFs, images, logs, temporary/backup files, IDE state, and personal files.

- **Result: none found.**
- Full extension inventory of all 86 candidates: 39 `.java` (incl. 1 `.xml` pom), 27 `.md`, 7 `.jsx`, 3 `.js`, 2 `.yml`, 2 `.json`, 1 `.sql`, 1 `.html`, 1 `.css`, 1 `.gitignore`, 1 `.env.example`.
- The only `.sql` file is the Flyway migration (expected). The only `.xml` is the Maven POM.
- No images/PDFs/archives (`png/jpg/pdf/zip/tar/gz`), no `*.log/tmp/bak/orig/swp/class`, no `.DS_Store`/`Thumbs.db`, no `.idea`/`.vscode`/`*.iml`, no hidden files other than the legitimate `.env` (ignored), `.env.example`, and `.specstory/`.
- No binary/NUL-byte content in any candidate (all text).

## Repository Completeness

Mapping of evaluator needs to the submission:

| # | Evaluator need | Covered by | Status |
|---|---|---|---|
| 1 | Understand requirements | `spec/requirements.md` | OK |
| 2 | Understand architecture | `spec/architecture.md`, `docs/architecture-decisions.md` | OK |
| 3 | Understand data model | `spec/data-model.md`, `V1__create_tickets_and_comments.sql` | OK |
| 4 | Understand API | `spec/api-contract.md`, `README.md` API table, `backend/src/...` | OK |
| 5 | Understand state machine | `spec/state-machine.md`, `TicketStatusTransitionPolicy.java` | OK |
| 6 | Run the backend | `README.md` (Quick start + `mvn spring-boot:run`), `pom.xml`, `application.yml`, `docker-compose.yml` | OK |
| 7 | Run the frontend | `README.md` (`npm install`/`npm run dev`), `package.json`, `vite.config.js` | OK |
| 8 | Run tests | `README.md` (`mvn test`, Docker required), `spec/test-strategy.md`, `pom.xml` surefire includes | OK |
| 9 | Understand AI development instructions | `rules/` (4), `skills/` (1), `commands/` (3) | OK |
| 10 | Understand the prompt history | `docs/prompt-history.md`, `.specstory/history/` (5 sessions) | OK (with caveat, see N4) |
| 11 | Understand AI mistakes and human review | `docs/ai-review.md` (issues → reasons → corrections), `docs/final-audit.md` findings trail | OK |
| 12 | Understand the final audit | `docs/final-audit.md`, `docs/submission-readiness.md` | OK |

**Genuinely missing / noted gaps (all documented in the final audit / readiness docs, none blocking):**
- No frontend automated tests (by design — `test-strategy.md` schedules manual UI testing for v1).
- No code-coverage metric (no JaCoCo) — coverage is qualitative, not measured.
- No CI configuration.
- Application services are not containerized — `docker-compose.yml` provisions Postgres only; backend (Maven) and frontend (Vite) run natively per the README, so the evaluator needs a local JDK 21 / Node 18+ toolchain.
- `.specstory/` contains 5 session summaries, not verbatim transcripts; numbering starts at 002 (no 001), and the final-audit/readiness/pre-commit sessions have no dedicated history entries (N4).

## Assessment Artefacts

Evidence verified live for this review:
- **Git inventory** — branch, refs, tracked/ignored/untracked enumerated exactly as reported above; committed tree confirmed empty (4 × 0-byte blobs).
- **Test suite** — `mvn clean test` passes **121 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**; 9 `surefire-reports/*.xml` files present (per-class: TicketStatusTransitionTest 28, TicketTransitionIT 30, TicketApiIT 22, TicketListIT 14, TicketCommentIT 12, CorsIT 3, TicketPersistenceIT 3, TicketRepositoryIT 6, FlywayMigrationIT 3).
- **Frontend build** — `npm run build` completes cleanly (Vite).
- **Compose file** — `docker compose config --quiet` validates (no syntax errors).
- **Secret/accidental-file/configuration scans** — results as recorded in the sections above.
- **Consistency** — README, `architecture-decisions.md`, `prompt-history.md`, `final-audit.md`, and `submission-readiness.md` cross-checked; no stale final-result claims (test counts, status line, specstory wording).

## Blocking Issues

- **B1 — The entire deliverable is uncommitted.** The only commit (`8e25429`) tracks 4 empty files (0 bytes each). All 86 content files (82 untracked + 4 modified tracked, all of which currently contain the real work) are uncommitted. If the repository were handed over in its committed state today, nothing would be delivered. A commit including the manifest in `Proposed Submission Manifest` must be created before submission.
- **B2 — `.env` contains local DB credentials.** It is correctly ignored today, but any forced staging (`git add -f .env`) or a later rule change would leak local credentials into history/repo. Guard against it during the commit step.

## Non-Blocking Issues

- **N1** — No incremental commit trail: history is a single "chore: initialize repository structure" snapshot, so the commit sequence itself does not document development stages (the `.specstory/` files carry that evidence instead). Accepted as a deliberate process choice.
- **N2** — No frontend automated tests (documented as manual in `test-strategy.md`).
- **N3** — Code coverage is not measured (no JaCoCo); only qualitative/descriptive coverage evidence exists.
- **N4** — Prompt-history evidence is session summarisation, not verbatim transcripts; no session 001; the final-audit, readiness and pre-commit sessions are not logged as `.specstory/` entries. This limitation is explicit in `README.md`, `prompt-history.md`, and `submission-readiness.md`.
- **N5** — No CI configuration.
- **N6** — `docker-compose.yml` interpolates `${POSTGRES_PASSWORD}` without a `:-` fallback; running `docker compose up` without sourcing `.env` would pass an empty password to Postgres init.
- **N7** — Backend/frontend are not containerized (only Postgres is); evaluator needs JDK 21 + Node 18+ and Docker with a usable socket (`DOCKER_HOST` noted in README).
- **N8** — `.env.example` contains functional throwaway credentials (`local_dev_only_change_me`) that match `requirements.md`/README local-run defaults; safe, but worth noting these are real working defaults for local use only.

## Recommended Actions

1. **Commit the full manifest before submission (B1):** stage exactly the 86 existing files + `docs/pre-commit-review.md` (87 files) per the manifest, with a message reflecting a content-adding commit (the existing "chore: initialize repository structure" covers only empty placeholders).
2. **Verify `.env` is never staged (B2):** after staging, run `git status` and confirm no `.env`, `target/`, `node_modules/`, or `dist/` entry appears in the staged set.
3. **Re-verify after staging:** rerun `git status --porcelain --ignored` and confirm the staged tree == Proposed Submission Manifest (87 files) and the ignored set == 2380 paths.
4. **(Optional, docs only)** — document in `prompt-history.md` that the pre-commit review is retained in `docs/pre-commit-review.md`; no action required if prompt-history is considered complete.
5. **(Optional, future hardening, N6)** — add a `:-` default for `POSTGRES_PASSWORD` in `docker-compose.yml` in a future iteration (out of scope for the frozen application).

---

## Final Counts

- **Files proposed for submission:** **87** (86 existing content files + `docs/pre-commit-review.md` created by this review: backend 42, frontend 14, docker-compose 1, spec 7, rules/skills/commands 8, AI-process evidence 11, README 1, scaffold 2, pre-commit review 1).
- **Files that should remain ignored/excluded:** **2380** (backend/target 63, frontend/node_modules 2313, frontend/dist 3, `.env` 1).
- **Blocking issues:** **1** — B1: no implementation committed (the committed tree is empty).
- **Non-blocking issues:** **8** — N1–N8 above.
- **Confirmation:** no files were staged and no commit was created during this review; no file other than `docs/pre-commit-review.md` was modified.