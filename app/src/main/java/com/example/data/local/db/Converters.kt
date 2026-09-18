package com.example.data.local.db

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class QuranTypeConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listStringType = Types.newParameterizedType(List::class.java, String::class.java)
    private val listStringAdapter = moshi.adapter<List<String>>(listStringType)

    private val listIntType = Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    private val listIntAdapter = moshi.adapter<List<Int>>(listIntType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return listStringAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            listStringAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return listIntAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            listIntAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
