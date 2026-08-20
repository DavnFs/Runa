# Local Data & Privacy Policy (Rona)

## 1. Zero-Cloud Default
Rona does not transmit, harvest, or monetize user health data. All period records, journal entries, symptom tracking, and user preferences remain on the device by default.

## 2. On-Device Encryption Architecture
- **Database Engine**: SQLCipher SQLite with 256-bit AES encryption.
- **Key Derivation**: 256-bit database passphrase randomly generated and stored within Android Keystore.
- **Hardware Backing**: Uses StrongBox Keymaster where supported by the physical hardware.

## 3. Safe Synthetic Fixture Policy
All test suites, previews, and CI executions must use **strictly synthetic data fixtures**:
- Example start dates: `2026-01-10`, `2026-02-08`.
- Example symptoms: `sample_cramp`, `sample_headache`.
- Example notes: `"test note only"`, `"synthetic log"`.
- **PROHIBITION**: Never use real personal records, real partner data, production backup dumps, or real encrypted databases in test fixtures or code samples.

## 4. Git Repository & CI Hygiene
- **Untracked Local Data**: All `.db`, `.sqlite`, `.sqlcipher`, `.rona`, `.ronabackup`, `.runa-backup`, `dumps/`, and `scratch/` directories are permanently ignored in `.gitignore`.
- **Secret Remediation**: If an API key or signing key is ever exposed, it must be **revoked and rotated immediately** at the provider level, in addition to purging from Git history.
- **CI Artifact Policy**: CI test/lint artifacts are retained for a maximum of 7 days only on build failure, and automated checks verify that no database or secret files are present before artifact creation.
