package com.example.data.provider

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "responseHeader") val responseHeader: ArchiveResponseHeader?,
    @Json(name = "response") val response: ArchiveResponseDocs?
)

@JsonClass(generateAdapter = true)
data class ArchiveResponseHeader(
    @Json(name = "status") val status: Int?,
    @Json(name = "QTime") val qTime: Int?
)

@JsonClass(generateAdapter = true)
data class ArchiveResponseDocs(
    @Json(name = "numFound") val numFound: Int?,
    @Json(name = "start") val start: Int?,
    @Json(name = "docs") val docs: List<ArchiveDoc>?
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String?,
    @Json(name = "creator") val creator: Any?, // Can be string or list of strings in Archive.org
    @Json(name = "description") val description: String?,
    @Json(name = "subject") val subject: Any?,
    @Json(name = "date") val date: String?,
    @Json(name = "mediatype") val mediatype: String?,
    @Json(name = "downloads") val downloads: Long?,
    @Json(name = "licenseurl") val licenseurl: String?
) {
    val creatorString: String?
        get() = when (creator) {
            is String -> creator
            is List<*> -> creator.filterIsInstance<String>().joinToString(", ")
            else -> null
        }

    val subjectString: String?
        get() = when (subject) {
            is String -> subject
            is List<*> -> subject.filterIsInstance<String>().joinToString(", ")
            else -> null
        }
}

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    @Json(name = "created") val created: Long?,
    @Json(name = "d1") val d1: String?,
    @Json(name = "d2") val d2: String?,
    @Json(name = "dir") val dir: String?,
    @Json(name = "files") val files: List<ArchiveFileMeta>?,
    @Json(name = "files_count") val filesCount: Int?,
    @Json(name = "item_size") val itemSize: Long?,
    @Json(name = "metadata") val metadata: ArchiveItemMeta?
)

@JsonClass(generateAdapter = true)
data class ArchiveItemMeta(
    @Json(name = "identifier") val identifier: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "creator") val creator: Any?,
    @Json(name = "description") val description: String?,
    @Json(name = "subject") val subject: Any?,
    @Json(name = "licenseurl") val licenseurl: String?,
    @Json(name = "rights") val rights: String?
)

@JsonClass(generateAdapter = true)
data class ArchiveFileMeta(
    @Json(name = "name") val name: String,
    @Json(name = "source") val source: String?,
    @Json(name = "format") val format: String?,
    @Json(name = "original") val original: String?,
    @Json(name = "size") val size: String?,
    @Json(name = "length") val length: String?,
    @Json(name = "bitrate") val bitrate: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "track") val track: String?,
    @Json(name = "creator") val creator: String?
)
