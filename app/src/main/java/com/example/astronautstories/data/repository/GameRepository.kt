package com.example.astronautstories.data.repository

import com.example.astronautstories.data.local.AchievementEntity
import com.example.astronautstories.data.local.GameDao
import com.example.astronautstories.data.local.OwnedItemEntity
import com.example.astronautstories.data.local.PlayerProgressEntity
import com.example.astronautstories.data.local.SkillLevelEntity
import com.example.astronautstories.data.local.StageRecordEntity
import com.example.astronautstories.domain.model.GameMode
import com.example.astronautstories.domain.model.HeroId
import com.example.astronautstories.domain.model.ShopCategory
import com.example.astronautstories.domain.model.ShopItemCatalog
import com.example.astronautstories.domain.model.SkillType
import com.example.astronautstories.domain.model.StoreAndAchievements
import com.example.astronautstories.domain.model.WeaponType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(
    private val dao: GameDao
) {
    val progressFlow: Flow<PlayerProgressEntity> = dao.observeProgress().map { it ?: PlayerProgressEntity() }
    val stageRecordsFlow: Flow<Map<Int, StageRecordEntity>> =
        dao.observeStageRecords().map { list -> list.associateBy { it.stageNumber } }
    val ownedItemIdsFlow: Flow<Set<String>> =
        dao.observeOwnedItems().map { list -> list.map { it.itemId }.toSet() + "weapon_blaster" }
    val skillLevelsFlow: Flow<Map<String, Int>> =
        dao.observeSkillLevels().map { list -> list.associate { it.skillId to it.level } }
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.observeAchievements()

    suspend fun ensureInitialized() {
        if (dao.getProgressOnce() == null) {
            dao.upsertProgress(PlayerProgressEntity())
        }
        if (dao.getOwnedItemsOnce().none { it.itemId == "weapon_blaster" }) {
            dao.insertOwnedItem(OwnedItemEntity("weapon_blaster"))
        }
        val existingSkills = dao.getSkillLevelsOnce().associateBy { it.skillId }
        SkillType.entries.forEach { skill ->
            if (!existingSkills.containsKey(skill.id)) {
                dao.upsertSkillLevel(SkillLevelEntity(skill.id, 0))
            }
        }
        val existingAch = dao.getAchievementsOnce().associateBy { it.achievementId }
        val missingAch = StoreAndAchievements.achievements.mapNotNull { catalog ->
            if (existingAch.containsKey(catalog.id)) null
            else AchievementEntity(catalog.id, 0, false, 0L)
        }
        if (missingAch.isNotEmpty()) {
            dao.upsertAchievements(missingAch)
        }
    }

    suspend fun selectHero(heroId: HeroId) {
        val current = dao.getProgressOnce() ?: PlayerProgressEntity()
        dao.upsertProgress(current.copy(selectedHeroId = heroId.id))
    }

    suspend fun equipWeapon(weaponType: WeaponType) {
        val current = dao.getProgressOnce() ?: PlayerProgressEntity()
        dao.upsertProgress(current.copy(equippedWeaponId = weaponType.id))
    }

    suspend fun purchaseOrEquipItem(item: ShopItemCatalog): Boolean {
        ensureInitialized()
        val progress = dao.getProgressOnce() ?: PlayerProgressEntity()
        val owned = dao.getOwnedItemsOnce().map { it.itemId }.toSet() + "weapon_blaster"

        if (item.category == ShopCategory.BOOSTERS) {
            if (progress.crystals < item.priceCrystals) return false
            val updated = when (item.id) {
                "booster_medkit" -> progress.copy(
                    crystals = progress.crystals - item.priceCrystals,
                    medkitBoosters = progress.medkitBoosters + 1
                )
                "booster_ammo" -> progress.copy(
                    crystals = progress.crystals - item.priceCrystals,
                    ammoBoosters = progress.ammoBoosters + 1
                )
                else -> progress.copy(
                    crystals = progress.crystals - item.priceCrystals,
                    shieldBoosters = progress.shieldBoosters + 1
                )
            }
            dao.upsertProgress(updated)
            return true
        }

        if (owned.contains(item.id)) {
            item.weaponType?.let { equipWeapon(it) }
            item.heroId?.let { selectHero(it) }
            return true
        }

        if (progress.crystals < item.priceCrystals) return false
        dao.insertOwnedItem(OwnedItemEntity(item.id))
        var updated = progress.copy(crystals = progress.crystals - item.priceCrystals)
        item.weaponType?.let { updated = updated.copy(equippedWeaponId = it.id) }
        item.heroId?.let { updated = updated.copy(selectedHeroId = it.id) }
        dao.upsertProgress(updated)
        evaluateAchievements()
        return true
    }

    suspend fun upgradeSkill(skill: SkillType): Boolean {
        ensureInitialized()
        val progress = dao.getProgressOnce() ?: PlayerProgressEntity()
        if (progress.skillPoints <= 0) return false
        val currentLevel = dao.getSkillLevelsOnce().firstOrNull { it.skillId == skill.id }?.level ?: 0
        if (currentLevel >= skill.maxLevel) return false
        dao.upsertSkillLevel(SkillLevelEntity(skill.id, currentLevel + 1))
        dao.upsertProgress(progress.copy(skillPoints = progress.skillPoints - 1))
        evaluateAchievements()
        return true
    }

    suspend fun consumeMissionBoosters(): Triple<Int, Int, Boolean> {
        val progress = dao.getProgressOnce() ?: return Triple(0, 0, false)
        val med = if (progress.medkitBoosters > 0) 2 else 0
        val ammo = if (progress.ammoBoosters > 0) 40 else 0
        val shield = progress.shieldBoosters > 0
        if (med > 0 || ammo > 0 || shield) {
            dao.upsertProgress(
                progress.copy(
                    medkitBoosters = (progress.medkitBoosters - if (med > 0) 1 else 0).coerceAtLeast(0),
                    ammoBoosters = (progress.ammoBoosters - if (ammo > 0) 1 else 0).coerceAtLeast(0),
                    shieldBoosters = (progress.shieldBoosters - if (shield) 1 else 0).coerceAtLeast(0)
                )
            )
        }
        return Triple(med, ammo, shield)
    }

    suspend fun recordMatchResult(
        mode: GameMode,
        stageNumber: Int,
        won: Boolean,
        stars: Int,
        score: Long,
        xpEarned: Int,
        crystalsEarned: Int,
        enemiesKilled: Int,
        bossKilled: Boolean,
        shotsFired: Int,
        shotsHit: Int,
        durationSeconds: Long,
        timeRemaining: Int,
        betrayalChosen: Boolean,
        playerDisplayName: String
    ) {
        ensureInitialized()
        val prev = dao.getProgressOnce() ?: PlayerProgressEntity()

        var newXp = prev.xp + xpEarned
        var newLevel = prev.level
        var newSkillPoints = prev.skillPoints
        while (newXp >= xpThresholdForLevel(newLevel)) {
            newXp -= xpThresholdForLevel(newLevel)
            newLevel++
            newSkillPoints++
        }

        val newUnlocked = if (mode == GameMode.STORY && won) {
            maxOf(prev.unlockedStage, (stageNumber + 1).coerceAtMost(100))
        } else prev.unlockedStage

        val updated = prev.copy(
            level = newLevel,
            xp = newXp,
            skillPoints = newSkillPoints,
            crystals = prev.crystals + crystalsEarned,
            unlockedStage = newUnlocked,
            totalScore = prev.totalScore + score,
            endlessBestScore = if (mode == GameMode.ENDLESS) maxOf(prev.endlessBestScore, score) else prev.endlessBestScore,
            challengeBestScore = if (mode == GameMode.CHALLENGE) maxOf(prev.challengeBestScore, score) else prev.challengeBestScore,
            enemiesDefeated = prev.enemiesDefeated + enemiesKilled,
            bossesDefeated = prev.bossesDefeated + if (bossKilled) 1 else 0,
            totalShotsFired = prev.totalShotsFired + shotsFired,
            totalShotsHit = prev.totalShotsHit + shotsHit,
            totalPlayTimeSeconds = prev.totalPlayTimeSeconds + durationSeconds,
            challengesWon = prev.challengesWon + if (mode == GameMode.CHALLENGE && won) 1 else 0,
            betrayalEndingChosen = prev.betrayalEndingChosen || betrayalChosen
        )
        dao.upsertProgress(updated)

        if (mode == GameMode.STORY && won) {
            val prevRecord = dao.getStageRecord(stageNumber)
            dao.upsertStageRecord(
                StageRecordEntity(
                    stageNumber = stageNumber,
                    stars = maxOf(prevRecord?.stars ?: 0, stars.coerceIn(1, 3)),
                    bestScore = maxOf(prevRecord?.bestScore ?: 0L, score),
                    completedCount = (prevRecord?.completedCount ?: 0) + 1,
                    bestTimeRemaining = maxOf(prevRecord?.bestTimeRemaining ?: 0, timeRemaining)
                )
            )
        }

        evaluateAchievements()
    }

    suspend fun resetStoryProgress() {
        val prev = dao.getProgressOnce() ?: PlayerProgressEntity()
        dao.upsertProgress(prev.copy(unlockedStage = 1))
        dao.clearStageRecords()
    }

    private suspend fun evaluateAchievements() {
        val progress = dao.getProgressOnce() ?: return
        val owned = dao.getOwnedItemsOnce().map { it.itemId }.toSet()
        val skills = dao.getSkillLevelsOnce().sumOf { it.level }
        val completedStages = (progress.unlockedStage - 1).coerceAtLeast(0)
        val existing = dao.getAchievementsOnce().associateBy { it.achievementId }

        var bonusCrystals = 0
        val updatedEntities = StoreAndAchievements.achievements.map { cat ->
            val currentVal = when (cat.id) {
                "first_launch" -> if (completedStages >= 1 || progress.totalScore > 0) 1 else 0
                "moon_survivor" -> completedStages
                "mars_raider" -> progress.unlockedStage
                "saturn_scientist" -> progress.unlockedStage
                "mercury_shadow" -> progress.unlockedStage
                "earth_hero" -> completedStages
                "alien_hunter_25", "alien_hunter_150", "alien_hunter_500" -> progress.enemiesDefeated
                "boss_slayer" -> progress.bossesDefeated
                "endless_1000", "endless_5000" -> progress.endlessBestScore.toInt()
                "challenge_winner" -> progress.challengesWon
                "armory_collector" -> owned.count { it.startsWith("weapon_") && it != "weapon_blaster" }
                "skill_master" -> skills
                "squad_leader" -> owned.count { it.startsWith("hero_") }
                else -> 0
            }
            val prevEntity = existing[cat.id]
            val wasUnlocked = prevEntity?.unlocked == true
            val nowUnlocked = wasUnlocked || currentVal >= cat.targetCount
            if (!wasUnlocked && nowUnlocked) {
                bonusCrystals += cat.rewardCrystals
            }
            AchievementEntity(
                achievementId = cat.id,
                progress = currentVal.coerceAtMost(cat.targetCount),
                unlocked = nowUnlocked,
                unlockedAt = if (wasUnlocked) prevEntity!!.unlockedAt else if (nowUnlocked) System.currentTimeMillis() else 0L
            )
        }
        dao.upsertAchievements(updatedEntities)
        if (bonusCrystals > 0) {
            dao.upsertProgress(progress.copy(crystals = progress.crystals + bonusCrystals))
        }
    }

    fun xpThresholdForLevel(level: Int): Int = 150 + (level - 1) * 90
}
