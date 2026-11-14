# Assumptions and limitations

This is a prototype for a hackathon. It is **not a medical device**, it has **no clinical validation**, and it uses
**synthetic data only**. A positive screening test is not a diagnosis. Humans stay in the loop: the software never
closes a case on its own.

## Data

- Background patients come from Synthea (module `colorectal_cancer`). Synthea has no referral, ownership, deadline,
  acknowledgement or closure. Those care-coordination events are **simulated by our own code** on top of Synthea
  patients (`demo-cases` profile). The dashboard figures therefore describe simulated cases, not real performance.
- Synthea limits we verified (see [synthea-findings.md](synthea-findings.md) and [fit-cutoff.md](fit-cutoff.md)):
  FIT values leave a gap between 25 and 150 ng/mL, and we write the POS/NEG interpretation at a configurable cutoff
  (default 100 ng/mL). The cutoff is a demo choice, not a clinical recommendation.
- Many Synthea patients have no active medication, so that answer can be empty in the demo.
- Demo case dates are created "now", so the average days to close is close to zero on demo data.

## Case model

- The case is the **parent `Task` only**. The per-handoff child Tasks described in
  [case-model.md](case-model.md) are deferred: the parent carries the current owner and due date, and each transition
  has its own `Provenance`.
- Opening a case writes no `Provenance`; only transitions do, so history starts at the first transition.
- **Referral acceptance rate** is computed from the current stage and leaves out cases that are already closed, because
  the `Task` alone does not tell whether a closed case went through a referral. An exact rate needs the `Provenance`
  history.
- Rejected referrals have no recorded rejection reason yet; only closures without care carry a coded reason.
- Overdue is computed in the service layer, because the standard `Task` search parameter `period` maps to
  `executionPeriod`, not to `restriction.period`.

## Identity and security

- **Facial liveness is simulated.** `POST /api/chat/liveness` only issues a one-time token; no face is captured,
  compared or stored. The web page opens the camera locally for realism and uploads nothing. A script can call the
  endpoint, so this proves nothing biometric. Path to production: a certified liveness and face-verification provider,
  explicit consent, retention limits and an impact assessment.
- The **civil registry is a mock** backed by the synthetic `us-ssn` identifier that Synthea generates. It stands in for
  a real civil registry.
- The attempt limiter and liveness tokens live **in memory**: they reset on restart and work for a single instance.
- Behind a Cloudflare Tunnel the server would see the tunnel address for every visitor. The `prod` profile reads the
  real address from the `CF-Connecting-IP` header, accepted only from private addresses, and the application is
  published only on the host loopback (see [deployment](deployment.md)). The lock is still per address: people who
  share one public address (an office, a mobile carrier) share its attempts.
- Staff accounts are demo accounts. Their passwords come from the environment (`COORDINATOR_PASSWORD`,
  `GP1_PASSWORD`, `GP2_PASSWORD`) and must be replaced on a public server. The guest credentials are public by design
  (read only).
- A real deployment would use an identity provider and SSO for staff, not an in-memory user list.

## Patient chat and the LLM

- The chat is a **closed domain**: no RAG, no internet, no medical advice. Intent detection is rule based, so a
  question phrased in an unusual way can fall into the fixed refusal. Replies are in English; a few Spanish keywords
  are recognised.
- Procedure and condition names come straight from the Synthea bundles (SNOMED CT display text, in English). They are
  shown unchanged in Spanish replies, so a Spanish answer can list English terms. We do not translate them: SNOMED CT
  is out of scope for licensing and a hand-made clinical translation would be unverified. Path to production: a
  licensed terminology with Spanish descriptions.
- The LLM is **optional and off by default**. When enabled it only receives a reply template with opaque placeholders
  (for example `{{NEXT_STEP_DATE}}`) and no patient data. The backend validates the rewrite (same placeholders, no
  digits, no links, no new facts, reassurance sentence kept) and falls back to the fixed template otherwise. The
  backend substitutes the real values. Permissions are never decided by the model.
- The validator cannot detect a subtle piece of medical advice written without digits. That residual risk is the main
  reason the LLM stays off by default.
- The Groq integration was tested against the real service on 2026-11-13 with `openai/gpt-oss-20b` (Groq shut down the
  earlier `llama-3.1-8b-instant` on 2026-08-16). The model reasons before answering, so we send `reasoning_effort` low
  and a 1024 token cap, and every call logs its token usage. If the service fails or returns nothing, the fixed
  template is used.

## Standards and terminology

- FHIR R4 (4.0.1) on HAPI FHIR. LOINC for laboratory codes. **SNOMED CT is out of scope** for our own content because
  licensing for Ecuador is unconfirmed; we use small custom code systems (`case-stage`, `task-type`, `output-type`,
  `closure-reason`) under `https://example.org/screening-loop/CodeSystem/`. These URLs are placeholders. Path to
  production: publish and maintain the code systems and map them to a licensed terminology.
- Synthea-generated bundles also carry SNOMED CT and RxNorm codes. We pass them through unchanged; see
  [data-sources-and-licences.md](data-sources-and-licences.md).

## Privacy and law (Ecuador)

- The Ecuadorian data protection law (LOPDP) treats health data as a special category (articles 25 and 26). We did
  **not** verify the international transfer rules or the impact-assessment requirements. Using an external LLM or a
  face-verification provider with real patients would require both.
- No real patient data is used anywhere. The audit trail (`AuditEvent`) covers reads made through our API, not direct
  access to the FHIR server.

## Operations

- Docker Compose runs a single HAPI server and PostgreSQL; there is no high availability, backup or monitoring.
- Search results are cached by HAPI for about a minute; our searches ask for `Cache-Control: no-cache`.
- HAPI enforces referential integrity: every reference we write must exist.
- Another local service (openelisglobal) was already using port 8080 on the development machine, so our backend
  uses 8082.

## Not built

Per-handoff child Tasks, `Communication` records for notifications, `Appointment` and `CarePlan` resources, push
notifications, multi-language UI, accessibility audit, load testing, and an exact referral-completion metric.
