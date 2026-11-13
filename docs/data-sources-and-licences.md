# Data sources and third-party components

The organisers provide no data. Every source and component we use is listed here.
Status column: **verified** means we checked it against the project's own source during this work; **to verify**
means it must be checked before submission.

## Data

| Source | Use | Licence | Status |
|--------|-----|---------|--------|
| [Synthea](https://github.com/synthetichealth/synthea) (MITRE), module `colorectal_cancer`, FHIR R4 output | Synthetic patients, FIT results (LOINC 57905-2), colonoscopies, conditions | Apache-2.0 | verified (see CLAUDE.md and [synthea-findings.md](synthea-findings.md)) |
| Our own simulation (`DemoCasePlanner`) | Cases, owners, due dates, transitions, closure reasons, `Provenance`, `AuditEvent` | MIT (this repository) | ours |

Generated Synthea bundles also contain **SNOMED CT** and **RxNorm** codes. We pass them through unchanged and do not
distribute a terminology. We drop `Claim` and `ExplanationOfBenefit` resources when loading.

## Terminology

| Code system | Use | Notice |
|-------------|-----|--------|
| LOINC | FIT laboratory code 57905-2 | LOINC is copyright Regenstrief Institute, Inc. and the LOINC Committee and is available at no cost under the licence at http://loinc.org/license. The exact current wording must be copied from loinc.org/license before submission (**to verify**; the site blocked automated access when we tried). |
| SNOMED CT, RxNorm | Present only inside Synthea data | Not used for our own content |
| Own code systems | `case-stage`, `task-type`, `output-type`, `closure-reason` | Placeholder URLs under `https://example.org/screening-loop/CodeSystem/` |

## Software

| Component | Use | Licence | Status |
|-----------|-----|---------|--------|
| HAPI FHIR 8.12.1 (client, R4 structures) and the HAPI JPA server image `hapiproject/hapi:v8.12.0-2` | FHIR client and server | Apache-2.0 | to verify against the release |
| Spring Boot 3.5.15, Spring Security, Spring Web | Backend | Apache-2.0 | to verify against the release |
| PostgreSQL 17 | HAPI database | PostgreSQL Licence | to verify |
| Jackson, Logback, SLF4J, JUnit 5, Mockito, AssertJ | Runtime and tests | Apache-2.0 / EPL / MIT / LGPL families | to verify (generate the full list with a Maven licence report) |
| Checkstyle | Build-time code style gate only, not shipped | LGPL | to verify |
| Docker, Docker Compose | Local environment | Apache-2.0 (Compose) | to verify |
| Frontend | Plain HTML, CSS and JavaScript, no third-party libraries and no web fonts | n/a | ours |

### Runtime dependencies (machine generated)

[third-party-licences.txt](third-party-licences.txt) lists the 81 compile and runtime dependencies of the backend with
their licences, produced on 2026-11-12 with `org.codehaus.mojo:license-maven-plugin:2.4.0:add-third-party`
(scopes `compile` and `runtime`). Summary: 71 are Apache-2.0 under various spellings, 3 MIT, plus the exceptions below.

| Component | Licence | Note |
|-----------|---------|------|
| Logback Classic and Core | EPL-2.0 or LGPL-2.1-only (dual) | Used as a library, unmodified |
| Saxon-HE (via HAPI FHIR) | MPL-2.0 | Unmodified |
| Jakarta Annotations API | EPL-2.0 with GPL-2.0 Classpath Exception | Unmodified |
| ICU4J | Unicode-3.0 | |
| HdrHistogram, LatencyUtils | BSD-2-Clause / CC0 (public domain) | |
| Checker Qual | MIT | |

Test-only libraries (JUnit 5, Mockito, AssertJ and the Spring test modules) are not shipped. Regenerate the list after
any dependency change. The remaining **to verify** items are the container images (HAPI JPA server, PostgreSQL), the
Checkstyle build plugin and the Meta Llama licence for the optional Groq model.

## Services (optional)

| Service | Use | Notes |
|---------|-----|-------|
| Groq API (`https://api.groq.com/openai/v1`) | Optional rewriting of reply templates. **Off by default** | Receives only templates with opaque placeholders, never patient data. Subject to Groq's terms. Model `llama-3.1-8b-instant` (Meta Llama licence applies; to verify). Not used unless an API key is configured and the switch is turned on |
| Google Stitch | Used once to draft the dashboard layout and palette | AI-generated design reference; we re-implemented the page by hand and removed content it invented |
| Cloudflare Tunnel | Planned public URL for the demo | Not set up yet |

## Notices

This project makes no clinical claim. LOINC is a registered trademark of Regenstrief Institute, Inc.
