# Daftar Langkah Toolchain Rona

Build rona di mesin ini tanpa install sistem (semua di `~/.local/`).

## Persyaratan
- Java 17
- Android SDK: platform 34, build-tools 34.0.0, platform-tools
- Gradle 8.11.1 (wrapper mengunduh otomatis)

## Instalasi (sekali saja)
```bash
# JDK 17
mkdir -p ~/.local/toolchain ~/.local/android-sdk ~/.local/dl
cd ~/.local/dl
curl -sL -o jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar -xzf jdk17.tar.gz -C ~/.local/toolchain

# Gradle (untuk bootstrap wrapper; setelah wrapper ada, pakai ./gradlew)
curl -sL -o gradle-8.11.1-bin.zip https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
unzip -q -o gradle-8.11.1-bin.zip -d ~/.local/toolchain

# Android cmdline-tools
curl -sL -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
mkdir -p ~/.local/android-sdk/cmdline-tools/latest
unzip -q -o cmdtools.zip -d ~/.local/android-sdk/cmdline-tools/latest
if [ -d ~/.local/android-sdk/cmdline-tools/latest/cmdline-tools ]; then
  mv ~/.local/android-sdk/cmdline-tools/latest/cmdline-tools/* ~/.local/android-sdk/cmdline-tools/latest/
  rmdir ~/.local/android-sdk/cmdline-tools/latest/cmdline-tools
fi

# SDK packages + lisensi
export JAVA_HOME="$HOME/.local/toolchain/jdk-17.0.20+8"
export PATH="$JAVA_HOME/bin:$PATH"
yes | ~/.local/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  --sdk_root="$HOME/.local/android-sdk" --licenses
~/.local/android-sdk/cmdline-tools/latest/bin/sdkmanager \
  --sdk_root="$HOME/.local/android-sdk" \
  "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Environment per sesi build
```bash
export JAVA_HOME="$HOME/.local/toolchain/jdk-17.0.20+8"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/.local/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

> Catatan: versi JDK bisa berbeda (cek `ls ~/.local/toolchain/`). Tidak perlu `local.properties`
> karena `ANDROID_HOME` diekspor.

## Perintah umum
```bash
./gradlew assembleDebug        # build APK debug
./gradlew lintDebug            # lint
./gradlew testDebugUnitTest    # unit test
./gradlew assembleRelease      # rilis (butuh signing)
./gradlew connectedDebugAndroidTest  # instrumented test (butuh device/emulator)
```
