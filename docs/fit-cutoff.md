# FIT positivity cutoff (verified)

Checked on 2026-11-11 against two primary documents that were downloaded and read as text.
This is a demo parameter, not clinical validation.

## What the sources say

1. **World Endoscopy Organization (WEO) FIT expert working group, Chicago 2014 meeting report**
   <https://worldendo.org/assets-craft/pdf/committee/colorectal-cancer/fit-for-screening/weo_expert_working_group_fit_meeting_report_chicago2014.pdf>
   - Positivity cut-off "> 20 µg Hb/g faeces (> 100 ng Hb/mL buffer)" for a screening pilot using OC-SENSOR.
   - England's 2014 FIT pilot (OC-SENSOR) used "> 20 µg/g (100 ng/mL)", expected positivity about 6%,
     to be adjusted to colonoscopy capacity.
   - Manufacturers differ: the cut-off for qualitative FIT is set by the manufacturer; for quantitative FIT
     it is set by the user according to local requirements.
2. **Ministerio de Salud Pública de Uruguay, "Guía de práctica clínica de tamizaje del cáncer colo-rectal" (2018)**,
   hosted by PAHO: <https://www.paho.org/sites/default/files/2026/04/guia-tamizaje-cancercolo-rectal.pdf>
   - Recommended method: "Punto de corte: entre 50 y 100 ngr Hb/ml o 10-20 µgr Hb/gr de materia fecal."
   - Uruguay has used 100 ng Hb/mL since 2001. The cut-off is described as controversial and under review,
     and it depends on the method and on colonoscopy capacity.
   - Positivity example in the guide: 8.1% at 50 ng/mL vs 4.8% at 100 ng/mL.
   - Screening population in the guide: age 50-74, asymptomatic, repeat every 2 years after a negative test.

## What is not verified

- **Ecuador's cutoff:** not found in any official MSP document (searched 2026-11-11). The only Ecuador-specific
  hit was a dated note saying the cutoff was still being standardised in the national pilot.
- **Current NHS England threshold:** not confirmed (the sources above describe a 2014 pilot only).
- **Device used by Synthea:** Synthea does not say. The unit `ng/mL` is per mL of buffer, and the conversion
  to µg Hb/g depends on the collection device (for example 10 mg of faeces in 2 mL of buffer for OC-SENSOR),
  so `ng/mL` values are not comparable across devices.

## Decision (assumption)

- Configurable cutoff, **default 100 ng Hb/mL buffer** (about 20 µg Hb/g for OC-SENSOR-type devices),
  stored as a named parameter with this document as its source. Valid range to explore in the demo: 50-100.
- Classification: value >= cutoff is positive, otherwise negative. The sources write both "above" and "or more";
  we choose `>=` and document it. No Synthea value (6.98, 12.13, 12.56, 14.58, 24.50, 173.21 ng/mL) falls
  between 50 and 100, so the classification is the same for any cutoff in that range.
- Write the result as a standard `interpretation` (`POS` / `NEG`, system
  `http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation`) since Synthea provides none.
- State in the UI and slides that the cutoff is an illustrative parameter and Ecuador's national value is
  a path-to-production item.
