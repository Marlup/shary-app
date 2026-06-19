# CLAUDE.md

Claude repo policy is canonicalized in `AGENT.md`.

Rules:
- Do not duplicate policy text here.
- Load `AGENT.md` first at session init.
- Keep this file as a pointer/adapter, not an independent policy source.

Session-init enforcement:
- `tools/codex/session-init.ps1`
- `tools/codex/session-init.manifest.json`
