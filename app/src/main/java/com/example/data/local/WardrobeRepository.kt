package com.example.data.local

import com.example.model.WardrobeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WardrobeRepository(private val wardrobeDao: WardrobeDao) {

    val savedWardrobes: Flow<List<WardrobeConfig>> = wardrobeDao.getAllSavedWardrobes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveWardrobe(config: WardrobeConfig): Long {
        val entity = WardrobeEntity.fromDomain(config)
        return wardrobeDao.insertWardrobe(entity)
    }

    suspend fun deleteWardrobe(idStr: String) {
        val rawId = idStr.removePrefix("saved_").toLongOrNull()
        if (rawId != null) {
            wardrobeDao.deleteWardrobeById(rawId)
        }
    }
}
