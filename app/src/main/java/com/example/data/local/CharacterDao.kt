package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character_profile WHERE id = 1 LIMIT 1")
    fun getCharacter(): Flow<CharacterEntity?>

    @Query("SELECT * FROM character_profile WHERE id = 1 LIMIT 1")
    suspend fun getCharacterSync(): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(character: CharacterEntity)

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("UPDATE character_profile SET strength = :strength, respect = :respect, money = :money, health = :health WHERE id = 1")
    suspend fun updateCoreStats(strength: Int, respect: Int, money: Long, health: Int)

    @Query("DELETE FROM character_profile")
    suspend fun deleteCharacter()
}
