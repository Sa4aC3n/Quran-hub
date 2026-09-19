package com.example.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class TafseerItem(
    @SerializedName("id") val id: Int = 1,
    @SerializedName("name") val name: String = "",
    @SerializedName("language") val language: String = "ar",
    @SerializedName("author") val author: String = "",
    @SerializedName("book_name") val bookName: String = ""
)

data class AyahTafseerResponse(
    @SerializedName("tafseer_id") val tafseerId: Int = 1,
    @SerializedName("tafseer_name") val tafseerName: String = "",
    @SerializedName("ayah_url") val ayahUrl: String = "",
    @SerializedName("ayah_number") val ayahNumber: Int = 1,
    @SerializedName("surah_number") val surahNumber: Int = 1,
    @SerializedName("text") val text: String = "",
    @SerializedName("source_provider") val sourceProvider: String = "",
    @SerializedName("schema_version") val schemaVersion: Int = 2
)

interface TafseerApi {

    @GET("tafseer")
    suspend fun getTafseerList(): List<TafseerItem>

    @GET("tafseer/{tafseer_id}/{sura_number}/{ayah_number}")
    suspend fun getAyahTafseer(
        @Path("tafseer_id") tafseerId: Int,
        @Path("sura_number") surahNumber: Int,
        @Path("ayah_number") ayahNumber: Int
    ): AyahTafseerResponse

    @GET("tafseer/{tafseer_id}/{sura_number}/{ayah_from}/{ayah_to}")
    suspend fun getAyahsTafseerRange(
        @Path("tafseer_id") tafseerId: Int,
        @Path("sura_number") surahNumber: Int,
        @Path("ayah_from") ayahFrom: Int,
        @Path("ayah_to") ayahTo: Int
    ): List<AyahTafseerResponse>

    companion object {
        private const val BASE_URL = "https://api.quran-tafseer.com/"

        fun create(): TafseerApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val gson = Gson()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(TafseerApi::class.java)
        }
    }
}
