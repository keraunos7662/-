package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.model.BossChatMessage
import com.example.model.MessageSender
import com.example.model.ReputationRank
import com.example.model.RivalBoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Service managing AI communications with 90s Russian criminal bosses.
 * Supports dynamic hidden patience/irritation meter: when boss patience runs out,
 * an attack is triggered.
 */
class GeminiBossService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Store last used fallback reply to avoid repeating phrases in a row
    private val lastReplies = mutableMapOf<String, String>()

    suspend fun getBossResponse(
        boss: RivalBoss,
        playerMessage: String,
        history: List<BossChatMessage>,
        heroNickname: String,
        heroRank: ReputationRank,
        heroRespect: Int,
        heroMoney: Long,
        districtsCount: Int,
        currentIrritation: Int = 0
    ): BossReply = withContext(Dispatchers.IO) {
        val irritationCalc = calculateIrritationDelta(playerMessage)
        val newIrritation = (currentIrritation + irritationCalc).coerceIn(0, 100)
        val willAttack = newIrritation >= 100

        val apiKey = resolveApiKey()
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            val modelsToTry = listOf("gemini-3.1-flash-lite-preview", "gemini-flash-latest")
            for (model in modelsToTry) {
                try {
                    val replyText = callGeminiApi(
                        model = model,
                        apiKey = apiKey,
                        boss = boss,
                        playerMessage = playerMessage,
                        history = history,
                        heroNickname = heroNickname,
                        heroRank = heroRank,
                        heroRespect = heroRespect,
                        heroMoney = heroMoney,
                        districtsCount = districtsCount,
                        currentIrritation = newIrritation,
                        willAttack = willAttack
                    )
                    if (replyText.isNotBlank()) {
                        val finalReply = if (willAttack && !isExplicitAttackText(replyText)) {
                            replyText.trim() + " Всё, фраер, моё терпение лопнуло! Братва, валите его!"
                        } else {
                            replyText.trim()
                        }
                        val sentimentDelta = calculateSentimentDelta(finalReply, playerMessage)
                        return@withContext BossReply(
                            text = finalReply,
                            sentimentDelta = sentimentDelta,
                            isAiGenerated = true,
                            irritationDelta = irritationCalc,
                            isAttackTriggered = willAttack
                        )
                    }
                } catch (e: Exception) {
                    Log.w("GeminiBossService", "Model $model failed: ${e.message}")
                }
            }
        }

        // Smart procedural fallback if API fails or offline
        val fallbackText = generateProceduralResponse(
            boss = boss,
            message = playerMessage,
            heroNickname = heroNickname,
            heroRank = heroRank,
            heroRespect = heroRespect,
            currentIrritation = newIrritation,
            willAttack = willAttack
        )
        lastReplies[boss.id] = fallbackText
        val sentimentDelta = calculateSentimentDelta(fallbackText, playerMessage)
        BossReply(
            text = fallbackText,
            sentimentDelta = sentimentDelta,
            isAiGenerated = false,
            irritationDelta = irritationCalc,
            isAttackTriggered = willAttack
        )
    }

    private fun resolveApiKey(): String {
        return try {
            val keyFromBuildConfig = BuildConfig.GEMINI_API_KEY
            if (keyFromBuildConfig.isNotBlank() && keyFromBuildConfig != "MY_GEMINI_API_KEY") {
                keyFromBuildConfig
            } else {
                System.getenv("GEMINI_API_KEY").orEmpty()
            }
        } catch (_: Exception) {
            System.getenv("GEMINI_API_KEY").orEmpty()
        }
    }

    private fun callGeminiApi(
        model: String,
        apiKey: String,
        boss: RivalBoss,
        playerMessage: String,
        history: List<BossChatMessage>,
        heroNickname: String,
        heroRank: ReputationRank,
        heroRespect: Int,
        heroMoney: Long,
        districtsCount: Int,
        currentIrritation: Int,
        willAttack: Boolean
    ): String {
        val attackInstruction = if (willAttack) {
            "ТВОЁ ТЕРПЕНИЕ ОКОНЧАТЕЛЬНО ЛОПНУЛО (100%)! Ты в ярости объявляешь немедленное нападение! Скомандуй своим бойцам напасть на игрока и достать оружие (например: «Всё, падла, договорился! Братва, валите фраера наглухо!» или «Харе базар разводить, ломайте ему хребет!»)."
        } else if (currentIrritation >= 70) {
            "Твоё терпение на исходе ($currentIrritation% раздражения)! Говори предельно агрессивно, открыто угрожай расправой, скажи, что пацаны уже сняли волыны с предохранителя и ещё одно слово — и начнётся бойня."
        } else if (currentIrritation >= 40) {
            "Ты напряжён и раздражён ($currentIrritation% раздражения). Осади собеседника, предупреди, что ходит по тонкому льду и терпение у тебя не резиновое."
        } else {
            "Ты контролируешь ситуацию, спокоен, но строг. Оценивай базар с позиции криминального босса."
        }

        val systemInstructionText = """
            Ты — криминальный авторитет эпохи лихих 90-х в России по имени ${boss.name} (${boss.title}).
            Характер и стиль: ${boss.description}.
            Опасность: ${boss.difficultyLabel}.
            
            Игрок:
            - Кличка: $heroNickname
            - Статус: ${heroRank.title}
            - Уважение: $heroRespect
            - Капитал: $heroMoney рублей
            - Районов под контролем: $districtsCount
            
            СОСТОЯНИЕ ТЕРПЕНИЯ:
            $attackInstruction
            
            ПРАВИЛА:
            1. Отвечай СТРОГО от первого лица на аутентичном блатном жаргоне 90-х («фраер», «масть», «общак», «по понятиям», «стрела», «крыша», «фильтруй базар», «перо под ребро», «за базар ответишь»).
            2. Длина: 1-3 сочных предложения.
            3. Если игрок угощает папиросой («Беломор», курево) или проявляет уважение — чуть смягчись, но не теряй авторитета.
            4. Если игрок дерзит, оскорбляет — сразу ставь на место!
            5. Будь эмоциональным, живым, не повторяйся, реагируй точно на слова игрока.
        """.trimIndent()

        val contentsArray = JSONArray()

        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.sender == MessageSender.PLAYER) "user" else "model"
            val partObj = JSONObject().put("text", msg.text)
            val contentObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }

        val currentPart = JSONObject().put("text", playerMessage)
        val currentContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(currentPart))
        contentsArray.put(currentContent)

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText))))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("topP", 0.95)
                put("maxOutputTokens", 220)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val body = rootJson.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw RuntimeException("Gemini HTTP ${response.code}: $errorBody")
            }
            val responseString = response.body?.string().orEmpty()
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates") ?: return ""
            val firstCandidate = candidates.optJSONObject(0) ?: return ""
            val content = firstCandidate.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""
            
            var result = ""
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                val text = part.optString("text", "")
                if (text.isNotBlank()) {
                    result += (if (result.isEmpty()) "" else "\n") + text
                }
            }
            return result.trim()
        }
    }

    private fun isExplicitAttackText(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("валите") || lower.contains("мочи") || lower.contains("ломайте") ||
                lower.contains("к бою") || lower.contains("огонь") || lower.contains("напасть") ||
                lower.contains("завалить")
    }

    fun calculateIrritationDelta(playerMsg: String): Int {
        val lower = playerMsg.lowercase().trim()

        // Extreme insults: triggers massive jump in irritation (+45 to +55)
        val extremeInsults = listOf(
            "лох", "lox", "мусор", "петух", "чушок", "козёл", "козел", "чмо", "шестёрка", "шестерка",
            "черт", "чёрт", "сука", "мразь", "урод", "пидор", "дебил", "тварь", "гандон", "баран",
            "крыса", "шнырь", "быдло", "гнида", "сдохни", "гниль", "падла", "заткнись", "соси"
        )
        if (extremeInsults.any { lower.contains(it) }) {
            return 50
        }

        // Defiance, threats, arrogance (+25 to +35)
        val provocations = listOf(
            "перо", "грохну", "завалю", "по зубам", "быкуешь", "быковать", "не боюсь", "ты кто такой",
            "плевать", "пох", "отвали", "порву", "морду набью", "сам пошёл", "сам пошел", "щенок", "кусок",
            "дрыщ", "чепуха", "шакал"
        )
        if (provocations.any { lower.contains(it) }) {
            return 30
        }

        // De-escalation, respect, cigarettes, peace (-20 to -35)
        val respectful = listOf(
            "папирос", "беломор", "закури", "курево", "угощайся", "уважаемый", "с уважением", "мир вашему",
            "договоримся", "мирно", "извини", "прости", "согласен", "бабки принёс", "доля", "общий бизнес",
            "союз", "по-братски", "чайку", "выпьем", "здравствуй"
        )
        if (respectful.any { lower.contains(it) }) {
            return -25
        }

        // Normal banter slowly tires a busy boss (+7)
        return 7
    }

    /**
     * Procedural generator used when offline or without internet
     */
    private fun generateProceduralResponse(
        boss: RivalBoss,
        message: String,
        heroNickname: String,
        heroRank: ReputationRank,
        heroRespect: Int,
        currentIrritation: Int,
        willAttack: Boolean
    ): String {
        val lower = message.lowercase().trim()
        val lastUsed = lastReplies[boss.id].orEmpty()

        // 1. Attack condition reached
        if (willAttack) {
            val attackReplies = listOf(
                "Всё, фраер, моё терпение лопнуло! Братва, валите его наглухо!",
                "Харе базар разводить, ломайте ему хребет прямо здесь!",
                "Ты допизделся, смертник! Пацаны, стволы к бою, мочи урода!",
                "Я предупреждал! Братва, вяжите его и в багажник, живым не выпускать!"
            )
            return pickNonRepeating(attackReplies, lastUsed)
        }

        // 2. High irritation warnings (70..99)
        if (currentIrritation >= 70) {
            val warningReplies = listOf(
                "Ты берега попутал, фраерок?! Моё терпение на нуле! Ещё одно кривое слово — и мои орлы тебя здесь же закопают!",
                "Ты смерти ищешь?! Братухи за спиной уже стволы с предохранителя сняли. Последнее предупреждение!",
                "Фильтруй базар, пацан, иначе живым с этой стрелки не выйдешь. Я дважды не повторяю!",
                "Ты чё, бессмертный? Рука на кобуре лежит, ещё полсекунды твоей борзоты — и будет стрельба!"
            )
            return pickNonRepeating(warningReplies, lastUsed)
        }

        // 3. Smoke / Cigarette offer
        if (lower.contains("беломор") || lower.contains("папирос") || lower.contains("курев") || lower.contains("закури") || lower.contains("угощай")) {
            val smokeReplies = listOf(
                "Дёрнул дымка... «Беломор» — наш табак, душевный. Ладно, чутка попустило. Говори дальше, но по делу.",
                "Уважил старика куревом. Ладно, слушаю тебя внимательно, пацан. Чё за тему трёшь?",
                "Закурил... Хорошо пошла. Умеешь подход найти. Выкладывай, с чем пришёл."
            )
            return pickNonRepeating(smokeReplies, lastUsed)
        }

        // 4. Insults and attacks
        val isInsult = lower.contains("лох") || lower.contains("мусор") || lower.contains("козел") ||
                lower.contains("сука") || lower.contains("петух") || lower.contains("чушок") ||
                lower.contains("чмо") || lower.contains("шестерк") || lower.contains("быку")
        if (isInsult) {
            val insultReplies = when (boss.id) {
                "boss_shtyr" -> listOf(
                    "Ты кого оскорбил, шакал?! Мои джигиты тебя из багажника доставать будут!",
                    "За такие гнилые слова в наши годы языки отрезали. Ты по тонкому льду ходишь!",
                    "Ты на кого пасть разинул, щегол? Смотри, как бы в лесополосе рассвет не встретил!"
                )
                "boss_kastet" -> listOf(
                    "Слышь ты, мешок с костями! Я тебе кадык одним пальцем вырву, если пасть не закроешь!",
                    "Ты чё вякнул? Мои пацаны из Люберецкой качалки тебя в узел завяжут!",
                    "Осади назад, пока челюсть на месте! За базар отвечать будешь перед всей бригадой!"
                )
                "boss_klyk" -> listOf(
                    "На вокзале за такой базар сразу перо под ребро ловили. Фильтруй хрюкало, фраерок!",
                    "Ты с кем так разговариваешь, падла? У меня на перроне сотня бойцов по свистку встанет!",
                    "Ещё одно кривое слово — и поедешь в товарняке в один конец. Понял меня?!"
                )
                "boss_baron" -> listOf(
                    "Я рынком и автобазой рулю, пока ты в песочнице сидел. Базар фильтруй, иначе счётчик включу!",
                    "Дерзкий больно? Мои братки таких дерзких в бетон закатывали. Не искушай судьбу!",
                    "Ты авторитета попутал, пацан. За оскорбление старших спрос будет кровавый."
                )
                else -> listOf(
                    "В воровском мире за такие слова кровью платят. Я предупредил один раз.",
                    "Ты переходишь черту. Моё терпение не безгранично, фильтруй базар.",
                    "Оскорбления оставь для дворовых пацанов. Здесь взрослый разговор с авторитетами."
                )
            }
            return pickNonRepeating(insultReplies, lastUsed)
        }

        // 5. Business and alliances
        val isBusiness = lower.contains("бизнес") || lower.contains("дело") || lower.contains("союз") ||
                lower.contains("бабк") || lower.contains("процент") || lower.contains("доля") || lower.contains("мир")
        if (isBusiness) {
            val businessReplies = when (boss.id) {
                "boss_shtyr" -> listOf(
                    "Разговор деловой, одобряю. С ларьков и точек на районе мне 30% в общак. Готов потянуть?",
                    "Союз — дело верное. Но слово вора крепче гранита: кинешь — спрошу свинцом. Каковы условия?",
                    "Вижу хватку, $heroNickname. Подкинь котлету зелени в общак, и мои бойцы прикроют твой тыл."
                )
                "boss_kastet" -> listOf(
                    "Бизнес я уважаю, когда бабло звонкое. Нам в качалку на протеин и железо долю заноси — будем крышевать.",
                    "Если тема реальная — Люберецкие подпишутся. Но если ментов на хвосте притащишь — сам закопаю.",
                    "Общий интерес — это по-пацански. Какую поляну делить будем?"
                )
                "boss_klyk" -> listOf(
                    "Контрабанда с поездов и вокзальные ларьки дают солидный навар. Если готов в долю войти — давай мазу держать.",
                    "Слышу разумную речь. На вокзале мир дороже войны. Давай прикинем барыши.",
                    "Я за толковый расклад. Если не крысятничать — поднимем куш на двоих."
                )
                "boss_baron" -> listOf(
                    "Торговля и фуры с товаром — это золотое дно. Занеси вступительные в общак, и будешь при делах.",
                    "Грамотный подход. Я держу склады и автобазу. Что взамен предложишь, авторитет?",
                    "Деньги любят порядок. Договариваемся на берегу: моё слово — закон на рынке."
                )
                else -> listOf(
                    "Разумное предложение. В нашем кругу ценят людей, умеющих считать прибыль.",
                    "Согласен обсудить долю. Город большой, кусок пирога найдётся каждому достойному.",
                    "По рукам, если условия честные. Держи марку и не подводи братву."
                )
            }
            return pickNonRepeating(businessReplies, lastUsed)
        }

        // 6. General / Background talk
        val generalReplies = when (boss.id) {
            "boss_shtyr" -> listOf(
                "Слышь, $heroNickname, я с авторитетами ещё в восьмидесятых на Колыме чай гонял. Говори конкретно, чё надо.",
                "Держи ухо востро. В районе менты облавы начали, так что если есть вопросы — решай шустро.",
                "Ты парень с понятием, вижу по глазам. Но город под нами, так что старших надо уважать."
            )
            "boss_kastet" -> listOf(
                "Мы в Люберцах слабаков не держим. Хочешь со мной тереть — покажи силу и характер.",
                "Я тут с пацанами железо тягал. Не отвлекай по пустякам, выкладывай суть.",
                "Держи спину прямо, когда с бригадиром говоришь. Чего припёрся?"
            )
            "boss_klyk" -> listOf(
                "На вокзале шум, поезда гудят, жизнь кипит. Что у тебя за тема, $heroNickname?",
                "Тут каждый угол простреливается. Веди себя скромно, и всё будет ровно.",
                "Мои орлы за каждым твоим шагом следят. Говори, с чем пожаловал."
            )
            "boss_baron" -> listOf(
                "Счётчик включить никогда не поздно. Говори по делу: что предлагаешь и какую долю готов отдать?",
                "Авторитет у тебя растёт, вижу. Но не зарывайся — город тесный, на всех коммерсантов не напасёшься.",
                "Рынок кормит полгорода. Хочешь уважения — веди себя как подобает солидному человеку."
            )
            else -> listOf(
                "Я ценю людей с головой на плечах. Деньги любят тишину, а авторитет требует безупречной репутации.",
                "В криминальном мире выживают не самые сильные, а самые дальновидные. Что у тебя на уме?",
                "Говори по существу. Моё время стоит дорого, а терпение смотрящего за городом имеет строгие границы.",
                "Город принадлежит тем, кто умеет договариваться. Слушаю твои условия."
            )
        }

        return pickNonRepeating(generalReplies, lastUsed)
    }

    private fun pickNonRepeating(options: List<String>, lastUsed: String): String {
        val filtered = options.filter { it != lastUsed }
        val pool = if (filtered.isNotEmpty()) filtered else options
        return pool[Random.nextInt(pool.size)]
    }

    private fun calculateSentimentDelta(reply: String, playerMsg: String): Int {
        val lowerReply = reply.lowercase()
        val lowerMsg = playerMsg.lowercase()

        var delta = 0
        if (lowerMsg.contains("деньги") || lowerMsg.contains("откуп") || lowerMsg.contains("уважение") ||
            lowerMsg.contains("мир") || lowerMsg.contains("доля") || lowerMsg.contains("беломор") ||
            lowerMsg.contains("папирос") || lowerMsg.contains("курев")
        ) {
            delta += 15
        }
        if (lowerMsg.contains("лох") || lowerMsg.contains("lox") || lowerMsg.contains("фраер") ||
            lowerMsg.contains("убью") || lowerMsg.contains("чушок") || lowerMsg.contains("мусор") ||
            lowerMsg.contains("козел") || lowerMsg.contains("сука") || lowerMsg.contains("быку")
        ) {
            delta -= 25
        }
        if (lowerReply.contains("кореш") || lowerReply.contains("разумн") || lowerReply.contains("дело") ||
            lowerReply.contains("союз") || lowerReply.contains("по-братски") || lowerReply.contains("уважил")
        ) {
            delta += 10
        }
        if (lowerReply.contains("заточк") || lowerReply.contains("перо") || lowerReply.contains("мокрое место") ||
            lowerReply.contains("осади") || lowerReply.contains("могил") || lowerReply.contains("челюсть") ||
            lowerReply.contains("валите") || lowerReply.contains("мочи")
        ) {
            delta -= 20
        }
        return delta
    }
}

data class BossReply(
    val text: String,
    val sentimentDelta: Int,
    val isAiGenerated: Boolean,
    val irritationDelta: Int = 7,
    val isAttackTriggered: Boolean = false
)
