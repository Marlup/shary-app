# Shary app intention and capabilities docs

## Scope

Create two audience-specific documents that explain what Shary is, why it
exists, and what capabilities it currently offers: one executive overview and
one technical overview.

## Change list

1. Added `docs/design/SHARY_APP_EXECUTIVE_OVERVIEW.md` for non-technical or
   stakeholder-facing explanation of product intention, value, and current
   capabilities.
2. Added `docs/design/SHARY_APP_TECHNICAL_OVERVIEW.md` for engineering-facing
   explanation of the current architecture, runtime flows, security model, and
   supported capability surfaces.
3. Updated `docs/README.md` so the new app overview documents are discoverable
   from the main docs index.
4. Added the required raw-order backup and index updates under
   `docs/backlog/orders_draft/` and `docs/backlog/works/`.

## Plan source

No prior repo-local plan file existed for this request.

## Viability/complexity per change

1. Executive overview: low risk, but needed to reflect the current cloud-first
   MCP direction rather than older local-MCP documents.
2. Technical overview: medium effort because it needed reconciliation across
   active UI flows, connector internals, and the supported remote OAuth MCP
   architecture.
3. Docs index update: low risk.
4. Backlog artifact updates: low risk but required for repo compliance.

## Design decisions

- Put both overview documents in `docs/design/` because they describe product
  intent and supported behavior rather than one narrow implementation patch.
- Keep the executive document product- and capability-focused with minimal code
  detail.
- Keep the technical document explicit about the current boundary:
  public OAuth MCP is the supported product path, while older local loopback
  MCP material is legacy-only.
- Ground the capability descriptions in the current code and active design docs
  instead of repeating older archived assumptions.

## Implementation plan

1. Inspect active product docs, current screen/viewmodel flows, and the latest
   supported MCP architecture note.
2. Separate the message into executive and technical audiences.
3. Write the two new docs in `docs/design/`.
4. Update discovery/index files in the same pass.
5. Validate formatting and consistency against the repo's current product
   boundary.

## Validation plan

1. Cross-check the new content against current active docs:
   `docs/README.md` and
   `docs/architecture/SHARY_REMOTE_MCP_OAUTH_DESIGN.md`.
2. Cross-check capability claims against current source surfaces in:
   `MainActivity`, `SettingsScreen`, `FieldViewModel`, `UserViewModel`,
   `RequestScreen`, `SummaryFieldScreen`, `SummaryRequestScreen`, and
   `SharyMcpDispatcher`.
3. Run a diff-format check on the touched documentation files.
4. Record the existing session-init gate failure caused by the checkout having
   `AGENTS.md` while `tools/codex/session-init.manifest.json` still requires
   `AGENT.md`.

## Result summary

Implemented. The repo now contains a stakeholder-facing executive overview and
an engineering-facing technical overview of Shary's intention and capabilities.
The content reflects the current cloud-first MCP product boundary and the
active local app surfaces rather than the superseded local-MCP-only design.
