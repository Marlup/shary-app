# L7 — Trust, Brand & Risk Transfer

Status: In progress (Phase 1)
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Parent index: [[docs/backlog/works/2026-10-31/lines/README.md]]
Cross-line register: [[docs/backlog/works/2026-10-31/lines/cross-line.md]]
Validation suite: [validation-suite.md](validation-suite.md)
Phase plan: [phase-plan.md](phase-plan.md)
Phase 1 artifact: [phase-1-trademark-insurance-disclosure.md](phase-1-trademark-insurance-disclosure.md)
Phase 1 companion: [phase-1-trademark-search-record.md](phase-1-trademark-search-record.md)

## 1. Purpose

Protect the Shary name, transfer residual risk to insurance, publish a credible
security and accessibility posture, and keep IP ownership inside the company.

## 2. Scope

### Owned components

- C-L7-01 Trademark search at OEPM and EUIPO.
- C-L7-02 Trademark filing for "Shary" in the relevant classes.
- C-L7-03 Open-source versus closed licensing decision.
- C-L7-04 Contributor and IP hygiene.
- C-L7-05 Cyber-liability and professional-indemnity insurance.
- C-L7-06 Responsible-disclosure policy and security.txt.
- C-L7-07 Accessibility audit against the EAA and EN 301 549.
- C-L7-08 Public trust page: entity, subprocessors, status, security posture.
- C-L7-09 Data-stewardship publication support.
- C-L7-10 Brand-use review for the app name and icon.

### Explicit exclusions

- Store listing and consumer disclosures belong to L3.
- Privacy Policy and ROPA belong to L2.
- Security fixes and pen-test remediation belong to L6.
- Support SLA and complaint intake belong to L8.
- Corporate incorporation belongs to L1.

### Interfaces

- Consumes SC1 entity identity from L1.
- Consumes SC12 subprocessor list from L2.
- Consumes SC9 stewardship clause from L5.
- Consumes L6 security report for the trust page.
- Provides SC11 accessibility report to L3.

## 3. Viability definition

L7 is viable when:

1. the trademark search is clear enough to proceed and the filing receipt
   exists;
2. the licensing decision is recorded and the repository reflects it;
3. cyber-liability and professional-indemnity insurance are bound before
   charging users;
4. responsible disclosure is live and tested;
5. the accessibility audit is complete and gaps are closed or formally accepted
   with a documented exemption.

## 4. Section 8 coverage

- 8.7 Insurance: primary owner.
- 8.8 IP and branding: primary owner.
- 8.11 Accessibility (EAA): primary owner.
- 8.13 Succession and data stewardship: secondary owner for publication on the
  trust page.

## 5. Phase summary

| Phase | Objective | Exit criteria |
|---|---|---|
| P1 | IP and licensing | L7-V1, L7-V2, L7-V3, L7-V4, L7-V10 |
| P2 | Insurance and disclosure | L7-V5, L7-V6 |
| P3 | Accessibility and trust surface | L7-V7, L7-V8, L7-V9 |

## 6. Official sources

- OEPM (Spanish Patent and Trademark Office) and EUIPO filing guidance.
- Directive (EU) 2019/882 (European Accessibility Act), applicable from 28 June
  2025, and its Spanish transposition. Verify the current text and the
  microenterprise exemption.
- UNE-EN 301 549 accessibility requirements for ICT products and services.
- ISO/IEC 29147 (vulnerability disclosure) and ISO/IEC 30111 (vulnerability
  handling).
- Ley 50/1980, insurance contract law.
- OSI and SPDX guidance for open-source licensing.

All sources must be re-checked at the phase gate and recorded with a retrieval
date in the validation suite.

## 7. Single-owner notes

- The project has one person, so the microenterprise service-provider exemption
  to the EAA likely applies. L7 must document the employee and turnover test
  and the exemption decision. An exemption is not a reason to ignore
  accessibility; it is a recorded scope decision.
- File the trademark in the S.L.'s name. If a filing predates incorporation,
  assign it to the company.
- Insurance should be bound before the first paid user, not after launch.
- The trust page must not overclaim security. Use the L6 report as the source
  and state the review date and scope.
- A responsible-disclosure policy needs a monitored inbox and a response
  target that one person can meet.

## 8. Open decisions

- Confirm trademark classes: likely 9 (software), 42 (SaaS/software services),
  and possibly 35 or 38.
- Choose OEPM first, EUIPO later, or both at once.
- Decide open-source versus closed licensing; if open, choose the license and
  contributor policy.
- Choose the insurance broker and coverage limits.
- Confirm the EAA exemption and the accessibility conformance target.
