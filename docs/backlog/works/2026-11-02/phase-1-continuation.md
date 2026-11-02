# Phase 1 Continuation — Zero-Cost Work

Date: 2026-11-02
Status: Completed for the zero-cost slice
Owner: Founder (single-owner project)
Scope: Continue repo-side, zero-cost work while keeping GCP and external
cost-bearing actions deferred.

## 1. Change list

- Added a read-only GCP readiness checker:
  `tools/ops/verify-gcp-readiness.ps1`, with `tools/ops/README.md`.
- Updated `docs/ai/SCRIPT_CATALOG.md` with the new script contract.
- Added the independent security-review RFP:
  `docs/backlog/works/2026-10-31/lines/L6-security-and-identity/phase-1-independent-review-rfp.md`.
- Added the trademark search record template:
  `docs/backlog/works/2026-10-31/lines/L7-trust-brand-and-risk/phase-1-trademark-search-record.md`.
- Updated the L5 runbook and deferred-actions register with the verified GCP
  state: location and edition correct, seven MCP TTL policies enabled, PITR
  disabled, no backup schedule, scheduler paused.
- Updated the L6 ADR and L7 workbook with the new companions.

## 2. Verified state

Read-only `tools/ops/verify-gcp-readiness.ps1` output:

| Check | Result |
|---|---|
| Active gcloud account | PASS |
| Firestore location `europe-southwest1` | PASS |
| Firestore edition `STANDARD` | PASS |
| PITR | FAIL — disabled |
| Backup schedule | FAIL — none |
| Completed backup | WARN — none listed |
| Scheduler `cleanup-expired-payloads` | FAIL — paused |
| Scheduler target/schedule | PASS |
| Seven MCP `delete_after` TTL policies | PASS |

No cloud mutation was run. No external service was contacted or purchased.

## 3. Android backup status

The earlier zero-cost change remains in place:

- `android:allowBackup="false"`.
- All-domain exclusions in `backup_rules.xml` and `data_extraction_rules.xml`.
- `BackupPolicyTest` passes.
- `:app:processDebugMainManifest` passes and the merged manifest reports
  `allowBackup="false"`.

The explicit encrypted export/import path remains a follow-up.

## 4. Deferred

All cost-bearing or external actions remain deferred:

- GCP PITR, backup schedule, scheduler resume, alerts (documented in
  `deferred-actions.md`).
- DPA acceptance.
- D-U-N-S and Play organization account.
- Incorporation, notary, bank, asesor.
- Trademark filing and insurance.
- Mailboxes, domain, security.txt publication.
- Independent reviewer booking.
- Pricing interviews.

## 5. Validation

- Readiness checker runs and returns the expected failures without mutating.
- Markdown link/table/numbering/whitespace checks pass for the new docs.
- No application code changed in this continuation beyond the already-recorded
  Android backup policy.

## 6. Next zero-cost candidates

- Explicit encrypted local export/import path.
- Account-deletion UI and lost-key erasure implementation (requires the L6 ADR
  to move from proposed to approved).
- `security.txt` template and hosting file (publication needs the domain).
- Additional L2/L3 policy updates that do not require external review.
