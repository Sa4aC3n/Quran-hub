package com.example.data.provider

import android.content.Context
import com.example.data.model.Ayah
import com.example.data.model.SurahText
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class QuranTextProvider(context: Context) {

    private val cache = ConcurrentHashMap<Int, SurahText>()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun getSurahText(surahNumber: Int): SurahText = withContext(Dispatchers.IO) {
        if (cache.containsKey(surahNumber)) {
            return@withContext cache[surahNumber]!!
        }

        // 1. Try local bundled / cached text
        val localText = loadSurahFromAssetsOrGenerated(surahNumber)
        if (localText != null && localText.ayahs.isNotEmpty()) {
            cache[surahNumber] = localText
            return@withContext localText
        }

        // 2. Try fetching full Uthmani text via online Quran Cloud API
        try {
            val request = Request.Builder()
                .url("https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val root = gson.fromJson(body, JsonObject::class.java)
                    if (root.has("data")) {
                        val dataObj = root.getAsJsonObject("data")
                        val sNum = dataObj.get("number").asInt
                        val sName = dataObj.get("name").asString
                        val englishName = dataObj.get("englishName").asString
                        val revType = dataObj.get("revelationType").asString
                        val count = dataObj.get("numberOfAyahs").asInt

                        val ayahsArray = dataObj.getAsJsonArray("ayahs")
                        val ayahsList = mutableListOf<Ayah>()

                        for (i in 0 until ayahsArray.size()) {
                            val aObj = ayahsArray.get(i).asJsonObject
                            val ayahNumber = aObj.get("number").asInt
                            val numInSurah = aObj.get("numberInSurah").asInt
                            var text = aObj.get("text").asString
                            val juz = aObj.get("juz").asInt
                            val page = aObj.get("page").asInt
                            val hizbQuarter = aObj.get("hizbQuarter").asInt
                            val sajda = aObj.get("sajda").let {
                                if (it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) it.asBoolean else false
                            }

                            // Clean Bismillah from first verse if not Al-Fatiha
                            if (numInSurah == 1 && sNum != 1) {
                                text = cleanBismillahFromVerse(text, sNum, numInSurah)
                            }

                            val tafsirSample = getQuickTafsir(sNum, numInSurah, text)

                            ayahsList.add(
                                Ayah(
                                    number = ayahNumber,
                                    numberInSurah = numInSurah,
                                    text = text,
                                    juz = juz,
                                    page = page,
                                    hizbQuarter = hizbQuarter,
                                    sajda = sajda,
                                    tafsir = tafsirSample
                                )
                            )
                        }

                        val result = SurahText(
                            number = sNum,
                            name = sName,
                            englishName = englishName,
                            englishNameTranslation = revType,
                            revelationType = if (revType.contains("Meccan", true)) "مكية" else "مدنية",
                            numberOfAyahs = count,
                            ayahs = ayahsList
                        )
                        cache[surahNumber] = result
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback default generated structure
        val fallback = generateFallbackSurah(surahNumber)
        cache[surahNumber] = fallback
        fallback
    }

    private fun loadSurahFromAssetsOrGenerated(surahNumber: Int): SurahText? {
        if (surahNumber == 1) {
            return SurahText(
                number = 1,
                name = "الفاتحة",
                englishName = "Al-Fatihah",
                englishNameTranslation = "The Opening",
                revelationType = "مكية",
                numberOfAyahs = 7,
                ayahs = listOf(
                    Ayah(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, 1, 1, false, "أبدأ قراءتي مستعيناً بالله، الرحمن بجميع خلقه، الرحيم بالمؤمنين."),
                    Ayah(2, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", 1, 1, 1, 1, 1, false, "الثناء الكامل والشكر الخالص لله وحده المربي لجميع المخلوقات بنعمه."),
                    Ayah(3, 3, "الرَّحْمَٰنِ الرَّحِيمِ", 1, 1, 1, 1, 1, false, "الرحمن ذو الرحمة الشاملة، والرحيم بالمؤمنين خصوصاً."),
                    Ayah(4, 4, "مَالِكِ يَوْمِ الدِّينِ", 1, 1, 1, 1, 1, false, "المالك المتصرف في يوم القيامة والجزاء والحساب وحده."),
                    Ayah(5, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", 1, 1, 1, 1, 1, false, "نخصك وحدك بالعبادة، ونطلب العون منك وحدك في كل أمورنا."),
                    Ayah(6, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", 1, 1, 1, 1, 1, false, "وفقنا وأرشدنا وثبتنا على الطريق الواضح المستقيم الذي لا عوج فيه (الإسلام)."),
                    Ayah(7, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", 1, 1, 1, 1, 1, false, "طريق النبيين والصديقين والشهداء والصالحين، لا طريق اليهود المغضوب عليهم، ولا النصارى التائهين.")
                )
            )
        }
        if (surahNumber == 112) {
            return SurahText(
                number = 112,
                name = "الإخلاص",
                englishName = "Al-Ikhlas",
                englishNameTranslation = "Sincerity",
                revelationType = "مكية",
                numberOfAyahs = 4,
                ayahs = listOf(
                    Ayah(6222, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", 30, 7, 604, 1, 240, false, "قل أيها الرسول لمن سألك: الله هو الواحد الأحد المتفرد بالكمال."),
                    Ayah(6223, 2, "اللَّهُ الصَّمَدُ", 30, 7, 604, 1, 240, false, "المقصود في الحوائج كلها، الذي كمل في صفاته وسيادته."),
                    Ayah(6224, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", 30, 7, 604, 1, 240, false, "ليس له ولد ولا والد ولا شبيه ولا مثيل تبارك وتعالى."),
                    Ayah(6225, 4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", 30, 7, 604, 1, 240, false, "ولم يكن له مماثلاً ولا مكافئاً أحد في ذاته أو أسمائه أو صفاته.")
                )
            )
        }
        if (surahNumber == 113) {
            return SurahText(
                number = 113,
                name = "الفلق",
                englishName = "Al-Falaq",
                englishNameTranslation = "Daybreak",
                revelationType = "مكية",
                numberOfAyahs = 5,
                ayahs = listOf(
                    Ayah(6226, 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", 30, 7, 604, 1, 240, false, "قل: ألتجئ وأعتصم برب الصبح وفالقه."),
                    Ayah(6227, 2, "مِن شَرِّ مَا خَلَقَ", 30, 7, 604, 1, 240, false, "من شر كل مخلوق فيه شر من إنس وجن وحيوان."),
                    Ayah(6228, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", 30, 7, 604, 1, 240, false, "ومن شر الليل المظلم إذا دخل وانتشر ظلامه."),
                    Ayah(6229, 4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", 30, 7, 604, 1, 240, false, "ومن شر الساحرات اللاتي يعقدن العقد وينفثن فيها بالسحر."),
                    Ayah(6230, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", 30, 7, 604, 1, 240, false, "ومن شر كل حاسد يتمنى زوال النعمة عن غيره ويسعى في ذلك.")
                )
            )
        }
        if (surahNumber == 114) {
            return SurahText(
                number = 114,
                name = "الناس",
                englishName = "An-Nas",
                englishNameTranslation = "Mankind",
                revelationType = "مكية",
                numberOfAyahs = 6,
                ayahs = listOf(
                    Ayah(6231, 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", 30, 7, 604, 1, 240, false, "قل: أعتصم وألتجئ برب البشر وخالقهم ومدبر أمورهم."),
                    Ayah(6232, 2, "مَلِكِ النَّاسِ", 30, 7, 604, 1, 240, false, "ملك الناس المتصرف في شؤونهم جميعاً."),
                    Ayah(6233, 3, "إِلَٰهِ النَّاسِ", 30, 7, 604, 1, 240, false, "معبودهم الحق الذي لا إله بحق سواه."),
                    Ayah(6234, 4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", 30, 7, 604, 1, 240, false, "من شر الشيطان الذي يوسوس عند الغفلة ويختفي عند ذكر الله."),
                    Ayah(6235, 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", 30, 7, 604, 1, 240, false, "الذي يلقي الشبهات والشرور في قلوب الناس."),
                    Ayah(6236, 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", 30, 7, 604, 1, 240, false, "من شياطين الجن وشياطين الإنس.")
                )
            )
        }
        return null
    }

    private fun generateFallbackSurah(surahNumber: Int): SurahText {
        val names = listOf(
            "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال", "التوبة", "يونس",
            "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل", "الإسراء", "الكهف", "مريم", "طه",
            "الأنبياء", "الحج", "المؤمنون", "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
            "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
            "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح", "الحجرات", "ق",
            "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر", "الممتحنة",
            "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج",
            "نوح", "الجن", "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
            "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد",
            "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة", "الزلزلة", "العاديات",
            "القارعة", "التكاثر", "العصر", "الهمزة", "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر",
            "المسد", "الإخلاص", "الفلق", "الناس"
        )
        val name = if (surahNumber in 1..114) names[surahNumber - 1] else "السورة $surahNumber"
        val juz = ((surahNumber * 30) / 114).coerceIn(1, 30)
        val page = ((surahNumber * 604) / 114).coerceIn(1, 604)

        // Generate placeholders that will be updated when connected or viewing
        val ayahs = (1..7).map { idx ->
            Ayah(
                number = idx,
                numberInSurah = idx,
                text = "سورة $name - الآية الكريمة رقم $idx",
                juz = juz,
                page = page,
                hizbQuarter = juz * 8,
                tafsir = "التفسير الميسر للآية الكريمة رقم $idx من سورة $name."
            )
        }

        return SurahText(
            number = surahNumber,
            name = name,
            englishName = "Surah $surahNumber",
            englishNameTranslation = "Holy Quran",
            revelationType = "مكية",
            numberOfAyahs = 7,
            ayahs = ayahs
        )
    }

    private fun getQuickTafsir(surah: Int, ayah: Int, text: String = ""): String {
        val embedded = EmbeddedTafseerRepository.getEmbeddedTafseer(
            tafseerId = 1,
            surahNumber = surah,
            ayahNumber = ayah,
            cleanAyahText = text
        )
        return embedded?.text ?: "تفسير وبيان لمعاني الآية $ayah من سورة $surah."
    }

    companion object {
        fun cleanBismillahFromVerse(rawText: String, surahNumber: Int = 2, ayahNumberInSurah: Int = 1): String {
            if (ayahNumberInSurah != 1 || surahNumber == 1) return rawText.trim()
            val text = rawText.trim()

            val knownPrefixes = listOf(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمٰنِ ٱلرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمٰنِ الرَّحِيمِ",
                "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيمِ",
                "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـنِ ٱلرَّحِيمِ",
                "بسم الله الرحمن الرحيم"
            )

            for (prefix in knownPrefixes) {
                if (text.startsWith(prefix)) {
                    val candidate = text.substring(prefix.length).trim()
                    if (candidate.isNotEmpty()) return candidate
                }
            }

            // Normalization strip fallback
            val normalized = normalizeArabic(text)
            val normPrefix = "بسم الله الرحمن الرحيم"
            if (normalized.startsWith(normPrefix)) {
                var baseCount = 0
                val targetBaseCount = normPrefix.replace(" ", "").length
                var cutIndex = -1
                for (i in text.indices) {
                    val char = text[i]
                    if (isArabicLetter(char)) {
                        baseCount++
                        if (baseCount == targetBaseCount) {
                            cutIndex = i + 1
                            break
                        }
                    }
                }
                if (cutIndex in 1 until text.length) {
                    val candidate = text.substring(cutIndex).trim()
                    if (candidate.isNotEmpty()) return candidate
                }
            }

            return text
        }

        private fun normalizeArabic(input: String): String {
            return input
                .replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
                .replace("ٱ", "ا")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ـ", "")
                .trim()
        }

        private fun isArabicLetter(c: Char): Boolean {
            return c in '\u0621'..'\u064A' || c == '\u0671' || c == 'ٱ'
        }
    }
}
