# Case model

Status: the parent Task, the transition policy, `Provenance`, `AuditEvent`, closure reasons and the dashboard are
implemented. The per-handoff child Tasks are **deferred**: the parent carries the current owner and due date.
Facts about FHIR are verified in [fhir-reference.md](fhir-reference.md).
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
- **Child Tasks (`partOf` = parent), one per handoff (deferred, not built).** Each would have its own `status`, `owner`,
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
| 6 | `referral-rejected` | Specialist or service declined | back to `referral-requested` |
| 7 | `referral-accepted` | Specialist accepted | `appointment` |
| 8 | `appointment-booked` | Visit scheduled | `treatment-or-surveillance` |
| 9 | `treatment-or-surveillance` | Care plan in progress | closure |
| 10 | `closed` | Closed with evidence | none |
| 11 | `closed-without-care` | Closed by a human decision with a coded reason | none |

Implemented transitions: each stage moves to the next one; `referral-rejected` returns to `referral-requested`;
`confirmation-done` may close directly when no referral is needed; any open stage may close without care.
Terminal stages have no exits. `referral-rejected` is a stage of the parent (not only a child Task status) so the
dashboard can list rejected referrals.

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
2. Closing as `completed` requires at least one `output` (evidence reference to a resource that exists). Closing as
   `cancelled` requires a `statusReason` coded in our `closure-reason` CodeSystem (`patient-declined`, `unreachable`,
   `relocated`, `care-elsewhere`, `other`) plus the authorised human decision-maker as a second agent in the
   `Provenance`. The server takes that person from the authenticated session; the client cannot set it.
3. Every transition writes a `Provenance` (`target` is the version-specific Task, `agent.who` = actor,
   `activity` = transition, same transaction as the Task update) and is linked from `Task.relevantHistory`.
4. Every query writes an `AuditEvent` (`action` = `E`, `entity.query` = the search).
5. `Task.status = rejected` is only valid before work starts (spec). After acceptance use `cancelled` or `failed`.
6. Permissions are decided by the backend, never by the LLM (see CLAUDE.md): coordinators open, advance any case and
   close without care; clinicians advance only the cases they own; guests and patients cannot change cases
   (see [api.md](api.md)).
7. Updates use optimistic locking (`If-Match` on the `Task` version); a stale write returns `409`.

## Visibility (outcome 2) and metrics (outcome 6)

Implemented in `GET /api/dashboard`:

- **Overdue:** open Task with `restriction.period.end` before today. The standard `period` search parameter maps to
  `executionPeriod`, so this is computed in the service layer.
- **Unassigned:** open Task with no `owner` (the transition policy always requires one, so this stays empty unless
  data is edited outside the API). **Rejected:** parent Task at `referral-rejected`.
- **Cases by stage, closed with evidence, closed without care, average days to close** (from `authoredOn` to the last
  update of closed Tasks; `null` when nothing is closed).
- **Drop-off reasons:** count of `closed-without-care` by closure reason (`unspecified` if a reason is missing).
- **Referral acceptance rate:** cases at `referral-accepted`, `appointment-booked` or `treatment-or-surveillance`
  divided by those plus the ones at `referral-requested` or `referral-rejected`. It uses the current stage and leaves
  out closed cases, so it is an approximation (see [assumptions-and-limitations.md](assumptions-and-limitations.md)).

Not built: times between stages from `Provenance.recorded` (result to notification, to confirmation, to referral
accepted).

## Decisions

1. **Escalation (decided).** A child Task `escalation` owned by a supervisor, plus parent
   `priority` = `urgent`. It is not a `businessStatus` value, so the underlying stage is never lost.
2. **CarePlan (assumption).** Left out of the demo; the `treatment-or-surveillance` child Task `output`
   is enough. Revisit only if time allows.

## Resolved

- Closure reasons for closing without care: own `closure-reason` CodeSystem with five codes (see above).
  `deceased` and other clinical reasons were left out on purpose; `other` covers them until a clinician-reviewed list exists.
