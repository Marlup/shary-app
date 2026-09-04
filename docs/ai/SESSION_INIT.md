# AI Session Init Enforcement

This repo enforces AI policy loading through:

- `AGENTS.md` (canonical policy)
- `CODEX.md` (Codex adapter, references `AGENTS.md`)
- `CLAUDE.md` (Claude adapter, references `AGENTS.md`)
- `.claude/settings.local.json` (tool-local settings)

## Startup Gate

Run before every session:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex\session-init.ps1
```

Behavior:
- Fails if a required file is missing.
- Fails if file checksum differs from `tools/codex/session-init.manifest.json`.

## Emit Prompt Payload

Plain text payload:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex\session-init.ps1 -EmitPrompt
```

JSON payload:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex\session-init.ps1 -EmitPrompt -OutputMode json
```

Use the payload as your first system/developer context in your launcher/wrapper.

## Intentional Policy Updates

After editing any required file, refresh manifest hashes:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\codex\update-manifest.ps1
```

Commit the policy file changes together with `tools/codex/session-init.manifest.json`.
