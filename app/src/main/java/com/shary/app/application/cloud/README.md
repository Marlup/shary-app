# Cloud Application Module

## Purpose

This module contains application-layer logic for inbound cloud import planning
and outbound per-recipient delivery state.

## Files and responsibilities

### `CloudFieldImportPlanner.kt`

- Defines `IncomingCloudField`, `CloudImportDiff`, and
  `CloudFieldImportPlan`.
- Parses incoming JSON payload strings into normalized field candidates.
- Compares incoming keys against existing local fields.
- Produces an import plan that separates:
  - fields to create;
  - duplicate keys;
  - changed keys;
  - unchanged keys;
  - ignored keys.
- Provides `previewKeys(...)` for lightweight key previews without a full
  import.

### `CloudDeliveryController.kt`

- Owns the `Review -> Sending -> Receipt` transition for one review route.
- Claims initial and retry attempts synchronously so rapid taps cannot launch
  duplicate transport work.
- Reduces normalized per-recipient backend results into `SENT`, `PARTIAL`, or
  `FAILED` while preserving recipient display order.
- Treats `SUCCESS`, `STORED`, and the backend's active-duplicate `EXISTS`
  result as acknowledged delivery.
- Retains earlier successes and returns only failed recipients for an explicit
  retry.
- Restores a validated outgoing-Request receipt loaded from local history and
  recomputes its summary instead of trusting stored aggregate text.
- Keeps terminal receipts until the route explicitly resets them; the
  controller itself never stores payload field values.

### `RequestDeliveryHistory.kt`

- Maps terminal outgoing-Request and received-response cloud receipts to the
  existing stable-ID Request records.
- Persists channel, update time, and every recipient result for `Sent`,
  `Partial`, and `Failed` outcomes.
- Recreates the controller receipt when a route/ViewModel obtains the same
  stable Request ID so Retry still targets only failed recipients.
- Does not persist arbitrary Field-share receipts because those flows do not
  have a durable local delivery-history identity.

## Inputs and outputs

- Inputs:
  - raw JSON strings from cloud payloads;
  - current local `FieldDomain` list.
- Outputs:
  - a deterministic import plan and diff summary for review/UI decisions.
  - an outbound delivery state and per-recipient receipt;
  - persisted outgoing-Request and received-response delivery metadata keyed
    by stable request ID.

## Dependencies

- `FieldDomain`
- `Tag`
- `StatusDataSentDb`
- `kotlinx.serialization.json`
- Kotlin `StateFlow`

## Agent notes

- This module plans imports; it does not commit field writes by itself.
- Review-diff behavior belongs here when the change is about interpreting cloud
  payload contents before the user accepts them.
- `CloudViewModel` owns a delivery controller per route and remains responsible
  for invoking transport and persisting every terminal outgoing-Request or
  received-response result, including partial and failed attempts.
- A missing result for an attempted recipient becomes `ERROR`; unexpected
  result-map recipients are ignored.
- Outgoing-Request and received-response receipts are persisted in
  `RequestRepository`; one-off Field shares remain route-local until they gain
  a stable local history identity.

## Update triggers

Update this README when:

- the JSON input shape changes;
- diff categories change;
- import planning starts applying writes directly;
- acknowledgement status semantics or outbound retry ownership changes.
