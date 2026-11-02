# L5 Phase 1 — Backup, TTL, and Monitoring Runbook

Status: In progress (console actions pending)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L5 — Reliability & Continuity](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

This runbook records what exists in code, what must be enabled in the Google
Cloud/Firebase console, and how to prove restore, TTL, and alerting. It does not
claim that any console action has been performed.

## 1. Data classification

| Class | Collections | Backup need |
|---|---|---|
| Non-transient identity | `identities_v2` | Required; losing it strands cloud identities |
| Non-transient MCP | `mcp_oauth_clients_v1`, `mcp_agent_profiles_v1`, `mcp_vault_grants_v1`, `mcp_devices_v1` | Required; grants/devices are not rebuildable from devices alone |
| Transient relay | `payloads`, `requests` | Useful for consistency; expires in 7-14 days |
| Transient decisions | `request_decisions_v1` | Useful for consistency; expires with the request |
| Short-lived security | `identity_rotate_nonces_v2`, `rate_limits_v1`, MCP OAuth requests/codes/refresh/consumptions, `mcp_operations_v1`, `mcp_grant_invitations_v1`, `mcp_rate_limits_v1` | Not required; TTL cleanup |
| Managed by Google | Firebase Auth, FCM, Cloud KMS, Cloud Logging, Google Play | Google-managed; verify retention and export |

## 2. TTL inventory

| Collection | Field | Retention | Cleanup mechanism | Verified? |
|---|---|---|---|---|
| `payloads` | `expires_at` | 7 days default; 14 days max | `clean_expired_docs` scheduler | Code yes; console pending |
| `requests` | `expires_at` | 7 days default; 14 days max | `clean_expired_docs` scheduler | Code yes; console pending |
| `request_decisions_v1` | `expires_at` | Aligned to request | `clean_expired_docs` scheduler | Code yes; console pending |
| `identity_rotate_nonces_v2` | `expires_at` | 5 minutes | `clean_expired_docs` scheduler | Code yes; console pending |
| `rate_limits_v1` | `expires_at` | Window plus two windows | `clean_expired_docs` scheduler | Code yes; console pending |
| `mcp_oauth_requests_v1` | `delete_after` | About 10 minutes | Firestore TTL policy | README requires; console pending |
| `mcp_oauth_codes_v1` | `delete_after` | About 10 minutes | Firestore TTL policy | README requires; console pending |
| `mcp_oauth_refresh_tokens_v1` | `delete_after` | 30 days | Firestore TTL policy | README requires; console pending |
| `mcp_oauth_consumptions_v1` | `delete_after` | 30 days | Firestore TTL policy | README requires; console pending |
| `mcp_operations_v1` | `delete_after` | 10 minutes plus 5 minutes | Firestore TTL policy | README requires; console pending |
| `mcp_grant_invitations_v1` | `delete_after` | About 10 minutes | Firestore TTL policy | README requires; console pending |
| `mcp_rate_limits_v1` | `delete_after` | Short window | Firestore TTL policy | README requires; console pending |
| `identities_v2` | None | Until deletion | Explicit delete only | Code yes |
| `mcp_oauth_clients_v1` | None | Persistent | Manual/review | Code yes |
| `mcp_agent_profiles_v1` | None | Persistent | Manual/review | Code yes |
| `mcp_vault_grants_v1` | `expires_at` | 30 days, renewable | Status/expiry; no TTL observed | Needs review |
| `mcp_devices_v1` | None | Until device removal | Manual/review | Code yes |

Two cleanup mechanisms exist and must both be verified:

1. The `clean_expired_docs` Cloud Function, invoked by Cloud Scheduler with an
   OIDC token, deletes docs where `expires_at <= now`.
2. Firestore TTL policies on `delete_after` for the MCP collections.

## 3. Region and scheduler facts

Code and docs state:

- Functions default region: `europe-southwest1` (Madrid).
- Firestore location: selected at database creation; verify it is
  `europe-southwest1` before claiming EU-only storage.
- Cloud Scheduler does not expose `europe-southwest1`; the runbook uses
  `europe-west1` and targets the Madrid function URL.
- Scheduler OIDC service account:
  `expiration-cleanup-scheduler@shary-21b61.iam.gserviceaccount.com`.
- Scheduler audience:
  `https://europe-southwest1-shary-21b61.cloudfunctions.net/clean_expired_docs`.

Verification commands (read-only, run before the gate):

```powershell
gcloud firestore databases describe `
  --project=shary-21b61 `
  --database='(default)' `
  --format='value(locationId,type)'

gcloud scheduler jobs describe <cleanup-job> `
  --location=europe-west1 `
  --project=shary-21b61
```

Expected: Firestore location `europe-southwest1`; scheduler target is the Madrid
function; OIDC service account matches the runtime env vars.

### Verified state — 2026-11-01

Read-only checks with the authenticated `gcloud` CLI found:

| Check | Result |
|---|---|
| Firestore location | `europe-southwest1` confirmed |
| Firestore edition | `STANDARD` |
| Firestore backups | No backup schedule; no completed backup listed |
| Scheduler job | `cleanup-expired-payloads` is **PAUSED** |
| Scheduler schedule | `0 0 * * *` (daily at midnight) |
| Scheduler target | `https://europe-southwest1-shary-21b61.cloudfunctions.net/clean_expired_docs` |
| PITR | `POINT_IN_TIME_RECOVERY_DISABLED` |
| MCP TTL policies | All seven `delete_after` policies enabled |

The scheduler being paused means expired relay data is not being cleaned.
Resume it only after confirming the OIDC service account and audience, then run
the TTL test.

## 4. Backup policy proposal

| Item | Proposal | Notes |
|---|---|---|
| Firestore scheduled backups | Enabled daily | Retention 30 days |
| Point-in-time recovery | Enabled | 7-day window |
| Backup location | Same EU region | Verify supported location |
| Restore target | Separate test database or project | Never restore over production |
| Restore drill | Quarterly | Record RTO/RPO |
| Backup access | Least privilege, MFA | Founder only, trustee instructions escrowed |
| Privacy disclosure | State the 30-day backup retention | L2 policy update |

The 30-day retention is a proposal; the abogado and asesor must confirm it
against the privacy policy and record-retention schedule.

## 5. Restore runbook

Preconditions:

- GCP project owner access with MFA.
- Firestore backups and PITR enabled.
- A clean test database or project.
- The backup ID or snapshot time.

Steps:

1. Record the incident start time and the chosen restore point.
2. List available backups and confirm the location.
3. Restore to the test database, not production.
4. Validate collection counts for `identities_v2`, `mcp_vault_grants_v1`,
   `mcp_devices_v1`, and the transient collections.
5. Validate an identity lookup and a grant lookup against the restored data.
6. Validate that TTL fields and indexes exist.
7. Measure elapsed time; compare with RTO 4h (support hours) and RPO 24h.
8. Record the drill report and any data loss.
9. Destroy the test database after validation.

Example commands (verify against current `gcloud` docs before use):

```powershell
gcloud firestore backups list --location=europe-southwest1 --project=shary-21b61

gcloud firestore databases restore `
  --source-backup=<backup-name> `
  --destination-database=<test-database> `
  --project=shary-21b61
```

Do not run a restore against the production database without a written rollback
plan and a second review.

## 6. Monitoring and alert specification

| Signal | Threshold | Window | Route |
|---|---|---|---|
| Function 5xx rate | > 2% | 5 minutes | Email/SMS |
| Function p95 latency | > 2 seconds | 10 minutes | Email |
| Cold-start ratio | > 50% | 15 minutes | Email (warning) |
| Auth failure spike | > 20 failures/minute | 5 minutes | Email/SMS |
| Scheduler last success | Older than 26 hours | 1 hour check | Email/SMS |
| Firestore daily reads/writes | > 80% of expected quota | Daily | Email |
| Firestore delete failures | Any | 5 minutes | Email |
| Billing budget | 50%, 80%, 100% | Monthly | Email/SMS |
| Backup/PITR disabled | Any | Daily | Email/SMS |
| MCP health endpoint | Non-200 | 5 minutes | Email/SMS |
| Uptime check `/ping` | Non-200 | 5 minutes | Email/SMS |

Dashboards:

- Cloud Functions: requests, errors, latency, instances.
- Firestore: reads, writes, deletes, storage, document counts.
- Scheduler: success/failure, last run.
- Billing: budget vs actual.
- MCP: OAuth errors, operation queue, device wake failures.

## 7. TTL and scheduler verification plan

1. Create a test document in `payloads` with `expires_at` in the past.
2. Invoke `clean_expired_docs` with a valid OIDC token.
3. Confirm the document is deleted.
4. Invoke the function without a token and confirm `403`.
5. Confirm `identities_v2` is not deleted.
6. Create a test document in an MCP collection with `delete_after` in the past;
   confirm the Firestore TTL policy deletes it within the documented window.
7. Record the results in EV-L5-13.

## 8. Existential-risk controls

| Risk | Control | Evidence |
|---|---|---|
| Android signing key loss | Play App Signing; offline key backup | EV-L5-07 |
| GCP billing lapse | Backup payment method; budget alerts | EV-L5-07 |
| Firebase project loss | MFA on owner accounts; recovery contacts | EV-L5-07 |
| Region lock-in | Verify Firestore location; document migration limits | EV-L5-03 |
| Vendor lock-in | Export path and migration note | EV-L5-11 |
| Founder unavailability | Trustee plus technical executor; escrowed credentials | EV-L5-09 |
| Android backup exposure | SC13 applied: backup disabled; encrypted export/import implemented | Applied |

## 9. Gaps and blockers

1. No Firestore backup schedule exists, and no completed backup is listed.
2. PITR is disabled.
3. Firestore location is confirmed as `europe-southwest1`.
4. The cleanup scheduler is PAUSED; scheduled cleanup is not running.
5. The seven MCP Firestore TTL policies are enabled, but the paused scheduler
   still blocks the main collection cleanup.
6. No alerting, budget alerts, or uptime checks evidenced.
7. No restore drill has been run.
8. No incident runbook has been exercised.
9. No status page.
10. No trustee acceptance.
11. Android backup is disabled and encrypted export/import is implemented; the
    policy/Data Safety update is still pending (SC13).
12. Log retention and backup retention need L2 sign-off.

## 10. External actions

| Action | Owner | Status |
|---|---|---|
| Verify Firestore location | Founder | Done — `europe-southwest1` |
| Enable PITR | Founder | Pending — currently disabled |
| Create the daily backup schedule | Founder | Pending — none exists |
| Resume the paused cleanup scheduler | Founder | Pending — currently PAUSED |
| Verify OIDC service account and audience | Founder | Pending |
| Configure Firestore TTL policies | Founder | Done — seven policies enabled |
| Run `tools/ops/verify-gcp-readiness.ps1` | Founder | Done — read-only check works |
| Configure monitoring and alerts | Founder | Pending |
| Add budget alerts and backup payment method | Founder | Pending |
| Run the first restore drill | Founder | Pending |
| Run the TTL/scheduler test | Founder | Pending |
| Name the trustee and technical executor | Founder | Pending |

## 11. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L5-V1 Backups enabled | EV-L5-01 | Pending |
| L5-V2 Restore drill | EV-L5-02 | Pending |
| L5-V3 EU region | EV-L5-03 | Pending |
| L5-V4 Monitoring | EV-L5-04 | Pending |
| L5-V5 Alerts fire | EV-L5-05 | Pending |
| L5-V6 Incident runbook | EV-L5-06 | Draft |
| L5-V7 Existential controls | EV-L5-07 | Pending |
| L5-V8 Five protocols | EV-L5-08 | Pending |
| L5-V9 Stewardship | EV-L5-09 | Pending |
| L5-V10 Status page | EV-L5-10 | Pending |
| L5-V11 Vendor lock-in | EV-L5-11 | Pending |
| L5-V12 Records backup | EV-L5-12 | Pending |
| L5-V13 TTL/scheduler | EV-L5-13 | Pending |
