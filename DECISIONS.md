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
