package com.example.astronautstories.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.WeaponType

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
    @PrimaryKey val id: Int = 1,
    val selectedHeroId: String = HeroId.KOUROSH.id,
    val equippedWeaponId: String = WeaponType.STANDARD_BLASTER.id,
    val level: Int = 1,
    val xp: Int = 0,
    val skillPoints: Int = 1,
    val crystals: Int = 150,
    val unlockedStage: Int = 1,
    val totalScore: Long = 0L,
    val endlessBestScore: Long = 0L,
    val challengeBestScore: Long = 0L,
    val enemiesDefeated: Int = 0,
    val bossesDefeated: Int = 0,
    val totalShotsFired: Int = 0,
    val totalShotsHit: Int = 0,
    val totalPlayTimeSeconds: Long = 0L,
    val challengesWon: Int = 0,
    val medkitBoosters: Int = 0,
    val ammoBoosters: Int = 0,
    val shieldBoosters: Int = 0,
    val betrayalEndingChosen: Boolean = false
)

@Entity(tableName = "stage_records")
data class StageRecordEntity(
    @PrimaryKey val stageNumber: Int,
    val stars: Int = 0,
    val bestScore: Long = 0L,
    val completedCount: Int = 0,
    val bestTimeRemaining: Int = 0
)

@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val itemId: String,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "skill_levels")
data class SkillLevelEntity(
    @PrimaryKey val skillId: String,
    val level: Int = 0
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val progress: Int = 0,
    val unlocked: Boolean = false,
    val unlockedAt: Long = 0L
)
