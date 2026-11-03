# Location Kit (Phase 1)

Entry: Home -> Location Kit. Tools: Map Explorer · Compass · GPS Coordinates · Distance.

```
app/src/main/java/com/example/location/
├── domain/   GeoPoint · GeoMath (Haversine, bearing, longitude wrapping) · DistanceCalculator/Distance/UnitSystem · HeadingFilter   (pure Kotlin)
├── core/     LocationModels (LocationFix, LocationState, freshness/accuracy) · LocationRepository (Fused Location, single shared source) · LocationKit
├── compass/  CompassEngine (SensorManager) · CompassModels · MagneticDeclination (true north)
├── map/      MapProvider(+OpenFreeMap Liberty) · MapModels · LocationMap (MapLibre Native composable: lifecycle, style, errors, camera, overlays, taps)
└── ui/       LocationKitViewModel · LocationKitScreen · MapTab · CompassTab · GpsTab · DistanceTab · LocationUiParts
app/src/test/java/com/example/location/GeoMathTest.kt
```

Dependencies added: `org.maplibre.gl:android-sdk:11.12.1`, `com.google.android.gms:play-services-location:21.3.0`.
Manifest added: INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION (foreground only; no background location).

Swap map provider: define another `MapProvider(name, styleUrl, attribution)` in `MapProviders` and change `MapProviders.Default`.
