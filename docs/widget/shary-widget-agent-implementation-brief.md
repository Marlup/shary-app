# Shary Widget/Plugin Agent Implementation Brief (Detailed)

## 1) Mission
Build a first functional web iteration of the Shary embeddable widget and template plugin so a target user can define required fields and external users can submit securely from any website.

## 2) Non-Negotiable Requirements
- Keep architecture modular and package-based.
- Widget public integration for V1 must be script-tag only.
- Published template versions are immutable.
- Server validates all submissions regardless of client checks.
- No acceptance of data when token/origin/template checks fail.
- Deliver runnable demo for fast trial.
- GDPR baseline required with retention default of 30 days.

## 3) Proposed Repo Layout
```text
web/
  packages/
    widget-core/          # Schema, validation, API client, events
    widget-ui/            # Web component implementation + styles
    template-plugin/      # Template builder UI + embed snippet generator
  services/
    ingestion-api/        # Config + submission + file presign endpoints
  apps/
    demo-host/            # Simulated partner website
  docs/
    api/
    architecture/
```

## 4) Implementation Sequence
### Phase 1: Foundations
1. Initialize `pnpm` workspace and TypeScript build setup.
2. Add shared schema package with strict runtime validation (e.g., Zod).
3. Define domain models:
- `Template`
- `TemplateField`
- `WidgetConfig`
- `SubmissionPayload`
- `SubmissionResult`

### Phase 2: Backend First
1. Create `GET /api/widget/config`.
2. Create `POST /api/widget/submissions`.
3. Create `POST /api/widget/files/presign`.
4. Add middleware:
- JWT/HMAC token verification
- CORS origin check (exact origin match only, no wildcard support in V1)
- rate limit
- request ID logging
5. Implement optional OTP flow:
- `verificationMode: none | email_otp`
- anonymous mode allowed, OTP optional per template
6. Add lightweight persistence (Postgres or temporary SQLite for local trial).
7. Add outbound signed webhook delivery with retries for:
- `submission.accepted`
- `submission.rejected`
- `file.scan.failed`
 - retry policy: max `3` retries inside `60 seconds`

### Phase 3: Widget SDK + UI
1. Implement declarative script-tag auto-init from `data-shary-*`.
2. Keep internal mount API if needed, but do not expose as public V1 contract.
3. Render dynamic form from schema with required field types.
4. Add client validation and file uploader flow.
5. Add event hooks:
- `onReady`
- `onValidationError`
- `onSubmitSuccess`
- `onSubmitError`
6. Implement isolated styling (Shadow DOM + CSS variables).
7. Add runtime i18n:
- browser language autodetect
- manual language override selector
- resources in central `i18n` folder

### Phase 4: Template Plugin UI
1. Build form-template builder for target users.
2. Add preview mode using the same widget renderer.
3. Add publish action to freeze template version.
4. Add embed code generator (script tag + programmatic snippet).

### Phase 5: Demo + QA
1. Build `demo-host` with:
- one plain HTML page
2. Add E2E tests (Playwright/Cypress):
- successful submission
- required field failure
- invalid token rejection
- wrong origin rejection
 - optional OTP verification flow
 - webhook emission path
3. Add smoke CI for build + tests.

## 5) Contract Details to Implement
### Widget init options
```ts
type SharyWidgetInit = {
  container: string | HTMLElement;
  targetUserId: string;
  templateId: string;
  widgetToken: string;
  prefill?: Record<string, string>;
  locale?: string;
  theme?: "light" | "dark" | "system";
  onReady?: () => void;
  onValidationError?: (errors: Array<{ key: string; message: string }>) => void;
  onSubmitSuccess?: (result: { submissionId: string }) => void;
  onSubmitError?: (error: { code: string; message: string }) => void;
};
```

Public V1 docs must only include script-tag integration.

### Field types required in V1
- `text`
- `textarea`
- `email`
- `phone`
- `number`
- `date`
- `select`
- `multiselect`
- `checkbox`
- `radio`
- `file`
- `country`
- `address`
- `iban`
- `tax_id`
- `policy_number`
- `order_id`
- `invoice_id`
- `membership_id`
- `consent`

### File upload flow
1. Widget requests presigned URL.
2. Widget uploads directly to object storage.
3. Widget sends file key in final submission payload.
4. Enforce template-level upload enablement.
5. Enforce max `3` files/submission and max `32MB` absolute per file.
6. Reject uploads when template has `uploadsEnabled=false`.

### Localization requirements
Create translation resources in:
- `web/packages/widget-core/src/i18n/en.json`
- `web/packages/widget-core/src/i18n/es.json`
- `web/packages/widget-core/src/i18n/fr.json`
- `web/packages/widget-core/src/i18n/de.json`
- `web/packages/widget-core/src/i18n/pl.json`
- `web/packages/widget-core/src/i18n/ro.json`
- `web/packages/widget-core/src/i18n/pt.json`
- `web/packages/widget-core/src/i18n/it.json`

## 6) Security Acceptance Criteria
- Token expiry enforced.
- Token must bind `templateId`, `targetUserId`, and allowed origin.
- API rejects payload if template version is outdated or mismatched.
- Server-side schema validation blocks unknown fields.
- File upload enforces max size and allowed MIME/extension.
- Rate limit returns deterministic error contract.
- Default token limit is `60/hour` and adjustable by configuration.
- IP limit is enforced in parallel as a secondary guardrail.
- Anonymous mode supported.
- OTP verification is optional and template-driven.
- GDPR retention default `30 days`.
- Data storage must remain in EU region for V1 environments.

## 7) Quality Gates
- TypeScript strict mode enabled in all packages.
- Minimum unit tests:
- schema validation
- widget init parser (`data-shary-*`)
- submission payload normalization
- Minimum E2E tests:
- happy path
- validation error path
- auth/origin failure path
- API returns typed error shape (no raw stack traces).

## 8) Deliverables
- Build artifacts:
- `shary-widget.min.js`
- `shary-widget.css` (or bundled styles in component)
- Running services:
- `ingestion-api`
- `template-plugin`
- `demo-host`
- Operational UI:
- submission dashboard with list/filter/detail/export CSV
- Documentation:
- integration guide
- API contract
- security notes
- known limitations

## 9) Definition of Done
- A tester can create template in plugin, publish it, copy embed code into demo host, submit data + file, and observe stored submission ID.
- Invalid token/origin/field payload paths are blocked with clear errors.
- Optional OTP flow works when enabled; anonymous flow works when disabled.
- Webhook events are emitted and signed.
- Setup from clean clone to working local demo in <= 20 minutes.

## 10) Agent Execution Instructions (Copy/Paste Prompt)
Use the text below to drive an implementation agent:

```text
You are implementing V1 of a Shary embeddable widget + template plugin.

Constraints:
1) Build modular packages: widget-core, widget-ui, template-plugin, ingestion-api, demo-host.
2) Public integration must be script tag only in V1.
3) Use strict TypeScript and runtime schema validation.
4) Enforce server-side security: token verification, origin checks, rate limit, schema validation.
5) Published template versions are immutable.
6) Do not break existing Android project files; isolate work under /web.
7) Implement optional OTP and optional CAPTCHA integration hooks.
8) Implement GDPR baseline with retention default 30 days.

Required outputs:
1) Local runnable setup with scripts for install, dev, test, build.
2) Working demo pages showing integration.
3) Automated tests for happy path + key failure paths.
4) Concise docs for integration and API.

Execution order:
1) Scaffold monorepo and shared schema models.
2) Implement ingestion API endpoints + middleware.
3) Build widget SDK and rendering.
4) Build template plugin and snippet generator.
5) Add demo host integration and E2E tests.
6) Validate end-to-end flow and produce setup instructions.

Completion criteria:
- End-to-end submission flow works and returns submissionId.
- Invalid origin/token/payload are rejected.
- Integration is reusable and host-framework agnostic.
- i18n supports en/es/fr/de/pl/ro/pt/it with browser autodetect + manual override.
- Webhooks fire for accepted/rejected/scan-failed events.
```
