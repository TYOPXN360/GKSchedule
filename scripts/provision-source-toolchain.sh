#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="$ROOT_DIR/source"
DOWNLOAD_DIR="$SOURCE_DIR/.downloads"
TMP_DIR="$SOURCE_DIR/.tmp-provision"

JDK_VERSION="17.0.10+7"
JDK_URL="https://api.adoptium.net/v3/binary/version/jdk-17.0.10%2B7/linux/x64/jdk/hotspot/normal/eclipse"
GRADLE_VERSION="8.13"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
CMDLINE_TOOLS_VERSION="21.0"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip"

ANDROID_SDK="$SOURCE_DIR/android-sdk"
JDK_DIR="$SOURCE_DIR/jdk-17"

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing required tool: $1" >&2
        exit 1
    fi
}

download() {
    local url="$1"
    local output="$2"
    if [[ -f "$output" ]]; then
        echo "Using cached $(basename "$output")"
        return
    fi
    echo "Downloading $(basename "$output")"
    curl -fL --retry 3 --retry-delay 2 "$url" -o "$output"
}

install_jdk() {
    if [[ -x "$JDK_DIR/bin/java" ]] && grep -q "JAVA_VERSION=\"17.0.10\"" "$JDK_DIR/release" 2>/dev/null; then
        echo "JDK $JDK_VERSION already installed"
        return
    fi

    local archive="$DOWNLOAD_DIR/temurin-jdk-${JDK_VERSION}.tar.gz"
    download "$JDK_URL" "$archive"

    rm -rf "$TMP_DIR/jdk" "$JDK_DIR"
    mkdir -p "$TMP_DIR/jdk"
    tar -xzf "$archive" -C "$TMP_DIR/jdk" --strip-components=1
    mv "$TMP_DIR/jdk" "$JDK_DIR"
}

install_gradle_zip() {
    download "$GRADLE_URL" "$SOURCE_DIR/gradle-${GRADLE_VERSION}-bin.zip"
}

install_cmdline_tools() {
    local source_props="$ANDROID_SDK/cmdline-tools/latest/source.properties"
    if [[ -f "$source_props" ]] && grep -q "Pkg.Revision=${CMDLINE_TOOLS_VERSION}" "$source_props"; then
        echo "Android cmdline-tools $CMDLINE_TOOLS_VERSION already installed"
        return
    fi

    local archive="$DOWNLOAD_DIR/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}.zip"
    download "$CMDLINE_TOOLS_URL" "$archive"

    rm -rf "$TMP_DIR/cmdline-tools" "$ANDROID_SDK/cmdline-tools/latest"
    mkdir -p "$TMP_DIR"
    unzip -q "$archive" -d "$TMP_DIR"
    mkdir -p "$ANDROID_SDK/cmdline-tools"
    mv "$TMP_DIR/cmdline-tools" "$ANDROID_SDK/cmdline-tools/latest"
}

install_android_packages() {
    export JAVA_HOME="$JDK_DIR"
    export ANDROID_HOME="$ANDROID_SDK"
    export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

    mkdir -p "$ANDROID_SDK"
    set +o pipefail
    yes | sdkmanager --sdk_root="$ANDROID_SDK" --licenses >/dev/null
    local license_status=$?
    set -o pipefail
    if [[ $license_status -ne 0 ]]; then
        echo "Failed to accept Android SDK licenses" >&2
        exit "$license_status"
    fi
    sdkmanager --sdk_root="$ANDROID_SDK" \
        "platform-tools" \
        "platforms;android-36" \
        "build-tools;36.0.0" \
        "build-tools;36.1.0"
}

sync_source_links() {
    local mirror_root="$SOURCE_DIR/src/main/java/com/classapp"
    rm -rf "$SOURCE_DIR/src"
    mkdir -p "$mirror_root"
    cp -al "$ROOT_DIR/app/src/main/java/com/classapp/schedule" "$mirror_root/"
}

main() {
    require_tool curl
    require_tool tar
    require_tool unzip

    mkdir -p "$DOWNLOAD_DIR" "$TMP_DIR" "$ANDROID_SDK"

    install_jdk
    install_gradle_zip
    install_cmdline_tools
    install_android_packages
    sync_source_links

    rm -rf "$TMP_DIR"

    echo "source/ toolchain is ready."
    echo "JDK: $JDK_DIR"
    echo "Android SDK: $ANDROID_SDK"
    echo "Gradle zip: $SOURCE_DIR/gradle-${GRADLE_VERSION}-bin.zip"
}

main "$@"
