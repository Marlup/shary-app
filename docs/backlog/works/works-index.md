# Works Index

## 2026-06-20

1. **Desktop bridge Phase 0 connector boundary**
   Extracted transport-neutral MCP dispatch, added locked/signed-out
   enforcement and bounded bearer-scoped rate limits, and preserved localhost
   MCP compatibility with focused tests.
2. **Agent connection checkpoint consolidation**
   Consolidated the MCP implementation, lifecycle UI, transport hardening, tests, and operator documentation; passed the policy gate and focused Android validation while retaining live device E2E as an explicit pending acceptance step.
3. **Production desktop bridge design**
   Designed an installable loopback MCP bridge with Android-initiated encrypted LAN pairing, capability-scoped agent profiles, lifecycle controls, threat mitigations, implementation phases, and no ADB production dependency.
4. **Repository routing and cloud MCP placement**
   Recorded the Android/backend workspace paths and selected an isolated Firebase MCP codebase inside the existing backend repository, with incremental backend modularization and shared Android connector execution.

## 2026-06-19

1. **Connector and MCP E2E quick-start guide**
   Added a detailed Windows-to-Android validation guide and completed the Settings workflow with command explanations, scrollable controls, a five-active-connector limit, bearer copy, individual revoke, revoke-all, audit-preserving lifecycle behavior, and focused tests.
2. **English-only agent policy and low-risk build warning cleanup**
   Made English mandatory for all agent-authored communication and removed low-risk AGP, Kotlin annotation-target, lifecycle, dropdown-anchor, and tooltip deprecations while preserving higher-risk migration flags.
3. **MCP checkpoint consolidation and fresh validation**
   Recovered the agent-access checkpoint on `connector-details`, passed the mandatory policy gate, rebuilt the Android Kotlin sources, and reran the focused MCP unit suite from scratch before commit.
4. **MCP PowerShell test suite**
   Converted the manual MCP E2E guide into standalone PowerShell scenarios plus an assertion runner, preparation/cleanup scripts, secure token inputs, mutation opt-in, and live public-suite validation.
5. **Real-agent MCP E2E guide**
   Added a local Codex/Claude-focused guide for testing natural-language tool selection, least-privilege reads, truthful denial handling, approval-gated writes, preview/commit, prompt-injection resistance, revocation, expiration, audit correlation, and cross-client consistency.
6. **Command-driven real-agent MCP suite**
   Added an executable Codex agent contract, structured scenarios, evidence recording and validation under `mcp-testing/real`, while preserving the direct PowerShell suite under `mcp-testing/protocol`.
7. **Same-device AI connector design**
   Designed local MCP, Android IPC, Shary-owned assistant, and optional remote-relay paths for connecting AI clients on Android without bypassing connector policy, audit, confirmation, or revocation.
8. **Local-agent MCP transport hardening**
   Narrowed the active architecture to MCP-capable agent tools and implemented case-insensitive headers, negotiation checks, request limits, explicit stateless methods, response security headers, and focused transport tests.

## 2026-06-15

1. **MCP server for agent access**  
   Added a Shary MCP JSON-RPC adapter and localhost `/mcp` HTTP endpoint over the existing connector security path, surfaced start/stop and pairing credentials in Settings, and covered tool discovery plus auth/policy behavior with targeted unit tests.

## 2026-06-14

1. **Cloud inbox review diff and error hardening**  
   Retested the previous cloud-readiness batch, added stronger unit and Compose source coverage, then implemented structured cloud error messaging, schema-aware inbox review dialogs, and local field-import diff planning before accept.

## 2026-06-13

1. **Android Layout Inspector setup**  
   Made the debug build explicitly inspectable and added a repo-local Android Studio Layout Inspector guide for visual UI diagnosis.
2. **Conversation recovery instruction and improvements predesign**  
   Added a reusable instruction for saving the latest two complete user-agent exchange pairs and started a feasibility/difficulty predesign table for visual, backend/cloud, and capability improvements.
3. **Cloud readiness status strip first batch**  
   Implemented the first low-regression improvement slice with qualitative cloud status labels, a visible Fields-screen status strip, disabled cloud-action feedback, clearer progressive loading copy, and cloud-status unit coverage.

## 2026-06-08

1. **List typed-field edit and detail layout fix**  
   Made list typed fields editable element-by-element in add/update dialogs, rendered typed list values vertically in field detail, and aligned detail actions in right-side vertical groups; verified by Kotlin app and Android-test compilation.

## 2026-05-31

1. **AI session-init policy enforcement and checksum gate**  
   Implemented canonical AI-policy loading order (`AGENT.md` first), added `CODEX.md`/`CLAUDE.md` adapters, and introduced manifest-backed startup checks plus prompt payload emission scripts.

## 2026-05-17

1. **Shary LLM/GenAI connector analysis and design evaluation**  
   Defined architecture options and recommended a local loopback connector MVP with capability-scoped auth, consent/policy controls, audit trail, and staged rollout; included optional `agent2user` design for future decision.
2. **MCP feasibility and ecosystem evaluation for Shary connector strategy**  
   Extended the connector design with an MCP-specific feasibility decision, comparative pros/cons against local loopback architecture, and a curated snapshot of available open-source MCP SDK/server tooling as of 2026-05-17.
3. **P0 Option A functional design freeze draft**  
   Added implementation-ready functional design for local connector behavior, capability matrix, consent modes, UX states, error semantics, and acceptance criteria before coding.
4. **P0 Option A technical design freeze draft**  
   Added implementation-ready technical design with architecture layers, module boundaries, local persistence schema, endpoint contracts, security controls, idempotency/concurrency strategy, and rollout gates.
5. **P0 Option A Phase 0 implementation skeleton**  
   Implemented connector core contracts (enums/models/envelopes/errors), repository interface plus DI-wired in-memory scaffold to unblock next-phase pair/preview/commit logic; verified by `:app:compileDebugKotlin`.
6. **P0 Option A Phase 1 and Phase 2 implementation pass**  
   Implemented pair/refresh use-cases and policy evaluator skeleton, then added loopback read-only transport endpoints (`/connector/v1/fields`, `/connector/v1/users`) with bearer auth + policy checks; verified by `:app:compileDebugKotlin`.
7. **P0 Option A Phase 3 and Phase 4 implementation pass**  
   Implemented write endpoints plus preview/commit action orchestration with confirmation gating, and added Settings UI surfaces for assistant connector pairing and recent activity; verified by `:app:compileDebugKotlin`.
8. **P0 Option A completion pass - tests and durable connector persistence**  
   Replaced in-memory connector repository with DataStore-backed durable persistence and added focused unit tests for policy evaluation and preview/commit orchestration behavior; compile and targeted unit tests pass.

## 2026-05-16

1. **Field typed-value UX and interaction redesign impact evaluation**  
   Viability/complexity assessment and implementation design for typed value auto-detection, link/url type support, card selection/detail behavior split, search visibility toggle, and command dock compaction.
2. **Implementation pass - card interaction, typed auto-detection, search toggle, and dock compaction**  
   Applied the evaluated UI/UX changes across Fields/Users shared surfaces, added `link_url` typed support and auto-detection on save, and validated by compiling `:app:compileDebugKotlin`.
3. **Completion pass - Request card interaction parity and typed-contract tests**  
   Extended the new selection/detail interaction model to Request cards and added focused unit tests for `FieldValueContract` `link_url` handling; compile and targeted tests pass.
4. **Selected-card quick action set for fields**  
   Added explicit single-selection icon actions for copy value, duplicate, edit value, edit tag, edit alias, and delete, reusing existing clipboard/update/delete behavior and validating via Kotlin compile.
5. **Field-detail dialog action placement and callback hardening**  
   Relocated field actions into the detail dialog beside their target sections (value/tag/alias) with labeled controls, moved field-level actions to bottom, hardened edit callback state handling, and updated AGENT instructions with mandatory unit-test verification guidance.
6. **Tag-edit crash fix and dialog callback test coverage**  
   Fixed `rememberSaveable` misuse with non-saveable `Tag` state in tag editor flow, renamed dialog action labels for unambiguous UX, and added Android Compose test coverage for detail-dialog action callback wiring.
