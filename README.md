# screening-loop

Closing the loop between an actionable screening result and completed care.

A prototype for the Future Health Digital Innovation Challenge (Challenge 05). Every actionable result becomes a FHIR `Task` with a clear owner, a due date, an audit trail and a documented closure.

> **Status:** the backend is complete and the dashboard page works. The patient chat page and the clinician web are
> still to build. All data is synthetic. This is not a medical device and makes no clinical claims.
> See [assumptions and limitations](docs/assumptions-and-limitations.md).

## Idea in one minute

- A positive screening result opens a **case**, tracked from issuance to documented closure.
- Each step has an **owner** and a **deadline**. Overdue, rejected or unassigned cases are visible on a dashboard.
- Patients check their case status through a **closed-domain chatbot** after an identity check.
- Clinicians read summaries and **escalate or advance** cases through structured tickets.
- Data is standardised with **FHIR R4** so it can be shared across organisations.
- Closure needs **evidence or a recorded human decision**; the software never closes a case alone.

## What works today

- Case lifecycle as a FHIR `Task`, with a transition policy, optimistic locking, a `Provenance` per transition (same
  FHIR transaction) and an `AuditEvent` per read.
- REST API with sessions, roles (coordinator, clinician, guest, patient) and permissions decided in the backend.
- Dashboard: overdue, rejected and unassigned cases, cases by stage, closure reasons, referral acceptance rate.
- Patient summary for clinicians, and a closed-domain patient chat with identity check (simulated registry and
  liveness) and reassuring template replies. An optional Groq rewriter exists and is **off by default**.
- 33 simulated demo cases on top of the Synthea patients with a positive FIT.

## Run it locally

Requirements: JDK 21 (`JAVA_HOME` must point to it), Maven, Docker.

```
docker compose up -d                       # PostgreSQL 17 and HAPI FHIR at http://127.0.0.1:8090/fhir
# load the Synthea patients once (see CLAUDE.md for the seed command)
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=demo-cases
```

The `demo-cases` profile creates the demo practitioners and 33 simulated cases, and skips if cases already exist.
Without the profile the backend starts with whatever is in HAPI. Open http://127.0.0.1:8082/ and use
"Continue as guest" or one of the demo users listed in [docs/api.md](docs/api.md).

Checks: `mvn -f backend/pom.xml verify` runs the tests and the Checkstyle gate.

Optional LLM (off by default): set `GROQ_API_KEY` and `SCREENING_LLM_ENABLED=true`.

## Repository layout

```
backend/    API and case state machine (Spring Boot, HAPI FHIR)
frontend/   Dashboard page today; patient chat and clinician web to come (plain HTML, CSS, JavaScript)
docs/       API, case model, assumptions and limitations, data sources and licences
infra/      HAPI configuration for Docker Compose
```

## Documentation

- [API and roles](docs/api.md)
- [Case model](docs/case-model.md)
- [Assumptions and limitations](docs/assumptions-and-limitations.md)
- [Data sources and licences](docs/data-sources-and-licences.md)
- [FHIR reference](docs/fhir-reference.md), [Synthea findings](docs/synthea-findings.md), [FIT cutoff](docs/fit-cutoff.md)

## Licence

MIT, see [LICENSE](LICENSE).
