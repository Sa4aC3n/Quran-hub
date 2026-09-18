package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ReciterEntity::class,
        EditionEntity::class,
        SurahRecordingEntity::class,
        AudioSourceEntity::class,
        ReviewItemEntity::class,
        SyncStatsEntity::class,
        BookmarkEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(QuranTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quran_audio_archive.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
