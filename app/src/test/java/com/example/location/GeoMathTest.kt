package com.example.location

import com.example.location.domain.DistanceCalculator
import com.example.location.domain.DistanceOutcome
import com.example.location.domain.GeoMath
import com.example.location.domain.GeoPoint
import com.example.location.domain.HeadingFilter
import com.example.location.domain.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GeoMathTest {

    private val london = GeoPoint(51.5074, -0.1278)
    private val paris = GeoPoint(48.8566, 2.3522)

    @Test fun londonToParis() {
        assertEquals(343_556.5, GeoMath.distanceMeters(london, paris), 5.0)
        assertEquals(148.1, GeoMath.initialBearing(london, paris), 0.2)
    }

    @Test fun newYorkToLosAngeles() {
        assertEquals(3_935_751.7, GeoMath.distanceMeters(GeoPoint(40.7128, -74.0060), GeoPoint(34.0522, -118.2437)), 50.0)
    }

    @Test fun antimeridianIsTheShortWay() {
        val d = GeoMath.distanceMeters(GeoPoint(0.0, 179.5), GeoPoint(0.0, -179.5))
        assertEquals(111_195.1, d, 5.0) // 1 degree of longitude at the equator, not 359
        assertEquals(90.0, GeoMath.initialBearing(GeoPoint(0.0, 179.5), GeoPoint(0.0, -179.5)), 0.01)
    }

    @Test fun poleToPoleIsHalfTheCircumference() {
        assertEquals(20_015_114.4, GeoMath.distanceMeters(GeoPoint(90.0, 0.0), GeoPoint(-90.0, 0.0)), 5.0)
    }

    @Test fun distanceIsSymmetric() {
        assertEquals(GeoMath.distanceMeters(london, paris), GeoMath.distanceMeters(paris, london), 1e-6)
    }

    @Test fun identicalPoints() {
        val r = DistanceCalculator.calculate(london, london) as DistanceOutcome.Success
        assertEquals(0.0, r.distance.meters, 0.0)
        assertTrue(r.identical)
        assertNull(r.bearingDegrees)
    }

    @Test fun incompleteAndInvalid() {
        assertTrue(DistanceCalculator.calculate(null, paris) is DistanceOutcome.Incomplete)
        assertTrue(DistanceCalculator.calculate(london, GeoPoint(95.0, 0.0)) is DistanceOutcome.Invalid)
        assertTrue(DistanceCalculator.calculate(GeoPoint(Double.NaN, 0.0), london) is DistanceOutcome.Invalid)
    }

    @Test fun longitudeWrapping() {
        assertEquals(-170.0, GeoMath.normalizeLongitude(190.0), 1e-9)
        assertEquals(170.0, GeoMath.normalizeLongitude(-190.0), 1e-9)
        assertEquals(0.0, GeoMath.normalizeLongitude(720.0), 1e-9)
        assertEquals(180.0, GeoMath.normalizeLongitude(180.0), 1e-9)
    }

    @Test fun pointFactoryValidates() {
        assertNull(GeoPoint.of(91.0, 0.0))
        assertNull(GeoPoint.of(0.0, Double.POSITIVE_INFINITY))
        assertEquals(-170.0, GeoPoint.of(10.0, 190.0)!!.longitude, 1e-9)
        assertNotNull(GeoPoint.parse("48,8566", "2.3522"))
        assertNull(GeoPoint.parse("abc", "2"))
        assertNull(GeoPoint.parse("10", "200"))
    }

    @Test fun formatting() {
        val us = Locale.US
        assertEquals("850 m", com.example.location.domain.Distance(850.0).format(UnitSystem.Metric, us))
        assertEquals("343.6 km", com.example.location.domain.Distance(343_556.5).format(UnitSystem.Metric, us))
        assertEquals("328 ft", com.example.location.domain.Distance(100.0).format(UnitSystem.Imperial, us))
    }

    @Test fun cardinalAndAngles() {
        assertEquals("N", GeoMath.cardinal(359.0))
        assertEquals("SE", GeoMath.cardinal(135.0))
        assertEquals(-20.0, GeoMath.angleDelta(10.0, 350.0), 1e-9)
    }

    @Test fun headingFilterDoesNotSwingThroughSouth() {
        val f = HeadingFilter(alpha = 0.5)
        f.filter(359.0)
        val next = f.filter(1.0)
        assertTrue("expected near north, was $next", next < 5.0 || next > 355.0)
    }
}
