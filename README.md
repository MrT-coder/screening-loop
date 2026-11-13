# screening-loop

Closing the loop between an actionable screening result and completed care.

A prototype for the Future Health Digital Innovation Challenge (Challenge 05). Every actionable result becomes a FHIR `Task` with a clear owner, a due date, an audit trail and a documented closure.

> **Status:** the backend is complete. The staff login, dashboard, case workspace (with a read-only clinician chat) and patient chat pages work. All data is synthetic. This is not a medical device and makes no clinical claims.
> See [assumptions and limitations](docs/assumptions-and-limitations.md).

## About the challenge

This project was built for the **Future Health Digital Innovation Challenge** (Future Health, Abu Dhabi, run on
Hack2skill), **Challenge 05: Closing the Screening-to-Care Loop**. The challenge asks for a solution that makes sure an
abnormal screening result is followed by completed care, instead of being issued and forgotten.

We picked **colorectal cancer screening**: a positive FIT (faecal immunochemical test) result opens a case that must be
followed up until it is closed with evidence or by a recorded human decision.

| What the challenge looks for | Where it lives here |
|------------------------------|---------------------|
| End-to-end case visibility | One FHIR `Task` per case, history from `Provenance`, case workspace |
| Clear ownership and timely progression | Owner and due date on every stage, overdue / rejected / unassigned views |
| Patient-centred communication | Closed-domain patient chat with reassuring, template-based answers |
| Cross-provider coordination | FHIR R4 data model, referral stages, clinician workspace with tickets |
| Safe, human-governed escalation | Closure needs evidence or a coordinator's recorded decision and reason |
| Closure and impact measures | Days to closure, referral acceptance rate, drop-off reasons |

The work started on 11 November 2026, during the challenge. All data is **synthetic**: patients come from
[Synthea](https://github.com/synthetichealth/synthea) and the care-coordination events (referrals, owners, deadlines,
closures) are simulated by this code. Nothing here is clinically validated and it is not a medical device.

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

Frontend work: add the `dev` profile (`-Dspring-boot.run.profiles=dev` from the repository root, or `demo-cases,dev`) and the backend serves
`frontend/` from disk, so a browser reload shows every change without rebuilding. Without it the frontend is served from the jar.

Checks: `mvn -f backend/pom.xml verify` runs the tests and the Checkstyle gate.

Optional LLM (off by default): set `GROQ_API_KEY` and `SCREENING_LLM_ENABLED=true`.

## Repository layout

```
backend/    API and case state machine (Spring Boot, HAPI FHIR)
frontend/   Login, dashboard, case workspace and patient chat (plain HTML, CSS, JavaScript)
docs/       API, case model, assumptions and limitations, data sources and licences
infra/      HAPI configuration for Docker Compose
deploy/     Compose file for OpenMediaVault; see docs/deployment.md
```

## Documentation

- [API and roles](docs/api.md)
- [Case model](docs/case-model.md)
- [Assumptions and limitations](docs/assumptions-and-limitations.md)
- [Data sources and licences](docs/data-sources-and-licences.md), [third-party licence list](docs/third-party-licences.txt)
- [Deployment](docs/deployment.md)
- [FHIR reference](docs/fhir-reference.md), [Synthea findings](docs/synthea-findings.md), [FIT cutoff](docs/fit-cutoff.md)

## Licence

MIT, see [LICENSE](LICENSE).
