package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WardrobeEntity::class], version = 1, exportSchema = false)
abstract class VisionSpaceDatabase : RoomDatabase() {
    abstract fun wardrobeDao(): WardrobeDao

    companion object {
        @Volatile
        private var INSTANCE: VisionSpaceDatabase? = null

        fun getDatabase(context: Context): VisionSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VisionSpaceDatabase::class.java,
                    "visionspace_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
