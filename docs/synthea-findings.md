# Synthea findings (real output)

Run on 2026-11-11 with the official `synthea-with-dependencies.jar` (master-branch-latest,
build bd8a52b, built 2026-10-21), Java 17. Command shape (outputs kept outside the repo):

```
java -jar synthea-with-dependencies.jar -s 1 -p 40 -a 50-75 -m colorectal_cancer \
  --exporter.baseDirectory=<out> --exporter.fhir.export=true
```

40 patients, ages 50-75, FHIR R4 bundles (`urn:uuid` references).

## What the FIT looks like

- Observation, LOINC `57905-2` ("Hemoglobin.gastrointestinal.lower [Presence] in Stool by Immunoassay --1st specimen"),
  category `laboratory`, `valueQuantity` in `ng/mL` (UCUM `ng/mL`), no `interpretation`, no `referenceRange`.
- The LOINC display says "[Presence]" but the value is a number: a Synthea quirk. We must define positive/negative ourselves.
- Only 5 of 40 patients have a FIT (6 observations). Values: 6.98, 12.13, 12.56, 14.58, 24.50, 173.21.
  Only one is clearly above any plausible cutoff. The 25-150 ng/mL gap noted earlier is consistent with the data
  (nothing observed in that range) but 6 samples cannot prove it.
- The positive cutoff is **not** in the data and must be verified from a real source before use.

## What the timeline looks like

- The FIT, the colonoscopy, the polypectomy and the "Screening for occult blood in feces" procedure all carry the
  **same date**. Even a FIT of 6.98 ng/mL is recorded with a polyp and a polypectomy on that day.
  So the FIT does not drive the colonoscopy in Synthea: it is a side record, as suspected.
- The one cancer case (FIT 173.21): colonoscopy + biopsy of colon on 2023-08-19, cancer and anemia
  diagnosed 2023-08-25, partial colon resection 2023-09-05.
- Only 1 "Biopsy of colon" procedure in 40 patients.

## What is missing (as expected)

Resource types present: Immunization, DiagnosticReport, Claim, ExplanationOfBenefit, Encounter, DocumentReference,
Observation, Procedure, Patient, Provenance, Condition, MedicationRequest, Medication, MedicationAdministration,
CareTeam, CarePlan. **No** `Task`, `ServiceRequest`, `Appointment` or `Communication`.
Code systems present include SNOMED CT, LOINC, RxNorm, CVX and CMS Blue Button systems: declare them in the
data-sources slide.

## Consequences for the design

1. We cannot replay Synthea's own timeline as the case. Our code **re-times** the coordination events:
   take a patient with a FIT result, and generate the lifecycle (issue, notify, acknowledge, confirm, referral, ...)
   on our own dates, with our own owners and due dates. This is the product, not invented clinical content.
2. A positive FIT is rare (about 1 in 40). Either generate a larger population and select, or take the FIT value
   from Synthea and set the demo cases deliberately (documented as synthetic).
3. Claim and ExplanationOfBenefit are noise for us (about 950 of ~2,700 resources); drop them on import.

## Larger run (2,000 patients)

Seed 7, ages 50-74, same flags, about 50 seconds. 2,024 records (2,000 alive, 24 dead).

- 316 patients have a FIT (329 observations). Values: min 0.20, max 488.52 ng/mL.
- Bins: 296 observations below 25 ng/mL, 33 at or above 150, **none between 25 and 150**.
  The gap is confirmed, so any cutoff in 50-100 classifies every value the same way (33 positive).
- 33 positives (1.65% of patients). All 33 have a colon biopsy on the FIT date; 23 have a neoplasm diagnosis
  on or after the FIT date (within about 2 weeks); 10 have a biopsy but no neoplasm diagnosis.
- Conclusion: a 2,000-patient run gives enough positives to select 8-12 protagonist cases and keep the rest as background.
