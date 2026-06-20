# Connector and MCP Server E2E Quick Start

Use this guide to validate the complete local path:

```text
Desktop MCP client
    -> adb TCP forwarding
    -> Shary Android MCP HTTP server
    -> MCP JSON-RPC adapter
    -> connector authentication and policy
    -> Shary field/user repositories
    -> connector audit history
```

The authoritative endpoint is:

```text
http://127.0.0.1:8765/mcp
```

The server runs inside the Android app process. A desktop client cannot reach
the emulator/device loopback address until `adb forward` is configured.

## Automated PowerShell scripts

The executable version of this guide is in `mcp-testing/protocol/`. It includes
standalone scenario scripts, shared assertion helpers, preparation and cleanup,
and an orchestrator that exits with code `1` on the first failed assertion.

Run the public transport/protocol suite:

```powershell
# Configure ADB forwarding and assert transport, initialization, discovery,
# ping, and origin protection against the running Shary MCP server.
.\mcp-testing\protocol\run-mcp-tests.ps1 -DeviceSerial "emulator-5554"
```

For authenticated and mutating runs, follow `mcp-testing/protocol/README.md`. Pairing
remains a manual in-app security action; scripts accept copied tokens through
parameters or process-scoped environment variables and never store them.

## 1. Prerequisites

You need:

1. Windows PowerShell.
2. Android Studio and an Android emulator or USB-connected device.
3. `adb` available from the Android SDK.
4. JDK 21 available to Gradle.
5. A debug build of Shary.
6. The Shary app logged in and its local vault usable.

Confirm the device connection:

```powershell
# Ask ADB to list connected Android targets and their authorization state.
# Here, it confirms that the device used for the Shary test is reachable.
adb devices
```

Expected result: one device or emulator is listed with status `device`.

For a physical Android device, authorize this computer when the device is
first connected:

1. On the device, enable **Developer options** and **USB debugging**.
2. Unlock the device and connect it to Windows with a data-capable USB cable.
3. Run:

   ```powershell
   # Ask ADB to rediscover connected targets after USB debugging is enabled.
   # Here, it triggers the device authorization flow and exposes its status.
   adb devices
   ```

4. When Android displays **Allow USB debugging?**, verify the RSA fingerprint,
   optionally select **Always allow from this computer**, and press **Allow**.
5. Run `adb devices` again. The status must change from `unauthorized` to
   `device`.

If the authorization prompt does not appear:

```powershell
# Stop the host ADB background process so its USB connections are discarded.
# Here, this clears a stale connection that may suppress the RSA prompt.
adb kill-server

# Start a fresh ADB background process and rescan attached Android targets.
# Here, this gives the device another opportunity to request authorization.
adb start-server

# List targets and states again to verify that authorization now says "device".
adb devices
```

Then reconnect the cable and keep the device unlocked. If it remains
`unauthorized`, open **Developer options**, select **Revoke USB debugging
authorizations**, reconnect, and accept the new RSA prompt. Also verify that
the USB connection mode supports data transfer and that the device's Windows
USB driver is installed.

If several devices are connected, select one for every command:

```powershell
# Store the exact ADB serial of the target that should receive later commands.
# Replace this emulator serial with the value shown by `adb devices`.
$Device = "emulator-5554"

# Run `get-state` only on that serial and report whether ADB sees it as usable.
# Here, the expected output is `device`.
adb -s $Device get-state
```

The examples below omit `-s $Device`. Add it immediately after `adb` when
multiple devices are present.

## 2. Run the automated baseline

From the repository root:

```powershell
# Change this PowerShell session's working directory to the Shary repository.
# This makes the repository's Gradle wrapper and relative paths available.
cd C:\Users\marsu\AndroidStudioProjects\sharydemo

# Compile the debug variant's Kotlin sources without installing the application.
# Here, it catches source or dependency errors before the manual E2E test.
.\gradlew :app:compileDebugKotlin

# Run only the focused JVM test class for the MCP adapter.
# Here, it checks discovery, authenticated reads, and read-only write denial.
.\gradlew :app:testDebugUnitTest --tests "com.shary.app.infrastructure.connector.mcp.SharyMcpServerTest"
```

Both commands must end with:

```text
BUILD SUCCESSFUL
```

The focused MCP suite validates:

- MCP tool discovery.
- Bearer-authenticated field reads.
- Denial and audit behavior when a read-only connector attempts a write.

## 3. Install and open Shary

Install the debug application:

```powershell
# Build the debug APK and install or replace it on the selected ADB target.
# Here, this is also required to receive the latest Settings UI changes.
.\gradlew :app:installDebug
```

Launch it from Android Studio or with:

```powershell
# Ask Android's `monkey` utility to send one launcher event to Shary.
# Here, it opens package `com.shary.app` without using Android Studio.
adb shell monkey -p com.shary.app -c android.intent.category.LAUNCHER 1
```

In the app:

1. Log in.
2. Unlock or initialize the local vault.
3. Add one harmless test field, for example:
   - Key: `e2e-existing-field`
   - Value: `before-mcp`
4. Add one harmless test user if the UI supports it:
   - Username: `E2E Existing User`
   - Email: `e2e-existing@example.test`

Use non-sensitive data. Connector reads return field values.

## 4. Start the MCP server

In Shary:

1. Open **Settings**.
2. Scroll to the **Assistant Connectors** card.
3. Find the **MCP server** section.
4. Press **Start MCP**.
5. Confirm the displayed state is:

```text
Running at http://127.0.0.1:8765/mcp
```

Keep the app process alive during the test. The server stops when the Android
process is terminated. Starting and stopping MCP controls only the network
listener. Existing connector credentials remain paired until they are revoked.

## 5. Forward the Android port to Windows

Run:

```powershell
# Create a host-to-device TCP tunnel for port 8765 on the selected target.
# Here, Windows 127.0.0.1:8765 reaches Shary's device-local MCP server.
adb forward tcp:8765 tcp:8765

# List all active ADB port-forwarding rules.
# Here, it verifies that the 8765 tunnel was registered on the intended target.
adb forward --list
```

Expected result: the selected device has a forwarding rule for
`tcp:8765 -> tcp:8765`.

Basic transport checks:

```powershell
# Send an HTTP GET request and include response headers in the output.
# Here, the expected 405 proves that the forwarded MCP endpoint is reachable.
curl.exe -i http://127.0.0.1:8765/mcp
```

Expected result:

```text
HTTP/1.1 405 Method Not Allowed
SSE stream is not supported by this Shary MCP server
```

That response is correct. Shary currently implements request/response MCP over
HTTP `POST`, not an SSE `GET` stream.

## 6. Pair a read-only connector

In **Settings > Assistant Connectors**:

1. Scroll below the **Start MCP** and **Stop MCP** buttons. The next row contains
   **Pair read-only** and **Pair read-write**.
2. Press **Pair read-only**.
3. Pairing is enabled only while MCP is running. If the buttons are disabled,
   start MCP and verify that the active connector count is below `5/5`.
4. If that row is absent, install the current debug build again with
   `.\gradlew :app:installDebug`, reopen Shary, and return to Settings. Older
   installed APKs may have the MCP controls without the pairing row.
5. Use **Copy bearer** to copy the `Bearer <token>` header value, or manually
   copy the complete access token from:

```text
Authorization: Bearer <token>
```

6. Copy it immediately. Shary stores only its hash; if the raw token is lost,
   pair a new connector.
7. Note the expiration epoch. The default session lifetime is 20 minutes.

The **Active connectors** section lists every usable connector and provides a
**Revoke** action for each one. Shary allows at most five active connectors.
Revoking a connector invalidates all of its sessions, removes it from the
active count, and preserves its audit history. **Revoke all** resets the active
count without deleting historical audit records.

Read-only capabilities are:

- `fields.read`
- `users.read`
- `actions.preview`

They do not include field/user writes or action commits.

## 7. Prepare a PowerShell MCP client

Set the endpoint and paste the token without the `Bearer ` prefix:

```powershell
# Store the forwarded MCP URL once so every request uses the same endpoint.
$McpUrl = "http://127.0.0.1:8765/mcp"

# Store the short-lived secret produced by the pairing action.
# Replace only the placeholder; do not include the displayed `Bearer ` prefix.
$Token = "PASTE_ACCESS_TOKEN_HERE"

# Define a reusable PowerShell command for sending MCP JSON-RPC requests.
function Invoke-SharyMcp {
    # Declare the values callers may supply for one MCP request.
    param(
        # Require the JSON-RPC method name, such as `tools/list`.
        [Parameter(Mandatory = $true)]
        [string]$Method,

        # Accept optional method parameters; an empty table means no parameters.
        [hashtable]$Params = @{},

        # Use a request ID so the response can be matched to this request.
        [object]$Id = 1,

        # Allow public MCP methods to omit the connector Authorization header.
        [switch]$WithoutAuth
    )

    # Construct the mandatory JSON-RPC 2.0 request fields.
    $payload = @{
        jsonrpc = "2.0"
        id = $Id
        method = $Method
    }

    # Include `params` only when the caller supplied at least one value.
    if ($Params.Count -gt 0) {
        $payload["params"] = $Params
    }

    # Tell Shary that the request body is JSON.
    $headers = @{
        "Content-Type" = "application/json"
    }

    # Authenticate protected tool calls unless the caller explicitly opts out.
    if (-not $WithoutAuth) {
        $headers["Authorization"] = "Bearer $Token"
    }

    # Serialize the payload and send it as an HTTP POST to Shary's MCP endpoint.
    # `-Depth 12` preserves the nested tool arguments used later in this guide.
    Invoke-RestMethod `
        -Uri $McpUrl `
        -Method Post `
        -Headers $headers `
        -Body ($payload | ConvertTo-Json -Depth 12)
}
```

Do not save real access tokens in source-controlled files, shell history,
screenshots, or test reports.

## 8. Test MCP initialization and discovery

Initialize:

```powershell
# Send MCP's initialization handshake without connector authentication.
# Here, it declares the protocol version and identifies this PowerShell client.
$Initialize = Invoke-SharyMcp `
    -Method "initialize" `
    -Id "initialize-1" `
    -WithoutAuth `
    -Params @{
        protocolVersion = "2025-06-18"
        capabilities = @{}
        clientInfo = @{
            name = "shary-e2e-powershell"
            version = "1.0"
        }
}

# Render the nested handshake response as readable JSON for manual inspection.
$Initialize | ConvertTo-Json -Depth 12
```

Expected values:

- `result.protocolVersion` is `2025-06-18`.
- `result.serverInfo.name` is `shary-mcp`.
- `result.capabilities.tools.listChanged` is `false`.

Test ping:

```powershell
# Call MCP `ping` without authentication to test basic JSON-RPC responsiveness.
# Convert the response to readable JSON so protocol errors are visible.
Invoke-SharyMcp -Method "ping" -Id "ping-1" -WithoutAuth |
    ConvertTo-Json -Depth 12
```

Discover tools:

```powershell
# Request the server's advertised MCP tool definitions without authentication.
$Tools = Invoke-SharyMcp -Method "tools/list" -Id "tools-1" -WithoutAuth

# Project the full tool definitions down to their names and human-readable titles.
$Tools.result.tools | Select-Object name, title
```

Expected tools:

```text
shary.fields.list
shary.fields.create
shary.fields.update
shary.users.list
shary.users.create
shary.users.update
shary.actions.preview
shary.actions.commit
```

## 9. Test authenticated read operations

List fields:

```powershell
# Invoke the authenticated `shary.fields.list` MCP tool with no tool arguments.
# The read-only bearer token is added automatically by `Invoke-SharyMcp`.
$Fields = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "fields-list-1" `
    -Params @{
        name = "shary.fields.list"
        arguments = @{}
}

# Print only the tool's structured result, retaining nested field records.
$Fields.result.structuredContent | ConvertTo-Json -Depth 12
```

Expected result:

- `result.isError` is `false`.
- `structuredContent.data.items` contains the app's current fields.
- The seeded `e2e-existing-field` is present.

List users:

```powershell
# Invoke the authenticated `shary.users.list` MCP tool with no tool arguments.
# Here, it reads the users visible to the paired read-only connector.
$Users = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "users-list-1" `
    -Params @{
        name = "shary.users.list"
        arguments = @{}
}

# Print only the tool's structured result, retaining nested user records.
$Users.result.structuredContent | ConvertTo-Json -Depth 12
```

Expected result:

- `result.isError` is `false`.
- `structuredContent.data.items` contains the app's current users.

## 10. Verify read-only write denial

Run a write with the read-only token:

```powershell
# Attempt an authenticated field creation while still using the read-only token.
# This deliberately valid mutation should reach policy evaluation and be denied.
$DeniedWrite = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "readonly-denial-1" `
    -Params @{
        name = "shary.fields.create"
        arguments = @{
            key = "e2e-must-not-be-created"
            value = "denied"
            idempotency_key = "readonly-denial-1"
            confirm = $true
        }
}

# Print the MCP tool result so `isError` and `unauthorized_scope` are visible.
$DeniedWrite.result | ConvertTo-Json -Depth 12
```

Expected result:

- The HTTP/JSON-RPC request succeeds.
- `result.isError` is `true`.
- The structured error code is `unauthorized_scope`.
- `e2e-must-not-be-created` does not appear in Shary.

MCP tool failures are returned inside the tool result. Do not treat HTTP `200`
alone as proof of tool success; always check `result.isError`.

In Shary Settings, confirm **Recent activity** includes the denied operation.

## 11. Pair a read-write connector

In **Settings > Assistant Connectors**:

1. Press **Pair read-write**.
2. Use **Copy bearer**, or manually copy the new access token.
3. Replace the PowerShell token:

```powershell
# Replace the session variable with the newly displayed read-write secret.
# Subsequent helper calls will authenticate with the broader capability set.
$Token = "PASTE_NEW_READ_WRITE_ACCESS_TOKEN_HERE"
```

Read-write capabilities include:

- `fields.read`
- `fields.write`
- `users.read`
- `users.write`
- `actions.preview`
- `actions.commit`

The default policy is strict and asks for confirmation on mutations. Therefore:

- Every mutation needs a unique `idempotency_key`.
- Direct writes and commits need `confirm = $true`.

## 12. Test direct field creation and update

Create a field:

```powershell
# Invoke the field-creation tool with a unique idempotency key and confirmation.
# Here, it creates the harmless `e2e-mcp-field` record in Shary.
$CreateField = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "field-create-1" `
    -Params @{
        name = "shary.fields.create"
        arguments = @{
            key = "e2e-mcp-field"
            value = "created-through-mcp"
            key_alias = "E2E MCP Field"
            idempotency_key = "field-create-1"
            confirm = $true
        }
}

# Print the complete tool result to verify the `created` flag and stored key.
$CreateField.result | ConvertTo-Json -Depth 12
```

Expected result:

- `result.isError` is `false`.
- `structuredContent.data.created` is `true`.
- `structuredContent.data.key` is `e2e-mcp-field`.

Update the same field:

```powershell
# Invoke the field-update tool for the key created by the preceding command.
# Here, it changes both the test value and its user-facing alias.
$UpdateField = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "field-update-1" `
    -Params @{
        name = "shary.fields.update"
        arguments = @{
            key = "e2e-mcp-field"
            value = "updated-through-mcp"
            key_alias = "Updated E2E MCP Field"
            idempotency_key = "field-update-1"
            confirm = $true
        }
}

# Print the complete tool result to verify that the update succeeded.
$UpdateField.result | ConvertTo-Json -Depth 12
```

List fields again and verify the new value and alias:

```powershell
# Read the current field collection again after the update.
$FieldsAfterUpdate = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "fields-list-2" `
    -Params @{
        name = "shary.fields.list"
        arguments = @{}
}

# Select the returned items, keep only the target key, and show all its fields.
# Here, this confirms the persisted value and alias rather than only the write response.
$FieldsAfterUpdate.result.structuredContent.data.items |
    Where-Object key -eq "e2e-mcp-field" |
    Format-List
```

Also verify the field in the Android UI.

## 13. Test direct user creation and update

Create a user:

```powershell
# Invoke the user-creation tool with explicit confirmation and replay protection.
# Here, it creates a non-sensitive `.test` user for E2E validation.
$CreateUser = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "user-create-1" `
    -Params @{
        name = "shary.users.create"
        arguments = @{
            username = "E2E MCP User"
            email = "e2e-mcp@example.test"
            idempotency_key = "user-create-1"
            confirm = $true
        }
}

# Print the tool result to verify that the user creation was accepted.
$CreateUser.result | ConvertTo-Json -Depth 12
```

Update the display name:

```powershell
# Invoke the user-update tool and identify the existing record by email.
# Here, it changes only the E2E user's display name.
$UpdateUser = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "user-update-1" `
    -Params @{
        name = "shary.users.update"
        arguments = @{
            email = "e2e-mcp@example.test"
            username = "Updated E2E MCP User"
            idempotency_key = "user-update-1"
            confirm = $true
        }
}

# Print the tool result to verify that the update completed without an MCP error.
$UpdateUser.result | ConvertTo-Json -Depth 12
```

List users and verify the result:

```powershell
# Read the current user collection after the update.
$UsersAfterUpdate = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "users-list-2" `
    -Params @{
        name = "shary.users.list"
        arguments = @{}
}

# Keep the user with the E2E email address and display all returned properties.
$UsersAfterUpdate.result.structuredContent.data.items |
    Where-Object email -eq "e2e-mcp@example.test" |
    Format-List
```

## 14. Test preview and commit

Preview a batch:

```powershell
# Submit two proposed mutations to the preview tool without applying them.
# Here, Shary validates and stages one field and one user creation as a batch.
$Preview = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "batch-preview-1" `
    -Params @{
        name = "shary.actions.preview"
        arguments = @{
            idempotency_key = "batch-preview-1"
            actions = @(
                @{
                    type = "CREATE_FIELD"
                    key = "e2e-batch-field"
                    value = "created-by-preview-commit"
                    key_alias = "E2E Batch Field"
                },
                @{
                    type = "CREATE_USER"
                    username = "E2E Batch User"
                    email = "e2e-batch@example.test"
                }
            )
        }
}

# Print the preview response, including its summary and generated pending ID.
$Preview.result | ConvertTo-Json -Depth 12

# Extract the pending batch identifier needed by the commit tool.
$PendingId = $Preview.result.structuredContent.data.pending_id

# Echo the identifier so an empty or missing value is obvious before committing.
$PendingId
```

Expected result:

- `result.isError` is `false`.
- `pending_id` is non-empty.
- A summary describes the proposed operations.
- The field and user are not yet applied.

Commit the preview:

```powershell
# Commit the exact staged batch identified by `$PendingId`.
# Here, confirmation and a unique key authorize applying both previewed mutations.
$Commit = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "batch-commit-1" `
    -Params @{
        name = "shary.actions.commit"
        arguments = @{
            pending_id = $PendingId
            idempotency_key = "batch-commit-1"
            confirm = $true
        }
}

# Print the commit result to verify committed, applied, and failed counts.
$Commit.result | ConvertTo-Json -Depth 12
```

Expected result:

- `result.isError` is `false`.
- `structuredContent.data.committed` is `true`.
- `applied_count` is `2`.
- `failed_count` is `0`.

List fields and users again, then verify both records in the Android UI.

## 15. Test commit idempotency

Repeat the exact commit request with the same JSON-RPC request ID and
`idempotency_key`:

```powershell
# Repeat the same commit identity, pending ID, and idempotency key.
# Here, the server should return its cached result instead of applying duplicates.
$CommitAgain = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "batch-commit-1" `
    -Params @{
        name = "shary.actions.commit"
        arguments = @{
            pending_id = $PendingId
            idempotency_key = "batch-commit-1"
            confirm = $true
        }
}

# Print the repeated response so it can be compared with the first commit.
$CommitAgain.result | ConvertTo-Json -Depth 12
```

Expected result: the server returns the cached commit response and does not
apply the batch a second time.

## 16. Test required mutation controls

### Missing idempotency key

```powershell
# Deliberately call a mutation without its required `idempotency_key`.
# Here, the test proves malformed mutations are rejected before data changes.
$MissingIdempotency = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "missing-idempotency-1" `
    -Params @{
        name = "shary.fields.create"
        arguments = @{
            key = "e2e-invalid-field"
            value = "must-not-be-created"
            confirm = $true
        }
}

# Print the expected validation error returned inside the MCP tool result.
$MissingIdempotency.result | ConvertTo-Json -Depth 12
```

Expected result: `isError = true` with `validation_failed`.

### Missing confirmation

```powershell
# Deliberately call a valid mutation without `confirm = $true`.
# Here, the test proves strict policy prevents unconfirmed writes.
$MissingConfirmation = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "missing-confirmation-1" `
    -Params @{
        name = "shary.fields.create"
        arguments = @{
            key = "e2e-unconfirmed-field"
            value = "must-not-be-created"
            idempotency_key = "missing-confirmation-1"
        }
}

# Print the expected `confirmation_required` tool error.
$MissingConfirmation.result | ConvertTo-Json -Depth 12
```

Expected result: `isError = true` with `confirmation_required`.

### Invalid token

```powershell
# Preserve the working secret so it can be restored after the negative test.
$ValidToken = $Token

# Replace it temporarily with a known-invalid bearer token.
$Token = "invalid-token"

# Attempt a protected read with the invalid token.
# Here, the test proves connector authentication is enforced on tool calls.
$InvalidTokenResult = Invoke-SharyMcp `
    -Method "tools/call" `
    -Id "invalid-token-1" `
    -Params @{
        name = "shary.fields.list"
        arguments = @{}
}

# Print the authentication or session error returned by the server.
$InvalidTokenResult.result | ConvertTo-Json -Depth 12

# Restore the valid token so later guide steps continue to work.
$Token = $ValidToken
```

Expected result: `isError = true` with an authentication/session error.

## 17. Verify origin protection

Non-local browser origins must be rejected:

```powershell
# Send a raw MCP ping while claiming it came from an untrusted web origin.
# `-i` prints headers; POST selects MCP transport; both `-H` flags set request
# metadata; `--data` supplies the JSON-RPC body. Shary should return HTTP 403.
curl.exe -i `
    -X POST `
    -H "Origin: https://example.com" `
    -H "Content-Type: application/json" `
    --data '{"jsonrpc":"2.0","id":"origin-1","method":"ping"}' `
    http://127.0.0.1:8765/mcp
```

Expected result:

```text
HTTP/1.1 403 Forbidden
Forbidden origin
```

Requests without an `Origin` header and requests from `localhost` or
`127.0.0.1` origins are allowed.

## 18. Connect an MCP-capable desktop client

Client configuration formats vary, but the required values are:

```text
Transport: Streamable/request-response HTTP
URL: http://127.0.0.1:8765/mcp
Header: Authorization: Bearer <current access token>
```

Before starting the client:

1. Confirm Shary shows the MCP server as running.
2. Confirm `adb forward --list` contains port `8765`.
3. Confirm the connector token has not expired.
4. Confirm the client sends the `Authorization` header on `tools/call`.

For write tests, instruct the client to:

1. Generate a unique `idempotency_key`.
2. Show the proposed mutation to the user.
3. Set `confirm=true` only after approval.
4. Prefer `shary.actions.preview` followed by `shary.actions.commit` for
   multi-item mutations.

## 19. Verify audit behavior

Return to **Settings > Assistant Connectors**.

Confirm:

- **Active connectors** increased after each pairing and never exceeds `5/5`.
- Each active connector has an individual **Revoke** action.
- Read and write operations appear under **Recent activity**.
- Denied read-only writes appear as denied activity.
- Successful operations identify the operation and target.

The audit trail is part of the E2E acceptance criteria, not an optional check.

## 20. Stop and clean up

Stop the server in Shary:

1. Open **Settings > Assistant Connectors**.
2. Press **Stop MCP**.
3. Confirm the state changes to stopped.

Stopping MCP does not revoke connector credentials. After finishing the test,
use **Revoke** for the test connectors or **Revoke all** if no connector should
remain authorized.

Remove host forwarding:

```powershell
# Delete the host TCP 8765 forwarding rule from the selected ADB target.
# Here, Windows can no longer reach the stopped device-local MCP listener.
adb forward --remove tcp:8765

# List remaining forwarding rules to verify that the cleanup succeeded.
adb forward --list
```

Delete the test records from Shary if they are no longer needed:

- `e2e-existing-field`
- `e2e-mcp-field`
- `e2e-batch-field`
- `e2e-existing@example.test`
- `e2e-mcp@example.test`
- `e2e-batch@example.test`

## Troubleshooting

### Connection refused

Check:

```powershell
# List Android targets to verify that the intended device is connected and authorized.
adb devices

# List ADB forwarding rules to verify the host-to-device 8765 tunnel exists.
adb forward --list

# Probe the forwarded URL and print headers to distinguish transport failures
# from the expected MCP response to an unsupported GET request.
curl.exe -i http://127.0.0.1:8765/mcp
```

Then confirm the app process is alive and **Start MCP** was pressed.

### Device is `unauthorized`

Unlock the physical device and accept its **Allow USB debugging?** RSA prompt.
If no prompt appears, restart the ADB server:

```powershell
# Stop the current ADB background process and discard stale USB connections.
adb kill-server

# Start a fresh ADB process and rescan connected Android targets.
adb start-server

# Verify that the target now reports `device` instead of `unauthorized`.
adb devices
```

If necessary, revoke USB debugging authorizations in Android **Developer
options**, reconnect the device, and authorize the computer again.

### Port 8765 is already in use

Find the Windows listener:

```powershell
# Query Windows TCP endpoints bound to local port 8765.
# Here, it identifies a process or stale tunnel competing with the MCP port.
Get-NetTCPConnection -LocalPort 8765 -ErrorAction SilentlyContinue
```

Remove stale ADB forwarding and retry:

```powershell
# Remove any existing ADB forwarding rule for host port 8765.
# This prevents an old target mapping from conflicting with the new test.
adb forward --remove tcp:8765

# Recreate the 8765 host-to-device tunnel for the currently selected target.
adb forward tcp:8765 tcp:8765
```

### Tool call returns HTTP 200 but failed

Inspect:

```powershell
# Read the MCP tool-level success flag from the saved response.
# HTTP 200 can still contain a tool failure, so `True` means the operation failed.
$Response.result.isError

# Display the structured payload that contains result data or the specific error.
$Response.result.structuredContent
```

MCP tool errors use `result.isError = true`.

### `unauthorized_scope`

The paired connector lacks the required capability. Pair read-write for write
operations or use a read tool with the read-only connector.

### `confirmation_required`

Add:

```powershell
# Set the mutation argument that records explicit approval under strict policy.
confirm = $true
```

Only do this after reviewing the mutation.

### `validation_failed`

Check required arguments and include a non-empty, unique `idempotency_key` for
mutations.

### Session expired

Revoke the expired connector, pair a new connector, and replace `$Token`. The
default session TTL is 20 minutes. Revoking first prevents expired client
records from consuming one of the five active connector slots.

### Pair buttons are disabled

Pairing requires the MCP server to be running and fewer than five active
connectors. Start MCP, then revoke an unused connector if the counter is `5/5`.
Stopping MCP intentionally leaves credentials paired; use **Revoke** or
**Revoke all** to invalidate them.

### Changes are not visible in the UI

Navigate away and back to the affected screen, or trigger its refresh flow.
Then confirm the tool response has `isError = false`.

## E2E acceptance checklist

- [ ] Debug Kotlin compilation passes.
- [ ] Focused MCP tests pass.
- [ ] Android app starts and vault data is accessible.
- [ ] MCP server reports running.
- [ ] Active connector count is within the configured `5/5` limit.
- [ ] ADB forwarding is active.
- [ ] `initialize`, `ping`, and `tools/list` succeed.
- [ ] Read-only field and user reads succeed.
- [ ] Read-only write is denied with `unauthorized_scope`.
- [ ] Read-write field create/update succeeds.
- [ ] Read-write user create/update succeeds.
- [ ] Preview returns a `pending_id`.
- [ ] Commit applies the previewed batch.
- [ ] Repeated commit is idempotent.
- [ ] Missing confirmation and idempotency controls are enforced.
- [ ] Invalid origin is rejected.
- [ ] Connector activity appears in Settings.
- [ ] Test connectors are revoked after use.
- [ ] Server and ADB forwarding are stopped after testing.
