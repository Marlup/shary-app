# L5 — Reliability & Continuity

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-backup-ttl-and-monitoring.md](phase-1-backup-ttl-and-monitoring.md)

## 1. Purpose

Keep the cloud relay recoverable, observable, and able to shut down or transfer
user data lawfully. This Line owns the operational resilience and the
end-of-life protocols that the contract and privacy policy disclose.

## 2. Scope

### Owned components

- C-L5-01 Firestore backups, point-in-time recovery, and retention.
- C-L5-02 Restore drills and RTO/RPO evidence.
- C-L5-03 EU region selection and documentation.
- C-L5-04 Monitoring, dashboards, and alerting.
- C-L5-05 Status page and in-app cloud status.
- C-L5-06 Incident runbooks and severity model.
- C-L5-07 Existential-risk controls: signing key, billing lapse, project
  ownership, MFA.
- C-L5-08 Vendor lock-in assessment and export path.
- C-L5-09 Five continuity protocols: outage, breach, solvent wind-down,
  insolvency, key compromise.
- C-L5-10 Data-stewardship clause and named trustee.
- C-L5-11 Business-records backup for continuity.
- C-L5-12 Scheduler/TTL correctness and guard checks.

### Explicit exclusions

- Breach notification law and the breach register belong to L2.
- Security hardening, identity, and rate-limit atomicity belong to L6.
- Support communication and SLA belong to L8.
- Store deletion disclosure belongs to L3.
- Accounting record retention belongs to L1.

### Interfaces

- Consumes SC7 breach procedure from L2.
- Consumes SC6 deletion behavior from L6.
- Provides SC5 retention/TTL values to L2 and L3.
- Provides SC9 stewardship clause to L2 and L3.
- Consumes SC10 records register from L1 and L2.
- Consumes SC8 support contact from L8 for incident communication.
- Consumes SC13 Android backup/device-transfer decision from L6 for local
  continuity.

## 3. Viability definition

L5 is viable when:

1. a Firestore restore drill meets RTO 4 hours and RPO 24 hours;
2. TTL and the scheduler hard-delete expired data and reject unauthenticated
   scheduler calls;
3. alerts fire for error rate, latency, function failures, scheduler failures,
   and quota burn;
4. the signing key, billing lapse, project ownership, and export risks have
   tested controls;
5. all five continuity protocols have a dated tabletop record;
6. the stewardship clause names a trustee who has accepted in writing.

## 4. Section 8 coverage

- 8.13 Succession and data stewardship: primary owner.
- 8.12 Business-records backup: secondary owner for continuity copies.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | Backups, restore, and TTL | L5-V1, L5-V2, L5-V3, L5-V13 |
| P2 | Monitoring and incident response | L5-V4, L5-V5, L5-V6, L5-V10 |
| P3 | Existential-risk controls | L5-V7, L5-V11 |
| P4 | Continuity protocols | L5-V8, L5-V9, L5-V12 |

## 6. Official sources

- Firebase documentation: Firestore backups, point-in-time recovery, scheduled
  backups, and region selection.
- Google Cloud Architecture Framework and disaster-recovery guidance.
- GDPR Arts. 32-34 for security, breach handling, and notification.
- Real Decreto Legislativo 1/2020 (TRLC), insolvency.
- ISO 22301 business-continuity guidance as good practice, not a certification
  requirement.
- Google Play App Signing documentation.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- The founder cannot be the only person who can notify users or delete data.
  Name an external trustee, document the procedure, and escrow the credentials.
  If the trustee cannot operate Firebase, name a technical executor and keep
  the legal authority with the trustee.
- Do not promise 24/7 on-call. Define support hours and an incident
  communication path that one person can sustain. RTO 4h applies during
  published support hours; outside them, the target is best effort up to 24h.
- The data is transient by design, but the identity and business records are
  not. Back up both.
- A failed card on the GCP project can silently stop the service. Keep a backup
  payment method and budget alerts.

## 8. Open decisions

- Confirm the trustee (abogado or asesor) and the scope of their authority.
- Confirm the backup retention period against the privacy policy.
- Confirm whether a second region or a cross-project backup is justified at
  launch scale.
- Confirm the status-page provider and the incident communication channel.
