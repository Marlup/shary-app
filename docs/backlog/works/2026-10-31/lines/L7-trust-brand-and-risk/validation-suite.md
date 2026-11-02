# L7 — Validation Suite

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Scope reference: [README.md](README.md)
Phase plan: [phase-plan.md](phase-plan.md)

## 1. How to use

Filings and insurance require official receipts or policies. Disclosure and
trust-page criteria require a live URL and a monitored-channel test.
Accessibility requires an audit report or a documented exemption decision.

## 2. Viability criteria

| ID | Criterion | Method | Pass condition | Evidence | Official source |
|---|---|---|---|---|---|
| L7-V1 | Trademark search is complete | Document review | OEPM and EUIPO searches are recorded with classes and conflicts | EV-L7-01 | OEPM; EUIPO |
| L7-V2 | Trademark is filed | Document review | Filing receipt exists for the chosen classes and owner (the S.L.) | EV-L7-02 | OEPM; EUIPO |
| L7-V3 | Licensing decision is recorded | Document review | Open versus closed decision, license text, and rationale are recorded | EV-L7-03 | OSI; SPDX |
| L7-V4 | Contributor/IP hygiene is defined | Document review | CLA or DCO, third-party notices, and ownership rules are documented | EV-L7-04 | OSI; SPDX |
| L7-V5 | Insurance is bound | Document review | Cyber-liability and professional-indemnity policies are active before paid launch | EV-L7-05 | Ley 50/1980 |
| L7-V6 | Responsible disclosure is live | Manual drill | Policy, security.txt, and monitored inbox exist; a test report is acknowledged | EV-L7-06 | ISO/IEC 29147; 30111 |
| L7-V7 | Accessibility is audited | External review | EAA/EN 301 549 audit is complete; gaps are closed or formally accepted | EV-L7-07 | EAA; EN 301 549 |
| L7-V8 | Trust page is published | Document review | Entity, subprocessors, status, security posture, and disclosure policy are live and consistent | EV-L7-08 | L2/L5/L6 sources |
| L7-V9 | Stewardship is published | Document review | Trustee and wind-down commitment appear in the trust surface and ToS | EV-L7-09 | L5 clause; GDPR Art. 17 |
| L7-V10 | Brand use is clear | Document review | App name and icon do not knowingly infringe; search record is stored | EV-L7-10 | OEPM; EUIPO |

## 3. Test and drill matrix

| ID | Type | Procedure | Expected result |
|---|---|---|---|
| L7-T1 | Document review | Run OEPM and EUIPO searches for "Shary" in classes 9, 35, 38, and 42 | Conflicts recorded; filing decision documented |
| L7-T2 | Manual drill | Send a test vulnerability report to the disclosure inbox | Acknowledged within the published target |
| L7-T3 | External review | Audit the app and store surface against EN 301 549 | Report with severity and remediation status |
| L7-T4 | Document review | Compare the trust page with L2, L5, and L6 sources | No overclaim; review date and scope visible |
| L7-T5 | Document review | Check the repository license and third-party notices | License matches the decision; notices complete |

## 4. Evidence register

| Evidence ID | Artifact | Location | Owner | Date | Review cadence |
|---|---|---|---|---|---|
| EV-L7-01 | Trademark search report | External secure folder | Founder | Pending | Before filing |
| EV-L7-02 | Trademark filing receipt | External secure folder | Founder | Pending | Annual |
| EV-L7-03 | Licensing decision and license text | Repository plus external folder | Founder | Pending | On change |
| EV-L7-04 | Contributor policy and notices | Repository | Founder | Pending | Annual |
| EV-L7-05 | Insurance policies | External secure folder | Founder | Pending | Annual |
| EV-L7-06 | Disclosure policy, security.txt, and test record | Public URL plus external folder | Founder | Pending | Annual |
| EV-L7-07 | Accessibility audit and remediation | External secure folder | Founder | Pending | Annual |
| EV-L7-08 | Trust page URL and consistency check | Public URL | Founder | Pending | Quarterly |
| EV-L7-09 | Stewardship publication | Public URL | Founder | Pending | Annual |
| EV-L7-10 | Brand-use review | External secure folder | Founder | Pending | Before launch |

## 5. LIS sign-off

| Check | Result | Notes |
|---|---|---|
| LIS-1 Scope integrity | Pending | Owned components C-L7-01 to C-L7-10 appear only here |
| LIS-2 Boundary integrity | Pending | Store listing, privacy policy, security fixes, and support are excluded |
| LIS-3 Evidence traceability | Pending | Every criterion has an evidence ID and source |
| LIS-4 Behavioral proof | Pending | T2 is a live drill; T3 is external review |
| LIS-5 Operability | Pending | Founder owns; broker, reviewer, and trustee are external |
| LIS-6 Public-claim consistency | Pending | Trust page must match L2/L5/L6 and not overclaim |
| LIS-7 Independence | Pending | SC1, SC9, and SC12 are consumed |
| LIS-8 Regression | Pending | Any security or stewardship change triggers a trust-page review |

## 6. Viability gate decision

- Decision: Pending
- Gate date: —
- Approved by: —
- Conditions: —
- Next review: after P1 search
