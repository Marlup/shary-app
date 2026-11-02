# Deferred Actions — Infrastructure and External Services

Date: 2026-11-01
Status: Deferred by owner; steps documented for later execution
Owner: Founder (single-owner project)
Policy: No GCP or external mutation runs until explicit approval. No cost is
incurred by this document or the current repo-side changes.

## 1. Why this exists

The owner chose to defer GCP/cloud operations and external legal/insurance
actions. This file records:

- what is deferred;
- what it blocks;
- what it costs;
- which third party is involved;
- the exact steps and commands to execute later;
- the trigger for executing each action.

Nothing here has been applied to the cloud.

## 2. Deferred register

| ID | Action | Defer until | Cost | Third party | Blocks |
|---|---|---|---|---|---|
| D1 | GCP backups, PITR, scheduler, TTL, alerts | Before public beta or first paid user | ~EUR 0-10/month | Google | L5 reliability gate |
| D2 | Google/Firebase DPA acceptance | Before public launch; can be done now free | EUR 0 | Google | EU processing claim |
| D3 | D-U-N-S + Play organization account | After S.L. and NIF exist | Free + USD 25 once | D&B, Google | Paid production listing |
| D4 | Incorporation, notary, bank, asesor | When ready to launch/charge | EUR 1.5k-3k once; EUR 60-150/month gestoría | Notary, bank, asesor | Entity identity, merchant, contracts |
| D5 | Trademark filing and insurance | Search now; file/bind 8-12 weeks before launch | Trademark EUR 150-1.2k; insurance EUR 500-1.5k/year | OEPM/EUIPO, attorney, broker | L7/launch |
| D6 | support@/security@, SLA page, security.txt | When the domain is chosen | EUR 5-15/month | Registrar/email provider | L7/L8 public trust |
| D7 | Independent security reviewer | After L6 code is implemented | EUR 1.5k-5k once | Reviewer | L6 gate |
| D8 | 20-30 pricing interviews | 6-8 weeks before launch | EUR 0 | None | L8 pricing confidence |

## 3. D1 — GCP backups, PITR, scheduler, TTL, alerts

### 3.1 Current verified state (2026-11-01)

Read-only checks with the authenticated `gcloud` CLI found:

| Check | Result |
|---|---|
| Firestore location | `europe-southwest1` confirmed |
| Firestore edition | `STANDARD` |
| Firestore backup schedule | None |
| Firestore completed backups | None listed |
| Scheduler `cleanup-expired-payloads` | **PAUSED** |
| Scheduler schedule | `0 0 * * *` daily at midnight |
| Scheduler target | `https://europe-southwest1-shary-21b61.cloudfunctions.net/clean_expired_docs` |
| PITR | `POINT_IN_TIME_RECOVERY_DISABLED` |
| MCP TTL policies | All seven `delete_after` policies enabled |

Run the read-only checker at any time:

```powershell
.\tools\ops\verify-gcp-readiness.ps1
```

The paused scheduler means expired relay data is **not** being cleaned.

### 3.2 Prerequisites

- Google Cloud project owner or equivalent permissions.
- `gcloud` authenticated to `shary-21b61`.
- Approval to mutate the project.
- A maintenance window (low traffic).

### 3.3 Execution order

Run in order; stop on the first failure.

**Step 1 — Confirm account and project**

```powershell
gcloud auth list
gcloud config get-value project
gcloud firestore databases describe `
  --project=shary-21b61 `
  --database="(default)" `
  --format="value(locationId,type)"
```

Expected: `europe-southwest1` and `FIRESTORE_NATIVE`.

**Step 2 — Enable point-in-time recovery**

```powershell
gcloud firestore databases update `
  --project=shary-21b61 `
  --database="(default)" `
  --enable-pitr
```

**Step 3 — Create a daily backup schedule**

```powershell
gcloud firestore backups schedules create `
  --project=shary-21b61 `
  --database="(default)" `
  --retention=30d `
  --recurrence=daily
```

**Step 4 — Verify backup schedule and backups**

```powershell
gcloud firestore backups schedules list `
  --project=shary-21b61 `
  --database="(default)"

gcloud firestore backups list `
  --project=shary-21b61 `
  --location=europe-southwest1
```

**Step 5 — Enable TTL on MCP `delete_after` fields**

Run once per collection group:

```powershell
gcloud firestore fields ttls update delete_after `
  --project=shary-21b61 `
  --database="(default)" `
  --collection-group=mcp_grant_invitations_v1 `
  --enable-ttl
```

Repeat for:

- `mcp_oauth_codes_v1`
- `mcp_oauth_consumptions_v1`
- `mcp_oauth_refresh_tokens_v1`
- `mcp_oauth_requests_v1`
- `mcp_operations_v1`
- `mcp_rate_limits_v1`

**Step 6 — Verify TTL**

```powershell
gcloud firestore fields ttls list --project=shary-21b61
```

**Step 7 — Verify the scheduler OIDC settings before resuming**

```powershell
gcloud scheduler jobs describe cleanup-expired-payloads `
  --project=shary-21b61 `
  --location=europe-west1 `
  --format="yaml(state,schedule,httpTarget)"
```

Confirm the OIDC service account is
`expiration-cleanup-scheduler@shary-21b61.iam.gserviceaccount.com` and the
audience is the Madrid function URL.

**Step 8 — Resume the scheduler**

```powershell
gcloud scheduler jobs resume cleanup-expired-payloads `
  --project=shary-21b61 `
  --location=europe-west1
```

**Step 9 — TTL smoke test**

1. Create a test document in `payloads` with `expires_at` in the past.
2. Trigger the scheduler job manually or wait for the next run.
3. Confirm the document is deleted.
4. Confirm an `identities_v2` document is not deleted.
5. Invoke the function without an OIDC token and confirm `403`.
6. Record the evidence in EV-L5-13.

**Step 10 — Monitoring, budget alerts, uptime checks**

The exact alert thresholds are in
[phase-1-backup-ttl-and-monitoring.md](lines/L5-reliability-and-continuity/phase-1-backup-ttl-and-monitoring.md).
Create them in Cloud Monitoring or with `gcloud monitoring` policies:

- Function 5xx rate > 2% for 5 minutes.
- p95 latency > 2 seconds for 10 minutes.
- Scheduler last success older than 26 hours.
- Firestore daily reads/writes > 80% of expected quota.
- Budget alerts at 50%, 80%, 100%.
- Backup/PITR disabled.
- MCP health and `/ping` uptime checks.

### 3.4 Restore drill

Use a separate test database or project; never restore over production.

```powershell
gcloud firestore backups list `
  --project=shary-21b61 `
  --location=europe-southwest1

gcloud firestore databases restore `
  --project=shary-21b61 `
  --source-backup=<backup-name> `
  --destination-database=<test-database>
```

Validate `identities_v2`, `mcp_vault_grants_v1`, and `mcp_devices_v1`; measure
elapsed time against RTO 4h (support hours) and RPO 24h; destroy the test
database afterwards.

### 3.5 Costs

| Item | Approximate cost |
|---|---|
| Backup storage | About USD 0.03/GiB-month; cents at launch size |
| PITR storage | About USD 0.03/GiB-month for the 7-day window |
| Cloud Scheduler | Free tier covers a few jobs |
| Cloud Monitoring alerts | Free tier; minimal |
| Restore/test database | Cents for a short drill |
| **Total** | **~EUR 0-10/month at launch** |

### 3.6 Rollback

- Disable PITR: `--no-enable-pitr`.
- Delete the backup schedule if it causes cost or retention issues.
- Pause the scheduler again if the OIDC settings are wrong.
- TTL policies can be disabled with `--disable-ttl`.

### 3.7 Trigger

Run D1 before public beta or the first paid user, and before the L5 gate. It is
not required for local development.

## 4. D2 — Google/Firebase DPA

### 4.1 What it is

Google's DPA is the **Data Processing and Security Terms**, incorporated into
the Google Cloud/Firebase terms. There is no separate signature or purchase for
the standard terms.

### 4.2 Steps

1. Sign in to Google Cloud Console as the project owner.
2. Open `https://console.cloud.google.com/terms` or Billing → Account
   management → Terms.
3. Accept the Google Cloud Platform Terms and the Data Processing and Security
   Terms.
4. Download/print the accepted terms and record the date and account.
5. Confirm the Firebase side at
   `https://firebase.google.com/terms/data-processing-terms`.
6. Store the evidence in the external secure folder as EV-L2-04.

### 4.3 Entity caveat

The project is currently under `marsunlop@gmail.com`, so the contracting party
is the individual. After the S.L. exists, move project ownership/billing to the
company and re-confirm the terms under the company. Otherwise the DPA is not
with the S.L.

### 4.4 Cost and trigger

Cost: EUR 0. Trigger: before public launch; can be done now.

## 5. D3 — D-U-N-S and Play organization account

### 5.1 D-U-N-S

1. Incorporate the S.L. and obtain the NIF first; D-U-N-S is for the legal
   entity, not the founder.
2. Request a D-U-N-S number from D&B (the basic request is free; expedited
   handling may cost).
3. Provide the legal name, address, phone, website, business activity, and
   contact.
4. Store the D-U-N-S record.

### 5.2 Play organization account

1. Create or convert the Google Play developer account to an organization.
2. Provide the S.L. legal name, NIF, address, D-U-N-S, and verification
   documents.
3. Pay the one-time USD 25 registration fee.
4. Complete the payments profile, merchant account, and tax forms.
5. If closed testing started under a personal account, transfer the app before
   production launch and verify transfer eligibility.

Cost: D-U-N-S free (expedited may cost); Play USD 25 once. Trigger: after the
S.L. and NIF exist, before paid production.

## 6. D4 — Incorporation, notary, bank, asesor

Use the L1 checklist:

[L1 Phase 1 — Entity, Tax, and Merchant Checklist](lines/L1-entity-and-payments/phase-1-entity-tax-and-merchant-checklist.md)

One-time cost: about EUR 1.5k-3k (asesor, notary, registry, bank). Monthly:
EUR 60-150 gestoría. Third parties: asesor, notary, Registro Mercantil, bank,
AEAT. Trigger: when ready to launch or charge.

## 7. D5 — Trademark and insurance

Use the L7 workbook:

[L7 Phase 1 — Trademark, Insurance, Disclosure, Accessibility](lines/L7-trust-brand-and-risk/phase-1-trademark-insurance-disclosure.md)

- Trademark search: free, can be done now.
- OEPM filing: about EUR 150-300 plus attorney.
- EUIPO filing: about EUR 850-1.2k plus attorney.
- Insurance: about EUR 500-1.5k/year (EUR 40-125/month amortized).
- Third parties: OEPM/EUIPO, attorney, insurance broker.
- Trigger: search now; file/bind 8-12 weeks before launch.

## 8. D6 — support@/security@, SLA, security.txt

### 8.1 Mailboxes

1. Choose a domain (for example `shary.app`) from a registrar.
2. Create `support@` and `security@` aliases with an email provider.
3. Publish the support SLA from L8.
4. Store the credentials in the company password manager; give the trustee
   recovery instructions.

Cost: domain about EUR 10-15/year; email about EUR 5-15/month. Third party:
registrar and email provider.

### 8.2 security.txt

Create `hosting/.well-known/security.txt` in the backend repo with:

```
Contact: mailto:security@<domain>
Expires: <date one year out>
Preferred-Languages: en, es
Canonical: https://<domain>/.well-known/security.txt
Policy: https://<domain>/security
```

Firebase Hosting currently rewrites `/.well-known/**` to the MCP function.
Static files normally take precedence, but verify after deployment. If the
rewrite wins, add a static route or a function route for `security.txt`.

Cost: EUR 0. Trigger: when the domain and mailbox exist.

## 9. D7 — Independent security reviewer

- Scope: identity, erasure when keys are lost, MCP authorization/revocation,
  rate-limit atomicity, and the Android backup boundary.
- Deliverable: written report with severity, reproduction, and closure.
- Cost: about EUR 1.5k-5k for a focused review; a full pen-test may be more.
- Trigger: after L6 P2/P3 code is implemented, before launch.
- I can prepare the scope/RFP; the owner books and pays the reviewer.

## 10. D8 — Pricing interviews

Use the L8 workbook:

[L8 Phase 1 — Support, SLA, Analytics, Growth](lines/L8-support-growth-and-analytics/phase-1-support-sla-and-analytics.md)

20-30 interviews, price ladder EUR 2.99/3.99/4.99 monthly and EUR
24.99/29.99/34.99 annual. Cost: EUR 0. Trigger: 6-8 weeks before launch. I can
draft the script; the owner conducts the interviews.

## 11. Deferral impact

Still possible with these actions deferred:

- local development, tests, and builds;
- L2/L3/L4/L6 drafting and code;
- local product validation;
- closed testing under a personal Play account, with a transfer plan.

Blocked until the relevant action runs:

- public launch and paid subscriptions;
- organization Play listing;
- contracts/policies naming the S.L.;
- DPA with the company;
- insurance;
- L5 reliability gate;
- L7 trust surface.

## 12. Execution rules

1. No GCP mutation without an explicit "run GCP" instruction.
2. Run read-only checks first and show the output.
3. Use a maintenance window and record the before/after state.
4. Do not paste secrets, tokens, or service-account JSON into docs or chat.
5. Store evidence in the external secure folder.
6. Update the relevant Line validation suite and the cross-line change log.
