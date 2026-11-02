# L1 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Each criterion has an ID, a method, a pass condition, an evidence artifact, and
an official source. A phase closes only when its criteria pass and the evidence
is stored in the external evidence folder. The repository records the location,
not the sensitive document.

Methods:

- **Document review**: signed or filed document checked against the criterion.
- **External sign-off**: asesor, abogado, or accountant confirms in writing.
- **Manual drill**: the process is executed end to end and recorded.
- **Automated test**: a named test or CI job proves the behavior.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L1-V1 | Legal entity exists | Document review | Notarial deed, NIF/CIF, and Registro Mercantil filing receipt or registration proof are present | EV-L1-01, EV-L1-02 | RDL 1/2010; Registro Mercantil |
| L1-V2 | Tax registrations complete | Document review + external sign-off | Alta censal, IAE, IVA, and OSS (if applicable) receipts are present; asesor confirms scope | EV-L1-03 | AEAT; Ley 58/2003 |
| L1-V3 | Company bank account active | Document review | Account is in the S.L.'s name; certificate or statement confirms IBAN and holder | EV-L1-04 | Bank records |
| L1-V4 | Play organization account ready | Document review | Organization developer account is approved and tied to the S.L.; D-U-N-S requirement verified | EV-L1-05 | Google Play Console Help |
| L1-V5 | Merchant and payments profile ready | Manual drill | Payments profile, merchant account, and tax forms are complete; a license-test purchase settles | EV-L1-06 | Google Play Console Help |
| L1-V6 | Invoicing and accounting workflow works | Manual drill | A dry-run invoice contains legal name, NIF, address, VAT treatment, and sequential number; gestoría accepts it | EV-L1-07 | Código de Comercio; AEAT |
| L1-V7 | GCP/Firebase billing is resilient | Document review | Billing account is owned by the S.L.; backup payment method and budget alerts are configured | EV-L1-08 | Google Cloud billing docs |
| L1-V8 | Finance records are retained and restorable | Manual drill | Retention schedule matches Código de Comercio and tax periods; a restore test recovers the records | EV-L1-09 | Código de Comercio; Ley 58/2003 |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L1-T1 | Manual drill | Run a license-test purchase through the Play billing sandbox | Purchase recorded; settlement report shows the expected amount and fee |
| L1-T2 | Manual drill | Issue a dry-run invoice for the test purchase | Invoice has legal entity data, VAT treatment, and sequential number |
| L1-T3 | Manual drill | Restore the finance records from backup into a clean folder | All invoices and receipts are readable and match the register |
| L1-T4 | Document review | Asesor reviews tax registrations and retention schedule | Written confirmation or a list of corrections |
| L1-T5 | Manual drill | Reconcile the settlement report against the invoice and bank statement | Amounts reconcile within the expected fee and tax tolerance |

## 4. Evidence register

Sensitive documents are stored outside the repository in the founder's secured
evidence folder. The repository records only the reference and location.

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L1-01 | Notarial deed and estatutos | External secure folder | Founder | Pending | On corporate change |
| EV-L1-02 | NIF/CIF and Registro Mercantil proof | External secure folder | Founder | Pending | Annual |
| EV-L1-03 | AEAT registration receipts (036/037, IAE, IVA, OSS) | External secure folder | Founder | Pending | Annual |
| EV-L1-04 | Bank account certificate | External secure folder | Founder | Pending | Annual |
| EV-L1-05 | Play organization account and D-U-N-S evidence | External secure folder | Founder | Pending | On account change |
| EV-L1-06 | License-test purchase and settlement report | External secure folder | Founder | Pending | On billing change |
| EV-L1-07 | Dry-run invoice and gestoría acceptance | External secure folder | Founder | Pending | On invoicing change |
| EV-L1-08 | Billing configuration, backup card, budget alerts | External secure folder | Founder | Pending | Quarterly |
| EV-L1-09 | Retention schedule and restore report | External secure folder | Founder | Pending | Annual |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L1-01 to C-L1-10 appear only here |
| LIS-2 Boundary integrity | Pending | Exclusions listed in README Section 2 |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T3 and T5 are manual drills; T4 is external review |
| LIS-5 Operability | Pending | Founder owns; gestoría and asesor named in P1 |
| LIS-6 Public-claim consistency | Pending | Entity data must match L2/L3 public artifacts |
| LIS-7 Independence | Pending | No shared inputs are consumed; SC1 is provided |
| LIS-8 Regression | Pending | Entity or billing changes require consumer confirmation |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 evidence collection
