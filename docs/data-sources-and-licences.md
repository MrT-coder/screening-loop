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
| LOINC | FIT laboratory code 57905-2 | Required notice, copied verbatim from loinc.org/license (page last updated 2026-08-06, checked 2026-11-16): see the LOINC notice below. |
| SNOMED CT, RxNorm | Present only inside Synthea data | Not used for our own content |
| Own code systems | `case-stage`, `task-type`, `output-type`, `closure-reason` | Placeholder URLs under `https://example.org/screening-loop/CodeSystem/` |

## Software

| Component | Use | Licence | Status |
|-----------|-----|---------|--------|
| HAPI FHIR 8.12.1 (client, R4 structures) and the HAPI JPA server image `hapiproject/hapi:v8.12.0-2` | FHIR client and server | Apache-2.0 | verified 2026-11-16 for the jpaserver-starter repository LICENSE; the client libraries are in the Maven licence list |
| Spring Boot 3.5.15, Spring Security, Spring Web | Backend | Apache-2.0 | to verify against the release |
| PostgreSQL 17 | HAPI database | PostgreSQL Licence | verified 2026-11-16 (postgresql.org/about/licence) |
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
Checkstyle build plugin and the licence of the optional Groq model.

## Services (optional)

| Service | Use | Notes |
|---------|-----|-------|
| Groq API (`https://api.groq.com/openai/v1`) | Optional rewriting of reply templates. **Off by default** | Receives only templates with opaque placeholders, never patient data. Subject to Groq's terms. Model `openai/gpt-oss-20b` (replaces `llama-3.1-8b-instant`, which Groq shut down on 2026-08-16; model licence Apache-2.0, verified on 2026-11-16 from the Hugging Face model card). Not used unless an API key is configured and the switch is turned on |
| Google Stitch | Used once to draft the dashboard layout and palette | AI-generated design reference; we re-implemented the page by hand and removed content it invented |
| Cloudflare Tunnel | Public URL for the demo (https://screening-loop.jbmllnube.com) | In use |

## Notices

This project makes no clinical claim.

### LOINC notice

This material contains content from LOINC (http://loinc.org). LOINC is copyright © Regenstrief Institute, Inc. and the
Logical Observation Identifiers Names and Codes (LOINC) Committee and is available at no cost under the license at
http://loinc.org/license. LOINC® is a registered United States trademark of Regenstrief Institute, Inc.

The licence also asks that extracted LOINC information stays associated with its code and a LOINC display name. We
keep code 57905-2 together with its display text and do not modify any LOINC field.
