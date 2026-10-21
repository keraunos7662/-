package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBossResponsesAreDynamicAndNotRepetitive() = kotlinx.coroutines.runBlocking {
    val service = com.example.data.ai.GeminiBossService()
    val boss = com.example.model.RivalBoss(
      id = "boss_shtyr",
      name = "Лёха Штырь",
      title = "Гопник с подворотни",
      avatarEmoji = "🔪",
      health = 100,
      attack = 15,
      defense = 5,
      requiredRank = com.example.model.ReputationRank.PACAN,
      requiredEnergy = 10,
      rewardMoney = 1500,
      rewardRespect = 10,
      description = "Дворовый хулиган",
      difficultyLabel = "Лёгкий"
    )

    // Test business proposal response
    val bizReply = service.getBossResponse(
      boss = boss,
      playerMessage = "Предлагаю общий бизнес",
      history = emptyList(),
      heroNickname = "Бродяга",
      heroRank = com.example.model.ReputationRank.PACAN,
      heroRespect = 10,
      heroMoney = 5000,
      districtsCount = 1
    )
    assertNotNull(bizReply.text)
    assertTrue(bizReply.text.isNotBlank())

    // Test insult response
    val insultReply = service.getBossResponse(
      boss = boss,
      playerMessage = "lox",
      history = emptyList(),
      heroNickname = "Бродяга",
      heroRank = com.example.model.ReputationRank.PACAN,
      heroRespect = 10,
      heroMoney = 5000,
      districtsCount = 1
    )
    assertNotNull(insultReply.text)
    assertTrue(insultReply.text.isNotBlank())
    // Ensure the insult reply is not the exact same as business reply!
    assertNotEquals(bizReply.text, insultReply.text)
  }
}
