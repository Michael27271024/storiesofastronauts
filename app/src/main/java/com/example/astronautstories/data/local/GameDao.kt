package com.example.astronautstories.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_progress WHERE id = 1")
    fun observeProgress(): Flow<PlayerProgressEntity?>

    @Query("SELECT * FROM player_progress WHERE id = 1")
    suspend fun getProgressOnce(): PlayerProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlayerProgressEntity)

    @Query("SELECT * FROM stage_records ORDER BY stageNumber ASC")
    fun observeStageRecords(): Flow<List<StageRecordEntity>>

    @Query("SELECT * FROM stage_records WHERE stageNumber = :stageNumber")
    suspend fun getStageRecord(stageNumber: Int): StageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStageRecord(record: StageRecordEntity)

    @Query("DELETE FROM stage_records")
    suspend fun clearStageRecords()

    @Query("SELECT * FROM owned_items")
    fun observeOwnedItems(): Flow<List<OwnedItemEntity>>

    @Query("SELECT * FROM owned_items")
    suspend fun getOwnedItemsOnce(): List<OwnedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnedItem(item: OwnedItemEntity)

    @Query("SELECT * FROM skill_levels")
    fun observeSkillLevels(): Flow<List<SkillLevelEntity>>

    @Query("SELECT * FROM skill_levels")
    suspend fun getSkillLevelsOnce(): List<SkillLevelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkillLevel(skill: SkillLevelEntity)

    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(items: List<AchievementEntity>)
}
