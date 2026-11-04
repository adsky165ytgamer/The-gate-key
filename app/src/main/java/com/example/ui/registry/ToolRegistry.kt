package com.example.ui.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PublishedWithChanges
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.VaultScreen
import com.example.ui.theme.VaultBadgeShape

/** Which Material color role a category (and its tools) is drawn with. Matches the Home cards. */
enum class ToolTone { PRIMARY, SECONDARY, TERTIARY }

/** What a tool needs or does, shown as chips in the help sheet. */
enum class ToolRequirement(val label: String, val icon: ImageVector) {
    OFFLINE("Works offline", Icons.Rounded.CloudOff),
    CAMERA("Uses the camera", Icons.Rounded.CameraAlt),
    LOCATION("Uses your location", Icons.Rounded.LocationOn),
    SENSORS("Uses motion sensors", Icons.Rounded.Sensors),
    NETWORK("Loads map tiles online", Icons.Rounded.Public)
}

/**
 * The three "types" on the Home screen. Each opens a hub: a 2-column grid of its tools.
 * Single source of truth for titles, descriptions, icons and colors.
 */
enum class ToolCategory(
    val title: String,
    val tagline: String,
    val about: String,
    val icon: ImageVector,
    val badge: VaultBadgeShape,
    val tone: ToolTone,
    val screen: VaultScreen,
    val isBeta: Boolean = false
) {
    SECURITY(
        title = "Security & Encryption",
        tagline = "Protect files and manage your security tools",
        about = "Seal files into authenticated .adsky containers with AES-256-GCM or XChaCha20-Poly1305, " +
            "unlock them again, and manage the recovery, migration and audit tools around them. " +
            "Everything runs offline on this device.",
        icon = Icons.Rounded.Shield,
        badge = VaultBadgeShape.Cookie12Sided,
        tone = ToolTone.PRIMARY,
        screen = VaultScreen.SECURITY_AND_ENCRYPTION
    ),
    CAMERA(
        title = "Camera Tools",
        tagline = "Scan, read, magnify and inspect on-device",
        about = "Camera tools for documents, codes and fine detail. Images are processed locally and " +
            "nothing is uploaded; files only leave the app when you export them yourself.",
        icon = Icons.Rounded.CameraAlt,
        badge = VaultBadgeShape.Sunny,
        tone = ToolTone.TERTIARY,
        screen = VaultScreen.CAMERA_TOOLS
    ),
    LOCATION(
        title = "Location Kit",
        tagline = "Map, compass, GPS and distance",
        about = "Find where you are and where things are. Location is only used while a tool is open, " +
            "there is no background tracking, and no coordinates are sent to an app server. " +
            "This is a beta feature.",
        icon = Icons.Rounded.Explore,
        badge = VaultBadgeShape.Clover4Leaf,
        tone = ToolTone.SECONDARY,
        screen = VaultScreen.LOCATION_KIT,
        isBeta = true
    );

    val tools: List<ToolId> get() = ToolId.entries.filter { it.category == this }

    /** Permissions and network needs across the whole category (offline is the default, so it is left out). */
    val requirements: List<ToolRequirement>
        get() = tools.flatMap { it.requirements }.distinct().filter { it != ToolRequirement.OFFLINE }

    companion object {
        fun forScreen(screen: VaultScreen): ToolCategory? = entries.firstOrNull { it.screen == screen }
    }
}

/** Every tool in the app. One tile in a hub grid each. */
enum class ToolId(
    val category: ToolCategory,
    val title: String,
    val summary: String,
    val about: String,
    val icon: ImageVector,
    val badge: VaultBadgeShape,
    val requirements: List<ToolRequirement>
) {
    // ── Security & Encryption ──
    ENCRYPT(
        ToolCategory.SECURITY, "Encrypt",
        "Seal any file into a protected .adsky container.",
        "Pick a file, choose AES-256-GCM or XChaCha20-Poly1305, then choose how it can be unlocked: " +
            "this device's Keystore, a PIN or passphrase (Argon2id) and a master recovery secret. " +
            "You can also sign the container with your ECDSA P-256 identity.",
        Icons.Rounded.Lock, VaultBadgeShape.Cookie9Sided, listOf(ToolRequirement.OFFLINE)
    ),
    DECRYPT(
        ToolCategory.SECURITY, "Decrypt",
        "Unlock and verify an encrypted container.",
        "Open an .adsky file with the original device Keystore, your PIN or passphrase, or the master " +
            "recovery secret. Every record is authenticated, and a digital signature is verified when present.",
        Icons.Rounded.LockOpen, VaultBadgeShape.Sunny, listOf(ToolRequirement.OFFLINE)
    ),
    BATCH(
        ToolCategory.SECURITY, "Batch",
        "Encrypt several files in one queue.",
        "Select multiple files and they are encrypted one after another, with progress for the whole queue.",
        Icons.Rounded.FolderZip, VaultBadgeShape.Gem, listOf(ToolRequirement.OFFLINE)
    ),
    RECOVERY(
        ToolCategory.SECURITY, "Recovery",
        "View and protect your master recovery secret.",
        "The 256-bit master recovery secret can unlock any .adsky file that carries a recovery envelope. " +
            "It is stored encrypted by the Android Keystore; reveal it to write it down. If you lose both " +
            "this phone and the secret, files cannot be recovered by anyone.",
        Icons.Rounded.Key, VaultBadgeShape.SoftBurst, listOf(ToolRequirement.OFFLINE)
    ),
    MIGRATION(
        ToolCategory.SECURITY, "Migration",
        "Move or upgrade a container to a new device or algorithm.",
        "Inspect a container header, unlock it, then re-wrap it for this device's Keystore, a new credential " +
            "or a different algorithm. Nothing leaves the phone.",
        Icons.Rounded.PublishedWithChanges, VaultBadgeShape.Cookie12Sided, listOf(ToolRequirement.OFFLINE)
    ),
    AUDIT(
        ToolCategory.SECURITY, "Audit & Device",
        "Hardware security, signing identity and activity log.",
        "See whether your keys are hardware-backed (StrongBox or TEE), check your signing identity, and " +
            "review the local audit log of cryptographic operations. The log stays on this device and can be cleared.",
        Icons.Rounded.Security, VaultBadgeShape.Clover4Leaf, listOf(ToolRequirement.OFFLINE)
    ),

    // ── Camera Tools ──
    SCANNER(
        ToolCategory.CAMERA, "Document Scanner",
        "Scan pages into a clean PDF or JPEG.",
        "Finds the page edges live, flattens the perspective and lets you choose Original, Color, Grayscale " +
            "or Black & white. Reorder or delete pages, then export a PDF or JPEG with Android's file picker. " +
            "Photos from your gallery can be scanned too.",
        Icons.Rounded.DocumentScanner, VaultBadgeShape.Cookie9Sided,
        listOf(ToolRequirement.OFFLINE, ToolRequirement.CAMERA)
    ),
    CODES(
        ToolCategory.CAMERA, "QR & Barcode",
        "Read codes from the camera or a photo.",
        "Recognizes QR codes and barcodes live or from a gallery image, and understands links, email, phone " +
            "numbers, Wi-Fi and coordinates, with copy, share and open actions. Scan history stays on this " +
            "device and can be cleared.",
        Icons.Rounded.QrCodeScanner, VaultBadgeShape.Sunny,
        listOf(ToolRequirement.OFFLINE, ToolRequirement.CAMERA)
    ),
    MAGNIFIER(
        ToolCategory.CAMERA, "Magnifier",
        "Live zoom with focus, light and contrast.",
        "A live-only magnifier with zoom, centre focus, flashlight, exposure and High contrast, Grayscale or " +
            "Inverted viewing. It never captures or stores a frame.",
        Icons.Rounded.ZoomIn, VaultBadgeShape.Gem,
        listOf(ToolRequirement.OFFLINE, ToolRequirement.CAMERA)
    ),
    IMAGE_INFO(
        ToolCategory.CAMERA, "Image Information",
        "Inspect a photo's details and colors.",
        "Reads dimensions, file details, EXIF properties and a color analysis from an image you choose. " +
            "It all happens locally, and no camera permission is needed.",
        Icons.Rounded.Image, VaultBadgeShape.SoftBurst, listOf(ToolRequirement.OFFLINE)
    ),

    // ── Location Kit ──
    MAP(
        ToolCategory.LOCATION, "Map Explorer",
        "Browse the map with your position on it.",
        "A MapLibre vector map (OpenFreeMap Liberty style) with your location dot, tap handling and clear " +
            "loading and error states. It needs a network connection to load map tiles.",
        Icons.Rounded.Map, VaultBadgeShape.Cookie9Sided,
        listOf(ToolRequirement.LOCATION, ToolRequirement.NETWORK)
    ),
    COMPASS(
        ToolCategory.LOCATION, "Compass",
        "Smooth heading with true-north correction.",
        "A sensor-based compass with a smoothed heading and cardinal directions. When a location fix is " +
            "available it can switch from magnetic to true north.",
        Icons.Rounded.Explore, VaultBadgeShape.Sunny,
        listOf(ToolRequirement.OFFLINE, ToolRequirement.SENSORS)
    ),
    GPS(
        ToolCategory.LOCATION, "GPS Coordinates",
        "Latitude, longitude, accuracy and freshness.",
        "Shows your current coordinates with accuracy and how fresh the fix is, and explains clearly when " +
            "permission or location services are missing. Easy to copy and share.",
        Icons.Rounded.GpsFixed, VaultBadgeShape.Gem, listOf(ToolRequirement.LOCATION)
    ),
    DISTANCE(
        ToolCategory.LOCATION, "Distance",
        "Measure distance and bearing between two points.",
        "Enter coordinates or use your current location for either end. Calculates the great-circle " +
            "distance and initial bearing in metric or imperial, and handles the antimeridian.",
        Icons.Rounded.Straighten, VaultBadgeShape.SoftBurst, listOf(ToolRequirement.OFFLINE)
    )
}
