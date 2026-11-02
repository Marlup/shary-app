# Ops scripts

Read-only operational checks for Shary infrastructure. These scripts do not
mutate cloud resources and do not contain credentials.

## `verify-gcp-readiness.ps1`

Checks the Firebase/GCP reliability prerequisites without changing anything:

- active `gcloud` account;
- Firestore location (`europe-southwest1`);
- Firestore point-in-time recovery (PITR);
- Firestore backup schedule and backup list;
- Cloud Scheduler cleanup job state and target;
- Firestore TTL policies for the MCP `delete_after` collections.

Usage:

```powershell
.\tools\ops\verify-gcp-readiness.ps1
```

Optional parameters:

```powershell
.\tools\ops\verify-gcp-readiness.ps1 `
  -ProjectId shary-21b61 `
  -Region europe-southwest1 `
  -SchedulerLocation europe-west1 `
  -SchedulerJob cleanup-expired-payloads
```

Exit code `0` means every required check passed. Exit code `1` means at least one
required check failed; warnings do not fail the run.

The script only reads. Enabling backups, PITR, TTL, or the scheduler is documented
in `docs/backlog/works/2026-10-31/deferred-actions.md`.
