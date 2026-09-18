package com.example.data.provider

import com.example.data.dedup.AudioDeduplicationManager

interface AudioSourceProvider {
    val providerName: String

    suspend fun searchArchiveItems(
        query: String,
        page: Int = 1,
        rows: Int = 50
    ): List<ArchiveDoc>

    suspend fun fetchItemRecordings(
        doc: ArchiveDoc
    ): List<AudioDeduplicationManager.RawAudioEntry>
}
