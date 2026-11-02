# L7 — Phase Plan

Status: Planned
Owner: Founder (single-owner project)
Last updated: 2026-10-31
Viability gate: trademark filed, insurance bound, disclosure live, accessibility closed
Scope reference: [README.md](README.md)
Validation suite: [validation-suite.md](validation-suite.md)

## Phase 1 — IP and licensing

Indicative duration: 2-4 weeks, plus examination time after filing.

Objective: secure the name and settle how the code is licensed.

Work items:

1. Run OEPM and EUIPO searches for "Shary" in classes 9, 35, 38, and 42.
2. Decide whether to file at OEPM first, EUIPO, or both.
3. File in the S.L.'s name; if filed earlier, assign to the company.
4. Decide open-source versus closed licensing and add the license text.
5. Define the contributor policy and third-party notices.
6. Review the app name and icon for obvious conflicts.

Exit gate: L7-V1, L7-V2, L7-V3, L7-V4, L7-V10.

Evidence: EV-L7-01 to EV-L7-04, EV-L7-10.

Dependencies: G1 entity identity for the filing owner.

Risks: a conflicting mark blocks launch branding. Mitigation: search before
investing in store assets; keep a fallback name.

## Phase 2 — Insurance and disclosure

Indicative duration: 2-3 weeks.

Objective: transfer residual risk and open a credible security channel.

Work items:

1. Obtain quotes for cyber-liability and professional indemnity.
2. Bind coverage before the first paid user.
3. Publish the responsible-disclosure policy and security.txt.
4. Set a monitored inbox and a response target.
5. Run a test disclosure report.

Exit gate: L7-V5, L7-V6.

Evidence: EV-L7-05, EV-L7-06.

Dependencies: G9 — L2 P2 privacy artifacts and L6 P3 security evidence for the
insurance questionnaire.

Risks: coverage excludes the actual activity. Mitigation: disclose the app,
cloud relay, and MCP scope to the broker in writing.

## Phase 3 — Accessibility and trust surface

Indicative duration: 3-4 weeks.

Objective: publish the trust surface and close or document accessibility gaps.

Work items:

1. Confirm the EAA applicability and the microenterprise exemption test.
2. Audit the app and store/e-commerce surface against EN 301 549.
3. Remediate critical gaps or record accepted exceptions.
4. Publish the trust page: entity, subprocessors, status, security posture,
   disclosure policy, and stewardship commitment.
5. Verify the page against L2, L5, and L6 sources.

Exit gate: L7-V7, L7-V8, L7-V9.

Evidence: EV-L7-07, EV-L7-08, EV-L7-09.

Dependencies: G9; G10 published Privacy Policy URL; L5 stewardship clause; L6
security report; L2 subprocessor list; L3 store surface.

Risks: accessibility remediation expands indefinitely. Mitigation: define the
launch conformance target and a dated remediation plan for the rest.

## Gate review

- All L7-V criteria pass.
- LIS-1 to LIS-8 pass.
- The trademark filing receipt and insurance policy are stored.
- SC11 is published to L3.
- Gate decision recorded in [validation-suite.md](validation-suite.md).
