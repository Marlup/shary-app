# Real-agent MCP E2E guide

## Scope

Create an operator guide for validating Shary through real local AI agents,
beyond direct JSON-RPC and PowerShell protocol tests.

## Change list

1. Added `docs/testing/connector-mcp-real-agent-e2e.md`.
2. Added current Codex Streamable HTTP and bearer-token configuration.
3. Added Claude Code HTTP/header configuration with environment expansion.
4. Added an optional Gemini CLI compatibility section with a dated transition
   warning from its official documentation.
5. Added natural-language tests for discovery, least-disclosure reads,
   read-only denial, approved writes, preview/commit, retry behavior, prompt
   injection resistance, revocation, expiration, and cross-client consistency.
6. Added evidence requirements, acceptance criteria, troubleshooting, and
   cleanup.
7. Linked the guide from `docs/README.md`.

## Design decisions

- Target local clients because a hosted agent cannot reach the Windows
  `127.0.0.1` endpoint.
- Keep client tool approval enabled and treat approval as separate from Shary's
  own policy enforcement.
- Test model behavior explicitly: tool choice, truthful error handling, data
  minimization, confirmation discipline, and untrusted-content handling.
- Use Codex and Claude Code as primary targets because their official
  documentation provides clear Streamable HTTP and rotating-token
  configuration.
- Keep Gemini CLI optional because its official documentation displays a
  June 18, 2026 product-transition notice.

## Validation

1. Cross-checked Shary tool names and security behavior against the current MCP
   guide and implementation.
2. Verified Codex configuration against the current official Codex manual and
   OpenAI MCP documentation.
3. Verified Claude Code HTTP/header/project-scope behavior against Anthropic's
   official MCP documentation.
4. Verified Gemini CLI `httpUrl`, headers, confirmation, and tool filtering
   against Google's official MCP documentation.
5. Run `git diff --check`.

## Result

The guide provides an executable manual validation plan for proving that a real
agent can discover, reason about, safely invoke, and accurately report Shary
operations while respecting connector capabilities, explicit approval, audit,
revocation, and expiry.
