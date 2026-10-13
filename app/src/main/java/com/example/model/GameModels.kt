package com.example.model

enum class ReputationRank(
    val title: String,
    val minRespect: Int,
    val perksDescription: String
) {
    SHNYR("Шнырь / Шестерка", 0, "Новичок на побегушках. Никакого уважения."),
    PATSAN("Ровный пацан", 50, "Свой среди уличных пацанов. Доступ к напёрсткам и мелким кражам."),
    BYK("Боевой бык", 150, "Силовик бригады. Урон в драках +15, доступ к рэкету ларьков."),
    BRIGADIR("Бригадир", 350, "Лидер боевого звена. Своя братва, доступ к стрелкам и авторынку."),
    SMOTRYASHIY("Смотрящий", 750, "Держит район и районный общак. Доступ к крупным налётам и казино."),
    AVTORITET("Авторитет", 1300, "Влиятельный босс. Связи с мэрией и силовиками, доступ к заводам."),
    VOR_V_ZAKONE("Вор в законе", 2000, "Высшая ступень воровской масти. Слово — закон для всего криминала.");

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
    val requiredStrength: Int = 0
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
    var rivalCurrentHp: Int,
    val rivalAttack: Int,
    val playerMaxHp: Int,
    var playerCurrentHp: Int,
    val rewardMoney: Long,
    val rewardRespect: Int,
    val battleLog: MutableList<String> = mutableListOf(),
    var isFinished: Boolean = false,
    var isPlayerWon: Boolean = false
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
    val daysSurvived: Int = 1
)

data class GameHistoryEntry(
    val day: Int,
    val text: String,
    val isPositive: Boolean
)
