# L2 — Privacy & Data Governance

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-data-map-and-ropa.md](phase-1-data-map-and-ropa.md)

## 1. Purpose

Make Shary's data processing lawful, documented, and operable. This Line owns
the privacy artifacts and the rights/breach machinery that the store listing,
the contract, and the security work depend on.

## 2. Scope

### Owned components

- C-L2-01 Data inventory and data-flow map.
- C-L2-02 Records of Processing Activities (ROPA, GDPR Art. 30).
- C-L2-03 Legal-basis register (GDPR Arts. 6 and 9).
- C-L2-04 Bilingual Privacy Policy (Spanish and English).
- C-L2-05 Data Processing Agreement with Google/Firebase.
- C-L2-06 DPIA for the biometric gate, MCP/agent access, and E2E key custody.
- C-L2-07 Retention schedule.
- C-L2-08 Data-subject rights runbook and surfaces.
- C-L2-09 Erasure-when-keys-lost policy; implementation belongs to L6.
- C-L2-10 Breach procedure, breach register, and 72-hour notification template.
- C-L2-11 Subprocessor list.
- C-L2-12 Age and consent handling, including the Spanish age-14 threshold.
- C-L2-13 Data-protection records retention and backup.

### Explicit exclusions

- ToS, withdrawal, refunds, and complaint terms belong to L3.
- Deletion and erasure implementation belongs to L6.
- Analytics tooling and consent banner implementation belong to L8.
- Trust page and public security posture belong to L7.
- Insurance and trademark belong to L7.

### Interfaces

- Consumes SC1 entity identity from L1.
- Consumes SC5 retention/TTL values from L5.
- Consumes SC6 erasure behavior from L6.
- Consumes SC8 support contact from L8.
- Consumes SC13 Android backup/device-transfer policy from L6.
- Provides SC2 ROPA and data map to L3 and L8.
- Provides SC3 Privacy Policy URL to all public surfaces.
- Provides SC7 breach procedure to L5.
- Provides SC12 subprocessor list to L7.

## 3. Viability definition

L2 is viable when:

1. every personal-data flow is mapped to a purpose, legal basis, retention
   period, and processor;
2. the Privacy Policy, DPA, and DPIA are published or signed and match the
   implementation;
3. an access/export request and an erasure request can be executed within one
   month, including the key-loss path;
4. a simulated breach can be assessed, recorded, and notified within 72 hours
   where required;
5. Android backup and device transfer are either disabled for Shary data or
   fully disclosed and lawful.

## 4. Section 8 coverage

- 8.12 Business-records backup: primary owner for ROPA, DPA, breach register,
  and other data-protection records.
- 8.13 Succession and data stewardship: secondary owner for the privacy-policy
  disclosure of the stewardship clause.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Data map and role model | L2-V1, L2-V2 |
| P2 | Privacy artifacts | L2-V3, L2-V4, L2-V5, L2-V11, L2-V13 |
| P3 | Rights and breach operations | L2-V6, L2-V7, L2-V8, L2-V9 |
| P4 | Publication and verification | L2-V10, L2-V12 |

## 6. Official sources

- Regulation (EU) 2016/679 (GDPR), especially Arts. 5, 6, 9, 12-22, 28, 30,
  32-35.
- Ley Orgánica 3/2018 (LOPDGDD), including the age-14 consent threshold.
- AEPD guidance on DPIA, breach notification, and data-subject rights.
- EDPB guidelines on breach notification, DPIA, and transparency.
- Google/Firebase Data Processing and Security Terms.
- Firebase documentation on EU regions and data location.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- The abogado review is mandatory evidence, not optional advice. Budget for it
  in D4.
- The founder cannot self-certify the DPIA. Use an external reviewer or a
  documented independent challenge session.
- The Privacy Policy must disclose the zero-knowledge key custody model and the
  consequence of key loss in plain language.
- Keep the ROPA and breach register outside the repo if they contain personal
  data; the repo stores the register reference only.

## 8. Open decisions

- Confirm whether a DPO is required or merely good practice at launch scale.
- Confirm the retention period for support emails and breach records.
- Confirm the transfer mechanism if any processing leaves the EU region.
- Confirm the age-verification method and whether it is self-declaration.
