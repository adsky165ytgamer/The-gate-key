package com.example.camera.scanner

import com.example.camera.core.Pt
import kotlin.math.hypot

/** Four document corners in normalized (0..1) coordinates of an upright image: top-left, top-right, bottom-right, bottom-left. */
data class Quad(val tl: Pt, val tr: Pt, val br: Pt, val bl: Pt) {

    fun points(): List<Pt> = listOf(tl, tr, br, bl)

    fun with(index: Int, p: Pt): Quad = when (index) {
        0 -> copy(tl = p)
        1 -> copy(tr = p)
        2 -> copy(br = p)
        else -> copy(bl = p)
    }

    /** Mean distance between matching corners. */
    fun distanceTo(o: Quad): Float {
        val a = points()
        val b = o.points()
        var sum = 0f
        for (i in 0 until 4) sum += hypot(a[i].x - b[i].x, a[i].y - b[i].y)
        return sum / 4f
    }

    fun lerp(o: Quad, t: Float): Quad {
        fun mix(a: Pt, b: Pt) = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        return Quad(mix(tl, o.tl), mix(tr, o.tr), mix(br, o.br), mix(bl, o.bl))
    }

    companion object {
        val FULL = Quad(Pt(0f, 0f), Pt(1f, 0f), Pt(1f, 1f), Pt(0f, 1f))

        fun inset(margin: Float = 0.08f) = Quad(
            Pt(margin, margin), Pt(1f - margin, margin), Pt(1f - margin, 1f - margin), Pt(margin, 1f - margin)
        )
    }
}
