# XML Source — Mock Service

XML endpoint that returns resident case/program records with simulated
unreliability.

## Behavior

- `GET /residents` — all residents as XML (1–3 second delay, ~20% chance of 500 error)
- `GET /residents/:id` — single resident by ID (same delay/failure behavior)
- `GET /health` — service health check (no delay, no failures)

## Running

```bash
npm install
npm start
```

Runs on **port 3002** by default (override with `PORT` env var).

## Seed Data

15 hardcoded resident records with fields: `id`, `firstName`, `lastName`,
`dateOfBirth`, `ssn` (masked), `caseNumber`, `program`, `status`.

Some residents share names with the REST source but have different IDs and
different data fields — this simulates the real-world scenario where two
systems track the same people independently.
