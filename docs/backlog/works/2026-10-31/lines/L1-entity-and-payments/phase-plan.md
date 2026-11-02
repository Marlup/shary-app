# L1 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: one real test payment travels from charge to retained invoice
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Entity and tax setup

Indicative duration: 6-10 weeks, driven by notary and Registro Mercantil lead
times.

Objective: create the S.L., obtain the NIF/CIF, complete tax registrations, and
open the company bank account.

Work items:

1. Confirm S.L. versus autónomo with the asesor; adopt S.L. (S.L.U. acceptable).
2. Reserve the company name and prepare estatutos with the notary.
3. Execute the deed and file with the Registro Mercantil.
4. Obtain NIF/CIF and the registered address proof.
5. File alta censal (036/037), IAE, IVA, and OSS if the asesor confirms it is
   needed at launch.
6. Open the company bank account in the S.L.'s name.

Exit gate: L1-V1, L1-V2, L1-V3.

Evidence: EV-L1-01 to EV-L1-04.

Dependencies: asesor engagement; D1 decision.

Risks: incorporation delay, bank onboarding, incomplete OSS scope. Mitigation:
start the notary and bank paperwork in parallel; keep the asesor's written scope
confirmation.

## Phase 2 — Merchant and Play readiness

Indicative duration: 2-3 weeks after the NIF exists.

Objective: make the S.L. able to receive Play payments and pay Google Cloud
bills without a single point of failure.

Work items:

1. Create or migrate the Google Play developer organization account.
2. Verify the D-U-N-S requirement for a Spanish organization account.
3. Complete the payments profile, merchant account, and tax forms.
4. Move GCP/Firebase billing to the S.L.; add a backup payment method and
   budget alerts.
5. Configure license testing and run the sandbox purchase.

Exit gate: L1-V4, L1-V5, L1-V7.

Evidence: EV-L1-05, EV-L1-06, EV-L1-08.

Dependencies: this phase satisfies G2 for [cross-line.md](../cross-line.md);
L4 product identifiers
for the test purchase.

Risks: organization account review, D-U-N-S delay, merchant verification.
Mitigation: start the organization account in week 1; keep the individual
account only for closed testing with a documented transfer plan.

## Phase 3 — Finance operations and records

Indicative duration: 1-2 weeks.

Objective: make invoicing, accounting, retention, and restore routine.

Work items:

1. Define the invoicing workflow for Play and any direct sales.
2. Hand off bookkeeping to the gestoría and agree the monthly close.
3. Write the retention schedule: Código de Comercio (6 years), tax records
   (4 years), and any longer period the asesor identifies.
4. Back up the finance records and run a restore test.
5. Reconcile the test purchase against the invoice and bank statement.

Exit gate: L1-V6, L1-V8.

Evidence: EV-L1-07, EV-L1-09.

Dependencies: P1 and P2 complete; gestoría engaged.

Risks: retention periods vary by record type; the asesor must confirm the
schedule. Mitigation: record the legal basis for each retention period.

## Gate review

- All L1-V criteria pass.
- LIS-1 to LIS-8 pass in the validation suite.
- Entity identity (SC1) is published to L2, L3, L4, and L7.
- The D4 planning envelope is replaced with actual quotes.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
