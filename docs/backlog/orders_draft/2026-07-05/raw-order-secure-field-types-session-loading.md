# Raw Order - Secure Field Types, Session, and Loading

Date: 2026-07-05

```text
Changes: 1) Add dtypes: tasks (list of titles of tasks and value menu: todo/doing/done/review/cancel) and sensitive (field-value and field-alias hidden by '*****', EXACTLY five. At selection of card for details, ask for biometrics to reveal card). 2) Propose a graph logic and dependency relations between widgets and actions. The goal is to have a quick user and agents read of the map of the application at elements level. 3) When user exits the app without closing, activate user session close after 5 min. Goal is to avoid the session open and reveal data. 4) Evaluate background loading and decryption of fields in order to transition swiftly from Login-Screen (after successful login) to the Fields-Screen. The field cards and users will be loaded in real time. Suggest transition to Field-screen when at least 5 fields have being fully loaded (save in constant var.). This improvement will avoid unnecessarily long waitings at Login-screen.

Implement the plan.
```

## Approved Decision Addenda

- Store status per task and default it to `todo`.
- Require one biometric/device-credential approval per sensitive detail or review/send flow.
- Block sensitive MCP reads and updates by default with a local per-field toggle.
- Deny MCP creation or classification of sensitive fields.
- Keep the element/action graph as development-only Markdown.
