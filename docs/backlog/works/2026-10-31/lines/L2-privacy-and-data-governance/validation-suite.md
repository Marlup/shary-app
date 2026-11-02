# L2 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

A criterion passes only when the evidence is stored and the method is complete.
Legal criteria require an abogado sign-off. Operational criteria require a
drill record. Code-related criteria require the named test from L6.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L2-V1 | Data map is complete | Document review | Every personal-data flow has source, purpose, system, recipient, and retention | EV-L2-01 | GDPR Art. 30; AEPD ROPA guidance |
| L2-V2 | Roles and legal bases are defined | Document review + external sign-off | Controller/processor roles and Art. 6 basis per purpose are recorded; Art. 9 data assessed | EV-L2-02 | GDPR Arts. 6, 9, 28 |
| L2-V3 | Privacy Policy is published | Document review | Bilingual ES/EN policy covers Arts. 13/14 content, key custody, retention, rights, and contact; URL is live and versioned | EV-L2-03 | GDPR Arts. 12-14 |
| L2-V4 | DPA is in place | Document review | Google/Firebase DPA is accepted; subprocessors are listed | EV-L2-04 | GDPR Art. 28 |
| L2-V5 | DPIA is approved | External review | DPIA covers biometric gate, MCP access, E2E keys, and residual risk; reviewer sign-off recorded | EV-L2-05 | GDPR Art. 35; AEPD DPIA guidance |
| L2-V6 | Data-subject rights are operable | Manual drill | Access/export, rectification, erasure, portability, restriction, and objection can be executed within one month | EV-L2-06 | GDPR Arts. 15-22 |
| L2-V7 | Key-loss erasure path works | Manual drill | Email-verified erase plus offline-secret rotate is documented and tested | EV-L2-07 | GDPR Art. 17; L6 tests |
| L2-V8 | Breach procedure is operable | Tabletop drill | Breach is assessed, recorded, and notified within 72 hours where required; template is ready | EV-L2-08 | GDPR Arts. 33-34 |
| L2-V9 | Retention schedule matches configuration | Document review | Policy retention matches TTL, backups, logs, and support records | EV-L2-09 | GDPR Art. 5(1)(e); L5 config |
| L2-V10 | Data-protection records are restorable | Manual drill | ROPA, DPA, DPIA, and breach register restore from backup | EV-L2-10 | GDPR Art. 30; Código de Comercio |
| L2-V11 | Age and consent handling is lawful | Document review | Age-14 handling and marketing consent are separate and documented | EV-L2-11 | LOPDGDD; GDPR Art. 8 |
| L2-V12 | Subprocessor list is available | Document review | Current list is published or available on request and matches the DPA | EV-L2-12 | GDPR Art. 28 |
| L2-V13 | Android backup is disabled or disclosed | Document review + configuration check | Manifest and backup rules match the policy: backup is disabled for Shary data, or every included category is disclosed and lawful | EV-L2-13 | Android backup docs; GDPR Art. 13 |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L2-T1 | Manual drill | Submit an access/export request through the published channel | Acknowledged and fulfilled within the stated SLA, with a complete export |
| L2-T2 | Manual drill | Submit an erasure request, including a lost-key scenario | Identity verified; data erased or key rotated per policy; confirmation recorded |
| L2-T3 | Tabletop | Simulate a lost device and a compromised backend credential | Severity assessed; AEPD notification decision recorded within 72 hours |
| L2-T4 | Document review | Compare Privacy Policy claims with TTL, deletion, encryption, and support contact | No mismatch; corrections tracked |
| L2-T5 | Manual drill | Restore the data-protection records from backup | Records readable and complete |
| L2-T6 | External review | Abogado reviews the policy, ROPA, DPA, DPIA, and breach procedure | Written sign-off or a tracked correction list |
| L2-T7 | Configuration review | Compare `AndroidManifest.xml` and backup rules with the policy | Every backed-up category is disabled or disclosed; no plaintext surprise |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L2-01 | Data-flow map | External secure folder | Founder | Pending | On data-flow change |
| EV-L2-02 | ROPA and legal-basis register | External secure folder | Founder | Pending | Quarterly |
| EV-L2-03 | Privacy Policy URL and version history | Public URL plus external folder | Founder | Pending | Each version |
| EV-L2-04 | DPA acceptance and subprocessor list | External secure folder | Founder | Pending | On vendor change |
| EV-L2-05 | DPIA and reviewer sign-off | External secure folder | Founder | Pending | Annual or on change |
| EV-L2-06 | Rights drill record | External secure folder | Founder | Pending | Annual |
| EV-L2-07 | Key-loss erasure test record | External secure folder | Founder | Pending | On flow change |
| EV-L2-08 | Breach tabletop report and template | External secure folder | Founder | Pending | Annual |
| EV-L2-09 | Retention matrix and configuration proof | External secure folder | Founder | Pending | Quarterly |
| EV-L2-10 | Records restore report | External secure folder | Founder | Pending | Annual |
| EV-L2-11 | Age/consent flow review | External secure folder | Founder | Pending | On flow change |
| EV-L2-12 | Published subprocessor list | Public URL | Founder | Pending | On vendor change |
| EV-L2-13 | Android backup decision and config evidence | External secure folder | Founder | Pending | On manifest change |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L2-01 to C-L2-13 appear only here |
| LIS-2 Boundary integrity | Pending | ToS, deletion code, analytics, and trust page are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T3 and T5 are drills; T6 is external review; T7 is a configuration review |
| LIS-5 Operability | Pending | Founder owns; abogado reviews; support channel from L8 |
| LIS-6 Public-claim consistency | Pending | Policy must match L5 TTL, L6 deletion, and the backup configuration |
| LIS-7 Independence | Pending | SC1, SC5, SC6, SC8, and SC13 are consumed |
| LIS-8 Regression | Pending | Any data-flow or vendor change triggers a re-run |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 data map
