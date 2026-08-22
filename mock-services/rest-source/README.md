# REST Source — Mock Service

Paginated REST endpoint that returns resident records in JSON format.

## Behavior

- `GET /residents?page=N&size=M` — paginated list (default: page=1, size=10)
- `GET /residents/:id` — single resident by ID
- `GET /health` — service health check
- ~30% of paginated responses include 1-2 **duplicate** records from other pages

## Running

```bash
npm install
npm start
```

Runs on **port 3001** by default (override with `PORT` env var).

## Seed Data

20 hardcoded resident records with fields: `id`, `firstName`, `lastName`,
`dateOfBirth`, `address`, `phone`.
