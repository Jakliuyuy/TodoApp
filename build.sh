#!/usr/bin/env bash
# 本地构建脚本（需要 JDK 17 + Android SDK）
# 用法: ./build.sh [debug|release|clean]
set -euo pipefail

TASK="${1:-debug}"
GRADLE_VERSION="8.7"

if [ ! -x ./gradlew ]; then
    echo ">> 未找到 gradlew，正在生成 Gradle Wrapper..."
    gradle wrapper --gradle-version "$GRADLE_VERSION" --distribution-type bin
    chmod +x ./gradlew
fi

case "$TASK" in
    debug)
        echo ">> 构建 Debug APK"
        ./gradlew assembleDebug --no-daemon
        echo ">> 产物: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo ">> 构建 Release APK（需先准备签名密钥并导出 KEYSTORE_FILE 等环境变量）"
        ./gradlew assembleRelease --no-daemon
        echo ">> 产物: app/build/outputs/apk/release/app-release.apk"
        ;;
    clean)
        ./gradlew clean --no-daemon
        ;;
    *)
        echo "用法: $0 [debug|release|clean]"
        exit 1
        ;;
esac
