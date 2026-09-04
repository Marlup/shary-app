# Codex Tooling Scripts

## Purpose

This folder contains repo-local scripts that enforce policy-file loading and
checksum integrity for Codex or other launcher wrappers.

## Script contracts

### `session-init.ps1`

- Purpose:
  - verify the required policy files listed in
    `session-init.manifest.json`;
  - optionally emit the loaded file contents as plain text or JSON prompt
    payload.
- Inputs:
  - optional `-ManifestPath`;
  - optional `-OutputMode`:
    - `text`
    - `json`
  - optional `-EmitPrompt`.
- Outputs:
  - without `-EmitPrompt`: pass/fail integrity result;
  - with `-EmitPrompt`: serialized prompt context payload.
- Side effects:
  - none beyond reading policy files and computing hashes.

### `update-manifest.ps1`

- Purpose:
  - recompute SHA-256 hashes for every required policy file and refresh the
    manifest timestamp.
- Inputs:
  - optional `-ManifestPath`.
- Outputs:
  - updated `session-init.manifest.json`.
- Side effects:
  - overwrites manifest hashes and timestamp.

## Required companion files

- `session-init.manifest.json`
  - declares `policy_version`, `required_files`, and `load_order`.

## Update triggers

Update this README, `docs/ai/SESSION_INIT.md`, and
`docs/ai/SCRIPT_CATALOG.md` when:

- the load order changes;
- a required file is renamed;
- the emitted payload format changes.
