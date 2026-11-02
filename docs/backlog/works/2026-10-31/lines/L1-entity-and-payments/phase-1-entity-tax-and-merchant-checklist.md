# L1 Phase 1 — Entity, Tax, and Merchant Checklist

Status: In progress (external actions pending)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L1 — Entity & Payments](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. Decision record

| Decision | Applied value | Rationale |
|---|---|---|
| Legal form | S.L.; single-shareholder S.L.U. acceptable | Limited liability matches the closure/insolvency protocols and separates company risk from the founder |
| Registered office | Madrid | Spain-first launch; local asesor and bank access |
| Capital | EUR 1 minimum under the current LSC; confirm the sub-EUR 3,000 liability rule with the asesor | Keeps incorporation cash low; the asesor must confirm the liability consequence |
| Director | Founder as sole director and legal representative | Single-owner project |
| Company name | "Shary" or an available variant | Subject to the Registro Mercantil Central name check and the L7 trademark search |
| Accounting | External gestoría | An S.L. needs annual accounts and Registro Mercantil filings; a solo founder should not self-file |

This is a planning decision, not legal or tax advice. The asesor must confirm the
capital, liability, and filing details before the deed.

## 2. Asesor brief

Ask the asesor to answer these in writing:

1. Confirm the S.L./S.L.U. incorporation route, notary, and expected timeline.
2. Confirm the minimum capital and any liability rule when capital is below
   EUR 3,000.
3. Confirm whether the registered office can be the founder's address and what
   evidence is needed.
4. Confirm the NIF/CIF sequence: what is available before registration and what
   the bank and notary will accept.
5. Confirm the alta censal models (036 or 037), IAE, IVA, and OSS scope.
6. Confirm whether Google Play Billing payouts require OSS registration or
   whether Google handles all consumer VAT.
7. Confirm how the S.L. invoices Google for Play payouts and whether reverse
   charge or withholding applies.
8. Confirm the Impuesto de Sociedades regime and the first two periods'
   reduced rate eligibility.
9. Confirm the annual accounts and Registro Mercantil filing calendar.
10. Confirm the record-retention schedule by record type.
11. Confirm the gestoría scope, monthly fee, and handoff requirements.
12. Confirm whether any registration triggers a beneficial-ownership filing.

## 3. Incorporation checklist

| Step | Action | Output | Owner | Depends on |
|---|---|---|---|---|
| 1 | Engage the asesor and book the incorporation intake | Signed scope and fee quote | Founder | — |
| 2 | Reserve the company name at the Registro Mercantil Central | Name reservation certificate | Asesor | 1 |
| 3 | Confirm the registered office and prepare the estatutos | Draft estatutos | Asesor | 1 |
| 4 | Open the company bank account or obtain the capital deposit certificate | Bank certificate | Founder | Name and NIF plan |
| 5 | Execute the notarial deed | Notarial deed | Founder and notary | 2-4 |
| 6 | File the deed with the Registro Mercantil | Filing receipt, then registration proof | Asesor | 5 |
| 7 | Obtain the final NIF/CIF | NIF card or AEAT proof | Asesor | 6 |
| 8 | Complete alta censal, IAE, IVA, and OSS if applicable | AEAT registration receipts | Asesor | 7 |
| 9 | Confirm the company bank account is active | Bank certificate or statement | Founder | 7 |
| 10 | Store the entity evidence in the external evidence folder | EV-L1-01 to EV-L1-04 | Founder | 5-9 |

## 4. Tax checklist

| Obligation | Model or action | When | Notes |
|---|---|---|---|
| Alta censal | Modelo 036 (or 037 if eligible) | Before starting activity | Confirm eligibility with the asesor |
| IAE | Registration in the census | With alta censal | Often exempt below thresholds, but registration still applies |
| IVA | Registration and periodic returns | Before first sale | Confirm whether Play sales are outside the S.L.'s IVA scope |
| OSS | One Stop Shop registration | Only if selling directly to EU consumers | Play Billing may remove the need; verify |
| Impuesto de Sociedades | Modelo 200 | Annual | Confirm the reduced rate for the first two periods |
| Annual accounts | Deposit at the Registro Mercantil | Annual | Approve within six months of the fiscal year end; file after approval |
| Accounting records | Código de Comercio and PGC | Continuous | Retain six years |
| Tax records | Ley 58/2003 | Continuous | Retain four years, or longer if the asesor requires |

## 5. Bank, merchant, and Play checklist

| Step | Action | Output | Owner | Depends on |
|---|---|---|---|---|
| 1 | Open the company bank account in the S.L.'s name | Account and IBAN | Founder | NIF |
| 2 | Obtain a D-U-N-S number for the S.L. | D-U-N-S record | Founder | NIF and company data |
| 3 | Create the Google Play developer organization account | Organization account approved | Founder | D-U-N-S |
| 4 | Complete the Google Payments profile | Payments profile active | Founder | Organization account and bank |
| 5 | Complete merchant and tax forms | Merchant account active | Founder | Payments profile |
| 6 | Move GCP/Firebase billing to the S.L. | Company billing account | Founder | NIF and bank |
| 7 | Add a backup payment method and budget alerts | Billing resilience evidence | Founder | Billing account |
| 8 | Configure license testing and run a sandbox purchase | EV-L1-06 | Founder | Merchant account and L4 product IDs |
| 9 | Confirm the production app transfer path if a personal account exists | Transfer plan | Founder | Organization account |

Do not publish paid production under a personal Play account. If closed testing
starts before incorporation, document the transfer to the organization account
before launch and verify that the app transfer is permitted.

## 6. Documents to collect

- Name reservation certificate.
- Notarial deed and estatutos.
- Registro Mercantil filing receipt and registration proof.
- NIF/CIF and AEAT registration receipts.
- Company bank certificate.
- D-U-N-S record.
- Play organization account approval.
- Payments profile and merchant account confirmation.
- GCP billing configuration and backup payment method.
- License-test purchase and settlement report.
- Gestoría engagement letter and fee quote.
- Retention schedule signed by the asesor.

## 7. Budget and timeline

Planning ranges, not quotes:

| Item | Range | Cadence |
|---|---|---|
| Asesor incorporation support | EUR 1,200-2,500 | One-time |
| Notary and Registro Mercantil | EUR 300-600 | One-time |
| Gestoría | EUR 60-150 | Monthly |
| Company bank account | EUR 0-30 | Monthly |
| D-U-N-S | Free | One-time |
| Google Play developer account | USD 25 | One-time |
| Contingency | 15-20% | One-time |

Indicative timeline: 6-10 weeks, driven by the notary, Registro Mercantil, bank,
and D-U-N-S. Start the asesor and name reservation in week 1.

## 8. External actions register

| Action | Owner | Status | Blocking |
|---|---|---|---|
| Engage the asesor | Founder | Pending | L1 P1 |
| Reserve the name | Asesor | Pending | L1 P1 |
| Book the notary | Founder | Pending | L1 P1 |
| Open the company bank account | Founder | Pending | L1 P1 |
| File the deed | Asesor | Pending | L1 P1 |
| Obtain the NIF | Asesor | Pending | L1 P1 |
| Complete tax registrations | Asesor | Pending | L1 P2 |
| Obtain D-U-N-S | Founder | Pending | L1 P2 |
| Create the Play organization account | Founder | Pending | L1 P2 |
| Configure GCP billing | Founder | Pending | L1 P2 |

## 9. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L1-V1 Entity exists | EV-L1-01, EV-L1-02 | Pending |
| L1-V2 Tax registrations | EV-L1-03 | Pending |
| L1-V3 Bank account | EV-L1-04 | Pending |
| L1-V4 Play organization account | EV-L1-05 | Pending |
| L1-V5 Merchant and payments profile | EV-L1-06 | Pending |
| L1-V6 Invoicing workflow | EV-L1-07 | Pending |
| L1-V7 GCP billing resilience | EV-L1-08 | Pending |
| L1-V8 Records retained and restorable | EV-L1-09 | Pending |

## 10. Open questions

- Confirm the sub-EUR 3,000 capital liability rule.
- Confirm whether the founder's address can be the registered office.
- Confirm the Play payout tax treatment and whether the S.L. must invoice Google.
- Confirm the D-U-N-S lead time and the organization account review time.
- Confirm the gestoría fee and the annual accounts calendar.
