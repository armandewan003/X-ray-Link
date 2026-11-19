# Android Client & Admin APK Build Guide

## Prerequisites
- JDK 17 (`JAVA_HOME` configured)
- Android Studio Ladybug+ OR Android SDK Platform 35 & Build Tools 35.0.0
- Gradle 8.10+ (generate wrapper binaries on first build via `gradle wrapper` or Android Studio Gradle Sync)

## Environment & Endpoint Configuration
1. By default:
   - `debug` builds target `http://10.0.2.2:3000/api/v1/` (and `ws://10.0.2.2:3000/ws`) with cleartext enabled only for `10.0.2.2`, `localhost`, and `127.0.0.1` via `src/debug/res/xml/network_security_config.xml`.
   - `release` builds enforce HTTPS/WSS (`https://api.xraylink.example.com/api/v1/`).
   - Override either via Gradle properties or environment variables: `-PXRAY_API_BASE_URL=https://your-domain/api/v1/ -PXRAY_WS_BASE_URL=wss://your-domain/ws`.
2. Configure release signing environment variables before assembling signed APKs:
   ```bash
   export KEYSTORE_FILE=/path/to/xraylink-release.jks
   export KEYSTORE_PASSWORD=...
   export KEY_ALIAS=xraylink
   export KEY_PASSWORD=...
   ```

## Build Debug / Release APKs
```bash
# Client VPN App
cd x-ray-link/android-client
gradle wrapper   # run once if ./gradlew is not yet generated
./gradlew assembleDebug
./gradlew assembleRelease

# Admin Control App
cd ../android-admin
gradle wrapper   # run once if ./gradlew is not yet generated
./gradlew assembleDebug
./gradlew assembleRelease
```

## Outputs
- Client Debug APK: `x-ray-link/android-client/app/build/outputs/apk/debug/app-debug.apk`
- Client Release APK: `x-ray-link/android-client/app/build/outputs/apk/release/app-release.apk`
- Admin Debug APK: `x-ray-link/android-admin/app/build/outputs/apk/debug/app-debug.apk`
- Admin Release APK: `x-ray-link/android-admin/app/build/outputs/apk/release/app-release.apk`


