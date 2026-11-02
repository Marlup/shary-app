# Raw Order Backup — Start From the Top

Date: 2026-11-02

## Verbatim instruction

> start from the top

## Context

The preceding message listed zero-cost candidates in recommended order. Item 1
was the encrypted local export/import path, which mitigates the data-loss risk
created by disabling Android backup.

## Work performed

- Added the encrypted `.sharyvault` export/import flow.
- Added PBKDF2 600k + AES-256-GCM cipher and versioned container.
- Added additive import for fields, contacts, requests, tags, and safe settings.
- Excluded credentials, MCP state, connector state, and workflow drafts.
- Added Settings passphrase, file-picker, preview, and result flows.
- Added focused and full JVM tests; lint passed.

## Durable output

- Work log: [[docs/backlog/works/2026-11-02/vault-export-import.md]]
- Module doc: `app/src/main/java/com/shary/app/application/backup/README.md`
- Cipher: `app/src/main/java/com/shary/app/infrastructure/security/backup/VaultBackupCipher.kt`
- Manager: `app/src/main/java/com/shary/app/application/backup/VaultBackupManager.kt`
- Settings UI: `app/src/main/java/com/shary/app/ui/screens/settings/SettingsScreen.kt`
