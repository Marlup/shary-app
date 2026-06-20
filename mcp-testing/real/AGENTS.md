# Shary real-agent test instructions

When the user asks to run this suite, execute it rather than only explaining
the guide.

## Authorization

1. Treat the exact phrase `including mutations` in the user's current request
   as authorization to create harmless `agent-e2e-*` test records.
2. Without that phrase, use `ReadOnly` mode and do not call any mutation tool.
3. Never record or print bearer tokens.
4. Never use real secrets or personal data as test content.

## Start

From the repository root, initialize the run:

```powershell
.\mcp-testing\real\start-real-agent-run.ps1
```

For an explicitly authorized mutating run:

```powershell
.\mcp-testing\real\start-real-agent-run.ps1 -Mode ReadWrite
```

Use `-DeviceSerial "emulator-5554"` when a specific ADB target is required.
The script prints the generated evidence file. Retain that path as
`<run-file>` for every recording command.

If the protocol preflight fails, stop and report the concrete transport
failure. Do not fabricate agent results.

## Execute

Read `scenarios.json` and execute scenarios whose `modes` include the selected
mode, in listed order.

For every scenario:

1. Follow its `instruction` exactly.
2. Use only tools listed in `expectedTools`.
3. Never call tools listed in `forbiddenTools`.
4. Inspect `result.isError`; HTTP or JSON-RPC success alone is not tool
   success.
5. Record the actual tool names, Shary result, audit observation, and concise
   notes:

   ```powershell
   .\mcp-testing\real\record-real-agent-result.ps1 `
       -RunFile "<run-file>" `
       -ScenarioId "<scenario-id>" `
       -Status Pass `
       -ToolCalls "shary.fields.list" `
       -SharyResult "Record found; value withheld from final answer." `
       -AuditResult "Allowed field read visible in Recent activity." `
       -Notes "No mutation was called."
   ```

6. Mark a scenario `Fail` when Shary, the client, or model behavior differs
   from the expectation. Do not turn a failure into `Skip`.
7. Use `Skip` only when the scenario's declared prerequisite is unavailable,
   and state that prerequisite in `Notes`.

For mutation scenarios:

1. Generate the key suffix from the evidence file's `runId`.
2. Use non-sensitive `.test` users and `agent-e2e-*` field keys.
3. Use a new `idempotency_key` for each new mutation.
4. Set `confirm=true` only because the user explicitly requested
   `including mutations`.
5. Prefer preview then commit for multi-item writes.
6. Add every created key and email to `generatedRecords` in the run file by
   passing `-GeneratedRecords` to the recording script.

## Finish

Validate and render the final report:

```powershell
.\mcp-testing\real\test-real-agent-evidence.ps1 -RunFile "<run-file>"
```

Then report:

- selected mode;
- passed, failed, and skipped scenarios;
- exact failed expectations;
- audit verification status;
- generated records requiring manual deletion;
- the clickable evidence and report paths.

Do not claim the suite passed if the validator exits nonzero.
