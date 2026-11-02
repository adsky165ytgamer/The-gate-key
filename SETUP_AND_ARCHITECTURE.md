# Gate Key v4.0 — Setup, Architecture & Deployment Manual

## 1. Release identity

Gate Key v4.0 is the stable Android release of the AD Sky Vault application. It combines the offline encrypted vault, authenticated `.adsky` containers, recovery and migration workflows, and the local camera toolkit.

| Property | Value |
|---|---|
| Application ID / APK package | `com.adsky.gatekey` |
| Android namespace | `com.example` (retained internally so the existing source packages and imports remain stable) |
| Version name | `4.0` |
| Version code | `4` |
| Release channel | Stable |
| Minimum SDK | API 24 / Android 7.0 |
| Target SDK | API 36 |
| Native ABIs | `arm64-v8a`, `armeabi-v7a` |
| Stable tag | `v4.0` |
| GitHub release | <https://github.com/adsky165ytgamer/The-gate-key/releases/tag/v4.0> |

The application label remains **AD Sky Vault** inside Android launcher metadata. The installable package identity is now `com.adsky.gatekey`.

## 2. Runtime security and privacy model

Gate Key is offline-first. The manifest requests camera access only when the camera tools are used and does not request `android.permission.INTERNET`. The camera tools do not upload images to a server, use a cloud image-processing API, or store captured media outside the app-controlled cache until the user explicitly exports it through Android’s Storage Access Framework.

The encrypted vault uses fresh per-container data-encryption keys, authenticated payload records, device-bound Android Keystore envelopes, user/recovery-key derivation, and optional digital signatures. The camera toolkit is separate from the vault cryptographic engine: it can produce local images and PDFs, while the vault remains responsible for encryption, verification, recovery, and migration.

The manifest declares `android.permission.CAMERA` and marks camera hardware, autofocus, and flash as optional device features. This allows the application to install on devices without a camera while keeping gallery-based image tools available.

## 3. Repository structure

```text
app/
  src/main/java/com/example/
    crypto/                         Vault cryptography and .adsky container engine
    data/                           Room database and local audit records
    camera/
      core/                         CameraX session, lifecycle, permissions, preview UI
      recognition/                  ZXing QR/barcode analysis and local scan history
      scanner/                      OpenCV document detection and scan processing
      magnifier/                    Live preview reading aids and camera controls
      imageinfo/                    Image metadata and color analysis
    ui/                             Material 3 application screens and view models
  src/main/AndroidManifest.xml      Camera permission and launcher configuration
opencv/                             Local OpenCV Android library module
  src/main/jniLibs/                 arm64-v8a and armeabi-v7a native libraries
  src/main/java/org/opencv/         Generated OpenCV Java bindings
  libcxx_helper/                    Native C++ runtime helper
RELEASE_NOTES_v4.0.md               Published v4.0 release notes
SETUP_AND_ARCHITECTURE.md           This document
```

The Android Gradle namespace remains `com.example` because changing every source package would create an unnecessary compatibility migration. The package users install and Android uses for updates is controlled by `applicationId = "com.adsky.gatekey"`.

## 4. Cryptographic architecture

| Component | Specification | Implementation |
|---|---|---|
| Payload algorithm 1 | AES-256-GCM, 12-byte nonce, 128-bit tag | Google Tink AEAD |
| Payload algorithm 2 | XChaCha20-Poly1305, 24-byte nonce, 128-bit MAC | Google Tink AEAD |
| Key derivation | Argon2id, RFC 9106 profile, 3 iterations, 64 MiB memory, parallelism 2, random 16-byte salt | Bouncy Castle |
| Device binding | Hardware-backed AES-256-GCM key | Android Keystore, StrongBox/TEE when available |
| Digital signing | ECDSA P-256 with SHA-256 | Android Keystore |
| File access | Android Storage Access Framework | `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT` |

### `.adsky` container layout

```text
+-------------------------------------------------------------------+
| MAGIC: "ADSKY-VAULT" (11 bytes ASCII)                             |
| Format version: uint16                                             |
| Algorithm ID: AES-GCM or XChaCha20-Poly1305                       |
| Container flags and monotonic generation number                    |
| Envelope section length and authorization envelopes               |
| Encrypted private metadata                                         |
+-------------------------------------------------------------------+
| Streaming payload records                                          |
|   Record type 0x10                                                 |
|   Monotonic chunk index                                            |
|   Algorithm nonce                                                   |
|   Ciphertext length, ciphertext, and authentication tag             |
|   AAD binds magic, version, algorithm, chunk index, and record type |
+-------------------------------------------------------------------+
| Authenticated final record                                          |
|   Total chunks, plaintext byte count, encrypted completion token    |
+-------------------------------------------------------------------+
| Optional digital signature record                                   |
|   X.509 public key and ECDSA signature over the container digest    |
+-------------------------------------------------------------------+
```

The vault engine streams large payloads in nominal 1 MiB chunks rather than loading an entire file into memory. Recovery and migration re-wrap the file key for a new device or authorization path without requiring the original device’s hardware key to be exported.

## 5. Camera toolkit architecture

### CameraX core

`CameraSession` owns the `LifecycleCameraController`, the single-threaded analysis executor, zoom/torch/exposure observers, and attach/detach lifecycle. `CameraPreview` binds a session to a `PreviewView` and releases it through `DisposableEffect`. Each screen uses `rememberCameraSession()` so the QR scanner, document scanner, and magnifier share the same lifecycle-safe camera abstraction without directly managing CameraX use cases.

`CameraPermissionGate` handles runtime camera permission. Camera controls are exposed through `CameraCapabilities`, including zoom range, flash availability, torch state, exposure support, and bound state. Analysis uses `STRATEGY_KEEP_ONLY_LATEST` to prevent stale frames from accumulating.

### QR and barcode recognition

`BarcodeAnalyzer` consumes `ImageProxy` frames and always closes them on success and failure paths. Live recognition uses ZXing with the supported barcode formats and `TRY_HARDER`. Gallery recognition goes through `BitmapLoader`, which applies EXIF orientation, then tries bounded 90°, 180°, and 270° fallback rotations when the upright image does not decode. Results are parsed into content kinds such as URL, email, phone, Wi-Fi, geographic coordinates, and plain text.

Scan history is local to the app, capped, and user-deletable. The recognition UI provides copy, share, and context-specific open actions without uploading the decoded value.

### Document scanner

The scanner pipeline is:

1. CameraX analysis produces an `ImageProxy`.
2. `DocumentAnalyzer` converts the frame into a bounded bitmap and closes the proxy.
3. `DocumentDetector` uses OpenCV grayscale, blur, Canny-edge, dilation, threshold, and morphology pipelines.
4. Candidate contours are filtered by area, minimum edge length, convexity, and angle plausibility.
5. Corners are sorted around their centroid, winding is normalized, and the result is normalized to upright image coordinates.
6. `PerspectiveCorrection` maps the quadrilateral into a flat page.
7. `ScanRenderer` applies rotation and the selected filter: Original, Color, Grayscale, or Black & white.
8. `ScannerViewModel` manages crop confirmation, page deletion, reordering, review, and export.

Multiple gallery images preserve the user’s selection order. The first selected image remains in the crop/confirm flow but is inserted at its original position after confirmation, so later selected pages cannot move ahead of it.

PDF export renders every page before writing the PDF. If any page cannot be rendered, export fails with an explicit error rather than silently producing an incomplete document. JPEG export similarly reports encoder failure and recycles its bitmap in all paths. Temporary scan files are removed when pages are deleted, cancelled, discarded, or the view model is cleared.

### Live magnifier

The magnifier is intentionally live-only. It does not capture or display a frozen frame. It provides CameraX zoom, center focus, flashlight, exposure, and live preview filters for Normal, High contrast, Grayscale, and Inverted viewing. This keeps the tool aligned with the accessibility requirement for continuously changing camera content and avoids stale-frame memory retention.

### Image information

The image-information tool reads selected images locally and reports dimensions, file characteristics, EXIF properties, and color analysis. `BitmapLoader` bounds decode size and applies EXIF orientation before downstream processing.

## 6. OpenCV integration

OpenCV is committed as a local Gradle module at `opencv/`; the build does not depend on a remote OpenCV Maven artifact at runtime. The module contains the generated Java bindings, native JNI libraries, license, and C++ helper configuration. The application loads `opencv_java5` through `OpenCvRuntime` and safely falls back when the native library is unavailable.

Only `arm64-v8a` and `armeabi-v7a` are packaged. Generated Gradle outputs, `.cxx` directories, APKs, keystores, and local SDK configuration are ignored and must not be committed.

## 7. Build prerequisites

Use a JDK compatible with the Android Gradle Plugin, preferably JDK 21 in the validated environment. The project currently targets Android SDK 36 and uses the project’s configured Gradle/AGP versions from `gradle/libs.versions.toml` and the root build files.

Set the Android SDK location through `ANDROID_HOME`/`ANDROID_SDK_ROOT` or create a local, ignored `local.properties` file:

```properties
sdk.dir=/absolute/path/to/android-sdk
```

Release builds require a signing keystore. The build script reads these environment variables:

```bash
export KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
export STORE_PASSWORD='...'
export KEY_PASSWORD='...'
```

The release signing alias is `upload`. Never commit a keystore or its passwords.

## 8. Build and test commands

From the repository root:

```bash
# Unit tests
./gradlew :app:testDebugUnitTest

# Debug APK
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

When a Gradle wrapper is not present, use the configured local Gradle executable with the same tasks. Release output is generated at:

```text
app/build/outputs/apk/release/app-release.apk
```

Verify package and version metadata with Android build tools:

```bash
aapt2 dump badging app/build/outputs/apk/release/app-release.apk | head -10
sha256sum app/build/outputs/apk/release/app-release.apk
```

The expected v4.0 metadata is:

```text
package: name='com.adsky.gatekey' versionCode='4' versionName='4.0'
```

Install a locally built debug or release APK with:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Because v4.0 changes the application ID from `com.example` to `com.adsky.gatekey`, Android treats it as a new package. It cannot be installed as an in-place update over an older package with the old ID; install it as the new Gate Key package and migrate vault data through the application’s supported recovery/migration flow.

## 9. Stable v4.0 release

The stable release is tagged `v4.0` and published at:

<https://github.com/adsky165ytgamer/The-gate-key/releases/tag/v4.0>

The release includes the signed APK `Gate_Key_v4.0_stable.apk` and its SHA-256 checksum. The v4.0 source commit includes the new application ID, camera toolkit, OpenCV module, scanner reliability fixes, live-only magnifier, package metadata, and these updated architecture notes.

## 10. Operational boundaries

The repository should contain source, generated OpenCV bindings/native libraries, Gradle configuration, documentation, and license files. Do not commit `app/build`, `opencv/build`, `.cxx`, `.gradle`, `local.properties`, debug/release keystores, or generated APK outputs. Keep release APKs in GitHub Releases rather than in the source tree unless a future release policy explicitly requires otherwise.
