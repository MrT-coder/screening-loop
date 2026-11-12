# screening-loop

Working prototype for the Future Health Digital Innovation Challenge, **Challenge 05: Closing the Screening-to-Care Loop**.
Primary goal: learning and a solid public portfolio project. Winning is secondary.

## Working rules

- **Contrast every idea against Challenge 05 before developing.** If a proposal drifts from the challenge, say so and get approval before writing code.
- Reply to the owner in Spanish (Rioplatense voseo, short answers, one question at a time). Code, docs, commits and UI copy are in English.
- Conventional commits, no AI attribution. Do not commit unless asked.
- Synthetic data only. Never real patient data.
- Do not claim clinical validation. Screening is not diagnosis; humans stay in the loop.

## Code standards (enforced by tooling)

- Java code has **no comments of any kind** (no `//`, `/* */`, Javadoc). Intent goes in names and structure.
- Everything in code is English: identifiers, strings, log and error messages, test names. ASCII only.
- English naming conventions: `PascalCase` types, `camelCase` methods and variables, `UPPER_SNAKE_CASE` constants,
  lowercase packages. Method names are verbs, boolean names read as questions (`isOverdue`).
- Limits: methods up to 30 lines, up to 5 parameters, cyclomatic complexity up to 8, lines up to 120 characters.
- No `System.out`, no `printStackTrace`, no star imports, no magic numbers, no suppressed rules.
- Gate: `mvn -f backend/pom.xml validate` runs Checkstyle (`backend/config/checkstyle/checkstyle.xml`) and is bound to
  the `validate` phase, so every Maven build fails on a violation. Run it before saying a change is done.
- Never loosen or bypass the Checkstyle config to make code pass; fix the code. Changing the config needs owner approval.

## Challenge 05 outcomes (what is evaluated)

1. End-to-end case visibility (issuance to documented closure).
2. Clear ownership and timely progression; overdue, rejected or unassigned cases are visible.
3. Patient-centered communication and reassurance.
4. Cross-provider consultation and coordination.
5. Safe, human-governed escalation; closure only with evidence or an authorized human decision.
6. Closure and patient impact measures (times, referral completion, unresolved results, drop-off reasons).

## Core concept

The **case lifecycle** is the backbone. Each actionable result is one FHIR `Task`, tracked through:
result issued, patient notified, acknowledged, confirmation, referral requested/accepted/rejected, appointment, treatment or surveillance, **closed**.
Side states: escalated, closed without care (human decision only).

Rules: every transition names an owner and a due date; closing requires evidence or a recorded human decision; every transition writes a `Provenance`; every query writes an `AuditEvent`.

Demo scenario: colorectal cancer screening, positive FIT, confirmation by colonoscopy and biopsy, specialist referral, closure. (History: lupus and myasthenia gravis were considered and dropped because they have no screening program and no usable synthetic lab data.)

Synthetic data approach:
- Background patients come from **Synthea** (Apache-2.0, MITRE), FHIR R4, `colorectal_cancer` module (FIT result LOINC 57905-2, colonoscopy, biopsy, follow-up). Generated bundles also carry SNOMED CT and RxNorm codes: declare this in the data-sources slide.
- Synthea has no referral, ownership, deadlines, patient acknowledgement or closure. Those care-coordination events (Task, Communication, ServiceRequest, Provenance, AuditEvent) are **simulated by our own code** on top of Synthea patients. This is the product, not invented clinical content.
- Known Synthea limits to verify on real output: the FIT result is drawn from an earlier random state, and its value ranges leave a gap between 25 and 150 ng/mL.
- Clinical codes and cutoffs must be verified against real sources before use; never from memory.

## Two interfaces over the same case

- **Patient chatbot (public web):** identity check first (mock civil registry + mock liveness), then answers only from that patient's own FHIR data (case status, prescriptions, latest exams). Strictly closed domain: no RAG, no internet, no medical advice. Out-of-scope questions get a fixed refusal. Session timeout.
- **Clinician web (login):** read-only chat for patient summaries, plus a structured ticket-style form to escalate or advance a case (maps to `Task` transitions). The clinician does not enter clinical data through chat.
- **Dashboard:** overdue, unassigned and rejected cases, plus closure metrics.

## LLM data-flow decision

The LLM receives only opaque placeholders (for example `{{APPOINTMENT_DATE}}`) and the case state. It composes the sentence; the backend substitutes real values. Placeholders are allowlisted per state and role, and the output is validated. Permissions are decided by the backend, never by the model. If the LLM is unavailable, fall back to fixed templates.

## Standards and stack

- FHIR **R4 (4.0.1)**, HAPI FHIR 8.12.1 (the exact version lives in `backend/pom.xml`), Java 21, Spring Boot 3.5.15
  (the version HAPI 8.12.1 itself is built against). Maven needs `JAVA_HOME` pointing to a JDK 21; the build fails otherwise.
- LOINC for laboratory codes (include the LOINC copyright notice). **SNOMED CT is out of scope** (licensing for Ecuador unconfirmed); use a small custom `CodeSystem` and document it as a path-to-production item.
- Docker Compose (`docker compose up -d`): PostgreSQL 17 + official HAPI JPA server (`hapiproject/hapi:v8.12.0-2`) at
  `http://127.0.0.1:8090/fhir`, config in `infra/hapi/application.yaml`. Our backend uses port 8082 (8080 is taken by another local service).
  Cloudflare Tunnel for the public URL, guest mode for reviewers.
- Seed data: `mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=seed
  "-Dspring-boot.run.arguments=--seed.synthea-directory=<path to Synthea fhir output>"` loads the reference bundles,
  every positive-FIT patient and `seed.background-count` (default 200) others into HAPI, dropping `Claim` and
  `ExplanationOfBenefit` and writing FIT `interpretation` POS/NEG at `seed.fit-cutoff` (default 100 ng/mL).
  Patient bundles use plain POST, so do not run it twice against the same database (reset the volume first).
- Context framed for Ecuador: LOPDP treats health data as a special category (articles 25-26). International transfer and impact-assessment rules were not verified; list as limitations.

## Submission constraints

- Deadline: 20 Nov 2026, 13:29 IST (02:59 Ecuador time). Deliver by the night of 19 Nov.
- Package: PDF up to 5 MB from the official PPT template, summary up to 2000 characters, "built during the challenge" up to 1500, prototype URL, demo video under 3 minutes with a public link.
- Organisers provide no data, not even synthetic. Declare every data source and every third-party component with its licence.
- Organiser terms include a 6-month right of first refusal on exclusive licences and a perpetual marketing licence.
