# Request workflow application module

## Purpose

Own transport-neutral transitions between received-request handling, response
matching, and outgoing request drafting.

## State boundaries

- `activeReceivedRequest` identifies the incoming request currently being handled.
- `responseDraft` locks the incoming request identity and requester before matched
  local fields are added.
- `responseDraft.requesterId` is the authenticated sender hash from the cloud
  envelope; the embedded requester address must hash to the same identity.
- `outgoingRequestDraft` stores only requested key/alias metadata, normalized
  recipient addresses, and the stable UUID for that draft transaction; it
  never stores Field values.
- `RequestDraftEditor` returns explicit `ADDED`, `DUPLICATE`, or `INVALID`
  outcomes. Duplicate and invalid edits never mutate the draft or rotate its
  transaction ID.
- Mode changes never copy fields between these workflows.

`matchedFieldKeys` contains normalized field keys because `FieldDomain` does not
yet have a stable ID. Treat this as a containment identity, not the final
entity-identity migration. `RequestDomain.id` is persisted; legacy proto records
receive a deterministic identity derived from raw stored material.

An outgoing draft generates one UUID when requested content or recipients
change and reuses that UUID for retries until the draft changes or is cleared.
Upload puts the UUID inside encrypted request JSON and Sent history uses the
same identity.
A successful requested-key add closes the editor only after the synchronous
draft mutation is confirmed; duplicate and invalid outcomes retain the draft
input with an inline explanation.
A repeated received ID is an immutable no-op, so retry cannot replace the
original sender, fields, timestamp, or response status. Old payloads without a
canonical UUID use a deterministic containment fallback.

## Inbox decision boundary

`RequestInboxDecisionController` owns the one request-inbox decision that may
run at a time. It atomically claims the payload verification before the
ViewModel launches cloud work, so rapid duplicate or opposite actions cannot
start a second operation. After an ambiguous failure, only the same decision
for the same verification can retry; a lost response may already have finalized
that decision remotely.

Accept deliberately keeps the established local-first order: decrypt and save
the stable received-request identity, then request the backend acknowledgement.
The stable received ID makes same-decision retry a local no-op rather than a
duplicate. State records local availability and backend acknowledgement
separately so the review receipt never implies that one proves the other.

Success remains terminal until the user chooses Continue or Done. Failure keeps
the inbox item and same-decision retry available. Compose owns only the reject
confirmation and a same-frame submission latch; operation identity and retry
policy remain in the application controller.

## Restoration policy

The process-local cache remains the live source of truth, while
`RequestRepository` persists a minimal workflow snapshot for process-death
recovery. Snapshots are keyed by normalized authenticated owner address, so one
account cannot restore another account's draft.

The snapshot contains stable request IDs, requested key/alias metadata,
normalized recipient addresses, requester identity, and matched Field keys; it
never contains Field values. Requested and matched keys pass through the same
vault codec as Request data. Response restoration also requires the persisted
request ID, authenticated requester hash, and requester address to match the
immutable received Request record. After re-authentication initializes the
vault codec, `RequestViewModel` restores the matching owner snapshot and receipt
restoration resumes from the same Request ID.

Outgoing and response review navigation waits for the latest snapshot write to
finish. This prevents a process kill immediately after navigation from leaving
the route ID ahead of durable workflow identity.

## Validation

Run the pure reducer test before changing Request or Summary screen composition:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.shary.app.application.request.*" `
  --tests "com.shary.app.ui.screens.summary.SummaryFieldReviewStateTest"
```

## Update triggers

Update this file when request identity, requester binding, inbox decision retry
policy, process restoration, or received/outgoing workflow ownership changes.
