# Shary Widget V1 Frozen Contract

Freeze date: `2026-04-12`

This document defines the implementation contract for V1 trials.

## 1) Fixed V1 Decisions
- Public integration mode: script-tag only.
- Backend: dedicated `shary-ingestion-api` service.
- User mode: anonymous by default, optional OTP per template.
- Verification modes: `none | email_otp`.
- Uploads: only when template enables uploads.
- File size: hard max `32MB` per file; max `3` files/submission.
- Rate limits: token default `60/hour` (configurable), IP rate limit enforced in parallel.
- Webhooks: enabled with retries (`3` retries total within `60s`).
- Data residency: EU-only storage for DB and object storage.
- Origin policy: strict exact origin matching only.

## 2) Script-Tag Integration Contract
Required attributes:
- `data-shary-target-user`
- `data-shary-template`
- `data-shary-token`
- `data-container`

Optional attributes:
- `data-prefill-email`
- `data-prefill-username`
- `data-prefill-*` (custom)
- `data-language` (`en|es|fr|de|pl|ro|pt|it`)
- `data-theme`

## 3) Template Schema Contract (V1)
```json
{
  "templateId": "claims-intake-v1",
  "targetUserId": "insurer_001",
  "version": 1,
  "title": "Claim Intake Form",
  "description": "Upload required claim data",
  "verificationMode": "none",
  "uploadsEnabled": true,
  "maxFilesPerSubmission": 3,
  "defaultFileMaxSizeMb": 20,
  "absoluteFileMaxSizeMb": 32,
  "retentionDays": 30,
  "allowedOrigins": ["https://partner-site.com"],
  "rateLimitPerIpPerHour": 30,
  "rateLimitPerTokenPerHour": 60,
  "consent": {
    "text": "I confirm the provided data is accurate and consent to processing.",
    "required": true
  },
  "fields": [
    { "key": "email", "type": "email", "required": true },
    { "key": "order_id", "type": "order_id", "required": false },
    { "key": "supporting_document", "type": "file", "required": false, "maxSizeMb": 20 }
  ],
  "webhooks": {
    "enabled": true,
    "events": [
      "submission.accepted",
      "submission.rejected",
      "file.scan.failed"
    ],
    "retryPolicy": {
      "maxRetries": 3,
      "totalWindowSeconds": 60
    }
  }
}
```

## 4) API Contract
Base path: `/api/widget`

### 4.1 `GET /config`
Query:
- `targetUserId`
- `templateId`
- `widgetToken`

Response `200`:
```json
{
  "template": {},
  "themeDefaults": {},
  "policy": {
    "verificationMode": "none",
    "uploadsEnabled": true
  }
}
```

### 4.2 `POST /otp/start` (optional flow)
Body:
```json
{
  "targetUserId": "insurer_001",
  "templateId": "claims-intake-v1",
  "email": "guest@example.com"
}
```
Response `200`:
```json
{
  "otpSessionId": "otp_sess_123",
  "expiresInSeconds": 300
}
```

### 4.3 `POST /files/presign`
Body:
```json
{
  "targetUserId": "insurer_001",
  "templateId": "claims-intake-v1",
  "filename": "invoice.pdf",
  "mime": "application/pdf",
  "sizeBytes": 1048576
}
```
Response `200`:
```json
{
  "fileKey": "tmp/2026/04/12/abc123",
  "uploadUrl": "https://storage.example/upload-signed-url",
  "expiresInSeconds": 300
}
```

### 4.4 `POST /submissions`
Body:
```json
{
  "targetUserId": "insurer_001",
  "templateId": "claims-intake-v1",
  "templateVersion": 1,
  "consentAccepted": true,
  "locale": "es",
  "fields": {
    "email": "guest@example.com",
    "order_id": "ORD-1001"
  },
  "files": [
    {
      "fieldKey": "supporting_document",
      "fileKey": "tmp/2026/04/12/abc123"
    }
  ],
  "otp": {
    "otpSessionId": "otp_sess_123",
    "otpCode": "123456"
  },
  "metadata": {
    "partnerId": "partner_01",
    "campaignId": "spring-2026"
  }
}
```
Response `202`:
```json
{
  "submissionId": "sub_01JV...",
  "status": "accepted"
}
```

## 5) Error Contract
All non-2xx responses:
```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "One or more fields are invalid.",
    "details": []
  },
  "requestId": "req_01JV..."
}
```

Error code set:
- `AUTH_TOKEN_INVALID`
- `AUTH_TOKEN_EXPIRED`
- `ORIGIN_NOT_ALLOWED`
- `TEMPLATE_NOT_FOUND`
- `TEMPLATE_VERSION_MISMATCH`
- `OTP_REQUIRED`
- `OTP_INVALID`
- `OTP_EXPIRED`
- `FILE_UPLOAD_DISABLED`
- `FILE_TOO_LARGE`
- `FILE_TYPE_NOT_ALLOWED`
- `RATE_LIMIT_IP_EXCEEDED`
- `RATE_LIMIT_TOKEN_EXCEEDED`
- `CONSENT_REQUIRED`
- `VALIDATION_FAILED`
- `INTERNAL_ERROR`

## 6) Webhook Contract
Events:
- `submission.accepted`
- `submission.rejected`
- `file.scan.failed`

Headers:
- `X-Shary-Event`
- `X-Shary-Delivery-Id`
- `X-Shary-Timestamp`
- `X-Shary-Signature` (`sha256=<hex(hmac_sha256(raw_body, webhook_secret))>`)

Retry behavior:
- Up to `3` retries in `60` seconds total.
- Stop retries on first HTTP `2xx`.

## 7) Security and Compliance Guardrails
- Validate token, origin, and template binding on every request.
- Origin matching is exact (scheme + host + port).
- No wildcard origins in V1.
- Server-side schema validation is mandatory.
- Store submission data and files in EU region only.
- Enforce retention default of `30` days.

