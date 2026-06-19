# Test Plan (Manual / Operative)

Date: 2026-04-11

## 1) Hybrid A (local credential usability)
### Scenario A1: No credentials
Steps:
1. Ensure no credentials files exist in app storage (fresh install).
2. Launch app.
Expected:
- App starts on Logup.

### Scenario A2: Valid credentials
Steps:
1. Sign up and sign in successfully.
2. Close and reopen app.
Expected:
- App starts on Login.
- No lock banner shown in Logup.

### Scenario A3: Locked credentials (simulated)
Steps:
1. Corrupt the credentials blob or simulate keystore failure.
2. Launch app.
Expected:
- App routes to Logup.
- Logup shows locked warning and "Retry unlock" button.
- No credentials are deleted automatically.

### Scenario A4: Retry unlock
Steps:
1. From A3, tap "Retry unlock".
Expected:
- If keystore recovers, credentials unlock and user is routed to Login.
- If not, message indicates still locked.

## 2) Cloud gating (visible but disabled)
### Scenario B1: Cloud offline
Steps:
1. Disable network.
2. Navigate to Summary screens.
Expected:
- Cloud card visible but disabled.
- Hint text: "Cloud disabled: Cloud offline".
- Clicking card shows snackbar with same reason.
- Cloud download buttons disabled in Fields/Requests.

### Scenario B2: Cloud online but email not verified
Steps:
1. Ensure network is on.
2. Use an unverified email.
3. Navigate to Summary screens.
Expected:
- Cloud card visible but disabled.
- Hint text: "Cloud disabled: Email not verified for cloud".
- Clicking card shows snackbar with same reason.

### Scenario B3: Cloud online + verified
Steps:
1. Verify email.
2. Ensure network is on.
3. Navigate to Summary screens.
Expected:
- Cloud card enabled.
- Cloud send works.
- Cloud download buttons enabled in Fields/Requests.

## 3) Start-up ping
Steps:
1. Launch app with network on.
Expected:
- Cloud online state updates within a short moment (no UI block).

## 4) Regression sanity
- Login flow still works.
- Signup flow still works.
- Change password flow still works.
