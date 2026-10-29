package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Expressive badge shapes that replace internal MaterialShapes.
 * Renders high-fidelity scalloped, cookie, sunburst, clover, and gem geometries.
 */
enum class VaultBadgeShape {
    Cookie9Sided,
    Cookie12Sided,
    Sunny,
    Clover4Leaf,
    Gem,
    SoftBurst;

    fun toShape(): Shape = when (this) {
        Cookie9Sided -> PolygonalBadgeShape(points = 9, innerRadiusRatio = 0.88f, cornerRadius = 6.dp)
        Cookie12Sided -> PolygonalBadgeShape(points = 12, innerRadiusRatio = 0.90f, cornerRadius = 5.dp)
        Sunny -> PolygonalBadgeShape(points = 8, innerRadiusRatio = 0.78f, cornerRadius = 4.dp)
        Clover4Leaf -> RoundedCornerShape(22.dp)
        Gem -> CutCornerShape(18.dp)
        SoftBurst -> PolygonalBadgeShape(points = 10, innerRadiusRatio = 0.84f, cornerRadius = 6.dp)
    }
}

/**
 * Procedural polygon badge shape supporting rounded star/cookie/sunburst curves.
 */
class PolygonalBadgeShape(
    private val points: Int,
    private val innerRadiusRatio: Float = 0.85f,
    private val cornerRadius: Dp = 4.dp
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = minOf(centerX, centerY)
        val innerRadius = outerRadius * innerRadiusRatio
        val totalSteps = points * 2
        val stepAngle = (2 * PI / totalSteps).toFloat()

        for (i in 0 until totalSteps) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * stepAngle - (PI / 2).toFloat()
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
