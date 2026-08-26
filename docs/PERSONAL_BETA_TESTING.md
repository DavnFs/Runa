# Runa Personal Beta Testing

## Access and privacy

This is a private GitHub repository. A tester must be an explicitly invited repository collaborator before downloading the beta.

Download only from:

1. Open the private repository on GitHub.
2. Open **Releases**.
3. Select the release marked **Pre-release**.
4. Download the APK, checksum, `RELEASE_NOTES.txt`, and `BUILD_INFO.txt` assets.

Do not share APK files, release links, screenshots, backups, crash logs, or private health data publicly. Do not change the repository visibility.

## Verify the download

Place the APK and checksum in the same directory, then run:

```bash
sha256sum -c runa-<version>.apk.sha256
```

The result must report `OK`. Stop if verification fails; download the asset again from the private release.

On macOS, use:

```bash
shasum -a 256 -c runa-<version>.apk.sha256
```

## Install the beta

Enable USB debugging on the Android device, connect it, and verify that ADB sees it:

```bash
adb devices
```

For a fresh install:

```bash
adb install runa-<version>.apk
```

For an update over an existing beta:

```bash
adb install -r runa-<new-version>.apk
```

Do **not** uninstall the existing app before attempting an update. Uninstalling removes local app data unless it has been exported separately.

If Android reports a signature mismatch:

1. Stop and do not uninstall immediately.
2. Export/test the app data using the app's encrypted backup flow if the app is still accessible.
3. Preserve the backup and verify the beta package/signing configuration with the owner.
4. Uninstall only after the data is safely exported and an uninstall is explicitly acceptable.

## Reporting a beta issue

Include:

- Beta version from `BUILD_INFO.txt`.
- Device model and Android version.
- Reproduction steps without private health details.
- Whether the issue happened after a fresh install or an update.

Redact private notes, cycle dates, symptoms, screenshots, backups, and other health information before sharing diagnostics with anyone.

To collect the Android crash buffer:

```bash
adb logcat -b crash -d -v threadtime
```

Save the output privately and review it for personal data before sending it to the owner. Do not upload crash logs to public issue trackers or public file hosts.

## Current beta defaults

- Package ID: `id.rona.app`
- Initial version name: `0.1.0-dev.1`
- Initial version code: `1`
- Distribution: private GitHub prerelease only
- Release channel: manually dispatched workflow, published after the `personal-beta` environment gate
- Network policy: the app has no INTERNET permission
