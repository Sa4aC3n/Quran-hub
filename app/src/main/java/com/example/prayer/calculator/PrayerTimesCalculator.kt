package com.example.prayer.calculator

import com.example.prayer.model.AsrJuristic
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.PrayerTimeOffsets
import com.example.prayer.model.PrayerType
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * High-accuracy astronomical Islamic Prayer Times Calculator.
 * Works 100% offline using standard astronomical solar equations (Jean Meeus / PrayTimes algorithms).
 */
object PrayerTimesCalculator {

    data class CalculatedTimes(
        val fajr: Long,
        val sunrise: Long,
        val dhuhr: Long,
        val asr: Long,
        val maghrib: Long,
        val isha: Long
    ) {
        fun getTime(type: PrayerType): Long = when (type) {
            PrayerType.FAJR -> fajr
            PrayerType.SUNRISE -> sunrise
            PrayerType.DHUHR -> dhuhr
            PrayerType.ASR -> asr
            PrayerType.MAGHRIB -> maghrib
            PrayerType.ISHA -> isha
        }
    }

    /**
     * Compute prayer times for a given calendar date, latitude, longitude and calculation configuration.
     */
    fun calculate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.EGYPTIAN,
        asrJuristic: AsrJuristic = AsrJuristic.STANDARD,
        offsets: PrayerTimeOffsets = PrayerTimeOffsets(),
        timeZone: TimeZone = calendar.timeZone
    ): CalculatedTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val tzOffsetHours = timeZone.getOffset(calendar.timeInMillis).toDouble() / (1000.0 * 60.0 * 60.0)

        // Julian Date
        val jd = julianDate(year, month, day)

        // Solar coordinates for midday of the given date
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val ra = fixHour(radToDeg(atan2(cos(degToRad(e)) * sin(degToRad(l)), cos(degToRad(l)))) / 15.0)
        val declination = radToDeg(asin(sin(degToRad(e)) * sin(degToRad(l))))
        val eqTime = q / 15.0 - ra

        // Midday (Dhuhr base time in decimal hours)
        val noon = fixHour(12.0 + tzOffsetHours - (longitude / 15.0) - eqTime)

        // 1. Fajr
        val fajrHour = noon - sunAngleTime(method.fajrAngle, latitude, declination)

        // 2. Sunrise (astronomical refraction 0.8333 deg)
        val sunriseHour = noon - sunAngleTime(0.8333, latitude, declination)

        // 3. Dhuhr (add small safe margin of ~1-2 minutes for sun zenith passage)
        val dhuhrHour = noon + (2.0 / 60.0)

        // 4. Asr
        val asrAltitude = radToDeg(atan(1.0 / (asrJuristic.factor + tan(degToRad(abs(latitude - declination))))))
        val asrHour = noon + sunAltitudeTime(asrAltitude, latitude, declination)

        // 5. Maghrib / Sunset (0.8333 deg)
        val maghribHour = noon + sunAngleTime(0.8333, latitude, declination)

        // 6. Isha
        val ishaHour = if (method.ishaIntervalMinutes != null) {
            maghribHour + (method.ishaIntervalMinutes.toDouble() / 60.0)
        } else {
            noon + sunAngleTime(method.ishaAngle, latitude, declination)
        }

        // Convert decimal hours to epoch millis for that day
        val fajrMillis = toEpochMillis(calendar, fajrHour, offsets.fajr)
        val sunriseMillis = toEpochMillis(calendar, sunriseHour, offsets.sunrise)
        val dhuhrMillis = toEpochMillis(calendar, dhuhrHour, offsets.dhuhr)
        val asrMillis = toEpochMillis(calendar, asrHour, offsets.asr)
        val maghribMillis = toEpochMillis(calendar, maghribHour, offsets.maghrib)
        val ishaMillis = toEpochMillis(calendar, ishaHour, offsets.isha)

        return CalculatedTimes(
            fajr = fajrMillis,
            sunrise = sunriseMillis,
            dhuhr = dhuhrMillis,
            asr = asrMillis,
            maghrib = maghribMillis,
            isha = ishaMillis
        )
    }

    /**
     * Determine best default calculation method based on country name or code.
     */
    fun getDefaultMethodForCountry(country: String): CalculationMethod {
        val lower = country.trim().lowercase()
        return when {
            lower.contains("مصر") || lower.contains("egypt") || lower.contains("سودان") || lower.contains("sudan") ||
                    lower.contains("ليبيا") || lower.contains("libya") || lower.contains("لبنان") || lower.contains("lebanon") ||
                    lower.contains("سوريا") || lower.contains("syria") -> CalculationMethod.EGYPTIAN

            lower.contains("سعودية") || lower.contains("saudi") || lower.contains("sa") || lower.contains("مكة") ||
                    lower.contains("riyadh") || lower.contains("يمن") || lower.contains("yemen") -> CalculationMethod.UMM_AL_QURA

            lower.contains("إمارات") || lower.contains("emirates") || lower.contains("uae") || lower.contains("dubai") -> CalculationMethod.DUBAI
            lower.contains("كويت") || lower.contains("kuwait") -> CalculationMethod.KUWAIT
            lower.contains("قطر") || lower.contains("qatar") -> CalculationMethod.QATAR
            lower.contains("تركيا") || lower.contains("turkey") || lower.contains("türkiye") -> CalculationMethod.DIYANET
            lower.contains("باكستان") || lower.contains("pakistan") || lower.contains("هند") || lower.contains("india") ||
                    lower.contains("bangladesh") || lower.contains("afghanistan") -> CalculationMethod.KARACHI
            lower.contains("أمريكا") || lower.contains("usa") || lower.contains("united states") || lower.contains("canada") -> CalculationMethod.ISNA
            else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        }
    }

    private fun sunAngleTime(angle: Double, latitude: Double, declination: Double): Double {
        val numerator = -sin(degToRad(angle)) - sin(degToRad(latitude)) * sin(degToRad(declination))
        val denominator = cos(degToRad(latitude)) * cos(degToRad(declination))
        val cosH = (numerator / denominator).coerceIn(-1.0, 1.0)
        return radToDeg(acos(cosH)) / 15.0
    }

    private fun sunAltitudeTime(altitude: Double, latitude: Double, declination: Double): Double {
        val numerator = sin(degToRad(altitude)) - sin(degToRad(latitude)) * sin(degToRad(declination))
        val denominator = cos(degToRad(latitude)) * cos(degToRad(declination))
        val cosH = (numerator / denominator).coerceIn(-1.0, 1.0)
        return radToDeg(acos(cosH)) / 15.0
    }

    private fun toEpochMillis(baseCalendar: Calendar, decimalHour: Double, offsetMinutes: Int): Long {
        var hourVal = fixHour(decimalHour)
        val hours = hourVal.toInt()
        val remainderMinutes = (hourVal - hours) * 60.0
        val minutes = remainderMinutes.toInt()
        val seconds = ((remainderMinutes - minutes) * 60.0).toInt()

        val cal = baseCalendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, hours)
        cal.set(Calendar.MINUTE, minutes)
        cal.set(Calendar.SECOND, seconds)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, offsetMinutes)
        return cal.timeInMillis
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun degToRad(deg: Double): Double = deg * (Math.PI / 180.0)
    private fun radToDeg(rad: Double): Double = rad * (180.0 / Math.PI)

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }
}
