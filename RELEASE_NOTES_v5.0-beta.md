# Gate Key v5.0 Beta

Gate Key v5.0 Beta adds the first integrated Location Kit while preserving the existing encrypted vault, recovery, migration, camera toolkit, and OpenCV architecture from v4.0.

## Release identity

- Application ID: `com.adsky.gatekey`
- Version name: `5.0`
- Version code: `5`
- Release channel: Beta
- Git tag: `v5.0`
- Minimum Android version: API 24 / Android 7.0
- Target SDK: API 36
- Native ABIs: `arm64-v8a`, `armeabi-v7a`

This is a prerelease for testing the Location Kit on physical Android devices. The beta is published as a GitHub prerelease and is not marked as the stable release channel.

## What's new

### Location Kit

Location Kit is available from the Home screen and contains four tools:

- **Map Explorer:** a MapLibre Native map with an OpenFreeMap Liberty style, current-location overlay, map camera movement, provider attribution, tap handling, route/measurement overlays, loading states, and visible error states.
- **Compass:** a sensor-backed compass with accelerometer/magnetic and rotation-vector handling, display-orientation remapping, heading smoothing, cardinal directions, and true-north correction when a location fix is available.
- **GPS Coordinates:** foreground location status, latitude/longitude display, accuracy and freshness information, permission guidance, location-services guidance, and copy/share-friendly coordinate presentation.
- **Distance:** coordinate entry and current-location endpoints with Haversine distance, initial bearing, metric/imperial formatting, invalid-coordinate validation, antimeridian handling, and identical-point handling.

### Foreground location behavior

The app requests `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` only for foreground Location Kit use. It does not request background location permission. The UI distinguishes missing permission, disabled system location services, searching, active fixes, and location-provider failures. Location updates stop when the shared repository has no active subscribers.

### Existing camera and vault functionality retained

The beta retains the v4.0 encrypted vault and camera tools:

- AES-256-GCM and XChaCha20-Poly1305 authenticated container encryption.
- Android Keystore device binding, recovery-key migration, Argon2id derivation, and optional ECDSA P-256 signatures.
- Streaming `.adsky` payload records and authenticated final integrity records.
- CameraX live preview, QR/barcode recognition, EXIF-aware gallery decoding, and local scan history.
- OpenCV document detection, corner ordering, perspective correction, multi-page scan review, filters, and PDF/JPEG export.
- Live-only magnifier with zoom, focus, torch, exposure, and accessibility filters.
- Local image metadata and color analysis.

## Implementation architecture

The feature is organized under `app/src/main/java/com/example/location/` and is split into isolated layers:

- `domain/` contains pure Kotlin value objects and algorithms: `GeoPoint`, longitude normalization, Haversine distance, initial bearing, `DistanceCalculator`, unit formatting, and `HeadingFilter`.
- `core/` contains `LocationFix`, `LocationState`, accuracy/freshness modeling, and `LocationRepository`. The repository is the single shared Fused Location Provider source used by the map, GPS, compass, and distance tools.
- `compass/` contains `CompassEngine`, sensor lifecycle handling, display-orientation remapping, magnetic declination, and heading models.
- `map/` contains provider abstraction, MapLibre lifecycle integration, style loading, map camera state, attribution, markers, overlays, taps, and error handling.
- `ui/` contains `LocationKitViewModel`, the Material 3 Location Kit screen, Map/Compass/GPS/Distance tabs, permission UI, status cards, and reusable location components.

Navigation adds `VaultScreen.LOCATION_KIT`, a Home screen feature card, a dedicated top app bar, and a back-stack-compatible Location Kit screen. The existing `VaultViewModel`, cryptographic engine, camera screens, and vault navigation remain the authoritative implementations for their respective domains.

## Open-source libraries and licenses

The beta uses the following open-source or openly distributed libraries already used by the project or added for Location Kit:

- **MapLibre Native Android SDK `11.12.1`** for native vector-map rendering and camera interaction.
- **OpenFreeMap Liberty style** for map styling and OpenMapTiles/OpenStreetMap attribution. Map data and style usage remain subject to their respective project terms.
- **Google Play services Location `21.3.0`** for the Fused Location Provider.
- **AndroidX Compose Material 3, Activity, Core, Lifecycle, Navigation, Room, DataStore, CameraX, and ExifInterface** for UI, lifecycle, persistence, camera, and image orientation support.
- **ZXing `3.5.4`** for QR and barcode decoding.
- **OpenCV local Android module** for document detection, perspective correction, image filters, and native processing. Native libraries are packaged for `arm64-v8a` and `armeabi-v7a`.
- **Google Tink** for authenticated encryption primitives.
- **Bouncy Castle** for Argon2id derivation and cryptographic support.
- **Kotlin Coroutines** for asynchronous state and processing flows.
- **Moshi** for structured local serialization where used by the existing vault and scan-history components.

The repository includes the OpenCV license at `opencv/LICENSE`. External library licenses and attribution requirements remain applicable to redistributed builds.

## Privacy and network scope

Location Kit requires `INTERNET` and `ACCESS_NETWORK_STATE` because MapLibre loads the configured OpenFreeMap style and map tiles. Foreground location access is requested separately through Android runtime permission. No background location permission is requested. Location data, coordinates, scan results, and processed images are not sent to an application-owned server. Users should review the selected map provider’s tile and privacy terms when using Map Explorer.

The encrypted vault remains local-first. Location Kit does not change the `.adsky` container format, its cryptographic algorithms, device binding, recovery workflow, or migration behavior.

## Validation completed for this beta

The merged implementation was built locally with:

```bash
gradle --no-daemon testDebugUnitTest assembleRelease
```

The Location Kit geometry tests and existing vault tests passed with zero failures. The release APK was checked with Android package metadata tools and reports package `com.adsky.gatekey`, version code `5`, and version name `5.0`.

## Testing focus for beta users

Please test Location Kit on physical devices with and without GPS hardware, with approximate and precise permissions, with location services disabled, with network disabled, after rotation, and after leaving/re-entering the feature. Also test MapLibre style-loading failures, compass sensor availability, antimeridian distances, invalid coordinates, rapid tab changes, and camera/vault navigation after returning from Location Kit.

Feedback should include the device model, Android version, permission mode, whether location services are enabled, and the affected Location Kit tab. Do not include private coordinates or encrypted vault contents in issue reports.
