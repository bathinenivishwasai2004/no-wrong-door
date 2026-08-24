# No Wrong Door — Unified Resident View API

A locally-deployable demonstration that reconciles resident records from two
independent mock data sources — a paginated REST Resident Index and an XML
Benefits Register — and exposes a searchable, investigation-ready dashboard.

The system is resilient to intermittent XML source failures (the Surprise
Challenge): the XML adapter retries with exponential backoff, and if all retries
fail the backend remains available with REST data only.


## Prerequisites

| Tool     | Minimum Version | Check Command        |
|----------|-----------------|----------------------|
| Java     | 17+             | `java -version`      |
| Node.js  | 18+             | `node --version`     |
| npm      | 9+              | `npm --version`      |
| Python   | 3.8+            | `python --version`   |

> **Maven is NOT required** — the project includes the Maven Wrapper (`mvnw.cmd`
> on Windows, `mvnw` on macOS/Linux) which auto-downloads Maven on first run.


## Quick Start (from clean clone)

Open **three separate terminals** and run the steps below in order.

### Terminal 1 — Start REST Mock Service

```bash
cd mock-services/rest-source
python rest_service.py --port 8081
```

You should see:
```
[REST Source] Mock service running on http://127.0.0.1:8081
```

### Terminal 2 — Start XML Mock Service

```bash
cd mock-services/xml-source
python xml_service.py --port 8082
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

Open `frontend/index.html` in your browser (double-click, or use File -> Open).
Alternatively, serve it with:

```bash
cd frontend
npx -y serve .
```

Then navigate to the URL shown (typically `http://localhost:3000`).


## How the Dashboard Works

### Automatic Ingestion

There is **no manual "Ingest Data" button**.

When the dashboard opens with an empty database:

1. The browser calls `GET /api/status` and detects `totalResidents = 0` with
   `lastIngestionStatus = null`.
2. It immediately shows:
   > *Reconciling REST and XML records...*
3. It fires exactly **one** `POST /api/ingest` call.
4. On success it shows:
   > *820 residents loaded from 1,160 source records.*
5. The stat cards update from the live API response.

If the database already has data (after a previous run), the dashboard skips
ingestion and shows the existing stats directly.

### Results Section — Search Required

After ingestion the Results section shows **"No search yet"** — it does **not**
automatically display all 820 residents.

To see residents, type a query and click **Search** (or press Enter).

Supported search fields:

- First name, last name, or full name (case-insensitive)
- REST ID (e.g. `R-10001`)
- XML reference (e.g. `CA/2016/4001` or partial prefix `CA/2016`)
- City / Town
- Address line

### Resident Investigation (Detail View)

Click any result card to open the **Resident Investigation** panel. It shows:

- Both source representations side-by-side (REST and XML)
- Match status and confidence score
- Field-level comparison labels (MATCH, NORMALIZED MATCH, DIFFERENT, MISSING,
  NOT AVAILABLE)
- Evidence explaining the matching decision
- Candidate XML references for AMBIGUOUS records
- An explicit **MANUAL REVIEW REQUIRED** warning for AMBIGUOUS matches
- A clear **NOT AVAILABLE** indicator for source-only records

### Status Filter

Select a match status from the filter dropdown before or after entering a query
to restrict results to records of that type. The filter never triggers a
full-database scan on its own — a search term is always required.

### Clear Search

Clicking **Clear Search** resets the Results section to "No search yet". It does
**not** show all 820 residents.


## Verify It's Working

| Check                          | How                                            | Expected                    |
|--------------------------------|------------------------------------------------|-----------------------------|
| Backend health                 | `curl http://localhost:8080/health`             | `{"status":"UP",...}`       |
| REST source reachable          | `curl http://localhost:8080/api/status/rest`    | `"status":"UP"`             |
| XML source reachable           | `curl http://localhost:8080/api/status/xml`     | `"status":"UP"`             |
| REST mock directly (optional)  | `curl http://127.0.0.1:8081/health`            | `{"status":"UP",...}`       |
| XML mock directly (optional)   | `curl http://127.0.0.1:8082/health`            | `<status>UP</status>`       |
| Dashboard status indicators    | Open `frontend/index.html` in browser          | Both dots green             |
| Search                         | Type a name or ID, click Search               | Matching resident cards     |


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

The suite currently contains **54 tests** covering:

- REST adapter pagination, deduplication, and field parsing (unit + integration)
- XML adapter retry/backoff, field parsing, and health check (unit + integration)
- Matching logic: EXACT, PROBABLE, AMBIGUOUS, REST_ONLY, XML_ONLY
- Ingestion: full run, partial failures, repeated-run idempotency
- API controllers: search, detail, status, ingestion endpoint
- Repository search queries


## Project Structure

```
no-wrong-door/
+-- backend/                          # Spring Boot API (Java 17, Maven)
|   +-- src/main/java/com/nowrongdoor/
|   |   +-- adapters/rest/            # REST source adapter (isolated)
|   |   +-- adapters/xml/             # XML source adapter with retry
|   |   +-- api/                      # Controllers (health, status, search, detail)
|   |   +-- ingestion/                # IngestionService + audit runs
|   |   +-- matching/                 # MatchingService + normalizers
|   |   +-- model/                    # UnifiedResident, IngestionRun, MatchStatus
|   |   +-- config/                   # CORS, RestTemplate
|   +-- src/main/resources/
|   |   +-- application.yml           # Mock service URLs (no hardcoding)
|   +-- src/test/java/                # Integration + unit tests (54 total)
|   +-- pom.xml
|   +-- mvnw / mvnw.cmd              # Maven Wrapper (no global Maven needed)
+-- frontend/                         # Static dashboard (no build step)
|   +-- index.html
|   +-- style.css
|   +-- app.js
+-- mock-services/
|   +-- rest-source/                  # Paginated REST (port 8081)
|   +-- xml-source/                   # Slow XML with retryable 500s (port 8082)
+-- README.md
+-- DECISIONS.md
+-- AI-USAGE.md
```


## Ports

| Service           | Port  | Direction               |
|-------------------|-------|-------------------------|
| REST mock service | 8081  | Backend -> REST mock    |
| XML mock service  | 8082  | Backend -> XML mock     |
| Backend API       | 8080  | Browser -> Backend only |
| Frontend (serve)  | 3000* | Local only              |

*When using `npx serve`; or just open the HTML file directly.

> The browser **only** communicates with the backend on port 8080.
> It never contacts the mock services directly.


## API Reference

| Method | Path                        | Description                        |
|--------|-----------------------------|------------------------------------|
| GET    | `/health`                   | Backend liveness check             |
| GET    | `/api/status`               | Dashboard statistics + last run    |
| GET    | `/api/status/rest`          | REST source connectivity           |
| GET    | `/api/status/xml`           | XML source connectivity            |
| POST   | `/api/ingest`               | Run ingestion (synchronous)        |
| GET    | `/api/ingest/status`        | Latest ingestion run summary       |
| GET    | `/api/residents/search?q=`  | Search residents (name/ID/address) |
| GET    | `/api/residents/{id}`       | Resident detail (REST ID, XML ref, or DB id) |

### Example cURL calls

```bash
# Status
curl http://localhost:8080/api/status

# Ingest
curl -X POST http://localhost:8080/api/ingest

# Search by name
curl "http://localhost:8080/api/residents/search?q=Kessler"

# Search by partial XML ref
curl "http://localhost:8080/api/residents/search?q=CA/2016"

# Filter to AMBIGUOUS only
curl "http://localhost:8080/api/residents/search?q=&status=AMBIGUOUS"

# Resident detail
curl http://localhost:8080/api/residents/R-10001

# H2 Console — JDBC URL: jdbc:h2:mem:nwddb
# http://localhost:8080/h2-console
```


## Expected Official Dataset Result

| Measure           | Count |
|:------------------|------:|
| REST records      |   620 |
| XML records       |   540 |
| Source total      | 1,160 |
| Unified residents |   820 |
| EXACT             |   306 |
| PROBABLE          |    34 |
| AMBIGUOUS         |     7 |
| REST_ONLY         |   273 |
| XML_ONLY          |   200 |

These values are validation expectations produced by the official mock datasets
and matching algorithm. They are **not** hardcoded production constants — the UI
reads all values from the live API response.

Source total (1,160) is derived from `restRecords + xmlRecords` in the
`/api/ingest` response, not reconstructed from the match-category counts.


## Surprise Challenge — Unreliable XML Source

The XML Benefits Register is **intentionally unreliable**.

When configured with `--failure-rate 0.40`, it returns HTTP 500 for approximately
40% of `/records` requests, and adds 0.7–2.4 seconds of latency on every call.

### What the application does

The XML adapter retries up to **3 times** with exponential backoff (starting at
500 ms). Each attempt is logged.

- **If a retry succeeds:** ingestion continues normally and completes as `SUCCESS`.
- **If all 3 retries fail:** the backend remains available; the ingestion result
  is `PARTIAL`; REST data is fully preserved; the error is reported in the
  ingestion response.

The backend never crashes and the REST source is unaffected by XML failures.

### Running the Surprise Challenge

Start the XML mock with the 40% failure rate:

```powershell
python mock-services\xml-source\xml_service.py --port 8082 --failure-rate 0.40
```

Open the dashboard — automatic ingestion will fire. Depending on which attempts
fail, you may see:

```
820 residents loaded from 1,160 source records.
```

(full success after retry) or:

```
Data loaded with source limitations. (620 REST + 0 XML records) -- XML source failed after 3 attempts
```

(partial — XML exhausted after 3 retries).

### Testing XML total failure

Stop the XML service entirely (Ctrl+C), then trigger ingestion:

```bash
curl -X POST http://localhost:8080/api/ingest
```

Response:

```json
{
  "status": "PARTIAL",
  "restRecords": 620,
  "xmlRecords": 0,
  "restOnly": 620,
  "error": "XML source failed after 3 attempts"
}
```

Restart XML and the next ingestion run will succeed normally:

```bash
python mock-services\xml-source\xml_service.py --port 8082
curl -X POST http://localhost:8080/api/ingest
```


## Match Status Meanings

| Status      | Meaning                                                      |
|-------------|--------------------------------------------------------------|
| `EXACT`     | Name and DOB match exactly across both sources               |
| `PROBABLE`  | Name matches; XML DOB missing; address and city agree        |
| `AMBIGUOUS` | Name matches but evidence is conflicting — manual review     |
| `REST_ONLY` | Resident found only in the REST Resident Index               |
| `XML_ONLY`  | Resident found only in the XML Benefits Register             |


## Final Verification

With both official services running, execute:

```
backend\mvnw.cmd clean test
```

The final suite contains **54 tests**. No authentication, external AI, message
broker, production database, or cloud deployment is part of this local demo.
