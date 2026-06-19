# CODEX.md

Codex repo policy is canonicalized in `AGENT.md`.

Rules:
- Do not duplicate policy text here.
- Load `AGENT.md` first at session init.
- Treat this file as a thin adapter for Codex-specific bootstrapping only.

Session-init enforcement:
- `tools/codex/session-init.ps1`
- `tools/codex/session-init.manifest.json`
