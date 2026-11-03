package com.example.location.map

/**
 * Everything that is specific to one map provider. Swapping providers (another OpenFreeMap style, MapTiler,
 * a self-hosted tile server...) means defining another instance - no rendering code depends on a concrete URL.
 */
data class MapProvider(
    val name: String,
    /** MapLibre style JSON URL. For OpenFreeMap this is a public endpoint: no API key, no account. */
    val styleUrl: String,
    /** Text that must stay visible on the map whenever this provider is used. */
    val attribution: String
)

object MapProviders {
    /** OpenFreeMap "Liberty" style, rendered from OpenMapTiles vector tiles built on OpenStreetMap data. */
    val OpenFreeMapLiberty = MapProvider(
        name = "OpenFreeMap Liberty",
        styleUrl = "https://tiles.openfreemap.org/styles/liberty",
        attribution = "OpenFreeMap © OpenMapTiles · Data © OpenStreetMap contributors"
    )

    val Default: MapProvider = OpenFreeMapLiberty
}
