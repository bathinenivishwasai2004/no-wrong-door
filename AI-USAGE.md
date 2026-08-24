# AI Usage Disclosure

This document describes how AI tools were used throughout development of the No Wrong Door project, with human direction, review, testing, and acceptance.

---

## Summary

Development of this project was carried out using the **Google Antigravity AI coding assistant**, with human direction, review, and decision-making at every stage.

AI assistance was extensive across requirements analysis, architecture planning, code generation, test writing, debugging, and documentation. However, the human developer provided continuous direction, reviewed generated code and diffs, made implementation and workflow decisions, ran commands and services, tested the application, investigated bugs, validated the Surprise Challenge, and approved the final submission.

Neither party worked alone:

- **AI** assisted by generating code, tests, configuration, and documentation drafts, as well as reasoning through architectural options and edge cases.
- **The human developer** directed the implementation through prompts and instructions, reviewed all generated code and diffs, approved architecture plans, executed and tested all services, investigated issues, and made all final acceptance decisions.

---

## Human Involvement

The following responsibilities belonged to the human developer throughout the project. AI assistance was extensive, and these points document the genuine human direction and oversight exercised at every step rather than claiming the human manually wrote every line of code:

- **Interpreted challenge requirements:** Read the specification, identified required source integrations (REST and XML), reconciliation rules, search/investigation criteria, and the Surprise Challenge resilience requirements.
- **Directed implementation through prompts:** Provided structured task instructions and prompts to the AI assistant for each development phase.
- **Reviewed and approved architecture plans:** Approved source adapter isolation, domain models, JPA/JPQL repository design, the ingestion pipeline structure, and the retry/backoff mechanism.
- **Made decisions about application workflow and UI behavior:** Decided the dashboard layout, visual hierarchy, evidence display, and responsive UI polish.
- **Directed automatic ingestion behavior:** Directed the frontend to automatically trigger ingestion upon detecting an empty database on first load, eliminating the need for a manual "Ingest Data" button.
- **Directed search/results behavior:** Mandated that empty search states do not dump all resident records and that results only appear after the user actively performs a search.
- **Reviewed AI-generated code and diffs:** Examined code modifications, pull requests/diffs, and ensured standards and requirements were met before accepting changes.
- **Ran all services:** Started, monitored, and stopped the REST mock service (port 8081), the XML mock service (port 8082), the Spring Boot backend (port 8080), and frontend tooling.
- **Verified backend APIs and source health using curl:** Issued manual HTTP requests against `/api/status`, `/api/ingest`, `/api/residents`, and mock endpoints to verify status codes and JSON payloads.
- **Diagnosed startup issues:** Identified and resolved port 8080 conflicts and Java environment configuration issues during backend startup.
- **Investigated ingestion failures:** Checked logs when source services were down or unreachable, confirming that appropriate error statuses were logged and returned.
- **Investigated frontend data/display inconsistencies:** Found and directed fixes for layout glitches, card rendering, and status tag colors.
- **Investigated source-record calculations:** Investigated and resolved an initial incorrect 1,167 source-record calculation, ensuring accurate source totals from live API reads.
- **Directed fixes for search behavior:** Ensured multi-field searches (name, REST ID, XML reference, city, address) worked accurately and case-insensitively.
- **Tested partial and full source failures:** Manually tested behavior when either or both mock services were unavailable.
- **Configured the XML source with a 40% failure rate:** Executed the XML mock service with `--failure-rate 0.40` to evaluate system resilience.
- **Verified retry behavior manually:** Inspected backend logs to confirm initial HTTP 500 failures triggered exponential backoff retries and subsequent successful ingestions.
- **Verified the complete XML failure scenario:** Shut down the XML mock service completely and validated that the ingestion completed with `PARTIAL` status while keeping REST records fully usable.
- **Reviewed the final 54/54 test result:** Ran the entire test suite via Maven Wrapper and verified that all unit and integration tests passed cleanly.
- **Reviewed the final Git diff:** Inspected git status and diffs across the entire repository to ensure clean, high-quality code.
- **Reviewed documentation:** Reviewed and refined `README.md`, `DECISIONS.md`, and `AI-USAGE.md`.
- **Approved final commit and GitHub submission:** Gave final approval for the project commit and submission.

---

## What Was AI-Generated / AI-Assisted

The following components were AI-generated or AI-assisted, with human direction, review, and verification:

| Component | AI-Assisted / Generated | Human-Directed & Reviewed |
| :--- | :---: | :---: |
| REST mock service (`mock-services/rest-source/`) | ✅ | ✅ |
| XML mock service (`mock-services/xml-source/`) | ✅ | ✅ |
| Spring Boot backend structure | ✅ | ✅ |
| Maven configuration (`pom.xml`, Maven Wrapper) | ✅ | ✅ |
| Configuration (`application.yml`) | ✅ | ✅ |
| REST adapter (`RestSourceAdapter.java`) | ✅ | ✅ |
| XML adapter (`XmlSourceAdapter.java`) | ✅ | ✅ |
| API controllers (`ResidentController`, `IngestionController`, `StatusController`) | ✅ | ✅ |
| CORS configuration (`WebConfig.java`) | ✅ | ✅ |
| Frontend (`index.html`, `style.css`, `app.js`) | ✅ | ✅ |
| Integration and unit tests | ✅ | ✅ |
| `README.md` | ✅ | ✅ |
| `DECISIONS.md` | ✅ | ✅ |
| `AI-USAGE.md` | ✅ | ✅ |

---

## Mock Services

### REST Mock Service
The REST Resident Index mock service (`mock-services/rest-source/`) was **AI-generated/AI-assisted with human direction and review**. It was originally created in Node.js and subsequently rewritten in Python (`rest_service.py`) to align with the official challenge specification. It provides a paginated JSON endpoint delivering resident contact records.

### XML Mock Service
The XML Benefits Register mock service (`mock-services/xml-source/`) was **AI-generated/AI-assisted with human direction and review**. It was originally created in Node.js and subsequently rewritten in Python (`xml_service.py`) to align with the official challenge specification. It provides an XML records endpoint and supports configurable failure rate (`--failure-rate`) and artificial latency for resilience testing.

---

## How AI Was Used

### Requirements Analysis
The AI analyzed the challenge brief, identifying key constraints: REST pagination and deduplication, XML schema structure, matching rules (`EXACT`, `PROBABLE`, `AMBIGUOUS`), persistence requirements, multi-field search, and the Surprise Challenge resilience criteria.

### Architecture and Planning
For each development phase, the AI proposed architectural designs and implementation plans (e.g., source adapter separation, retry policies, JPQL queries, response structures). The human developer reviewed, refined, and approved these plans before execution.

### Code Generation
Following approved plans, the AI generated implementation files across the Spring Boot backend, source adapters, domain entities, repositories, controllers, frontend UI, and test suites.

### Testing
The AI created automated test suites using JUnit 5, WireMock, and Spring Boot Test. The human developer ran and verified all tests locally.

### Debugging
When issues arose (port conflicts, display inconsistencies, calculation errors, search bugs), the AI assisted by analyzing error outputs and suggesting fixes. The human developer verified all fixes.

### Documentation
The AI drafted and updated project documentation (`README.md`, `DECISIONS.md`, `AI-USAGE.md`). The human developer reviewed, edited, and approved all documentation.

---

## What Was Not AI-Generated

- **Challenge requirements and constraints** — Provided by the hiring team.
- **Human approval of implementation decisions** — Evaluated and decided by the human developer.
- **Human validation and acceptance** — All manual testing, log verification, and browser checks were performed by the human developer.
- **Final acceptance testing and submission decisions** — Verified and executed by the human developer.

---

## Phase 0 — Initial Project Foundation

**Commit:** `35519fb` | **Date:** 2026-08-22

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. Created initial project structure: Spring Boot backend, Maven Wrapper, frontend static files, and mock services.
2. Implemented initial stub source adapters (`RestSourceAdapter`, `XmlSourceAdapter`) with basic health checks.
3. Built initial frontend dashboard skeleton with source status polling.
4. Created Phase 0 integration tests for source adapters.
5. Drafted initial `README.md`, `DECISIONS.md`, and `AI-USAGE.md`.

**Human review:** Project structure, technology choices, and all foundation files were reviewed and approved by the human developer before committing.

---

## Phase 1 — REST Source Integration

**Commits:** `b12a8e4`, `ccc1c04`, `ed609e7` | **Date:** 2026-08-22

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. **Design review:** Evaluated pagination termination strategy (`has_more` flag vs. record-count heuristic) and deduplication key options (`id` vs. full equality).
2. **Implementation:** Generated `RestResident.java`, `RestPageResponse.java`, `RestFetchResult.java`, updated `RestSourceAdapter.java` with pagination loop and deduplication logic, and updated `ResidentController.java` to expose REST fetch results.
3. **Testing:** Generated eight WireMock unit tests in `RestSourceAdapterUnitTest.java` and a Phase 1 integration test in `RestSourceAdapterTest.java`.
4. **Bug fix:** Identified Java source compatibility requirements for pattern-matching switch and configured `java.version` to 21.
5. **Documentation:** Drafted ADR-008, ADR-009, and ADR-010 for `DECISIONS.md`; updated `README.md` with Phase 1 instructions.

**Human review:** Implementation plans and generated code were reviewed and tested by the human developer.

---

## Phase 2 — XML Source Integration and Resilience

**Commit:** `6fa7a8e` | **Date:** 2026-08-22

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. **XML mock service:** The XML mock service was AI-generated/AI-assisted with human direction and review, implemented in Python (`xml_service.py`) supporting configurable failure rates and response delays.
2. **XML source implementation and parsing:** Implemented `XmlSourceAdapter` utilizing JAXB parsing (`BenefitsRegister`, `XmlRecord`) to parse incoming XML payloads.
3. **Source health check:** Added `checkHealth()` to ping `/health` on the XML service and report source availability.
4. **Retry behavior and backoff:** Implemented exponential backoff retry logic (up to 3 attempts with configurable initial delay) to handle transient HTTP 500 errors.
5. **Resilience & partial ingestion handling:** Handled complete XML failure gracefully, allowing ingestion to complete with `PARTIAL` status so that REST data remains fully usable.
6. **Domain model:** Created `UnifiedResident`, `IngestionRun`, `MatchStatus`, `UnifiedResidentRepository`, and `IngestionRunRepository`.
7. **Testing:** Created eight WireMock unit tests in `XmlSourceAdapterUnitTest.java` covering single record, multiple records, retry-then-success, all-retries-exhausted, connection failure, and health checks.

**Human review:** The human developer reviewed and approved the retry strategy, failure handling, field mappings, and verified behavior against running mock services.

---

## Phase 3 — Ingestion and Matching

**Commits:** `2f1e536`, `ee8101d` | **Date:** 2026-08-22

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. **Ingestion pipeline:** Built `IngestionService` coordinating parallel or sequential fetch, reconciliation, audit persistence (`IngestionRun`), and error reporting.
2. **Reconciliation & matching engine:** Built `MatchingService` with string and address normalizers (street suffix normalization, punctuation removal, case insensitivity, date formatting).
3. **Match classification:** Implemented logic for all match statuses:
   - `EXACT` — Name and Date of Birth agree exactly.
   - `PROBABLE` — Name agrees, XML DOB is missing, but address and city match.
   - `AMBIGUOUS` — Name agrees but DOB or address conflict; flagged for manual review with candidate reference links.
   - `REST_ONLY` / `XML_ONLY` — Records present in only one source with no credible match in the other.
4. **Matching evidence:** Tracked explicit comparison flags (`evidenceNameEqual`, `evidenceDobEqual`, `evidenceAddressEqual`, `evidenceXmlDobMissing`, `evidenceCandidateRefs`) on unified records.
5. **Repeated ingestion behavior:** Implemented upsert-by-natural-source-key idempotency ensuring repeated ingestion runs do not create duplicate resident records.
6. **Integration tests:** Generated ten unit tests in `MatchingServiceTest`, seven tests in `IngestionServiceTest`, and `IngestionPersistenceIntegrationTest` validating pipeline persistence against an H2 database.

**Human review:** Human review and testing were performed against the official mock dataset to verify reconciliation accuracy, category counts, and repeated-run stability.

---

## Phase 4 — Search and Resident Investigation

**Commit:** `afdb1d8` | **Date:** 2026-08-23

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. **Resident search API:** Implemented multi-field search queries in `UnifiedResidentRepository` supporting:
   - First name, last name, concatenated full name, XML name
   - REST ID search
   - XML reference prefix / full reference search
   - City and town search
   - Address line search
2. **Status filters:** Added query filtering by `MatchStatus` (`EXACT`, `PROBABLE`, `AMBIGUOUS`, `REST_ONLY`, `XML_ONLY`).
3. **Resident investigation / detail view:** Implemented `GET /api/residents/{id}` returning rich `ResidentResponse` DTOs with side-by-side REST and XML comparisons, field-by-field evidence indicators (`MATCH`, `NORMALIZED MATCH`, `DIFFERENT`, `MISSING`, `NOT AVAILABLE`), confidence level, and source availability flags.
4. **Ambiguous and manual-review behavior:** Highlighted ambiguous matches with warnings and candidate references for human review.
5. **Source-only behavior:** Handled `REST_ONLY` and `XML_ONLY` records with clear indicators showing that the counterpart source is not available.
6. **Testing:** Created unit and integration tests in `ResidentControllerTest` and `StatusAndIngestionControllerTest`.

**Human review:** The human developer manually tested all search parameters, filter combinations, and resident detail views via `curl` and the browser UI.

---

## Phase 5 — Frontend and Automatic Ingestion

**Commits:** `afdb1d8`, `cb9a6f8` | **Dates:** 2026-08-23

**Tool used:** Google Antigravity AI coding assistant

**Tasks completed with AI assistance:**
1. **Dashboard UI:** Built a responsive, dark-themed dashboard using vanilla HTML, CSS, and JavaScript with glassmorphism cards, stat counters, distribution breakdown bars, and live source health badges.
2. **Automatic ingestion:** Implemented automatic ingestion on first dashboard load when the database is empty (`totalResidents === 0`), completely removing the need for a manual "Ingest Data" button.
3. **Ingestion states:** Handled loading, success, partial, and failure banner states dynamically based on API responses.
4. **Dashboard statistics:** Implemented live statistics reading directly from `GET /api/status`.
5. **Search-required Results section:** Built the Results section so that on initial load and post-ingestion, it displays an initial "No search yet" state.
   > **Important:** The final dashboard does **NOT** display all residents automatically after ingestion. Results appear only after the user performs an active search.
6. **Search result cards & detail view:** Rendered interactive result cards displaying match badges, resident identifiers, and addresses. Clicking a card opens the slide-out resident investigation panel with detailed evidence comparison.
7. **Responsive UI improvements:** Added smooth transitions, mobile-friendly grid layouts, clear search button behaviors, and accessibility improvements.

**Human review:** The human developer directed the automatic ingestion flow, enforced the search-required display constraint, tested responsive layouts across window sizes, and resolved frontend display discrepancies.

---

## Phase 6 — Surprise Challenge

**Configuration:** XML mock service executed with:
```powershell
python mock-services\xml-source\xml_service.py --port 8082 --failure-rate 0.40
```

**Tool used:** Google Antigravity AI coding assistant (implementation and test planning); human developer (manual execution and validation).

**Tasks completed and resilience mechanisms:**
1. **Unreliable XML source:** The XML mock service was executed with approximately 40% HTTP 500 failures and artificial response latency.
2. **Retry with exponential backoff:** `XmlSourceAdapter` intercepted HTTP 500 errors and automatically retried the request up to 3 times with exponential backoff (initial delay 500 ms) configured via `application.yml`.
3. **Successful retry scenario:** In cases where a retry succeeded within 3 attempts, ingestion completed as `SUCCESS` with all 620 REST and 540 XML records unified.
4. **Complete XML failure scenario:** When all retries were exhausted or when the XML service was stopped entirely:
   - Ingestion completed with `PARTIAL` status.
   - All 620 REST records were ingested and preserved as `REST_ONLY`.
   - The backend remained fully available and responsive.
   - REST data remained completely usable for searching and investigation.
   - The error message was captured in the ingestion audit record.

**Human validation performed:**
- Configured and launched the XML service with `--failure-rate 0.40`.
- Triggered ingestion and verified backend logs showing initial HTTP 500 errors followed by successful retries.
- Terminated the XML mock service completely, executed ingestion, and verified `PARTIAL` status and error reporting via `curl` and the UI.
- Confirmed REST records remained searchable even with XML down.
- Restarted XML service and confirmed recovery on subsequent ingestion.

---

## Final Verification

**Date:** 2026-08-24

**Automated Test Suite:**
```text
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
All 54 unit and integration tests passed cleanly.

**Official Mock Dataset Verification (Live Ingestion):**

| Metric | Official Dataset Value | Ingestion Result | Status |
| :--- | :---: | :---: | :---: |
| REST source records | 620 | 620 | ✅ Pass |
| XML source records | 540 | 540 | ✅ Pass |
| Unified resident records | 820 | 820 | ✅ Pass |
| EXACT matches | 306 | 306 | ✅ Pass |
| PROBABLE matches | 34 | 34 | ✅ Pass |
| AMBIGUOUS matches | 7 | 7 | ✅ Pass |
| REST_ONLY records | 273 | 273 | ✅ Pass |
| XML_ONLY records | 200 | 200 | ✅ Pass |

*(Note: These figures reflect the results of running against the official mock dataset and are computed dynamically by the backend, not hardcoded into the application.)*

**Comprehensive Manual Verification:**
- Automatic ingestion on initial application startup.
- Full multi-field search (first/last/full name, REST ID, XML ref, city, address).
- Match status filtering across all categories.
- Clear search resetting the interface to the "No search yet" state.
- Resident investigation panel displaying field-by-field evidence and candidate references for ambiguous records.
- Live source health polling and status indicator updates.
- XML retry behavior under a 40% failure rate.
- Complete XML outage resulting in `PARTIAL` ingestion with usable REST data.
- Frontend responsiveness and backend API correctness.

---

## AI Limitations Observed

- **Assumptions about pre-existing mock services:** The AI initially assumed mock services existed in the environment; the human clarified they needed to be created.
- **Local environment tooling:** Maven was not globally installed; the AI adapted by utilizing the Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Mock service implementation language:** Initial mock services were generated in Node.js; they were later rewritten in Python to strictly conform to challenge specifications.
- **Stale calculations & hardcoded values:** Early frontend prototypes included static count placeholders; the human caught this and directed the implementation of live API statistics.
- **Accidental comment truncation:** During large refactoring passes, some Javadoc comments were dropped; the human identified this in diffs and directed their restoration.

---

## Tools Used

- **AI Assistant:** Google Antigravity AI coding assistant
- **IDE:** Antigravity IDE
- **Runtimes:** Java 24, Python 3.x, Node.js v24.14.1, npm 11.11.0
- **Test Frameworks:** JUnit 5, WireMock, Spring Boot Test
- **Build Tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Database:** H2 in-memory database
