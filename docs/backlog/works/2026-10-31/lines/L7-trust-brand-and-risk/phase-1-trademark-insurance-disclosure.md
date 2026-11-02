# L7 Phase 1 — Trademark, Insurance, Disclosure, and Accessibility

Status: In progress (external actions pending)
Date: 2026-10-31
Owner: Founder (single-owner project)
Line: [L7 — Trust, Brand & Risk Transfer](../README.md)
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. Trademark search protocol

### 1.1 Offices and tools

- OEPM (Spain): https://www.oepm.es — local trademark search.
- EUIPO eSearch plus: https://euipo.europa.eu/eSearch/ — EU trademark search.
- WIPO Global Brand Database: https://www.wipo.int/branddb/ — international
  cross-check.
- Nice classes: 9 (software), 42 (SaaS/software services); consider 35
  (business/admin services) and 38 (telecommunications) only if the product
  expands.

### 1.2 Search terms

Search exact and phonetic variants:

- Shary
- Shary App
- Shary Vault
- Shari
- Sari
- Chary
- Shary + field/vault/secure/agent

Record for each result: mark, owner, office, class, status, filing date,
similarity (identical / confusingly similar / unrelated), and risk.

### 1.3 Risk matrix

| Finding | Risk | Action |
|---|---|---|
| Identical mark in class 9 or 42 | High | Do not launch under the name; choose a fallback |
| Confusingly similar mark in class 9 or 42 | Medium-high | Seek an attorney opinion before filing |
| Same mark in unrelated classes | Low-medium | File in the core classes; monitor |
| No relevant result | Low | File OEPM first, EUIPO next |

### 1.4 Filing options

| Route | Coverage | Cost range (verify) | When |
|---|---|---|---|
| OEPM | Spain | About EUR 150-300 plus attorney | First, before store assets |
| EUIPO | EU | About EUR 850-1,200 plus attorney | After OEPM or in parallel if budget allows |
| WIPO Madrid | Multiple countries | Higher | Later, after EU traction |

File in the S.L.'s name, not the founder's. If a filing predates incorporation,
execute an assignment to the company.

Search record template:
[phase-1-trademark-search-record.md](phase-1-trademark-search-record.md).

### 1.5 Filing checklist

- [ ] Run OEPM, EUIPO, and WIPO searches.
- [ ] Record the results and risk decision.
- [ ] Choose OEPM, EUIPO, or both.
- [ ] Prepare the mark representation (word mark first).
- [ ] Confirm classes 9 and 42; add 35/38 only if justified.
- [ ] File and store the receipt.
- [ ] Set a watch reminder for similar marks.
- [ ] Confirm the company owns the filing.

## 2. Insurance request for proposal

### 2.1 Coverage

Request quotes for:

- **Cyber liability:** privacy liability, network security liability, data
  breach response, business interruption, cyber extortion, media liability.
- **Technology errors and omissions / professional indemnity:** failure to
  deliver, software errors, negligent advice, contractual liability where
  insurable.
- **General liability:** optional at launch; required by some channels later.

### 2.2 Limits and deductibles

| Item | Target range |
|---|---|
| Per-claim limit | EUR 500k-1M |
| Aggregate limit | EUR 1M-2M |
| Deductible | EUR 2.5k-10k |
| Retroactive date | Before first paid user |
| Annual premium | EUR 500-1,500 for a solo launch (quote required) |

### 2.3 Broker questions

1. Is zero-knowledge encryption and the absence of plaintext server data
   understood and reflected in the policy?
2. Are regulatory fines covered? If not, state that clearly.
3. Are cloud-provider outages and subprocessor failures covered?
4. Are MCP/agent actions and approval-gated writes covered?
5. Are prior acts, known claims, and pre-existing breaches excluded?
6. What incident-response services are included?
7. What evidence does the insurer need: MFA, backups, pen-test, incident plan?
8. What is the notice period and claims process?
9. Does the policy cover Spanish/EU consumers and cross-border claims?
10. What is the premium and deductible for the target limits?

### 2.4 Bind trigger

Bind coverage before the first paid user, not after launch. Store the policy and
the broker's coverage summary in the external evidence folder.

## 3. Responsible disclosure policy (draft)

### 3.1 Scope

In scope:

- Shary Android app.
- Shary backend and Firebase functions.
- MCP gateway and OAuth flows.
- Public website and hosting.

Out of scope:

- Denial-of-service or load testing.
- Social engineering or phishing.
- Physical attacks.
- Third-party services (Google, Firebase, Play) unless the issue is Shary's
  integration.
- Reports that require accessing another user's data.

### 3.2 Contact and response

- Contact: `security@[domain]` (create the mailbox before publication).
- Acknowledge within 3 business days.
- Triage within 10 business days.
- Critical issues: contain and fix as soon as practicable; keep the reporter
  informed.
- No bug bounty at launch; recognition is optional.

### 3.3 Safe harbor

Shary will not pursue legal action for good-faith research that:

- respects privacy and does not access, modify, or exfiltrate another user's
  data;
- does not degrade the service or run denial-of-service tests;
- reports the issue promptly and does not publicly disclose it before a fix;
- complies with applicable law.

### 3.4 security.txt

Publish at `https://<domain>/.well-known/security.txt`:

```
Contact: mailto:security@[domain]
Expires: 2027-10-31T00:00:00.000Z
Preferred-Languages: en, es
Canonical: https://<domain>/.well-known/security.txt
Policy: https://<domain>/security
```

The current Firebase Hosting rewrite sends `/.well-known/**` to the MCP
function. Add a static `security.txt` or a function route so it is served.

### 3.5 References

- ISO/IEC 29147 (vulnerability disclosure).
- ISO/IEC 30111 (vulnerability handling).

## 4. Accessibility applicability

### 4.1 Legal frame

- Directive (EU) 2019/882 (European Accessibility Act), applicable from 28 June
  2025.
- Spanish transposition: verify the current text (Ley 11/2023 is the candidate;
  confirm scope and exceptions with the abogado).
- Microenterprise service-provider exemption: likely applies to a one-person
  project with turnover at or below EUR 2M. This must be documented, not assumed.
- Even if exempt, accessibility remains a quality and store-review issue.

### 4.2 Decision tree

1. Does Shary provide an in-scope service (e-commerce/digital service)?
2. Is the S.L. a microenterprise (fewer than 10 employees and turnover or
   balance sheet at or below EUR 2M)?
3. Does the Spanish transposition exempt microenterprises providing services?
4. If exempt, record the decision and still run a baseline accessibility audit.
5. If not exempt, complete an EN 301 549 conformance audit before launch.

### 4.3 Baseline checklist (app and store surface)

- [ ] Content descriptions for icons and meaningful images.
- [ ] Touch targets at least 48dp.
- [ ] Contrast at least 4.5:1 for normal text.
- [ ] Font scaling to 200% without clipping or lost actions.
- [ ] TalkBack traversal and focus order.
- [ ] Switch Access compatibility.
- [ ] No color-only meaning.
- [ ] Error identification and suggestions.
- [ ] Consistent navigation and headings.
- [ ] Accessible legal documents (HTML, not image-only PDF).
- [ ] Store listing text readable and descriptive.
- [ ] No timed content that cannot be extended.
- [ ] Orientation changes do not lose state.

### 4.4 Audit method

- Android Accessibility Scanner.
- Manual TalkBack and Switch Access pass.
- Compose semantics tests for critical flows.
- External audit if the exemption does not apply or if a store partner requires
  it.

## 5. Trust page outline

Publish after L6 and L2 are ready:

1. What Shary is and what it does not do.
2. Legal entity and contact.
3. Privacy Policy and ToS links.
4. Subprocessor list.
5. Security posture: encryption, key custody, approved review date and scope.
6. Responsible disclosure policy and security.txt.
7. Status page and incident communication.
8. Data stewardship and wind-down commitment.
9. Accessibility statement or exemption decision.
10. Last reviewed date and change log.

Do not overclaim. Use the L6 report as the source and state its date and scope.

## 6. External actions

| Action | Owner | Status |
|---|---|---|
| Run OEPM/EUIPO/WIPO searches | Founder | Pending |
| Choose the filing route | Founder | Pending |
| File the trademark | Founder/attorney | Pending |
| Request insurance quotes | Founder/broker | Pending |
| Bind insurance | Founder | Pending |
| Create security@ mailbox | Founder | Pending |
| Publish security.txt and policy | Founder | Pending |
| Confirm EAA applicability | Founder/abogado | Pending |
| Run the accessibility baseline | Founder | Pending |
| Publish the trust page | Founder | Pending |

## 7. Validation mapping

| Criterion | Evidence | Status |
|---|---|---|
| L7-V1 Trademark search | Section 1 | Pending |
| L7-V2 Trademark filed | Filing receipt | Pending |
| L7-V3 Licensing decision | Not recorded | Pending |
| L7-V4 Contributor/IP hygiene | Not recorded | Pending |
| L7-V5 Insurance bound | Policy | Pending |
| L7-V6 Disclosure live | Section 3 | Draft |
| L7-V7 Accessibility audited | Section 4 | Pending |
| L7-V8 Trust page | Section 5 | Pending |
| L7-V9 Stewardship published | L5 | Pending |
| L7-V10 Brand use clear | Section 1 | Pending |
