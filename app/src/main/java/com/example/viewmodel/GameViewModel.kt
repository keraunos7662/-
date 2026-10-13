package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.SoundType
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameUiState(
    val nickname: String = "Седой",
    val money: Long = 5000L,
    val respect: Int = 10,
    val health: Int = 100,
    val policeHeat: Int = 5,
    val strength: Int = 25,
    val influence: Int = 10,
    val day: Int = 1,
    val isJailed: Boolean = false,
    val jailDaysLeft: Int = 0,
    val rank: ReputationRank = ReputationRank.SHNYR,

    // Lists
    val crimes: List<CrimeItem> = emptyList(),
    val assets: List<AssetItem> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    val districts: List<District> = emptyList(),
    val history: List<GameHistoryEntry> = emptyList(),
    val stats: PlayerStats = PlayerStats(),

    // Sub-screens & Dialogs
    val activeShowdown: ShowdownState? = null,
    val activeEvent: GameEvent? = null,
    val currentRiddle: PrisonRiddle? = null,
    val riddleFeedback: String? = null,
    val showNewGameDialog: Boolean = false,
    val alertMessage: String? = null
)

sealed interface ShowdownAction {
    data object BAZAR : ShowdownAction // Speech / negotiation
    data object PUNCH : ShowdownAction // Physical melee
    data object SHOOT : ShowdownAction // Weapon shot
    data object CALL_CREW : ShowdownAction // Gang backup
    data object FLEE : ShowdownAction // Retreat
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadOrInitGame()
        }
    }

    private suspend fun loadOrInitGame() {
        if (repository.hasSavedGame()) {
            val saved = repository.loadGameState()
            val allAssets = repository.getDefaultAssets().map { asset ->
                asset.copy(
                    isPurchased = saved.purchasedAssetIds.contains(asset.id),
                    isEquipped = saved.equippedAssetIds.contains(asset.id)
                )
            }
            val allCrew = repository.getDefaultCrew().map { c ->
                c.copy(isHired = saved.hiredCrewIds.contains(c.id))
            }
            val allDistricts = repository.getDefaultDistricts().map { d ->
                val control = saved.districtControl[d.id] ?: d.controlPercent
                d.copy(controlPercent = control)
            }

            _uiState.update {
                it.copy(
                    nickname = saved.nickname,
                    money = saved.money,
                    respect = saved.respect,
                    health = saved.health,
                    policeHeat = saved.policeHeat,
                    strength = saved.strength,
                    influence = saved.influence,
                    day = saved.day,
                    isJailed = saved.isJailed,
                    jailDaysLeft = saved.jailDaysLeft,
                    rank = ReputationRank.fromRespect(saved.respect),
                    crimes = repository.getDefaultCrimes(),
                    assets = allAssets,
                    crew = allCrew,
                    districts = allDistricts,
                    stats = saved.stats,
                    history = saved.history
                )
            }
        } else {
            initFreshGame("Седой")
        }
    }

    fun startNewGame(nickname: String, perk: String) {
        viewModelScope.launch {
            val initialNick = nickname.trim().ifEmpty { "Седой" }
            repository.clearSavedGame()

            val baseMoney: Long
            var baseRespect = 10
            var baseStrength = 25
            var baseInfluence = 10

            when (perk) {
                "Боксёр" -> {
                    baseStrength += 20
                    baseMoney = 3000
                }
                "Связи в органах" -> {
                    baseInfluence += 25
                    baseMoney = 4000
                }
                "Братва с района" -> {
                    baseRespect += 30
                    baseMoney = 2500
                }
                else -> { // "Деньги от цеховика"
                    baseMoney = 15000
                }
            }

            val allAssets = repository.getDefaultAssets()
            val allCrew = repository.getDefaultCrew()
            val allDistricts = repository.getDefaultDistricts()

            _uiState.update {
                it.copy(
                    nickname = initialNick,
                    money = baseMoney,
                    respect = baseRespect,
                    health = 100,
                    policeHeat = 5,
                    strength = baseStrength,
                    influence = baseInfluence,
                    day = 1,
                    isJailed = false,
                    jailDaysLeft = 0,
                    rank = ReputationRank.fromRespect(baseRespect),
                    crimes = repository.getDefaultCrimes(),
                    assets = allAssets,
                    crew = allCrew,
                    districts = allDistricts,
                    stats = PlayerStats(daysSurvived = 1, totalEarned = baseMoney),
                    history = listOf(
                        GameHistoryEntry(1, "Начало криминального пути: $initialNick делает первые шаги в городе.", true)
                    ),
                    activeShowdown = null,
                    activeEvent = null,
                    currentRiddle = null,
                    riddleFeedback = null,
                    showNewGameDialog = false,
                    alertMessage = "Добро пожаловать в игру, $initialNick! Твой путь в криминальной России начался."
                )
            }
            persistState()
        }
    }

    private suspend fun initFreshGame(nickname: String) {
        startNewGame(nickname, "Деньги от цеховика")
    }

    fun openNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = true) }
    }

    fun dismissNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = false) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
    }

    // CRIME EXECUTION
    fun executeCrime(crime: CrimeItem) {
        val state = _uiState.value
        if (state.isJailed) {
            _uiState.update { it.copy(alertMessage = "Ты на тюремных нарах! В СИЗО дела на воле не крутят.") }
            return
        }
        if (state.health < 20) {
            _uiState.update { it.copy(alertMessage = "Ты еле на ногах стоишь! Поправь здоровье в больничке.") }
            return
        }

        // Calculate chance of success
        val equippedBonus = state.assets.filter { it.isEquipped }.sumOf { it.attackBonus }
        val crewIncomeBonus = state.crew.filter { it.isHired }.sumOf { it.incomePercentBonus }
        val effectiveStrength = state.strength + equippedBonus

        val strengthDiff = (effectiveStrength - crime.requiredStrength).coerceIn(-20, 20)
        val successChance = (crime.baseSuccessRate + strengthDiff / 2).coerceIn(15, 95)

        val roll = Random.nextInt(100)
        val isSuccess = roll < successChance

        if (isSuccess) {
            // Success
            val rawMoney = Random.nextLong(crime.minMoney, crime.maxMoney + 1)
            val bonusMoney = (rawMoney * crewIncomeBonus) / 100
            val totalReward = rawMoney + bonusMoney

            val newMoney = state.money + totalReward
            val newRespect = state.respect + crime.respectReward
            val newHeat = (state.policeHeat + crime.heatPenalty).coerceIn(0, 100)
            val newRank = ReputationRank.fromRespect(newRespect)

            val newStats = state.stats.copy(
                totalCrimes = state.stats.totalCrimes + 1,
                successfulCrimes = state.stats.successfulCrimes + 1,
                totalEarned = state.stats.totalEarned + totalReward
            )

            val logEntry = GameHistoryEntry(
                state.day,
                "Успешное дело: «${crime.title}». Куш: +${formatMoney(totalReward)} ₽, уважение +${crime.respectReward}.",
                true
            )

            repository.playSound(SoundType.CASH)

            _uiState.update {
                it.copy(
                    money = newMoney,
                    respect = newRespect,
                    policeHeat = newHeat,
                    rank = newRank,
                    stats = newStats,
                    history = it.history + logEntry,
                    alertMessage = "Дело выгорело! Поднял ${formatMoney(totalReward)} ₽. Пацаны уважают!"
                )
            }

            // Check if heat triggered arrest
            checkPoliceIntervention(newHeat)
        } else {
            // Failure
            val damage = crime.healthRisk + Random.nextInt(5, 15)
            val newHealth = (state.health - damage).coerceAtLeast(1)
            val newHeat = (state.policeHeat + crime.heatPenalty + 10).coerceIn(0, 100)
            val respectLoss = (crime.respectReward / 2).coerceAtLeast(1)
            val newRespect = (state.respect - respectLoss).coerceAtLeast(0)

            val newStats = state.stats.copy(
                totalCrimes = state.stats.totalCrimes + 1
            )

            val logEntry = GameHistoryEntry(
                state.day,
                "Провал дела «${crime.title}»! Получил по шее (-$damage HP), хвост ментов вырос.",
                false
            )

            repository.playSound(SoundType.PUNCH)

            _uiState.update {
                it.copy(
                    health = newHealth,
                    respect = newRespect,
                    policeHeat = newHeat,
                    stats = newStats,
                    history = it.history + logEntry,
                    alertMessage = "Облом! Всё пошло не по плану. Получил травму (-$damage HP) и внимание милиции!"
                )
            }

            checkPoliceIntervention(newHeat)
        }
        persistState()
    }

    private fun checkPoliceIntervention(heat: Int) {
        if (heat >= 95) {
            // Arrest!
            arrestPlayer("Облава РУОП! За тобой выехал спецназ из-за запредельного розыска.")
        } else if (heat >= 75 && Random.nextInt(100) < 40) {
            repository.playSound(SoundType.SIREN)
            triggerPoliceShowdown()
        }
    }

    // CONFRONTATION / SHOWDOWN
    fun startShowdown(rivalName: String, rivalTitle: String, maxHp: Int, attack: Int, rewardMoney: Long, rewardRespect: Int) {
        val playerMaxHp = _uiState.value.health
        val showdown = ShowdownState(
            rivalName = rivalName,
            rivalTitle = rivalTitle,
            rivalMaxHp = maxHp,
            rivalCurrentHp = maxHp,
            rivalAttack = attack,
            playerMaxHp = playerMaxHp,
            playerCurrentHp = playerMaxHp,
            rewardMoney = rewardMoney,
            rewardRespect = rewardRespect,
            battleLog = mutableListOf("Стрелка началась! Перед тобой $rivalName ($rivalTitle). Обстановка накалена до предела.")
        )
        _uiState.update { it.copy(activeShowdown = showdown) }
    }

    fun triggerDistrictShowdown(district: District) {
        val difficultyMultiplier = when (district.id) {
            "dist_zavod" -> 1
            "dist_vokzal" -> 2
            "dist_center" -> 3
            "dist_port" -> 4
            else -> 5
        }
        startShowdown(
            rivalName = "Бригада «${district.rivalGang}»",
            rivalTitle = "Смотрящие за ${district.name}",
            maxHp = 60 + difficultyMultiplier * 25,
            attack = 12 + difficultyMultiplier * 6,
            rewardMoney = district.dailyTribute * 4,
            rewardRespect = 40 + difficultyMultiplier * 20
        )
    }

    private fun triggerPoliceShowdown() {
        startShowdown(
            rivalName = "Опергруппа УгРо и ОМОН",
            rivalTitle = "Милицейская засада",
            maxHp = 90,
            attack = 20,
            rewardMoney = 15000,
            rewardRespect = 50
        )
    }

    fun performShowdownAction(action: ShowdownAction) {
        val currentShowdown = _uiState.value.activeShowdown ?: return
        if (currentShowdown.isFinished) return

        val state = _uiState.value
        val equippedAttack = state.assets.filter { it.isEquipped }.sumOf { it.attackBonus }
        val equippedDefense = state.assets.filter { it.isEquipped }.sumOf { it.defenseBonus }
        val crewCombat = state.crew.filter { it.isHired }.sumOf { it.combatBonus }

        when (action) {
            ShowdownAction.BAZAR -> {
                // Speech check based on Influence & Respect
                val speechPower = (state.influence * 2 + state.respect / 10).coerceAtLeast(15)
                val success = Random.nextInt(100) < speechPower
                if (success) {
                    val damage = Random.nextInt(25, 45)
                    currentShowdown.rivalCurrentHp -= damage
                    currentShowdown.battleLog.add(
                        "Ты раскидал всё по понятиям! Противник растерялся и сдал назад (-$damage морали)."
                    )
                    repository.playSound(SoundType.CLICK)
                } else {
                    currentShowdown.battleLog.add(
                        "Оппоненты не стали слушать: «Фильтруй хрюкало, фраер!» Базар сорвался."
                    )
                }
            }
            ShowdownAction.PUNCH -> {
                val damage = (15 + state.strength / 3 + Random.nextInt(5, 12))
                currentShowdown.rivalCurrentHp -= damage
                currentShowdown.battleLog.add("Ты пробил сокрушительную двойку в челюсть! (-$damage HP)")
                repository.playSound(SoundType.PUNCH)
            }
            ShowdownAction.SHOOT -> {
                val hasFirearm = state.assets.any { it.isEquipped && it.category == AssetCategory.WEAPON && it.attackBonus > 20 }
                val damage = if (hasFirearm) {
                    (35 + equippedAttack / 2 + Random.nextInt(15, 30))
                } else {
                    (20 + equippedAttack + Random.nextInt(5, 15))
                }
                currentShowdown.rivalCurrentHp -= damage
                if (hasFirearm) {
                    currentShowdown.battleLog.add("Выстрел из волыны! Грохот выстрела оглушил район (-$damage HP оппонента).")
                    repository.playSound(SoundType.SHOT)
                } else {
                    currentShowdown.battleLog.add("Удар тяжелым оружием! (-$damage HP)")
                    repository.playSound(SoundType.PUNCH)
                }
            }
            ShowdownAction.CALL_CREW -> {
                if (crewCombat > 0) {
                    val damage = crewCombat + Random.nextInt(10, 25)
                    currentShowdown.rivalCurrentHp -= damage
                    currentShowdown.battleLog.add("Твоя братва навалилась толпой с арматурой! (-$damage HP врагов).")
                    repository.playSound(SoundType.PUNCH)
                } else {
                    currentShowdown.battleLog.add("У тебя нет нанятых бойцов в бригаде! Пришлось отбиваться в одиночку.")
                }
            }
            ShowdownAction.FLEE -> {
                val hasCar = state.assets.any { it.isEquipped && it.category == AssetCategory.VEHICLE }
                val fleeChance = if (hasCar) 75 else 40
                if (Random.nextInt(100) < fleeChance) {
                    currentShowdown.isFinished = true
                    currentShowdown.isPlayerWon = false
                    currentShowdown.battleLog.add("Ты дал по газам и оторвался от преследования!")
                    _uiState.update { it.copy(alertMessage = "Ты успешно оторвался от противников на стрелке.") }
                    persistState()
                    return
                } else {
                    currentShowdown.battleLog.add("Сбежать не удалось! Тебя зажали в угол.")
                }
            }
        }

        // Check if rival is defeated
        if (currentShowdown.rivalCurrentHp <= 0) {
            currentShowdown.rivalCurrentHp = 0
            currentShowdown.isFinished = true
            currentShowdown.isPlayerWon = true
            currentShowdown.battleLog.add("ПОБЕДА! Враг повержен и молит о пощаде!")
            repository.playSound(SoundType.VICTORY)

            // Grant rewards
            val newMoney = state.money + currentShowdown.rewardMoney
            val newRespect = state.respect + currentShowdown.rewardRespect
            val newStats = state.stats.copy(
                showdownsWon = state.stats.showdownsWon + 1,
                totalEarned = state.stats.totalEarned + currentShowdown.rewardMoney
            )
            val log = GameHistoryEntry(
                state.day,
                "Победа на стрелке против «${currentShowdown.rivalName}»! Трофеи: +${formatMoney(currentShowdown.rewardMoney)} ₽, уважение +${currentShowdown.rewardRespect}.",
                true
            )

            _uiState.update {
                it.copy(
                    money = newMoney,
                    respect = newRespect,
                    rank = ReputationRank.fromRespect(newRespect),
                    stats = newStats,
                    history = it.history + log
                )
            }
            persistState()
            return
        }

        // Rival attacks back
        val rawAttack = currentShowdown.rivalAttack + Random.nextInt(0, 10)
        val mitigation = (equippedDefense / 4).coerceAtMost(rawAttack - 3)
        val actualDamage = (rawAttack - mitigation).coerceAtLeast(3)
        currentShowdown.playerCurrentHp -= actualDamage
        currentShowdown.battleLog.add("${currentShowdown.rivalName} наносит ответный удар! (-$actualDamage HP).")

        if (currentShowdown.playerCurrentHp <= 0) {
            currentShowdown.playerCurrentHp = 0
            currentShowdown.isFinished = true
            currentShowdown.isPlayerWon = false
            currentShowdown.battleLog.add("Поражение! Ты потерял сознание и очнулся в больнице...")

            val hospitalBill = (state.money / 4).coerceAtLeast(1000)
            val newMoney = (state.money - hospitalBill).coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    health = 35,
                    money = newMoney,
                    history = it.history + GameHistoryEntry(
                        state.day,
                        "Поражение на стрелке против ${currentShowdown.rivalName}. Очнулся в больнице (-${formatMoney(hospitalBill)} ₽).",
                        false
                    )
                )
            }
        } else {
            // Update current HP in state
            _uiState.update { it.copy(health = currentShowdown.playerCurrentHp) }
        }

        persistState()
    }

    fun closeShowdown() {
        _uiState.update { it.copy(activeShowdown = null) }
    }

    // PRISON & RIDDLES
    fun arrestPlayer(reason: String) {
        repository.playSound(SoundType.SIREN)
        val riddles = repository.getPrisonRiddles()
        val randomRiddle = riddles.random()

        _uiState.update {
            it.copy(
                isJailed = true,
                jailDaysLeft = 3,
                policeHeat = 15,
                currentRiddle = randomRiddle,
                riddleFeedback = null,
                activeShowdown = null,
                stats = it.stats.copy(timesArrested = it.stats.timesArrested + 1),
                history = it.history + GameHistoryEntry(it.day, "Арест! $reason Отправлен в СИЗО.", false),
                alertMessage = "Твой розыск зашкалил! Милиция повязала тебя. Ты в тюремной камере."
            )
        }
        persistState()
    }

    fun answerPrisonRiddle(optionIndex: Int) {
        val riddle = _uiState.value.currentRiddle ?: return
        if (optionIndex == riddle.correctIndex) {
            repository.playSound(SoundType.VICTORY)
            val newRespect = _uiState.value.respect + 35
            val daysLeft = (_uiState.value.jailDaysLeft - 1).coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    respect = newRespect,
                    rank = ReputationRank.fromRespect(newRespect),
                    jailDaysLeft = daysLeft,
                    riddleFeedback = "Верно! ${riddle.explanation} Арестанты оценили твою масть (+35 авторитета)!",
                    currentRiddle = null
                )
            }
            if (daysLeft == 0) {
                releaseFromJail("Братва подогрела лагерь и тебя выпустили досрочно по воровскому закону!")
            }
        } else {
            repository.playSound(SoundType.PUNCH)
            val newRespect = (_uiState.value.respect - 20).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    respect = newRespect,
                    rank = ReputationRank.fromRespect(newRespect),
                    riddleFeedback = "Неправильный ответ! ${riddle.explanation} Уважение на нарах упало (-20 авторитета).",
                    currentRiddle = null
                )
            }
        }
        persistState()
    }

    fun bribePrisonWarden() {
        val state = _uiState.value
        val bribeCost = 35000L
        if (state.money < bribeCost) {
            _uiState.update { it.copy(alertMessage = "У тебя нет $bribeCost ₽ для взятки начальнику тюрьмы!") }
            return
        }

        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money - bribeCost,
                alertMessage = "Начальник СИЗО взял конверт и выпустил тебя через чёрный ход."
            )
        }
        releaseFromJail("Вышел за взятку начальнику СИЗО (-35 000 ₽).")
    }

    fun hireLawyerInJail() {
        val state = _uiState.value
        val hasLawyer = state.crew.any { it.id == "crew_advokat" && it.isHired }
        if (hasLawyer) {
            releaseFromJail("Твой адвокат Генрих Львович развалил уголовное дело из-за процессуальных нарушений!")
        } else {
            val cost = 20000L
            if (state.money < cost) {
                _uiState.update { it.copy(alertMessage = "На адвоката нужно 20 000 ₽!") }
                return
            }
            _uiState.update { it.copy(money = it.money - cost) }
            releaseFromJail("Дежурный адвокат добился подписки о невыезде (-20 000 ₽).")
        }
    }

    private fun releaseFromJail(reason: String) {
        _uiState.update {
            it.copy(
                isJailed = false,
                jailDaysLeft = 0,
                currentRiddle = null,
                riddleFeedback = null,
                history = it.history + GameHistoryEntry(it.day, "Освобождение из СИЗО: $reason", true),
                alertMessage = "Свобода! $reason"
            )
        }
        persistState()
    }

    // ASSETS & PURCHASES
    fun buyAsset(asset: AssetItem) {
        val state = _uiState.value
        if (state.money < asset.price) {
            _uiState.update { it.copy(alertMessage = "Не хватает денег! Нужно ${formatMoney(asset.price)} ₽.") }
            return
        }

        repository.playSound(SoundType.CASH)
        val newMoney = state.money - asset.price
        val newRespect = state.respect + asset.respectBonus

        val updatedAssets = state.assets.map {
            if (it.id == asset.id) it.copy(isPurchased = true, isEquipped = true) else it
        }

        val log = GameHistoryEntry(
            state.day,
            "Приобретение: «${asset.name}» за ${formatMoney(asset.price)} ₽ (+${asset.respectBonus} уважения).",
            true
        )

        _uiState.update {
            it.copy(
                money = newMoney,
                respect = newRespect,
                rank = ReputationRank.fromRespect(newRespect),
                assets = updatedAssets,
                history = it.history + log,
                alertMessage = "Куплено: «${asset.name}»!"
            )
        }
        persistState()
    }

    fun toggleEquipAsset(asset: AssetItem) {
        repository.playSound(SoundType.CLICK)
        val updatedAssets = _uiState.value.assets.map {
            if (it.id == asset.id) {
                it.copy(isEquipped = !it.isEquipped)
            } else if (it.category == asset.category && asset.category != AssetCategory.BUSINESS) {
                // Single equip per equipment slot (one car, one outfit, etc.)
                it.copy(isEquipped = false)
            } else {
                it
            }
        }
        _uiState.update { it.copy(assets = updatedAssets) }
        persistState()
    }

    // CREW MANAGEMENT
    fun hireCrew(crew: CrewMember) {
        val state = _uiState.value
        if (state.money < crew.hireCost) {
            _uiState.update { it.copy(alertMessage = "Не хватает средств на подъёмные! Нужно ${formatMoney(crew.hireCost)} ₽.") }
            return
        }

        repository.playSound(SoundType.CASH)
        val newMoney = state.money - crew.hireCost
        val updatedCrew = state.crew.map {
            if (it.id == crew.id) it.copy(isHired = true) else it
        }

        val log = GameHistoryEntry(
            state.day,
            "В бригаду принят ${crew.name} (${crew.role}). Выплачены подъёмные: ${formatMoney(crew.hireCost)} ₽.",
            true
        )

        _uiState.update {
            it.copy(
                money = newMoney,
                crew = updatedCrew,
                history = it.history + log,
                alertMessage = "В бригаду вступил: ${crew.name}!"
            )
        }
        persistState()
    }

    fun fireCrew(crew: CrewMember) {
        repository.playSound(SoundType.CLICK)
        val updatedCrew = _uiState.value.crew.map {
            if (it.id == crew.id) it.copy(isHired = false) else it
        }
        _uiState.update {
            it.copy(
                crew = updatedCrew,
                alertMessage = "${crew.name} покинул твою бригаду."
            )
        }
        persistState()
    }

    // DISTRICTS & TERRITORY
    fun expandDistrictControl(district: District) {
        val state = _uiState.value
        val expansionCost = (district.dailyTribute * 1.5).toLong()
        if (state.money < expansionCost) {
            _uiState.update { it.copy(alertMessage = "На расширение влияния нужно ${formatMoney(expansionCost)} ₽ на братву.") }
            return
        }

        triggerDistrictShowdown(district)
    }

    // DAY TRANSITION & PASSING TIME
    fun passDay() {
        val state = _uiState.value
        repository.playSound(SoundType.CLICK)

        val nextDay = state.day + 1
        var dayMoney = state.money
        var dayRespect = state.respect
        var dayHeat = state.policeHeat
        var dayHealth = state.health

        // 1. Business passive income
        val businessIncome = state.assets.filter { it.isPurchased && it.category == AssetCategory.BUSINESS }
            .sumOf { it.dailyIncome }

        // 2. District tributes
        val districtIncome = state.districts.sumOf { (it.dailyTribute * it.controlPercent) / 100 }

        val totalDailyGain = businessIncome + districtIncome
        dayMoney += totalDailyGain

        // 3. Crew salaries
        val totalSalaries = state.crew.filter { it.isHired }.sumOf { it.dailySalary }
        dayMoney -= totalSalaries

        // 4. Heat reduction
        val equippedHeatRed = state.assets.filter { it.isEquipped }.sumOf { it.heatReductionDaily }
        val crewHeatRed = state.crew.filter { it.isHired }.sumOf { it.heatReductionBonus }
        dayHeat = (dayHeat - 4 - equippedHeatRed - crewHeatRed).coerceAtLeast(0)

        // 5. Health regeneration
        dayHealth = (dayHealth + 12).coerceAtMost(100)

        // Jail countdown if jailed
        var isJailedNow = state.isJailed
        var jailDaysNow = state.jailDaysLeft
        if (isJailedNow) {
            jailDaysNow -= 1
            if (jailDaysNow <= 0) {
                isJailedNow = false
                jailDaysNow = 0
            }
        }

        val log = GameHistoryEntry(
            nextDay,
            "Наступил День $nextDay. Прибыль от бизнесов: +${formatMoney(totalDailyGain)} ₽, зарплаты братве: -${formatMoney(totalSalaries)} ₽.",
            totalDailyGain >= totalSalaries
        )

        val newStats = state.stats.copy(
            daysSurvived = state.stats.daysSurvived + 1,
            totalEarned = state.stats.totalEarned + totalDailyGain.coerceAtLeast(0)
        )

        _uiState.update {
            it.copy(
                day = nextDay,
                money = dayMoney,
                respect = dayRespect,
                policeHeat = dayHeat,
                health = dayHealth,
                isJailed = isJailedNow,
                jailDaysLeft = jailDaysNow,
                stats = newStats,
                history = it.history + log
            )
        }

        // Trigger random event (25% chance) if not in battle or jail
        if (!isJailedNow && Random.nextInt(100) < 25) {
            val event = repository.getRandomEvents().random()
            _uiState.update { it.copy(activeEvent = event) }
        }

        persistState()
    }

    // RESOLVE RANDOM EVENT
    fun resolveEvent(option: GameEventOption) {
        val state = _uiState.value
        val newMoney = state.money + option.moneyDelta
        val newRespect = (state.respect + option.respectDelta).coerceAtLeast(0)
        val newHeat = (state.policeHeat + option.heatDelta).coerceIn(0, 100)
        val newHealth = (state.health + option.healthDelta).coerceIn(1, 100)

        val log = GameHistoryEntry(
            state.day,
            "Событие: ${option.outcomeDescription}",
            option.moneyDelta >= 0 && option.respectDelta >= 0
        )

        repository.playSound(if (option.moneyDelta > 0) SoundType.CASH else SoundType.CLICK)

        _uiState.update {
            it.copy(
                money = newMoney,
                respect = newRespect,
                policeHeat = newHeat,
                health = newHealth,
                rank = ReputationRank.fromRespect(newRespect),
                activeEvent = null,
                history = it.history + log,
                alertMessage = option.outcomeDescription
            )
        }
        persistState()
    }

    fun dismissEvent() {
        _uiState.update { it.copy(activeEvent = null) }
    }

    // HOSPITAL & POLICE BRIBES
    fun healHospital() {
        val state = _uiState.value
        val cost = 6000L
        if (state.money < cost) {
            _uiState.update { it.copy(alertMessage = "В платной клинике требуют $cost ₽!") }
            return
        }
        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money - cost,
                health = 100,
                alertMessage = "Знакомый хирург зашил раны и поставил капельницу. Здоровье 100%!"
            )
        }
        persistState()
    }

    fun bribePolice() {
        val state = _uiState.value
        val cost = 12000L
        if (state.money < cost) {
            _uiState.update { it.copy(alertMessage = "Для взятки в РОВД нужно $cost ₽!") }
            return
        }
        if (state.policeHeat <= 0) {
            _uiState.update { it.copy(alertMessage = "У милиции к тебе сейчас нет претензий!") }
            return
        }
        repository.playSound(SoundType.CASH)
        val newHeat = (state.policeHeat - 30).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                money = it.money - cost,
                policeHeat = newHeat,
                alertMessage = "Занесли конверт следователю. Ориентировки из РОВД отозваны (-30 розыска)!"
            )
        }
        persistState()
    }

    private fun persistState() {
        viewModelScope.launch {
            val state = _uiState.value
            val purchasedAssetIds = state.assets.filter { it.isPurchased }.map { it.id }.toSet()
            val equippedAssetIds = state.assets.filter { it.isEquipped }.map { it.id }.toSet()
            val hiredCrewIds = state.crew.filter { it.isHired }.map { it.id }.toSet()
            val districtControl = state.districts.associate { it.id to it.controlPercent }

            repository.saveGameState(
                nickname = state.nickname,
                money = state.money,
                respect = state.respect,
                health = state.health,
                policeHeat = state.policeHeat,
                strength = state.strength,
                influence = state.influence,
                day = state.day,
                isJailed = state.isJailed,
                jailDaysLeft = state.jailDaysLeft,
                purchasedAssetIds = purchasedAssetIds,
                equippedAssetIds = equippedAssetIds,
                hiredCrewIds = hiredCrewIds,
                districtControl = districtControl,
                stats = state.stats,
                history = state.history
            )
        }
    }

    private fun formatMoney(amount: Long): String {
        return "%,d".format(amount).replace(',', ' ')
    }
}
