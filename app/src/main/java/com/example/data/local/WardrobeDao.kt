package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WardrobeDao {
    @Query("SELECT * FROM saved_wardrobes ORDER BY timestamp DESC")
    fun getAllSavedWardrobes(): Flow<List<WardrobeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWardrobe(wardrobe: WardrobeEntity): Long

    @Query("DELETE FROM saved_wardrobes WHERE id = :id")
    suspend fun deleteWardrobeById(id: Long)

    @Query("DELETE FROM saved_wardrobes")
    suspend fun clearAll()
}
