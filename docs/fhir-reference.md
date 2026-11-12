# FHIR R4 reference notes (verified)

Source of truth: https://hl7.org/fhir/R4/ (FHIR 4.0.1). Pages read on 2026-11-11:
task, valueset-task-status, valueset-task-intent, workflow, workflow-management, servicerequest,
communication, careplan, appointment, diagnosticreport, observation, provenance, auditevent,
valueset-audit-event-action, codesystem, v3 ObservationInterpretation.

Caveat: the pages were read through an automated summariser, and only the first ~100k characters
of several long pages were covered. Re-check the exact element in the spec before relying on it
in code. Items marked **[not verified]** were not found in the pages read.

## Task

- `status` (1..1, required binding, modifier element), 12 codes: `draft`, `requested`, `received`,
  `accepted`, `rejected`, `ready`, `cancelled`, `in-progress`, `on-hold`, `failed`, `completed`,
  `entered-in-error`.
  - `requested`: ready to be acted upon, action sought.
  - `received`: a potential performer claimed it and is evaluating.
  - `accepted`: performer agreed, has not started.
  - `rejected`: the performer who claimed it decided not to execute it **before performing any action**.
  - `ready`: used in place of requested/received/accepted/rejected when assignment/acceptance is a given.
  - `cancelled`: not completed. `failed`: attempted, could not complete due to an error.
  - `on-hold`: started, paused. `completed`: done.
- `intent` (1..1, required): `unknown | proposal | plan | order | original-order | reflex-order |
  filler-order | instance-order | option`. `unknown` is the Task-specific one.
- `priority`: `routine | urgent | asap | stat`.
- `statusReason` (0..1 CodeableConcept): why held, failed, refused.
- `businessStatus` (0..1): business-specific nuance of state. Examples in spec: "Specimen collected".
  Search param `business-status` (token).
- `code`: what the task involves; Example binding, workflow-specific.
- `focus` (0..1 Reference(Any)): the request being actioned or resource manipulated.
- `basedOn` (0..*): higher-level authorization that triggered the Task.
- `groupIdentifier`: links tasks/requests created in the same context.
- `partOf` (0..*): parent Task (composite task). Suspending/resuming a parent also applies to children.
- `for`: beneficiary (patient). `requester`: who created it. `owner` (0..1): currently responsible
  (Practitioner | PractitionerRole | Organization | CareTeam | HealthcareService | Patient | Device | RelatedPerson).
- `performerType` (0..*): requested performer role (Preferred binding).
- `authoredOn`, `lastModified` (lastModified >= authoredOn).
- `restriction` (0..1): only meaningful when `focus` is a request being fulfilled.
  `restriction.period` = when fulfillment is sought (**our due date**), `restriction.repetitions`,
  `restriction.recipient`.
- `executionPeriod` (0..1): start = first action taken, end = final action before completion.
- `relevantHistory` (0..*, Reference(Provenance)): key events in the Task history.
- `input` / `output` (0..*): `type` (CodeableConcept, 1..1) + `value[x]` (1..1). Output = results / evidence.
- `note` (0..*): comments.
- State machine (spec prose): Created -> Ready (once assigned to an owner) -> In Progress ->
  Completed | Failed | Cancelled. Created/Ready/In Progress can be suspended and resumed.
  In Progress can be returned to Ready (delegate, retry, let others claim).
- Search params: `business-status`, `owner`, `period` (= `Task.executionPeriod`, NOT restriction.period),
  `part-of`, `focus`, `status`, `patient` (= Task.for), `authored-on`, `modified`, `requester`, `based-on`.
  **Consequence:** overdue queries on `restriction.period.end` need a custom SearchParameter in HAPI
  (or filtering in our service layer).

## Workflow patterns (workflow, workflow-management)

- Task is both a "request" and an "event" resource.
- Simple fulfillment: placer creates a request, then a Task pointing to it that seeks fulfillment.
  If the Task names only a performer type it is "open" and any fulfiller may claim it.
- Referral pattern (Option I, outpatient referral): placer creates the request and POSTs a Task to the
  recipient's system. After acceptance, the recipient creates a **sub-Task on the placer's system**
  (`partOf` -> original Task, also referencing the request). Scheduling, no-show and completion are
  recorded on the sub-Task. Spec warns: "Need to keep the two Task states in sync."
- Acceptance = fulfiller updates the Task. Rejection = fulfiller declines; a broker may create a new Task
  for another fulfiller. Completion = fulfiller creates an event resource, sets Task `completed` pointing to it.
  Placer may cancel; the filler controls whether cancellation succeeds.
- Our prototype is a single system, so we simplify: one server holds the parent case Task and the
  child handoff Tasks. Document this as a simplification.

## ServiceRequest

- `status`: `draft | active | on-hold | revoked | completed | entered-in-error | unknown`.
- `intent`: `proposal | plan | directive | order | original-order | reflex-order | filler-order | instance-order | option`.
- `priority`: `routine | urgent | asap | stat`.
- Key elements: `code`, `subject` (1..1), `requester`, `performer` (0..*), `authoredOn`, `occurrence[x]`,
  `reasonCode`, `basedOn`, `replaces`, `requisition`.
- Relation to Task: ServiceRequest can be the authorization (`Task.basedOn`) or the request fulfilled
  (`Task.focus`). Task spans intent and event and tracks execution; ServiceRequest is the proposal/plan/order.

## Communication

- `status` (EventStatus): `preparation | in-progress | not-done | on-hold | stopped | completed |
  entered-in-error | unknown`.
- Elements: `sent`, `received` (arrival at destination), `recipient` (one resource per recipient if
  receipts are tracked individually), `sender`, `payload.content[x]`, `topic`, `basedOn`, `partOf`,
  `inResponseTo`, `about`.
- **No acknowledgment/read-receipt element.** "Patient acknowledged" must be our own modelling
  (e.g. reply Communication with `inResponseTo`, or a child Task); this is an inference, not a documented pattern.

## CarePlan

- `status`: `draft | active | on-hold | revoked | completed | entered-in-error | unknown`.
- `intent`: `proposal | plan | order | option`.
- `activity.detail.status`: `not-started | scheduled | in-progress | on-hold | completed | cancelled |
  stopped | unknown | entered-in-error`.
- `activity.reference` may point to Task or ServiceRequest. Activities can exist without a CarePlan.
  Optional for us: use only for the treatment/surveillance stage.

## Appointment

- `status`: `proposed | pending | booked | arrived | fulfilled | cancelled | noshow | entered-in-error |
  checked-in | waitlist`.
- `participant.status`: `accepted | declined | tentative | needs-action`.
- `basedOn` -> the ServiceRequest (e.g. incoming referral). `start`/`end` both present or both absent;
  only `proposed`/`cancelled` may omit them. `cancelationReason` allowed for `cancelled` and `noshow`.
- A no-show is `status = noshow`.

## DiagnosticReport / Observation

- DiagnosticReport `status`: `registered | partial | preliminary | final +` (list truncated in source) **[full list not verified]**.
  Elements: `basedOn` (can be ServiceRequest), `code` (1..1, LOINC preferred), `subject`, `effective[x]`,
  `issued`, `performer`, `result` (-> Observation), `conclusion`, `conclusionCode`, `category`.
- Observation `status`: `registered | preliminary | final | amended +` (list truncated) **[full list not verified]**.
  Elements: `code` (1..1), `subject`, `effective[x]`, `issued`, `value[x]` (Quantity, CodeableConcept, ...),
  `interpretation` (0..*, Extensible), `referenceRange`, `basedOn`, `dataAbsentReason`.
- Numeric lab values use `valueQuantity`; interpretation is a separate CodeableConcept.
- Interpretation codes, system `http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation`:
  `POS`, `NEG`, `A`, `N`, `H`, `L`, `DET`, `ND`.
- UCUM units: **[not verified in the pages read]**.

## Provenance

- `target` (1..*, Any, should be version-specific on a RESTful system), `recorded` (1..1),
  `occurred[x]` (0..1), `agent` (1..*: `who` 1..1, `type`, `role`, `onBehalfOf`), `activity` (0..1),
  `reason` (0..*), `entity` (0..*: `role` derivation|revision|quotation|source|removal, `what`).
- Bindings: `activity` = ProvenanceActivityType (extensible), `agent.type` = ProvenanceParticipantType
  (extensible), `agent.role` = SecurityRoleType (example), `reason` = v3 PurposeOfUse (extensible).
- Created by the app that does the create/update; submit it with its target in one transaction.
- Search params: `agent`, `agent-role`, `agent-type`, `entity`, `location`, `patient`, `recorded`,
  `signature-type`, `target`, `when`.
- Spec guidance: if a resource already has a property recording how it was obtained, prefer that over Provenance.

## AuditEvent

- Elements: `type` (1..1 Coding), `subtype`, `action` (C|R|U|D|E), `period`, `recorded` (1..1),
  `outcome`, `outcomeDesc`, `purposeOfEvent`, `agent` (1..*: `requestor` 1..1, `who`), `source` (1..1),
  `entity` (`what`, `type`, `role`, `name` XOR `query`, `detail`).
- Action codes: `C` create, `R` read/view/print, `U` update, `D` delete, `E` execute
  (also covers performing a query/search).
- `entity.query` is base64Binary. Maturity: Trial Use. Servers generally should not accept
  update/delete on AuditEvent.
- Provenance = how a resource was generated; AuditEvent = usage and other activity. Overlap is expected.

## CodeSystem (our custom lifecycle codes)

- Required: `status` (1..1), `content` (1..1: `not-present | example | fragment | complete | supplement`),
  `concept.code` (1..1). Recommended: `url` (canonical, globally unique), `concept.display`, `concept.definition`.
- Codes within a system must be unique. References use the canonical URL.

## Open items to verify before coding

- Exact `Task` JSON for `restriction.period` and `output` in a real HAPI 8.x server.
- Full status lists for Observation and DiagnosticReport.
- UCUM unit for the FIT result (ng/mL) and LOINC 57905-2 details from loinc.org.
- Synthea real output for the colorectal module.
