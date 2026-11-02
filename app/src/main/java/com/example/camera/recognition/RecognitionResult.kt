package com.example.camera.recognition

import com.example.camera.core.Pt
import com.google.zxing.BarcodeFormat

enum class ContentKind(val label: String) {
    Url("Link"), Email("Email"), Phone("Phone"), Sms("SMS"), Wifi("Wi-Fi"), Geo("Location"), Contact("Contact"), Product("Product code"), Text("Text")
}

data class RecognitionResult(
    val text: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Corner points normalized to the analysed frame (empty for gallery images). */
    val points: List<Pt> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
) {
    val kind: ContentKind = classify(text, format)

    /** Structured fields for formats worth unpacking (currently Wi-Fi). */
    val fields: List<Pair<String, String>> get() = if (kind == ContentKind.Wifi) parseWifi(text) else emptyList()

    companion object {
        fun prettyFormat(format: BarcodeFormat): String = when (format) {
            BarcodeFormat.QR_CODE -> "QR Code"
            BarcodeFormat.DATA_MATRIX -> "Data Matrix"
            BarcodeFormat.AZTEC -> "Aztec"
            BarcodeFormat.PDF_417 -> "PDF417"
            BarcodeFormat.EAN_13 -> "EAN-13"
            BarcodeFormat.EAN_8 -> "EAN-8"
            BarcodeFormat.UPC_A -> "UPC-A"
            BarcodeFormat.UPC_E -> "UPC-E"
            BarcodeFormat.CODE_39 -> "Code 39"
            BarcodeFormat.CODE_93 -> "Code 93"
            BarcodeFormat.CODE_128 -> "Code 128"
            BarcodeFormat.ITF -> "ITF"
            BarcodeFormat.CODABAR -> "Codabar"
            BarcodeFormat.RSS_14 -> "GS1 DataBar"
            BarcodeFormat.RSS_EXPANDED -> "GS1 DataBar Expanded"
            else -> format.name.replace('_', ' ')
        }

        private fun classify(text: String, format: String): ContentKind {
            val t = text.trim()
            val lower = t.lowercase()
            return when {
                lower.startsWith("http://") || lower.startsWith("https://") -> ContentKind.Url
                lower.startsWith("wifi:") -> ContentKind.Wifi
                lower.startsWith("mailto:") || Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(t) -> ContentKind.Email
                lower.startsWith("tel:") -> ContentKind.Phone
                lower.startsWith("smsto:") || lower.startsWith("sms:") -> ContentKind.Sms
                lower.startsWith("geo:") -> ContentKind.Geo
                lower.startsWith("begin:vcard") || lower.startsWith("mecard:") -> ContentKind.Contact
                format != "QR Code" && format != "Data Matrix" && format != "Aztec" && format != "PDF417" && t.all { it.isLetterOrDigit() || it == '-' } -> ContentKind.Product
                else -> ContentKind.Text
            }
        }

        /** WIFI:T:WPA;S:name;P:secret;; */
        private fun parseWifi(text: String): List<Pair<String, String>> {
            val body = text.substringAfter(':', "")
            val parts = Regex("(?<!\\\\);").split(body)
            fun field(key: String) = parts.firstOrNull { it.startsWith("$key:") }?.substringAfter(':')?.replace("\\;", ";")
            return listOfNotNull(
                field("S")?.let { "Network" to it },
                field("T")?.let { "Security" to it },
                field("P")?.takeIf { it.isNotEmpty() }?.let { "Password" to it }
            )
        }
    }
}
