# Raw order backup - unify Contacts/Requests with the Fields aesthetic

## User order

> The Field screen is different to Contacts and Request ones. Bring and adapt the aesthetics and structure from fields to contacts/users and requests.

## Clarifying answers

- Bottom shelf: adopt Fields-style full-width labeled buttons (icon + text, 52dp, weight-equal) for Contacts and Requests.
- Search bar: make Contacts/Requests search full-width (weight 1f) like `FieldsToolbar`.
- Loading skeleton: only where it makes sense; analyze first.
- Sort/filter chips: add for Contacts only. Design Contact tag logic inspired by Fields. Suggested groups: friends, employees, colleagues, family, unknown, vendor.
- Contact tag model: dedicated, seeded set (separate from Field tags), extensible add/rename/recolor/delete.
- Contact sort: Name / Tag / Added.
- Tag edit entry point: editor dialog + detail quick-edit.
- Implementation order: low-regression first (Part 1 layout/shelf, then wiring, then tag store). Record backlog artifacts.
