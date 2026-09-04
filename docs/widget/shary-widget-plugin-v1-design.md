# Shary Website Plugin + Widget (V1 Fast Trial Design)

## 1) Goal
Create a reusable website integration that lets any external (not-registered) user submit structured data to a target Shary account (company, institution, etc.) through a configurable widget.

The target user defines required fields once (global template), and website owners embed a widget snippet with minimal setup.

## 2) Core Concepts
- `Target User`: the Shary account that receives submissions (e.g., insurer, retailer).
- `Template`: list of fields + validation + consent text + policy rules defined by the target user.
- `Plugin`: admin-facing module used to create/manage templates and generate embed code.
- `Widget`: embeddable front-end form shown on third-party websites for external users.
- `Submission`: payload sent by external users to Shary backend.

## 3) V1 Scope (Functional First Iteration)
### In scope
- Template creation/editing with core field types.
- Embed widget in any site with a single script (script-tag only in V1).
- Optional prefilled values (`email`, `username`, custom fields).
- Anonymous external user flow with optional `email OTP` verification by template.
- Client-side + server-side validation.
- Submission API with auth token, rate limiting, and audit metadata.
- Secure file upload for selected templates only (size/type restrictions).
- Basic success/failure states and submission tracking ID.
- Optional CAPTCHA integration toggle.
- Outbound webhook events for downstream systems.
- Basic submissions dashboard (list/filter/detail/export CSV).
- Built-in multilingual widget runtime with centralized translation files.
- Demo host website for quick trials.

### Out of scope (V2+)
- Full workflow automation/routing.
- Translation-management admin UI.
- Advanced conditional logic between fields.
- Advanced analytics dashboard.
- Complex identity verification (KYC/eID).

## 4) Recommended Architecture
### Components
1. `Template Plugin` (admin app)
- Used by target users to define templates and generate integration snippet.

2. `Widget SDK` (embeddable JS package)
- Loads template configuration from backend.
- Renders form UI and handles validation/submission.

3. `Submission API`
- Verifies widget token, template version, field schema.
- Stores encrypted metadata and payload references.

4. `File Upload Service`
- Presigned upload URL flow.
- Virus scan hook placeholder.

5. `Storage`
- Template store.
- Submission store.
- Optional object storage for files.

### Suggested Stack (for speed + modularity)
- Frontend widget: `TypeScript + Web Components` (framework-agnostic embedding).
- Plugin/admin UI: `Vue 3 + TypeScript` (or React if preferred).
- API: `TypeScript (Node.js/Express or Fastify)` for fastest MVP.
- DB: PostgreSQL (templates, submissions metadata).
- Files: S3-compatible storage.
- i18n: `i18next` with locale JSON resources bundled in widget core.

Reason: web component SDK keeps host integration simple for any website (plain HTML, Vue, React, CMS).

## 5) Embedding Model (Host Website)
### Option A: Script tag (lowest friction)
```html
<div id="shary-widget"></div>
<script
  src="https://cdn.shary.com/widget/v1/shary-widget.min.js"
  data-shary-target-user="insurer_001"
  data-shary-template="claims-intake-v1"
  data-shary-token="SIGNED_PUBLIC_WIDGET_TOKEN"
  data-prefill-email="guest@example.com"
  data-prefill-username="john_doe"
  data-theme="light"
  data-container="#shary-widget"
></script>
```

V1 integration policy:
- Public integration documentation is script-tag only.
- Internal widget code can keep programmatic APIs for maintainability, but not as public V1 contract.

## 6) Template Schema (V1)
```json
{
  "templateId": "claims-intake-v1",
  "targetUserId": "insurer_001",
  "version": 1,
  "title": "Claim Intake Form",
  "description": "Upload required claim data",
  "fields": [
    {
      "key": "full_name",
      "label": "Full name",
      "type": "text",
      "required": true,
      "minLength": 2,
      "maxLength": 120
    },
    {
      "key": "email",
      "label": "Email",
      "type": "email",
      "required": true
    },
    {
      "key": "claim_amount",
      "label": "Claim amount",
      "type": "number",
      "required": false,
      "min": 0
    },
    {
      "key": "supporting_document",
      "label": "Supporting document",
      "type": "file",
      "required": true,
      "accept": [".pdf", ".jpg", ".png"],
      "maxSizeMb": 10
    }
  ],
  "consent": {
    "text": "I confirm the provided data is accurate and consent to processing.",
    "required": true
  },
  "verificationMode": "none",
  "uploadsEnabled": true,
  "maxFilesPerSubmission": 3,
  "defaultFileMaxSizeMb": 20,
  "absoluteFileMaxSizeMb": 32,
  "retentionDays": 30,
  "webhooks": {
    "enabled": true,
    "retryPolicy": {
      "maxRetries": 3,
      "totalWindowSeconds": 60
    },
    "events": [
      "submission.accepted",
      "submission.rejected",
      "file.scan.failed"
    ]
  },
  "allowedOrigins": ["https://partner-site.com"],
  "rateLimitPerIpPerHour": 30,
  "rateLimitPerTokenPerHour": 60
}
```

## 7) API Contract (V1)
### `GET /api/widget/config`
Input:
- `targetUserId`
- `templateId`
- `widgetToken`

Output:
- sanitized template schema
- theme defaults
- policy flags

### `POST /api/widget/submissions`
Input:
- template identifiers + version
- field values
- file references
- consent accepted
- optional verification proof (`otpSessionId`, `otpCode`) when template requires it

Output:
- `submissionId`
- status (`accepted` | `rejected`)
- validation errors if any

### `POST /api/widget/files/presign`
Input:
- filename, mime, size

Output:
- presigned URL + temporary file key

### Webhook events (outbound)
Events emitted to configured endpoint:
- `submission.accepted`
- `submission.rejected`
- `file.scan.failed`

Retry policy:
- max `3` retries
- retry window `60 seconds` total

## 8) Security Baseline (Must-Have in V1)
- Signed widget tokens (JWT or HMAC), short TTL, bound to `templateId + origin`.
- Strict CORS allowlist by template settings (exact origin match only, no wildcards).
- Server-side schema validation (never trust client).
- Rate limit by IP + token + template.
- File type and size checks on client and server.
- Malware scan hook (can be async placeholder in V1).
- Submission auditing: timestamp, target user, origin, trace ID.
- Optional `hCaptcha/Cloudflare Turnstile` toggle for abuse control (off by default).
- Data minimization: store only required metadata + encrypted payload.
- GDPR baseline: explicit consent capture, consent text versioning, retention default `30 days`, deletion workflow.
- Data hosting policy for V1: EU-only region for database and object storage.

## 9) Reusability and Modularity Rules
- `@shary/widget-core`: field schema, validation, API client, events.
- `@shary/widget-ui`: rendering layer (web component + styles).
- `@shary/template-plugin`: admin template builder and embed snippet generator.
- Stable contract through versioned schema (`version` field mandatory).
- Theming via CSS variables only, no host CSS dependency.
- Shadow DOM for style isolation.
- Event hooks (`onReady`, `onValidationError`, `onSubmitSuccess`, `onSubmitError`).
- i18n resources under `web/packages/widget-core/src/i18n/`:
  - `en`, `es`, `fr`, `de`, `pl`, `ro`, `pt`, `it`
  - browser-language auto-detect + manual override selector in widget UI

## 10) First Functional Iteration: Delivery Slice
### Target result
A partner can copy one snippet into a website, submit data + one file, and the target user receives a stored submission record in backend.

### Minimal feature checklist
- Template CRUD (create/edit/publish/archive).
- Field types:
  - Core: `text`, `textarea`, `email`, `phone`, `number`, `date`, `select`, `multiselect`, `checkbox`, `radio`, `file`, `consent`
  - Domain-oriented: `country`, `address`, `iban`, `tax_id`, `policy_number`, `order_id`, `invoice_id`, `membership_id`
- Widget render from remote template.
- Prefill support for `email`, `username`, and arbitrary keys.
- Per-field prefill mode: `editable` or `locked` (default `editable`).
- Submission endpoint with full server validation.
- One demo integration page.
- Webhook delivery with signed payload and retry.
- Rate limits: token default `60/hour` (adjustable per template/config), IP limit enforced as separate guardrail.
- Basic submissions dashboard (list/filter/detail/export CSV).
- Basic tests for form validation and submit flow.

## 11) Trial Plan (Fast Testing)
### Environments
- `local`: mock API + local demo host.
- `staging`: real API + isolated test tenant.

### Test scenarios
1. Happy path: complete form and upload valid file.
2. Missing required fields.
3. Invalid type (bad email, oversized file).
4. Invalid/expired token.
5. Wrong origin.
6. Rate limit exceeded.

Success criteria:
- Integration time under 15 minutes.
- >95% successful submissions in happy-path test batch.
- No data accepted if token or schema is invalid.
- Capacity target for pilot: ~`3600 submissions/day`, baseline `8 concurrent users`, with burst tolerance.

## 12) Backend Placement Recommendation
Use a dedicated `shary-ingestion-api` service in V1, decoupled from Android app internals.

Why:
- Faster independent iteration for web trials.
- Cleaner security boundary for external submissions.
- Easier future integration (proxy, merge, or event bridge) after pilot validation.

## 13) Industry Starter Templates (Examples)
- Banking onboarding / claims.
- Retail returns / warranty claims.
- Healthcare document intake.
- Energy supplier incident/contract intake.
- Gym membership and health declaration intake.

Each starter template should be configurable, not hardcoded.

## 14) Key Risks and Mitigations
- Host website CSS/JS conflicts.
  - Mitigation: Shadow DOM, namespaced events, no global CSS leakage.
- Spam or bot traffic.
  - Mitigation: rate limit + captcha toggle.
- Template breaking changes.
  - Mitigation: immutable published versioning.
- File security.
  - Mitigation: presigned upload + server-side checks + scanning hook.

## 15) Suggested V1 Timeline (Aggressive)
- Day 1-2: schema + API + mock storage.
- Day 3-4: widget core + form rendering + submission.
- Day 5: template plugin UI + snippet generation.
- Day 6: demo host integration + tests.
- Day 7: bugfix + pilot trial.
