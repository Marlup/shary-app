# Codex-operated MCP Phase 0 live check

> Development-only device check. ADB is valid here because this file verifies
> the local dispatcher boundary, signed-out denial, and bearer limiter.
> Production agents connect without ADB through the public OAuth path in
> `docs/testing/connector-mcp-real-agent-e2e.md`.

Use this guide by asking Codex to execute the live check. Codex runs ADB,
issues MCP requests, asserts results, records a concise evidence summary, and
cleans up. The user performs only the Android security actions that cannot be
automated safely.

## Command Codex

From a Codex session opened at the Shary repository root, send:

```text
Run the Codex-operated MCP Phase 0 live check in
docs/testing/connector-mcp-phase0-manual-check.md. Use the connected Android
target, test dispatcher access, signed-out denial, and bearer rate limiting,
then clean up and report exact pass/fail evidence. Do not print or store the
bearer token.
```

This is a read-only test. It does not authorize Shary data mutations.

## Preconditions

Codex must verify:

1. `adb devices` shows exactly one usable target, or the user supplied a
   device serial.
2. The debug Shary app is installed and open.
3. The user is signed in and the local vault is usable.
4. MCP is started in **Settings**.
5. A read-only connector is paired.
6. `SHARY_MCP_TOKEN` contains the raw access token in Codex's process
   environment.

If an in-app action or token is missing, Codex must pause and ask the user to
perform only that action. The token must never be printed, written to a file,
included in evidence, or passed as a command-line argument.

## Codex execution contract

### 1. Prepare and preflight

Codex:

1. Runs the repository policy gate.
2. Selects the ADB target.
3. Configures `adb forward tcp:8765 tcp:8765`.
4. Confirms `SHARY_MCP_TOKEN` exists without displaying its value.
5. Sends MCP `initialize`, `ping`, and `tools/list` requests to:

   ```text
   http://127.0.0.1:8765/mcp
   ```

6. Requires protocol version `2025-06-18` and all nine local Shary tools.

Any failed preflight stops the run with the concrete failure.

### 2. Dispatcher access

Codex calls `shary.fields.list` with the read-only bearer and asserts:

- HTTP and JSON-RPC transport succeed;
- `result.isError` is `false`;
- `structuredContent` contains the connector success envelope;
- no field values are reproduced in the final report.

### 3. Signed-out lifecycle denial

Codex asks the user to sign out of Shary while leaving the app process
available. After the user confirms, Codex repeats `shary.fields.list` and
asserts:

- `result.isError` is `true`;
- the error code is `policy_denied`;
- the message says Shary is locked or signed out;
- no field records or values are returned.

Codex then asks the user to sign in again, start MCP again if needed, and
confirm readiness before continuing.

### 4. Bearer-scoped rate limit

Codex waits until a fresh 60-second window, then sends 121 authenticated
`shary.fields.list` calls within that window using unique JSON-RPC IDs.

Codex asserts:

- requests 1 through 120 are not rejected with `rate_limited`;
- request 121 returns `result.isError = true`;
- its error code is `rate_limited`;
- `retry_after_seconds` is present and positive;
- a request after the reported interval succeeds.

Codex must not include response field values in logs or the final report.

### 5. Cleanup

Codex:

1. Removes `adb forward tcp:8765`.
2. Clears `SHARY_MCP_TOKEN` from the current process environment when the
   execution surface permits it.
3. Confirms that no token-bearing file or evidence artifact was created.

## Required final report

Codex reports:

- Android target used;
- preflight result;
- dispatcher access result;
- signed-out denial result;
- rate-limit result, including the first limited request number and observed
  retry interval;
- cleanup result;
- exact failed assertion, if any.

Codex must not claim a pass when a check was skipped, and must never include
the bearer token or Shary field values.
