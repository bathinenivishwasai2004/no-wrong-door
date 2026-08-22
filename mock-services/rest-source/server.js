const express = require('express');
const app = express();
const PORT = process.env.PORT || 3001;

/* ───────────────────────────────────────────────
   Seed data — 20 resident records
   ─────────────────────────────────────────────── */
const residents = [
  { id: "R001", firstName: "Maria",    lastName: "Garcia",     dateOfBirth: "1985-03-14", address: "742 Evergreen Terrace, Springfield, IL 62704",  phone: "(217) 555-0101" },
  { id: "R002", firstName: "James",    lastName: "Johnson",    dateOfBirth: "1990-07-22", address: "1600 Pennsylvania Ave NW, Washington, DC 20500", phone: "(202) 555-0102" },
  { id: "R003", firstName: "Aisha",    lastName: "Patel",      dateOfBirth: "1978-11-05", address: "350 Fifth Avenue, New York, NY 10118",            phone: "(212) 555-0103" },
  { id: "R004", firstName: "Robert",   lastName: "Williams",   dateOfBirth: "1962-01-30", address: "221B Baker Street, London, KY 40741",             phone: "(606) 555-0104" },
  { id: "R005", firstName: "Chen",     lastName: "Wei",        dateOfBirth: "1995-09-18", address: "1 Infinite Loop, Cupertino, CA 95014",            phone: "(408) 555-0105" },
  { id: "R006", firstName: "Sarah",    lastName: "O'Brien",    dateOfBirth: "1988-04-12", address: "4059 Mt Lee Dr, Los Angeles, CA 90068",           phone: "(323) 555-0106" },
  { id: "R007", firstName: "David",    lastName: "Kim",        dateOfBirth: "1973-06-25", address: "1060 W Addison St, Chicago, IL 60613",            phone: "(773) 555-0107" },
  { id: "R008", firstName: "Fatima",   lastName: "Al-Rashid",  dateOfBirth: "1991-12-08", address: "233 S Wacker Dr, Chicago, IL 60606",              phone: "(312) 555-0108" },
  { id: "R009", firstName: "Thomas",   lastName: "Anderson",   dateOfBirth: "1982-02-19", address: "1 Main St, Smalltown, OH 44256",                  phone: "(330) 555-0109" },
  { id: "R010", firstName: "Priya",    lastName: "Sharma",     dateOfBirth: "1996-08-03", address: "500 Oracle Pkwy, Redwood City, CA 94065",         phone: "(650) 555-0110" },
  { id: "R011", firstName: "Michael",  lastName: "Brown",      dateOfBirth: "1969-10-17", address: "1 Hacker Way, Menlo Park, CA 94025",              phone: "(650) 555-0111" },
  { id: "R012", firstName: "Elena",    lastName: "Rodriguez",  dateOfBirth: "1984-05-29", address: "2300 Traverwood Dr, Ann Arbor, MI 48105",         phone: "(734) 555-0112" },
  { id: "R013", firstName: "William",  lastName: "Davis",      dateOfBirth: "1957-03-11", address: "1600 Amphitheatre Pkwy, Mountain View, CA 94043", phone: "(650) 555-0113" },
  { id: "R014", firstName: "Yuki",     lastName: "Tanaka",     dateOfBirth: "1993-07-04", address: "410 Terry Ave N, Seattle, WA 98109",              phone: "(206) 555-0114" },
  { id: "R015", firstName: "Grace",    lastName: "Okonkwo",    dateOfBirth: "1987-11-21", address: "1 Microsoft Way, Redmond, WA 98052",              phone: "(425) 555-0115" },
  { id: "R016", firstName: "Ahmed",    lastName: "Hassan",     dateOfBirth: "1976-09-06", address: "79 Anson Rd, Singapore, 079906",                  phone: "(555) 555-0116" },
  { id: "R017", firstName: "Lisa",     lastName: "Thompson",   dateOfBirth: "1999-01-15", address: "151 3rd St, San Francisco, CA 94103",             phone: "(415) 555-0117" },
  { id: "R018", firstName: "Carlos",   lastName: "Mendez",     dateOfBirth: "1981-04-27", address: "1 Apple Park Way, Cupertino, CA 95014",           phone: "(408) 555-0118" },
  { id: "R019", firstName: "Anna",     lastName: "Kowalski",   dateOfBirth: "1992-12-03", address: "3500 Deer Creek Rd, Palo Alto, CA 94304",         phone: "(650) 555-0119" },
  { id: "R020", firstName: "Marcus",   lastName: "Lee",        dateOfBirth: "1965-08-14", address: "1355 Market St, San Francisco, CA 94103",         phone: "(415) 555-0120" }
];

/* ───────────────────────────────────────────────
   GET /residents?page=N&size=M
   Paginated with occasional cross-page duplicates
   ─────────────────────────────────────────────── */
app.get('/residents', (req, res) => {
  const page = Math.max(1, parseInt(req.query.page) || 1);
  const size = Math.min(50, Math.max(1, parseInt(req.query.size) || 10));

  const totalRecords = residents.length;
  const totalPages = Math.ceil(totalRecords / size);
  const startIndex = (page - 1) * size;

  // Build this page's results
  let pageData = residents.slice(startIndex, startIndex + size);

  // ~30% chance: inject 1-2 duplicate records from adjacent pages
  if (Math.random() < 0.3 && totalPages > 1) {
    const dupeCount = Math.random() < 0.5 ? 1 : 2;
    for (let i = 0; i < dupeCount; i++) {
      // Pick a random resident NOT on this page (simulates cross-page duplicate)
      const otherIndices = [];
      for (let j = 0; j < residents.length; j++) {
        if (j < startIndex || j >= startIndex + size) {
          otherIndices.push(j);
        }
      }
      if (otherIndices.length > 0) {
        const dupeIndex = otherIndices[Math.floor(Math.random() * otherIndices.length)];
        pageData.push(residents[dupeIndex]);
      }
    }
  }

  res.json({
    page,
    size,
    totalPages,
    totalRecords,
    data: pageData
  });
});

/* ───────────────────────────────────────────────
   GET /residents/:id — single resident lookup
   ─────────────────────────────────────────────── */
app.get('/residents/:id', (req, res) => {
  const resident = residents.find(r => r.id === req.params.id);
  if (!resident) {
    return res.status(404).json({ error: 'Resident not found' });
  }
  res.json(resident);
});

/* ───────────────────────────────────────────────
   GET /health — service health check
   ─────────────────────────────────────────────── */
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'rest-source', port: PORT });
});

/* ───────────────────────────────────────────────
   Start server
   ─────────────────────────────────────────────── */
app.listen(PORT, () => {
  console.log(`[REST Source] Mock service running on http://localhost:${PORT}`);
  console.log(`[REST Source] Endpoints:`);
  console.log(`  GET /residents?page=1&size=10  — paginated resident list`);
  console.log(`  GET /residents/:id             — single resident lookup`);
  console.log(`  GET /health                    — service health check`);
});
