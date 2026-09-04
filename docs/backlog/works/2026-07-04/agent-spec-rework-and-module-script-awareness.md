# Agent spec rework and module/script awareness

## Scope

Rework the `sharydemo` agent-facing documentation so the repository keeps
general governance in Enhanced-BRAIN while gaining repo-specific retrieval
guides, module-awareness docs, and script-contract docs that agents can use
without re-reading source files every time.

## Change list

1. Added a root `README.md` that points agents to the canonical repo policy,
   docs index, and new agent-doc surfaces.
2. Added `docs/ai/README.md` as the repo-local agent documentation hub.
3. Added `docs/ai/SHARY_AGENT_REPO_SPEC.md` to explain what stays inherited
   from Enhanced-BRAIN versus what remains repo-specific in `sharydemo`.
4. Added `docs/ai/SCRIPT_CATALOG.md` with root, MCP, and Codex script
   summaries.
5. Added `README.md` files under
   `app/src/main/java/com/shary/app/application/` and each current
   application submodule folder.
6. Expanded `mcp-testing/real/README.md` with per-script contracts.
7. Reworked `mcp-testing/legacy-protocol/README.md` into a current
   legacy/development protocol-harness reference with per-script contracts.
8. Added `tools/codex/README.md` for the session-init tooling.
9. Updated root `AGENTS.md`, `CODEX.md`, `CLAUDE.md`, `docs/ai/SESSION_INIT.md`,
   `docs/README.md`, and `tools/codex/session-init.manifest.json` so the live
   policy surface consistently uses `AGENTS.md` and points at the new
   retrieval/docs layer.

## Plan source

No prior repo-local plan file existed for this request.

## Viability/complexity per change

1. Repo-local agent-spec layer: medium complexity because it must stay aligned
   with existing `AGENTS.md`, active backlog routing, and the current cloud
   MCP product boundary.
2. Module-awareness docs: low to medium complexity because the surfaces are
   limited but need stable headings and update rules.
3. Script-contract docs: medium complexity because they must summarize
   parameters, inputs, outputs, and side effects from several PowerShell and
   Python entrypoints without drifting from the code.

## Design decisions

- Keep general record-task governance, case policy, and librarian skill design
  in Enhanced-BRAIN; only import the minimum repo-specific instructions into
  `sharydemo`.
- Add retrieval-oriented docs with stable headings so agents can target them
  directly with bounded Markdown retrieval instead of loading large mixed docs.
- Add explicit update rules that require refreshing the relevant README or
  AGENTS file whenever a module boundary or script contract changes.

## Implementation plan

1. Review current root Markdown and active backlog notes that define the agent,
   MCP, and repository-routing baseline.
2. Add a repo-specific agent documentation layer under the main docs tree.
3. Add `README.md` files to the application module folders under
   `app/src/main/java/com/shary/app/application/`.
4. Expand script documentation for `mcp-testing` and `tools/codex`, and add a
   durable script catalog for the remaining root Python entrypoint.
5. Update discovery and policy files so the new docs become part of the normal
   agent path.

## Validation plan

1. Review the new docs for consistency with root `AGENTS.md`,
   `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md`, and
   `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`.
2. Run `git diff --check` in `sharydemo`.
3. Refresh `tools/codex/session-init.manifest.json` if the root policy files
   change.

## Result summary

Implemented. The repo now has a dedicated repo-specific agent-doc layer that
keeps general governance in Enhanced-BRAIN while making `sharydemo` easier to
operate through direct Markdown retrieval. Application-module awareness and
script-contract docs now exist in the nearest relevant folders, and the active
session-init/policy references were aligned to the real `AGENTS.md` filename.

Validation results:

- Reviewed the new docs against the active repo policy and MCP/product docs.
- `git diff --check` passed.
- `tools/codex/session-init.ps1` passed with `policy_version=2026-07-04.1`.
- `tools/codex/update-manifest.ps1` failed with an `UnauthorizedAccessException`
  when trying to overwrite `tools/codex/session-init.manifest.json`, so the
  manifest was refreshed manually in the tracked file instead.
