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

### Quick start with the demo data

The synthetic patients and the simulated cases are published as a database dump (5 MB) in the
[demo-data-v1 release](https://github.com/MrT-coder/screening-loop/releases/tag/demo-data-v1), so you see exactly what
we show in the demo.

```
git clone https://github.com/MrT-coder/screening-loop.git
cd screening-loop
docker compose up -d db                    # PostgreSQL 17 only, HAPI must not start before the restore
curl -L -o hapi-demo-data.sql.gz https://github.com/MrT-coder/screening-loop/releases/download/demo-data-v1/hapi-demo-data.sql.gz
gunzip -c hapi-demo-data.sql.gz | docker compose exec -T db psql -U hapi -d hapi -q
docker compose up -d fhir                  # HAPI FHIR at http://127.0.0.1:8090/fhir, the first start takes a minute or two
mvn -f backend/pom.xml spring-boot:run
```

Open http://127.0.0.1:8082/ and use "Continue as guest", or one of the demo users listed in
[docs/api.md](docs/api.md). The patient chat is at http://127.0.0.1:8082/patient.html and lists four demo patients.

### Regenerate the data from scratch

Only needed if you want different patients. Generate them with Synthea (we used seed 7, 2000 patients, ages 50 to 75,
module `colorectal_cancer`), then load them and create the simulated cases:

```
java -jar synthea-with-dependencies.jar -s 7 -p 2000 -a 50-74 -m colorectal_cancer   --exporter.baseDirectory=<out> --exporter.fhir.export=true
docker compose up -d
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=seed   "-Dspring-boot.run.arguments=--seed.synthea-directory=<out>/fhir"
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=demo-cases
```

Synthea output depends on its build, so the patients may differ from ours, and the demo patients listed on the patient
page may not exist in your data. Run the seed once per database: it uses plain `POST`.

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
