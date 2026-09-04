# Shary App Executive Overview

## What Shary is

Shary is an Android application for keeping sensitive, structured personal
information under the owner's control and sharing only the exact data needed
for a specific interaction.

At its core, Shary combines a local vault, recipient management, selective
sharing flows, encrypted cloud relay, and approval-based AI/agent access.

## Product intention

Shary is intended to solve a common trust problem:

- People need to store small but sensitive pieces of information.
- They often need to share only part of that information with specific people.
- They need stronger control than "send the whole note" or "give the tool full
  account access."

The app therefore aims to:

- Keep personal data usable even when cloud services are unavailable.
- Make sharing selective rather than all-or-nothing.
- Preserve owner review and consent at the moment data leaves the device.
- Let external AI/agent tools help only through explicit, auditable grants.

## Core capabilities

### 1. Personal structured vault

Shary stores personal data as structured fields rather than loose notes. This
makes it easier to search, organize, review, and selectively share specific
items.

Current user-facing capabilities include:

- Creating and updating fields.
- Organizing fields with aliases and tags.
- Searching and sorting the vault.
- Recovering from some recent value changes.

### 2. Recipient directory

The app keeps a separate list of users/recipients so sharing is targeted and
repeatable.

This allows the owner to:

- Maintain trusted contacts.
- Reuse recipient selections across send/request flows.
- Separate "what I know" from "who I share with."

### 3. Secure send flows

Shary supports sending selected information to selected recipients through a
review step.

Today the app offers:

- Review-before-send summaries.
- Email as a direct channel.
- Encrypted cloud relay as the supported secure remote channel.

### 4. Request flows

Shary is not only for pushing information out. It also supports requesting
specific data from another person and reviewing inbound responses.

This makes it possible to:

- Send requests for named fields/keys.
- Receive pending cloud items in an inbox.
- Review what is new or duplicated before importing.
- Accept or reject inbound items with explicit owner action.

### 5. Privacy and control

Shary is designed around owner control rather than silent automation.

Current control mechanisms include:

- Offline-first local behavior for core vault usage.
- Visible cloud availability gating instead of hidden failures.
- Optional biometric gate before viewing cloud inbox items.
- Configurable Wi-Fi-only cloud sync.
- Expiry defaults for cloud-delivered items.

### 6. AI and agent connectivity

Shary now supports a product-facing MCP path for agentic tools through a
public OAuth flow.

That means:

- Tools such as Codex or Claude can connect through an OAuth-capable MCP
  client flow.
- The owner approves scopes inside Shary.
- Active grants can be reviewed and revoked from Settings.
- Sensitive write operations remain approval-gated on the device.

The older local loopback MCP setup is no longer the supported product path.

## Where Shary fits best

Shary is strongest when the owner wants:

- A private structured vault on Android.
- Selective encrypted sharing rather than bulk export.
- A bridge between human-controlled data and AI-assisted workflows.

It is not positioned as an unrestricted background automation tool. The product
direction keeps the device owner as the approval boundary.
