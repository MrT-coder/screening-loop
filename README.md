# screening-loop

Closing the loop between an actionable screening result and completed care.

A prototype for the Future Health Digital Innovation Challenge (Challenge 05). Every actionable result becomes a FHIR `Task` with a clear owner, a due date, an audit trail and a documented closure.

> **Status:** design phase. All data is synthetic. This is not a medical device and makes no clinical claims.

## Idea in one minute

- A positive screening result opens a **case**, tracked from issuance to documented closure.
- Each step has an **owner** and a **deadline**. Overdue, rejected or unassigned cases are visible on a dashboard.
- Patients check their case status through a **closed-domain chatbot** after an identity check.
- Clinicians read summaries and **escalate or advance** cases through structured tickets.
- Data is standardised with **FHIR R4** so it can be shared across organisations.

## Repository layout

```
backend/    API and case state machine (Spring Boot, HAPI FHIR)
frontend/   Patient chat, clinician web, dashboard
docs/       Architecture, case lifecycle, assumptions and limitations
```

## Licence

MIT, see [LICENSE](LICENSE).
