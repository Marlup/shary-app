# L5 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Backup and monitoring criteria require configuration evidence plus a drill.
Continuity criteria require a dated tabletop record with participants and
decisions. A runbook that has never been executed does not pass.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L5-V1 | Backups are enabled | Document review | Firestore scheduled backups and PITR are enabled with a defined retention | EV-L5-01 | Firebase backup docs |
| L5-V2 | Restore drill meets RTO/RPO | Manual drill | Restore to a clean project within 4 hours and 24-hour RPO | EV-L5-02 | Firebase PITR docs |
| L5-V3 | EU region is selected and documented | Document review | Firestore and Functions region are recorded; no unintended transfer | EV-L5-03 | Firebase region docs; GDPR |
| L5-V4 | Monitoring covers critical signals | Document review | Error rate, latency, function failures, scheduler failures, and quota burn are monitored | EV-L5-04 | Google Cloud monitoring docs |
| L5-V5 | Alerts fire | Manual drill | A synthetic failure triggers the expected alert and route | EV-L5-05 | Google Cloud monitoring docs |
| L5-V6 | Incident runbook is operable | Manual drill | Severity, roles, communication, rollback, and postmortem steps are executable | EV-L5-06 | SRE practice; GDPR Art. 33 |
| L5-V7 | Existential-risk controls are tested | Document review + drill | Signing key, billing lapse, project MFA, and export controls have evidence | EV-L5-07 | Play App Signing; Google Cloud docs |
| L5-V8 | Five continuity protocols are exercised | Tabletop drills | Outage, breach, wind-down, insolvency, and key compromise each have a dated record | EV-L5-08 | TRLC; GDPR; Play policy |
| L5-V9 | Stewardship clause and trustee exist | Document review | Clause is published; trustee accepted in writing; handover instructions exist | EV-L5-09 | GDPR Art. 17; TRLC |
| L5-V10 | Status page works | Manual drill | Status page and in-app cloud status reflect a simulated incident | EV-L5-10 | Incident communication practice |
| L5-V11 | Vendor lock-in is mitigated | Document review | Export format and migration path are documented and tested | EV-L5-11 | Business draft Section 7 |
| L5-V12 | Business records are backed up | Manual drill | Accounting and data-protection records restore from backup | EV-L5-12 | Código de Comercio; GDPR Art. 30 |
| L5-V13 | TTL and scheduler are correct | Automated test + document review | Expired documents are hard-deleted; the scheduler guard rejects unauthorized calls; tier expiry matches L4 | EV-L5-13 | Firebase scheduler docs; L4 tier spec |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L5-T1 | Manual drill | Restore a Firestore backup to a clean test project | Data complete; elapsed time within RTO; data loss within RPO |
| L5-T2 | Manual drill | Trigger a synthetic function failure and a quota-burn condition | Both alerts fire and reach the founder |
| L5-T3 | Tabletop | Simulate a regional outage during an active share | Offline-first behavior communicated; rollback/restore decision recorded |
| L5-T4 | Tabletop | Simulate a lost signing key | Play App Signing recovery path executed or the worst case documented |
| L5-T5 | Tabletop | Simulate a failed GCP payment method | Backup payment method restores billing; users see the correct status |
| L5-T6 | Tabletop | Simulate solvent wind-down and insolvency | Notice, export, refund, deletion, and trustee handover steps recorded |
| L5-T7 | Manual drill | Export the relay data and import it into a test environment | Export is complete and readable |
| L5-T8 | Automated test | Set a document to expire, run the scheduler, then call the scheduler endpoint without the guard header | Document is hard-deleted; unauthenticated call is rejected |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L5-01 | Backup/PITR configuration | External secure folder | Founder | Pending | Quarterly |
| EV-L5-02 | Restore drill report | External secure folder | Founder | Pending | Quarterly |
| EV-L5-03 | Region configuration | External secure folder | Founder | Pending | Annual |
| EV-L5-04 | Monitoring dashboard and alert list | External secure folder | Founder | Pending | Quarterly |
| EV-L5-05 | Alert test record | External secure folder | Founder | Pending | Quarterly |
| EV-L5-06 | Incident runbook and drill record | External secure folder | Founder | Pending | Annual |
| EV-L5-07 | Existential-control register | External secure folder | Founder | Pending | Quarterly |
| EV-L5-08 | Five tabletop reports | External secure folder | Founder | Pending | Annual |
| EV-L5-09 | Stewardship clause and trustee acceptance | External secure folder | Founder | Pending | Annual |
| EV-L5-10 | Status-page test record | External secure folder | Founder | Pending | Quarterly |
| EV-L5-11 | Export format and migration test | External secure folder | Founder | Pending | Annual |
| EV-L5-12 | Records restore report | External secure folder | Founder | Pending | Annual |
| EV-L5-13 | TTL and scheduler test record | CI plus external folder | Founder | Pending | Each release |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L5-01 to C-L5-12 appear only here |
| LIS-2 Boundary integrity | Pending | Breach law, security code, support, and store disclosure are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T1-T2, T7, and T8 are drills/tests; T3-T6 are tabletops |
| LIS-5 Operability | Pending | Founder owns; trustee and L8 support are named |
| LIS-6 Public-claim consistency | Pending | RTO/RPO and retention claims must match L2/L3 |
| LIS-7 Independence | Pending | SC6, SC7, SC8, SC10, and SC13 are consumed |
| LIS-8 Regression | Pending | Any TTL, backup, or region change triggers L2/L3 review |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 restore drill
