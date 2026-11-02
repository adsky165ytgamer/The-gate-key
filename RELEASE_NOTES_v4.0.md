# Gate Key v4.0

Gate Key v4.0 is the stable release of the AD Sky Vault application with the camera toolkit integrated into the encrypted local vault experience.

## Package and release identity

- Android application ID: `com.adsky.gatekey`
- Version name: `4.0`
- Version code: `4`
- Release channel: stable
- Minimum Android version: Android 7.0 / API 24
- Target SDK: 36
- Supported native ABIs: `arm64-v8a`, `armeabi-v7a`

## New in v4.0

### Camera toolkit

- Live CameraX preview with lifecycle-safe attach/detach handling.
- QR and barcode recognition using ZXing with live analysis and gallery-image decoding.
- EXIF-aware image orientation and bounded rotation fallbacks for imported images.
- Document scanner with OpenCV page-edge detection, corner overlays, manual crop adjustment, perspective correction, rotation, filters, and multi-page review.
- Deterministic page ordering when importing multiple images.
- PDF and JPEG export with explicit render failure handling so pages are never silently omitted.
- Live magnifier with zoom, focus, flashlight, exposure, and high-contrast, grayscale, and inverted preview filters.
- Image information tool with dimensions, color analysis, and metadata inspection.

### Reliability and privacy

- Camera resources and analysis threads are released with the UI lifecycle.
- Image analysis closes every `ImageProxy` on success and failure paths.
- Scanned files remain local to the app cache until the user explicitly exports them.
- Scan history is stored locally with clear and per-item deletion actions.
- No cloud image upload or remote media-processing service is used by the camera tools.
- PDF export fails visibly when a page cannot be rendered instead of producing an incomplete document.

### OpenCV integration

- OpenCV is included as a local Android library module.
- Native libraries are packaged for `arm64-v8a` and `armeabi-v7a`.
- Document detection uses edge and threshold pipelines with contour validation, convexity checks, area bounds, and stable corner ordering.

### Existing vault functionality

The release retains the encrypted local vault, recovery-key flow, supported cipher selection, batch operations, migration tools, device-security information, and Material 3 interface from v3.0.

## Build verification

The v4.0 release was built with `assembleRelease`, signed with the configured release signing path, and checked with Android package metadata tools. The published APK is the artifact attached to the GitHub release.
