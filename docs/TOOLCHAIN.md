# Rona Toolchain

## Requirements

- JDK 17 is required by the Android Gradle Plugin used by this project.
- Android SDK with:
  - Android platform 34
  - Build-tools 34.0.0
  - Platform-tools
  - An emulator or physical device for instrumented tests
- Gradle is provided by the checked-in Gradle wrapper (`./gradlew`).

## Local setup

Set `JAVA_HOME` to an installed JDK 17 before building. The path is machine-specific and must not be committed:

```bash
export JAVA_HOME="$HOME/path/to/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

This repository's `local.properties` may contain a local `sdk.dir`; do not commit personal SDK or JDK paths.

## Verify Java and Gradle

```bash
java -version
./gradlew --version
```

Both commands must report Java 17. Gradle's `Launcher JVM` and `Daemon JVM` should also be JDK 17.

## Build and test

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleRelease
```

Instrumented tests require a running emulator or connected device:

```bash
./gradlew connectedDebugAndroidTest
```

## CI

GitHub Actions is the portable source of truth for CI's JDK 17 setup. Local machine paths are not part of the build contract.

Do not commit:

- Machine-specific JDK paths
- `org.gradle.java.home` pointing to a home-directory installation such as `~/tools/jdk17`
- Personal SDK paths outside the intended local `local.properties`

The Gradle wrapper and the Android SDK configuration are the portable project inputs; JDK 17 is selected by the local environment or GitHub Actions.
