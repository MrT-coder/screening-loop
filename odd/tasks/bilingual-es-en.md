# Bilingual system (Spanish and English)

Objective: the whole system (patient chat, clinician workspace, dashboard, login) works in Spanish and English with a language switch.
Branch: feat/bilingual-es-en. Delivery strategy: ask-on-risk. Test runner: `mvn -f backend/pom.xml test` and `mvn -f backend/pom.xml validate`.

## Specs

- S1: "y hagamos una version en espanol osea poder cambiar el idio se puede?" A language switch (EN / ES) is available in the UI.
- S2: "mi idea seria que el valor de si esta en espanol o ingles se tome y segun eso repsonda el LLM". The selected language value travels
  with each chat request and drives the language of the reply, including the LLM rewriter when it is enabled.
- S3: "y quiero que todo el ssitema incluso dahsboard este en espanol e ingles". Every screen is available in both languages:
  login, index, patient chat, cases workspace, clinician chat, dashboard.
- S4 (assumption, accepted by the owner with "claro"): fixed templates exist in both languages as the base and the fallback when the LLM
  is off or fails. The LLM only rewrites in the selected language. The validator keeps requiring the same placeholders.
- S5 (assumption, accepted by the owner with "claro"): Spanish text lives in resource files outside Java classes, so the Java rules (no
  non-ASCII, English strings) stay untouched. Checkstyle config is not changed.
- S6: Challenge 05 outcome 3 (patient-centered communication) is the reason for the feature. Safety rules do not change: closed domain,
  fixed refusal, no medical advice, permissions decided by the backend.

## Tasks

- [x] T1 (S2, S4, S5, S6) Backend language plumbing: language value on chat requests, language-aware templates (patient and clinician),
  stage labels, rewriter receives the language. Route: inline. Commit: none yet (project rule: no commit unless asked). Observed: 227 tests, 0 failures, Checkstyle clean; 6 integration errors need a local HAPI on port 8090 (environmental). Closure reasons and API error details stay English in the backend; the frontend maps them (T3).
- [x] T2 (S1, S3) Frontend i18n core: js/i18n.js, js/locales/en.js and es.js, selector on every page (data-lang-switch), choice in localStorage, html lang attribute. Route: inline. Commit: none yet.
- [x] T3 (S3) Every page and script translated. Staff pages reload on a language change; the patient page re-renders in place. Observed in a real browser against a local dev server: patient chat in ES (chips, refusal, switch back to EN), dashboard, cases list, detail, clinician chat in ES. Route: inline. Commit: none yet.
- [ ] T4 (S1-S6) Backend tests for both languages (done: 227 tests), browser check in both languages (done locally), docs (done: README, docs/api.md), image rebuilt (f83cb3aed5ad). Pending: redeploy to the OMV (owner runs the ssh steps) and a check on the public URL. Known limits: backend error details for case transitions stay English when the UI is English-only text from the server; in ES the UI shows a generic message.

## Log

- L1 (2026-11-13): "y hagamos una version en espanol osea poder cambiar el idio se puede?"
- L2 (2026-11-13): "claro osea mi idea seria que el valor de si esta en espanol o ingles se tome y segun eso repsonda el LLM"
- L3 (2026-11-13): "y quiero que todo el ssitema incluso dahsboard este en espanol e ingles"
- L4 (2026-11-13): Scope inventory: frontend 1343 lines (6 pages, 6 scripts); backend text in ReplyTemplates (92), ClinicianReplies (90),
  CaseStage, ClosureReason, ApiExceptionHandler, identity exceptions. All tasks run inline (no parallel units with a writer reason).
- L5 (2026-11-13): Local dev DB incident: restoring the dump over a DB that already had data left HAPI id sequences behind (HAPI-0550, res_id already exists, 409 on /api/dashboard). Fixed by bumping every public sequence by 10000 and restarting HAPI; no data deleted.
