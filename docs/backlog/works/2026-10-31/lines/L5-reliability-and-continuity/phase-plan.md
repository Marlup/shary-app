# L5 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: restore meets RTO/RPO, alerts fire, five protocols exercised
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — Backups and restore

Indicative duration: 1-2 weeks.

Objective: make the relay recoverable and its TTL deterministic before launch.

Work items:

1. Enable Firestore scheduled backups and point-in-time recovery.
2. Set retention and record the legal/privacy rationale with L2.
3. Confirm the EU region for Firestore and Functions.
4. Confirm TTL values: 7 days Free and up to 14 days Plus, with hard delete.
5. Verify the scheduler guard rejects calls without the expected header.
6. Restore a backup into a clean test project and measure RTO/RPO.
7. Record the restore and TTL reports and any gaps.

Exit gate: L5-V1, L5-V2, L5-V3, L5-V13.

Evidence: EV-L5-01, EV-L5-02, EV-L5-03, EV-L5-13.

Dependencies: Firebase project ownership from L1; G4 for the Plus TTL.

Risks: backups exist but never restore. Mitigation: the drill is mandatory, not
optional. A failed drill keeps the phase open.

## Phase 2 — Monitoring and incident response

Indicative duration: 2-3 weeks.

Objective: know when the service is failing and what to do first.

Work items:

1. Add dashboards for error rate, latency, function failures, scheduler
   failures, auth failures, and quota burn.
2. Configure alerts and a route that reaches the founder.
3. Publish the status page and verify the in-app cloud status.
4. Write the incident runbook: severity, containment, rollback, communication,
   postmortem.
5. Run an alert test and an incident drill.

Exit gate: L5-V4, L5-V5, L5-V6, L5-V10.

Evidence: EV-L5-04, EV-L5-05, EV-L5-06, EV-L5-10.

Dependencies: G8 support contact and communication channel from L8.

Risks: alert fatigue for one person. Mitigation: few high-signal alerts, clear
severity, and a documented ignore rule.

## Phase 3 — Existential-risk controls

Indicative duration: 1-2 weeks.

Objective: remove single points of failure that can end the service.

Work items:

1. Confirm Play App Signing and the signing-key recovery path.
2. Add a backup payment method and budget alerts to the GCP project.
3. Enforce MFA on the Firebase/GCP and Play owner accounts.
4. Document and test the export path for relay data.
5. Record the control register and the residual risks.

Exit gate: L5-V7, L5-V11.

Evidence: EV-L5-07, EV-L5-11.

Dependencies: L1 billing ownership.

Risks: account recovery depends on a personal email or phone. Mitigation: use
company-owned recovery channels and record them with the trustee.

## Phase 4 — Continuity protocols

Indicative duration: 2-3 weeks.

Objective: make the five end-of-life and compromise scenarios executable.

Work items:

1. Write and tabletop the outage protocol.
2. Write and tabletop the breach protocol with L2.
3. Write and tabletop the solvent wind-down protocol.
4. Write and tabletop the insolvency protocol under TRLC.
5. Write and tabletop the key/credential compromise protocol.
6. Name the trustee, obtain written acceptance, and publish the stewardship
   clause through L2/L3.
7. Confirm the Android backup decision (SC13) and how users recover local data
   after device loss.
8. Back up the business records and run a restore test.

Exit gate: L5-V8, L5-V9, L5-V12.

Evidence: EV-L5-08, EV-L5-09, EV-L5-12.

Dependencies: SC7 breach procedure from L2; SC10 records from L1/L2; G11
Android backup decision from L6.

Risks: the trustee is named but cannot act. Mitigation: escrow credentials and
test the handover instructions with the trustee.

## Gate review

- All L5-V criteria pass.
- LIS-1 to LIS-8 pass.
- The restore drill and all five tabletops are dated and signed.
- SC5 and SC9 are published to L2 and L3.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
