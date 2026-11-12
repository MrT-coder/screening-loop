# Case model

Status: draft. Facts about FHIR are verified in [fhir-reference.md](fhir-reference.md).
Items marked **(assumption)** are our design choices, not spec requirements.

## Goal (Challenge 05)

One actionable screening result = one **case**, visible from issuance to documented closure.
Every handoff has an owner and a due date. Closure needs evidence or a recorded human decision.

## Structure

- **Parent Task = the case.** `code` = `screening-case` (own CodeSystem), `intent` = `order`,
  `for` = Patient, `focus` = the triggering FIT `Observation`
  (Synthea records the FIT as an `Observation` with no `DiagnosticReport`),
  `businessStatus` = current lifecycle stage (own CodeSystem), `priority` = `routine` or `urgent`.
  Single-system simplification of the spec's referral pattern (which uses a sub-Task on the placer's system).
- **Child Tasks (`partOf` = parent), one per handoff.** Each has its own `status`, `owner`,
  `restriction.period.end` (due date), `executionPeriod` and `output`.
- Parent `status` follows the lifecycle: `in-progress` while open, `completed` on closure with evidence,
  `cancelled` on closure without care (human decision, **(assumption)** `failed` is not used for this).

## Lifecycle (`Task.businessStatus`, own CodeSystem)

| # | Code | Meaning | Child Task that moves it forward |
|---|------|---------|----------------------------------|
| 1 | `result-issued` | Positive FIT recorded | `notify-patient` |
| 2 | `patient-notified` | Patient was contacted | `patient-acknowledgement` |
| 3 | `acknowledged` | Patient confirmed they got it | `confirmation` (colonoscopy, biopsy) |
| 4 | `confirmation-done` | Confirmatory result available | `referral` (if needed) |
| 5 | `referral-requested` | Referral sent to a specialist | `referral` |
| 6 | `referral-accepted` | Specialist accepted | `appointment` |
| 7 | `appointment-booked` | Visit scheduled | `treatment-or-surveillance` |
| 8 | `treatment-or-surveillance` | Care plan in progress | closure |
| 9 | `closed` | Closed with evidence | none |

Side states: `referral-rejected` (child `referral` is `rejected`, parent returns to step 4 with a new owner),
`closed-without-care` (human decision only).

## Resource per stage

| Stage | FHIR resources |
|-------|----------------|
| Result issued | `Observation` (FIT, LOINC 57905-2), the `focus` of the case |
| Notified / acknowledged | `Communication` (`sent`, `received`; reply via `inResponseTo`). Acknowledgement is our modelling: `Communication` has no read-receipt element |
| Confirmation | `ServiceRequest` + child `Task` (`focus` = the ServiceRequest) |
| Referral | `ServiceRequest` + child `Task` with native `requested`, `accepted`, `rejected` |
| Appointment | `Appointment` (`basedOn` = referral ServiceRequest, `noshow` supported) |
| Treatment or surveillance | `CarePlan` (optional) |
| Closure | parent `Task` `completed` with `output` evidence, or `cancelled` + `statusReason` |

## Rules enforced by the backend

1. Every transition sets `owner` and `restriction.period.end`. A transition without both is rejected.
2. Closing as `completed` requires at least one `output` (evidence reference). Closing as `cancelled`
   requires `statusReason` plus an authorised human agent in the `Provenance`.
3. Every transition writes a `Provenance` (`target` is the version-specific Task, `agent.who` = actor,
   `activity` = transition, same transaction as the Task update) and is linked from `Task.relevantHistory`.
4. Every query writes an `AuditEvent` (`action` = `E`, `entity.query` = the search).
5. `Task.status = rejected` is only valid before work starts (spec). After acceptance use `cancelled` or `failed`.
6. Permissions are decided by the backend, never by the LLM (see CLAUDE.md).

## Visibility (outcome 2) and metrics (outcome 6)

- **Overdue:** open Task with `restriction.period.end` in the past. The standard `period` search parameter
  maps to `executionPeriod`, so we need a custom `SearchParameter` or a service-layer filter.
- **Unassigned:** open Task with no `owner`. **Rejected:** child Task `status = rejected`.
- **Times:** from `Provenance.recorded` and `Task.authoredOn`/`executionPeriod` (result to notification,
  to confirmation, to referral accepted, to closure).
- **Referral completion, unresolved results, drop-off reasons:** from `businessStatus` and `statusReason`.

## Decisions

1. **Escalation (decided).** A child Task `escalation` owned by a supervisor, plus parent
   `priority` = `urgent`. It is not a `businessStatus` value, so the underlying stage is never lost.
2. **CarePlan (assumption).** Left out of the demo; the `treatment-or-surveillance` child Task `output`
   is enough. Revisit only if time allows.

## Open

- Exact `statusReason` codes for closure without care (drop-off reasons: patient declined, unreachable,
  deceased, moved, etc.). Our own CodeSystem; to be defined with the dashboard drop-off metric.
