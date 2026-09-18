package com.example.prayer.calculator

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculates high-precision Great-Circle Qibla direction (Bearing) and distance
 * to the Holy Kaaba in Mecca (21.422487° N, 39.826206° E).
 */
object QiblaCalculator {

    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the Qibla bearing in degrees from True North (0° - 360° clockwise).
     */
    fun calculateQiblaBearing(userLat: Double, userLng: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLng)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val bearingRad = atan2(y, x)
        var bearingDeg = Math.toDegrees(bearingRad)
        bearingDeg = (bearingDeg + 360.0) % 360.0

        return bearingDeg.toFloat()
    }

    /**
     * Calculates distance to Mecca in Kilometers.
     */
    fun calculateDistanceToMecca(userLat: Double, userLng: Double): Double {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaPhi = Math.toRadians(KAABA_LATITUDE - userLat)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLng)

        val a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Compute relative angle between current device heading and Qibla bearing.
     * Value ranges between -180 and +180 degrees (0 = user is pointing directly at Kaaba).
     */
    fun calculateRelativeAngle(deviceHeading: Float, qiblaBearing: Float): Float {
        var diff = qiblaBearing - deviceHeading
        while (diff < -180.0f) diff += 360.0f
        while (diff > 180.0f) diff -= 360.0f
        return diff
    }

    /**
     * Returns true if the device is pointing directly towards Qibla within tolerance (e.g. ±3 degrees).
     */
    fun isPointingTowardsQibla(deviceHeading: Float, qiblaBearing: Float, toleranceDeg: Float = 3.0f): Boolean {
        val relative = kotlin.math.abs(calculateRelativeAngle(deviceHeading, qiblaBearing))
        return relative <= toleranceDeg
    }
}
