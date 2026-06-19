# Firebase Functions Python: Deploy Checklist

1. Confirm runtime layout matches `firebase.json`.
- If `firebase.json` has `"source": "functions"`, your Python entrypoint must be inside `functions/` (typically `functions/main.py`).
- Move/copy `main_sample.py` to `functions/main.py` (or change `source` accordingly).

2. Add dependencies in the function source directory.
- Place `requirements.txt` inside the function source folder used by Firebase CLI.
- Ensure it includes: `firebase-functions`, `firebase-admin`, `cryptography`, `google-cloud-firestore`, `Flask`.

3. Verify env/config for local dev credentials.
- Local: ensure `PATH_CRED_FIREBASE_PROJECT` points to a valid service-account JSON.
- Cloud: no local credential file path should be required (`initialize_app()` default).

4. Validate function exports and routes.
- Ensure deployed functions include: `v2`, `ping`, `login_refresh_token`, `get_pubkey`, `upload_payload`, `upload_request`, `fetch_payload`, `fetch_request`, `clean_expired_docs`.
- Confirm `/v2/identity/*` routes are reachable through the `v2` function.

5. Deploy Firestore rules/indexes first.
- `firebase deploy --only firestore:rules`
- `firebase deploy --only firestore:indexes`

6. Deploy functions.
- `firebase deploy --only functions`

7. Post-deploy smoke tests.
- `GET /ping` returns 200.
- `POST /v2/identity/register` with verified Firebase user works.
- `GET /v2/identity/pubkey?user_hash=...` returns `pub_kex_b64`.
- `POST /upload_payload` and `GET /fetch_payload` work with matching sender/recipient hash ownership.

8. Security checks.
- Confirm unverified email tokens are rejected on identity operations.
- Confirm token-email mismatch to requested identity/hash is rejected.
- Confirm old legacy identity endpoints are intentionally deprecated or internally wrapped.
