# Official Source Register

Date: 2026-10-31
Status: Baseline
Owner: Founder (single-owner project)
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]

## 1. How to use

- Prefer a stable identifier (CELEX, BOE-A, ISO number) over a long URL.
- The URL is a canonical entry point; use the identifier to locate the exact
  text at the gate.
- Record the retrieval date when a source is actually used as evidence.
- If a source changes, update the affected Line's `validation-suite.md` and the
  cross-line change log.

Status values:

- **Confirmed**: retrieved or verified in this pass.
- **Identifier recorded**: the instrument and identifier are recorded, but the
  full text was not retrieved in this pass; verify at the gate.
- **Verify at gate**: a known change or ambiguity requires a fresh check.

## 2. EU sources

| Ref | Source | Identifier | Canonical URL | Used by | Status |
|---|---|---|---|---|---|
| EU-01 | GDPR | CELEX 32016R0679 | https://eur-lex.europa.eu/eli/reg/2016/679/oj | L2, L3, L5, L6 | Identifier recorded |
| EU-02 | Consumer Rights Directive | CELEX 32011L0083 | https://eur-lex.europa.eu/eli/dir/2011/83/oj | L3 | Identifier recorded |
| EU-03 | ePrivacy Directive | CELEX 32002L0058 | https://eur-lex.europa.eu/eli/dir/2002/58/oj | L8 | Identifier recorded |
| EU-04 | European Accessibility Act | CELEX 32019L0882 | https://eur-lex.europa.eu/eli/dir/2019/882/oj | L3, L7 | Verify at gate |
| EU-05 | ODR Regulation | CELEX 32013R0524 | https://eur-lex.europa.eu/eli/reg/2013/524/oj | L3 | Verify at gate |
| EU-06 | ODR repeal Regulation | CELEX 32024R3228 | https://eur-lex.europa.eu/eli/reg/2024/3228/oj | L3 | Verify at gate |
| EU-07 | VAT Directive | CELEX 32006L0112 | https://eur-lex.europa.eu/eli/dir/2006/112/oj | L1, L4 | Identifier recorded |
| EU-08 | Omnibus Directive (price reductions) | CELEX 32019L2161 | https://eur-lex.europa.eu/eli/dir/2019/2161/oj | L8 | Identifier recorded |

## 3. Spain sources

| Ref | Source | Identifier | Canonical URL | Used by | Status |
|---|---|---|---|---|---|
| ES-01 | LOPDGDD 3/2018 | BOE-A-2018-16673 | https://www.boe.es/buscar/act.php?id=BOE-A-2018-16673 | L2 | Identifier recorded |
| ES-02 | LSSI-CE 34/2002 | BOE-A-2002-13758 | https://www.boe.es/buscar/act.php?id=BOE-A-2002-13758 | L3, L8 | Identifier recorded |
| ES-03 | TRLGDCU (RDL 1/2007) | BOE-A-2007-20555 | https://www.boe.es/buscar/act.php?id=BOE-A-2007-20555 | L3, L4, L8 | Identifier recorded |
| ES-04 | TRLC (RDL 1/2020) | BOE-A-2020-4859 | https://www.boe.es/buscar/act.php?id=BOE-A-2020-4859 | L5 | Identifier recorded |
| ES-05 | Ley de Sociedades de Capital | BOE-A-2010-10544 | https://www.boe.es/buscar/act.php?id=BOE-A-2010-10544 | L1 | Identifier recorded |
| ES-06 | Código de Comercio | BOE-A-1885-6627 | https://www.boe.es/buscar/act.php?id=BOE-A-1885-6627 | L1, L5 | Identifier recorded |
| ES-07 | Ley 58/2003, General Tributaria | BOE-A-2003-23186 | https://www.boe.es/buscar/act.php?id=BOE-A-2003-23186 | L1 | Identifier recorded |
| ES-08 | Ley 50/1980, Contrato de Seguro | BOE-A-1980-22501 | https://www.boe.es/buscar/act.php?id=BOE-A-1980-22501 | L7 | Identifier recorded |
| ES-09 | Spanish EAA transposition | Ley 11/2023 (verify current scope) | https://www.boe.es/buscar/ | L7 | Verify at gate |
| ES-10 | AEAT OSS | One Stop Shop | https://sede.agenciatributaria.gob.es | L1, L4 | Identifier recorded |
| ES-11 | Registro Mercantil | Portal | https://www.registradores.org | L1 | Identifier recorded |
| ES-12 | AEPD | Agencia Española de Protección de Datos | https://www.aepd.es | L2, L8 | Identifier recorded |
| ES-13 | EDPB | European Data Protection Board | https://www.edpb.europa.eu | L2 | Identifier recorded |

## 4. Google and Firebase sources

| Ref | Source | Identifier | Canonical URL | Used by | Status |
|---|---|---|---|---|---|
| GO-01 | Google Play Policy Center | Policy hub | https://play.google.com/about/developer-content-policy/ | L3, L4 | Identifier recorded |
| GO-02 | Play account deletion requirements | Help article 13327111 | https://support.google.com/googleplay/android-developer/answer/13327111 | L3 | Confirmed 2026-10-31 |
| GO-03 | Play Data Safety | Help article 10787469 | https://support.google.com/googleplay/android-developer/answer/10787469 | L3 | Identifier recorded |
| GO-04 | Play payments policy | Help article 10281818 | https://support.google.com/googleplay/android-developer/answer/10281818 | L3, L4 | Identifier recorded |
| GO-05 | Play Billing | Android developer guide | https://developer.android.com/google/play/billing | L4 | Identifier recorded |
| GO-06 | Firestore backups | Firebase docs | https://firebase.google.com/docs/firestore/backups | L5 | Identifier recorded |
| GO-07 | Firestore PITR | Google Cloud docs | https://cloud.google.com/firestore/docs/pitr | L5 | Identifier recorded |
| GO-08 | Firebase locations | Firebase docs | https://firebase.google.com/docs/projects/locations | L5 | Identifier recorded |
| GO-09 | Firebase App Check | Firebase docs | https://firebase.google.com/docs/app-check | L6 | Identifier recorded |
| GO-10 | Google Cloud IAM | Google Cloud docs | https://cloud.google.com/iam/docs/overview | L6 | Identifier recorded |
| GO-11 | Android Auto Backup | Android developer docs | https://developer.android.com/guide/topics/data/autobackup | L2, L5, L6 | Identifier recorded |
| GO-12 | Play Console Android vitals | Play Console help | https://support.google.com/googleplay/android-developer/answer/9844487 | L8 | Identifier recorded |

## 5. Standards and professional references

| Ref | Source | Identifier | Canonical URL | Used by | Status |
|---|---|---|---|---|---|
| ST-01 | OWASP ASVS | Current stable version | https://owasp.org/www-project-application-security-verification-standard/ | L6 | Identifier recorded |
| ST-02 | NIST Digital Identity Guidelines | SP 800-63B, current revision | https://csrc.nist.gov/projects/digital-identity-guidelines | L6 | Identifier recorded |
| ST-03 | EN 301 549 | Current version | https://www.etsi.org | L7 | Verify at gate |
| ST-04 | ISO/IEC 29147 | Vulnerability disclosure | https://www.iso.org | L7 | Identifier recorded |
| ST-05 | ISO/IEC 30111 | Vulnerability handling | https://www.iso.org | L7 | Identifier recorded |
| ST-06 | ISO 22301 | Business continuity | https://www.iso.org | L5 | Identifier recorded |
| ST-07 | OEPM | Spanish trademark office | https://www.oepm.es | L7 | Identifier recorded |
| ST-08 | EUIPO | EU trademark office | https://euipo.europa.eu | L7 | Identifier recorded |
| ST-09 | OSI | Open-source licenses | https://opensource.org/licenses | L7 | Identifier recorded |
| ST-10 | SPDX | License identifiers | https://spdx.org/licenses/ | L7 | Identifier recorded |

## 6. Known evidence flags

1. **ODR.** Regulation (EU) 2024/3228 repealed Regulation (EU) 524/2013, and the
   European ODR platform closed on 20 July 2025. L3 P2 must confirm the current
   ADR/consumer-authority route with the abogado and remove stale ODR links.
2. **EAA.** Directive (EU) 2019/882 applies from 28 June 2025. The Spanish
   transposition and the microenterprise service-provider exemption must be
   confirmed in L7. A one-person project likely meets the size test, but the
   exemption is a documented decision, not an assumption.
3. **Play account deletion.** Confirmed from Play Console Help on 2026-10-31:
   if account creation exists anywhere in the app, an in-app deletion path and a
   functional web link are required, plus Data Safety deletion answers.
4. **Play Billing.** Digital subscriptions must use Play Billing; EEA
   external-offer rules change periodically. L4 P1 re-verifies the current
   policy before building.
5. **Firestore pricing.** Standard versus Enterprise edition pricing must be
   re-checked at L4 P4 before the business case is signed.
6. **EUR-Lex availability.** EUR-Lex was not reachable during the 2026-10-31
   session. The identifiers are recorded; retrieve the texts at the gate.

## 7. Source update protocol

When a source changes:

1. Update the row in this register and the retrieval status.
2. Update the affected Line's `validation-suite.md`.
3. Record the change in [cross-line.md](cross-line.md).
4. Re-run LIS-3 (evidence traceability) and LIS-6 (public-claim consistency)
   for the affected Line.
