package com.example.model

enum class ReputationRank(
    val title: String,
    val minRespect: Int,
    val perksDescription: String
) {
    SHNYR("Шнырь / Шестерка", 0, "Новичок на побегушках. Никакого веса в криминале."),
    PATSAN("Ровный пацан", 150, "Свой среди уличной шпаны. Доступ к напёрсткам и мелким делам."),
    BYK("Боевой бык", 450, "Силовик бригады. Урон в драках +20, доступ к рэкету ларьков."),
    BRIGADIR("Бригадир", 1100, "Лидер боевого звена. Своя братва, доступ к стрелкам и авторынку."),
    SMOTRYASHIY("Смотрящий", 2500, "Держит район и районный общак. Доступ к налётам и казино."),
    AVTORITET("Авторитет", 5200, "Влиятельный босс мафии. Связи в мэрии, доступ к заводам."),
    VOR_V_ZAKONE("Вор в законе", 10000, "Высшая ступень воровской масти. Закон и кодекс для всего криминала.");

    companion object {
        fun fromRespect(respect: Int): ReputationRank {
            val ranks = entries.sortedByDescending { it.minRespect }
            return ranks.firstOrNull { respect >= it.minRespect } ?: SHNYR
        }
    }
}

enum class CrimeCategory(val title: String) {
    STREET("Уличные мутки"),
    RACKET("Рэкет и прессовка"),
    HEIST("Налёты и угоны"),
    SYNDICATE("Крупный синдикат")
}

data class CrimeItem(
    val id: String,
    val title: String,
    val description: String,
    val category: CrimeCategory,
    val requiredRank: ReputationRank,
    val minMoney: Long,
    val maxMoney: Long,
    val respectReward: Int,
    val heatPenalty: Int,
    val healthRisk: Int,
    val baseSuccessRate: Int,
    val requiredStrength: Int = 0,
    val energyCost: Int = 25,
    val hoursNeeded: Int = 3
)

enum class AssetCategory(val title: String) {
    WEAPON("Арсенал и стволы"),
    VEHICLE("Автопарк"),
    CLOTHING("Шмот и статус"),
    BUSINESS("Бизнесы и точки")
}

data class AssetItem(
    val id: String,
    val name: String,
    val description: String,
    val category: AssetCategory,
    val price: Long,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val respectBonus: Int = 0,
    val dailyIncome: Long = 0,
    val heatReductionDaily: Int = 0,
    val isPurchased: Boolean = false,
    val isEquipped: Boolean = false
)

data class CrewMember(
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val hireCost: Long,
    val dailySalary: Long,
    val combatBonus: Int = 0,
    val incomePercentBonus: Int = 0,
    val heatReductionBonus: Int = 0,
    val isHired: Boolean = false
)

data class District(
    val id: String,
    val name: String,
    val description: String,
    val controlPercent: Int,
    val dailyTribute: Long,
    val rivalGang: String
)

data class ShowdownState(
    val rivalName: String,
    val rivalTitle: String,
    val rivalMaxHp: Int,
    val rivalCurrentHp: Int,
    val rivalAttack: Int,
    val rivalDefense: Int = 5,
    val playerMaxHp: Int,
    val playerCurrentHp: Int,
    val playerShield: Int = 0,
    val firstAidKits: Int = 2,
    val ammoCount: Int = 5,
    val rageMeter: Int = 0,
    val rewardMoney: Long,
    val rewardRespect: Int,
    val round: Int = 1,
    val rivalIntent: String = "Готовит удар кастетом",
    val rivalAvatarEmoji: String = "👊",
    val lastActionFeedback: String = "",
    val wasPlayerHit: Boolean = false,
    val wasCriticalHit: Boolean = false,
    val battleLog: List<String> = emptyList(),
    val isFinished: Boolean = false,
    val isPlayerWon: Boolean = false,
    val districtId: String? = null
)

data class RivalBoss(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val avatarEmoji: String,
    val maxHp: Int,
    val attack: Int,
    val rewardMoney: Long,
    val rewardRespect: Int,
    val minRank: ReputationRank,
    val difficultyLabel: String
)

data class PrisonRiddle(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GameEventOption(
    val text: String,
    val outcomeDescription: String,
    val moneyDelta: Long = 0,
    val respectDelta: Int = 0,
    val heatDelta: Int = 0,
    val healthDelta: Int = 0
)

data class GameEvent(
    val id: String,
    val title: String,
    val description: String,
    val options: List<GameEventOption>
)

data class PlayerStats(
    val totalCrimes: Int = 0,
    val successfulCrimes: Int = 0,
    val showdownsWon: Int = 0,
    val totalEarned: Long = 0,
    val timesArrested: Int = 0,
    val daysSurvived: Int = 1,
    val casinoWins: Int = 0
)

data class GameHistoryEntry(
    val day: Int,
    val text: String,
    val isPositive: Boolean
)

data class CriminalSkill(
    val id: String,
    val name: String,
    val description: String,
    val level: Int = 0,
    val maxLevel: Int = 5,
    val costPerLevel: Int = 1
)

data class NaperstkiState(
    val isPlaying: Boolean = false,
    val betAmount: Long = 1000L,
    val winningCup: Int = 1,
    val chosenCup: Int? = null,
    val isRevealed: Boolean = false,
    val resultText: String? = null
)

data class DiceGameState(
    val isPlaying: Boolean = false,
    val betAmount: Long = 2000L,
    val dice1: Int = 1,
    val dice2: Int = 1,
    val isRolled: Boolean = false,
    val won: Boolean = false,
    val resultText: String? = null
)

data class CardItem(
    val name: String,
    val value: Int,
    val suit: String
)

data class OchkoGameState(
    val isPlaying: Boolean = false,
    val betAmount: Long = 1000L,
    val playerCards: List<CardItem> = emptyList(),
    val dealerCards: List<CardItem> = emptyList(),
    val playerScore: Int = 0,
    val dealerScore: Int = 0,
    val isFinished: Boolean = false,
    val resultMessage: String? = null,
    val isWin: Boolean = false
)

data class DailySummary(
    val day: Int,
    val headline: String,
    val businessIncome: Long,
    val districtTribute: Long,
    val crewSalaries: Long,
    val livingExpenses: Long,
    val netChange: Long,
    val heatDecay: Int,
    val healthRegen: Int,
    val specialEffect: String
)

enum class MessageSender {
    PLAYER, BOSS
}

data class BossChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val isAiGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class BossChatState(
    val boss: RivalBoss,
    val messages: List<BossChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val relationshipScore: Int = 0,
    val statusNote: String = "Напряжённый базар",
    val isAiConnected: Boolean = true,
    val error: String? = null,
    val patienceIrritation: Int = 0,
    val moodStatus: String = "Спокоен, крутит чётки",
    val moodEmoji: String = "🚬",
    val isAttacking: Boolean = false,
    val attackReason: String? = null
)

