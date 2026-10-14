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
 * Uses Gemini 3.5 Flash via REST API with fallback to procedural dialogue generator.
 */
class GeminiBossService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getBossResponse(
        boss: RivalBoss,
        playerMessage: String,
        history: List<BossChatMessage>,
        heroNickname: String,
        heroRank: ReputationRank,
        heroRespect: Int,
        heroMoney: Long,
        districtsCount: Int
    ): BossReply = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val replyText = callGeminiApi(
                    apiKey = apiKey,
                    boss = boss,
                    playerMessage = playerMessage,
                    history = history,
                    heroNickname = heroNickname,
                    heroRank = heroRank,
                    heroRespect = heroRespect,
                    heroMoney = heroMoney,
                    districtsCount = districtsCount
                )
                if (replyText.isNotBlank()) {
                    val sentimentDelta = calculateSentimentDelta(replyText, playerMessage)
                    return@withContext BossReply(
                        text = replyText.trim(),
                        sentimentDelta = sentimentDelta,
                        isAiGenerated = true
                    )
                }
            } catch (e: Exception) {
                Log.w("GeminiBossService", "Gemini API call failed, falling back to procedural engine: ${e.message}")
            }
        }

        // Procedural atmospheric response fallback
        val fallbackText = generateProceduralResponse(boss, playerMessage, heroRank, heroRespect)
        val sentimentDelta = calculateSentimentDelta(fallbackText, playerMessage)
        BossReply(
            text = fallbackText,
            sentimentDelta = sentimentDelta,
            isAiGenerated = false
        )
    }

    private fun callGeminiApi(
        apiKey: String,
        boss: RivalBoss,
        playerMessage: String,
        history: List<BossChatMessage>,
        heroNickname: String,
        heroRank: ReputationRank,
        heroRespect: Int,
        heroMoney: Long,
        districtsCount: Int
    ): String {
        val systemInstructionText = """
            Ты — криминальный авторитет эпохи лихих 90-х в России.
            Твоё имя: ${boss.name} (${boss.title}).
            Твоя характеристика: ${boss.description}.
            Твой уровень опасности: ${boss.difficultyLabel}.
            
            Перед тобой на стрелке/переговорах стоит игрок по кличке '$heroNickname'.
            Статус игрока:
            - Ранг по масти: ${heroRank.title}
            - Авторитет: $heroRespect
            - Капитал: $heroMoney рублей
            - Контролирует районов: $districtsCount
            
            Правила ответа:
            1. Говори строго от лица ${boss.name}, используя аутентичный криминальный сленг 90-х («фраер», «масть», «общак», «по понятиям», «стрела», «крыша», «базар фильтруй», «браток» и т.п.).
            2. Ответ должен быть лаконичным, сочным и атмосферным: ровно 2-3 предложения.
            3. Никаких современных выражений и искусственности.
            4. Если игрок дерзит, осади его или пригрози стволом/разборкой. Если игрок предлагает деньги или союз с высоким авторитетом, оцени выгоду для своей группировки.
        """.trimIndent()

        val contentsArray = JSONArray()

        // Include last 6 messages for context
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.sender == MessageSender.PLAYER) "user" else "model"
            val partObj = JSONObject().put("text", msg.text)
            val contentObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }

        // Add current user message
        val currentPart = JSONObject().put("text", playerMessage)
        val currentContent = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(currentPart))
        contentsArray.put(currentContent)

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText))))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
                put("topP", 0.95)
                put("maxOutputTokens", 200)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
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
            val firstPart = parts.optJSONObject(0) ?: return ""
            return firstPart.optString("text", "").trim()
        }
    }

    /**
     * Atmospheric procedural fallback generator if offline or no API key set
     */
    private fun generateProceduralResponse(
        boss: RivalBoss,
        message: String,
        heroRank: ReputationRank,
        heroRespect: Int
    ): String {
        val lower = message.lowercase()

        return when (boss.id) {
            "boss_shtyr" -> {
                when {
                    lower.contains("мир") || lower.contains("договор") ->
                        "Слышь, мирный нашёлся! С тебя пачка «Беломора» и штука деревянных за проход через гаражи, тогда разойдёмся."
                    lower.contains("деньг") || lower.contains("бабк") || lower.contains("доля") ->
                        "Опа, шуршишь купюрами? Гони долю, и пацаны с района тебя пальцем не тронут, зуб даю!"
                    lower.contains("кто") || lower.contains("под кем") ->
                        "Я под Серым Клыком хожу, усёк? А ты чо за вася с бугра, авторитет тут качаешь?"
                    lower.contains("стрел") || lower.contains("драть") || lower.contains("бой") ->
                        "Ах ты фраер борзый! Доставай перо, щас я тебе спортивки заточкой проветрю!"
                    else -> {
                        val replies = listOf(
                            "Слышь, базар фильтруй! Ты кому чешешь, я на этих гаражах с пелёнок заправляюсь!",
                            "Чо зенки вылупил? Есть закурить? А если найду в карманах лишнюю котлету рублей?",
                            "Понятия знаешь? Тут каждый второй фраер думает, что авторитет, а потом с фингалом под забором валяется!"
                        )
                        replies[Random.nextInt(replies.size)]
                    }
                }
            }
            "boss_kastet" -> {
                when {
                    lower.contains("мир") || lower.contains("договор") ->
                        "Мир с дрыщами не заключаю! Хочешь уважения — пожми от груди соточку или выстави ящик пива для качалки."
                    lower.contains("деньг") || lower.contains("доля") || lower.contains("бизнес") ->
                        "Бабки — это тема. Мы ларьки держим крепко. Отстегнёшь двадцать кусков на спортпит — разговор другой будет."
                    lower.contains("стрел") || lower.contains("в торец") || lower.contains("бой") ->
                        "Ха! Ну давай, выходи раз на раз! Мой чугунный кастет давно по зубам соскучился!"
                    else -> {
                        val replies = listOf(
                            "Ты чё такой дерзкий? Я в подвале Люберец железо тягал, когда ты ещё в песочнице сидел!",
                            "Слышь, братуха, мускулы береги. Тут разговоры короткие: слово не то — сразу в челюсть!",
                            "Бизнес у нас простой: кто сильнее, тот и прав. А судя по твоей комплекции, прав сегодня я!"
                        )
                        replies[Random.nextInt(replies.size)]
                    }
                }
            }
            "boss_klyk" -> {
                when {
                    lower.contains("общак") || lower.contains("доля") || lower.contains("бизнес") ->
                        "Дело говоришь, браток. На вокзале напёрстки и лотки приносят солидный куш. Вложишься в общак — получишь процент."
                    lower.contains("мир") || lower.contains("по понятиям") ->
                        "По понятиям — это правильно. Кровь лить без толку никому не с руки. Главное, чужую поляну не топтать."
                    lower.contains("менты") || lower.contains("легавые") ->
                        "Менты у нас на окладе, вокзальный отдел прикормлен. Но лишнего шума не поднимай, пули дуры."
                    lower.contains("стрел") || lower.contains("убью") || lower.contains("пул") ->
                        "Ты на бригадира голос повысил?! Мой обрез осечек не даёт. Подумай дважды, пока живой."
                    else -> {
                        val replies = listOf(
                            "Базар веди солидно. Я на пересылках полжизни отмотал, меня дешёвыми понтами не взять.",
                            "Вокзал — место суетное. Тут либо ты стрижёшь лохов, либо тебя стригут под ноль. Кем будешь?",
                            "Уважение зарабатывается не языком, а делами. Докажи, что твоё слово чего-то стоит в нашем городе."
                        )
                        replies[Random.nextInt(replies.size)]
                    }
                }
            }
            "boss_baron" -> {
                when {
                    lower.contains("рынок") || lower.contains("автобаз") || lower.contains("доля") ->
                        "Рынок и автобаза — моя вотчина. Каждый коммерсант платит вовремя. Хочешь свой процент? Покажи свой авторитет."
                    lower.contains("мир") || lower.contains("переговоры") ->
                        "Переговоры люблю. Когда они подкреплены пачками купюр. В знак уважения занеси полтинник в общак, и будем корешами."
                    lower.contains("стрел") || lower.contains("войн") ->
                        "Войну захотел? Моя бригада на трёх девятках приедет — от твоей банды только мокрое место останется."
                    else -> {
                        val replies = listOf(
                            "Ты под кем ходишь, пацан? В этом районе без моей санкции даже собака не гавкает.",
                            "Счётчик включить никогда не поздно. Говори конкретно: что предлагаешь и какую долю готов отдать?",
                            "Авторитет у тебя растёт, вижу. Но не зарывайся — город тесный, на всех коммерсантов не напасёшься."
                        )
                        replies[Random.nextInt(replies.size)]
                    }
                }
            }
            "boss_mramor" -> {
                when {
                    lower.contains("центр") || lower.contains("власть") || lower.contains("город") ->
                        "Центр города — это высшая лига. Здесь решаются вопросы на миллионы. Готов ли ты играть по крупным ставкам, коллега?"
                    lower.contains("мир") || lower.contains("союз") || lower.contains("уважение") ->
                        "Разумный подход. Война разоряет бизнес, мир приносит дивиденды. Предлагаю разделить контроль над банками и казино."
                    lower.contains("угроза") || lower.contains("стрел") || lower.contains("смерть") ->
                        "Опрометчивые слова. Мой пистолет Стечкина не знает промаха, а на городском кладбище много свободных мест."
                    else -> {
                        val replies = listOf(
                            "Я ценю людей с головой на плечах. Деньги любят тишину, а авторитет требует безупречной репутации.",
                            "В криминальном мире выживают не самые сильные, а самые дальновидные. Что у тебя на уме?",
                            "Говори по существу. Моё время стоит дорого, а терпение смотрящего за городом имеет строгие границы."
                        )
                        replies[Random.nextInt(replies.size)]
                    }
                }
            }
            else -> "Слышь, говори конкретно, чего припёрся на стрелу? Время — деньги!"
        }
    }

    private fun calculateSentimentDelta(reply: String, playerMsg: String): Int {
        val lowerReply = reply.lowercase()
        val lowerMsg = playerMsg.lowercase()

        var delta = 0
        if (lowerMsg.contains("деньги") || lowerMsg.contains("откуп") || lowerMsg.contains("уважение") || lowerMsg.contains("мир")) {
            delta += 10
        }
        if (lowerMsg.contains("фраер") || lowerMsg.contains("убью") || lowerMsg.contains("на стрелу") || lowerMsg.contains("чушок")) {
            delta -= 15
        }
        if (lowerReply.contains("кореш") || lowerReply.contains("разумн") || lowerReply.contains("дело") || lowerReply.contains("союз")) {
            delta += 10
        }
        if (lowerReply.contains("заточк") || lowerReply.contains("мокрое место") || lowerReply.contains("осади") || lowerReply.contains("могил")) {
            delta -= 15
        }
        return delta
    }
}

data class BossReply(
    val text: String,
    val sentimentDelta: Int,
    val isAiGenerated: Boolean
)
