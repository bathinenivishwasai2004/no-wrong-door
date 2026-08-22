# No Wrong Door — Unified Resident View API

A single API that provides a unified view of residents across multiple data
sources. This is **Phase 0** — the foundation skeleton with both mock services,
isolated adapters, and a dashboard that proves end-to-end wiring.

---

## Prerequisites

| Tool     | Minimum Version | Check Command        |
|----------|-----------------|----------------------|
| Java     | 17+             | `java -version`      |
| Node.js  | 18+             | `node --version`     |
| npm      | 9+              | `npm --version`      |

> **Maven is NOT required** — the project includes the Maven Wrapper (`mvnw.cmd`
> on Windows, `mvnw` on macOS/Linux) which auto-downloads Maven on first run.

---

## Quick Start (from clean clone)

Open **three separate terminals** and run the steps below in order.

### Terminal 1 — Start REST Mock Service

```bash
cd mock-services/rest-source
npm install
npm start
```

You should see:
```
[REST Source] Mock service running on http://localhost:3001
```

### Terminal 2 — Start XML Mock Service

```bash
cd mock-services/xml-source
npm install
npm start
```

You should see:
```
[XML Source] Mock service running on http://localhost:3002
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

---

## Verify It's Working

| Check                          | How                                            | Expected                    |
|--------------------------------|------------------------------------------------|-----------------------------|
| Backend health                 | `curl http://localhost:8080/health`             | `{"status":"UP",...}`       |
| REST source reachable          | `curl http://localhost:8080/api/status/rest`    | `"status":"UP"`             |
| XML source reachable           | `curl http://localhost:8080/api/status/xml`     | `"status":"UP"`             |
| REST mock directly (optional)  | `curl http://localhost:3001/health`             | `{"status":"UP",...}`       |
| XML mock directly (optional)   | `curl http://localhost:3002/health`             | `<status>UP</status>`       |
| Dashboard status indicators    | Open `frontend/index.html` in browser          | Both dots green             |
| Search stub                    | Type any name, click Search                    | 3 stub results appear       |

---

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

---

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
│   ├── rest-source/                  # Paginated REST (port 3001)
│   └── xml-source/                   # Slow XML with 500s (port 3002)
├── README.md                         # ← You are here
├── DECISIONS.md                      # Architectural decisions
└── AI-USAGE.md                       # AI generation disclosure
```

---

## Ports

| Service           | Port  |
|-------------------|-------|
| REST mock service | 3001  |
| XML mock service  | 3002  |
| Backend API       | 8080  |
| Frontend (serve)  | 3000* |

*When using `npx serve`; or just open the HTML file directly.

---

## Phase Roadmap

- **Phase 0** ✅ (complete): Foundation — mock services, adapters, dashboard skeleton
- **Phase 1** ✅ (complete): REST pagination walking, deduplication, real search endpoint
- **Phase 2**: XML source integration, graceful degradation, unified resident view
- **Phase 3**: Identity matching across REST and XML sources

---

## Phase 1 — REST Integration Demo

Phase 1 replaces the Phase 0 stub with real data from the paginated REST source.

### What changed

- `RestSourceAdapter.fetchAllResidents()` walks all pages and deduplicates by `id`.
- `GET /api/residents/search?query=...` now returns real REST source data.
- Each result card includes `phone`, `address`, and `source: "rest-source"`.
- Results header shows `N results (from M unique REST records)`.

### Quick demo

```bash
# Search for a resident by name
curl "http://localhost:8080/api/residents/search?query=Garcia"

# Search with no results
curl "http://localhost:8080/api/residents/search?query=Zyx"

# Empty query returns empty list
curl "http://localhost:8080/api/residents/search?query="

# Kill the REST mock, then:
curl "http://localhost:8080/api/residents/search?query=Garcia"
# → HTTP 502 with {"error":"REST source is unreachable: ..."}
```

### Java version

Phase 1 uses Java 21 (sealed interface pattern matching in switch). The runtime
is Java 24 (backward-compatible).
