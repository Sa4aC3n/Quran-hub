package com.example.data.dedup

import com.example.data.local.db.AudioSourceEntity
import com.example.data.local.db.EditionEntity
import com.example.data.local.db.ReciterEntity
import com.example.data.local.db.ReviewItemEntity
import com.example.data.local.db.SurahRecordingEntity
import com.example.data.model.AudioQualityLevel
import com.example.data.model.DiscoveredReciter
import com.example.data.model.RecitationEdition
import com.example.data.model.ReviewItem
import com.example.data.model.SurahAudioItem
import com.example.data.validation.QuranEditionValidator
import java.util.concurrent.atomic.AtomicInteger

class AudioDeduplicationManager {

    val duplicatesRemovedCount = AtomicInteger(0)
    val totalSourcesCount = AtomicInteger(0)
    val itemsNeedingReviewCount = AtomicInteger(0)

    data class DeduplicationResult(
        val reciters: List<ReciterEntity>,
        val editions: List<EditionEntity>,
        val recordings: List<SurahRecordingEntity>,
        val sources: List<AudioSourceEntity>,
        val reviewItems: List<ReviewItemEntity>,
        val duplicatesRemoved: Int
    )

    data class RawAudioEntry(
        val reciterId: String,
        val reciterArabicName: String,
        val reciterEnglishName: String,
        val reciterCountry: String,
        val riwayah: String,
        val recitationType: String,
        val surahNumber: Int?,
        val surahName: String?,
        val source: String,
        val sourceIdentifier: String?,
        val filename: String,
        val title: String?,
        val url: String,
        val format: String,
        val qualityLevel: AudioQualityLevel,
        val bitrateKbps: Int?,
        val fileSize: Long?,
        val durationSeconds: Long?,
        val license: String?,
        val isAmbiguous: Boolean = false,
        val reviewReason: String? = null
    )

    fun processAndDeduplicate(entries: List<RawAudioEntry>): DeduplicationResult {
        val reciterMap = mutableMapOf<String, ReciterEntity>()
        val editionMap = mutableMapOf<String, EditionEntity>()
        val recordingMap = mutableMapOf<String, SurahRecordingEntity>()
        val sourcesByRecording = mutableMapOf<String, MutableMap<String, AudioSourceEntity>>()
        val seenUrls = mutableSetOf<String>()
        val reviewItemsList = mutableListOf<ReviewItemEntity>()
        val editionSurahNumbers = mutableMapOf<String, MutableSet<Int>>()

        for (entry in entries) {
            // Check if item was ambiguous or unresolvable
            if (entry.isAmbiguous || entry.surahNumber == null) {
                reviewItemsList.add(
                    ReviewItemEntity(
                        identifier = entry.sourceIdentifier ?: "unknown",
                        filename = entry.filename,
                        title = entry.title ?: "",
                        creator = entry.reciterArabicName,
                        reason = entry.reviewReason ?: "تعذر استخراج رقم السورة بدقة موثوقة"
                    )
                )
                itemsNeedingReviewCount.incrementAndGet()
                continue
            }

            // Check exact duplicate URL
            if (seenUrls.contains(entry.url)) {
                duplicatesRemovedCount.incrementAndGet()
                continue
            }
            seenUrls.add(entry.url)
            totalSourcesCount.incrementAndGet()

            // 1. Build Canonical Reciter
            reciterMap.getOrPut(entry.reciterId) {
                ReciterEntity(
                    id = entry.reciterId,
                    arabicName = entry.reciterArabicName,
                    englishName = entry.reciterEnglishName,
                    country = entry.reciterCountry,
                    defaultRiwayah = entry.riwayah
                )
            }

            // 2. Build Canonical Edition (Reciter + Style + Riwayah)
            val styleSlug = sanitizeSlug(entry.recitationType)
            val riwayahSlug = sanitizeSlug(entry.riwayah)
            val editionId = "${entry.reciterId}_${styleSlug}_${riwayahSlug}"

            val editionSurahs = editionSurahNumbers.getOrPut(editionId) { mutableSetOf() }
            editionSurahs.add(entry.surahNumber)

            editionMap.getOrPut(editionId) {
                val editionDisplayName = when (entry.recitationType) {
                    "مجوّد" -> "المصحف المجوّد (${entry.riwayah})"
                    "مرتّل" -> "المصحف المرتّل (${entry.riwayah})"
                    "المصحف المعلم" -> "المصحف المعلم (${entry.riwayah})"
                    "تلاوات خاشعة" -> "تلاوات خاشعة (${entry.riwayah})"
                    else -> "حفلات وتلاوات نادرة (${entry.riwayah})"
                }
                EditionEntity(
                    editionId = editionId,
                    reciterId = entry.reciterId,
                    reciterName = entry.reciterArabicName,
                    name = editionDisplayName,
                    riwayah = entry.riwayah,
                    recitationType = entry.recitationType,
                    isCompleteQuran = false,
                    surahCount = 0,
                    archiveIdentifier = entry.sourceIdentifier,
                    license = entry.license
                )
            }

            // 3. Build Surah Recording Key: reciterId_editionId_surahNumber
            val recordingId = "${editionId}_surah_${String.format("%03d", entry.surahNumber)}"
            val surahName = entry.surahName ?: QuranEditionValidator.getSurahName(entry.surahNumber)

            recordingMap.getOrPut(recordingId) {
                SurahRecordingEntity(
                    recordingId = recordingId,
                    reciterId = entry.reciterId,
                    reciterName = entry.reciterArabicName,
                    editionId = editionId,
                    riwayah = entry.riwayah,
                    recitationType = entry.recitationType,
                    surahNumber = entry.surahNumber,
                    surahName = surahName,
                    title = entry.title,
                    isCompleteQuran = false
                )
            }

            // 4. Attach Audio Source
            val recordingSources = sourcesByRecording.getOrPut(recordingId) { mutableMapOf() }
            recordingSources[entry.url] = AudioSourceEntity(
                sourceUrl = entry.url,
                recordingId = recordingId,
                sourceName = entry.source,
                sourceIdentifier = entry.sourceIdentifier,
                format = entry.format,
                qualityKey = entry.qualityLevel.key,
                bitrateKbps = entry.bitrateKbps,
                fileSize = entry.fileSize,
                durationSeconds = entry.durationSeconds,
                license = entry.license
            )
        }

        // Finalize editions surah count & complete Quran validation
        val finalizedEditions = editionMap.values.map { ed ->
            val surahs = editionSurahNumbers[ed.editionId] ?: emptySet()
            val isComplete = QuranEditionValidator.isCompleteQuran(surahs)
            ed.copy(
                isCompleteQuran = isComplete,
                surahCount = surahs.size
            )
        }

        // Finalize reciters total editions count
        val editionsByReciter = finalizedEditions.groupBy { it.reciterId }
        val finalizedReciters = reciterMap.values.map { rec ->
            val count = editionsByReciter[rec.id]?.size ?: 1
            rec.copy(totalEditionsCount = count)
        }

        val allSources = sourcesByRecording.values.flatMap { it.values }

        return DeduplicationResult(
            reciters = finalizedReciters,
            editions = finalizedEditions,
            recordings = recordingMap.values.toList(),
            sources = allSources,
            reviewItems = reviewItemsList,
            duplicatesRemoved = duplicatesRemovedCount.get()
        )
    }

    private fun sanitizeSlug(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]+"), "_").trim('_')
    }

    fun resetStats() {
        duplicatesRemovedCount.set(0)
        totalSourcesCount.set(0)
        itemsNeedingReviewCount.set(0)
    }
}
