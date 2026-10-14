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
    val safeMoney: Long = 0L,
    val respect: Int = 10,
    val health: Int = 100,
    val energy: Int = 100, // Daily energy / Action Points (0-100%)
    val policeHeat: Int = 5,
    val strength: Int = 25,
    val influence: Int = 10,
    val day: Int = 1,
    val isJailed: Boolean = false,
    val jailDaysLeft: Int = 0,
    val rank: ReputationRank = ReputationRank.SHNYR,

    // Skill Perks
    val skillPoints: Int = 1,
    val skillBoxing: Int = 0,
    val skillShooting: Int = 0,
    val skillStealth: Int = 0,
    val skillLuck: Int = 0,
    val skillToughness: Int = 0,

    // Lists
    val crimes: List<CrimeItem> = emptyList(),
    val assets: List<AssetItem> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    val districts: List<District> = emptyList(),
    val rivalBosses: List<RivalBoss> = emptyList(),
    val history: List<GameHistoryEntry> = emptyList(),
    val stats: PlayerStats = PlayerStats(),

    // Mini-games and Dialogs
    val activeShowdown: ShowdownState? = null,
    val activeEvent: GameEvent? = null,
    val currentRiddle: PrisonRiddle? = null,
    val riddleFeedback: String? = null,
    val showNewGameDialog: Boolean = false,
    val showCasinoDialog: Boolean = false,
    val showBossArenaDialog: Boolean = false,
    val showOchkoDialog: Boolean = false,
    val showGymDialog: Boolean = false,
    val showSkillsDialog: Boolean = false,
    val showSafeDialog: Boolean = false,
    val showBossChatDialog: Boolean = false,
    val activeBossChat: BossChatState? = null,
    val naperstki: NaperstkiState = NaperstkiState(),
    val diceGame: DiceGameState = DiceGameState(),
    val ochkoState: OchkoGameState = OchkoGameState(),
    val morningSummary: DailySummary? = null,
    val morningNews: String? = null,
    val alertMessage: String? = null
)

sealed interface ShowdownAction {
    data object BAZAR : ShowdownAction // Speech / demoralize
    data object PUNCH : ShowdownAction // Hand-to-hand combat
    data object SHOOT : ShowdownAction // Aimed gunshot
    data object GUARD : ShowdownAction // Defensive cover
    data object FIRST_AID : ShowdownAction // Healing bandage
    data object CALL_CREW : ShowdownAction // Crew assault
    data object RAGE_KO : ShowdownAction // 100% Rage KO
    data object FLEE : ShowdownAction // Escape in car
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val bossService = com.example.data.ai.GeminiBossService()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadOrInitGame()
        }
    }

    private suspend fun loadOrInitGame() {
        val bosses = repository.getRivalBosses()
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
                    safeMoney = saved.safeMoney,
                    respect = saved.respect,
                    health = saved.health,
                    energy = saved.energy,
                    policeHeat = saved.policeHeat,
                    strength = saved.strength,
                    influence = saved.influence,
                    day = saved.day,
                    isJailed = saved.isJailed,
                    jailDaysLeft = saved.jailDaysLeft,
                    skillPoints = saved.skillPoints,
                    skillBoxing = saved.skillBoxing,
                    skillShooting = saved.skillShooting,
                    skillStealth = saved.skillStealth,
                    skillLuck = saved.skillLuck,
                    skillToughness = saved.skillToughness,
                    rank = ReputationRank.fromRespect(saved.respect),
                    crimes = repository.getDefaultCrimes(),
                    assets = allAssets,
                    crew = allCrew,
                    districts = allDistricts,
                    rivalBosses = bosses,
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

            initFreshGame(initialNick, baseMoney, baseRespect, baseStrength, baseInfluence)
        }
    }

    private fun initFreshGame(
        nick: String = "Седой",
        startMoney: Long = 5000L,
        startRespect: Int = 10,
        startStrength: Int = 25,
        startInfluence: Int = 10
    ) {
        val initialHistory = listOf(
            GameHistoryEntry(1, "Начало криминального пути: $nick прибыл на район с $startMoney ₽.", true)
        )
        val defaultAssets = repository.getDefaultAssets().map { asset ->
            if (asset.id == "cloth_tracksuit") asset.copy(isPurchased = true, isEquipped = true)
            else asset
        }
        val defaultCrew = repository.getDefaultCrew()
        val defaultDistricts = repository.getDefaultDistricts()
        val defaultCrimes = repository.getDefaultCrimes()
        val bosses = repository.getRivalBosses()

        _uiState.update {
            it.copy(
                nickname = nick,
                money = startMoney,
                safeMoney = 0L,
                respect = startRespect,
                health = 100,
                energy = 100,
                policeHeat = 5,
                strength = startStrength,
                influence = startInfluence,
                day = 1,
                isJailed = false,
                jailDaysLeft = 0,
                skillPoints = 1,
                skillBoxing = 0,
                skillShooting = 0,
                skillStealth = 0,
                skillLuck = 0,
                skillToughness = 0,
                rank = ReputationRank.fromRespect(startRespect),
                crimes = defaultCrimes,
                assets = defaultAssets,
                crew = defaultCrew,
                districts = defaultDistricts,
                rivalBosses = bosses,
                stats = PlayerStats(totalEarned = startMoney),
                history = initialHistory,
                showNewGameDialog = false,
                showBossArenaDialog = false,
                showOchkoDialog = false,
                showGymDialog = false,
                showSafeDialog = false,
                morningSummary = null
            )
        }
        persistState()
    }

    // CRIME EXECUTION WITH ENERGY & REAL RISKS
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
        if (state.energy < crime.energyCost) {
            _uiState.update { it.copy(alertMessage = "Ты выдохся за сегодня (осталось ${state.energy}% сил)! Нажми «След. день», чтобы выспаться и набраться сил.") }
            return
        }

        // Calculate chance of success
        val equippedBonus = state.assets.filter { it.isEquipped }.sumOf { it.attackBonus }
        val crewIncomeBonus = state.crew.filter { it.isHired }.sumOf { it.incomePercentBonus }
        val effectiveStrength = state.strength + equippedBonus + (state.skillBoxing * 4)

        val strengthDiff = (effectiveStrength - crime.requiredStrength).coerceIn(-25, 25)
        val successChance = (crime.baseSuccessRate + strengthDiff / 2).coerceIn(20, 92)

        val roll = Random.nextInt(100)
        val isSuccess = roll < successChance
        val newEnergy = (state.energy - crime.energyCost).coerceAtLeast(0)

        if (isSuccess) {
            // Success
            val rawMoney = Random.nextLong(crime.minMoney, crime.maxMoney + 1)
            val bonusMoney = (rawMoney * (crewIncomeBonus + state.skillLuck * 8)) / 100
            val totalReward = rawMoney + bonusMoney

            val newMoney = state.money + totalReward
            val newRespect = state.respect + crime.respectReward
            val heatAdd = (crime.heatPenalty - (state.skillStealth * 2)).coerceAtLeast(2)
            val newHeat = (state.policeHeat + heatAdd).coerceIn(0, 100)
            val newRank = ReputationRank.fromRespect(newRespect)

            // Skill point chance on milestone
            val skillPointGained = if (newRespect / 400 > state.respect / 400) 1 else 0

            val newStats = state.stats.copy(
                totalCrimes = state.stats.totalCrimes + 1,
                successfulCrimes = state.stats.successfulCrimes + 1,
                totalEarned = state.stats.totalEarned + totalReward
            )

            val logEntry = GameHistoryEntry(
                state.day,
                "Успешное дело: «${crime.title}». Куш: +${formatMoney(totalReward)} ₽, уважение +${crime.respectReward}. Потрачено ${crime.hoursNeeded} ч.",
                true
            )

            repository.playSound(SoundType.CASH)

            _uiState.update {
                it.copy(
                    money = newMoney,
                    respect = newRespect,
                    energy = newEnergy,
                    policeHeat = newHeat,
                    rank = newRank,
                    skillPoints = it.skillPoints + skillPointGained,
                    stats = newStats,
                    history = it.history + logEntry,
                    alertMessage = if (skillPointGained > 0)
                        "Дело выгорело! +${formatMoney(totalReward)} ₽. Получено +1 очко понятий!"
                    else "Дело выгорело! Поднял ${formatMoney(totalReward)} ₽."
                )
            }

            checkPoliceIntervention(newHeat)
        } else {
            // Failure
            val damage = (crime.healthRisk + Random.nextInt(6, 16) - (state.skillToughness * 3)).coerceAtLeast(5)
            val newHealth = (state.health - damage).coerceAtLeast(1)
            val newHeat = (state.policeHeat + crime.heatPenalty + 12).coerceIn(0, 100)
            val respectLoss = (crime.respectReward / 3).coerceAtLeast(1)
            val newRespect = (state.respect - respectLoss).coerceAtLeast(0)

            val newStats = state.stats.copy(
                totalCrimes = state.stats.totalCrimes + 1
            )

            val logEntry = GameHistoryEntry(
                state.day,
                "Провал дела «${crime.title}»! Получил по шее (-$damage HP), внимание ментов выросло.",
                false
            )

            repository.playSound(SoundType.PUNCH)

            _uiState.update {
                it.copy(
                    health = newHealth,
                    respect = newRespect,
                    energy = newEnergy,
                    policeHeat = newHeat,
                    stats = newStats,
                    history = it.history + logEntry,
                    alertMessage = "Облом! Дело сорвалось. Получил травму (-$damage HP) и хвост милиции!"
                )
            }

            checkPoliceIntervention(newHeat)
        }
        persistState()
    }

    private fun checkPoliceIntervention(heat: Int) {
        if (heat >= 95) {
            arrestPlayer("Облава РУОП! За тобой выехал спецназ из-за запредельного розыска.")
        } else if (heat >= 75 && Random.nextInt(100) < 40) {
            repository.playSound(SoundType.SIREN)
            triggerPoliceShowdown()
        }
    }

    // SHOWDOWN ARENA AND COMBAT ENGINE
    fun openBossArena() {
        _uiState.update { it.copy(showBossArenaDialog = true) }
    }

    fun closeBossArena() {
        _uiState.update { it.copy(showBossArenaDialog = false) }
    }

    // AI BOSS TALK & DIPLOMACY
    fun openBossChat(targetBoss: RivalBoss? = null) {
        val bosses = _uiState.value.rivalBosses.ifEmpty { repository.getRivalBosses() }
        val boss = targetBoss ?: bosses.firstOrNull() ?: return
        val currentChat = _uiState.value.activeBossChat
        val initialMessages = if (currentChat != null && currentChat.boss.id == boss.id && currentChat.messages.isNotEmpty()) {
            currentChat.messages
        } else {
            val greeting = when (boss.id) {
                "boss_shtyr" -> "Слышь, фраерок! Ты на чьей поляне трёшься? Закурить есть или обосновать пришёл?"
                "boss_kastet" -> "Здорово, дрыщ. Чо припёрся в Люберцы? Силушки померяться захотел или бабки на качалку занёс?"
                "boss_klyk" -> "Здорово, браток. На вокзале свои законы. С чем пожаловал на стрелу к бригадиру?"
                "boss_baron" -> "Ты под кем ходишь, пацан? У меня рынок и автобаза под контролем. Говори по делу, счётчик тикает."
                "boss_mramor" -> "Приветствую, коллега. В центре города всё решается цивилизованно, но по воровским понятиям. Слушаю."
                else -> "Говори, с чем пожаловал на стрелу? Время — деньги."
            }
            listOf(
                BossChatMessage(
                    sender = MessageSender.BOSS,
                    text = greeting
                )
            )
        }

        repository.playSound(SoundType.PHONE)

        _uiState.update {
            it.copy(
                showBossChatDialog = true,
                showBossArenaDialog = false,
                activeBossChat = BossChatState(
                    boss = boss,
                    messages = initialMessages,
                    relationshipScore = currentChat?.takeIf { c -> c.boss.id == boss.id }?.relationshipScore ?: 0
                )
            )
        }
    }

    fun closeBossChat() {
        repository.playSound(SoundType.CLICK)
        _uiState.update { it.copy(showBossChatDialog = false) }
    }

    fun switchBossInChat(newBoss: RivalBoss) {
        openBossChat(newBoss)
    }

    fun sendBossChatMessage(playerText: String) {
        val trimmed = playerText.trim()
        if (trimmed.isEmpty()) return

        val state = _uiState.value
        val currentChat = state.activeBossChat ?: return
        if (currentChat.isLoading) return

        val playerMsg = BossChatMessage(
            sender = MessageSender.PLAYER,
            text = trimmed
        )
        val updatedMessages = currentChat.messages + playerMsg

        repository.playSound(SoundType.CLICK)

        _uiState.update {
            it.copy(
                activeBossChat = currentChat.copy(
                    messages = updatedMessages,
                    isLoading = true
                )
            )
        }

        viewModelScope.launch {
            val reply = bossService.getBossResponse(
                boss = currentChat.boss,
                playerMessage = trimmed,
                history = updatedMessages,
                heroNickname = state.nickname,
                heroRank = state.rank,
                heroRespect = state.respect,
                heroMoney = state.money,
                districtsCount = state.districts.count { it.controlPercent >= 50 }
            )

            val bossMsg = BossChatMessage(
                sender = MessageSender.BOSS,
                text = reply.text
            )

            val newScore = (currentChat.relationshipScore + reply.sentimentDelta).coerceIn(-100, 100)

            if (reply.sentimentDelta > 0) {
                repository.playSound(SoundType.CASH)
            } else if (reply.sentimentDelta < -10) {
                repository.playSound(SoundType.PUNCH)
            } else {
                repository.playSound(SoundType.CLICK)
            }

            val respectBonus = if (reply.sentimentDelta >= 10) 2 else 0

            _uiState.update { s ->
                val chat = s.activeBossChat
                val updatedRespect = s.respect + respectBonus
                if (chat != null) {
                    s.copy(
                        respect = updatedRespect,
                        rank = ReputationRank.fromRespect(updatedRespect),
                        activeBossChat = chat.copy(
                            messages = chat.messages + bossMsg,
                            isLoading = false,
                            relationshipScore = newScore
                        )
                    )
                } else s
            }
            if (respectBonus > 0) {
                persistState()
            }
        }
    }

    fun payBossTribute() {
        val state = _uiState.value
        val chat = state.activeBossChat ?: return
        val tributeAmount = 3000L

        if (state.money < tributeAmount) {
            _uiState.update { it.copy(alertMessage = "Не хватает денег на откуп ($tributeAmount ₽)!") }
            return
        }

        val newMoney = state.money - tributeAmount
        val newHeat = (state.policeHeat - 8).coerceAtLeast(0)
        val newScore = (chat.relationshipScore + 30).coerceAtMost(100)

        repository.playSound(SoundType.CASH)

        val bossThankMsg = BossChatMessage(
            sender = MessageSender.BOSS,
            text = "Купюры хрустят приятно! За уважение к общаку твои проблемы с ментами и районом мы порешаем."
        )

        _uiState.update {
            it.copy(
                money = newMoney,
                policeHeat = newHeat,
                activeBossChat = chat.copy(
                    messages = chat.messages + bossThankMsg,
                    relationshipScore = newScore
                )
            )
        }
        persistState()
    }

    fun startBossShowdown(boss: RivalBoss) {
        val state = _uiState.value
        if (state.health < 25) {
            _uiState.update { it.copy(alertMessage = "Ты ранен! Подлечись в больничке перед стрелкой.") }
            return
        }
        if (state.energy < 25) {
            _uiState.update { it.copy(alertMessage = "Нет сил на стрелку! Нажми «След. день», чтобы набраться сил.") }
            return
        }

        val playerMaxHp = (state.health + (state.skillToughness * 15)).coerceAtLeast(60)

        val showdown = ShowdownState(
            rivalName = boss.name,
            rivalTitle = boss.title,
            rivalMaxHp = boss.maxHp,
            rivalCurrentHp = boss.maxHp,
            rivalAttack = boss.attack,
            playerMaxHp = playerMaxHp,
            playerCurrentHp = playerMaxHp,
            playerShield = 0,
            firstAidKits = 2,
            ammoCount = 6,
            rageMeter = 0,
            round = 1,
            rivalIntent = "Собирается атаковать в лоб!",
            rivalAvatarEmoji = boss.avatarEmoji,
            rewardMoney = boss.rewardMoney,
            rewardRespect = boss.rewardRespect,
            battleLog = listOf("Стрелка началась! Перед тобой ${boss.name} (${boss.title}). Напряжение на пределе!"),
            isFinished = false,
            isPlayerWon = false,
            districtId = null
        )

        _uiState.update {
            it.copy(
                energy = it.energy - 25,
                activeShowdown = showdown,
                showBossArenaDialog = false
            )
        }
        persistState()
    }

    fun triggerDistrictShowdown(district: District) {
        val difficultyMultiplier = when (district.id) {
            "dist_zavod" -> 1
            "dist_vokzal" -> 2
            "dist_center" -> 3
            "dist_port" -> 4
            else -> 5
        }
        val maxHp = 200 + difficultyMultiplier * 60
        val attack = 18 + difficultyMultiplier * 7

        startShowdown(
            rivalName = "Бригада «${district.rivalGang}»",
            rivalTitle = "Смотрящие за ${district.name}",
            maxHp = maxHp,
            attack = attack,
            rewardMoney = district.dailyTribute * 4,
            rewardRespect = 70 + difficultyMultiplier * 40,
            districtId = district.id
        )
    }

    private fun triggerPoliceShowdown() {
        startShowdown(
            rivalName = "Опергруппа УгРо и ОМОН",
            rivalTitle = "Милицейская засада",
            maxHp = 280,
            attack = 25,
            rewardMoney = 30000,
            rewardRespect = 90,
            districtId = null
        )
    }

    fun startShowdown(
        rivalName: String,
        rivalTitle: String,
        maxHp: Int,
        attack: Int,
        rewardMoney: Long,
        rewardRespect: Int,
        districtId: String? = null
    ) {
        val playerMaxHp = (_uiState.value.health + (_uiState.value.skillToughness * 15)).coerceAtLeast(60)
        val showdown = ShowdownState(
            rivalName = rivalName,
            rivalTitle = rivalTitle,
            rivalMaxHp = maxHp,
            rivalCurrentHp = maxHp,
            rivalAttack = attack,
            playerMaxHp = playerMaxHp,
            playerCurrentHp = playerMaxHp,
            playerShield = 0,
            firstAidKits = 2,
            ammoCount = 6,
            rageMeter = 0,
            round = 1,
            rivalIntent = "Собирается нанести удар!",
            rivalAvatarEmoji = "👊",
            rewardMoney = rewardMoney,
            rewardRespect = rewardRespect,
            battleLog = listOf("Стрелка началась! Перед тобой $rivalName ($rivalTitle). Братва заняла позиции."),
            isFinished = false,
            isPlayerWon = false,
            districtId = districtId
        )
        _uiState.update { it.copy(activeShowdown = showdown) }
    }

    fun performShowdownAction(action: ShowdownAction) {
        val currentShowdown = _uiState.value.activeShowdown ?: return
        if (currentShowdown.isFinished) return

        val state = _uiState.value
        val equippedAttack = state.assets.filter { it.isEquipped }.sumOf { it.attackBonus }
        val equippedDefense = state.assets.filter { it.isEquipped }.sumOf { it.defenseBonus }
        val crewCombat = state.crew.filter { it.isHired }.sumOf { it.combatBonus }

        val newLog = currentShowdown.battleLog.toMutableList()
        var rivalHp = currentShowdown.rivalCurrentHp
        var playerHp = currentShowdown.playerCurrentHp
        var playerShield = currentShowdown.playerShield
        var ammo = currentShowdown.ammoCount
        var kits = currentShowdown.firstAidKits
        var rage = currentShowdown.rageMeter
        var round = currentShowdown.round
        var rivalStunned = false
        var wasPlayerHit = false
        var wasCritical = false
        var actionFeedback = ""

        when (action) {
            ShowdownAction.BAZAR -> {
                val speechPower = (state.influence * 2 + state.respect / 20).coerceIn(25, 85)
                val success = Random.nextInt(100) < speechPower
                if (success) {
                    val damage = Random.nextInt(25, 45)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    rivalStunned = true
                    newLog.add("[Раунд $round] Базар по понятиям: Ты морально задавил оппонента (-$damage морали)! Противник ошеломлён и пропускает ход.")
                    actionFeedback = "Базар проканал! Противник дрогнул."
                    repository.playSound(SoundType.CLICK)
                } else {
                    newLog.add("[Раунд $round] Базар по понятиям: Противник огрызнулся: «Базар не канает!»")
                    actionFeedback = "Базар не сработал!"
                    repository.playSound(SoundType.CLICK)
                }
            }
            ShowdownAction.PUNCH -> {
                val base = 18 + state.strength / 4 + (state.skillBoxing * 6) + Random.nextInt(4, 10)
                val isCrit = rage >= 90
                val damage = if (isCrit) {
                    wasCritical = true
                    newLog.add("[Раунд $round] КРИТИЧЕСКИЙ УДАР! Мощный нокаут в челюсть!")
                    rage = 0
                    base * 2
                } else {
                    rage = (rage + 25).coerceAtMost(100)
                    base
                }
                rivalHp = (rivalHp - damage).coerceAtLeast(0)
                newLog.add("[Раунд $round] Удар в упор: -$damage HP! (Адреналин: $rage%)")
                actionFeedback = "Удар в челюсть (-$damage HP)"
                repository.playSound(SoundType.PUNCH)
            }
            ShowdownAction.RAGE_KO -> {
                if (rage >= 100) {
                    wasCritical = true
                    val damage = 55 + (state.strength / 2) + Random.nextInt(15, 25)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    rivalStunned = true
                    rage = 0
                    newLog.add("[Раунд $round] ЯРОСТНЫЙ НОКАУТ! Сокрушительное добивание (-$damage HP)! Противник сбит с ног.")
                    actionFeedback = "ЯРОСТНЫЙ НОКАУТ (-$damage HP)!"
                    repository.playSound(SoundType.RAGE)
                } else {
                    newLog.add("[Раунд $round] Не хватает адреналина для добивания!")
                }
            }
            ShowdownAction.SHOOT -> {
                if (ammo > 0) {
                    ammo -= 1
                    val damage = 38 + (equippedAttack / 2) + (state.skillShooting * 8) + Random.nextInt(10, 22)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    newLog.add("[Раунд $round] Прицельный выстрел из волыны! -$damage HP! (Патронов осталось: $ammo)")
                    actionFeedback = "Выстрел из ствола (-$damage HP)"
                    repository.playSound(SoundType.SHOT)
                } else {
                    newLog.add("[Раунд $round] Щёлк! Закончились патроны! Пришлось бить рукояткой пистолета.")
                    val damage = 12 + Random.nextInt(4, 8)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    actionFeedback = "Осечка! Удар рукояткой (-$damage HP)"
                    repository.playSound(SoundType.PUNCH)
                }
            }
            ShowdownAction.GUARD -> {
                playerShield = 40 + (equippedDefense / 2) + (state.skillToughness * 5)
                newLog.add("[Раунд $round] Глухая защита: Ты занял укрытие за машиной! Блок щитом +$playerShield HP.")
                actionFeedback = "Защитный блок (+$playerShield)"
                repository.playSound(SoundType.CLICK)
            }
            ShowdownAction.FIRST_AID -> {
                if (kits > 0) {
                    kits -= 1
                    playerHp = (playerHp + 45).coerceAtMost(currentShowdown.playerMaxHp)
                    newLog.add("[Раунд $round] Аптечка: Перевязал раны и принял анальгин (+45 HP). Осталось аптечек: $kits.")
                    actionFeedback = "Перевязка ран (+45 HP)"
                    repository.playSound(SoundType.CLICK)
                } else {
                    newLog.add("[Раунд $round] Аптечки закончились!")
                    actionFeedback = "Нет аптечек!"
                }
            }
            ShowdownAction.CALL_CREW -> {
                if (crewCombat > 0) {
                    val damage = 30 + (crewCombat / 2) + Random.nextInt(8, 18)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    newLog.add("[Раунд $round] Братва, в бой: Твои бойцы налетели с монтировками и битами (-$damage HP)!")
                    actionFeedback = "Налёт братвы (-$damage HP)"
                    repository.playSound(SoundType.PUNCH)
                } else {
                    newLog.add("[Раунд $round] В твоей банде нет бойцов! Пришлось отбиваться самому.")
                    val damage = 10 + Random.nextInt(3, 7)
                    rivalHp = (rivalHp - damage).coerceAtLeast(0)
                    actionFeedback = "Отбился сам (-$damage HP)"
                    repository.playSound(SoundType.CLICK)
                }
            }
            ShowdownAction.FLEE -> {
                val hasCar = state.assets.any { it.isEquipped && it.category == AssetCategory.VEHICLE }
                val fleeChance = if (hasCar) 75 else 40
                if (Random.nextInt(100) < fleeChance) {
                    newLog.add("[Раунд $round] Ты дал по газам и оторвался от погони!")
                    val finishedShowdown = currentShowdown.copy(
                        battleLog = newLog,
                        isFinished = true,
                        isPlayerWon = false
                    )
                    _uiState.update { it.copy(activeShowdown = finishedShowdown, alertMessage = "Ты успешно оторвался от преследования на стрелке.") }
                    persistState()
                    return
                } else {
                    newLog.add("[Раунд $round] Сбежать не удалось! Тебя зажали в кольцо.")
                    actionFeedback = "Побег сорван!"
                    repository.playSound(SoundType.PUNCH)
                }
            }
        }

        // Check if rival is defeated
        if (rivalHp <= 0) {
            rivalHp = 0
            newLog.add("🏆 ПОБЕДА! Враг повержен и молит о пощаде!")
            repository.playSound(SoundType.VICTORY)

            val newMoney = state.money + currentShowdown.rewardMoney
            val newRespect = state.respect + currentShowdown.rewardRespect
            val newStats = state.stats.copy(
                showdownsWon = state.stats.showdownsWon + 1,
                totalEarned = state.stats.totalEarned + currentShowdown.rewardMoney
            )

            // If district contest, increase control
            val updatedDistricts = if (currentShowdown.districtId != null) {
                state.districts.map { d ->
                    if (d.id == currentShowdown.districtId) {
                        d.copy(controlPercent = (d.controlPercent + 25).coerceAtMost(100))
                    } else d
                }
            } else {
                state.districts
            }

            val log = GameHistoryEntry(
                state.day,
                "Победа на стрелке против «${currentShowdown.rivalName}»! Куш: +${formatMoney(currentShowdown.rewardMoney)} ₽, уважение +${currentShowdown.rewardRespect}.",
                true
            )

            val finishedShowdown = currentShowdown.copy(
                rivalCurrentHp = 0,
                playerCurrentHp = playerHp,
                playerShield = 0,
                firstAidKits = kits,
                ammoCount = ammo,
                rageMeter = rage,
                round = round,
                battleLog = newLog,
                isFinished = true,
                isPlayerWon = true
            )

            _uiState.update {
                it.copy(
                    activeShowdown = finishedShowdown,
                    health = playerHp,
                    money = newMoney,
                    respect = newRespect,
                    skillPoints = it.skillPoints + 1, // Bonus skill point for winning showdown!
                    rank = ReputationRank.fromRespect(newRespect),
                    districts = updatedDistricts,
                    stats = newStats,
                    history = it.history + log
                )
            }
            persistState()
            return
        }

        // Rival attacks back if not stunned
        if (rivalStunned) {
            newLog.add("${currentShowdown.rivalName} приходит в себя и не успевает атаковать!")
        } else {
            val rawAttack = currentShowdown.rivalAttack + Random.nextInt(-3, 6)
            val mitigation = (equippedDefense / 4).coerceAtMost(rawAttack - 4)
            var actualDamage = (rawAttack - mitigation).coerceAtLeast(6)

            if (playerShield > 0) {
                if (playerShield >= actualDamage) {
                    playerShield -= actualDamage
                    newLog.add("${currentShowdown.rivalName} наносит удар, но блок поглотил весь урон! (Остаток щита: $playerShield)")
                    actualDamage = 0
                } else {
                    actualDamage -= playerShield
                    playerShield = 0
                    newLog.add("${currentShowdown.rivalName} пробил защитный блок и нанёс -$actualDamage HP!")
                    wasPlayerHit = true
                }
            } else {
                newLog.add("${currentShowdown.rivalName} наносит ответный удар! (-$actualDamage HP).")
                wasPlayerHit = true
            }

            playerHp = (playerHp - actualDamage).coerceAtLeast(0)
            if (wasPlayerHit) {
                repository.playSound(SoundType.PUNCH)
            }
        }

        if (playerHp <= 0) {
            playerHp = 0
            newLog.add("💀 Поражение! Ты потерял сознание и очнулся в больнице...")
            repository.playSound(SoundType.PUNCH)

            val hospitalBill = (state.money / 4).coerceAtLeast(1000)
            val newMoney = (state.money - hospitalBill).coerceAtLeast(0)

            val finishedShowdown = currentShowdown.copy(
                rivalCurrentHp = rivalHp,
                playerCurrentHp = 0,
                playerShield = 0,
                firstAidKits = kits,
                ammoCount = ammo,
                round = round,
                battleLog = newLog,
                isFinished = true,
                isPlayerWon = false
            )

            _uiState.update {
                it.copy(
                    activeShowdown = finishedShowdown,
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
            // Pick next round's rival intent
            val intents = listOf(
                "❗ Собирается нанести сокрушительный удар!",
                "⚠️ Вытаскивает ствол и целится!",
                "🛡️ Занял защитную глухую стойку!",
                "👊 Готовит серию быстрых ударов кастетом!",
                "🔪 Ищет брешь в твоей защите!"
            )
            val nextIntent = intents.random()

            val updatedShowdown = currentShowdown.copy(
                rivalCurrentHp = rivalHp,
                playerCurrentHp = playerHp,
                playerShield = playerShield,
                firstAidKits = kits,
                ammoCount = ammo,
                rageMeter = rage,
                round = round + 1,
                rivalIntent = nextIntent,
                lastActionFeedback = actionFeedback,
                wasPlayerHit = wasPlayerHit,
                wasCriticalHit = wasCritical,
                battleLog = newLog,
                isFinished = false,
                isPlayerWon = false
            )

            _uiState.update {
                it.copy(
                    activeShowdown = updatedShowdown,
                    health = playerHp
                )
            }
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

    fun answerPrisonRiddle(selectedIndex: Int) {
        val riddle = _uiState.value.currentRiddle ?: return
        val isCorrect = selectedIndex == riddle.correctIndex

        if (isCorrect) {
            repository.playSound(SoundType.VICTORY)
            val newRespect = _uiState.value.respect + 35
            val remainingDays = (_uiState.value.jailDaysLeft - 1).coerceAtLeast(0)
            val isReleased = remainingDays == 0

            _uiState.update {
                it.copy(
                    respect = newRespect,
                    jailDaysLeft = remainingDays,
                    isJailed = !isReleased,
                    riddleFeedback = "Правильно по понятиям! ${riddle.explanation}",
                    rank = ReputationRank.fromRespect(newRespect),
                    alertMessage = if (isReleased) "Твой авторитет на хате признан! Досрочно освобождён из СИЗО." else "Авторитет вырос! Срок сокращён на 1 день."
                )
            }
        } else {
            repository.playSound(SoundType.PUNCH)
            val newHealth = (_uiState.value.health - 20).coerceAtLeast(10)
            val newRespect = (_uiState.value.respect - 15).coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    health = newHealth,
                    respect = newRespect,
                    riddleFeedback = "Не по понятиям! Тебя осадили сокамерники. ${riddle.explanation}",
                    alertMessage = "Косяк в хате! Потерял здоровье (-20 HP) и уважение."
                )
            }
        }
        persistState()
    }

    fun bribePrisonWarden() {
        val state = _uiState.value
        val cost = 20000L
        if (state.money < cost) {
            _uiState.update { it.copy(alertMessage = "Вертухай требует $cost ₽ за выход через служебный ход!") }
            return
        }

        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money - cost,
                isJailed = false,
                jailDaysLeft = 0,
                alertMessage = "Купленный вертухай открыл дверь в пересменку. Ты на свободе!"
            )
        }
        persistState()
    }

    fun hireLawyerInJail() {
        val state = _uiState.value
        val hasLawyerCrew = state.crew.any { it.isHired && it.id == "crew_advokat" }
        val cost = if (hasLawyerCrew) 5000L else 35000L

        if (state.money < cost) {
            _uiState.update { it.copy(alertMessage = "Для работы адвоката нужно $cost ₽!") }
            return
        }

        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money - cost,
                isJailed = false,
                jailDaysLeft = 0,
                policeHeat = 10,
                alertMessage = "Адвокат нашёл процессуальные нарушения. Дело развалилось, ты свободен!"
            )
        }
        persistState()
    }

    // ASSETS & EQUIPMENT
    fun buyAsset(asset: AssetItem) {
        val state = _uiState.value
        if (state.money < asset.price) {
            _uiState.update { it.copy(alertMessage = "Не хватает денег! Нужно ${formatMoney(asset.price)} ₽.") }
            return
        }

        repository.playSound(SoundType.CASH)
        val updatedAssets = state.assets.map { a ->
            if (a.id == asset.id) {
                val shouldAutoEquip = (a.category != AssetCategory.BUSINESS)
                a.copy(isPurchased = true, isEquipped = shouldAutoEquip)
            } else {
                if (asset.category != AssetCategory.BUSINESS && a.category == asset.category) {
                    a.copy(isEquipped = false)
                } else a
            }
        }

        val newRespect = state.respect + asset.respectBonus
        val newMoney = state.money - asset.price

        val log = GameHistoryEntry(state.day, "Куплено имущество: «${asset.name}» за ${formatMoney(asset.price)} ₽.", true)

        _uiState.update {
            it.copy(
                money = newMoney,
                respect = newRespect,
                assets = updatedAssets,
                rank = ReputationRank.fromRespect(newRespect),
                history = it.history + log,
                alertMessage = "Приобретено: ${asset.name}!"
            )
        }
        persistState()
    }

    fun toggleEquipAsset(asset: AssetItem) {
        if (!asset.isPurchased || asset.category == AssetCategory.BUSINESS) return

        repository.playSound(SoundType.CLICK)
        val newEquippedState = !asset.isEquipped

        val updatedAssets = _uiState.value.assets.map { a ->
            if (a.id == asset.id) {
                a.copy(isEquipped = newEquippedState)
            } else if (newEquippedState && a.category == asset.category) {
                a.copy(isEquipped = false)
            } else a
        }

        _uiState.update {
            it.copy(
                assets = updatedAssets,
                alertMessage = if (newEquippedState) "Экипировано: ${asset.name}" else "Снято: ${asset.name}"
            )
        }
        persistState()
    }

    // CREW MANAGEMENT
    fun hireCrew(crew: CrewMember) {
        val state = _uiState.value
        if (state.money < crew.hireCost) {
            _uiState.update { it.copy(alertMessage = "Не хватает денег на подъёмные (${formatMoney(crew.hireCost)} ₽)!") }
            return
        }

        repository.playSound(SoundType.CASH)
        val updatedCrew = state.crew.map { c ->
            if (c.id == crew.id) c.copy(isHired = true) else c
        }

        val log = GameHistoryEntry(state.day, "В банду нанят «${crew.name}» (${crew.role}).", true)

        _uiState.update {
            it.copy(
                money = it.money - crew.hireCost,
                crew = updatedCrew,
                history = it.history + log,
                alertMessage = "${crew.name} теперь в твоей бригаде!"
            )
        }
        persistState()
    }

    fun fireCrew(crew: CrewMember) {
        repository.playSound(SoundType.CLICK)
        val updatedCrew = _uiState.value.crew.map { c ->
            if (c.id == crew.id) c.copy(isHired = false) else c
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
        if (state.health < 20) {
            _uiState.update { it.copy(alertMessage = "Ты еле дышишь! Подлечись в больничке перед стрелкой.") }
            return
        }
        if (district.controlPercent >= 100) {
            _uiState.update { it.copy(alertMessage = "Район «${district.name}» уже полностью под твоим контролем (100%)!") }
            return
        }

        triggerDistrictShowdown(district)
    }

    // STRATEGIC DAY TRANSITION (THE HEART OF THE SIMULATOR)
    fun passDay() {
        val state = _uiState.value
        repository.playSound(SoundType.CLICK)

        val nextDay = state.day + 1
        var dayMoney = state.money
        var dayRespect = state.respect
        var dayHeat = state.policeHeat
        var dayHealth = state.health
        var safeMoney = state.safeMoney

        // 1. Business passive income
        val businessIncome = state.assets.filter { it.isPurchased && it.category == AssetCategory.BUSINESS }
            .sumOf { it.dailyIncome }

        // 2. District tributes
        val districtIncome = state.districts.sumOf { (it.dailyTribute * it.controlPercent) / 100 }

        // 3. Crew salaries
        val crewSalaries = state.crew.filter { it.isHired }.sumOf { it.dailySalary }

        // 4. Base living expense (apartment, car maintenance, phone)
        val livingExpenses = 400L + (nextDay * 10L).coerceAtMost(2500L)

        // 5. Safe interest (+2% daily)
        var safeGain = 0L
        if (safeMoney > 0) {
            safeGain = (safeMoney * 0.02).toLong()
            safeMoney += safeGain
        }

        val netDailyChange = businessIncome + districtIncome - crewSalaries - livingExpenses
        dayMoney = (dayMoney + netDailyChange).coerceAtLeast(0)

        // 6. Natural heat decay
        val equippedHeatRed = state.assets.filter { it.isEquipped }.sumOf { it.heatReductionDaily }
        val crewHeatRed = state.crew.filter { it.isHired }.sumOf { it.heatReductionBonus }
        val totalHeatDecay = 8 + equippedHeatRed + crewHeatRed
        dayHeat = (dayHeat - totalHeatDecay).coerceAtLeast(0)

        // 7. Health natural overnight regeneration
        val healthRegen = 25
        dayHealth = (dayHealth + healthRegen).coerceAtMost(100)

        // 8. Energy restored fully to 100%
        val newEnergy = 100

        // 9. Skill point bonus every 3 days
        val skillPointsBonus = if (nextDay % 3 == 0) 1 else 0

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

        val news = repository.getMorningNews(nextDay)
        val summary = DailySummary(
            day = nextDay,
            headline = news,
            businessIncome = businessIncome,
            districtTribute = districtIncome,
            crewSalaries = crewSalaries,
            livingExpenses = livingExpenses,
            netChange = netDailyChange,
            heatDecay = totalHeatDecay,
            healthRegen = healthRegen,
            specialEffect = if (safeGain > 0) "Общак в сейфе принёс +${formatMoney(safeGain)} ₽ дивидендов." else "Силы полностью восстановлены (⚡ 100%)."
        )

        val log = GameHistoryEntry(
            nextDay,
            "День $nextDay. $news Баланс дня: ${if (netDailyChange >= 0) "+${formatMoney(netDailyChange)}" else "-${formatMoney(-netDailyChange)}"} ₽. Силы восстановлены.",
            netDailyChange >= 0
        )

        val newStats = state.stats.copy(
            daysSurvived = state.stats.daysSurvived + 1,
            totalEarned = state.stats.totalEarned + (businessIncome + districtIncome).coerceAtLeast(0)
        )

        _uiState.update {
            it.copy(
                day = nextDay,
                money = dayMoney,
                safeMoney = safeMoney,
                respect = dayRespect,
                policeHeat = dayHeat,
                health = dayHealth,
                energy = newEnergy,
                skillPoints = it.skillPoints + skillPointsBonus,
                isJailed = isJailedNow,
                jailDaysLeft = jailDaysNow,
                morningSummary = summary,
                morningNews = news,
                stats = newStats,
                history = it.history + log,
                alertMessage = "Наступил День $nextDay! Энергия восстановлена на 100%."
            )
        }

        // Trigger random event (30% chance)
        if (!isJailedNow && Random.nextInt(100) < 30) {
            val event = repository.getRandomEvents().random()
            _uiState.update { it.copy(activeEvent = event) }
        }

        persistState()
    }

    fun dismissMorningSummary() {
        _uiState.update { it.copy(morningSummary = null) }
    }

    // GYM & TRAINING
    fun openGymDialog() {
        _uiState.update { it.copy(showGymDialog = true) }
    }

    fun closeGymDialog() {
        _uiState.update { it.copy(showGymDialog = false) }
    }

    fun trainGym(type: String) {
        val state = _uiState.value
        val cost = 25
        if (state.energy < cost) {
            _uiState.update { it.copy(alertMessage = "Нет сил для тренировки! Нажми «След. день», чтобы отдохнуть.") }
            return
        }

        when (type) {
            "strength" -> {
                val addStrength = Random.nextInt(3, 6)
                repository.playSound(SoundType.PUNCH)
                _uiState.update {
                    it.copy(
                        energy = it.energy - cost,
                        strength = it.strength + addStrength,
                        alertMessage = "Тягал чугунные блины в подвале! Сила выросла на +$addStrength."
                    )
                }
            }
            "boxing" -> {
                repository.playSound(SoundType.PUNCH)
                _uiState.update {
                    it.copy(
                        energy = it.energy - cost,
                        skillBoxing = it.skillBoxing + 1,
                        alertMessage = "Спарринг с грушей! Навык «Боксёрская школа» повышен на +1."
                    )
                }
            }
            "shooting" -> {
                repository.playSound(SoundType.SHOT)
                _uiState.update {
                    it.copy(
                        energy = it.energy - cost,
                        skillShooting = it.skillShooting + 1,
                        alertMessage = "Стрельба по бутылкам на пустыре! Навык «Хладнокровный стрелок» повышен на +1."
                    )
                }
            }
        }
        persistState()
    }

    // SAFE & OBSHCHAK
    fun openSafeDialog() {
        _uiState.update { it.copy(showSafeDialog = true) }
    }

    fun closeSafeDialog() {
        _uiState.update { it.copy(showSafeDialog = false) }
    }

    fun depositToSafe(amount: Long) {
        val state = _uiState.value
        if (amount <= 0 || state.money < amount) {
            _uiState.update { it.copy(alertMessage = "Недостаточно денег для вклада в сейф!") }
            return
        }
        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money - amount,
                safeMoney = it.safeMoney + amount,
                alertMessage = "Положено в общак: ${formatMoney(amount)} ₽. Деньги защищены от ментов и приносят 2% в сутки."
            )
        }
        persistState()
    }

    fun withdrawFromSafe(amount: Long) {
        val state = _uiState.value
        if (state.safeMoney < amount || amount <= 0) {
            _uiState.update { it.copy(alertMessage = "В общаке нет такой суммы!") }
            return
        }
        repository.playSound(SoundType.CASH)
        _uiState.update {
            it.copy(
                money = it.money + amount,
                safeMoney = it.safeMoney - amount,
                alertMessage = "Взято из общака: ${formatMoney(amount)} ₽."
            )
        }
        persistState()
    }

    // CASINO: OCHKO (21)
    fun openOchkoDialog() {
        _uiState.update { it.copy(showOchkoDialog = true) }
    }

    fun closeOchkoDialog() {
        _uiState.update { it.copy(showOchkoDialog = false) }
    }

    fun startOchkoGame(bet: Long) {
        val state = _uiState.value
        if (state.money < bet) {
            _uiState.update { it.copy(alertMessage = "Не хватает денег на ставку (${formatMoney(bet)} ₽)!") }
            return
        }

        val deck = createOchkoDeck().shuffled()
        val pCard1 = deck[0]
        val pCard2 = deck[1]
        val dCard1 = deck[2]

        val pCards = listOf(pCard1, pCard2)
        val dCards = listOf(dCard1)
        val pScore = pCards.sumOf { it.value }
        val dScore = dCards.sumOf { it.value }

        val isGolden21 = (pScore == 21)
        val newMoney = if (isGolden21) state.money + (bet * 2) else state.money - bet

        if (isGolden21) repository.playSound(SoundType.VICTORY) else repository.playSound(SoundType.CARD)

        _uiState.update {
            it.copy(
                money = newMoney,
                ochkoState = OchkoGameState(
                    isPlaying = true,
                    betAmount = bet,
                    playerCards = pCards,
                    dealerCards = dCards,
                    playerScore = pScore,
                    dealerScore = dScore,
                    isFinished = isGolden21,
                    resultMessage = if (isGolden21) "ОЧКО (21)! Чистая победа с раздачи (+${formatMoney(bet * 2)} ₽)!" else null,
                    isWin = isGolden21
                )
            )
        }
        persistState()
    }

    fun hitOchkoCard() {
        val current = _uiState.value.ochkoState
        if (!current.isPlaying || current.isFinished) return

        val newCard = createOchkoDeck().random()
        val updatedCards = current.playerCards + newCard
        val newScore = updatedCards.sumOf { it.value }

        if (newScore > 21) {
            // Bust
            repository.playSound(SoundType.PUNCH)
            _uiState.update {
                it.copy(
                    ochkoState = current.copy(
                        playerCards = updatedCards,
                        playerScore = newScore,
                        isFinished = true,
                        isWin = false,
                        resultMessage = "Перебор ($newScore очков)! Ставка осталась у каталщика."
                    )
                )
            }
        } else if (newScore == 21) {
            standOchko(updatedCards, newScore)
        } else {
            repository.playSound(SoundType.CARD)
            _uiState.update {
                it.copy(
                    ochkoState = current.copy(
                        playerCards = updatedCards,
                        playerScore = newScore
                    )
                )
            }
        }
        persistState()
    }

    fun standOchko(
        playerCards: List<CardItem> = _uiState.value.ochkoState.playerCards,
        pScore: Int = _uiState.value.ochkoState.playerScore
    ) {
        val current = _uiState.value.ochkoState
        if (!current.isPlaying || current.isFinished) return

        val dCards = current.dealerCards.toMutableList()
        var dScore = dCards.sumOf { it.value }

        // Dealer draws until 17 or higher
        while (dScore < 17) {
            val card = createOchkoDeck().random()
            dCards.add(card)
            dScore += card.value
        }

        val state = _uiState.value
        val playerWon = (dScore > 21) || (pScore > dScore)
        val isDraw = (pScore == dScore)

        val returnGain = if (playerWon) current.betAmount * 2 else if (isDraw) current.betAmount else 0L
        val newMoney = state.money + returnGain

        val msg = when {
            dScore > 21 -> "У каталщика перебор ($dScore очков)! Ты победил (+${formatMoney(current.betAmount * 2)} ₽)!"
            playerWon -> "Твоя рука сильнее ($pScore против $dScore)! Выигрыш: +${formatMoney(current.betAmount * 2)} ₽!"
            isDraw -> "Ничья ($pScore против $dScore). Ставка возвращена."
            else -> "Каталщик взял больше ($dScore против $pScore). Ставка проиграна."
        }

        if (playerWon) repository.playSound(SoundType.CASH) else repository.playSound(SoundType.CLICK)

        _uiState.update {
            it.copy(
                money = newMoney,
                stats = if (playerWon) it.stats.copy(casinoWins = it.stats.casinoWins + 1) else it.stats,
                ochkoState = current.copy(
                    playerCards = playerCards,
                    dealerCards = dCards,
                    playerScore = pScore,
                    dealerScore = dScore,
                    isFinished = true,
                    isWin = playerWon,
                    resultMessage = msg
                )
            )
        }
        persistState()
    }

    private fun createOchkoDeck(): List<CardItem> {
        val suits = listOf("♠", "♣", "♥", "♦")
        val deck = mutableListOf<CardItem>()
        for (suit in suits) {
            deck.add(CardItem("6$suit", 6, suit))
            deck.add(CardItem("7$suit", 7, suit))
            deck.add(CardItem("8$suit", 8, suit))
            deck.add(CardItem("9$suit", 9, suit))
            deck.add(CardItem("10$suit", 10, suit))
            deck.add(CardItem("В$suit", 2, suit)) // Валет
            deck.add(CardItem("Д$suit", 3, suit)) // Дама
            deck.add(CardItem("К$suit", 4, suit)) // Король
            deck.add(CardItem("Т$suit", 11, suit)) // Туз
        }
        return deck
    }

    // SKILL UPGRADE
    fun openSkillsDialog() {
        _uiState.update { it.copy(showSkillsDialog = true) }
    }

    fun closeSkillsDialog() {
        _uiState.update { it.copy(showSkillsDialog = false) }
    }

    fun upgradeSkill(skillId: String) {
        val state = _uiState.value
        if (state.skillPoints <= 0) {
            _uiState.update { it.copy(alertMessage = "Нет очков понятий! Зарабатывай их за дни и дела.") }
            return
        }

        repository.playSound(SoundType.VICTORY)
        _uiState.update {
            when (skillId) {
                "boxing" -> it.copy(skillPoints = it.skillPoints - 1, skillBoxing = it.skillBoxing + 1, alertMessage = "Навык «Боксёрская школа» прокачан!")
                "shooting" -> it.copy(skillPoints = it.skillPoints - 1, skillShooting = it.skillShooting + 1, alertMessage = "Навык «Хладнокровный стрелок» прокачан!")
                "stealth" -> it.copy(skillPoints = it.skillPoints - 1, skillStealth = it.skillStealth + 1, alertMessage = "Навык «Тайные тропы» прокачан!")
                "luck" -> it.copy(skillPoints = it.skillPoints - 1, skillLuck = it.skillLuck + 1, alertMessage = "Навык «Воровской фарт» прокачан!")
                "toughness" -> it.copy(skillPoints = it.skillPoints - 1, skillToughness = it.skillToughness + 1, alertMessage = "Навык «Железная шкура» прокачан!")
                else -> it
            }
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

    fun openNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = true) }
    }

    fun dismissNewGameDialog() {
        _uiState.update { it.copy(showNewGameDialog = false) }
    }

    fun dismissAlert() {
        _uiState.update { it.copy(alertMessage = null) }
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
                energy = state.energy,
                safeMoney = state.safeMoney,
                skillPoints = state.skillPoints,
                skillBoxing = state.skillBoxing,
                skillShooting = state.skillShooting,
                skillStealth = state.skillStealth,
                skillLuck = state.skillLuck,
                skillToughness = state.skillToughness,
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
