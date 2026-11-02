# Raw Order Backup — Defer Infrastructure, Continue Our-Side Automation

Date: 2026-11-01

## Verbatim instruction

> keep infrastructure in GCP and similar to defer and document the steps to doit. Continue with the our-side tasks with no costs and automated

## Interpretation

- Keep GCP and similar infrastructure actions deferred.
- Document the exact steps, costs, third parties, and triggers to execute later.
- Continue with repo-side tasks that have no cost and can be automated now.

## Work performed

- Created the deferred-actions runbook with exact GCP commands, costs, and
  triggers.
- Applied Android backup fail-closed: `allowBackup="false"` plus all-domain
  backup and data-extraction exclusions.
- Added `BackupPolicyTest` and verified it passes.
- Verified the merged manifest reports `allowBackup="false"`.
- Ran read-only GCP checks: Firestore location confirmed
  `europe-southwest1`; no backups listed; cleanup scheduler paused.
- No GCP mutation and no external service was contacted or purchased.

## Durable output

- Deferred runbook: [[docs/backlog/works/2026-10-31/deferred-actions.md]]
- Phase 1 execution log: [[docs/backlog/works/2026-10-31/phase-1-execution.md]]
- Android policy: `app/src/main/AndroidManifest.xml`,
  `app/src/main/res/xml/backup_rules.xml`,
  `app/src/main/res/xml/data_extraction_rules.xml`
- Test: `app/src/test/java/com/shary/app/security/BackupPolicyTest.kt`
