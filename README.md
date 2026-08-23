# No Wrong Door — Unified Resident View API

A final-hardened local demonstration that provides a unified view of residents
across a paginated REST Resident Index and an XML Benefits Register. The
dashboard supports search, source comparison, match evidence, and manual
review of ambiguous records.


## Prerequisites

| Tool     | Minimum Version | Check Command        |
|----------|-----------------|----------------------|
| Java     | 17+             | `java -version`      |
| Node.js  | 18+             | `node --version`     |
| npm      | 9+              | `npm --version`      |

> **Maven is NOT required** — the project includes the Maven Wrapper (`mvnw.cmd`
> on Windows, `mvnw` on macOS/Linux) which auto-downloads Maven on first run.


## Quick Start (from clean clone)

Open **three separate terminals** and run the steps below in order.

### Terminal 1 — Start REST Mock Service

```bash
cd mock-services/rest-source
npm install
python rest_service.py
```

You should see:
```
[REST Source] Mock service running on http://127.0.0.1:8081
```

### Terminal 2 — Start XML Mock Service

```bash
cd mock-services/xml-source
npm install
python xml_service.py
```

You should see:
```
[XML Source] Mock service running on http://127.0.0.1:8082
```

### Terminal 3 — Start Backend

```bash
cd backend

# On Windows:
mvnw.cmd spring-boot:run

# On macOS/Linux:
./mvnw spring-boot:run
```

First run will download Maven and all dependencies (~2-5 minutes).
You should see:
```
Started NoWrongDoorApplication in X.XXX seconds
```

### Open the Dashboard

Open `frontend/index.html` in your browser (just double-click, or use
File → Open). Alternatively, serve it with:

```bash
cd frontend
npx -y serve .
```

Then navigate to the URL shown (typically `http://localhost:3000`).


## Verify It's Working

| Check                          | How                                            | Expected                    |
|--------------------------------|------------------------------------------------|-----------------------------|
| Backend health                 | `curl http://localhost:8080/health`             | `{"status":"UP",...}`       |
| REST source reachable          | `curl http://localhost:8080/api/status/rest`    | `"status":"UP"`             |
| XML source reachable           | `curl http://localhost:8080/api/status/xml`     | `"status":"UP"`             |
| REST mock directly (optional)  | `curl http://127.0.0.1:8081/health`            | `{"status":"UP",...}`       |
| XML mock directly (optional)   | `curl http://127.0.0.1:8082/health`            | `<status>UP</status>`       |
| Dashboard status indicators    | Open `frontend/index.html` in browser          | Both dots green             |
| Search                         | Type a name, click Search                     | Matching resident cards     |


## Running Tests

Both mock services **must be running** before tests are executed (they are
integration tests that hit the real mocks).

```bash
cd backend

# On Windows:
mvnw.cmd test

# On macOS/Linux:
./mvnw test
```


## Project Structure

```
no-wrong-door/
├── backend/                          # Spring Boot API (Java 17, Maven)
│   ├── src/main/java/com/nowrongdoor/
│   │   ├── adapters/rest/            # REST source adapter (isolated)
│   │   ├── adapters/xml/             # XML source adapter (isolated)
│   │   ├── api/                      # Controllers (health, status, search)
│   │   └── config/                   # CORS, RestTemplate
│   ├── src/main/resources/
│   │   └── application.yml           # Mock service URLs (no hardcoding)
│   ├── src/test/java/                # Integration tests
│   ├── pom.xml
│   └── mvnw / mvnw.cmd              # Maven Wrapper (no global Maven needed)
├── frontend/                         # Static dashboard (no build step)
│   ├── index.html
│   ├── style.css
│   └── app.js
├── mock-services/
│   ├── rest-source/                  # Paginated REST (port 8081)
│   └── xml-source/                   # Slow XML with 500s (port 8082)
├── README.md                         # ← You are here
├── DECISIONS.md                      # Architectural decisions
└── AI-USAGE.md                       # AI generation disclosure
```


## Ports

| Service           | Port  |
|-------------------|-------|
| REST mock service | 8081  |
| XML mock service  | 8082  |
| Backend API       | 8080  |
| Frontend (serve)  | 3000* |

*When using `npx serve`; or just open the HTML file directly.


## Phase Roadmap

- **Phases 1-4** (complete): source adapters, matching, ingestion, APIs, and investigation UI
- **Phase 5** (complete): verification, resilience checks, accessibility review, and demo readiness

## Phase 1 — REST Integration Demo

The unified API returns real data from the paginated REST and XML sources.

### What changed


### Quick demo

```bash
# Search for a resident by name
curl "http://localhost:8080/api/residents/search?q=Garcia"

# Search with no results
curl "http://localhost:8080/api/residents/search?q=Zyx"

# Empty query returns empty list
curl "http://localhost:8080/api/residents/search?q="

# Kill the REST mock, then:
curl "http://localhost:8080/api/residents/search?q=Garcia"
# → HTTP 502 with {"error":"REST source is unreachable: ..."}
```

### Java version

Phase 1 uses Java 21 (sealed interface pattern matching in switch). The runtime
is Java 24 (backward-compatible).

## Phase 2C - Ingestion Pipeline

Start the official services and backend in three terminals:

```bash
cd mock-services/rest-source
python rest_service.py
```

```bash
cd mock-services/xml-source
python xml_service.py
```

The REST service listens on `http://127.0.0.1:8081` and the XML service on
`http://127.0.0.1:8082`. Start the backend from `backend` with `mvnw.cmd
spring-boot:run` on Windows or `./mvnw spring-boot:run` on macOS/Linux.

Trigger one synchronous ingestion run:

```bash
curl -X POST http://127.0.0.1:8080/api/ingest
```

The response contains the run ID, source counts, match counts, status,
duration, and any warning or failure message. A successful run normally reports
620 REST records and 540 XML records. If XML fails after retries, the run is
`PARTIAL` and successful REST records are persisted as `REST_ONLY`.

Inspect the latest audit run with:

```bash
curl http://127.0.0.1:8080/api/ingest/status
```

Repeated runs update rows using REST `id` and XML `Ref` rather than creating
uncontrolled duplicates. The H2 console is available at
`http://127.0.0.1:8080/h2-console` with JDBC URL `jdbc:h2:mem:nwddb`.

## Phase 4 - Resident Investigation

Search results open an investigation view without a page reload. The view
shows the REST Resident Index and XML Benefits Register side by side, keeps
each source's raw values visible, and explains the matching decision with
field-level comparison labels:

	evidence agrees.

Match status meanings are `EXACT` (verified), `PROBABLE` (credible but limited
evidence), `AMBIGUOUS` (manual review required), `REST_ONLY`, and `XML_ONLY`.
Source-only investigations show an explicit missing-source message and never
invent values. The detail API is available by REST ID, XML reference, or the
database record ID:

```bash
curl http://127.0.0.1:8080/api/residents/R-10001
```

The response includes `rest`, `xml`, `matchStatus`, `matchConfidence`,
`matchNotes`, `sourceAvailability`, and backend-provided `evidence`.

## Final Demo Workflow

1. Start both official services and the backend.
2. Open the dashboard and confirm source health is UP.
3. Click **Ingest Data** and wait for the result.
4. Show overview totals, filters, and a resident search.
5. Open an investigation to compare raw REST/XML values, confidence,
	evidence, and discrepancies.
6. Show an `AMBIGUOUS` record and its **MANUAL REVIEW REQUIRED** message.
7. Show `REST_ONLY` and `XML_ONLY` records with the missing source marked
	`NOT AVAILABLE`.

## Expected Official Dataset Result

| Measure | Count |
|---|---:|
| REST records | 620 |
| XML records | 540 |
| Unified residents | 820 |
| EXACT | 306 |
| PROBABLE | 34 |
| AMBIGUOUS | 7 |
| REST_ONLY | 273 |
| XML_ONLY | 200 |

These values are validation expectations, not production constants.

## Resilience And Scope

The XML mock intentionally adds 0.7 to 2.4 seconds of latency and returns HTTP
500 for approximately 15% of requests. The XML adapter retries with backoff.
If all attempts fail, ingestion remains available for REST and reports
`PARTIAL`; successful records are still persisted. Stopping either mock reports
the corresponding source as `DOWN`, while the backend remains responsive.

The browser calls only the backend on port 8080 and never reads mock datasets
directly. The official datasets, mock behavior, adapters, matching, and
ingestion architecture are preserved for the demonstration.

## Final Verification

With both official services running, execute:

```text
backend\mvnw.cmd clean test
```

The final suite contains 52 tests. No authentication, external AI, message
broker, production database, or cloud deployment is part of this local demo.
