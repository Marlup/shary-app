# Raw order backup - public OAuth multi-vault MCP implementation

## User request

Review the MCP-related code and documentation, then adapt and implement the
production flow so Codex, Claude, and similar agents can:

- authorize through the Shary owner instead of copied tokens;
- manage the owner's vault without ADB;
- attach additional vaults when their owners authorize the same agent;
- minimize connection steps while maximizing security;
- retain ADB only for development testing;
- notify the user when Firebase deployment is ready, without deploying
  automatically.

The final instruction was: `Implement the plan.`
