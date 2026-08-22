# Architectural Decisions — Phase 0

This document records key design decisions made during the Phase 0 foundation
build. Each entry explains the decision, context, and rationale.

---

## ADR-001: Adapters are fully isolated — no shared aggregation layer

**Decision:** The `RestSourceAdapter` and `XmlSourceAdapter` live in completely
separate packages (`adapters.rest` and `adapters.xml`) with zero cross-dependencies.
There is no shared interface, no common base class, and no aggregation service
that calls both.

**Context:** The challenge description hints that "day 2 introduces an
unannounced requirement change." The most likely change involves how sources
are consumed, prioritized, or merged.

**Rationale:**
- If the sources are fully decoupled, a requirement change to one source
  (e.g., "the XML service changes its schema") cannot cascade into the other.
- An aggregation/merging layer will be introduced in Phase 1 when the actual
  requirements for unification are known — building it speculatively now
  would risk building the wrong abstraction.
- Each adapter can be independently tested, replaced, or extended.

**Trade-off:** Slight code duplication (both adapters use `RestTemplate`, both
have a `checkHealth()` method) — this is acceptable and preferable to premature
abstraction.

---

## ADR-002: No deduplication, retry, timeout, or caching in Phase 0

**Decision:** None of these resilience/correctness mechanisms are implemented.

**Context:** The REST mock produces duplicate records across pages. The XML mock
is slow and intermittently returns 500 errors.

**Rationale:**
- Phase 0 is purely a wiring proof. Adding retry/dedup now would obscure the
  fundamental question: "can the backend reach both services?"
- These are Phase 1 concerns with specific design choices (exponential backoff
  vs. fixed retry? deduplicate by ID vs. by content hash?) that should be made
  when the full requirements are visible.

**What this means:** Tests may occasionally flake due to the XML mock's
simulated failures. This is expected and acceptable in Phase 0.

---

## ADR-003: Frontend served as static files, not bundled into Spring Boot

**Decision:** The frontend (`index.html`, `style.css`, `app.js`) lives in
`/frontend/` and is opened directly in the browser or served via `npx serve`.
It is NOT embedded in Spring Boot's `src/main/resources/static/`.

**Rationale:**
- The challenge specifies "plain HTML/CSS/JavaScript, no framework, no build
  step" — serving from Spring Boot's classpath would conflate the frontend
  and backend deployment concerns.
- The CORS configuration on the backend explicitly allows cross-origin requests
  from any origin (acceptable in dev; must be locked down in production).
- This keeps the frontend trivially editable — edit a file, refresh the browser.

**Trade-off:** Requires CORS headers on the backend. In production, the
frontend would sit behind a reverse proxy or CDN, and CORS policy would
be tightened.

---

## ADR-004: Maven Wrapper instead of requiring global Maven installation

**Decision:** The project includes `mvnw` / `mvnw.cmd` scripts that
automatically download Maven 3.9.9 on first use.

**Context:** The development machine did not have Maven installed globally.

**Rationale:**
- Eliminates "works on my machine" issues — any developer with Java 17+
  can build the project without installing Maven separately.
- Standard practice for Spring Boot projects.
- The wrapper is committed to version control; the downloaded Maven
  distribution is cached in `~/.m2/wrapper/`.

---

## ADR-005: Frontend calls backend only — never mock services directly

**Decision:** `app.js` is hardcoded to call `http://localhost:8080` (our
backend). It never calls `localhost:3001` or `localhost:3002`.

**Rationale:**
- The backend is the single gateway to all data sources. This boundary
  exists for a reason: in Phase 1+, the backend will handle retries,
  caching, rate limiting, and data transformation.
- If the frontend could bypass the backend, any resilience or aggregation
  logic would be circumventable.
- The status indicators call `/api/status/rest` and `/api/status/xml`
  (backend endpoints) which then check mock connectivity — the frontend
  never checks mock health directly.

---

## ADR-006: Application configuration via application.yml — no hardcoded URLs

**Decision:** Mock service URLs are configured in `application.yml` under
`mock-services.rest.base-url` and `mock-services.xml.base-url`. Adapter
classes receive these via `@ConfigurationProperties`.

**Rationale:**
- Enables environment-specific configuration (dev vs. staging vs. production)
  without code changes.
- If mock service ports change, only one file needs updating.
- Standard Spring Boot practice.

---

## ADR-007: Stub search endpoint for frontend state verification

**Decision:** `GET /api/residents/search?query=...` returns hardcoded stub
data in Phase 0 rather than calling the adapters.

**Rationale:**
- The purpose of Phase 0 search is to prove the frontend can render all
  states: empty, loading, results, and error.
- Real search requires decisions about which adapter(s) to call, how to
  merge results, and how to handle partial failures — all Phase 1 work.
- The stub response includes a `"source": "stub"` field so it's obvious
  the data isn't real.

---

## ADR-008: Pagination terminates using server-supplied `totalPages` — Phase 1

**Decision:** The REST adapter reads `totalPages` from the first page response
and loops from `page=1` through `page=totalPages`. The page count is never
hard-coded.

**Context:** The mock REST service returns a JSON envelope containing
`{ page, size, totalPages, totalRecords, data }`. This gives the adapter a
reliable, server-authoritative count of pages.

**Rationale:**
- Hard-coding page count (e.g., always fetch 2 pages) would silently miss
  records if the source adds more data or the page size changes.
- A "stop when fewer records than page size" heuristic would fail if a
  page happens to be exactly full by coincidence.
- Using `totalPages` directly is the simplest, most correct approach given
  that the server provides it.

**Trade-off:** If the first page is itself unreachable, we cannot determine
`totalPages` and return a `Failure` immediately. This is correct behaviour —
we cannot make safe assumptions about pagination state we have never seen.

---

## ADR-009: Deduplication by stable `id` field — Phase 1

**Decision:** Cross-page duplicate records are deduplicated using the `id`
field (e.g., `"R001"`) as the unique key. Records are accumulated into a
`LinkedHashMap<String, RestResident>` keyed by `id`; first-seen wins.

**Context:** The mock REST service intentionally injects duplicate records
across page boundaries (30% probability, 1–2 dupes per page). The `id`
field is assigned by the source system and is stable across pages.

**Rationale:**
- `id` is the only stable, server-assigned identifier in the response.
  Deduplicating by full field equality (all fields equal) would fail for
  records that have the same ID but differ in a mutable field like `address`.
- "First-seen wins" preserves the earliest page's version of a resident,
  which is the most natural behaviour and produces deterministic output for
  a given source state.
- `LinkedHashMap` preserves insertion order, making results deterministic
  without a separate sort step.

**What this means for identity matching (Phase 2+):** The REST `id` field
is opaque to the XML source. Matching a REST resident to an XML case record
requires a separate identity-matching strategy — that is deferred to Phase 2.

---

## ADR-010: `RestFetchResult` sealed interface — adapter never throws — Phase 1

**Decision:** `RestSourceAdapter.fetchAllResidents()` returns a sealed
`RestFetchResult` (`Success` | `Failure`) and never propagates exceptions to
callers.

**Rationale:**
- The controller layer should decide the HTTP response, not the adapter.
  If the adapter threw, the controller would need try/catch for every call,
  coupling error-handling logic to the adapter's exception hierarchy.
- A `Failure` result can carry enough context (message + cause) for the
  controller to return a meaningful HTTP 502 to the frontend.
- Phase 2 graceful-degradation logic needs to inspect "did source X fail?"
  without catching exceptions — the sealed result type makes this natural:
  `if (restResult instanceof Failure) { /* fallback to XML */ }`.

**Trade-off:** Callers must pattern-match on the result type rather than
relying on try/catch. This is an intentional forcing function toward
explicit error handling at the API boundary.

