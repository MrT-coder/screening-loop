# API and roles

Base URL when running locally: `http://127.0.0.1:8082`. All data is synthetic.
Responses use JSON. Errors use RFC 9457 `application/problem+json` (`detail` is a safe, generic message).

## Authentication

Two kinds of sessions, both cookie based (`JSESSIONID`, `HttpOnly`, `SameSite=Lax`).

- **Staff** log in with a demo user (`POST /api/login`, form fields `username` and `password`).
- **Patients** do not have accounts. They pass an identity check (see below) and get a short session (10 minutes
  of inactivity) with the role `PATIENT`.

CSRF protection is on. The server sets an `XSRF-TOKEN` cookie; send its value in the `X-XSRF-TOKEN` header on every
`POST`. Spring rotates the token when a session starts, so make a `GET` (for example `/api/me`) after logging in
before the first `POST`.

### Demo staff users (synthetic, defined in `application.yml`, overridable by environment)

| User | Password | Role | FHIR Practitioner |
|------|----------|------|-------------------|
| `coordinator` | `coordinator-demo` | `COORDINATOR` | `Practitioner/demo-coordinator` |
| `gp1` | `gp1-demo` | `CLINICIAN` | `Practitioner/demo-gp-1` |
| `gp2` | `gp2-demo` | `CLINICIAN` | `Practitioner/demo-gp-2` |
| `guest` | `guest-demo` | `GUEST` | `Practitioner/demo-guest` |

Change these before exposing the prototype on a public URL.

## Permissions (decided by the backend, never by the LLM)

| Action | Coordinator | Clinician | Guest | Patient |
|--------|:-----------:|:---------:|:-----:|:-------:|
| Read dashboard, cases, case detail, patient summary | yes | yes | yes | no |
| Open a case | yes | no | no | no |
| Advance a case | any case | only cases they own | no | no |
| Close a case without care (human decision) | yes | no | no | no |
| Patient chat | no | no | no | own data only |

When a case is closed without care, the server records the authenticated coordinator as the human decision-maker.
A client cannot choose or forge it.

## Staff endpoints

| Method and path | Purpose |
|-----------------|---------|
| `POST /api/login`, `POST /api/logout`, `GET /api/me` | Session |
| `GET /api/dashboard` | Totals, cases by stage, overdue, rejected and unassigned cases, average days to close, drop-off reasons, referral acceptance rate |
| `GET /api/cases` | Open cases |
| `GET /api/cases/{id}` | One case plus its history (one `Provenance` per transition) |
| `POST /api/cases` | Open a case (`patient`, `fitResult`, `owner`, `dueDate`) |
| `POST /api/cases/{id}/transitions` | Move a case: `target`, `owner`, `dueDate`, `evidence`, `reason` |
| `GET /api/patients/{id}/summary` | Demographics, FIT results, conditions, active medications, recent procedures, cases |

Every transition writes a `Provenance` in the same FHIR transaction as the `Task` update. Every read of cases,
dashboard, case detail, patient summary and chat data writes an `AuditEvent`.

Transition rules (`422` when broken): only allowed targets from the current stage, always an owner, a due date for
open stages, at least one evidence reference to close, and a closure reason from our own code system to close
without care (`patient-declined`, `unreachable`, `relocated`, `care-elsewhere`, `other`).

## Patient chat endpoints

| Method and path | Purpose |
|-----------------|---------|
| `POST /api/chat/liveness` | Issues a one-time liveness token (valid 2 minutes). **Simulated**, see limitations |
| `POST /api/chat/verify` | Identity check: `documentNumber`, `birthDate`, `livenessToken`. `204` opens the patient session |
| `GET /api/chat/session` | `200` while the patient session is active |
| `POST /api/chat/messages` | `{ "text": "..." }` (max 300 chars) returns `{ "reply", "intent" }` |

Identity check: the mock civil registry looks the patient up by a synthetic document number (the `us-ssn` identifier
Synthea generates, in the invalid `999` range) and compares the birth date. Every failure returns the same `401`.
Five failures lock the document and the address for 15 minutes (`429`).

Chat: the patient is always the one in the session, never one named in the text or body. Supported intents are case
status, active medications, latest exams, help and greeting. Anything else, and any request for medical advice,
gets a fixed refusal that points the patient to the care team.

## Error codes

| Code | Meaning |
|------|---------|
| `400` | Invalid body, or a reference that does not exist |
| `401` | No session, or identity not verified |
| `403` | Wrong role, not the owner of the case, or missing CSRF token |
| `404` | Resource does not exist |
| `409` | The case was changed by someone else (optimistic locking with `If-Match`) |
| `422` | The transition is not allowed |
| `429` | Too many failed identity attempts |

## Health check

`GET /actuator/health` is public.
