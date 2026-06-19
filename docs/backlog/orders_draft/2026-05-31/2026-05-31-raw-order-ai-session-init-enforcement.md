# Raw Order Backup - AI Session Init Enforcement

User request (verbatim):

> from 'Brief answer. How can I make codex follow the AGENT.md file and other ai-driven ones at each session init?
>
> • Put the enforcement in the repo, not in chat state:
>
> 1. Keep one canonical policy file (e.g., AGENT.md) at repo root, and make all other AI files reference it instead of duplicating rules.
> 2. Add a startup hook/script that prepends those files into the first prompt every session (your launcher/wrapper should cat/read them and inject as system/developer context).
>
> 3. Add a hard gate in init commands: fail startup if required files are missing or changed unexpectedly (checksum/version check).
> 4. In AGENT.md, include an explicit “session-init checklist” section that says which files must be loaded and in what order.
> 5. For Codex specifically, keep these rules in the developer/system instructions path (plugin/skill or session bootstrap), since user messages alone are not reliable enforcement.' Proceed, if unclear, ask questions
