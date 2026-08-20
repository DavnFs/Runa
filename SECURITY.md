# Security Policy

## Reporting a Vulnerability

**Do NOT open public GitHub issues for security vulnerabilities, private key disclosures, or sensitive data leaks.**

If you discover a security vulnerability or potential data leakage risk in Rona:
1. Please report it privately to the maintainers via GitHub Private Vulnerability Reporting or by contacting `security@rona.app` (or the repository owner directly).
2. Include a detailed description, reproduction steps, affected component/version, and any proof of concept.
3. We will acknowledge receipt of your report within 48 hours and provide an initial assessment and timeline for a patch.

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0.0 | :x:                |

## Security Scope & Architecture Principles

Rona is engineered as a **Privacy-First, Local-First** application:
- **Local Storage Encryption**: All user data (cycles, logs, notes, symptoms) is stored locally in an encrypted SQLCipher database, protected with keys backed by Android Keystore and StrongBox hardware security module when available.
- **Offline Integrity**: The core application operates entirely offline without server dependencies.
- **Export & Backup Security**: Encrypted backups use AES-256-GCM authenticated encryption.
- **CI/CD & Repository Hygiene**:
  - No real user health data, credentials, or private keys are ever committed.
  - Automated CI pipelines verify that no unauthorized network permissions are added to local builds.
  - Third-party GitHub Actions are pinned to full immutable 40-character commit SHAs.
  - Artifacts in CI are limited to failure diagnostics and are strictly audited to ensure no database files or private data are packaged.
