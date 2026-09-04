# Shary Android App Repository

## Purpose

This repository contains the Android application for Shary and its
repo-local agent operating guidance.

## Agent entry points

1. Start with [AGENTS.md](AGENTS.md).
2. Use [docs/README.md](docs/README.md) for the main documentation map.
3. Use [docs/ai/CODEBASE_NAVIGATION.md](docs/ai/CODEBASE_NAVIGATION.md) for
   task-to-code, owner, test, and search shortcuts.
4. Use [docs/ai/README.md](docs/ai/README.md) for repo-specific agent
   retrieval surfaces, module awareness, and script contracts.

## Root scripts and policy files

- `AGENTS.md`: canonical repo policy.
- `CODEX.md`, `CLAUDE.md`: thin tool adapters that point back to
  `AGENTS.md`.
- `main.py`: Firebase HTTP-function entrypoint preserved in this repository;
  see `docs/ai/SCRIPT_CATALOG.md#root-scripts`.

## Update rule

When a root script, repo policy path, or major documentation entry point
changes, update this file and the linked `docs/ai` surfaces in the same pass.
