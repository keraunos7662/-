package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing the main hero's profile and progress in Criminal Russia.
 * Core attributes required: Сила (Strength), Авторитет (Respect/Authority),
 * Деньги (Money), and Здоровье (Health).
 */
@Entity(tableName = "character_profile")
data class CharacterEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "nickname")
    val nickname: String = "Седой",

    // Основные характеристики героя по запросу:
    @ColumnInfo(name = "strength")
    val strength: Int = 25, // Сила

    @ColumnInfo(name = "respect")
    val respect: Int = 10, // Авторитет

    @ColumnInfo(name = "money")
    val money: Long = 5000L, // Деньги (₽)

    @ColumnInfo(name = "health")
    val health: Int = 100, // Здоровье

    // Суточная система и общак
    @ColumnInfo(name = "energy")
    val energy: Int = 100, // Бодрость / Энергия на день (0-100)

    @ColumnInfo(name = "safe_money")
    val safeMoney: Long = 0L, // Деньги в общаке

    // Очки навыков и прокачка понятий
    @ColumnInfo(name = "skill_points")
    val skillPoints: Int = 1,

    @ColumnInfo(name = "skill_boxing")
    val skillBoxing: Int = 0,

    @ColumnInfo(name = "skill_shooting")
    val skillShooting: Int = 0,

    @ColumnInfo(name = "skill_stealth")
    val skillStealth: Int = 0,

    @ColumnInfo(name = "skill_luck")
    val skillLuck: Int = 0,

    @ColumnInfo(name = "skill_toughness")
    val skillToughness: Int = 0,

    // Сопутствующий криминальный статус и прогресс
    @ColumnInfo(name = "police_heat")
    val policeHeat: Int = 5,

    @ColumnInfo(name = "influence")
    val influence: Int = 10,

    @ColumnInfo(name = "day")
    val day: Int = 1,

    @ColumnInfo(name = "is_jailed")
    val isJailed: Boolean = false,

    @ColumnInfo(name = "jail_days_left")
    val jailDaysLeft: Int = 0,

    @ColumnInfo(name = "rank_title")
    val rankTitle: String = "Шнырь / Шестерка",

    @ColumnInfo(name = "total_crimes")
    val totalCrimes: Int = 0,

    @ColumnInfo(name = "successful_crimes")
    val successfulCrimes: Int = 0,

    @ColumnInfo(name = "showdowns_won")
    val showdownsWon: Int = 0,

    @ColumnInfo(name = "total_earned")
    val totalEarned: Long = 5000L,

    @ColumnInfo(name = "times_arrested")
    val timesArrested: Int = 0,

    @ColumnInfo(name = "days_survived")
    val daysSurvived: Int = 1,

    @ColumnInfo(name = "casino_wins")
    val casinoWins: Int = 0
)
