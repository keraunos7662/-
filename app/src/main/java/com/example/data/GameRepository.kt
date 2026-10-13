package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.AppDatabase
import com.example.data.local.CharacterDao
import com.example.data.local.CharacterEntity
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class GameRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    val characterDao: CharacterDao = database.characterDao()
    val heroFlow: Flow<CharacterEntity?> = characterDao.getCharacter()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("criminal_russia_prefs", Context.MODE_PRIVATE)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            // Tone generator may not be available on all audio profiles
        }
    }

    fun playSound(type: SoundType) {
        try {
            when (type) {
                SoundType.CLICK -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
                SoundType.CASH -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                    vibrate(40)
                }
                SoundType.PUNCH -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 80)
                    vibrate(80)
                }
                SoundType.SHOT -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 150)
                    vibrate(120)
                }
                SoundType.SIREN -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 300)
                    vibrate(250)
                }
                SoundType.VICTORY -> {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 250)
                    vibrate(100)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // Default Catalog Data
    fun getDefaultCrimes(): List<CrimeItem> = listOf(
        CrimeItem(
            id = "crime_gop_stop",
            title = "Гоп-стоп у ларька",
            description = "Потрясти подвыпивших фраеров возле ночного ларька «Колосок». Быстрые карманные деньги.",
            category = CrimeCategory.STREET,
            requiredRank = ReputationRank.SHNYR,
            minMoney = 500,
            maxMoney = 2500,
            respectReward = 4,
            heatPenalty = 5,
            healthRisk = 5,
            baseSuccessRate = 85
        ),
        CrimeItem(
            id = "crime_naperstki",
            title = "Напёрстки на вокзале",
            description = "Крутить шарик под тремя колпачками. Классическая схема 90-х с подставными зеваками.",
            category = CrimeCategory.STREET,
            requiredRank = ReputationRank.SHNYR,
            minMoney = 1500,
            maxMoney = 5000,
            respectReward = 6,
            heatPenalty = 8,
            healthRisk = 10,
            baseSuccessRate = 80
        ),
        CrimeItem(
            id = "crime_palenka",
            title = "Сбыт палёной водки",
            description = "Партия спирта «Рояль» и липовые акцизы. Разлив по гаражам и сдача мелким торговцам.",
            category = CrimeCategory.STREET,
            requiredRank = ReputationRank.PATSAN,
            minMoney = 4000,
            maxMoney = 12000,
            respectReward = 10,
            heatPenalty = 12,
            healthRisk = 12,
            baseSuccessRate = 75
        ),
        CrimeItem(
            id = "crime_wheels",
            title = "Скрутить колёса с «Волги»",
            description = "Ночная вылазка во двор обкомовской номенклатуры. Импортная резина стоит кучу денег.",
            category = CrimeCategory.STREET,
            requiredRank = ReputationRank.PATSAN,
            minMoney = 6000,
            maxMoney = 15000,
            respectReward = 12,
            heatPenalty = 14,
            healthRisk = 15,
            baseSuccessRate = 72
        ),
        CrimeItem(
            id = "crime_kiosk_racket",
            title = "Рэкет ларьков и челноков",
            description = "Прийти на рынок, объяснить местным коммерсантам необходимость «братской защиты».",
            category = CrimeCategory.RACKET,
            requiredRank = ReputationRank.BYK,
            minMoney = 15000,
            maxMoney = 35000,
            respectReward = 20,
            heatPenalty = 15,
            healthRisk = 20,
            baseSuccessRate = 70,
            requiredStrength = 20
        ),
        CrimeItem(
            id = "crime_debt_collection",
            title = "Выбивание долга у кооператора",
            description = "Директор кооператива забыл вернуть долг. Прокатить в багажнике до лесополосы для беседы.",
            category = CrimeCategory.RACKET,
            requiredRank = ReputationRank.BYK,
            minMoney = 25000,
            maxMoney = 60000,
            respectReward = 25,
            heatPenalty = 20,
            healthRisk = 25,
            baseSuccessRate = 65,
            requiredStrength = 30
        ),
        CrimeItem(
            id = "crime_auto_theft",
            title = "Угон «Вишнёвой девятки» под заказ",
            description = "Вскрыть сигнализацию, перебить номера в секретном боксе и загнать перекупщикам.",
            category = CrimeCategory.HEIST,
            requiredRank = ReputationRank.BRIGADIR,
            minMoney = 50000,
            maxMoney = 120000,
            respectReward = 35,
            heatPenalty = 25,
            healthRisk = 20,
            baseSuccessRate = 60
        ),
        CrimeItem(
            id = "crime_warehouse_raid",
            title = "Налёт на таможенный склад",
            description = "Вооруженный налёт на склад с бытовой техникой Sony и видеомагнитофонами.",
            category = CrimeCategory.HEIST,
            requiredRank = ReputationRank.BRIGADIR,
            minMoney = 100000,
            maxMoney = 250000,
            respectReward = 50,
            heatPenalty = 30,
            healthRisk = 35,
            baseSuccessRate = 55,
            requiredStrength = 40
        ),
        CrimeItem(
            id = "crime_highway_intercept",
            title = "Перехват фуры на трассе",
            description = "Остановить дальнобойщиков с импортным табаком и коньяком. Действовать слаженно на двух машинах.",
            category = CrimeCategory.HEIST,
            requiredRank = ReputationRank.SMOTRYASHIY,
            minMoney = 200000,
            maxMoney = 450000,
            respectReward = 75,
            heatPenalty = 35,
            healthRisk = 30,
            baseSuccessRate = 52
        ),
        CrimeItem(
            id = "crime_factory_privatization",
            title = "Рейдерский захват завода",
            description = "Скупить ваучеры у работяг, прессануть красного директора и переоформить металлургический цех.",
            category = CrimeCategory.SYNDICATE,
            requiredRank = ReputationRank.SMOTRYASHIY,
            minMoney = 500000,
            maxMoney = 1200000,
            respectReward = 120,
            heatPenalty = 40,
            healthRisk = 30,
            baseSuccessRate = 50
        ),
        CrimeItem(
            id = "crime_bank_machinations",
            title = "Афера с фальшивыми авизо",
            description = "Банковская схема государственного масштаба через подкупленный расчетно-кассовый центр.",
            category = CrimeCategory.SYNDICATE,
            requiredRank = ReputationRank.AVTORITET,
            minMoney = 1500000,
            maxMoney = 3500000,
            respectReward = 200,
            heatPenalty = 45,
            healthRisk = 15,
            baseSuccessRate = 48
        ),
        CrimeItem(
            id = "crime_contraband_train",
            title = "Эшелон цветного металла за кордон",
            description = "Вывоз эшелонов меди и титана в Прибалтику. Огромные взятки на границе, баснословный куш в валюте.",
            category = CrimeCategory.SYNDICATE,
            requiredRank = ReputationRank.AVTORITET,
            minMoney = 3000000,
            maxMoney = 7000000,
            respectReward = 300,
            heatPenalty = 50,
            healthRisk = 25,
            baseSuccessRate = 45
        )
    )

    fun getDefaultAssets(): List<AssetItem> = listOf(
        // Weapons
        AssetItem(
            id = "wpn_kastet",
            name = "Свинцовый кастет",
            description = "Тяжёлый самодельный кастет. Веский аргумент в уличной драке.",
            category = AssetCategory.WEAPON,
            price = 1500,
            attackBonus = 8,
            respectBonus = 5
        ),
        AssetItem(
            id = "wpn_bita",
            name = "Бейсбольная бита",
            description = "Американская бейсбольная бита. Никакого бейсбола, только бизнес.",
            category = AssetCategory.WEAPON,
            price = 4000,
            attackBonus = 15,
            respectBonus = 10
        ),
        AssetItem(
            id = "wpn_pm",
            name = "Пистолет Макарова (ПМ)",
            description = "Надёжный армейский 9-мм пистолет. Удобно прятать под курткой.",
            category = AssetCategory.WEAPON,
            price = 25000,
            attackBonus = 35,
            respectBonus = 25
        ),
        AssetItem(
            id = "wpn_tt",
            name = "Пистолет ТТ (Тульский Токарев)",
            description = "Культовое оружие киллеров 90-х. Мощный патрон шьёт любые бронежилеты.",
            category = AssetCategory.WEAPON,
            price = 55000,
            attackBonus = 55,
            respectBonus = 45
        ),
        AssetItem(
            id = "wpn_obrez",
            name = "Бандитский обрез ТОЗ-34",
            description = "Спиленные стволы охотничьего ружья. Страшная огневая мощь в упор.",
            category = AssetCategory.WEAPON,
            price = 85000,
            attackBonus = 75,
            respectBonus = 60
        ),
        AssetItem(
            id = "wpn_ak47",
            name = "Автомат Калашникова АК-47",
            description = "Складной приклад, калибр 7.62. Окончательный довод на любой криминальной стрелке.",
            category = AssetCategory.WEAPON,
            price = 220000,
            attackBonus = 120,
            respectBonus = 120
        ),

        // Vehicles
        AssetItem(
            id = "car_vaz2106",
            name = "ВАЗ-2106 «Шестёрка»",
            description = "Классический советский седан. Не привлекает лишнего внимания милиции.",
            category = AssetCategory.VEHICLE,
            price = 18000,
            respectBonus = 10,
            defenseBonus = 5,
            heatReductionDaily = 2
        ),
        AssetItem(
            id = "car_vaz2109",
            name = "ВАЗ-2109 «Вишнёвая Девятка»",
            description = "Символ эпохи. Передний привод, тонировка, сабвуфер в багажнике.",
            category = AssetCategory.VEHICLE,
            price = 55000,
            respectBonus = 30,
            defenseBonus = 10,
            heatReductionDaily = 3
        ),
        AssetItem(
            id = "car_volga",
            name = "ГАЗ-31029 «Волга»",
            description = "Чёрная директорская Волга. В потоке принимают за важную шишку.",
            category = AssetCategory.VEHICLE,
            price = 110000,
            respectBonus = 60,
            defenseBonus = 15,
            heatReductionDaily = 4
        ),
        AssetItem(
            id = "car_bumer",
            name = "BMW E34 «Черный Бумер»",
            description = "Хищный баварский зверь с рядной шестёркой. Уважение на трассе обеспечено.",
            category = AssetCategory.VEHICLE,
            price = 350000,
            respectBonus = 150,
            attackBonus = 25,
            defenseBonus = 30,
            heatReductionDaily = 5
        ),
        AssetItem(
            id = "car_jeep",
            name = "Jeep Grand Cherokee «Широкий»",
            description = "Американский танк с мотором V8. Идеален для выезда бригады на стрелки в карьер.",
            category = AssetCategory.VEHICLE,
            price = 700000,
            respectBonus = 250,
            attackBonus = 40,
            defenseBonus = 45,
            heatReductionDaily = 6
        ),
        AssetItem(
            id = "car_kaban",
            name = "Mercedes W140 «Шестисотый Кабан»",
            description = "V12, двойные стеклопакеты, бронелисты. Абсолютный эталон босса мафии.",
            category = AssetCategory.VEHICLE,
            price = 1500000,
            respectBonus = 500,
            attackBonus = 60,
            defenseBonus = 80,
            heatReductionDaily = 10
        ),

        // Clothing & Status
        AssetItem(
            id = "cloth_sport",
            name = "Спортивный костюм «Abibas»",
            description = "Три полоски, кепка-восьмиклинка и туфли под спортивки. Базовый прикид.",
            category = AssetCategory.CLOTHING,
            price = 2500,
            respectBonus = 8,
            defenseBonus = 4
        ),
        AssetItem(
            id = "cloth_leather",
            name = "Кожаная куртка-бомбер",
            description = "Тяжёлая турецкая кожа. Защищает от порезов финкой и холодного ветра.",
            category = AssetCategory.CLOTHING,
            price = 22000,
            respectBonus = 25,
            defenseBonus = 15
        ),
        AssetItem(
            id = "cloth_vest",
            name = "Титановый бронежилет скрытника",
            description = "Одевается под свитер. Способен остановить пулю из ПМ или ТТ.",
            category = AssetCategory.CLOTHING,
            price = 80000,
            respectBonus = 40,
            defenseBonus = 50
        ),
        AssetItem(
            id = "cloth_chain",
            name = "Золотая цепь в два пальца",
            description = "Массивное золото 585 пробы с крестом. Наглядное свидетельство достатка.",
            category = AssetCategory.CLOTHING,
            price = 180000,
            respectBonus = 120,
            defenseBonus = 10
        ),
        AssetItem(
            id = "cloth_crimson",
            name = "Малиновый пиджак Versace",
            description = "Вершина бандитского шика. В таком встречают директоров банков и губернаторов.",
            category = AssetCategory.CLOTHING,
            price = 450000,
            respectBonus = 300,
            defenseBonus = 20
        ),

        // Businesses & Real Estate
        AssetItem(
            id = "biz_kiosk",
            name = "Ларёк «Круглосуточный»",
            description = "Сникерсы, ликер «Амаретто», сигареты «Magna». Стабильный мелкий доход каждый день.",
            category = AssetCategory.BUSINESS,
            price = 40000,
            dailyIncome = 3500,
            respectBonus = 15
        ),
        AssetItem(
            id = "biz_autoservice",
            name = "Гаражный автосервис и шиномонтаж",
            description = "Ремонт и перекраска тачек без лишних вопросов к техпаспорту.",
            category = AssetCategory.BUSINESS,
            price = 120000,
            dailyIncome = 9500,
            respectBonus = 35
        ),
        AssetItem(
            id = "biz_market",
            name = "Вещевой рынок «Лужники» (Ряд)",
            description = "Торговый ряд из 30 контейнеров с челноками. Солидный сбор дани.",
            category = AssetCategory.BUSINESS,
            price = 380000,
            dailyIncome = 28000,
            respectBonus = 80
        ),
        AssetItem(
            id = "biz_chop",
            name = "ЧОП «Беркут-Щит»",
            description = "Легальная крыша. Свои охранники с разрешениями на гладкоствол.",
            category = AssetCategory.BUSINESS,
            price = 900000,
            dailyIncome = 65000,
            respectBonus = 160,
            defenseBonus = 30,
            heatReductionDaily = 5
        ),
        AssetItem(
            id = "biz_casino",
            name = "Казино «Корона»",
            description = "Рулетка, блэкджек, бар и отмывание наличных со всего города.",
            category = AssetCategory.BUSINESS,
            price = 2500000,
            dailyIncome = 190000,
            respectBonus = 350,
            heatReductionDaily = 8
        ),
        AssetItem(
            id = "biz_port",
            name = "Нефтяной терминал в порту",
            description = "Перевалка сырой нефти и танкеры за рубеж. Неиссякаемый источник миллиардов.",
            category = AssetCategory.BUSINESS,
            price = 8000000,
            dailyIncome = 650000,
            respectBonus = 800,
            heatReductionDaily = 15
        )
    )

    fun getDefaultCrew(): List<CrewMember> = listOf(
        CrewMember(
            id = "crew_torpeda",
            name = "Колян «Торпеда»",
            role = "Боевик-штурмовик",
            description = "Бывший боксёр-перворазрядник. Ни страха, ни сомнений. В драках на передовой.",
            hireCost = 15000,
            dailySalary = 1200,
            combatBonus = 20
        ),
        CrewMember(
            id = "crew_navodchik",
            name = "Вася «Шустрый»",
            role = "Наводчик и разведчик",
            description = "Знает, кто из цеховиков прячет доллары под матрасом. Увеличивает куш от дел на 20%.",
            hireCost = 35000,
            dailySalary = 2500,
            incomePercentBonus = 20
        ),
        CrewMember(
            id = "crew_mechanic",
            name = "Дядя Юра «Золотые руки»",
            role = "Подпольный механик",
            description = "Умеет форсировать движки и перебивать номера за полчаса. Снижает риск поимки.",
            hireCost = 50000,
            dailySalary = 3200,
            heatReductionBonus = 3
        ),
        CrewMember(
            id = "crew_advokat",
            name = "Генрих Львович",
            role = "Криминальный адвокат",
            description = "Знает все лазейки в Уголовном кодексе. Любой протокол развалит ещё до суда.",
            hireCost = 150000,
            dailySalary = 9000,
            heatReductionBonus = 6
        ),
        CrewMember(
            id = "crew_major",
            name = "Майор Сорокин",
            role = "Оборотень в погонах",
            description = "Замначальника РОВД. Предупреждает о готовящихся облавах ОМОНа.",
            hireCost = 400000,
            dailySalary = 22000,
            heatReductionBonus = 12,
            combatBonus = 30
        )
    )

    fun getDefaultDistricts(): List<District> = listOf(
        District(
            id = "dist_zavod",
            name = "Заводской район",
            description = "Серые хрущёвки, промзона и бараки. Место силы уличных пацанов.",
            controlPercent = 20,
            dailyTribute = 8000,
            rivalGang = "Люберецкие"
        ),
        District(
            id = "dist_vokzal",
            name = "Вокзальная площадь",
            description = "Напёрсточники, валютчики, таксисты и вещевые развалы.",
            controlPercent = 10,
            dailyTribute = 15000,
            rivalGang = "Казанские"
        ),
        District(
            id = "dist_center",
            name = "Центральный проспект",
            description = "Банки, валютные магазины «Берёзка», дорогие рестораны и ночные клубы.",
            controlPercent = 0,
            dailyTribute = 45000,
            rivalGang = "Солнцевские"
        ),
        District(
            id = "dist_port",
            name = "Грузовой речной порт",
            description = "Склады, краны, перевалка цветных металлов и импортных контейнеров.",
            controlPercent = 0,
            dailyTribute = 90000,
            rivalGang = "Тамбовские"
        ),
        District(
            id = "dist_rublevka",
            name = "Элитный посёлок «Дубки»",
            description = "Особняки из красного кирпича с трёхметровыми заборами и охраной.",
            controlPercent = 0,
            dailyTribute = 220000,
            rivalGang = "Кавказские"
        )
    )

    fun getPrisonRiddles(): List<PrisonRiddle> = listOf(
        PrisonRiddle(
            question = "Заходишь впервые в хату, а у порога полотенце лежит. Твои действия по понятиям?",
            options = listOf(
                "Вытереть ноги о полотенце",
                "Перешагнуть и поздороваться: «Мир вашей хате»",
                "Поднять, вытереть руки и повесить на крючок",
                "Пнуть его в угол сапогом"
            ),
            correctIndex = 1,
            explanation = "По воровским понятиям полотенце у порога топтать нельзя — это проверка на вшивость. Надо перешагнуть и поприветствовать арестантов."
        ),
        PrisonRiddle(
            question = "Смотрящий за хатой спрашивает: «С какой мастью по жизни идёшь, бродяга?»",
            options = listOf(
                "Я честный трудяга с завода",
                "По понятиям живу, чужого не брал, ментам не сдавал",
                "Я вообще тут случайно, меня подставили",
                "Я начальник над вами всеми"
            ),
            correctIndex = 1,
            explanation = "Правильный ответ показывает достоинство и приверженность людскому закону."
        ),
        PrisonRiddle(
            question = "Предлагают сыграть партейку в нарды или картишки на «просто так»?",
            options = listOf(
                "Согласиться, игра скрасит время",
                "Вежливо отказаться: в тюрьме «просто так» не играют!",
                "Потребовать поставить на кон пачку сигарет",
                "Обидеться и полезть в драку"
            ),
            correctIndex = 1,
            explanation = "В тюрьме «на интерес» играть смертельно опасно. За «просто так» потом могут потребовать всё что угодно."
        ),
        PrisonRiddle(
            question = "Вертухай бросил пайку на грязный пол и ухмыляется. Что делать?",
            options = listOf(
                "Поднять и сразу съесть",
                "Оставить лежать, не прикасаясь — это западло",
                "Ударить вертухая по лицу",
                "Заплакать и просить другую"
            ),
            correctIndex = 1,
            explanation = "Осквернённую пищу бродяга не подбирает. Братва поддержит и поделится чаем и хлебом из общака."
        )
    )

    fun getRandomEvents(): List<GameEvent> = listOf(
        GameEvent(
            id = "event_fearful_merchant",
            title = "Испуганный кооператор",
            description = "В твой автосервис прибежал трясущийся владелец ювелирной мастерской: «Братки, меня казанские прессуют, требуют половину прибыли! Возьмите под крыло, заплачу щедро!»",
            options = listOf(
                GameEventOption(
                    text = "Взять под свою крышу (+Деньги, +Респект, +Розыск)",
                    outcomeDescription = "Ты выставил у его мастерской двух быков. Казанские ушли ни с чем, коммерс отстегнул 80 000 ₽.",
                    moneyDelta = 80000,
                    respectDelta = 25,
                    heatDelta = 10
                ),
                GameEventOption(
                    text = "Потребовать тройную плату за защиту",
                    outcomeDescription = "Коммерс скривился, но отдал 140 000 ₽. Правда, среди торговцев пошёл слух о твоей жадности.",
                    moneyDelta = 140000,
                    respectDelta = -10,
                    heatDelta = 12
                ),
                GameEventOption(
                    text = "Послать его: не время для войны с казанскими",
                    outcomeDescription = "Ты решил не обострять отношения. Мудрая осторожность сохранила покой.",
                    moneyDelta = 0,
                    respectDelta = -5,
                    heatDelta = -5
                )
            )
        ),
        GameEvent(
            id = "event_omon_raid",
            title = "Облава ОМОНа в ресторане «Русь»",
            description = "Ты отдыхал с бригадой в ресторане, когда двери вышибли автоматчики в масках: «Всем лежать, мордой в пол! Работает ОМОН!»",
            options = listOf(
                GameEventOption(
                    text = "Сбросить волыну под диван и лечь на пол",
                    outcomeDescription = "ОМОНовцы попинали для порядка, но улик не нашли. Отделался синяками.",
                    healthDelta = -15,
                    respectDelta = 5,
                    heatDelta = -10
                ),
                GameEventOption(
                    text = "Попытаться выпрыгнуть через окно кухни",
                    outcomeDescription = "Ты рыбкой вылетел в окно заднего двора и ушёл дворами на девятке!",
                    respectDelta = 30,
                    healthDelta = -10,
                    heatDelta = 15
                ),
                GameEventOption(
                    text = "Показать корочку внештатного сотрудника МВД",
                    outcomeDescription = "Старший группы глянул на ксиву, выругался и велел валить отсюда.",
                    respectDelta = -15,
                    heatDelta = -20
                )
            )
        ),
        GameEvent(
            id = "event_church_donation",
            title = "Батюшка просит пожертвование",
            description = "К тебе подошел отец Василий из строящегося храма святителя Николая: «Сын мой, пожертвуй на золотые купола, замоли грехи тяжкие за братву».",
            options = listOf(
                GameEventOption(
                    text = "Щедро пожертвовать 50 000 ₽ на колокола",
                    outcomeDescription = "Батюшка перекрестил тебя и благословил. На душе стало легче, грехи отпущены.",
                    moneyDelta = -50000,
                    respectDelta = 40,
                    heatDelta = -15,
                    healthDelta = 20
                ),
                GameEventOption(
                    text = "Дать 5 000 ₽ на свечки",
                    outcomeDescription = "Батюшка поблагодарил скромно. Капля в море, но жест зачтён.",
                    moneyDelta = -5000,
                    respectDelta = 10,
                    heatDelta = -5
                ),
                GameEventOption(
                    text = "«Бог подаст, святой отец. Деньги нужны на патроны»",
                    outcomeDescription = "Батюшка вздохнул и ушёл. Пацаны посмотрели с лёгким осуждением.",
                    respectDelta = -15,
                    heatDelta = 5
                )
            )
        ),
        GameEvent(
            id = "event_shodnyak_invite",
            title = "Приглашение на Воровской сходняк",
            description = "Старый законник дед Хасан прислал маляву: в загородной бане собираются авторитеты делить сферы влияния в городе.",
            options = listOf(
                GameEventOption(
                    text = "Приехать в строгом костюме с дорогим подарком",
                    outcomeDescription = "Ты преподнес золотые швейцарские часы. Старики оценили уважение к старшим.",
                    moneyDelta = -100000,
                    respectDelta = 80,
                    heatDelta = 10
                ),
                GameEventOption(
                    text = "Явиться с вооруженным эскортом на бумерах",
                    outcomeDescription = "Ты показал реальную силу своей бригады. С твоим мнением стали считаться.",
                    respectDelta = 60,
                    heatDelta = 20
                ),
                GameEventOption(
                    text = "Сказаться больным и прислать доверенного человека",
                    outcomeDescription = "Осторожность спасла от скандала, но авторитеты шепчутся, что ты боишься.",
                    respectDelta = -20,
                    heatDelta = -5
                )
            )
        )
    )

    // Save & Load Player State
    suspend fun saveGameState(
        nickname: String,
        money: Long,
        respect: Int,
        health: Int,
        policeHeat: Int,
        strength: Int,
        influence: Int,
        day: Int,
        isJailed: Boolean,
        jailDaysLeft: Int,
        purchasedAssetIds: Set<String>,
        equippedAssetIds: Set<String>,
        hiredCrewIds: Set<String>,
        districtControl: Map<String, Int>,
        stats: PlayerStats,
        history: List<GameHistoryEntry>
    ) {
        // Save hero data with core stats: Сила, Авторитет, Деньги, Здоровье in Room Database
        val characterEntity = CharacterEntity(
            id = 1,
            nickname = nickname,
            strength = strength,
            respect = respect,
            money = money,
            health = health,
            policeHeat = policeHeat,
            influence = influence,
            day = day,
            isJailed = isJailed,
            jailDaysLeft = jailDaysLeft,
            rankTitle = ReputationRank.fromRespect(respect).title,
            totalCrimes = stats.totalCrimes,
            successfulCrimes = stats.successfulCrimes,
            showdownsWon = stats.showdownsWon,
            totalEarned = stats.totalEarned,
            timesArrested = stats.timesArrested,
            daysSurvived = stats.daysSurvived
        )
        characterDao.insertOrUpdate(characterEntity)

        val editor = prefs.edit()
        editor.putString("nickname", nickname)
        editor.putLong("money", money)
        editor.putInt("respect", respect)
        editor.putInt("health", health)
        editor.putInt("policeHeat", policeHeat)
        editor.putInt("strength", strength)
        editor.putInt("influence", influence)
        editor.putInt("day", day)
        editor.putBoolean("isJailed", isJailed)
        editor.putInt("jailDaysLeft", jailDaysLeft)
        editor.putStringSet("purchasedAssetIds", purchasedAssetIds)
        editor.putStringSet("equippedAssetIds", equippedAssetIds)
        editor.putStringSet("hiredCrewIds", hiredCrewIds)

        // Save districts
        val distObj = JSONObject()
        districtControl.forEach { (k, v) -> distObj.put(k, v) }
        editor.putString("districtControl", distObj.toString())

        // Save stats
        editor.putInt("stat_totalCrimes", stats.totalCrimes)
        editor.putInt("stat_successfulCrimes", stats.successfulCrimes)
        editor.putInt("stat_showdownsWon", stats.showdownsWon)
        editor.putLong("stat_totalEarned", stats.totalEarned)
        editor.putInt("stat_timesArrested", stats.timesArrested)
        editor.putInt("stat_daysSurvived", stats.daysSurvived)

        // Save history as JSON array (last 30 items)
        val historyArr = JSONArray()
        history.takeLast(30).forEach { item ->
            val obj = JSONObject()
            obj.put("day", item.day)
            obj.put("text", item.text)
            obj.put("isPositive", item.isPositive)
            historyArr.put(obj)
        }
        editor.putString("history", historyArr.toString())
        editor.apply()
    }

    suspend fun hasSavedGame(): Boolean = characterDao.getCharacterSync() != null || prefs.contains("nickname")

    suspend fun clearSavedGame() {
        characterDao.deleteCharacter()
        prefs.edit().clear().apply()
    }

    suspend fun loadGameState(): SavedState {
        val roomHero = characterDao.getCharacterSync()
        val nickname = roomHero?.nickname ?: prefs.getString("nickname", "Седой") ?: "Седой"
        val money = roomHero?.money ?: prefs.getLong("money", 5000L)
        val respect = roomHero?.respect ?: prefs.getInt("respect", 10)
        val health = roomHero?.health ?: prefs.getInt("health", 100)
        val policeHeat = roomHero?.policeHeat ?: prefs.getInt("policeHeat", 5)
        val strength = roomHero?.strength ?: prefs.getInt("strength", 25)
        val influence = roomHero?.influence ?: prefs.getInt("influence", 10)
        val day = roomHero?.day ?: prefs.getInt("day", 1)
        val isJailed = roomHero?.isJailed ?: prefs.getBoolean("isJailed", false)
        val jailDaysLeft = roomHero?.jailDaysLeft ?: prefs.getInt("jailDaysLeft", 0)
        val purchasedAssetIds = prefs.getStringSet("purchasedAssetIds", emptySet()) ?: emptySet()
        val equippedAssetIds = prefs.getStringSet("equippedAssetIds", emptySet()) ?: emptySet()
        val hiredCrewIds = prefs.getStringSet("hiredCrewIds", emptySet()) ?: emptySet()

        val districtControl = mutableMapOf<String, Int>()
        val distStr = prefs.getString("districtControl", null)
        if (distStr != null) {
            try {
                val obj = JSONObject(distStr)
                obj.keys().forEach { k -> districtControl[k] = obj.getInt(k) }
            } catch (_: Exception) {
            }
        }

        val stats = PlayerStats(
            totalCrimes = prefs.getInt("stat_totalCrimes", 0),
            successfulCrimes = prefs.getInt("stat_successfulCrimes", 0),
            showdownsWon = prefs.getInt("stat_showdownsWon", 0),
            totalEarned = prefs.getLong("stat_totalEarned", 5000L),
            timesArrested = prefs.getInt("stat_timesArrested", 0),
            daysSurvived = prefs.getInt("stat_daysSurvived", 1)
        )

        val history = mutableListOf<GameHistoryEntry>()
        val histStr = prefs.getString("history", null)
        if (histStr != null) {
            try {
                val arr = JSONArray(histStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    history.add(
                        GameHistoryEntry(
                            day = obj.getInt("day"),
                            text = obj.getString("text"),
                            isPositive = obj.getBoolean("isPositive")
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        return SavedState(
            nickname = nickname,
            money = money,
            respect = respect,
            health = health,
            policeHeat = policeHeat,
            strength = strength,
            influence = influence,
            day = day,
            isJailed = isJailed,
            jailDaysLeft = jailDaysLeft,
            purchasedAssetIds = purchasedAssetIds,
            equippedAssetIds = equippedAssetIds,
            hiredCrewIds = hiredCrewIds,
            districtControl = districtControl,
            stats = stats,
            history = history
        )
    }

    data class SavedState(
        val nickname: String,
        val money: Long,
        val respect: Int,
        val health: Int,
        val policeHeat: Int,
        val strength: Int,
        val influence: Int,
        val day: Int,
        val isJailed: Boolean,
        val jailDaysLeft: Int,
        val purchasedAssetIds: Set<String>,
        val equippedAssetIds: Set<String>,
        val hiredCrewIds: Set<String>,
        val districtControl: Map<String, Int>,
        val stats: PlayerStats,
        val history: List<GameHistoryEntry>
    )
}

enum class SoundType {
    CLICK, CASH, PUNCH, SHOT, SIREN, VICTORY
}
