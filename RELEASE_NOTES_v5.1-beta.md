# Gate Key v5.1 Beta

Gate Key v5.1 Beta introduces the supplied type-and-tool hub interface update while preserving the existing vault, encryption, recovery, camera, OpenCV, Location Kit, storage, and database implementations.

## Release identity

- Application ID: `com.adsky.gatekey`
- Version name: `5.1`
- Version code: `6`
- Release channel: Beta
- Git tag: `v5.1-beta`
- Minimum Android version: API 24 / Android 7.0
- Target SDK: API 36
- Native ABIs: `arm64-v8a`, `armeabi-v7a`

## What's new

### Type-and-tool hub interface

The Home flow now opens three functional types: Security & Encryption, Camera Tools, and Location Kit. Each type displays its available tools in a responsive two-column grid that collapses for large font sizes.

Every tool has a real route into its existing implementation. The hub provides:

- Tool registry as the single source of truth for titles, descriptions, icons, category ownership, and requirements.
- Responsive tool tiles with accessibility-aware sizing.
- Long-press actions with Open and What is this? choices.
- Category help sheets and tool help sheets.
- Contextual information button in the top bar.
- Compact top bar while an individual camera, map, sensor, or vault tool is open.
- Back behavior from open tool to category grid, then from category grid to Home.
- Deep links from existing vault actions into the corresponding registered tool.

### Existing functionality preserved

The update does not replace the cryptographic engine, Android Keystore binding, recovery and migration workflows, CameraX/OpenCV pipeline, QR/barcode recognition, document scanning, magnifier, image information, MapLibre map, GPS, compass, distance calculations, Room database, or Storage Access Framework integration.

## Implementation details

New UI source is under `app/src/main/java/com/example/ui/registry/`, `ui/components/HubComponents.kt`, and `ui/screens/ToolHubScreen.kt`. `VaultViewModel` now owns the active tool state and provides `openTool`, `closeTool`, and deep-link routing. The prior category screen files were removed because their functionality is now hosted by the registry-driven hub and real tool content branches.

No new runtime service or network endpoint was added by this UI update. Existing permissions and dependencies remain unchanged.

## Validation

The beta was built locally with:

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

Results: 28 tests passed, 0 failures, 0 errors.

The attached APK is debug-signed for beta testing because release-signing credentials were not available in this build environment. It is not represented as release-signing compatible with an existing installation.
