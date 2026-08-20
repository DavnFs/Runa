# Security Model (Rona)

Threat model and architectural guarantees for Rona's local-first, offline-only data handling.

## Threat Assumptions

- The device OS (Android) is trusted; untrusted layers are app code, storage media, backups, and any future transfer channel.
- An attacker who obtains the `.rona` backup file, the SQLCipher DB file, or the device's storage image **must not** be able to read health data without the user's passphrase / hardware-protected key.
- There is no server, no account, no telemetry, and no `INTERNET` permission — remote exfiltration is impossible in the current local-only build variants.

## Component Scope

### Android Keystore / StrongBox

- Database passphrase (256-bit) is generated on first run and stored in Android Keystore.
- StrongBox-backed keys are used when the hardware supports them; otherwise the TEE keymaster is the fallback.
- The passphrase never leaves the Keystore; it is never logged, exported, or included in backups.

### SQLCipher

- All tables (periods, daily logs, symptoms, notes, settings, predictions) live in one AES-256 encrypted database.
- WAL files are also encrypted (SQLCipher WAL encryption).
- No destructive Room migrations are permitted (see `docs/PLAN.md` §7); schema bumps require explicit migrations.

### Backup / Export (`.rona`)

- Backups are AES-256-GCM authenticated encryption with a user passphrase (PBKDF2 ≥ 120k iterations).
- A backup file without the passphrase is unreadable. Passphrase recovery is intentionally impossible.
- Exports never include the Keystore passphrase or raw database files.

### CI/CD

- CI asserts zero `android.permission.INTERNET` in **both** debug and release merged manifests.
- CI artifacts are failure diagnostics only: retention ≤ 7 days, and a pre-upload audit asserts no `*.db`, `*.sqlite`, `*.sqlcipher`, `*.rona*`, `*.env`, `*.jks`, `*.keystore`, `*.pem`, `*.key` files are present.
- CI artifacts are **not** encrypted by the workflow; the policy is exclusion of sensitive data, not encryption.
- All third-party actions are pinned to full commit SHAs; Dependabot and Dependency Review Action monitor the supply chain.
- Gitleaks scans PR diffs (no secrets used, no `pull_request_target`) and full history on schedule/push.
- Signing credentials exist only as GitHub environment secrets and are written to gitignored `app/keystore/` paths at build time; a CI step fails if those paths are tracked in git.

### Future Partner Sharing (P3, not implemented)

- When remote sharing is designed, it must be gated behind build variants: `localDebug`/`localRelease` keep the no-INTERNET policy, `remoteDebug`/`remoteRelease` get crypto-relay policy tests and a "no health content in push payloads" test.

## Data Minimization

- Period records store only epoch-day ranges. Daily logs are independent per-date observations.
- Period merge/delete never deletes daily logs, notes, or symptom logs — they are separate tables with no cascade from `period_records`.

## Supported Versions & Reporting

See [SECURITY.md](../SECURITY.md) for supported versions and private vulnerability reporting.
