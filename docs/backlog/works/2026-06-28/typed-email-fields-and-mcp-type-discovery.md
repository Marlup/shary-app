# Typed email fields and MCP field-type discovery

## Scope

Add `email` to Shary's typed field values and extend local and remote MCP contracts so agents can discover supported types and create/update fields with a selected type, alias, and tag.

## Change list

1. Added a canonical field-value type registry with `plain`, `email`, `list`, `link_url`, `json`, `questionnaire`, `yaml`, and `file`.
2. Added email selection, keyboard behavior, auto-detection, and validation to the field editor.
3. Added the immediate `shary.fields.types` MCP tool.
4. Added optional `value_type` to direct and batch field writes while preserving untyped writes as plain text.
5. Preserved `key_alias` and fixed connector tag parsing so supplied tags are stored instead of degrading to `Unknown`.
6. Added value type to field metadata and persisted it through pending batch actions.
7. Updated Android, backend, executable scenario, and contract documentation.

## Design decisions

- `value_type` is the canonical MCP argument because batch actions already use `type` for the mutation kind.
- `shary.fields.types` is immediate and does not queue a device operation.
- Omitting `value_type` is backward-compatible and stores the original plain value.
- Android remains authoritative for typed-value encoding and email validation.
- The backend validates canonical type names before encrypting and queueing commands.

## Validation plan

- Android focused and full unit tests.
- Android production and instrumentation-test compilation.
- Isolated MCP backend tests and frozen backend regression tests.
- Python compile checks, JSON parsing, and diff whitespace checks in both repositories.

## Result summary

Focused Android tests and the isolated MCP backend suite passed during implementation. Final broad validation is recorded in the delivery response.
