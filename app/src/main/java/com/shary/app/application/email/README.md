# Email sharing application module

## Purpose

Own the process-local state transition from email attachment preparation to an
Android share-sheet handoff without claiming that the message was sent or
delivered.

## State boundary

`EmailShareController` allows one preparation or chooser-launch claim at a
time. The terminal success state is `ShareSheetOpened`; there is intentionally
no `Sent` or `Delivered` state because Android's share intent supplies neither
receipt.

The controller stores attachment identity, expiry, and share kind only. The
prepared `Intent` remains in `EmailViewModel` until it is claimed exactly once.
Infrastructure owns the readable JSON file and URI grant.

## Attachment lifecycle

- `EmailAttachmentStore` adopts generated JSON into the dedicated
  `cache/email-attachments/` directory and rejects non-UUID lookup paths.
- `EmailAttachmentCleanupCoordinator` schedules deletion after 24 hours while
  the process is alive.
- Application startup sweeps expired files and reschedules active leases, so an
  attachment that expires while Shary is not running is removed on the next
  app start.
- A chooser-launch failure deletes the prepared attachment immediately.
- `FileProvider` exposes only the dedicated attachment directory.

The attachment is readable by the selected sharing app. With multiple
recipients, all selected addresses are supplied in `To` and may be visible to
each recipient. UI copy must disclose both facts.

## Validation

```powershell
.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.shary.app.application.email.EmailShareControllerTest" `
  --tests "com.shary.app.infrastructure.services.email.EmailAttachmentStoreTest" `
  --tests "com.shary.app.infrastructure.services.email.EmailAttachmentCleanupCoordinatorTest"
```

## Update triggers

Update this file when share-sheet state, attachment retention, URI exposure, or
the boundary between preparation and delivery acknowledgement changes.
