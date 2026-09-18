package com.example.prayer.calculator

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

/**
 * Astronomical and algorithmic converter for Hijri (Islamic) Calendar dates and formatting.
 */
object HijriCalendarHelper {

    private val HIJRI_MONTH_NAMES_AR = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val ARABIC_DAYS = arrayOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    data class HijriDate(
        val day: Int,
        val month: Int, // 1 to 12
        val year: Int,
        val monthNameAr: String,
        val dayNameAr: String
    ) {
        fun formatFormattedArabic(): String {
            val dayAr = toArabicDigits(day)
            val yearAr = toArabicDigits(year)
            return "$dayNameAr، $dayAr $monthNameAr $yearAr هـ"
        }
    }

    /**
     * Convert a Gregorian Date/Calendar to Hijri Date.
     */
    fun getHijriDate(calendar: Calendar = Calendar.getInstance()): HijriDate {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday
        val dayNameAr = ARABIC_DAYS[dayOfWeek - 1]

        // Approximate Astronomical Kuwaiti Algorithm
        var jd = getJulianDay(y, m, d)
        val epoch = 1948439.5
        val daysSinceEpoch = jd - epoch
        val cycle = floor(daysSinceEpoch / 10631.0)
        val remainingDays = daysSinceEpoch - cycle * 10631.0
        val yearInCycle = floor((remainingDays - 0.5) / 354.366)
        val hijriYear = (cycle * 30 + yearInCycle + 1).toInt()

        val yearStartDays = floor((yearInCycle * 354.366) + 0.5)
        val dayInYear = remainingDays - yearStartDays

        var hijriMonth = (floor(dayInYear / 29.5) + 1).toInt().coerceIn(1, 12)
        var hijriDay = (dayInYear - floor((hijriMonth - 1) * 29.5)).toInt() + 1

        if (hijriDay > 30) {
            hijriDay = 30
        }
        if (hijriDay < 1) {
            hijriDay = 1
        }

        val monthName = HIJRI_MONTH_NAMES_AR[(hijriMonth - 1).coerceIn(0, 11)]

        return HijriDate(
            day = hijriDay,
            month = hijriMonth,
            year = hijriYear,
            monthNameAr = monthName,
            dayNameAr = dayNameAr
        )
    }

    fun formatGregorianArabic(calendar: Calendar = Calendar.getInstance()): String {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val monthNames = arrayOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val dayName = ARABIC_DAYS[dayOfWeek - 1]
        val monthName = monthNames[month.coerceIn(0, 11)]

        return "$dayName، ${toArabicDigits(day)} $monthName ${toArabicDigits(year)} م"
    }

    fun formatTimeArabic(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        var hour = cal.get(Calendar.HOUR)
        if (hour == 0) hour = 12
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "ص" else "م"

        val hourStr = String.format(Locale.getDefault(), "%02d", hour)
        val minStr = String.format(Locale.getDefault(), "%02d", minute)

        return "${toArabicDigits(hourStr)}:${toArabicDigits(minStr)} $amPm"
    }

    fun formatDurationArabic(millis: Long): String {
        if (millis <= 0) return "حان الآن"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${toArabicDigits(hours.toInt())} س و ${toArabicDigits(minutes.toInt())} د"
            minutes > 0 -> "${toArabicDigits(minutes.toInt())} د و ${toArabicDigits(seconds.toInt())} ث"
            else -> "${toArabicDigits(seconds.toInt())} ثانية"
        }
    }

    fun toArabicDigits(number: Any): String {
        val s = number.toString()
        val builder = StringBuilder()
        for (ch in s) {
            when (ch) {
                '0' -> builder.append('٠')
                '1' -> builder.append('١')
                '2' -> builder.append('٢')
                '3' -> builder.append('٣')
                '4' -> builder.append('٤')
                '5' -> builder.append('٥')
                '6' -> builder.append('٦')
                '7' -> builder.append('٧')
                '8' -> builder.append('٨')
                '9' -> builder.append('٩')
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun getJulianDay(year: Int, month: Int, day: Int): Double {
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
}
