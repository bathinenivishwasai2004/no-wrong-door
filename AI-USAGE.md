# AI Usage Disclosure — Phase 0

This document describes how AI tools were used during Phase 0 development,
per the challenge's AI usage policy.

---

## Summary

All code in Phase 0 was **AI-generated** using an AI coding assistant
(Google Antigravity / Claude), with human review and direction at each step.

---

## What was AI-generated

| Component                       | AI Generated | Human Reviewed |
|---------------------------------|:---:|:---:|
| Mock services (rest-source)     | ✅  | ✅  |
| Mock services (xml-source)      | ✅  | ✅  |
| Spring Boot backend scaffold    | ✅  | ✅  |
| pom.xml / Maven config          | ✅  | ✅  |
| application.yml                 | ✅  | ✅  |
| RestSourceAdapter               | ✅  | ✅  |
| XmlSourceAdapter                | ✅  | ✅  |
| API Controllers                 | ✅  | ✅  |
| WebConfig (CORS)                | ✅  | ✅  |
| Frontend (HTML/CSS/JS)          | ✅  | ✅  |
| Integration tests               | ✅  | ✅  |
| README.md                       | ✅  | ✅  |
| DECISIONS.md                    | ✅  | ✅  |
| This file (AI-USAGE.md)         | ✅  | ✅  |

---

## How AI was used

1. **Requirements analysis:** The AI read the full challenge prompt and
   identified tasks, constraints, and the definition of done.

2. **Architecture & planning:** The AI proposed the project structure,
   adapter isolation pattern, and technology choices — then presented
   an implementation plan for human approval before writing any code.

3. **Code generation:** After plan approval, the AI generated all source
   files. Each file was created with intent commentary explaining design
   decisions and Phase 0 scope limitations.

4. **Testing:** The AI wrote integration tests and verified they pass
   against the running mock services.

5. **Documentation:** The AI wrote README.md (setup guide), DECISIONS.md
   (architectural rationale), and this file.

---

## What was NOT AI-generated

- The challenge requirements and constraints (provided by the hiring team)
- The decision to approve the implementation plan (human judgment)
- Any manual configuration or environment-specific adjustments

---

## AI limitations observed

- The AI assumed mock services were pre-existing based on the prompt but
  they were not present on disk — flagged this to the human who confirmed
  they should be created.
- Maven was not installed on the development machine — the AI adapted by
  using the Maven Wrapper pattern instead.

---

## Tools used

- **AI Assistant:** Google Antigravity (Claude-based)
- **IDE:** Antigravity IDE
- **Runtime:** Java 24, Node.js v24.14.1, npm 11.11.0
