package com.example.data.remote

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

data class Mp3QuranRecitersResponse(
    @SerializedName("reciters") val reciters: List<Mp3QuranReciterDto> = emptyList()
)

data class Mp3QuranReciterDto(
    @SerializedName(value = "id", alternate = ["reciter_id"]) val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("letter") val letter: String? = null,
    @SerializedName(value = "moshaf", alternate = ["moshafs"]) val moshafs: List<Mp3QuranMoshafDto> = emptyList()
)

data class Mp3QuranMoshafDto(
    @SerializedName(value = "id", alternate = ["moshaf_id"]) val id: Int = 0,
    @SerializedName(value = "name", alternate = ["rewaya_name"]) val name: String? = null,
    @SerializedName("server") val server: String = "",
    @SerializedName(value = "surah_total", alternate = ["surahs_count"]) val surahTotal: Int = 114,
    @SerializedName(value = "moshaf_type", alternate = ["rewaya_id"]) val moshafType: Int = 1,
    @SerializedName("surah_list")
    @JsonAdapter(SurahListDeserializer::class)
    val surahList: List<Int> = emptyList()
)

class SurahListDeserializer : JsonDeserializer<List<Int>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Int> {
        if (json == null || json.isJsonNull) return emptyList()
        return try {
            if (json.isJsonArray) {
                json.asJsonArray.mapNotNull {
                    try { it.asInt } catch (_: Exception) { null }
                }
            } else if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
                val str = json.asString
                str.split(",", " ", "-")
                    .mapNotNull { it.trim().toIntOrNull() }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

data class Mp3QuranSuwarResponse(
    @SerializedName("suwar") val suwar: List<Mp3QuranSurahDto> = emptyList()
)

data class Mp3QuranSurahDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("start_page") val startPage: Int = 1,
    @SerializedName("end_page") val endPage: Int = 1,
    @SerializedName(value = "makkia", alternate = ["is_makkia"]) val makkia: Any? = null,
    @SerializedName("type_label") val typeLabel: String? = null
) {
    val isMakkia: Boolean
        get() = when (makkia) {
            is Boolean -> makkia
            is Number -> makkia.toInt() == 1
            is String -> makkia == "1" || makkia.contains("مكية")
            else -> typeLabel?.contains("مكية") == true
        }
}

interface Mp3QuranApi {

    @GET("api/v3/reciters")
    suspend fun getReciters(
        @Query("language") language: String = "ar",
        @Query("reciter") reciterId: Int? = null
    ): Mp3QuranRecitersResponse

    @GET("api/v3/suwar")
    suspend fun getSuwar(
        @Query("language") language: String = "ar"
    ): Mp3QuranSuwarResponse

    companion object {
        private const val BASE_URL = "https://www.mp3quran.net/"

        fun create(): Mp3QuranApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            val gson = Gson()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(Mp3QuranApi::class.java)
        }
    }
}
