package com.example.prayer.notification

import com.example.prayer.model.AdhanVoice

object AdhanVoicesCatalog {

    val VOICES = listOf(
        AdhanVoice(
            id = "makkah",
            name = "أذان الحرم المكي الشريف",
            muezzin = "الشيخ علي أحمد ملا (شيخ المؤذنين)",
            origin = "مكة المكرمة",
            audioUrl = "https://server8.mp3quran.net/adhan/makkah.mp3"
        ),
        AdhanVoice(
            id = "madinah",
            name = "أذان الحرم النبوي الشريف",
            muezzin = "الشيخ عصام بخاري",
            origin = "المدينة المنورة",
            audioUrl = "https://server8.mp3quran.net/adhan/madinah.mp3"
        ),
        AdhanVoice(
            id = "egypt",
            name = "أذان مصر (الجامع الأزهر)",
            muezzin = "الشيخ محمد رفعت رحمه الله",
            origin = "القاهرة",
            audioUrl = "https://server8.mp3quran.net/adhan/egypt.mp3"
        ),
        AdhanVoice(
            id = "afasy",
            name = "أذان الشيخ مشاري العفاسي",
            muezzin = "الشيخ مشاري راشد العفاسي",
            origin = "الكويت",
            audioUrl = "https://server8.mp3quran.net/adhan/afasy.mp3"
        ),
        AdhanVoice(
            id = "aqsa",
            name = "أذان المسجد الأقصى المبارك",
            muezzin = "مؤذنو المسجد الأقصى",
            origin = "القدس الشريف",
            audioUrl = "https://server8.mp3quran.net/adhan/aqsa.mp3"
        )
    )

    fun getVoiceById(id: String): AdhanVoice {
        return VOICES.find { it.id == id } ?: VOICES.first()
    }
}
