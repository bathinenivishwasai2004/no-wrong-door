const express = require('express');
const app = express();
const PORT = process.env.PORT || 3002;

/* ───────────────────────────────────────────────
   Seed data — 15 resident records (XML source)
   Some overlap with REST source by name but
   different IDs / slightly different data
   ─────────────────────────────────────────────── */
const residents = [
  { id: "X001", firstName: "Maria",    lastName: "Garcia",     dateOfBirth: "1985-03-14", ssn: "***-**-1234", caseNumber: "CN-2024-0001", program: "SNAP",          status: "Active" },
  { id: "X002", firstName: "James",    lastName: "Johnson",    dateOfBirth: "1990-07-22", ssn: "***-**-5678", caseNumber: "CN-2024-0002", program: "Medicaid",      status: "Active" },
  { id: "X003", firstName: "Robert",   lastName: "Williams",   dateOfBirth: "1962-01-30", ssn: "***-**-9012", caseNumber: "CN-2024-0003", program: "Housing",       status: "Pending" },
  { id: "X004", firstName: "Sarah",    lastName: "O'Brien",    dateOfBirth: "1988-04-12", ssn: "***-**-3456", caseNumber: "CN-2024-0004", program: "TANF",          status: "Active" },
  { id: "X005", firstName: "Chen",     lastName: "Wei",        dateOfBirth: "1995-09-18", ssn: "***-**-7890", caseNumber: "CN-2024-0005", program: "SNAP",          status: "Under Review" },
  { id: "X006", firstName: "Fatima",   lastName: "Al-Rashid",  dateOfBirth: "1991-12-08", ssn: "***-**-2345", caseNumber: "CN-2024-0006", program: "Medicaid",      status: "Active" },
  { id: "X007", firstName: "Thomas",   lastName: "Anderson",   dateOfBirth: "1982-02-19", ssn: "***-**-6789", caseNumber: "CN-2024-0007", program: "SNAP",          status: "Closed" },
  { id: "X008", firstName: "Priya",    lastName: "Sharma",     dateOfBirth: "1996-08-03", ssn: "***-**-0123", caseNumber: "CN-2024-0008", program: "Housing",       status: "Active" },
  { id: "X009", firstName: "Michael",  lastName: "Brown",      dateOfBirth: "1969-10-17", ssn: "***-**-4567", caseNumber: "CN-2024-0009", program: "Medicaid",      status: "Active" },
  { id: "X010", firstName: "Elena",    lastName: "Rodriguez",  dateOfBirth: "1984-05-29", ssn: "***-**-8901", caseNumber: "CN-2024-0010", program: "TANF",          status: "Pending" },
  { id: "X011", firstName: "Grace",    lastName: "Okonkwo",    dateOfBirth: "1987-11-21", ssn: "***-**-2346", caseNumber: "CN-2024-0011", program: "SNAP",          status: "Active" },
  { id: "X012", firstName: "Ahmed",    lastName: "Hassan",     dateOfBirth: "1976-09-06", ssn: "***-**-6780", caseNumber: "CN-2024-0012", program: "Housing",       status: "Under Review" },
  { id: "X013", firstName: "Lisa",     lastName: "Thompson",   dateOfBirth: "1999-01-15", ssn: "***-**-1230", caseNumber: "CN-2024-0013", program: "Medicaid",      status: "Active" },
  { id: "X014", firstName: "Carlos",   lastName: "Mendez",     dateOfBirth: "1981-04-27", ssn: "***-**-5670", caseNumber: "CN-2024-0014", program: "SNAP",          status: "Active" },
  { id: "X015", firstName: "Anna",     lastName: "Kowalski",   dateOfBirth: "1992-12-03", ssn: "***-**-9010", caseNumber: "CN-2024-0015", program: "TANF",          status: "Pending" }
];

/* ───────────────────────────────────────────────
   Helper: convert a resident object to XML
   ─────────────────────────────────────────────── */
function residentToXml(r) {
  // Escape XML special chars in string values
  const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;');
  return `  <resident id="${esc(r.id)}">
    <firstName>${esc(r.firstName)}</firstName>
    <lastName>${esc(r.lastName)}</lastName>
    <dateOfBirth>${esc(r.dateOfBirth)}</dateOfBirth>
    <ssn>${esc(r.ssn)}</ssn>
    <caseNumber>${esc(r.caseNumber)}</caseNumber>
    <program>${esc(r.program)}</program>
    <status>${esc(r.status)}</status>
  </resident>`;
}

/* ───────────────────────────────────────────────
   Artificial delay: 1–3 seconds
   ─────────────────────────────────────────────── */
function randomDelay() {
  return 1000 + Math.floor(Math.random() * 2000);
}

/* ───────────────────────────────────────────────
   Intermittent failure: ~20% chance of 500
   ─────────────────────────────────────────────── */
function shouldFail() {
  return Math.random() < 0.2;
}

/* ───────────────────────────────────────────────
   GET /residents — all residents as XML
   ─────────────────────────────────────────────── */
app.get('/residents', (req, res) => {
  const delay = randomDelay();

  setTimeout(() => {
    if (shouldFail()) {
      console.log(`[XML Source] Simulated 500 error on GET /residents (after ${delay}ms)`);
      return res.status(500).type('application/xml').send(
        `<?xml version="1.0" encoding="UTF-8"?>\n<error>\n  <code>INTERNAL_ERROR</code>\n  <message>Service temporarily unavailable</message>\n</error>`
      );
    }

    const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<residents count="${residents.length}">\n${residents.map(residentToXml).join('\n')}\n</residents>`;
    console.log(`[XML Source] Responded to GET /residents with ${residents.length} records (${delay}ms delay)`);
    res.type('application/xml').send(xml);
  }, delay);
});

/* ───────────────────────────────────────────────
   GET /residents/:id — single resident as XML
   ─────────────────────────────────────────────── */
app.get('/residents/:id', (req, res) => {
  const delay = randomDelay();

  setTimeout(() => {
    if (shouldFail()) {
      console.log(`[XML Source] Simulated 500 error on GET /residents/${req.params.id} (after ${delay}ms)`);
      return res.status(500).type('application/xml').send(
        `<?xml version="1.0" encoding="UTF-8"?>\n<error>\n  <code>INTERNAL_ERROR</code>\n  <message>Service temporarily unavailable</message>\n</error>`
      );
    }

    const resident = residents.find(r => r.id === req.params.id);
    if (!resident) {
      return res.status(404).type('application/xml').send(
        `<?xml version="1.0" encoding="UTF-8"?>\n<error>\n  <code>NOT_FOUND</code>\n  <message>Resident ${req.params.id} not found</message>\n</error>`
      );
    }

    const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<residents count="1">\n${residentToXml(resident)}\n</residents>`;
    console.log(`[XML Source] Responded to GET /residents/${req.params.id} (${delay}ms delay)`);
    res.type('application/xml').send(xml);
  }, delay);
});

/* ───────────────────────────────────────────────
   GET /health — service health check (no delay,
   no failure simulation)
   ─────────────────────────────────────────────── */
app.get('/health', (req, res) => {
  res.type('application/xml').send(
    `<?xml version="1.0" encoding="UTF-8"?>\n<health>\n  <status>UP</status>\n  <service>xml-source</service>\n  <port>${PORT}</port>\n</health>`
  );
});

/* ───────────────────────────────────────────────
   Start server
   ─────────────────────────────────────────────── */
app.listen(PORT, () => {
  console.log(`[XML Source] Mock service running on http://localhost:${PORT}`);
  console.log(`[XML Source] Endpoints:`);
  console.log(`  GET /residents      — all residents (XML, 1-3s delay, ~20% 500 errors)`);
  console.log(`  GET /residents/:id  — single resident (XML, 1-3s delay, ~20% 500 errors)`);
  console.log(`  GET /health         — service health check (no delay, no failures)`);
});
